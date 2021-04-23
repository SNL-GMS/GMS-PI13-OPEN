package gms.core.performancemonitoring.soh.control;

import com.google.common.collect.*;
import com.google.common.util.concurrent.Uninterruptibles;
import gms.core.performancemonitoring.soh.control.StationSohControlConfiguration.ConfigurationPair;
import gms.core.performancemonitoring.soh.control.api.StationSohControlInterface;
import gms.core.performancemonitoring.soh.control.api.StationSohMonitoringResultsFluxPair;
import gms.core.performancemonitoring.soh.control.configuration.ChannelSohDefinition;
import gms.core.performancemonitoring.soh.control.configuration.SohControlDefinition;
import gms.core.performancemonitoring.soh.control.configuration.StationSohDefinition;
import gms.core.performancemonitoring.soh.control.kafka.KafkaSohExtractConsumerFactory;
import gms.core.performancemonitoring.soh.control.kafka.ReactorKafkaSohExtractReceiver;
import gms.core.performancemonitoring.soh.control.kafka.SohExtractReceiver;
import gms.shared.frameworks.control.ControlContext;
import gms.shared.frameworks.osd.api.SohRepositoryInterface;
import gms.shared.frameworks.osd.api.util.TimeRangeRequest;
import gms.shared.frameworks.osd.coi.channel.dataacquisitionstatus.AcquiredStationSohExtract;
import gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssue;
import gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssueAnalog;
import gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssueBoolean;
import gms.shared.frameworks.osd.coi.waveforms.RawStationDataFrameMetadata;
import gms.shared.frameworks.soh.repository.SohRepositoryFactory;
import gms.shared.frameworks.systemconfig.SystemConfig;
import gms.shared.frameworks.utilities.SumStatsAccumulator;
import java.util.function.Supplier;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static com.google.common.base.Preconditions.checkNotNull;
import static gms.core.performancemonitoring.soh.control.StationSohControlConstants.APPLICATION_ID;
import static gms.core.performancemonitoring.soh.control.StationSohControlConstants.CAPABILITY_SOH_ROLLUP_OUTPUT_TOPIC;
import static gms.core.performancemonitoring.soh.control.StationSohControlConstants.CAPABILITY_SOH_ROLLUP_OUTPUT_TOPIC_DEFAULT;
import static gms.core.performancemonitoring.soh.control.StationSohControlConstants.INPUT_TOPIC;
import static gms.core.performancemonitoring.soh.control.StationSohControlConstants.INPUT_TOPIC_DEFAULT;
import static gms.core.performancemonitoring.soh.control.StationSohControlConstants.KAFKA_BOOTSTRAP_SERVERS;
import static gms.core.performancemonitoring.soh.control.StationSohControlConstants.MONITOR_LOGGING_DEFAULT_OUTPUT_PERIOD;
import static gms.core.performancemonitoring.soh.control.StationSohControlConstants.MONITOR_LOGGING_FORMAT;
import static gms.core.performancemonitoring.soh.control.StationSohControlConstants.MONITOR_LOGGING_PERIOD;
import static gms.core.performancemonitoring.soh.control.StationSohControlConstants.STATION_SOH_OUTPUT_TOPIC;
import static gms.core.performancemonitoring.soh.control.StationSohControlConstants.STATION_SOH_OUTPUT_TOPIC_DEFAULT;

/**
 * StationSohControl is responsible for controlling computation of StationSoh from
 * AcquiredStationSohExtracts.
 */
public class StationSohControl implements StationSohControlInterface {

  // Used as keys for the SumStatsAccumulator
  enum MonitorBenchmarks {
    CALLS_TO_MONITOR,
    NUM_EXTRACTS,
    NUM_STATION_SOH,
    REFRESH_STATION_SOH_TIME,
    STATION_SOH_COMPUTATION_TIME,
    CAPABILITY_SOH_ROLLUP_COMPUTATION_TIME,
  }

