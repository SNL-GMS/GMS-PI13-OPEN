package gms.core.performancemonitoring.ssam.control;

import com.google.common.collect.Lists;
import gms.core.performancemonitoring.soh.control.configuration.StationSohDefinition;
import gms.core.performancemonitoring.ssam.control.api.StationSohAnalysisManager;
import gms.core.performancemonitoring.ssam.control.cache.StationSohCorrelatingCacheProcessor;
import gms.core.performancemonitoring.ssam.control.config.StationSohMonitoringDefinition;
import gms.core.performancemonitoring.ssam.control.config.StationSohMonitoringUiClientParameters;
import gms.core.performancemonitoring.ssam.control.dataprovider.FluxProvider;
import gms.core.performancemonitoring.ssam.control.dataprovider.KafkaFluxProvider;
import gms.core.performancemonitoring.ssam.control.datapublisher.KafkaPublisher;
import gms.core.performancemonitoring.ssam.control.processor.AcknowledgeSohStatusChangeMaterializedViewProcessor;
import gms.core.performancemonitoring.ssam.control.processor.QuietedSohStatusChangeUpdateMaterializedViewProcessor;
import gms.core.performancemonitoring.uimaterializedview.AcknowledgedSohStatusChange;
import gms.core.performancemonitoring.ssam.control.processor.MaterializedViewProcessor;
import gms.core.performancemonitoring.uimaterializedview.QuietedSohStatusChangeUpdate;
import gms.core.performancemonitoring.uimaterializedview.SohQuietAndUnacknowledgedCacheManager;
import gms.core.performancemonitoring.uimaterializedview.UiStationAndStationGroups;
import gms.core.performancemonitoring.uimaterializedview.UiStationSoh;
import gms.shared.frameworks.control.ControlContext;
import gms.shared.frameworks.osd.api.SohRepositoryInterface;
import gms.shared.frameworks.osd.coi.signaldetection.StationGroup;
import gms.shared.frameworks.osd.coi.soh.CapabilitySohRollup;
import gms.shared.frameworks.osd.coi.soh.StationSoh;
import gms.shared.frameworks.osd.coi.soh.quieting.QuietedSohStatusChange;
import gms.shared.frameworks.osd.coi.soh.quieting.UnacknowledgedSohStatusChange;
import gms.shared.frameworks.osd.coi.systemmessages.SystemMessage;
import gms.shared.frameworks.soh.repository.SohRepositoryFactory;
import gms.shared.frameworks.systemconfig.SystemConfig;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;

/**
 * StationSohAnalysisManagerControl(SSAM) is responsible for controlling computation needed for
 * the UI including station acknowledgement and quieting.  SSAM tracks changes to quiet and
 * acknowledge stations and publishes to the ui-materialized-view kafka topic.  SSAM also publishes
 * Station SOH related status messages to be viewed using SystemMessagesDisplay.
 */
public class StationSohAnalysisManagerControl implements StationSohAnalysisManager {

  private static class DataContainer {

    // Cache of latest StationSoh. Should contain the most recent StationSoh for each station being
    // monitored. Initialized at startup from the OSD and kept up to date with StationSoh received
    // from a kafka topic. Keyed by station name.
    final Map<String, StationSoh> latestStationSohByStation;

    // Cache of latest CapabilitySohRollup. Should contain the most recent
    // CapabilitySohRollup for each station group being monitored.
    // Initialized at startup from the OSD and kept up to date with CapabilitySohRollup
    // received from a kafka topic. Keyed by station group name.
    final Map<String, CapabilitySohRollup> latestCapabilitySohRollupByStationGroup;

    // Contains quieted Soh status changes. Initialized from the OSD at
    // startup and used in the list manager initialization.
    final Set<QuietedSohStatusChange> quietedSohStatusChanges;

    // Contains unacknowledged SohStatus changes. This is initialized from
    // the OSD at startup and used in the list manager initialization.
    final Set<UnacknowledgedSohStatusChange> unacknowledgedSohStatusChanges;

