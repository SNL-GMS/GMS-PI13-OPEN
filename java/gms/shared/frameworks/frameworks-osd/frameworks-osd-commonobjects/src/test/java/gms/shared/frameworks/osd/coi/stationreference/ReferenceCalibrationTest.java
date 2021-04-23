package gms.shared.frameworks.osd.coi.stationreference;


import gms.shared.frameworks.osd.coi.util.TestUtilities;
import org.junit.jupiter.api.Test;

public class ReferenceCalibrationTest {

  @Test
  public void testSerialization() throws Exception {
    TestUtilities
        .testSerialization(StationReferenceTestFixtures.REF_CALIBRATION_BHE_V_1,
            ReferenceCalibration.class);
  }

}