  // We must use apache log4j to have custom log levels
  private static final Logger logger = LogManager.getLogger(StationSohControl.class);
  private static final Level SOH_TIMING = Level.getLevel("SOH_TIMING");

  //
  // We use a supplier here, so that the StationSohControlConfiguration object can be created at a time
  // after the StationSohControl object has been created. This is so that creation of the
  // StationSohControlConfiguration object, which involves reaching out to the OSD for a large
  // number of records, does not hold up creation of StationSohControl.
  //
  private final Supplier<StationSohControlConfiguration> stationSohControlConfigurationSupplier;

  private final AtomicReference<ConfigurationPair> configurationPairRef = new AtomicReference<>();

  private final SystemConfig systemConfig;

  private final KafkaSohExtractConsumerFactory kafkaSohExtractConsumerFactory;

  private final AcquiredSampleTimesByChannel acquiredSampleTimesByChannel;

  private final boolean startAtNextMinute;

  private final SohRepositoryInterface sohRepository;

  // Used in the monitor method to benchmark various items.
  private final SumStatsAccumulator<MonitorBenchmarks> monitorStatsAccumulator =
      new SumStatsAccumulator<>(new EnumMap<>(
          MonitorBenchmarks.class));

  private final Duration monitorLoggingPeriod;

  private SohExtractReceiver sohExtractReceiver;

  private Instant nextMonitorLoggingInstant;

  private volatile boolean started = false;

  /**
   * Constructor which accepts a receiver and senders, so that they can be moocked for unit
   * testing.
   *
   * @param stationSohControlConfigurationSupplier Supplier for a StationSohControlConfiguration
   * object.
   * @param systemConfig the system configuration, which may not be null.
   * @param sohRepository the {@link SohRepositoryInterface} used in creating the
   * stationSohControlConfiguration and for retrieving the latest acquisition time of channels.
   * @param sohExtractReceiver if non-null, a consumer of extracts. This is provided mainly for unit
   * testing.
   * @param preconfiguredKafkaSender if non-null, a sender for the StationSoh messages. This is
   * provided mainly for unit testing. It will normally be non-null when the sohExtractReceiver is
   * non-null.
   */
  StationSohControl(
      Supplier<StationSohControlConfiguration> stationSohControlConfigurationSupplier,
      SystemConfig systemConfig,
      SohRepositoryInterface sohRepository,
      SohExtractReceiver sohExtractReceiver,
      KafkaSender<String, String> preconfiguredKafkaSender) {

    this.startAtNextMinute = false;

    this.stationSohControlConfigurationSupplier = stationSohControlConfigurationSupplier;

    this.systemConfig = systemConfig;

    this.sohRepository = sohRepository;

    this.sohExtractReceiver = sohExtractReceiver;
    this.kafkaSohExtractConsumerFactory = new KafkaSohExtractConsumerFactory(
        preconfiguredKafkaSender,
        getSystemConfig(systemConfig, STATION_SOH_OUTPUT_TOPIC,
            STATION_SOH_OUTPUT_TOPIC_DEFAULT),
        getSystemConfig(systemConfig,
            CAPABILITY_SOH_ROLLUP_OUTPUT_TOPIC, CAPABILITY_SOH_ROLLUP_OUTPUT_TOPIC_DEFAULT),
        this::monitor
    );

    this.acquiredSampleTimesByChannel = new AcquiredSampleTimesByChannel();

    // Use a systemConfig parameter for now, but we might consider adding this
    // to the SohControlDefinition.
    Duration tentativMonitorLoggingPeriod = null;
    String monitorLogginPeriodStr = getSystemConfig(systemConfig, MONITOR_LOGGING_PERIOD,
        MONITOR_LOGGING_DEFAULT_OUTPUT_PERIOD.toString());
    if (monitorLogginPeriodStr != null) {
      try {
        tentativMonitorLoggingPeriod = Duration.parse(monitorLogginPeriodStr);
      } catch (DateTimeParseException pe) {
        logger.error("Not a valid duration: {}", monitorLogginPeriodStr);
      }
    }

    this.monitorLoggingPeriod = tentativMonitorLoggingPeriod != null ?
        tentativMonitorLoggingPeriod : MONITOR_LOGGING_DEFAULT_OUTPUT_PERIOD;

    logger.info("******** Log level is {} *********", logger.getLevel());
  }