    DataContainer(
        Map<String, StationSoh> latestStationSohByStation,
        Map<String, CapabilitySohRollup> latestCapabilitySohRollupByStationGroup,
        Set<QuietedSohStatusChange> quietedSohStatusChanges,
        Set<UnacknowledgedSohStatusChange> unacknowledgedSohStatusChanges) {
      this.latestStationSohByStation = latestStationSohByStation;
      this.latestCapabilitySohRollupByStationGroup = latestCapabilitySohRollupByStationGroup;
      this.quietedSohStatusChanges = quietedSohStatusChanges;
      this.unacknowledgedSohStatusChanges = unacknowledgedSohStatusChanges;
    }
  }

  private static final Logger logger = LogManager.getLogger(StationSohAnalysisManagerControl.class);

  private static final Level SOH_TIMING = Level.getLevel("SOH_TIMING");

  private static final Level TIMING = Level.getLevel("TIMING");

  public static final String KAFKA_BOOTSTRAP_SERVERS = "kafka-bootstrap-servers";
  public static final String STATION_SOH_INPUT_TOPIC = "soh_station_input_topic";
  public static final String STATION_SOH_INPUT_TOPIC_DEFAULT = "soh.station-soh";
  public static final String ACKNOWLEDGED_SOH_STATUS_CHANGE_INPUT_TOPIC = "status_change_input_topic";
  public static final String ACKNOWLEDGED_SOH_STATUS_CHANGE_INPUT_TOPIC_DEFAULT = "soh.ack-station-soh";
  public static final String QUIETED_SOH_STATUS_CHANGE_INPUT_TOPIC = "quieted_list_input_topic";
  public static final String QUIETED_SOH_STATUS_CHANGE_INPUT_TOPIC_DEFAULT = "soh.quieted-list";
  public static final String CAPABILITY_SOH_ROLLUP_INPUT_TOPIC = "capability_rollup_input_topic";
  public static final String CAPABILITY_SOH_ROLLUP_INPUT_TOPIC_DEFAULT = "soh.capability-rollup";
  public static final String STATION_SOH_ANALYSIS_VIEW_OUTPUT_TOPIC = "materialized_view_output_topic";
  public static final String SOH_SYSTEM_MESSAGE_OUTPUT_TOPIC = "system_message_output_topic";
  public static final String SOH_SYSTEM_MESSAGE_OUTPUT_TOPIC_DEFAULT = "system.system-messages";
  public static final String STATION_SOH_ANALYSIS_VIEW_OUTPUT_TOPIC_DEFAULT = "soh.ui-materialized-view";
  public static final String STATION_SOH_QUIETED_OUTPUT_TOPIC = "quieted_status_change_output_topic";
  public static final String STATION_SOH_QUIETED_OUTPUT_TOPIC_DEFAULT = "soh.quieted-status-change";
  public static final String STATION_SOH_STATUS_CHANGE_OUTPUT_TOPIC = "status_change_output_topic";
  public static final String STATION_SOH_STATUS_CHANGE_OUTPUT_TOPIC_DEFAULT = "soh.status-change-event";
  private final StationSohAnalysisManagerConfiguration stationSohAnalysisManagerConfiguration;
  private final SystemConfig systemConfig;

  // Set to true in start(), back to false in stop()
  private volatile boolean active = false;

  // List manager to handle unacknowledged and quieted states needed for the
  // UiStationAndStationGroupGenerator class
  private SohQuietAndUnacknowledgedCacheManager sohQuietAndUnacknowledgedCacheManager;

  // Initialized in startKafkaConsumersAndProducer()
  private KafkaSender<String, String> kafkaSender;

  private final EmitterProcessor<SystemMessage> systemMessageEmitterProcessor = EmitterProcessor
      .create();

  private final EmitterProcessor<UnacknowledgedSohStatusChange> unacknowledgedSohStatusChangeEmitterProcessor = EmitterProcessor
      .create();

  private StationSohAnalysisManagerControl(
      StationSohAnalysisManagerConfiguration stationSohAnalysisManagerConfiguration,
      SystemConfig systemConfig
  ) {
    this.stationSohAnalysisManagerConfiguration = stationSohAnalysisManagerConfiguration;
    this.systemConfig = systemConfig;
  }

