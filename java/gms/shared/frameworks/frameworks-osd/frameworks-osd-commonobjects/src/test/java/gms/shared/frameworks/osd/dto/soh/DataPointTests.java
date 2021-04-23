package gms.shared.frameworks.osd.dto.soh;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import org.junit.jupiter.api.Test;

public class DataPointTests {

  @Test
  void testBuilder() {
    var nowInMillis = Instant.now().toEpochMilli();
    var dataPoint = DataPoint.builder()
        .setStatus(DoubleOrInteger.ofInteger(0))
        .setTimeStamp(nowInMillis)
        .build();

    assertEquals(DoubleOrInteger.ofInteger(0), dataPoint.getStatus());
    assertEquals(nowInMillis, dataPoint.getTimeStamp());
  }

}
