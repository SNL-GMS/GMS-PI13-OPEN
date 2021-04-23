package gms.core.dataacquisition;

import gms.shared.frameworks.osd.api.SohRepositoryInterface;
import gms.shared.frameworks.osd.coi.channel.Channel;
import gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssue;
import gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssueAnalog;
import gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssueBoolean;
import gms.shared.frameworks.osd.coi.signaldetection.Station;
import gms.shared.frameworks.utilities.SumStatsAccumulator;
import gms.shared.frameworks.utilities.TimeMarker;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.apache.commons.lang3.Validate;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.kafka.receiver.ReceiverOffset;

public class AceiStorageRunnable implements Runnable, ACEIAccumulator {

  private static final Logger logger = LoggerFactory.getLogger(AceiStorageRunnable.class);

  // Populated once, then used to filter ACEIs stored to the OSD.
  private Set<String> channelsInOsd;
  private Set<String> channelsNotInOsd;

  // Set in the beginning of run() to true, so that channels in the OSD are queried in
  // order to filter ACEIs for channels not in the OSD.
  private final boolean checkChannelsInOSD;

  // Is used as a synchronization lock for access to the sets containing the ACEIs. Its value is the
  // sum of the sizes of the sets.
  private Object collectionLock = new Object();
  // Used for wait/notify mechanism in waitForWorkToDo()/updateTotalItemCount()
  private Object waitLock = new Object();

  private Set<AcquiredChannelEnvironmentIssueBoolean> booleanIssuesToRemove = new HashSet<>();
  private Set<AcquiredChannelEnvironmentIssueBoolean> booleanIssuesToInsert = new HashSet<>();
  private Set<AcquiredChannelEnvironmentIssueAnalog> analogIssuesToInsert = new HashSet<>();

  private AtomicInteger totalItemCount = new AtomicInteger(0);

  private Map<TopicPartition, ReceiverOffset> offsetMap = new HashMap<>();

  // Populated during each run of the task from the preceding 3 sets.
  private Set<AcquiredChannelEnvironmentIssueBoolean> booleansToInsertThisRun;
  private Set<AcquiredChannelEnvironmentIssueBoolean> booleansToRemoveThisRun;
  private Set<AcquiredChannelEnvironmentIssueAnalog> analogsToInsertThisRun;
  private Map<TopicPartition, ReceiverOffset> offsetsToCommitThisRun;

  private final AtomicLong booleansInserted = new AtomicLong(0L);
  private final AtomicLong booleansRemoved = new AtomicLong(0L);
  private final AtomicLong analogsInserted = new AtomicLong(0L);

  // These must be initialized to non-null to prevent a intermittent unit test failure resulting
  // from ACEIs arriving before the instance is completely started up.
  private Set<AcquiredChannelEnvironmentIssueBoolean> failedBooleanInsertsFromLastRun =
      new HashSet<>();
  private Set<AcquiredChannelEnvironmentIssueBoolean> failedBooleanRemovalsFromLastRun =
      new HashSet<>();
  private Set<AcquiredChannelEnvironmentIssueAnalog> failedAnalogInsertsFromLastRun =
      new HashSet<>();

  // Set in the constructor
  private SohRepositoryInterface sohRepository;
  private int minItemsToPerformDbOperations;
  private int maxItemsPerDbOperation;
  private int maxParallelDbOperations;
  private Duration updateIntervalLength;
  private Duration loggingIntervalLength;
  //

  // Used in the run method.
  private final AtomicReference<Thread> runnerRef = new AtomicReference<>();
  private volatile boolean stop;
  //

  // Used in handlePeriodicLogging().
  private final SumStatsAccumulator<AceiBenchmarks> sumStatsAccumulator = new SumStatsAccumulator<>(
      new EnumMap<AceiBenchmarks, SummaryStatistics>(AceiBenchmarks.class)
  );
  private final TimeMarker<AceiStorageTimeMarks> timeMarker = new TimeMarker<>();

  // Initialized in run() and updated with every stats logging.
  private Instant nextLoggingInstant;