  /**
   * Factory Method for {@link StationSohAnalysisManager}
   *
   * @param controlContext access to externalized dependencies.
   * @return {@link StationSohAnalysisManagerControl}
   */
  public static StationSohAnalysisManagerControl create(ControlContext controlContext) {

    Objects.requireNonNull(controlContext);

    StationSohAnalysisManagerConfiguration stationSohAnalysisManagerConfiguration =
        StationSohAnalysisManagerConfiguration.create(
            controlContext.getProcessingConfigurationConsumerUtility(),
            SohRepositoryFactory.createSohRepository(controlContext.getSystemConfig()));

    return new StationSohAnalysisManagerControl(
        stationSohAnalysisManagerConfiguration,
        controlContext.getSystemConfig());
  }

  /**
   * Starts the control. This method should perform any time-consuming initialization steps and
   * start long-running background processes, if any. It should not run indefinitely, but should
   * return within a reasonable amount of time of not more than a few seconds.
   */
  public synchronized void start() {

    if (active) {
      throw new IllegalStateException("control is already active");
    }

    try {

      active = true;

      // Initialize from OSD on start
      Instant cachePullStart = Instant.now();
      logger.log(SOH_TIMING, "Starting cache initializing at {}", cachePullStart);

      initializeFromOsd(dataContainer -> {
        Duration cacheFinish = Duration.between(cachePullStart, Instant.now());
        logger.log(SOH_TIMING, "Cache initialization completed at {}", cacheFinish);

        //
        // Instantiate our quiet/unacknowledged list manager
        //
        // This needs to be instantiated before we start our providers and publishers
        // because they will need to utilize it.
        //
        sohQuietAndUnacknowledgedCacheManager = new SohQuietAndUnacknowledgedCacheManager(
            dataContainer.quietedSohStatusChanges,
            dataContainer.unacknowledgedSohStatusChanges,
            new ArrayList<>(dataContainer.latestStationSohByStation.values()),
            this.stationSohAnalysisManagerConfiguration.resolveDisplayParameters(),
            systemMessageEmitterProcessor.sink(),
            unacknowledgedSohStatusChangeEmitterProcessor.sink()
        );

        //
        // Start the various providers and publishers
        //
        logger.info("Starting providers and publishers");
        startProvidersAndPublishers(
            dataContainer.latestStationSohByStation,
            dataContainer.latestCapabilitySohRollupByStationGroup
        );
      });

      // Register a shutdown hook to stop the control.
      Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
    } catch (RuntimeException rte) {

      // This is most likely a NullPointerException from a configuration variable not being set
      // or some other kind of unchecked exception. Rather than letting such exceptions bubble up
      // to crash the application and hence the docker container, which will just restart, crash
      // again, restart, crash again..., log the error so people can look in the container log
      // and diagnosis the problem.
      logger.error("Error starting the control", rte);
    }
  }

  /**
   * Start up all of our providers and publishes so that we can receive things and publish things.
   *
   * @param latestStationSohByStation The latest StationSohs, by station name
   * @param latestCapabilitySohRollupByStationGroup The latest CapabilitySohRollups, by group name
   */
  private void startProvidersAndPublishers(
      Map<String, StationSoh> latestStationSohByStation,
      Map<String, CapabilitySohRollup> latestCapabilitySohRollupByStationGroup
  ) {

    kafkaSender = KafkaSender.create(senderOptions(systemConfig));

    List<StationGroup> stationGroups = stationSohAnalysisManagerConfiguration.stationGroups();

    EmitterProcessor<Pair<CapabilitySohRollup, List<StationSoh>>> correlationProvider = EmitterProcessor
        .create();

    startSystemMessagesPublisher(
        systemMessageEmitterProcessor,
        kafkaSender,
        systemConfig
    );

    StationSohCorrelatingCacheProcessor rollupSohCacheProcessor =
        StationSohCorrelatingCacheProcessor.create(correlationProvider.sink());

    startStateOfHealthProviders(
        rollupSohCacheProcessor,
        systemConfig,
        latestStationSohByStation,
        latestCapabilitySohRollupByStationGroup
    );

    startMaterializedViewPublisher(
        correlationProvider,
        stationGroups,
        latestStationSohByStation
    );

    startAcknowledgedMaterializedViewPublisher(
        latestStationSohByStation,
        latestCapabilitySohRollupByStationGroup,
        stationGroups
    );

    var quietedPublisherProcessor = EmitterProcessor.<QuietedSohStatusChangeUpdate>create();

    startQuietedMaterializedViewPublisher(
        quietedPublisherProcessor.sink(),
        latestStationSohByStation,
        latestCapabilitySohRollupByStationGroup,
        stationGroups
    );

    //
    // Start the relatively simple Quieted and Unack publishers.
    //
    var quietedOutputTopic = getSystemConfigValue(systemConfig, STATION_SOH_QUIETED_OUTPUT_TOPIC,
        STATION_SOH_QUIETED_OUTPUT_TOPIC_DEFAULT);

    var unackOutputTopic = getSystemConfigValue(systemConfig,
        STATION_SOH_STATUS_CHANGE_OUTPUT_TOPIC,
        STATION_SOH_STATUS_CHANGE_OUTPUT_TOPIC_DEFAULT);

    var quietedPublisher = new KafkaPublisher<>(
        quietedPublisherProcessor,
        this.kafkaSender,
        quietedOutputTopic
    );

    quietedPublisher.start();

    var unackPublisher = new KafkaPublisher<>(
        unacknowledgedSohStatusChangeEmitterProcessor,
        this.kafkaSender,
        unackOutputTopic
    );

    unackPublisher.start();

  }

