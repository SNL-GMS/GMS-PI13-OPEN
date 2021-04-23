package gms.core.performancemonitoring.soh.control;

import gms.core.performancemonitoring.soh.control.capabilityrollup.CapabilityRollupUtility;
import gms.core.performancemonitoring.soh.control.configuration.CapabilitySohRollupDefinition;
import gms.core.performancemonitoring.soh.control.configuration.StationSohDefinition;
import gms.shared.frameworks.osd.coi.channel.dataacquisitionstatus.AcquiredStationSohExtract;
import gms.shared.frameworks.osd.coi.soh.CapabilitySohRollup;
import gms.shared.frameworks.osd.coi.soh.StationSoh;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.Flux;

/**
 * Class that builds two fluxes: the StationSoh Flux (with "pure" station rollups) and the
 * CapabilitySohRollup Flux (with "capability" station group rollups). The Fluxes are built
 * using the "pure" and "capability" rollup utilities, respectively.
 */
class RollupFluxBuilder {

  private static final Logger logger = LogManager.getLogger(RollupFluxBuilder.class);

  private final ConnectableFlux<CapabilitySohRollup> capabilitySohRollupFlux;

  private final ConnectableFlux<StationSoh> stationSohFlux;

  /**
   * Construct a new RollupFluxBuilder
   *
   * @param acquiredStationSohExtractFlux The Flux of AcquiredStationSohExtract - the input SOH data
   * @param stationSohDefinitionSet Set of configurations specifying "pure" rollup behavior
   * @param capabilitySohRollupDefinitionSet Set of configurations specifying "capability" rollup
   * behavior
   */
  RollupFluxBuilder(
      Set<AcquiredStationSohExtract> acquiredStationSohExtractSet,
      Set<StationSohDefinition> stationSohDefinitionSet,
      Set<CapabilitySohRollupDefinition> capabilitySohRollupDefinitionSet,
      Duration rollupStationSohTimeTolerance,
      AcquiredSampleTimesByChannel acquiredSampleTimesByChannel) {

    Instant now = Instant.now();

    this.stationSohFlux = buildStationSohFlux(
        acquiredStationSohExtractSet,
        stationSohDefinitionSet,
        now,
        acquiredSampleTimesByChannel
    );

    this.capabilitySohRollupFlux = buildCapabilityRollupFlux(
        this.stationSohFlux,
        capabilitySohRollupDefinitionSet,
        rollupStationSohTimeTolerance,
        now
    );

    //
    // Connect all of our fluxes to their source
    //
    this.stationSohFlux.connect();
    this.capabilitySohRollupFlux.connect();

  }

  /**
   * @return Flux of CapabilitySohRollup
   */
  Flux<CapabilitySohRollup> getCapabilitySohRollupFlux() {

    return capabilitySohRollupFlux;
  }

  /**
   * @return Flux of StationSoh
   */
  Flux<StationSoh> getStationSohFlux() {

    return stationSohFlux;
  }

  /**
   * Build the Flux of CapabilitySohRollup
   *
   * @param stationSohFlux Flux of StationSoh used to build capability rollups
   * @param capabilitySohRollupDefinitionSet Set of configs specifying capability rollup behavior
   * @return Flux of CapabilitySohRollup
   */
  private static ConnectableFlux<CapabilitySohRollup> buildCapabilityRollupFlux(
      Flux<StationSoh> stationSohFlux,
      Set<CapabilitySohRollupDefinition> capabilitySohRollupDefinitionSet,
      Duration rollupStationSohTimeTolerance,
      Instant now
  ) {

    return CapabilityRollupUtility.buildCapabilitySohRollupFlux
        (
            capabilitySohRollupDefinitionSet,
            stationSohFlux.filter(stationSoh ->
                //
                // TODO: This comparison is just comparing now to itself - the now that is passed
                //  in above is the same now that was passed into buildStationSohFlux, and that
                //  calculation just sets the time of all StationSoh objects to now. So this
                //  comparison will be negative.
                //  Need to understand what meaning rollupStationSohTimeTolerance is supposed to
                //  have.
                now.minus(rollupStationSohTimeTolerance).compareTo(stationSoh.getTime())
                    <= 0)
        )
        //
        // Cache results so that they can be sent to multiple subscribers
        //
        .replay();
  }

  /**
   * Build the Flux of StationSoh
   *
   * @param acquiredStationSohExtractFlux Flux of AcquiredStationSohExtract used to build the "pure"
   * rollups
   * @param stationSohDefinitionSet Set of configs specifying pure rollup behavior
   * @return Flux of StationSoh
   */
  private static ConnectableFlux<StationSoh> buildStationSohFlux(
      Set<AcquiredStationSohExtract> acquiredStationSohExtractSet,
      Set<StationSohDefinition> stationSohDefinitionSet,
      Instant now,
      AcquiredSampleTimesByChannel acquiredSampleTimesByChannel
  ) {

    AtomicInteger restartCount = new AtomicInteger(0);

    return StationSohCalculationUtility.buildStationSohFlux(
        acquiredStationSohExtractSet,
        stationSohDefinitionSet,
        now,
        acquiredSampleTimesByChannel)
        .doFirst(() ->
            logger.debug(
                "RollupFluxBuilder: stationSohFlux restarting for {}th time",
                restartCount.incrementAndGet()
            )
        )
        //
        // Cache results so that they can be sent to multiple subscribers
        //
        .replay();

  }
}
