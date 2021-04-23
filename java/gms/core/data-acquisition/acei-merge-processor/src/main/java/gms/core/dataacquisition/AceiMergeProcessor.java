package gms.core.dataacquisition;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import gms.shared.frameworks.common.annotations.Component;
import gms.shared.frameworks.control.ControlContext;
import gms.shared.frameworks.osd.api.SohRepositoryInterface;
import gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssue.AcquiredChannelEnvironmentIssueType;
import gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssueBoolean;
import gms.shared.frameworks.soh.repository.SohRepositoryFactory;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.ws.rs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component("acei-merge-processor")
@Path("/")
public class AceiMergeProcessor {

  private static final Logger logger = LoggerFactory.getLogger(AceiMergeProcessor.class);

  // The same duration is applied for both boolean and analog benchmark logging.
  private Duration benchmarkLoggingPeriod;

  // And the instant when the next benchmark logging should take place (approx)
  private Instant nextBenchmarkLoggingInstant;

  private final AceiMergeProcessorConfiguration configuration;
  private final SohRepositoryInterface sohRepository;
  // Row key == channel name, col key == issue type,
  // values are ranged by deviation from an instant value (merge tolerance).
  private final Table<String, AcquiredChannelEnvironmentIssueType, AceiBooleanRangeMap> aceiRangeTable;

  // Can be passed to the constructor for use in consumed ACEI messages. This is usually provided
  // for unit testing. If null, an AceiReactiveKafkaConsumer is set up in the start() method.
  private final AceiReactiveConsumer preconfiguredConsumer;

  // Whether or not to keep the original UUIDs of ACEIs read from the input topic, or to replace
  // them before inserting them into the OSD.
  private final boolean keepUUIDs;

  // Processor that handles ACEIs as they arrive on multiple background threads. This is
  // instantiated in startStreaming along with storageTask.
  private AceiParallelProcessor aceiParallelProcessor;
  // Configurable number of threads used by aceiParallelProcessor
  private int processorThreadCount;
  // How often the storage task is scheduled to execute, in milliseconds.
  private long storagePeriodMilliseconds;
  // How many ACEIs must accumulate for the aceiStorageRunnable to perform db operations.
  // (It will also perform db ops if a time interval elapses and it has any ACEIs accumulated.)
  private int minItemsToPerformDbOperations;
  // Limit on how many ACEIs may be inserted or removed via one call to the OSD
  private int maxItemsPerDbInteraction;
  // Max number of db operations that can be performed on separate threads simultaneously.
  private int maxParallelDbOperations;

  private final Duration mergeTolerance;

  // The number of seconds before old ACEIs are purged from the table. Boolean ACEIs whose
  // end times are this number of seconds in the past are thrown out.
  private int cacheExpirationPeriodSeconds;

  // Whether or not to check that ACEIs received have channels in
  // the OSD.
  private boolean checkChannelsInOSD;

  // Set up in the start() method and closed in stop()
  private volatile boolean consuming;
  private AceiReactiveConsumer aceiConsumer;
  private AceiStorageRunnable aceiStorageRunnable;
  private ExecutorService aceiStorageExecutorService;

  AceiMergeProcessor(
      AceiMergeProcessorConfiguration configuration,
      SohRepositoryInterface sohRepository,
      Table<String, AcquiredChannelEnvironmentIssueType, AceiBooleanRangeMap> aceiRangeTable,
      AceiReactiveConsumer preconfiguredConsumer,
      boolean keepUUIDs,
      boolean checkChannelsInOSD) {

    this.configuration = configuration;
    this.sohRepository = sohRepository;
    this.aceiRangeTable = aceiRangeTable;
    // This may be null. In fact, it is usually only non-null for unit testing.
    this.preconfiguredConsumer = preconfiguredConsumer;

    this.keepUUIDs = keepUUIDs;
    this.checkChannelsInOSD = checkChannelsInOSD;

    this.mergeTolerance = Duration.ofMillis(configuration.getMergeToleranceMs());

    this.benchmarkLoggingPeriod = Duration.ofSeconds(
        configuration.getBenchmarkLoggingPeriodSeconds());

    this.storagePeriodMilliseconds = configuration.getStoragePeriodMilliseconds();
    this.cacheExpirationPeriodSeconds = configuration.getCacheExpirationPeriodSeconds();
    this.processorThreadCount = configuration.getProcessorThreadCount();
    this.maxItemsPerDbInteraction = configuration.getMaxItemsPerDbInteraction();
    this.maxParallelDbOperations = configuration.getMaxParallelDbOperations();
    this.minItemsToPerformDbOperations = configuration.getMinItemsToPerformDbOperations();

    Instant now = Instant.now();

    // Just to ensure this cannot be null -- it is reset in startStreaming().
    this.nextBenchmarkLoggingInstant = now;
  }

  public static AceiMergeProcessor create(ControlContext controlContext) {

    checkNotNull(controlContext, "controlContext must not be null");

    AceiMergeProcessorConfiguration configuration = AceiMergeProcessorConfiguration
        .create(controlContext.getSystemConfig());

    SohRepositoryInterface sohRepository = SohRepositoryFactory.createSohRepository(
        controlContext.getSystemConfig()
    );

    Table<String, AcquiredChannelEnvironmentIssueType, AceiBooleanRangeMap> aceiRangeTable =
        HashBasedTable.create();

    return new AceiMergeProcessor(
        configuration, sohRepository, aceiRangeTable, null, false, true
    );
  }

