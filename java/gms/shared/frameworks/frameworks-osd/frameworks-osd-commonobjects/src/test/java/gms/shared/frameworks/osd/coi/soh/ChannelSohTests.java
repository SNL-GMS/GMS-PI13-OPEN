package gms.shared.frameworks.osd.coi.soh;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.util.Set;
import static gms.shared.frameworks.osd.coi.SohTestFixtures.BAD_LAG_MISSING_CHANNEL_SOH;
import static gms.shared.frameworks.osd.coi.SohTestFixtures.BAD_SEAL_BROKEN_SOH_MONITOR_VALUE_AND_STATUS;
import static gms.shared.frameworks.osd.coi.SohTestFixtures.MARGINAL_LAG_SOH_MONITOR_VALUE_AND_STATUS;
import static gms.shared.frameworks.osd.coi.SohTestFixtures.MARGINAL_MISSING_SOH_MONITOR_VALUE_AND_STATUS;

class ChannelSohTests {

  @Test
  void testSerialization() throws IOException {

    TestUtilities.testSerialization(
        BAD_LAG_MISSING_CHANNEL_SOH,
        ChannelSoh.class
    );
  }
}
