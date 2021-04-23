package gms.core.performancemonitoring.soh.control;

import gms.core.performancemonitoring.soh.control.configuration.CapabilitySohRollupDefinition;
import gms.core.performancemonitoring.soh.control.configuration.SohControlDefinition;
import gms.core.performancemonitoring.soh.control.configuration.StationGroupNamesConfigurationOption;
import gms.core.performancemonitoring.soh.control.configuration.StationSohDefinition;
import gms.core.performancemonitoring.soh.control.configuration.StationSohMonitoringDefinition;
import gms.core.performancemonitoring.soh.control.configuration.TimeWindowDefinition;
import gms.shared.frameworks.configuration.ConfigurationRepository;
import gms.shared.frameworks.configuration.Selector;
import gms.shared.frameworks.configuration.repository.client.ConfigurationConsumerUtility;
import gms.shared.frameworks.osd.api.OsdRepositoryInterface;
import gms.shared.frameworks.osd.api.station.StationGroupRepositoryInterface;
import gms.shared.frameworks.osd.coi.signaldetection.Station;
import gms.shared.frameworks.osd.coi.signaldetection.StationGroup;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

/**
 * Contains configuration values used by {@link StationSohControl}.  Includes {@link
 * StationSohMonitoringDefinition} that is required by {@link StationSohControl#monitor(Set)} to
 * call {@link StationSohCalculationUtility#buildStationSohFlux(Set, Set, Instant, ChannelSohCalculationUtility)}
 */
public class StationSohControlConfiguration {

  // We must use apache log4j to have custom log levels
  private static final Logger logger = LogManager.getLogger(StationSohControlConfiguration.class);

  private static final Level SOH_TIMING = Level.getLevel("SOH_TIMING");

  private static final boolean SOH_TIMING_ENABLED = logger.isEnabled(SOH_TIMING);

  private static final String STATION_SOH_PREFIX = "soh-control";

  private static final String STATION_GROUP_NAME_SELECTOR_KEY = "StationGroupName";

  private static final String STATION_NAME_SELECTOR_KEY = "StationName";

  private static final String CHANNEL_NAME_SELECTOR_KEY = "ChannelName";

  private static final String NULL_CONFIGURATION_CONSUMER_UTILITY = "Null configurationConsumerUtility";

  private Flux<ConfigurationPair> configurationPairFlux;

  private ConfigurationPair initialConfigurationPair;

  private final ConfigurationConsumerUtility configurationConsumerUtility;

  private final CapabilityRollupConfigurationUtility capabilityRollupConfigurationUtility;

  private final Collection<StationGroup> stationGroups;

  private final Collection<Station> stations;

  private Disposable configurationPairFluxDisposable;

  private Duration refreshInterval;

  private boolean isDirty = false;

  public static class ConfigurationPair {

    private final StationSohMonitoringDefinition stationSohMonitoringDefinition;

    private final SohControlDefinition sohControlDefinition;

    public ConfigurationPair(
        StationSohMonitoringDefinition stationSohMonitoringDefinition,
        SohControlDefinition sohControlDefinition) {
      this.stationSohMonitoringDefinition = stationSohMonitoringDefinition;
      this.sohControlDefinition = sohControlDefinition;
    }

    public StationSohMonitoringDefinition getStationSohMonitoringDefinition() {
      return stationSohMonitoringDefinition;
    }

    public SohControlDefinition getSohControlDefinition() {
      return sohControlDefinition;
    }
  }

  /**
   * Instantiates and returns a new StationSohControlConfiguration.  The returned
   * StationSohControlConfiguration contains configuration loaded using the provided {@link
   * ConfigurationConsumerUtility} and {@link Station}s that are part of {@link StationGroup}s
   * loaded from the provided {@link OsdRepositoryInterface}.
   *
   * @param configurationRepository Used to retrieve configuration values from the OSDn.
   * @param stationGroupRepositoryInterface Used to retrieve StationGroups from the OSD.
   * @return New StationSohControlConfiguration.
   */
  public static StationSohControlConfiguration create(
      ConfigurationRepository configurationRepository,
      StationGroupRepositoryInterface stationGroupRepositoryInterface) {

    return new StationSohControlConfiguration(
        createConfigurationConsumerUtility(
            configurationRepository
        ),
        stationGroupRepositoryInterface,
        false
    );
  }