  /**
   * Constructor which only takes configuration. Use this for production - it creates its own
   * receiver and sensers.
   *
   * @param stationSohControlConfigurationSupplier Supplier for a StationSohControlConfiguration
   * object.
   * @param systemConfig system configuration
   */
  StationSohControl(
      Supplier<StationSohControlConfiguration> stationSohControlConfigurationSupplier,
      SystemConfig systemConfig,
      SohRepositoryInterface sohRepository) {

    this.sohRepository = sohRepository;

    this.startAtNextMinute = true;

    this.stationSohControlConfigurationSupplier = stationSohControlConfigurationSupplier;

    this.systemConfig = systemConfig;

    this.kafkaSohExtractConsumerFactory = new KafkaSohExtractConsumerFactory(
        KafkaSender.create(SenderOptions.create(senderProperties())),
        getSystemConfig(systemConfig, STATION_SOH_OUTPUT_TOPIC,
            STATION_SOH_OUTPUT_TOPIC_DEFAULT),
        getSystemConfig(systemConfig,
            CAPABILITY_SOH_ROLLUP_OUTPUT_TOPIC, CAPABILITY_SOH_ROLLUP_OUTPUT_TOPIC_DEFAULT),
        this::monitor
    );

    this.acquiredSampleTimesByChannel = new AcquiredSampleTimesByChannel();

    // Use a systemConfig parameter for now, but we might consider adding this
    // to the SohControlDefinition.
    Duration dur = null;
    String s = getSystemConfig(systemConfig, MONITOR_LOGGING_PERIOD,
        MONITOR_LOGGING_DEFAULT_OUTPUT_PERIOD.toString());
    if (s != null) {
      try {
        dur = Duration.parse(s);
      } catch (DateTimeParseException pe) {
        logger.error("Not a valid duration: {}", s);
      }
    }

    this.monitorLoggingPeriod = dur != null ? dur : MONITOR_LOGGING_DEFAULT_OUTPUT_PERIOD;

    logger.info("******** Log level is {} *********", logger.getLevel());
  }

  /**
   * Factory method for {@code StationSohControl}
   *
   * @param controlContext a control context used to obtain configuration info. This must not be
   * null.
   * @return a new instance of {@code StationSohControl}
   */
  public static StationSohControl create(ControlContext controlContext) {

    checkNotNull(controlContext, "ControlContext Cannot be null");

    var systemConfig = controlContext.getSystemConfig();

    var sohRepositoryInterface =
        SohRepositoryFactory.createSohRepository(controlContext.getSystemConfig());

    var configurationSupplier = new Supplier<StationSohControlConfiguration>() {

      private StationSohControlConfiguration stationSohControlConfiguration;

      @Override
      public StationSohControlConfiguration get() {

        if(Objects.isNull(stationSohControlConfiguration)) {
          stationSohControlConfiguration = StationSohControlConfiguration.create(
              controlContext.getProcessingConfigurationConsumerUtility(),
              sohRepositoryInterface);
        }

        return stationSohControlConfiguration;
      }
    };

    return new StationSohControl(
        configurationSupplier,
        systemConfig,
        sohRepositoryInterface);

  }

  /**
   * Return the system configuration used to configure this instance
   *
   * @return SystemConfig
   */
  public SystemConfig getSystemConfig() {
    return systemConfig;
  }

  private TimeRangeRequest buildTimeRangeRequest(Instant cacheStart, Instant endTime) {
    Instant possibleStart = endTime.minus(Duration.ofMinutes(5));
    return TimeRangeRequest
        .create(possibleStart.isAfter(cacheStart) ? possibleStart : cacheStart, endTime);
  }

