package gms.core.performancemonitoring.ssam.control.processor;

import gms.core.performancemonitoring.ssam.control.config.StationSohMonitoringUiClientParameters;
import gms.core.performancemonitoring.uimaterializedview.SohQuietAndUnacknowledgedCacheManager;
import gms.core.performancemonitoring.uimaterializedview.UiStationAndStationGroupGenerator;
import gms.core.performancemonitoring.uimaterializedview.UiStationAndStationGroups;
import gms.shared.frameworks.osd.coi.signaldetection.StationGroup;
import gms.shared.frameworks.osd.coi.soh.CapabilitySohRollup;
import gms.shared.frameworks.osd.coi.soh.StationSoh;
import gms.shared.frameworks.osd.coi.systemmessages.SystemMessage;
import java.util.Collection;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import reactor.core.publisher.FluxSink;

public class MaterializedViewProcessor implements
    Function<List<Pair<CapabilitySohRollup, List<StationSoh>>>, List<UiStationAndStationGroups>> {

  private final SohQuietAndUnacknowledgedCacheManager quietAndUnackListsManager;
  private final StationSohMonitoringUiClientParameters stationSohConfig;
  private final List<StationGroup> stationGroups;
  private final FluxSink<SystemMessage> systemMessageFluxSink;

  MaterializedViewProcessor(SohQuietAndUnacknowledgedCacheManager quietAndUnackListsManager,
      StationSohMonitoringUiClientParameters stationSohConfig, List<StationGroup> stationGroups,
      FluxSink<SystemMessage> systemMessageFluxSink) {
    this.quietAndUnackListsManager = quietAndUnackListsManager;
    this.stationSohConfig = stationSohConfig;
    this.stationGroups = stationGroups;
    this.systemMessageFluxSink = systemMessageFluxSink;
  }

  public static MaterializedViewProcessor create(
      SohQuietAndUnacknowledgedCacheManager quietAndUnackListsManager,
      StationSohMonitoringUiClientParameters stationSohConfig,
      List<StationGroup> stationGroups,
      FluxSink<SystemMessage> systemMessageFluxSink) {

    Objects.requireNonNull(quietAndUnackListsManager);
    Objects.requireNonNull(stationSohConfig);
    Objects.requireNonNull(stationGroups);
    Objects.requireNonNull(systemMessageFluxSink);

    return new MaterializedViewProcessor(quietAndUnackListsManager, stationSohConfig,
        stationGroups,systemMessageFluxSink);
  }

  @Override
  public  List<UiStationAndStationGroups> apply(
      List<Pair<CapabilitySohRollup, List<StationSoh>>> pairs) {

    var unacknowledgedStatusChanges = quietAndUnackListsManager
        .getUnacknowledgedList();

    var quietedSohStatusChanges = quietAndUnackListsManager
        .getQuietedSohStatusChanges();

    // TRUE indicates the UI should immediately redraw the SOH UI.
    // FALSE indicates the UI will batch the List of UiStationAndStationGroups before redraw.
    final var IS_UPDATE = false;

    var stationSohList = pairs.stream()
        .map(Pair::getValue)
        .flatMap(Collection::stream)
        .distinct()
        .collect(Collectors.toList());

    var latestCapabilitySohRollup = pairs.stream()
        .map(Pair::getKey)
        .collect(Collectors.toList());

    return UiStationAndStationGroupGenerator.generateUiStationAndStationGroups(
        stationSohList,
        unacknowledgedStatusChanges,
        quietedSohStatusChanges,
        latestCapabilitySohRollup,
        stationSohConfig,
        stationGroups,
        IS_UPDATE,
        systemMessageFluxSink
    );

  }
}
