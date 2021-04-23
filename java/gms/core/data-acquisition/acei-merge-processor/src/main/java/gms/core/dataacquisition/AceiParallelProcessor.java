package gms.core.dataacquisition;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.Table;
import com.google.common.collect.TreeRangeMap;
import gms.core.dataacquisition.AceiBooleanRangeMap.PutStats;
import gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssue;
import gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssue.AcquiredChannelEnvironmentIssueType;
import gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssueAnalog;
import gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssueBoolean;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.kafka.receiver.ReceiverOffset;
import reactor.util.function.Tuple2;

/**
 * Handles processing of {@link AcquiredChannelEnvironmentIssue} instances received via a
 * Kafka channel or some other communication mechanism. Analog issues are simply passed on
 * to an accumulator object. Boolean issues are cached for a period of time. New boolean issues
 * are merged with previously-received issues if they are alike in every way except their
 * start time to end time intervals and if their intervals are within a merge tolerance.
 */
public class AceiParallelProcessor {

  private static final Logger logger = LoggerFactory.getLogger(AceiParallelProcessor.class);

  // Represents the state of the instance.
  public enum State {
    NEW,            // Constructed, but not started.
    RUNNING,        // start() has been called and the instance is actively processing ACEIs
    SHUTTING_DOWN,  // shutdown() has been called, but shutdown is not yet complete
    SHUTDOWN        // shutdown() has been called and has completed
  }

  private final AtomicInteger aceiProcessorCounter = new AtomicInteger();

  private final ACEIAccumulator aceiAccumulator;
  private final Duration mergeTolerance;
  private final Duration expirationDuration;
  private final Duration mergeLoggingPeriod;
  private final boolean keepUUIDs;

  private final AtomicReference<State> stateRef = new AtomicReference<>(State.NEW);

  // A thread pool running the ACEIProcessors while in the RUNNING state.
  private ExecutorService threadPool;

  // Maps channel names to the AceiProcessor handling that channel. Each processor may handle
  // many channels
  private final Map<String, AceiProcessor> processorMap = new ConcurrentHashMap<>();
  // The processors, equal in length to the number of threads
  private final AceiProcessor[] aceiProcessors;
  // A revolving cursor used to pair channel names with processors
  private final AtomicInteger processorCursor = new AtomicInteger(0);

  /**
   * Constructor
   * @param aceiAccumulator receives ACEI updates from this processor
   * @param numThreads the number of parallel threads to use
   * @param mergeTolerance tolerance for merging old boolean ACEIs with new ones
   * @param expirationDuration how long to cache old boolean ACEIs
   */
  public AceiParallelProcessor(
      final ACEIAccumulator aceiAccumulator,
      final int numThreads,
      final Duration mergeTolerance,
      final Duration expirationDuration,
      final Duration mergeLoggingPeriod,
      final boolean keepUUIDs) {
    Preconditions.checkNotNull(aceiAccumulator, "aceiAccumulator cannot be null");
    Preconditions.checkNotNull(mergeTolerance, "mergeTolerance cannot be null");
    Preconditions.checkArgument(!mergeTolerance.isNegative(),
        "mergeTolerance must not be negative: " + mergeTolerance);
    Preconditions.checkNotNull(expirationDuration, "expirationDuration cannot be null");
    Preconditions.checkArgument(!expirationDuration.isNegative(),
        "expirationDuration must not be negative: " + expirationDuration);
    Preconditions.checkNotNull(mergeLoggingPeriod, "mergeLoggingPeriod cannot be null");
    Preconditions.checkArgument(!mergeLoggingPeriod.isNegative(),
      "mergeLoggingPeriod cannot be negative");
    this.aceiAccumulator = aceiAccumulator;
    this.mergeTolerance = mergeTolerance;
    this.expirationDuration = expirationDuration;
    this.mergeLoggingPeriod = mergeLoggingPeriod;
    this.keepUUIDs = keepUUIDs;
    int actualNumThreads = numThreads > 0 ? numThreads : Runtime.getRuntime().availableProcessors();
    this.aceiProcessors = new AceiProcessor[actualNumThreads];
    for (int i=0; i<actualNumThreads; i++) {
      this.aceiProcessors[i] = new AceiProcessor();
    }
  }