  public List<AcquiredStationSohExtract> restoreCache(Duration cacheDuration) {
    Instant cachePullTime = Instant.now();
    logger.log(SOH_TIMING, "Starting cache population for {} of data at {}", cacheDuration,
        cachePullTime);

    Duration windowSize = Duration.ofMinutes(5);
    long subIntervals = cacheDuration.dividedBy(windowSize);
    List<Instant> endTimes = LongStream.range(0, subIntervals > 0 ? subIntervals : 1)
        .mapToObj(intervalNum -> cachePullTime.minus(windowSize.multipliedBy(intervalNum)))
        .collect(Collectors.toList());

    Instant cacheStart = cachePullTime.minus(cacheDuration);
    List<AcquiredChannelEnvironmentIssueAnalog> aceiAnalogList = endTimes.parallelStream()
        .map(endTime -> buildTimeRangeRequest(cacheStart, endTime))
        .map(sohRepository::retrieveAcquiredChannelEnvironmentIssueAnalogByTime)
        .flatMap(List::stream)
        .distinct()
        .collect(Collectors.toList());
    List<AcquiredChannelEnvironmentIssueBoolean> aceiBooleanList = endTimes.parallelStream()
        .map(endTime -> buildTimeRangeRequest(cacheStart, endTime))
        .map(sohRepository::retrieveAcquiredChannelEnvironmentIssueBooleanByTime)
        .flatMap(List::stream)
        .distinct()
        .collect(Collectors.toList());
    List<RawStationDataFrameMetadata> rsdfList = endTimes.parallelStream()
        .map(endTime -> buildTimeRangeRequest(cacheStart, endTime))
        .map(sohRepository::retrieveRawStationDataFrameMetadataByTime)
        .flatMap(List::stream)
        .distinct()
        .collect(Collectors.toList());

    Map<String, RangeMap<Instant, AcquiredChannelEnvironmentIssue<?>>> aceiByChannelAndTimeRange =
        Streams.concat(aceiAnalogList.stream(), aceiBooleanList.stream())
            .collect(Collectors.groupingBy(AcquiredChannelEnvironmentIssue::getChannelName,
                Collector.of(TreeRangeMap::create,
                    (map, acei) -> map.put(Range.closed(acei.getStartTime(), acei.getEndTime()),
                        acei),
                    (map1, map2) -> {
                      map1.putAll(map2);
                      return map1;
                    },
                    Collector.Characteristics.IDENTITY_FINISH)));

    List<AcquiredStationSohExtract> cache = rsdfList.stream()
        .map(rsdfMetadata -> {
          List<AcquiredChannelEnvironmentIssue> aceis = rsdfMetadata.getChannelNames().stream()
              .map(aceiByChannelAndTimeRange::get)
              .filter(Objects::nonNull)
              .map(aceisByTimeRange ->
                  aceisByTimeRange.subRangeMap(Range.closed(rsdfMetadata.getPayloadStartTime(),
                      rsdfMetadata.getPayloadEndTime())))
              .map(RangeMap::asMapOfRanges)
              .map(Map::values)
              .flatMap(Collection::stream)
              .collect(Collectors.toList());
          return AcquiredStationSohExtract.create(List.of(rsdfMetadata), aceis);
        })
        .collect(Collectors.toList());

    // collect the stations we already have in the cache
    var stationsAlreadyInCache = cache.stream()
        .flatMap(acquiredStationSohExtract -> acquiredStationSohExtract.getAcquisitionMetadata()
            .stream())
        .map(RawStationDataFrameMetadata::getStationName).collect(
            Collectors.toSet());

    // Load cache of latest end times for each channel but only for those
    // stations not already in the cache
    var channelsToQuery = configurationPairRef
        .get()
        .getStationSohMonitoringDefinition()
        .getStationSohDefinitions()
        .stream()
        .filter(definition -> !stationsAlreadyInCache.contains(definition.getStationName()))
        .map(StationSohDefinition::getChannelSohDefinitions)
        .flatMap(Set::stream)
        .map(ChannelSohDefinition::getChannelName)
        .distinct()
        .collect(Collectors.toList());

    // The cache for timeliness is only initially loaded with the stations it does not have at first
    // when timeliness is calculated the cache is updated with the remaining
    // extracts that flow through the system
    if (!channelsToQuery.isEmpty()) {
      logger.info("Pulling latest sample times for {} channels", channelsToQuery.size());
      // This uses a triply nested query, so small batch sizes
      Map<String, Instant> latestSampleTimeByChannel = Lists.partition(channelsToQuery, 10)
          .parallelStream()
          .map(sohRepository::retrieveLatestSampleTimeByChannel)
          .map(Map::entrySet)
          .flatMap(Set::stream)
          .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

      this.acquiredSampleTimesByChannel.setLatestChannelToEndTime(latestSampleTimeByChannel);
    }

    Instant completionTime = Instant.now();
    Duration cachePullExecutionLength = Duration.between(cachePullTime, completionTime);
    logger.log(SOH_TIMING, "Finished cache retrieval at {} taking {}", completionTime,
        cachePullExecutionLength);

    return cache;
  }