  /**
   * Start publishing  System Messages provided by systemMessageFlux
   *
   * @param systemMessageFlux Flux of SystemMessage objects to publish
   * @param kafkaSender KafkaSender object
   * @param systemConfig system configuration containing connection info
   */
  private static void startSystemMessagesPublisher(
      Flux<SystemMessage> systemMessageFlux,
      KafkaSender<String, String> kafkaSender,
      SystemConfig systemConfig
  ) {
    var systemMessagesOutputTopic = getSystemConfigValue(systemConfig,
        SOH_SYSTEM_MESSAGE_OUTPUT_TOPIC,
        SOH_SYSTEM_MESSAGE_OUTPUT_TOPIC_DEFAULT);

    var systemMessagePublisher = new KafkaPublisher<>(
        systemMessageFlux,
        kafkaSender,
        systemMessagesOutputTopic
    );

    systemMessagePublisher.start();
  }

  /**
   * Start the StationSoh and CapabilitySohRollup providers, and subscribe the
   * stationSohCorrelatingCacheProcessor add and track methods to them.
   *
   * @param stationSohCorrelatingCacheProcessor The object that correlates StationSoh and CapabilitySohRollup
   * @param systemConfig System configuration
   * @param latestStationSohByStation The latest StationSohs, by station name
   * @param latestCapabilitySohRollupByStationGroup The latest CapabilitySohRollups, by group name
   */
  private static void startStateOfHealthProviders(
      StationSohCorrelatingCacheProcessor stationSohCorrelatingCacheProcessor,
      SystemConfig systemConfig,
      Map<String, StationSoh> latestStationSohByStation,
      Map<String, CapabilitySohRollup> latestCapabilitySohRollupByStationGroup
  ) {
    var stationSohInputTopic = getSystemConfigValue(systemConfig, STATION_SOH_INPUT_TOPIC,
        STATION_SOH_INPUT_TOPIC_DEFAULT);

    var capabilityRollupInputTopic = getSystemConfigValue(systemConfig,
        CAPABILITY_SOH_ROLLUP_INPUT_TOPIC, CAPABILITY_SOH_ROLLUP_INPUT_TOPIC_DEFAULT);

    FluxProvider<StationSoh> stationSohKafkaFluxProvider = KafkaFluxProvider
        .create(StationSoh.class, stationSohInputTopic, systemConfig);
    FluxProvider<CapabilitySohRollup> capabilitySohRollupFluxProvider = KafkaFluxProvider
        .create(CapabilitySohRollup.class, capabilityRollupInputTopic, systemConfig);

    stationSohKafkaFluxProvider.getFlux()
        .doOnNext(
            stationSoh -> latestStationSohByStation.put(stationSoh.getStationName(), stationSoh)
        )
        .subscribe(stationSohCorrelatingCacheProcessor::add);

    capabilitySohRollupFluxProvider.getFlux()
        .doOnNext(
            capabilitySohRollup -> latestCapabilitySohRollupByStationGroup.put(
                capabilitySohRollup.getForStationGroup(), capabilitySohRollup
            )
        )
        .subscribe(stationSohCorrelatingCacheProcessor::track);
  }

