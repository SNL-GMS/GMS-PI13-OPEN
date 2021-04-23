package gms.shared.frameworks.soh.repository.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gms.shared.frameworks.osd.api.util.StationTimeRangeSohTypeRequest;
import gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssue;
import gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssue.AcquiredChannelEnvironmentIssueType;
import java.time.Instant;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class AcquiredChannelEnvironmentalIssuesGeneratorTests {

  @Test
  void assertThrowsWithTooSmallQueryWindow() {
    var startTime = Instant.parse("2016-12-23T01:23:45Z");
    var endTime = Instant.parse("2016-12-23T01:23:55Z");
    var request = StationTimeRangeSohTypeRequest
        .create("MKAR", startTime, endTime,
            AcquiredChannelEnvironmentIssueType.CLOCK_DIFFERENTIAL_IN_MICROSECONDS);
    var generator = AcquiredChannelEnvironmentalIssuesGenerator.create(request);
    generator.setNumberOfChannelsPerStation(1);
    assertThrows(IllegalStateException.class,
        generator::getHistoricalAcquiredChannelEnvironmentalIssues);
  }

  @Test
  @Disabled // tpf - 9-14-20 - disabled due to code being OBE, but want to see why it's failing prior to removing
  void verifyMinimumData() {
    var startTime = Instant.parse("2016-12-23T01:23:45Z");
    var endTime = Instant.parse("2016-12-23T01:23:56Z");
    var request = StationTimeRangeSohTypeRequest
        .create("ASAR", startTime, endTime,
            AcquiredChannelEnvironmentIssueType.VAULT_DOOR_OPENED);
    var request2 = StationTimeRangeSohTypeRequest
        .create("MKAR", startTime, endTime,
            AcquiredChannelEnvironmentIssueType.CLOCK_DIFFERENTIAL_IN_MICROSECONDS);

    var generator = AcquiredChannelEnvironmentalIssuesGenerator.create(request);
    var generator2 = AcquiredChannelEnvironmentalIssuesGenerator.create(request2);
    generator.setNumberOfChannelsPerStation(1);
    generator2.setNumberOfChannelsPerStation(1);
    assertEquals(2, generator.getHistoricalAcquiredChannelEnvironmentalIssues().size());
    assertEquals(2, generator2.getHistoricalAcquiredChannelEnvironmentalIssues().size());
  }

  @Test
  void testFiveMinuteQueryWindowRequest() {
    var startTime = Instant.parse("2016-12-23T01:23:45Z");
    var endTime = Instant.parse("2016-12-23T01:28:45Z");
    var request = StationTimeRangeSohTypeRequest
        .create("MKAR", startTime, endTime,
            AcquiredChannelEnvironmentIssueType.CLOCK_DIFFERENTIAL_IN_MICROSECONDS);
    var request2 = StationTimeRangeSohTypeRequest
        .create("ASAR", startTime, endTime,
            AcquiredChannelEnvironmentIssueType.VAULT_DOOR_OPENED);
    var generator = AcquiredChannelEnvironmentalIssuesGenerator.create(request);
    var generator2 = AcquiredChannelEnvironmentalIssuesGenerator.create(request2);
    generator.setNumberOfChannelsPerStation(1);
    generator2.setNumberOfChannelsPerStation(1);
    var issues = generator.getHistoricalAcquiredChannelEnvironmentalIssues();
    var issues2 = generator.getHistoricalAcquiredChannelEnvironmentalIssues();
    assertTrue(issues.size() > 3 && issues.size() < 30);
    assertTrue(issues2.size() > 3 && issues2.size() < 30);
  }

  @Test
  void testMultipleChannelIssueRequest() {
    var startTime = Instant.parse("2016-12-23T01:23:45Z");
    var endTime = Instant.parse("2016-12-23T01:28:45Z");
    var request = StationTimeRangeSohTypeRequest
        .create("ASAR", startTime, endTime,
            AcquiredChannelEnvironmentIssueType.VAULT_DOOR_OPENED);
    var generator = AcquiredChannelEnvironmentalIssuesGenerator.create(request);
    generator.setNumberOfChannelsPerStation(3);
    var issues = generator.getHistoricalAcquiredChannelEnvironmentalIssues();
    assertEquals(3,
        issues.stream().map(AcquiredChannelEnvironmentIssue::getChannelName).distinct().count());
  }

}