  public static StationSohControlConfiguration create(
      ConfigurationConsumerUtility configurationConsumerUtility,
      StationGroupRepositoryInterface stationGroupRepositoryInterface) {

    return new StationSohControlConfiguration(
        configurationConsumerUtility,
        stationGroupRepositoryInterface,
        true
    );
  }

  /**
   * Creates a new {@link ConfigurationConsumerUtility} for holding configurations for this class.
   *
   * @param configurationRepository Used to load configuration values from processing
   * configuration.
   * @return new {@link ConfigurationConsumerUtility}
   */
  private static ConfigurationConsumerUtility createConfigurationConsumerUtility(
      ConfigurationRepository configurationRepository) {
    Objects.requireNonNull(configurationRepository, "Null configurationRepository");
    return ConfigurationConsumerUtility.builder(configurationRepository)
        .configurationNamePrefixes(Set.of(STATION_SOH_PREFIX))
        .build();
  }

  /**
   * Instantiates a new StationSohControlConfiguration that contains the provided {@link
   * StationSohMonitoringDefinition}.
   *
   * @param configurationConsumerUtility Used to obtain configuration parameters.
   * @param stationGroupRepositoryInterface Used to obtain {@link StationGroup}s from the OSD.
   */
  private StationSohControlConfiguration(
      ConfigurationConsumerUtility configurationConsumerUtility,
      StationGroupRepositoryInterface stationGroupRepositoryInterface,
      boolean doRefresh
  ) {

    // Start timing "CONFIG TIMING: Querying station groups"
    var startMs = System.currentTimeMillis();

    this.stationGroups = resolveStationGroups(
        configurationConsumerUtility,
        stationGroupRepositoryInterface
    );

    this.stations = stationGroups.stream()
        .flatMap(staGroup -> staGroup.getStations().stream()).collect(
            Collectors.toSet());

    if (SOH_TIMING_ENABLED) {
      logger.log(
          SOH_TIMING,
          "CONFIG TIMING: Querying station groups took {} ms",
          System.currentTimeMillis() - startMs
      );
    }

    this.configurationConsumerUtility = configurationConsumerUtility;

    this.capabilityRollupConfigurationUtility = new CapabilityRollupConfigurationUtility(
        STATION_GROUP_NAME_SELECTOR_KEY,
        STATION_NAME_SELECTOR_KEY,
        CHANNEL_NAME_SELECTOR_KEY,
        STATION_SOH_PREFIX,
        configurationConsumerUtility,
        stationGroups
    );

    this.initialConfigurationPair = resolveConfigurationPair();

    this.refreshInterval = this.initialConfigurationPair.sohControlDefinition
        .getReprocessingPeriod();

    if (doRefresh) {
      this.configurationPairFlux = resolveConfigurationPairFlux();
    } else {
      this.configurationPairFlux = Flux.empty();
    }
  }

  public ConfigurationPair getInitialConfigurationPair() {

    if (initialConfigurationPair != null) {

      var pair = initialConfigurationPair;
      initialConfigurationPair = null;
      return pair;

    } else {
      throw new IllegalStateException(
          "StationSohControlConfiguration: getInitialConfigurationPair should only be called once"
      );
    }
  }

  public void subscribeToInterval(Consumer<ConfigurationPair> subscriber) {

    Consumer<ConfigurationPair> configurationPairSubscriber = configurationPair -> {

      subscriber.accept(configurationPair);

      //
      // If the reprocessing period itself changed, we need a new Flux
      // that emits at that new interval. We then need to subscribe to that new
      // Flux.
      //
      if (this.isDirty) {
        this.isDirty = false;
        this.configurationPairFluxDisposable.dispose();
        this.configurationPairFlux = resolveConfigurationPairFlux();
        subscribeToInterval(subscriber);
      }
    };

    this.configurationPairFluxDisposable =
        this.configurationPairFlux.subscribe(
            configurationPairSubscriber
        );

  }