  /**
   * Start the main materialized view processor - the one that contains the most recent state-of-health
   * info from upstream.
   *
   * @param correlationProvider Flux that provides the correlated CapabilitySohRollups and StationSohs,
   * in the form a of a pairing of a single CapabilitySohRollup to the list of StationSoh objects
   * associated with it.
   * @param stationGroups The StationGroups we are working with, for reference
   * @param latestStationSohByStation The latest StationSohs, by station name
   */
  private void startMaterializedViewPublisher(
      Flux<Pair<CapabilitySohRollup, List<StationSoh>>> correlationProvider,
      List<StationGroup> stationGroups,
      Map<String, StationSoh> latestStationSohByStation
  ) {
    var materializedViewOutputTopic = getSystemConfigValue(systemConfig,
        STATION_SOH_ANALYSIS_VIEW_OUTPUT_TOPIC,
        STATION_SOH_ANALYSIS_VIEW_OUTPUT_TOPIC_DEFAULT);

    MaterializedViewProcessor matViewProcessor = MaterializedViewProcessor
        .create(sohQuietAndUnacknowledgedCacheManager,
            stationSohAnalysisManagerConfiguration.resolveDisplayParameters(),
            stationGroups, systemMessageEmitterProcessor.sink());

    // Log for TIMING metrics
    var logEmitter = EmitterProcessor.<UiStationAndStationGroups>create();
    var logEmitterSink = logEmitter.sink();
    logEmitter.flatMap(
        uiStationAndStationGroups -> Flux.fromIterable(uiStationAndStationGroups.getStationSoh()))
        .distinct(UiStationSoh::getUuid)
        .subscribeOn(Schedulers.boundedElastic())
        .subscribe(uiStationSoh -> logger.log(
            TIMING,
            "SOH object {} with timestamp {}; now: {}",
            uiStationSoh.getUuid(),
            Instant.ofEpochMilli(uiStationSoh.getTime()),
            Instant.now()
        ));

    var mainMaterializedViewPublisher = new KafkaPublisher<>(
        correlationProvider

            //
            // Because the correlation logic must be invoked when either a new StationSoh OR
            // new CapabilitySohRollup appears, there is a slight chance that a pair will be duplicated
            // when the last StationSoh for a CapabilitySohRollup appears at the same time that the
            // CapabilitySohRollup appears.
            //
            .distinct(Pair::getKey)

            //
            // Separate groups of pairs out by the calculation time, which should match the time of
            // the first, or any, StationSoh of the correlated pair.
            //
            // TODO: On the SOH-control side, Look unto giving all CapabilitySohRollups in a single
            //  calculation interval the same timestamp like we do for StationSoh, so that we arent
            //  doing this awkward retrieval of just the first element of a list.
            //
            .bufferUntilChanged(pair -> pair.getRight().get(0).getTime())
            .doOnNext(listOfPairs -> sohQuietAndUnacknowledgedCacheManager.updateUnacknowledgedList(
                new ArrayList<>(latestStationSohByStation.values()))
            )
            .map(matViewProcessor)
            .flatMap(Flux::fromIterable)
            .doOnNext(logEmitterSink::next)
            .doOnTerminate(logEmitterSink::complete),

        this.kafkaSender,
        materializedViewOutputTopic
    );

    mainMaterializedViewPublisher.start();
  }

