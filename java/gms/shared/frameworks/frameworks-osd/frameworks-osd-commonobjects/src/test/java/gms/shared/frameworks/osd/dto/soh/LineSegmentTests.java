package gms.shared.frameworks.osd.dto.soh;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

public class LineSegmentTests {

  @Test
  void testBuilder() {
    var nowInMillis = Instant.now().toEpochMilli();
    var dataPoint1 = DataPoint.builder()
        .setStatus(DoubleOrInteger.ofInteger(0))
        .setTimeStamp(nowInMillis)
        .build();
    var dataPoint2 = DataPoint.builder()
        .setStatus(DoubleOrInteger.ofInteger(1))
        .setTimeStamp(nowInMillis)
        .build();
    var dataPoint3 = DataPoint.builder()
        .setStatus(DoubleOrInteger.ofDouble(0.0))
        .setTimeStamp(nowInMillis)
        .build();
    var dataPoint4 = DataPoint.builder()
        .setStatus(DoubleOrInteger.ofDouble(1.0))
        .setTimeStamp(nowInMillis)
        .build();
    var lineSegment1 = LineSegment.builder()
        .addDataPoint(dataPoint1)
        .addDataPoint(dataPoint2)
        .build();
    var lineSegment2 = LineSegment.builder()
        .setDataPoints(List.of(dataPoint3, dataPoint4))
        .build();
    assertEquals(2, lineSegment1.getDataPoints().size());
    assertEquals(2, lineSegment2.getDataPoints().size());
    assertEquals(DoubleOrInteger.ofInteger(0), lineSegment1.getDataPoints().get(0).getStatus());
    assertEquals(DoubleOrInteger.ofDouble(0.0), lineSegment2.getDataPoints().get(0).getStatus());
  }

}