  /**
   * Add another issue for processing. Must only be called after the instance has been started and
   * before the instance has been shutdown
   * @param tuple2
   */
  public void add(Tuple2<AcquiredChannelEnvironmentIssue<?>, ReceiverOffset> tuple2) {
    Preconditions.checkNotNull(tuple2, "tuple2 must not be null");
    if (getState() != State.RUNNING) {
      throw new IllegalStateException("not started");
    }
    AcquiredChannelEnvironmentIssue<?> acei = tuple2.getT1();
    AceiProcessor processor = processorMap.computeIfAbsent(acei.getChannelName(), cname ->
        aceiProcessors[processorCursor.getAndUpdate(n -> {
          int m = n + 1;
          return m < aceiProcessors.length ? m : 0;
        })]);
    processor.addAcei(tuple2);
  }

  /**
   * Start the processor. This method must only be called for an instance that has not previously
   * been started.
   */
  public void start() {
    if (getState() != State.NEW) {
      throw new IllegalStateException("already started");
    }
    stateRef.set(State.RUNNING);
    threadPool = Executors.newFixedThreadPool(aceiProcessors.length);
    for (AceiProcessor processor: aceiProcessors) {
      threadPool.submit(processor);
    }
  }

  /**
   * Shutdown the instance. This must only be called on an instance that has been started.
   */
  public void shutdown() {
    if (getState() == State.RUNNING) {
      stateRef.set(State.SHUTTING_DOWN);
      // Loop until all processors have handled the ACEIs submitted to them.
      boolean processorsFinished = false;
      do {
        processorsFinished = true;
        for (AceiProcessor processor: aceiProcessors) {
          if (processor.hasAceisToProcess()) {
            processorsFinished = false;
            break;
          }
        }
      } while (!processorsFinished);
      // Complete the shutdown
      stateRef.set(State.SHUTDOWN);
      threadPool.shutdownNow();
      threadPool = null;
    }
  }

  /**
   * Returns the state of the instance.
   * @return
   */
  public State getState() {
    return stateRef.get();
  }

  /**
   * Runnable class that processes a subset of the ACEIs.
   */
  private class AceiProcessor implements Runnable {

    // Queue containing the submitted ACEIs.
    private final Queue<Tuple2<AcquiredChannelEnvironmentIssue<?>, ReceiverOffset>> aceiQueue
        = new ConcurrentLinkedQueue<>();

    // Table that serves as a distributed cache. The keys are channel name and issue type.
    private final Table<String, AcquiredChannelEnvironmentIssueType, AceiBooleanRangeMap>
        booleanRangeMapTable = HashBasedTable.create();
    // Table that serves as a distributed cache for analog aceis. Analog aceis are not merged
    // though, so the values are simple RangeMaps.
    private final Table<String, AcquiredChannelEnvironmentIssueType,
        RangeMap<Instant, AcquiredChannelEnvironmentIssueAnalog>> analogRangeMapTable =
        HashBasedTable.create();

    // The instance number of this AceiProcessor.
    private final int instanceNum = aceiProcessorCounter.getAndIncrement();

    /**
     * Add an issue to the queue for processing.
     * @param tuple2 a tuple containing an ACEI and the ReceiverOffset of the Kafka topic
     *   from which it was read.
     */
    void addAcei(Tuple2<AcquiredChannelEnvironmentIssue<?>, ReceiverOffset> tuple2) {
      synchronized (aceiQueue) {
        aceiQueue.add(tuple2);
        aceiQueue.notifyAll();
      }
    }

    /**
     * Wait for a period for ACEIs to arrive for processing.
     * @param msec
     * @throws InterruptedException
     */
    void waitForData(long msec) throws InterruptedException {
      if (aceiQueue.isEmpty()) {
        long timesUpMsec = System.currentTimeMillis() + msec;
        synchronized (aceiQueue) {
          while (aceiQueue.isEmpty() && System.currentTimeMillis() < timesUpMsec) {
            aceiQueue.wait(msec);
          }
        }
      }
    }

    /**
     * Returns true whenever unprocessed ACEIs are in the queue.
     * @return
     */
    boolean hasAceisToProcess() {
      return !aceiQueue.isEmpty();
    }