  /**
   * Start consuming off the Kafka topic.
   */
  public synchronized void start() {

    if (consuming) {
      throw new IllegalStateException("already consuming");
    }

    consuming = true;

    if (logger.isInfoEnabled()) {
      logger.info("Starting up...");
      logger.info("OSD updates are performed using an instance of {}",
          (sohRepository != null ? sohRepository.getClass().getName() : "NULL"));
    }

    // So benchmark logging isn't performed immediately before stats can be collected.
    nextBenchmarkLoggingInstant = Instant.now().plus(benchmarkLoggingPeriod);

    aceiConsumer = preconfiguredConsumer != null ? preconfiguredConsumer :
        createReactiveKafkaConsumer();

    aceiStorageRunnable = new AceiStorageRunnable(
        sohRepository,
        checkChannelsInOSD,
        minItemsToPerformDbOperations,
        maxItemsPerDbInteraction,
        maxParallelDbOperations,
        Duration.ofMillis(storagePeriodMilliseconds),
        benchmarkLoggingPeriod
    );

    final int actualThreadCount = processorThreadCount > 0 ? processorThreadCount :
        Runtime.getRuntime().availableProcessors();

    logger.info("Parallel processing of received ACEIs with {} threads", actualThreadCount);

    this.aceiParallelProcessor = new AceiParallelProcessor(
        aceiStorageRunnable,
        actualThreadCount,
        mergeTolerance,
        Duration.ofSeconds(cacheExpirationPeriodSeconds),
        // May make this a configurable parameter.
        Duration.ofMinutes(10L),
        keepUUIDs);

    // Needs to be started up before it can receive any aceis from aceiConsumer.
    this.aceiParallelProcessor.start();

    // This will kick off the aceiStorageRunnable on a background thread.
    aceiStorageExecutorService = Executors.newFixedThreadPool(1);
    aceiStorageExecutorService.submit(aceiStorageRunnable);

    aceiConsumer.consume(
        tuple -> aceiParallelProcessor.add(tuple),
        tuple -> aceiParallelProcessor.add(tuple)
    );

    Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
  }

  /**
   * Stop consuming off the kafka topic and processing ACEIs.
   */
  public synchronized void stop() {
    if (consuming) {
      stopAceiConsumer();
      stopAceiParallelProcessor();
      stopStorageTask();
      consuming = false;
    }
  }

  /**
   * Stop the ACEI consumer.
   */
  private void stopAceiConsumer() {
    if (aceiConsumer != null) {
      try {
        aceiConsumer.stop();
      } finally {
        aceiConsumer = null;
      }
    }
  }

  /**
   * Stop the processor that handles parallel processing of the received ACEIs.
   */
  private void stopAceiParallelProcessor() {
    if (aceiParallelProcessor != null) {
      try {
        // Shuts down the threads in the parallel processor.
        aceiParallelProcessor.shutdown();
      } finally {
        aceiParallelProcessor = null;
      }
    }
  }

  /**
   * Stop the storage task.
   */
  private void stopStorageTask() {
    if (aceiStorageRunnable != null) {
      try {
        if (aceiStorageRunnable.hasIssuesToProcess()) {
          long timesUpMsec = System.currentTimeMillis() + storagePeriodMilliseconds;
          Object waitObj = new Object();
          try {
            while (aceiStorageRunnable.hasIssuesToProcess() &&
                System.currentTimeMillis() < timesUpMsec) {
              synchronized (waitObj) {
                waitObj.wait(100L);
              }
            }
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          }
        }
      } finally {
        aceiStorageRunnable.stop();
        aceiStorageExecutorService.shutdownNow();

        aceiStorageRunnable = null;
        aceiStorageExecutorService = null;
      }
    }
  }

  public long booleanACEIsInserted() {
    return aceiStorageRunnable != null ? aceiStorageRunnable.booleanAceisInserted() : 0L;
  }

  public long analogACEIsInserted() {
    return aceiStorageRunnable != null ? aceiStorageRunnable.analogAceisInserted() : 0L;
  }

  public AceiBooleanRangeMap.Update put(AcquiredChannelEnvironmentIssueBoolean acei) {
    return lazyGet(acei.getChannelName(), acei.getType()).put(acei);
  }

  public AceiBooleanRangeMap lazyGet(String channelName, AcquiredChannelEnvironmentIssueType type) {
    AceiBooleanRangeMap aceiRanges = aceiRangeTable.get(channelName, type);
    if (aceiRanges == null) {
      aceiRanges = new AceiBooleanRangeMap(channelName, type, mergeTolerance, keepUUIDs);
      aceiRangeTable.put(channelName, type, aceiRanges);
    }
    return aceiRanges;
  }

  private AceiReactiveKafkaConsumer createReactiveKafkaConsumer() {
    return new AceiReactiveKafkaConsumer(
        configuration.getBootstrapServers(),
        configuration.getInputAceiTopic(),
        configuration.getApplicationId()
    );
  }
}