  /**
   * Start the "Acknowledged" materialized view processor, which publishes the materialized view
   * with only acknowledged state-of-health statuses.
   *
   * @param latestStationSohByStation The latest StationSohs, by station name
   * @param latestCapabilitySohRollupByStationGroup The latest CapabilitySohRollups, by group name
   * @param stationGroups The StationGroups we are working with, for reference
   */
  private void startAcknowledgedMaterializedViewPublisher(
      Map<String, StationSoh> latestStationSohByStation,
      Map<String, CapabilitySohRollup> latestCapabilitySohRollupByStationGroup,
      List<StationGroup> stationGroups
  ) {
    var ackInputTopic = getSystemConfigValue(systemConfig,
        ACKNOWLEDGED_SOH_STATUS_CHANGE_INPUT_TOPIC,
        ACKNOWLEDGED_SOH_STATUS_CHANGE_INPUT_TOPIC_DEFAULT);

    var acknowledgedMaterializedViewOutputTopic = getSystemConfigValue(systemConfig,
        STATION_SOH_ANALYSIS_VIEW_OUTPUT_TOPIC,
        STATION_SOH_ANALYSIS_VIEW_OUTPUT_TOPIC_DEFAULT);

    KafkaFluxProvider<AcknowledgedSohStatusChange> ackFluxProvider = KafkaFluxProvider
        .create(AcknowledgedSohStatusChange.class, ackInputTopic, systemConfig);
    Flux<AcknowledgedSohStatusChange> ackFlux = ackFluxProvider.getFlux();

    var acknowledgedMaterializedViewPublisher = new KafkaPublisher<>(
        ackFlux.doOnNext(sohQuietAndUnacknowledgedCacheManager::addAcknowledgedStationToQuietList)
            .onErrorContinue(
                (throwable, object) ->
                    logger.error(
                        () -> "Error with acknowledgement " + object,
                        throwable
                    )
            )
            .map(AcknowledgeSohStatusChangeMaterializedViewProcessor.create(
                stationSohAnalysisManagerConfiguration.resolveDisplayParameters(),
                sohQuietAndUnacknowledgedCacheManager,
                latestStationSohByStation,
                latestCapabilitySohRollupByStationGroup,
                systemMessageEmitterProcessor.sink(),
                stationGroups
            ))
            .flatMap(Flux::fromIterable),
        this.kafkaSender,
        acknowledgedMaterializedViewOutputTopic
    );

    acknowledgedMaterializedViewPublisher.start();
  }

  /**
   * Start the "Quieted" materialized view processor, which publishes the materialized view
   * with only quieted state-of-health statuses.
   *
   * @param quietedPublisherSink QuietedSohStatusChangeUpdate sink
   * @param latestStationSohByStation The latest StationSohs, by station name
   * @param latestCapabilitySohRollupByStationGroup The latest CapabilitySohRollups, by group name
   * @param stationGroups The StationGroups we are working with, for reference
   */
  private void startQuietedMaterializedViewPublisher(
      FluxSink<QuietedSohStatusChangeUpdate> quietedPublisherSink,
      Map<String, StationSoh> latestStationSohByStation,
      Map<String, CapabilitySohRollup> latestCapabilitySohRollupByStationGroup,
      List<StationGroup> stationGroups
  ) {
    var quietedInputTopic = getSystemConfigValue(systemConfig,
        QUIETED_SOH_STATUS_CHANGE_INPUT_TOPIC,
        QUIETED_SOH_STATUS_CHANGE_INPUT_TOPIC_DEFAULT);

    var quietedMaterializedViewOutputTopic = getSystemConfigValue(systemConfig,
        STATION_SOH_ANALYSIS_VIEW_OUTPUT_TOPIC,
        STATION_SOH_ANALYSIS_VIEW_OUTPUT_TOPIC_DEFAULT);

    KafkaFluxProvider<QuietedSohStatusChangeUpdate> quietFluxProvider = KafkaFluxProvider
        .create(QuietedSohStatusChangeUpdate.class,
            quietedInputTopic,
            systemConfig);

    Flux<QuietedSohStatusChangeUpdate> quietedFlux = quietFluxProvider.getFlux()
        .doOnTerminate(quietedPublisherSink::complete)
        .doOnNext(quietedPublisherSink::next);

    var quietedMaterializedViewPublisher = new KafkaPublisher<>(
        quietedFlux
            .doOnNext(sohQuietAndUnacknowledgedCacheManager::addQuietSohStatusChange)
            .onErrorContinue(
                (throwable, object) ->
                    logger.error(
                        () -> "Error with quieted status change " + object,
                        throwable
                    )
            )
            .map(QuietedSohStatusChangeUpdateMaterializedViewProcessor.create(
                stationSohAnalysisManagerConfiguration.resolveDisplayParameters(),
                sohQuietAndUnacknowledgedCacheManager,
                latestStationSohByStation,
                latestCapabilitySohRollupByStationGroup,
                systemMessageEmitterProcessor.sink(),
                stationGroups
            ))
            .flatMap(Flux::fromIterable),
        this.kafkaSender,
        quietedMaterializedViewOutputTopic
    );

    quietedMaterializedViewPublisher.start();
  }