  /**
   * Unsubscribe from (dispose of) the interval flux. For the moment, this is only used
   * to stop the reactive test. It may be worth seeing if its worth disposing of this in the future.
   * For now, it is assumed that it will last the life of the service.
   */
  void unsubscribe() {
    if (!this.configurationPairFluxDisposable.isDisposed()) {
      this.configurationPairFluxDisposable.dispose();
    }
  }

  /**
   * Determine the largest cache expiration duration from a set of {@link Duration}s contained on
   * the set of {@link StationSohDefinition}s.
   *
   * @param stationSohMonitoringDefinition {@link StationSohMonitoringDefinition} used to determine
   * the cacheExpirationDuration value.
   */
  private static Duration calculateCacheExpirationDuration(
      StationSohMonitoringDefinition stationSohMonitoringDefinition) {

    // Get the set of calculation intervals
    Set<Duration> calculationIntervals =
        stationSohMonitoringDefinition.getStationSohDefinitions().stream()
            .flatMap(
                stationSohDefinition -> stationSohDefinition.getTimeWindowBySohMonitorType()
                    .values().stream())
            .map(TimeWindowDefinition::getCalculationInterval).collect(Collectors.toSet());

    // Get the set of back off durations
    Set<Duration> backOffDurations =
        stationSohMonitoringDefinition.getStationSohDefinitions().stream()
            .flatMap(
                stationSohDefinition -> stationSohDefinition.getTimeWindowBySohMonitorType()
                    .values().stream())
            .map(TimeWindowDefinition::getBackOffDuration).collect(Collectors.toSet());

    return backOffDurations.stream()
        //
        // Find the max back-off duration
        //
        .max(Comparator.naturalOrder())
        //
        // "map" the Optional<Duration> to a calculation that adds it to the max calculation interval
        //
        .map(backOffDuration -> {
              //
              // Find the largest calculation interval.
              //
              Duration maxCalculationInterval = calculationIntervals.stream()
                  .max(Comparator.naturalOrder())
                  .orElseThrow(() -> new IllegalArgumentException(
                      "cacheExpirationDuration cannot be calculated - max calculationInterval was not found"));

              Duration cacheExpirationDuration =
                  backOffDuration.plus(maxCalculationInterval);

              cacheExpirationDuration = cacheExpirationDuration.plus(
                  Duration.of(
                      cacheExpirationDuration.toMillis() / 10,
                      ChronoUnit.MILLIS
                  )
              );

              if (logger.isDebugEnabled()) {
                logger.debug(
                    "cacheExpirationDuration of {} calculated from a calculationIntervals {} and backOffDurations {}",
                    cacheExpirationDuration, calculationIntervals, backOffDurations);
              }

              return cacheExpirationDuration;
            }
        ).orElseThrow(() -> new IllegalArgumentException(
            "cacheExpirationDuration cannot be calculated - max backOffDuration was not found"
        ));
  }