    @Override
    public void run() {
      long nowMillis = System.currentTimeMillis();
      long nextExpirationMsec = nowMillis + expirationDuration.toMillis();
      long nextMergeLoggingMsec = nowMillis + mergeLoggingPeriod.toMillis();
      // The state is never placed into SHUTDOWN until no more ACEIs are left to process and
      // no more will ever be submitted.
      while (getState() != State.SHUTDOWN) {
        try {
          // Wait an arbitrary number of milliseconds for data to be available.
          waitForData(100L);
          while (!aceiQueue.isEmpty()) {
            Tuple2<AcquiredChannelEnvironmentIssue<?>, ReceiverOffset> tuple2 = aceiQueue.remove();
            AcquiredChannelEnvironmentIssue<?> acei = tuple2.getT1();
            ReceiverOffset offset = tuple2.getT2();
            if (acei instanceof AcquiredChannelEnvironmentIssueAnalog) {
              handleAnalogAcei((AcquiredChannelEnvironmentIssueAnalog) acei, offset);
            } else if (acei instanceof AcquiredChannelEnvironmentIssueBoolean) {
              handleBooleanAcei((AcquiredChannelEnvironmentIssueBoolean) acei, offset);
            }
          }
          nextExpirationMsec = clearOldData(nextExpirationMsec);
          nextMergeLoggingMsec = mergeLogging(nextMergeLoggingMsec);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
    }

    private long clearOldData(long nextExpirationMsec) {
      // Clear out old boolean ACEIs if necessary.
      long currentTimeMsec = System.currentTimeMillis();
      if (currentTimeMsec >= nextExpirationMsec) {
        Instant now = Instant.now();
        Instant expirationThreshold = now.minus(expirationDuration);
        booleanRangeMapTable.values().forEach(rangeMap -> rangeMap.clearBefore(
            expirationThreshold
        ));
        analogRangeMapTable.values().forEach(rangeMap -> {
          List<Range<Instant>> toClear = new ArrayList<>();
          // Have to accumulate in one loop and remove in another to
          // avoid ConcurrentModificationExceptions.
          for (Range<Instant> range: rangeMap.asMapOfRanges().keySet()) {
            if (range.upperEndpoint().isBefore(expirationThreshold)) {
              toClear.add(range);
            }
          }
          // Now remove them.
          for (Range<Instant> range: toClear) {
            rangeMap.remove(range);
          }
        });
        nextExpirationMsec = now.plus(expirationDuration).toEpochMilli();
      }
      return nextExpirationMsec;
    }

    /**
     * Logs merging statistics for the previous merge logging period.
     * @return when the next merge logging is due, in epoch msecs.
     */
    private long mergeLogging(long nextMergeLoggingMsec) {

      long nowMillis = System.currentTimeMillis();

      if (nowMillis < nextMergeLoggingMsec) {
        // Not time yet, so return the same value.
        return nextMergeLoggingMsec;
      }

      if (logger.isInfoEnabled()) {

        int totalConflicts = 0;
        int totalN = 0;
        double minMergeGapMillis = Double.MAX_VALUE;
        double maxMergeGapMillis = -1.0;
        double meanMergeGapSum = 0.0;

        for (AceiBooleanRangeMap aceiBooleanRangeMap : booleanRangeMapTable.values()) {
          PutStats putStats = aceiBooleanRangeMap.getPutStats(true);
          totalConflicts += putStats.getNumConflicts();
          int numMerges = putStats.getNumMerges();
          // If numMerges == 0, all the values will be NaNs.
          if (numMerges > 0) {
            totalN += numMerges;
            if (putStats.getMinMergeGapMillis() < minMergeGapMillis) {
              minMergeGapMillis = putStats.getMinMergeGapMillis();
            }
            if (putStats.getMaxMergeGapMillis() > maxMergeGapMillis) {
              maxMergeGapMillis = putStats.getMaxMergeGapMillis();
            }
            meanMergeGapSum += numMerges * putStats.getMeanMergeGapMillis();
          }
        }

        double meanMergeGapMillis = totalN > 0 ? meanMergeGapSum/totalN : 0.0;
        if (totalN == 0) {
          minMergeGapMillis = 0.0;
          maxMergeGapMillis = 0.0;
        }

        logger.info(String.format(
            "Processor %02d: in the previous %02d:%02d: %d conflicts have occurred and %d merges with min/mean/max merge gaps of (%.02f, %.02f, %.02f) msecs",
            this.instanceNum,
            mergeLoggingPeriod.toMinutes(), mergeLoggingPeriod.toSecondsPart(),
            totalConflicts, totalN, minMergeGapMillis, meanMergeGapMillis, maxMergeGapMillis));
      }

      return nowMillis + mergeLoggingPeriod.toMillis();
    }

    /**
     * Handles the boolean issues.
     * @param acei
     */
    private void handleBooleanAcei(AcquiredChannelEnvironmentIssueBoolean acei,
        ReceiverOffset offset) {
      String channelName = acei.getChannelName();
      AcquiredChannelEnvironmentIssueType type = acei.getType();
      AceiBooleanRangeMap rangeMap = booleanRangeMapTable.get(channelName, type);
      if (rangeMap == null) {
        rangeMap = new AceiBooleanRangeMap(channelName, type, mergeTolerance, keepUUIDs);
        booleanRangeMapTable.put(channelName, type, rangeMap);
      }
      AceiBooleanRangeMap.Update update = rangeMap.put(acei);
      if (!update.getInsertedAceis().isEmpty() || !update.getRemovedAceis().isEmpty()) {
        aceiAccumulator.addBooleanIssueUpdate(update, offset);
      }
    }

    private void handleAnalogAcei(AcquiredChannelEnvironmentIssueAnalog acei,
        ReceiverOffset offset) {

      String channelName = acei.getChannelName();
      AcquiredChannelEnvironmentIssueType type = acei.getType();
      RangeMap<Instant, AcquiredChannelEnvironmentIssueAnalog> rangeMap =
          analogRangeMapTable.get(channelName, type);
      if (rangeMap == null) {
        rangeMap = TreeRangeMap.create();
        analogRangeMapTable.put(channelName, type, rangeMap);
      }

      List<AcquiredChannelEnvironmentIssueAnalog> analogList = new ArrayList<>();

      Range<Instant> aceiRange = Range.closedOpen(acei.getStartTime(), acei.getEndTime());
      Map<Range<Instant>, AcquiredChannelEnvironmentIssueAnalog> mapOfRanges =
          rangeMap.subRangeMap(aceiRange).asMapOfRanges();

      if (mapOfRanges.isEmpty()) {

        rangeMap.put(aceiRange, acei);
        analogList.add(acei);

      } else {

        if (logger.isWarnEnabled()) {
          logger.warn("Analog ACEI received that overlaps ranges of previously received ACEIs: {}, {}, {}, {}",
              acei.getChannelName(), acei.getType(), acei.getStartTime(), acei.getEndTime());
        }

        // Don't override setting for previous ACEIs, but fill in the gaps between them using values
        // in this acei.

        Set<Range<Instant>> rangeSet = mapOfRanges.keySet();
        Range<Instant>[] rangeArray = rangeSet.toArray(new Range[rangeSet.size()]);

        if (aceiRange.lowerEndpoint().isBefore(rangeArray[0].lowerEndpoint())) {
          AcquiredChannelEnvironmentIssueAnalog aceiBefore =
              AcquiredChannelEnvironmentIssueAnalog.create(acei.getChannelName(), acei.getType(),
                  acei.getStartTime(), rangeArray[0].lowerEndpoint(), acei.getStatus());
          analogList.add(aceiBefore);
          rangeMap.put(Range.closedOpen(aceiBefore.getStartTime(), aceiBefore.getEndTime()),
              aceiBefore);
        }

        int lim = rangeArray.length - 1;
        for (int i=0; i<lim; i++) {
          if (rangeArray[i].upperEndpoint().isBefore(rangeArray[i+1].lowerEndpoint())) {
            AcquiredChannelEnvironmentIssueAnalog aceiBetween =
                AcquiredChannelEnvironmentIssueAnalog.create(acei.getChannelName(), acei.getType(),
                    rangeArray[i].upperEndpoint(), rangeArray[i+1].lowerEndpoint(), acei.getStatus());
            analogList.add(aceiBetween);
            rangeMap.put(Range.closedOpen(aceiBetween.getStartTime(), aceiBetween.getEndTime()),
                aceiBetween);
          }
        }

        if (rangeArray[rangeArray.length - 1].upperEndpoint().isBefore(aceiRange.upperEndpoint())) {
          AcquiredChannelEnvironmentIssueAnalog aceiAfter =
              AcquiredChannelEnvironmentIssueAnalog.create(acei.getChannelName(), acei.getType(),
                  rangeArray[rangeArray.length - 1].upperEndpoint(), aceiRange.upperEndpoint(),
                  acei.getStatus());
          analogList.add(aceiAfter);
          rangeMap.put(Range.closedOpen(aceiAfter.getStartTime(), aceiAfter.getEndTime()), aceiAfter);
        }

      }

      aceiAccumulator.addAnalogIssues(analogList, offset);
    }
  }
}