  public AceiStorageRunnable(
      SohRepositoryInterface sohRepository,
      boolean checkChannelsInOSD,
      int minItemsToPerformDbOperations,
      int maxItemsPerDbOperation,
      int maxParallelDbOperations,
      Duration updateIntervalLength,
      Duration loggingIntervalLength) {

    Validate.notNull(sohRepository, "sohRepository is required");
    Validate.notNull(updateIntervalLength, "updateIntervalLength is required");
    Validate.notNull(loggingIntervalLength, "loggingIntervalLength is required");
    Validate.isTrue(minItemsToPerformDbOperations > 0,
        "minItemsPerDbIteraction must be greater than 0");
    Validate.isTrue(maxItemsPerDbOperation > 0,
        "maxItemsPerDbIteraction must be greater than 0");

    this.sohRepository = sohRepository;
    this.checkChannelsInOSD = checkChannelsInOSD;
    this.updateIntervalLength = updateIntervalLength;
    this.loggingIntervalLength = loggingIntervalLength;
    this.minItemsToPerformDbOperations = minItemsToPerformDbOperations;
    this.maxItemsPerDbOperation = maxItemsPerDbOperation;
    this.maxParallelDbOperations = maxParallelDbOperations;
  }

  public long booleanAceisInserted() {
    return booleansInserted.get();
  }

  public long booleanAceisRemoved() {
    return booleansRemoved.get();
  }

  public long analogAceisInserted() {
    return analogsInserted.get();
  }

  public boolean hasIssuesToProcess() {
    return totalItemCount.get() > 0L;
  }

  public boolean isRunning() {
    return runnerRef.get() != null && !stop;
  }

  @Override
  public void addBooleanIssueUpdate(AceiBooleanRangeMap.Update update,
      ReceiverOffset offset) {

      // Need a synchronization lock in case access is at the same time as a run of the
      // storage task.
      synchronized (collectionLock) {

        Set<AcquiredChannelEnvironmentIssueBoolean> toRemove = update.getRemovedAceis();
        Set<AcquiredChannelEnvironmentIssueBoolean> toInsert = update.getInsertedAceis();

        // Can't remove ACEIs from the OSD if they haven't yet been added to the OSD.
        booleanIssuesToRemove.addAll(
            // If it's in the set to be inserted, it hasn't been added to the OSD yet.
            toRemove.stream().filter(acei -> !booleanIssuesToInsert.contains(acei))
                .collect(Collectors.toList())
        );

        // And don't insert those that are in the "toRemove" set.
        booleanIssuesToInsert.removeAll(toRemove);
        booleanIssuesToInsert.addAll(toInsert);

        offsetMap.compute(offset.topicPartition(), (tp, ro) -> {
          if (ro == null || ro.offset() < offset.offset()) {
            return offset;
          }
          return ro;
        });

        // Need to update the total item count, since waitOnWorkToDo()
        // checks it.
        updateTotalItemCount();
      }
  }

  @Override
  public void addAnalogIssues(Collection<AcquiredChannelEnvironmentIssueAnalog> analogIssues,
      ReceiverOffset offset) {
    synchronized (collectionLock) {
        analogIssuesToInsert.addAll(analogIssues);
        offsetMap.compute(offset.topicPartition(), (tp, ro) -> {
          if (ro == null || ro.offset() < offset.offset()) {
            return offset;
          }
          return ro;
        });
        // Need to update the total item count, since waitOnWorkToDo()
        // checks it.
        updateTotalItemCount();
    }
  }

  /**
   * Updates the total item count, which is the count of all
   * ACEIs for which db operations have not yet been performed.
   */
  private void updateTotalItemCount() {

      int totalItems = booleanIssuesToInsert.size() + booleanIssuesToRemove.size()
          + analogIssuesToInsert.size() + failedBooleanInsertsFromLastRun.size() +
          failedBooleanRemovalsFromLastRun.size() +
          failedAnalogInsertsFromLastRun.size();

      totalItemCount.set(totalItems);

      // If the total item count is enough to trigger db operations, call notifyAll()
      // on the totalItemCount to break out of the wait in waitForWorkToDo().
      if (totalItems >= minItemsToPerformDbOperations) {
        // To break the wait in waitForWorkToDo()
        synchronized (waitLock) {
          waitLock.notifyAll();
        }
      }
  }

