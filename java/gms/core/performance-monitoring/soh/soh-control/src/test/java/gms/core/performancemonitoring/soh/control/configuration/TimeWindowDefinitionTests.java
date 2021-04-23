package gms.core.performancemonitoring.soh.control.configuration;

import gms.core.performancemonitoring.soh.control.TestUtilities;
import gms.core.performancemonitoring.soh.control.configuration.TimeWindowDefinition;
import java.io.IOException;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class TimeWindowDefinitionTests {

  @Test
  void testSerialization() throws IOException {

    TestUtilities.testSerialization(
        TimeWindowDefinition.create(
            Duration.ofDays(2),
            Duration.ofHours(2)
        ),
        TimeWindowDefinition.class
    );
  }
}