  private ConfigurationPair resolveConfigurationPair() {

    // Start timing "CONFIG TIMING: Creating CapabilityRollupConfigurationUtility"
    var startMs = System.currentTimeMillis();

    if (SOH_TIMING_ENABLED) {
      logger.log(
          SOH_TIMING,
          "CONFIG TIMING: Creating CapabilityRollupConfigurationUtility took {} ms",
          System.currentTimeMillis() - startMs
      );
    }

    // Start timing "CONFIG TIMING: Resolving StationSohDefinitions"
    startMs = System.currentTimeMillis();

    Set<StationSohDefinition> stationSohDefinitions =
        StationSohControlUtility.resolveStationSohDefinitions(
            configurationConsumerUtility,
            stations
        );

    if (SOH_TIMING_ENABLED) {
      logger.log(
          SOH_TIMING,
          "CONFIG TIMING: Resolving {} StationSohDefinitions took {} ms",
          stationSohDefinitions.size(),
          System.currentTimeMillis() - startMs
      );
    }

    // Start timing "CONFIG TIMING: Resolving CapabilityRollupDefinitions"
    startMs = System.currentTimeMillis();

    Set<CapabilitySohRollupDefinition> capabilitySohRollupDefinitions =
        capabilityRollupConfigurationUtility.resolveCapabilitySohRollupDefinitions();
    
    if (SOH_TIMING_ENABLED) {
      logger.log(
          SOH_TIMING,
          "CONFIG TIMING: Resolving {} CapabilityRollupDefinitions took {} ms",
          capabilitySohRollupDefinitions.size(),
          System.currentTimeMillis() - startMs
      );
    }

    var stationSohMonitoringDefinition = StationSohMonitoringDefinition.create(
        capabilityRollupConfigurationUtility.resolveRollupStationSohTimeTolerance(),
        stationGroups.stream().map(StationGroup::getName).collect(Collectors.toSet()),
        stationSohDefinitions,
        capabilitySohRollupDefinitions
    );

    // Start timing "CONFIG TIMING: Calculating the cache expiration"
    startMs = System.currentTimeMillis();

    var cacheExpirationDuration = calculateCacheExpirationDuration(stationSohMonitoringDefinition);

    if (SOH_TIMING_ENABLED) {
      logger.log(
          SOH_TIMING,
          "CONFIG TIMING: Calculating the cache expiration took {} ms",
          System.currentTimeMillis() - startMs
      );
    }

    return new ConfigurationPair(
        stationSohMonitoringDefinition,
        SohControlDefinition.create(
            Duration.parse(String.valueOf(
                configurationConsumerUtility.resolve(
                    STATION_SOH_PREFIX,
                    List.of()
                ).get("reprocessingPeriod"))),
            cacheExpirationDuration
        )
    );
  }

  private Flux<ConfigurationPair> resolveConfigurationPairFlux() {

    return intervalOrOnce(refreshInterval)
        .map(
            dummy -> resolveConfigurationPair()
        ).doOnNext(
            configurationPair -> {
              //
              // If the reprocessing period changed, we need to signal this.
              //
              if (configurationPair.sohControlDefinition
                  .getReprocessingPeriod().compareTo(refreshInterval) != 0) {
                this.refreshInterval = configurationPair
                    .sohControlDefinition.getReprocessingPeriod();
                this.isDirty = true;
              }

            }
        );
  }

  private static Flux<Long> intervalOrOnce(Duration repeatInterval) {

    if (repeatInterval.compareTo(Duration.ZERO) == 0) {
      return Flux.just(0L);
    } else {
      return Flux.interval(repeatInterval);
    }
  }


  /**
   * Resolves a {@link Set} of {@link Station}s for which to resolve {@link StationSohDefinition}s.
   * The returned Set contains all Stations in the {@link StationGroup}s whose names are loaded from
   * processing configuration.
   *
   * @return Set of Stations for which to resolve StationSohDefinitions.
   */
  private static List<StationGroup> resolveStationGroups(
      ConfigurationConsumerUtility configurationConsumerUtility,
      StationGroupRepositoryInterface stationGroupRepositoryInterface) {

    final StationGroupNamesConfigurationOption stationGroupNamesConfigurationOption =
        resolveStationGroupNamesConfigurationOption(configurationConsumerUtility);

    return stationGroupRepositoryInterface
        .retrieveStationGroups(stationGroupNamesConfigurationOption.getStationGroupNames());

  }


  /**
   * Resolves the default {@link StationGroupNamesConfigurationOption} from processing
   * configuration.
   *
   * @return The default StationGroupNamesConfigurationOption from processing configuration.
   */
  private static StationGroupNamesConfigurationOption resolveStationGroupNamesConfigurationOption(
      ConfigurationConsumerUtility configurationConsumerUtility) {

    Objects.requireNonNull(configurationConsumerUtility, NULL_CONFIGURATION_CONSUMER_UTILITY);

    final String stationGroupNamesConfigurationName =
        STATION_SOH_PREFIX + ".station-group-names";

    final List<Selector> selectors = new ArrayList<>();

    return configurationConsumerUtility.resolve(
        stationGroupNamesConfigurationName,
        selectors,
        StationGroupNamesConfigurationOption.class
    );
  }
}