  /**
   * Asynchronously Receives AcquiredStationSohExtract objects and produces the StationSoh objects
   * from and to the publish-subscribe topics. Consider this the start method for the control.
   * Generally, this method will be called once during an application run. If called multiple times,
   * {@code shutdownKafkaThreads()} must be called between calls to this method.
   *
   * @throws IllegalStateException if already called and {@code shutdownKafkaThreads()} has not been
   * called.
   */
  public synchronized void start() {

    if (started) {
      throw new IllegalStateException("Already acquiring and publishing");
    }

    finishInitialization();

    started = true;

    this.stationSohControlConfigurationSupplier.get()
        .subscribeToInterval(
            configurationPair -> {
              logger.info(
                  "Received new configuration. Will readjust service if needed."
              );

              //Update cache
              updateCacheExpiration(configurationPair.getSohControlDefinition());

              //Update the processing interval
              updateReceiverConfiguration(configurationPair.getSohControlDefinition());

              //Update our copy of configuration
              this.configurationPairRef.set(configurationPair);
            }
        );

    // In production, delay until the top of the minute before continuing.
    if (this.startAtNextMinute) {
      Uninterruptibles.sleepUninterruptibly(sleepToNextMinute(), TimeUnit.SECONDS);
    }

    // Sets the start time in the accumulator to now.
    monitorStatsAccumulator.reset();
    // This must be set to prevent a NullPointerException in the monitor method.
    nextMonitorLoggingInstant = monitorStatsAccumulator.getStartTime()
        .plus(monitorLoggingPeriod);

    // A shutdown hook to gracefully shutdown both workers and the thread pool
    Runtime.getRuntime().addShutdownHook(new Thread(this::stop));

    //restore cache
    var cache = restoreCache(
        configurationPairRef.get().getSohControlDefinition().getCacheExpirationDuration());

    // Finally, kick off the extract receiver.
    sohExtractReceiver.receive(
        this.configurationPairRef.get()
            .getSohControlDefinition()
            .getReprocessingPeriod(),
        this.kafkaSohExtractConsumerFactory.getConsumer(),
        cache
    );

    if (logger.isInfoEnabled()) {
      logger.info("Started up with {} processors", Runtime.getRuntime().availableProcessors());
    }
  }

  /**
   * Attempts to shutdown all kafka producer and consumer threads. This method can be thought of as
   * the shutdown or stop method.
   */
  public synchronized void stop() {
    if (started && sohExtractReceiver.isReceiving()) {
      sohExtractReceiver.stop();
    }
  }

