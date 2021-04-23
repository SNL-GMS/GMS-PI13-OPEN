package gms.shared.frameworks.osd.coi.signaldetection;

import gms.shared.frameworks.osd.coi.util.TestUtilities;
import org.junit.jupiter.api.Test;

public class AmplitudePhaseResponseTest {

  @Test
  public void testSerialization() throws Exception {
    TestUtilities.testSerialization(SignalDetectionTestFixtures.amplitudePhaseResponse,
        AmplitudePhaseResponse.class);

  }
}
