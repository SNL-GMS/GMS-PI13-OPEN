package gms.core.performancemonitoring.uimaterializedview;

import static gms.core.performancemonitoring.uimaterializedview.utils.MaterializedViewTestFixtures.STATION_SOH_PARAMETERS;
import static gms.shared.frameworks.osd.coi.SohTestFixtures.MARGINAL_STATION_GROUP_AND_STATION_CAPABILITY_ROLLUP;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import gms.shared.frameworks.osd.coi.systemmessages.SystemMessage;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.EmitterProcessor;

public class UiStationGroupGeneratorTest {
  @BeforeAll
  static void initializeContributingMap() {
    // Initialize the StationSohContributingUtility before creating a channel soh
    StationSohContributingUtility.getInstance().initialize(STATION_SOH_PARAMETERS);
  }

  @Test
  void testGeneratorStationGroups() {
    EmitterProcessor<SystemMessage> systemMessageEmitterProcessor = EmitterProcessor.create();
    List<String> stationGroupNames =
        STATION_SOH_PARAMETERS.getStationSohControlConfiguration().getDisplayedStationGroups();
    List<UiStationGroupSoh> actual = assertDoesNotThrow(() ->
        UIStationGroupGenerator.buildSohStationGroups(
            List.of(MARGINAL_STATION_GROUP_AND_STATION_CAPABILITY_ROLLUP),
            stationGroupNames,
            systemMessageEmitterProcessor.sink()
        ));
    Assertions.assertEquals(actual.size(), stationGroupNames.size());
  }
}