  /**
   * This method contains the business logic for the control. It processes the contents of the
   * AcquiredStationSohExtracts to calculate StationSoh. It also uses a preconfigured
   * StationSohDefinition provided by StationSohControlConfiguration and any additional input
   * processing results (e.g. RawStationDataFrameMetadata, AcquiredChannelEnvironmentIssues,
   * ChannelSegments, etc.) necessary to compute the SOH Monitor Values. It publishes the computed
   * StationSohResult objects to a publish-subscribe topic.
   */
  @Override
  public StationSohMonitoringResultsFluxPair monitor(
      Set<AcquiredStationSohExtract> acquiredStationSohExtracts) {

    if (logger.isDebugEnabled()) {
      logger.debug("monitor called with {} extracts", acquiredStationSohExtracts.size());
    }

    final long methodStartMs = System.currentTimeMillis();

    if (acquiredStationSohExtracts.isEmpty()) {
      logger.info("Monitor received no extracts.");
    }

    try {

      monitorStatsAccumulator.addValue(
          MonitorBenchmarks.NUM_EXTRACTS,
          acquiredStationSohExtracts.size());

      // Get the configuration from the AtomicReference and use it for the duration of the
      // method, since it's possible the ref may be updated by another thread before the
      // completion of the method.

      final Set<StationSohDefinition> stationSohDefinitions =
          configurationPairRef.get().getStationSohMonitoringDefinition()
              .getStationSohDefinitions();

      if (stationSohDefinitions == null || stationSohDefinitions.isEmpty()) {
        logger.warn("Not configured to monitor any stations");
        return new StationSohMonitoringResultsFluxPair(
            Flux.empty(),
            Flux.empty()
        );
      }

      // Remember to NEVER throw exceptions because of bad data. Log it, filter it, but
      // don't throw an exception.
      final Duration rollupStationSohTimeTolerance =
          configurationPairRef.get().getStationSohMonitoringDefinition()
              .getRollupStationSohTimeTolerance();

      //
      // Use RollupFluxBuilder to build up our StationSoh Flux and CapabilitySohRollup Flux
      //
      RollupFluxBuilder rollupFluxBuilder = new RollupFluxBuilder(
          acquiredStationSohExtracts,
          stationSohDefinitions,
          configurationPairRef.get().getStationSohMonitoringDefinition()
              .getCapabilitySohRollupDefinitions(),
          rollupStationSohTimeTolerance,
          acquiredSampleTimesByChannel
      );

      long startMs = System.currentTimeMillis();

      AtomicInteger stationSohCount = new AtomicInteger(0);

      AtomicInteger capabilitySohCount = new AtomicInteger(0);

      return new StationSohMonitoringResultsFluxPair(
          rollupFluxBuilder.getStationSohFlux()
              .doOnNext(stationSoh -> {
                stationSohCount.incrementAndGet();
                logger.log(SOH_TIMING, "Emitted Station soh for station {}", stationSoh.getStationName());
              })
              .onErrorContinue((t, o) -> {
                // no Logger.error override that takes a parameter list and a throwable.
                String message = "Error emitting StationSoh " + o;
                logger.error(
                    message,
                    t
                );
              })
              .doOnComplete(
                  () -> {
                    logger.info("COMPLETED statiopnSoh flux!");
                    double computationMs = (double) System.currentTimeMillis() - startMs;
                    // Keep track of the avg amount of time to compute station soh objects.
                    monitorStatsAccumulator.addValue(
                        MonitorBenchmarks.STATION_SOH_COMPUTATION_TIME,
                        computationMs / stationSohCount.get()
                    );
                  }
              ),

          rollupFluxBuilder.getCapabilitySohRollupFlux()
              .doOnNext(capabilitySohRollup -> {
                capabilitySohCount.incrementAndGet();
                logger.log(SOH_TIMING,"Emitted Capability for station group{}", capabilitySohRollup.getForStationGroup());
              })
              .onErrorContinue((t, o) -> {
                String message = "Error emitting CapabilitySohRollup " + o;
                logger.error(
                    message,
                    t
                );
              })
              .doOnComplete(
                  () -> {
                    logger.info("COMPLETED capability flux!");
                    double computationMs = (double) System.currentTimeMillis() - startMs;
                    // Keep track of the avg amount of time to compute station soh objects.
                    monitorStatsAccumulator.addValue(
                        MonitorBenchmarks.CAPABILITY_SOH_ROLLUP_COMPUTATION_TIME,
                        computationMs / capabilitySohCount.get()
                    );
                  }
              )
      );

    } finally {

      // For this benchmark, keep track of the number of seconds to complete the call.
      //
      monitorStatsAccumulator.addValue(
          MonitorBenchmarks.CALLS_TO_MONITOR,
          ((double) (System.currentTimeMillis() - methodStartMs)) / 1000.0);

      // Handle periodic logging.
      handleMonitorLogging();
    }
  }

