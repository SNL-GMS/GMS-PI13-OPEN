package gms.shared.frameworks.soh.repository.performancemonitoring.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gms.shared.frameworks.osd.api.util.StationTimeRangeSohTypeRequest;
import gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssue.AcquiredChannelEnvironmentIssueType;
import gms.shared.frameworks.osd.dto.soh.DoubleOrInteger;
import gms.shared.frameworks.soh.repository.utils.AcquiredChannelEnvironmentalIssuesGenerator;
import java.time.Instant;
import org.junit.jupiter.api.Test;

public class AcquiredChannelEnvironmentalIssuesTransformerTests {

  @Test
  void testTransform() {
    var numberOfChannels = 10;
    var startTime = Instant.parse("2016-12-23T01:23:45Z");
    var endTime = Instant.parse("2016-12-23T02:29:45Z");
    var request = StationTimeRangeSohTypeRequest
        .create("MKAR", startTime, endTime,
            AcquiredChannelEnvironmentIssueType.CLOCK_DIFFERENTIAL_IN_MICROSECONDS);
    var request2 = StationTimeRangeSohTypeRequest
        .create("ASAR", startTime, endTime,
            AcquiredChannelEnvironmentIssueType.VAULT_DOOR_OPENED);
    var generator = AcquiredChannelEnvironmentalIssuesGenerator.create(request);
    var generator2 = AcquiredChannelEnvironmentalIssuesGenerator.create(request2);

    generator.setNumberOfChannelsPerStation(numberOfChannels);
    generator2.setNumberOfChannelsPerStation(numberOfChannels);

    var response = generator.getHistoricalAcquiredChannelEnvironmentalIssues();
    var response2 = generator2.getHistoricalAcquiredChannelEnvironmentalIssues();

    var transformed = AcquiredChannelEnvironmentalIssuesTransformer
        .toHistoricalAcquiredChannelEnvironmentalIssues(response);

    assertEquals(numberOfChannels, transformed.size());
    assertEquals(request.getType().name(), transformed.get(0).getMonitorType());
    transformed.forEach(t -> assertTrue(t.getChannelName().contains(request.getStationName())));
    transformed.forEach(t -> assertTrue(t.getTrendLine().get(0).getDataPoints().size() >= 2));

    var transformed2 = AcquiredChannelEnvironmentalIssuesTransformer
        .toHistoricalAcquiredChannelEnvironmentalIssues(response2);

    assertEquals(numberOfChannels, transformed2.size());
    assertEquals(request2.getType().name(), transformed2.get(0).getMonitorType());
    transformed2.forEach(t -> assertTrue(t.getChannelName().contains(request2.getStationName())));
    transformed2
        .forEach(t -> assertTrue(t.getTrendLine().get(0).getDataPoints().size() >= 2));
  }

  @Test
  void testThatStatusesWereCreatedCorrectly() {
    var numberOfChannels = 1;
    var startTime = Instant.parse("2016-12-23T02:29:34Z");
    var endTime = Instant.parse("2016-12-23T02:29:45Z");
    var request = StationTimeRangeSohTypeRequest
        .create("ASAR", startTime, endTime,
            AcquiredChannelEnvironmentIssueType.VAULT_DOOR_OPENED);
    var generator = AcquiredChannelEnvironmentalIssuesGenerator.create(request);
    generator.setNumberOfChannelsPerStation(numberOfChannels);
    var response = generator.getHistoricalAcquiredChannelEnvironmentalIssues();
    var transformed = AcquiredChannelEnvironmentalIssuesTransformer
        .toHistoricalAcquiredChannelEnvironmentalIssues(response);
    assertEquals(numberOfChannels, transformed.size());
    assertEquals(request.getType().name(), transformed.get(0).getMonitorType());
    transformed.forEach(t -> assertTrue(t.getChannelName().contains(request.getStationName())));
    transformed.forEach(t -> assertTrue(t.getTrendLine().get(0).getDataPoints().size() >= 2));
    assertEquals(DoubleOrInteger.ofInteger(response.get(0).getStatus().equals(false) ? 0 : 1),
        transformed.get(0).getTrendLine().get(0).getDataPoints().get(0).getStatus());
    var dataPoints = transformed.get(0).getTrendLine().get(0).getDataPoints();
    assertEquals(DoubleOrInteger
            .ofInteger(response.get(response.size() - 1).getStatus().equals(false) ? 0 : 1),
        dataPoints.get(dataPoints.size() - 1).getStatus());
    assertEquals(dataPoints.get(dataPoints.size() - 1).getStatus(),
        dataPoints.get(dataPoints.size() > 2 ? dataPoints.size() - 2 : 0).getStatus());
  }

