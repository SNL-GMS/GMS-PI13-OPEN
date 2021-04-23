package gms.core.performancemonitoring.soh.control;

import gms.core.performancemonitoring.soh.control.configuration.DurationSohMonitorStatusThresholdDefinition;
import java.io.IOException;
import java.time.Duration;
import org.junit.jupiter.api.Test;

public class DurationSohMonitorStatusThresholdDefinitionTests {

  @Test
  void testSerialization() throws IOException {

    TestUtilities.testSerialization(
        DurationSohMonitorStatusThresholdDefinition.create(
            Duration.ofDays(1),
            Duration.ofMillis(122)
        ),
        DurationSohMonitorStatusThresholdDefinition.class
    );
  }
}