  /**
   * Get a value from the system config, returning a default value if not defined.
   */
  private static String getSystemConfig(SystemConfig systemConfig, String key,
      String defaultValue) {
    String value = defaultValue;
    try {
      value = systemConfig.getValue(key);
    } catch (MissingResourceException e) {
      if (logger.isWarnEnabled()) {
        logger.warn("{} is not defined in SystemConfig, using default value: {}",
            key, defaultValue);
      }
    }
    return value;
  }

  /**
   * Determine the time between now and the next minute and return the difference in seconds.
   *
   * @return the duration in seconds between now and the next minute.
   */
  private static long sleepToNextMinute() {
    Instant now = Instant.now();
    Instant nextMinute = now.truncatedTo(ChronoUnit.MINUTES).plus(1, ChronoUnit.MINUTES);
    Duration sleep = Duration.between(now, nextMinute);

    logger.info("now {} : nextMinute {}", now, nextMinute);
    logger.info("Waiting {} second(s) before Kafka consumer connection ...", sleep.getSeconds());

    return sleep.getSeconds();
  }

  /**
   * To be called on its own thread once every processing interval, but NOT on the same thread that
   * monitor() is called on.
   */
  private void updateCacheExpiration(
      SohControlDefinition newSohControlDefinition
  ) {
    Duration previousCacheExpiration = this.configurationPairRef.get()
        .getSohControlDefinition()
        .getCacheExpirationDuration();

    Duration newCacheExpiration = newSohControlDefinition
        .getCacheExpirationDuration();

    if (!newCacheExpiration.equals(previousCacheExpiration)) {
      logger.info("Reconstructing cache with new expirationDuration {}", newCacheExpiration);
      sohExtractReceiver.setCachingDuration(newCacheExpiration);
    }

  }

  private void updateReceiverConfiguration(
      SohControlDefinition sohControlDefinition
  ) {

    if (!this.configurationPairRef.get()
        .getSohControlDefinition()
        .getReprocessingPeriod()
        .equals(sohControlDefinition.getReprocessingPeriod())
    ) {

      this.sohExtractReceiver.stopProcessingInterval();

      this.sohExtractReceiver.receive(
          sohControlDefinition.getReprocessingPeriod(),
          this.kafkaSohExtractConsumerFactory.getConsumer(),
          List.of()
      );
    }
  }

