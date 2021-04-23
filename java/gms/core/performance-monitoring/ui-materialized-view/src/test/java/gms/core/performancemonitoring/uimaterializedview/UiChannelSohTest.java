package gms.core.performancemonitoring.uimaterializedview;

import static gms.core.performancemonitoring.uimaterializedview.utils.MaterializedViewTestFixtures.BAD_MISSING_LATENCY_SEAL_UI_CHANNEL_SOH;
import static gms.core.performancemonitoring.uimaterializedview.utils.MaterializedViewTestFixtures.CHANNEL_DEFINITION;
import static gms.core.performancemonitoring.uimaterializedview.utils.MaterializedViewTestFixtures.CHANNEL_SOH;
import static gms.core.performancemonitoring.uimaterializedview.utils.MaterializedViewTestFixtures.QUIETED_CHANGE_1;
import static gms.core.performancemonitoring.uimaterializedview.utils.MaterializedViewTestFixtures.STATION_SOH_PARAMETERS;
import static gms.core.performancemonitoring.uimaterializedview.utils.MaterializedViewTestFixtures.UNACK_CHANGE_1;
import static gms.shared.frameworks.osd.coi.SohTestFixtures.BAD_LAG_MISSING_CHANNEL_SOH;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.google.common.base.Functions;
import gms.core.performancemonitoring.soh.control.configuration.ChannelSohDefinition;
import gms.shared.frameworks.osd.coi.soh.ChannelSoh;
import gms.shared.frameworks.osd.coi.soh.SohMonitorType;
import gms.shared.frameworks.osd.coi.soh.quieting.UnacknowledgedSohStatusChange;
import gms.shared.frameworks.osd.coi.util.TestUtilities;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class UiChannelSohTest {

  @Test
  void testSerialization() throws IOException {
    TestUtilities.testSerialization(CHANNEL_SOH, UiChannelSoh.class);
  }
}