  @Test
  void testThatTimestampsWereCreatedCorrectly() {
    var numberOfChannels = 1;
    var startTime = Instant.parse("2016-12-23T02:29:34Z");
    var endTime = Instant.parse("2016-12-23T02:29:45Z");
    var request = StationTimeRangeSohTypeRequest
        .create("ASAR", startTime, endTime,
            AcquiredChannelEnvironmentIssueType.VAULT_DOOR_OPENED);
    var generator = AcquiredChannelEnvironmentalIssuesGenerator.create(request);
    generator.setNumberOfChannelsPerStation(numberOfChannels);
    var response = generator.getHistoricalAcquiredChannelEnvironmentalIssues();
    var transformed = AcquiredChannelEnvironmentalIssuesTransformer
        .toHistoricalAcquiredChannelEnvironmentalIssues(response);
    assertEquals(numberOfChannels, transformed.size());
    assertEquals(request.getType().name(), transformed.get(0).getMonitorType());
    transformed.forEach(t -> assertTrue(t.getChannelName().contains(request.getStationName())));
    transformed.forEach(t -> assertTrue(t.getTrendLine().get(0).getDataPoints().size() >= 2));
    assertEquals(response.get(0).getStartTime().toEpochMilli(),
        transformed.get(0).getTrendLine().get(0).getDataPoints().get(0).getTimeStamp());
    var dataPoints = transformed.get(0).getTrendLine().get(0).getDataPoints();
    assertEquals(response.get(response.size() - 1).getEndTime().toEpochMilli(),
        dataPoints.get(dataPoints.size() - 1).getTimeStamp());
    assertEquals(response.get(response.size() - 1).getStartTime().toEpochMilli(),
        dataPoints.get(dataPoints.size() > 2 ? dataPoints.size() - 2 : 0).getTimeStamp());
  }

  @Test
  void volumeTest() {
    var numberOfChannels = 100;
    var startTime = Instant.parse("2015-12-23T02:29:45Z");
    var endTime = Instant.parse("2016-12-23T02:29:45Z");
    var request = StationTimeRangeSohTypeRequest
        .create("MKAR", startTime, endTime,
            AcquiredChannelEnvironmentIssueType.CLOCK_DIFFERENTIAL_IN_MICROSECONDS);
    var request2 = StationTimeRangeSohTypeRequest
        .create("ASAR", startTime, endTime,
            AcquiredChannelEnvironmentIssueType.VAULT_DOOR_OPENED);
    var generator = AcquiredChannelEnvironmentalIssuesGenerator.create(request);
    var generator2 = AcquiredChannelEnvironmentalIssuesGenerator.create(request2);
    generator.setNumberOfChannelsPerStation(numberOfChannels);
    generator2.setNumberOfChannelsPerStation(numberOfChannels);
    var response = generator.getHistoricalAcquiredChannelEnvironmentalIssues();
    var response2 = generator2.getHistoricalAcquiredChannelEnvironmentalIssues();
    var transformed = AcquiredChannelEnvironmentalIssuesTransformer
        .toHistoricalAcquiredChannelEnvironmentalIssues(response);
    assertEquals(numberOfChannels, transformed.size());
    assertEquals(request.getType().name(), transformed.get(0).getMonitorType());
    transformed.forEach(t -> assertTrue(t.getChannelName().contains(request.getStationName())));
    transformed.forEach(t -> assertTrue(t.getTrendLine().get(0).getDataPoints().size() >= 2));
    var transformed2 = AcquiredChannelEnvironmentalIssuesTransformer
        .toHistoricalAcquiredChannelEnvironmentalIssues(response2);
    assertEquals(numberOfChannels, transformed2.size());
    assertEquals(request2.getType().name(), transformed2.get(0).getMonitorType());
    transformed2.forEach(t -> assertTrue(t.getChannelName().contains(request2.getStationName())));
    transformed2
        .forEach(t -> assertTrue(t.getTrendLine().get(0).getDataPoints().size() >= 2));
  }

}
