package gms.core.performancemonitoring.ssam.control.processor;

import gms.core.performancemonitoring.ssam.control.config.StationSohMonitoringUiClientParameters;
import gms.core.performancemonitoring.uimaterializedview.QuietedSohStatusChangeUpdate;
import gms.core.performancemonitoring.uimaterializedview.SohQuietAndUnacknowledgedCacheManager;
import gms.core.performancemonitoring.uimaterializedview.UiStationAndStationGroups;
import gms.core.performancemonitoring.uimaterializedview.UiStationAndStationGroupGenerator;
import gms.shared.frameworks.osd.coi.signaldetection.StationGroup;
import gms.shared.frameworks.osd.coi.soh.CapabilitySohRollup;
import gms.shared.frameworks.osd.coi.soh.StationSoh;
import gms.shared.frameworks.osd.coi.systemmessages.SystemMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import reactor.core.publisher.FluxSink;

/**
 * Creates a materialized view containing the single station with quieted changes represented by the
 * provided QuietedSohStatusChangeUpdate object
 */
public interface QuietedSohStatusChangeUpdateMaterializedViewProcessor
    extends Function<QuietedSohStatusChangeUpdate, List<UiStationAndStationGroups>> {

  /**
   * Create the QuietedSohStatusChangeUpdateMaterializedViewProcessor that will generate the
   * materialized view, containing the one quieted station.
   *
   * @param stationSohConfig Display configuration
   * @param quietAndUnackListsManager SohQuietAndUnacknowledgedCacheManager containing unack/quiet
   * statuses
   * @param latestStationSohCache Cache containing the latest set of StationSohs
   * @param latestCapabilityRollupCache Cache containing the latest set of CapabilitySohRollups
   * @param systemMessageFluxSink System message map to populate with generated system messages
   * @param stationGroups Station groups to use to create the materialized view
   * @return A Function that given a single AcknowledgedSohStatusChange, returns a List containing a
   * single UiStationAndStationGroups object, which contains the latest Capability rollups as will
   * as the single StationSoh that was acknowledged
   */
  static QuietedSohStatusChangeUpdateMaterializedViewProcessor create(
      StationSohMonitoringUiClientParameters stationSohConfig,
      SohQuietAndUnacknowledgedCacheManager quietAndUnackListsManager,
      Map<String, StationSoh> latestStationSohCache,
      Map<String, CapabilitySohRollup> latestCapabilityRollupCache,
      FluxSink<SystemMessage> systemMessageFluxSink,
      List<StationGroup> stationGroups) {

    return quietedSohStatusChangeUpdate -> UiStationAndStationGroupGenerator
        .generateUiStationAndStationGroups(
            List.of(
                latestStationSohCache.get(
                    quietedSohStatusChangeUpdate.getStationName()
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