  /**
   * Called in the run loop to wait for there to be work to do. Having work to do
   * results from 1) ACEIs accumulate in the collections until their total count is
   * minItemToPerformDbOperations or above, or 2) it has been at least updateIntervalLength
   * since the last db operations and the ACEIs collections are not empty.
   *
   * @return true if db operations should be performed, false if not. The only situation
   *   that this should return false is if all the ACEI collections are empty.
   *
   * @throws InterruptedException
   */
  private boolean waitOnWorkToDo() throws InterruptedException {
    final long startMsec = System.currentTimeMillis();
    final long timesUpMsec = startMsec + updateIntervalLength.toMillis();
    while (totalItemCount.get() < minItemsToPerformDbOperations &&
      System.currentTimeMillis() < timesUpMsec) {
      long timeToWaitMs = timesUpMsec - System.currentTimeMillis();
      if (timeToWaitMs > 0L) {
        // totalItemCount is only modified while synchronized on collectionLock.
        synchronized (waitLock) {
          waitLock.wait(timeToWaitMs);
        }
      }
    }
    boolean gotWork = totalItemCount.get() > 0;
    if (gotWork) {
      long msecWaited = System.currentTimeMillis() - startMsec;
      sumStatsAccumulator.addValue(AceiBenchmarks.WAIT_FOR_WORK_MSEC, msecWaited);
    }
    return gotWork;
  }

  private void populateSetsForThisRun() {

    synchronized (collectionLock) {

      this.booleansToInsertThisRun = this.booleanIssuesToInsert;
      this.booleansToRemoveThisRun = this.booleanIssuesToRemove;
      this.analogsToInsertThisRun = this.analogIssuesToInsert;
      this.offsetsToCommitThisRun = this.offsetMap;
      this.booleanIssuesToInsert = new HashSet<>();
      this.booleanIssuesToRemove = new HashSet<>();
      this.analogIssuesToInsert = new HashSet<>();
      this.offsetMap = new HashMap<>();

      if (!failedBooleanInsertsFromLastRun.isEmpty()) {
        // These need to be filtered. Even though one was meant to be inserted last run but
        // wasn't, it might now be in the set that is set for removal this run.
        for (AcquiredChannelEnvironmentIssueBoolean acei : failedBooleanInsertsFromLastRun) {
          if (booleansToRemoveThisRun.contains(acei)) {
            // Don't attempt to remove it, because it was never inserted.
            booleansToRemoveThisRun.remove(acei);
          } else {
            booleansToInsertThisRun.add(acei);
          }
        }
        failedBooleanInsertsFromLastRun = new HashSet<>();
      }

      // Add those that failed for the last run.
      if (!failedAnalogInsertsFromLastRun.isEmpty()) {
        this.analogsToInsertThisRun.addAll(failedAnalogInsertsFromLastRun);
        failedAnalogInsertsFromLastRun = new HashSet<>();
      }

      if (!failedBooleanRemovalsFromLastRun.isEmpty()) {
        this.booleansToRemoveThisRun.addAll(failedBooleanRemovalsFromLastRun);
        failedBooleanRemovalsFromLastRun = new HashSet<>();
      }

      updateTotalItemCount();
    }

    if (checkChannelsInOSD) {
      int n = this.channelsNotInOsd.size();
      filterChannelsNotInOSD(booleansToInsertThisRun);
      filterChannelsNotInOSD(booleansToRemoveThisRun);
      filterChannelsNotInOSD(analogsToInsertThisRun);
      if (n != this.channelsNotInOsd.size()) {
        logger.warn("ACEIs encountered for channels not in the OSD: {}", this.channelsNotInOsd);
      }
    }
  }

  private void filterChannelsNotInOSD(Set<? extends AcquiredChannelEnvironmentIssue<?>> set) {
    Iterator<? extends AcquiredChannelEnvironmentIssue<?>> it = set.iterator();
    while (it.hasNext()) {
      String channelName = it.next().getChannelName();
      if (!channelsInOsd.contains(channelName)) {
        channelsNotInOsd.add(channelName);
        it.remove();
      }
    }
  }

  private void getChannelsInOsd() {
      this.channelsInOsd = new HashSet<>();
      List<Station> allStations = sohRepository.retrieveAllStations(Collections.emptyList());
      for (Station station : allStations) {
        channelsInOsd.addAll(
            station.getChannels().stream().map(Channel::getName).collect(Collectors.toList())
        );
      }
      this.channelsNotInOsd = new LinkedHashSet<>();
  }

