package gms.shared.frameworks.osd.coi.soh;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

public class CapabilitySohRollupTests {

  @Test
  void testSerialization() throws IOException {

    CapabilitySohRollup capabilitySohRollup = CapabilitySohRollup.create(
        UUID.randomUUID(),
        Instant.ofEpochSecond(101),
        SohStatus.GOOD,
        "A",
        Set.of(UUID.randomUUID()),
        Map.of(
            "B", SohStatus.MARGINAL
        )
    );

    TestUtilities.testSerialization(
        capabilitySohRollup,
        CapabilitySohRollup.class
    );
  }

}