  /**
   * At fairly fixed intervals (~ 10 minutes) outputs to the log some statistics on calculations
   * done by the monitor method.
   */
  private void handleMonitorLogging() {

    Instant now = Instant.now();

    if (now.isAfter(nextMonitorLoggingInstant)) {

      // Only do these calculations if the log level is SOH_TIMING or something more sensitive.
      //
      if (logger.isInfoEnabled()) {

        Duration duration = Duration.between(monitorStatsAccumulator.getStartTime(), now);
        long msec = duration.toMillis();
        long minutes = msec / 60_000L;
        long seconds = (msec % 60_000L) / 1000;

        int minExtracts = (int) monitorStatsAccumulator.getMin(MonitorBenchmarks.NUM_EXTRACTS);
        double avgExtracts = monitorStatsAccumulator.getMean(MonitorBenchmarks.NUM_EXTRACTS);
        int maxExtracts = (int) monitorStatsAccumulator.getMax(MonitorBenchmarks.NUM_EXTRACTS);

        // The number of calls to monitor() and the min, avg, max seconds per call.
        long numCallsToMonitor = monitorStatsAccumulator.getN(
            MonitorBenchmarks.CALLS_TO_MONITOR);
        double minSecondsPerCallToMonitor = monitorStatsAccumulator.getMin(
            MonitorBenchmarks.CALLS_TO_MONITOR
        );
        double avgSecondsPerCallToMonitor = monitorStatsAccumulator.getMean(
            MonitorBenchmarks.CALLS_TO_MONITOR
        );
        double maxSecondsPerCallToMonitor = monitorStatsAccumulator.getMax(
            MonitorBenchmarks.CALLS_TO_MONITOR
        );

        // The min, avg, max milliseconds taken to compute each StationSoh
        double minStationSohCompMsec = monitorStatsAccumulator.getMin(
            MonitorBenchmarks.STATION_SOH_COMPUTATION_TIME
        );
        double avgStationSohCompMsec = monitorStatsAccumulator.getMean(
            MonitorBenchmarks.STATION_SOH_COMPUTATION_TIME
        );
        double maxStationSohCompMsec = monitorStatsAccumulator.getMax(
            MonitorBenchmarks.STATION_SOH_COMPUTATION_TIME
        );

        // The min, avg, max milliseconds taken to compute each CapabilitySohRollup
        double minCapSohRollupCompMsec = monitorStatsAccumulator.getMin(
            MonitorBenchmarks.CAPABILITY_SOH_ROLLUP_COMPUTATION_TIME
        );
        double avgCapSohRollupCompMsec = monitorStatsAccumulator.getMean(
            MonitorBenchmarks.CAPABILITY_SOH_ROLLUP_COMPUTATION_TIME
        );
        double maxCapSohRollupCompMsec = monitorStatsAccumulator.getMax(
            MonitorBenchmarks.CAPABILITY_SOH_ROLLUP_COMPUTATION_TIME
        );

        logger.log(Level.INFO, String.format(MONITOR_LOGGING_FORMAT,
            minutes,
            seconds,
            numCallsToMonitor,
            minSecondsPerCallToMonitor,
            avgSecondsPerCallToMonitor,
            maxSecondsPerCallToMonitor,
            minExtracts,
            avgExtracts,
            maxExtracts,
            minStationSohCompMsec,
            avgStationSohCompMsec,
            maxStationSohCompMsec,
            minCapSohRollupCompMsec,
            avgCapSohRollupCompMsec,
            maxCapSohRollupCompMsec
        ));

      }

      monitorStatsAccumulator.reset();
      nextMonitorLoggingInstant = monitorStatsAccumulator.getStartTime()
          .plus(monitorLoggingPeriod);
    }
  }

  /**
   * Finish all of the initialization that could potentially take time, so that we are not
   * holding up setting up the HTTP endpoint.
   */
  private void finishInitialization() {

    // This is the main piece that could hold things up, becaue it reaches out to the OSD
    // to get a large number of records (stations)
    this.configurationPairRef.set(
        stationSohControlConfigurationSupplier.get().getInitialConfigurationPair()
    );

    //
    // If sohExtractReceiver is null by the time this method is called, it means we need to
    // initialize it using production configuration.
    //
    if (Objects.isNull(this.sohExtractReceiver)) {
      this.sohExtractReceiver =
          new ReactorKafkaSohExtractReceiver(
              systemConfig.getValue(KAFKA_BOOTSTRAP_SERVERS),
              getSystemConfig(systemConfig, INPUT_TOPIC, INPUT_TOPIC_DEFAULT),
              systemConfig.getValue(APPLICATION_ID),
              configurationPairRef.get()
                  .getSohControlDefinition()
                  .getCacheExpirationDuration()
          );
    }
  }

  private Properties senderProperties() {
    Properties properties = new Properties();
    properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
        systemConfig.getValue(KAFKA_BOOTSTRAP_SERVERS));
    properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
    properties
        .put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
    // By default, a producer doesn't wait for an acknowledgement from kafka when it sends
    // a message to a topic. Setting it to "1" means that it will wait for at least one kafka
    // node to acknowledge. The safest is "all", but that makes sending a little slower.
    properties.put(ProducerConfig.ACKS_CONFIG, "1");
    return properties;
  }
}