  /**
   * Does the reverse of start().
   */
  public synchronized void stop() {
    logger.info("Stopping services");
    if (active) {
      try {
        if (kafkaSender != null) {
          kafkaSender.close();
        }
      } finally {
        kafkaSender = null;
        active = false;
      }
    }
  }

  @Override
  public StationSohMonitoringUiClientParameters resolveStationSohMonitoringUiClientParameters(
      String placeholder) {
    return this.stationSohAnalysisManagerConfiguration.resolveDisplayParameters();
  }

  private static SenderOptions<String, String> senderOptions(SystemConfig systemConfig) {
    Map<String, Object> properties = new HashMap<>();
    properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
        systemConfig.getValue(KAFKA_BOOTSTRAP_SERVERS));
    properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
    properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
    properties.put(ProducerConfig.LINGER_MS_CONFIG, 0);
    // By default, a producer doesn't wait for an acknowledgement from kafka when it sends
    // a message to a topic. Setting it to "1" means that it will wait for at least one kafka
    // node to acknowledge. The safest is "all", but that makes sending a little slower.
    properties.put(ProducerConfig.ACKS_CONFIG, "0");
    return SenderOptions.create(properties);
  }

  @Override
  public SystemConfig getSystemConfig() {
    return this.systemConfig;
  }

  private void initializeFromOsd(Consumer<DataContainer> dataContainerConsumer) {

    StationSohMonitoringUiClientParameters parameters =
        resolveStationSohMonitoringUiClientParameters("");

    Objects.requireNonNull(parameters,
        "resolveStationSohMonitoringUiClientParameters returned null");

    StationSohMonitoringDefinition stationSohMonitoringDefinition =
        parameters.getStationSohControlConfiguration();

    SohRepositoryInterface sohRepositoryInterface =
        stationSohAnalysisManagerConfiguration.getSohRepositoryInterface();

    //
    // Calling Mono.just with the method calls will call them immediately and block, we want to
    // parallelize them. So using Mono.just(true).
    //

    var stationSohMono = Mono.just(true).map(
        b ->
            initializeCurrentStationSoh(
                stationSohMonitoringDefinition, sohRepositoryInterface
            )
    );

    var capabilityMono = Mono.just(true).map(
        b ->
            initializeCurrentCapabilitySohRollups(
                stationSohMonitoringDefinition, sohRepositoryInterface
            )
    );

    var quietedStatusChangeMono = Mono.just(true).map(
        b ->
            initializeQuietedSohStatusChanges(
                sohRepositoryInterface
            )
    );

    var unackStatusChangeMono = Mono.just(true).map(
        b ->
            initializeUnacknowledgedSohStatusChanges(
                stationSohMonitoringDefinition, sohRepositoryInterface
            )
    );

    Mono.zip(stationSohMono, capabilityMono, quietedStatusChangeMono, unackStatusChangeMono)
        .subscribe(
            tuple -> dataContainerConsumer.accept(new DataContainer(
                tuple.getT1(),
                tuple.getT2(),
                tuple.getT3(),
                tuple.getT4()
            ))
        );
  }

  /**
   * Populates the current {@link StationSoh} map for each station contained in the {@link
   * StationSohMonitoringDefinition}.
   */
  private Map<String, StationSoh> initializeCurrentStationSoh(
      StationSohMonitoringDefinition stationSohMonitoringDefinition,
      SohRepositoryInterface sohRepositoryInterface
  ) {

    var latestStationSohByStation = new ConcurrentHashMap<String, StationSoh>();

    Set<StationSohDefinition> stationSohDefinitions =
        stationSohMonitoringDefinition.getStationSohDefinitions();

    List<String> stationNames = stationSohDefinitions.stream()
        .map(StationSohDefinition::getStationName)
        .collect(Collectors.toList());

    Lists.partition(stationNames, (stationNames.size() / 4) + 1)
        .stream()
        .parallel()
        .filter(names -> !names.isEmpty())
        .peek(names -> logger.info("Retrieving latest StationSoh for {} stations", names.size()))
        .map(sohRepositoryInterface::retrieveByStationId)
        .flatMap(List::stream)
        .forEach(
            stationSoh -> latestStationSohByStation.put(stationSoh.getStationName(), stationSoh));
    logger.info("StationSoh DB retrieval  returned {} entries.", latestStationSohByStation.size());

    return latestStationSohByStation;
  }

  /**
   * Populates the most current {@link CapabilitySohRollup}s for the configured station groups.
   */
  private Map<String, CapabilitySohRollup> initializeCurrentCapabilitySohRollups(
      StationSohMonitoringDefinition stationSohMonitoringDefinition,
      SohRepositoryInterface sohRepositoryInterface) {

    var latestCapabilitySohRollupByStationGroup = new ConcurrentHashMap<String, CapabilitySohRollup>();

    Set<String> stationGroups = new HashSet<>(
        stationSohMonitoringDefinition.getDisplayedStationGroups());

    List<CapabilitySohRollup> capabilitySohRollups;
    if (stationGroups.isEmpty()) {
      logger.warn("No displayed station groups have been defined");
      capabilitySohRollups = Collections.emptyList();
    } else {
      logger.info("Retrieving CapabilitySohRollups for {} StationGroups", stationGroups.size());
      capabilitySohRollups = sohRepositoryInterface.retrieveLatestCapabilitySohRollupByStationGroup(
          stationGroups);
      logger.info("CapabilitySohRollup DB retrieval returned {} entries.",
          capabilitySohRollups.size());
    }

    for (CapabilitySohRollup capabilitySohRollup : capabilitySohRollups) {
      latestCapabilitySohRollupByStationGroup.put(capabilitySohRollup.getForStationGroup(),
          capabilitySohRollup);
    }

    return latestCapabilitySohRollupByStationGroup;
  }

  /**
   * Retrieves unacknowledged SOH status change events from the db.
   */
  private Set<UnacknowledgedSohStatusChange> initializeUnacknowledgedSohStatusChanges(
      StationSohMonitoringDefinition stationSohMonitoringDefinition,
      SohRepositoryInterface sohRepositoryInterface) {

    Set<StationSohDefinition> stationSohDefinitions =
        stationSohMonitoringDefinition.getStationSohDefinitions();

    List<String> stationNames = stationSohDefinitions != null ?
        stationSohDefinitions.stream()
            .map(StationSohDefinition::getStationName)
            .collect(Collectors.toList()) : Collections.emptyList();

    logger.info("Retrieving UnacknowledgedSohStatusChanges for {} stations", stationNames.size());
    var unacknowledgedSohStatusChanges = !stationNames.isEmpty() ?
        new HashSet<>(sohRepositoryInterface.retrieveUnacknowledgedSohStatusChanges(stationNames)) :
        Collections.<UnacknowledgedSohStatusChange>emptySet();

    logger.info("UnacknowledgedSohStatusChanges DB retrieval returned {} entries.",
        unacknowledgedSohStatusChanges.size());

    return unacknowledgedSohStatusChanges;
  }

  /**
   * Retrieves quieted SOH status changes for the current instant minus the specified duration.
   */
  private Set<QuietedSohStatusChange> initializeQuietedSohStatusChanges(
      SohRepositoryInterface sohRepositoryInterface
  ) {

    logger.info("Retrieving active QuietedSohStatusChanges");
    var quietedSohStatusChanges =
        new HashSet<>(sohRepositoryInterface.retrieveQuietedSohStatusChangesByTime(Instant.now()));
    logger.info("QuitedSohStatusChange DB retrieval returned {} entries.",
        quietedSohStatusChanges.size());

    return quietedSohStatusChanges;
  }

  /**
   * Get a value from the system config, returning a default value if not defined.
   */
  private static String getSystemConfigValue(SystemConfig systemConfig, String key,
      String defaultValue) {
    String value = defaultValue;
    try {
      value = systemConfig.getValue(key);
    } catch (Exception e) {
      logger.warn("{} is not defined in SystemConfig, using default value: {}",
          key, defaultValue);
    }
    return value;
  }
}
