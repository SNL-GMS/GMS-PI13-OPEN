package gms.shared.frameworks.osd.dto.soh;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.fasterxml.jackson.core.JsonProcessingException;
import gms.shared.frameworks.osd.coi.datatransferobjects.CoiObjectMapperFactory;
import java.time.Instant;
import org.junit.jupiter.api.Test;

public class HistoricalAcquiredChannelEnvironmentalIssuesTests {

  @Test
  public void testCreate() {
    var currentTime = Instant.now();
    var startingDataPoint = DataPoint.builder()
        .setTimeStamp(currentTime.toEpochMilli())
        .setStatus(DoubleOrInteger.ofInteger(0))
        .build();
    var endingDataPoint = DataPoint.builder()
        .setTimeStamp(currentTime.plusSeconds(10).toEpochMilli())
        .setStatus(DoubleOrInteger.ofInteger(0))
        .build();
    var lineSegment = LineSegment.builder()
        .addDataPoint(startingDataPoint)
        .addDataPoint(endingDataPoint)
        .build();
    assertDoesNotThrow(() ->
        HistoricalAcquiredChannelEnvironmentalIssues
            .builder()
            .setChannelName("ASAR.AS01.SHZ")
            .setMonitorType("VAULT_DOOR_OPEN")
            .addLineSegment(lineSegment)
            .build()
    );
  }

  @Test
  public void testSerialization() throws JsonProcessingException {
    var jsonObjectMapper = CoiObjectMapperFactory.getJsonObjectMapper();
    var currentTime = Instant.now();
    var startingDataPoint = DataPoint.builder()
        .setTimeStamp(currentTime.toEpochMilli())
        .setStatus(DoubleOrInteger.ofInteger(0))
        .build();
    var endingDataPoint = DataPoint.builder()
        .setTimeStamp(currentTime.plusSeconds(10).toEpochMilli())
        .setStatus(DoubleOrInteger.ofInteger(0))
        .build();
    var lineSegment = LineSegment.builder()
        .addDataPoint(startingDataPoint)
        .addDataPoint(endingDataPoint)
        .build();
    var historicalAcquiredChannelEnvironmentalIssues = HistoricalAcquiredChannelEnvironmentalIssues
        .builder()
        .setChannelName("ASAR.AS01.SHZ")
        .setMonitorType("VAULT_DOOR_OPEN")
        .addLineSegment(lineSegment)
        .build();

    assertDoesNotThrow(
        () -> jsonObjectMapper.writeValueAsString(historicalAcquiredChannelEnvironmentalIssues));
  }

}
