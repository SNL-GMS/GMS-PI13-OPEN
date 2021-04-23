package gms.shared.frameworks.osd.coi.stationreference;

import gms.shared.frameworks.osd.coi.util.TestUtilities;
import org.junit.jupiter.api.Test;

public class ReferenceDigitizerTest {

  @Test
  public void testSerialization() throws Exception {
    TestUtilities
        .testSerialization(StationReferenceTestFixtures.DIGITIZER, ReferenceDigitizer.class);
  }
}
