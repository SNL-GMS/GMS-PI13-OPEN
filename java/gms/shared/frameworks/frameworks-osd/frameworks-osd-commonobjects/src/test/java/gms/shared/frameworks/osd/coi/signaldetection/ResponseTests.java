package gms.shared.frameworks.osd.coi.signaldetection;

import static org.junit.jupiter.api.Assertions.assertEquals;

import gms.shared.frameworks.osd.coi.util.TestUtilities;
import org.junit.jupiter.api.Test;

public class ResponseTests {

  @Test
  public void testSerialization() throws Exception {
    TestUtilities.testSerialization(SignalDetectionTestFixtures.response,
        Response.class);
  }

  @Test
  public void testResponseCreate() {
    Response response = Response.from(
        SignalDetectionTestFixtures.channelName,
        SignalDetectionTestFixtures.calibration,
        SignalDetectionTestFixtures.fapResponse);

    assertEquals(SignalDetectionTestFixtures.channelName, response.getChannelName());
    assertEquals(SignalDetectionTestFixtures.calibration, response.getCalibration());
    assertEquals(SignalDetectionTestFixtures.fapResponse, response.getFapResponse().orElse(null));
  }

}
