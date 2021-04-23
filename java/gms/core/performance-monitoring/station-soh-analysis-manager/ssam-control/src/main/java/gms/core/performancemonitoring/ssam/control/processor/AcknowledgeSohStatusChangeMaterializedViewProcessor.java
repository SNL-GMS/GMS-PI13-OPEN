package gms.core.performancemonitoring.ssam.control.processor;

import gms.core.performancemonitoring.ssam.control.config.StationSohMonitoringUiClientParameters;
import gms.core.performancemonitoring.uimaterializedview.AcknowledgedSohStatusChange;
import gms.core.performancemonitoring.uimaterializedview.SohQuietAndUnacknowledgedCacheManager;
import gms.core.performancemonitoring.uimaterializedview.UiStationAndStationGroupGenerator;
import gms.core.performancemonitoring.uimaterializedview.UiStationAndStationGroups;
import gms.shared.frameworks.osd.coi.signaldetection.StationGroup;
import gms.shared.frameworks.osd.coi.soh.CapabilitySohRollup;
import gms.shared.frameworks.osd.coi.soh.StationSoh;

import gms.shared.frameworks.osd.coi.systemmessages.SystemMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import reactor.core.publisher.FluxSink;

/**
 * Creates a materialized view containing the single station with acknowledged changes
 * represented by the provided AcknowledgedSohStatusChange object
 */
public interface AcknowledgeSohStatusChangeMaterializedViewProcessor
    extends Function<AcknowledgedSohStatusChange, List<UiStationAndStationGroups>> {

  /**
   * Create the AcknowledgeSohStatusChangeMaterializedViewProcessor that will generate
   * the materialized view, containing the one acknowledged station.
   *
   * @param stationSohConfig Display configuration
   * @param quietAndUnackListsManager SohQuietAndUnacknowledgedCacheManager containing unack/quiet
   * statuses
   * @param latestStationSohCache Cache containing the latest set of StationSohs
   * @param latestCapabilityRollupCache Cache containing the latest set of CapabilitySohRollups
   * @param systemMessageFluxSink System message map to populate with generated system messages
   * @param stationGroups Station groups to use to create the materialized view
   * @return A Function that given a single AcknowledgedSohStatusChange, returns a List containing
   * a single UiStationAndStationGroups object, which contains the
   * latest Capability rollups as well as the single StationSoh that was acknowledged.
   */
  static AcknowledgeSohStatusChangeMaterializedViewProcessor create(
      StationSohMonitoringUiClientParameters stationSohConfig,
      SohQuietAndUnacknowledgedCacheManager quietAndUnackListsManager,
      Map<String, StationSoh> latestStationSohCache,
      Map<String, CapabilitySohRollup> latestCapabilityRollupCache,
      FluxSink<SystemMessage> systemMessageFluxSink,
      List<StationGroup> stationGroups) {

    return ack -> UiStationAndStationGroupGenerator.generateUiStationAndStationGroups(
        List.of(
            Optional.ofNullable(latestStationSohCache.get(
                ack.getAcknowledgedStation()
            )).orElseThrow(
                () -> new IllegalStateException("Tried to acknowledge a station not in the cache!")
            )
        ),
        quietAndUnackListsManager
            .getUnacknowledgedList(),
        quietAndUnackListsManager
            .getQuietedSohStatusChanges(),
        new ArrayList<>(latestCapabilityRollupCache.values()),
        stationSohConfig,
        stationGroups,
        true,
        systemMessageFluxSink
    );
  }

}