  private long removeBooleanACEIs() {

    final AtomicInteger successfullyRemoved = new AtomicInteger(0);

    if (!this.booleansToRemoveThisRun.isEmpty()) {

      sumStatsAccumulator.addValue(AceiBenchmarks.BOOLEAN_REMOVAL_COUNT,
          booleansToRemoveThisRun.size());

      timeMarker.markTime(AceiStorageTimeMarks.BOOLEAN_REMOVAL_START);

        // Partition into smaller sets
        List<Set<AcquiredChannelEnvironmentIssueBoolean>> partitioned =
            CollectionUtil.partitionSet(booleansToRemoveThisRun, maxItemsPerDbOperation);

        int numPartitions = partitioned.size();

        // These do the db operations. They return the number of milliseconds taken to
        // perform the operation.
        List<Callable<Long>> callables = new ArrayList<>(numPartitions);

        for (int i=0; i<numPartitions; i++) {
          Set<AcquiredChannelEnvironmentIssueBoolean> partition = partitioned.get(i);
          int partitionNum = i;
          callables.add(() -> {
            long startMsec = System.currentTimeMillis();
            try {
              sohRepository.removeAcquiredChannelEnvironmentIssueBooleans(partition);
              successfullyRemoved.addAndGet(partition.size());
            } catch (Exception e) {
              logger.error("failure removing {} boolean ACEIs from partition {}",
                  partition.size(), partitionNum, e);
              synchronized (failedBooleanRemovalsFromLastRun) {
                failedBooleanRemovalsFromLastRun.addAll(partition);
              }
            }
            return System.currentTimeMillis() - startMsec;
          });
        }

        List<Long> millisecondsList = performDbInteractions(callables);
        for (Long msec: millisecondsList) {
          // A value < 0 indicates a failure. Only count the ones that succeeded.
          if (msec.longValue() >= 0L) {
            sumStatsAccumulator.addValue(AceiBenchmarks.BOOLEAN_REMOVAL_PARTITION_MSEC,
                msec.longValue());
          }
        }

        sumStatsAccumulator.addValue(AceiBenchmarks.BOOLEAN_REMOVAL_SECONDS,
            timeMarker.timeSinceMark(AceiStorageTimeMarks.BOOLEAN_REMOVAL_START,
                TimeUnit.SECONDS, true));

    }

    return successfullyRemoved.get();
  }

  private long insertBooleanACEIs() {

    AtomicInteger successfullyInserted = new AtomicInteger(0);

    if (!booleansToInsertThisRun.isEmpty()) {

      sumStatsAccumulator.addValue(AceiBenchmarks.BOOLEAN_INSERT_COUNT,
          booleansToInsertThisRun.size());
      timeMarker.markTime(AceiStorageTimeMarks.BOOLEAN_INSERT_START);

        // Partition into sufficiently small sets.
        List<Set<AcquiredChannelEnvironmentIssueBoolean>> partitioned =
            CollectionUtil.partitionSet(booleansToInsertThisRun, maxItemsPerDbOperation);

        int numPartitions = partitioned.size();

        List<Callable<Long>> callables = new ArrayList<>(numPartitions);
        for (int i = 0; i < numPartitions; i++) {
          // In order to use in the lambda, since i is not "effectively final".
          int partitionNum = i;
          Set<AcquiredChannelEnvironmentIssueBoolean> partition = partitioned.get(i);
          callables.add(() -> {
            long startMsec = System.currentTimeMillis();
            try {
              sohRepository.storeAcquiredChannelEnvironmentIssueBoolean(partition);
              successfullyInserted.addAndGet(partition.size());
            } catch (Exception e) {
              logger.error("failure inserting {} boolean ACEIs from partition {}",
                  partition.size(), partitionNum, e);
              synchronized (failedBooleanInsertsFromLastRun) {
                failedBooleanInsertsFromLastRun.addAll(partition);
              }
            }
            return System.currentTimeMillis() - startMsec;
          });
        }

        List<Long> millisecondsList = performDbInteractions(callables);
        for (Long msec: millisecondsList) {
          if (msec.longValue() >= 0L) {
            sumStatsAccumulator.addValue(AceiBenchmarks.BOOLEAN_INSERT_PARTITION_MSEC,
                msec.longValue());
          }
        }

        sumStatsAccumulator.addValue(AceiBenchmarks.BOOLEAN_INSERT_SECONDS,
            timeMarker.timeSinceMark(AceiStorageTimeMarks.BOOLEAN_INSERT_START,
                TimeUnit.SECONDS, true));
    }

    return successfullyInserted.get();
  }

  private long insertAnalogACEIs() {

    final AtomicInteger successfullyInserted = new AtomicInteger(0);

    if (!analogsToInsertThisRun.isEmpty()) {

      sumStatsAccumulator.addValue(AceiBenchmarks.ANALOG_INSERT_COUNT,
          analogsToInsertThisRun.size());
      timeMarker.markTime(AceiStorageTimeMarks.ANALOG_INSERT_START);

        // Partition
        List<Set<AcquiredChannelEnvironmentIssueAnalog>> partitioned =
            CollectionUtil.partitionSet(analogsToInsertThisRun, maxItemsPerDbOperation);

        int numPartitions = partitioned.size();

        List<Callable<Long>> callables = new ArrayList<>(numPartitions);
        for (int i=0; i<numPartitions; i++) {
          int partitionNum = i;
          Set<AcquiredChannelEnvironmentIssueAnalog> partition = partitioned.get(i);
          callables.add(() -> {
            long startMsec = System.currentTimeMillis();
            try {
              sohRepository.storeAcquiredChannelSohAnalog(partition);
              successfullyInserted.addAndGet(partition.size());
            } catch (Exception e) {
              logger.error("failure inserting {} analog ACEIs from partition {}",
                  partition.size(), partitionNum, e);
              synchronized (failedAnalogInsertsFromLastRun) {
                failedAnalogInsertsFromLastRun.addAll(partition);
              }
            }
            return System.currentTimeMillis() - startMsec;
          });
        }

        List<Long> millisecondsList = performDbInteractions(callables);
        for (Long msec: millisecondsList) {
          if (msec.longValue() >= 0L) {
            sumStatsAccumulator.addValue(AceiBenchmarks.ANALOG_INSERT_PARTITION_MSEC,
                msec.longValue());
          }
        }

        sumStatsAccumulator.addValue(AceiBenchmarks.ANALOG_INSERT_SECONDS,
            timeMarker.timeSinceMark(AceiStorageTimeMarks.ANALOG_INSERT_START,
                TimeUnit.SECONDS, true));
    }

    return successfullyInserted.get();
  }

  /**
   * Performs db operations by executing each runnable in the list. They operations may be
   * completed sequentially or in a parallel fashion depending on the configuration
   * parameter maxParallelDbOperations.
   *
   * @param callables
   */
  private List<Long> performDbInteractions(List<Callable<Long>> callables) {

    if (callables.isEmpty()) {
      return Collections.emptyList();
    }

    List<Long> results = null;

    int numToCompleteInParallel = maxParallelDbOperations > 0 ? maxParallelDbOperations :
        callables.size();

    if (numToCompleteInParallel == 1) {

      results = performSequentialDbInteractions(callables);

    } else {

      results = performConcurrentDbInteractions(callables, numToCompleteInParallel);

    }

    return results;
  }

  private List<Long> performSequentialDbInteractions(List<Callable<Long>> callables) {
    AtomicInteger callableNum = new AtomicInteger(0);

    return callables.stream()
        .map(callable -> {
          try {
            return (Long) callable.call();
          } catch (Exception e) {
            throw new AceiStorageException(
                String.format("Error performing sequential db operation %d", callableNum.get()),
                e);
          } finally {
            callableNum.incrementAndGet();
          }
        }).collect(Collectors.toList());
  }

  private List<Long> performConcurrentDbInteractions(
      List<Callable<Long>> callables,
      int numToCompleteInParallel) {

    AtomicInteger callableNum = new AtomicInteger(0);

    List<CompletableFuture<Long>> completableFutures = callables.stream()
        .map(callable -> CompletableFuture.supplyAsync(() -> {
            try {
              return callable.call();
            } catch (Exception e) {
              throw new AceiStorageException(
                  String.format("Error performing sequential db operation %d", callableNum.get()),
                  e);
            } finally {
              callableNum.incrementAndGet();
            }
          })
        )
        .collect(Collectors.toList());

    int numCallables = callables.size();

    List<Long> results = new ArrayList<>(numCallables);

    int start = 0;
    while (start < numCallables) {
      List<Long> subsetResults = completeInParallel(
          completableFutures, start, numToCompleteInParallel);
      for (int i = 0; i < subsetResults.size(); i++) {
        results.set(i + start, subsetResults.get(i));
      }
      start += numToCompleteInParallel;
    }

    return results;
  }

  /**
   * Completes a subset from a list of completable futures. It runs the subset simultaneously,
   * but not the others not in the subset.
   *
   * @param completableFutures
   * @param start
   * @param count
   */
  private List<Long> completeInParallel(
      List<CompletableFuture<Long>> completableFutures, int start, int count) {

    int lim = Math.min(start + count, completableFutures.size());
    int actualCount = lim - start;

    CompletableFuture[] futuresArray = new CompletableFuture[actualCount];
    for (int i=start; i<lim; i++) {
      futuresArray[i-start] = completableFutures.get(i);
    }

    CompletableFuture.allOf(futuresArray).join();

    List<Long> result = new ArrayList<>(actualCount);
    for (int i=start; i<lim; i++) {
      Long msec = Long.valueOf(-1L);
      try {
        msec = completableFutures.get(i).get();
      } catch (ExecutionException e) {
        logger.error("Error during parallel execution of db operation {}", i, e);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
      result.add(msec);
    }

    return result;
  }

  private void commitOffsets() {
    Collection<ReceiverOffset> offsets = offsetsToCommitThisRun.values();
    int sz = offsets != null ? offsets.size() : 0;
    if (sz > 0) {
      timeMarker.markTime(AceiStorageTimeMarks.COMMIT_OFFSETS_START);
      AtomicInteger successfulCommitCount = new AtomicInteger(0);
      CountDownLatch latch = new CountDownLatch(sz);
      for (ReceiverOffset receiverOffset : offsetsToCommitThisRun.values()) {
        receiverOffset.commit().subscribe(
            v -> {
            },
            t -> {
              logger.error("Error committing offset {} for topic partition {}",
                  receiverOffset.offset(),
                  receiverOffset.topicPartition(),
                  t);
              latch.countDown();
            },
            () -> {
              successfulCommitCount.incrementAndGet();
              latch.countDown();
            }
        );
      }
      try {
        if (!latch.await(10L, TimeUnit.SECONDS)) {
          logger.error("Timeout waiting for offset commit to finish.");
        }
      } catch (InterruptedException e) {
        logger.error("Thread interrupted while committing offsets");
        // Reinterrupt the thread to avoid a sonarlint warning.
        Thread.currentThread().interrupt();
      } finally {
        sumStatsAccumulator.addValue(AceiBenchmarks.COMMIT_OFFSETS_MSEC,
            timeMarker.timeSinceMark(
                AceiStorageTimeMarks.COMMIT_OFFSETS_START, TimeUnit.MILLISECONDS, true
            ));
      }
    }
  }

  private void handlePeriodicLogging() {

    if (logger.isInfoEnabled()) {

      long msecSinceLastLogging = Math.round(timeMarker.timeSinceMark(
          AceiStorageTimeMarks.LAST_LOGGING_INSTANT, TimeUnit.MILLISECONDS, true));

      long minutesSinceLastLogging = msecSinceLastLogging/60_000L;
      long secondsPartSinceLastLogging = Math.round(((double) msecSinceLastLogging%60_000L)/1000.0);
      if (secondsPartSinceLastLogging == 60L) {
        minutesSinceLastLogging++;
        secondsPartSinceLastLogging = 0L;
      }

      long numStorageOperations = sumStatsAccumulator.getN(AceiBenchmarks.DB_OPERATIONS_SECONDS);

      if (numStorageOperations == 0L) {

        logger.info("BENCHMARKS: %02d:%02d period, 0 database storage operations since last report");

      } else {

        double minStorageSeconds = sumStatsAccumulator.getMin(AceiBenchmarks.DB_OPERATIONS_SECONDS);
        double avgStorageSeconds = sumStatsAccumulator
            .getMean(AceiBenchmarks.DB_OPERATIONS_SECONDS);
        double maxStorageSeconds = sumStatsAccumulator.getMax(AceiBenchmarks.DB_OPERATIONS_SECONDS);

        logger.info(String.format(
            "BENCHMARKS: %02d:%02d period, %d database storage operations taking (%.02f, %.02f, %.02f) seconds",
            minutesSinceLastLogging, secondsPartSinceLastLogging,
            numStorageOperations,
            minStorageSeconds, avgStorageSeconds, maxStorageSeconds));

        handleSpecificOperationLogging("boolean removals",
            AceiBenchmarks.BOOLEAN_REMOVAL_SECONDS,
            AceiBenchmarks.BOOLEAN_REMOVAL_PARTITION_MSEC,
            AceiBenchmarks.BOOLEAN_REMOVAL_COUNT);

        handleSpecificOperationLogging("boolean inserts",
            AceiBenchmarks.BOOLEAN_INSERT_SECONDS,
            AceiBenchmarks.BOOLEAN_INSERT_PARTITION_MSEC,
            AceiBenchmarks.BOOLEAN_INSERT_COUNT);

        handleSpecificOperationLogging("analog inserts",
            AceiBenchmarks.ANALOG_INSERT_SECONDS,
            AceiBenchmarks.ANALOG_INSERT_PARTITION_MSEC,
            AceiBenchmarks.ANALOG_INSERT_COUNT);

        long minCommitOffsetsMsec = Math.round(sumStatsAccumulator.getMin(
            AceiBenchmarks.COMMIT_OFFSETS_MSEC));
        double avgCommitOffsetsMsec = sumStatsAccumulator
            .getMean(AceiBenchmarks.COMMIT_OFFSETS_MSEC);
        long maxCommitOffsetsMsec = Math.round(sumStatsAccumulator.getMax(
            AceiBenchmarks.COMMIT_OFFSETS_MSEC));

        logger.info(String.format("BENCHMARKS: (%d, %.02f, %d) msecs for committing offsets",
            minCommitOffsetsMsec, avgCommitOffsetsMsec, maxCommitOffsetsMsec));

        long minWaitForWorkMsec = Math.round(sumStatsAccumulator.getMin(
            AceiBenchmarks.WAIT_FOR_WORK_MSEC));
        double avgWaitForWorkMsec = sumStatsAccumulator.getMean(AceiBenchmarks.WAIT_FOR_WORK_MSEC);
        long maxWaitForWorkMsec = Math.round(sumStatsAccumulator.getMax(
            AceiBenchmarks.WAIT_FOR_WORK_MSEC
        ));

        logger.info(String.format("BENCHMARKS: (%d, %.02f, %d) msecs waiting for ACEIs to arrive",
            minWaitForWorkMsec, avgWaitForWorkMsec, maxWaitForWorkMsec));
      }
    }

    timeMarker.markTime(AceiStorageTimeMarks.LAST_LOGGING_INSTANT);
    sumStatsAccumulator.reset();
  }

  private void handleSpecificOperationLogging(String operation,
      AceiBenchmarks overallTimingBenchmark,
      AceiBenchmarks partitionTimingBenchmark,
      AceiBenchmarks countBenchmark
      ) {

    long operationCount = sumStatsAccumulator.getN(overallTimingBenchmark);

    double minSeconds = sumStatsAccumulator.getMin(overallTimingBenchmark);
    double avgSeconds = sumStatsAccumulator.getMean(overallTimingBenchmark);
    double maxSeconds = sumStatsAccumulator.getMax(overallTimingBenchmark);

    long minPartitionMsec = Math.round(sumStatsAccumulator.getMin(partitionTimingBenchmark));
    double avgPartitionMsec = sumStatsAccumulator.getMean(partitionTimingBenchmark);
    long maxPartitionMsec = Math.round(sumStatsAccumulator.getMax(partitionTimingBenchmark));

    long minCount = Math.round(sumStatsAccumulator.getMin(countBenchmark));
    long avgCount = Math.round(sumStatsAccumulator.getMean(countBenchmark));
    long maxCount = Math.round(sumStatsAccumulator.getMax(countBenchmark));

    if (logger.isInfoEnabled()) {
      logger.info(String.format(
          "BENCHMARKS: %d operations had %s with (%d, %d, %d) ACEIs taking (%.02f, %.02f, %.02f) seconds with (%d, %.02f, %d) msecs for partition ops",
          operationCount, operation,
          minCount, avgCount, maxCount,
          minSeconds, avgSeconds, maxSeconds,
          minPartitionMsec, avgPartitionMsec, maxPartitionMsec
      ));
    }
  }

  private void tryWork() throws InterruptedException {

    try {

      if (waitOnWorkToDo()) {

        timeMarker.markTime(AceiStorageTimeMarks.DB_OPERATIONS_START);

        // Put items in the sets used for OSD interactions for this run of the task.
        populateSetsForThisRun();

        // Do the 3 types of OSD interactions.

        // Removals first, since the db uses (channelName, type, startTime, endTime) as
        // a unique key constraint. If an ACEI comes in the reverses the status for a given
        // composite key, booleansRemoved should contain the ACEI with that key that needs
        // to be removed.
        booleansRemoved.getAndAdd(removeBooleanACEIs());
        booleansInserted.getAndAdd(insertBooleanACEIs());
        analogsInserted.getAndAdd(insertAnalogACEIs());

        sumStatsAccumulator.addValue(AceiBenchmarks.DB_OPERATIONS_SECONDS,
            timeMarker.timeSinceMark(AceiStorageTimeMarks.DB_OPERATIONS_START,
                TimeUnit.SECONDS, true));

        commitOffsets();
      }

      Instant now = Instant.now();
      if (!now.isBefore(nextLoggingInstant)) {
        handlePeriodicLogging();
        nextLoggingInstant = now.plus(loggingIntervalLength);
      }

      // To prevent exceptions that might bubble up from the SOH repository API from killing
      // the thread.
    } catch (RuntimeException e) {
      logger.error("Error in AceiStorageRunnable", e);
    }
  }

  @Override
  public void run() {

    if (!runnerRef.compareAndSet(null, Thread.currentThread())) {
      throw new IllegalStateException("already running");
    }

    nextLoggingInstant = Instant.now().plus(loggingIntervalLength);
    timeMarker.markTime(AceiStorageTimeMarks.LAST_LOGGING_INSTANT);

    stop = false;

    try {

      logger.info("ACEI database operation thread starting up, {} min ACEIs for operations to be performed, {} max ACEIs per db call",
          this.minItemsToPerformDbOperations,
          this.maxItemsPerDbOperation);
      logger.info("Update interval: {}", updateIntervalLength);

      if (maxParallelDbOperations <= 1) {
        logger.info("Database operations will be performed sequentially");
      } else {
        logger.info(
            "Database operations will be performed concurrently with a limit of {} to be performed in parallel",
            maxParallelDbOperations);
      }

      if (checkChannelsInOSD) {
        long startMsec = System.currentTimeMillis();
        getChannelsInOsd();
        long msec = System.currentTimeMillis() - startMsec;
        logger.info("Retrieving the {} channels currently stored in the OSD required {} milliseconds",
            channelsInOsd.size(), msec);
      }

      sumStatsAccumulator.reset();

      while (!stop) {
        tryWork();
      }

    } catch (InterruptedException e) {
      stop = true;
      Thread.currentThread().interrupt();
    } finally {
      runnerRef.set(null);
      logger.info("ACEI database operation thread shutting down.");
    }
  }

  public void stop() {
    stop = true;
    Thread runner = runnerRef.get();
    if (runner != null) {
      runner.interrupt();
    }
  }

  // These are used as keys for the TimeMarker.
  //
  private enum AceiStorageTimeMarks {
    DB_OPERATIONS_START,
    BOOLEAN_REMOVAL_START,
    BOOLEAN_INSERT_START,
    ANALOG_INSERT_START,
    COMMIT_OFFSETS_START,
    LAST_LOGGING_INSTANT
  }

  // These are used as keys for the SummaryStatsAccumulator
  private enum AceiBenchmarks {
    DB_OPERATIONS_SECONDS,
    BOOLEAN_REMOVAL_SECONDS,
    BOOLEAN_REMOVAL_PARTITION_MSEC,
    BOOLEAN_INSERT_SECONDS,
    BOOLEAN_INSERT_PARTITION_MSEC,
    ANALOG_INSERT_SECONDS,
    ANALOG_INSERT_PARTITION_MSEC,
    COMMIT_OFFSETS_MSEC,
    BOOLEAN_REMOVAL_COUNT,
    BOOLEAN_INSERT_COUNT,
    ANALOG_INSERT_COUNT,
    WAIT_FOR_WORK_MSEC
  }
}
