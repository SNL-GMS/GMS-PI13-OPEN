package gms.shared.frameworks.soh.repository.utils;

import static com.google.common.base.Preconditions.checkState;
import static gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssue.AcquiredChannelEnvironmentIssueType.CLOCK_DIFFERENTIAL_IN_MICROSECONDS;
import static gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssue.AcquiredChannelEnvironmentIssueType.ENV_LAST_GPS_SYNC_TIME;

import gms.shared.frameworks.osd.api.util.StationTimeRangeSohTypeRequest;
import gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssue;
import gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssue.AcquiredChannelEnvironmentIssueType;
import gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssueAnalog;
import gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssueBoolean;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.security.SecureRandom;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AcquiredChannelEnvironmentalIssuesGenerator {

  private static final Logger logger = LoggerFactory
      .getLogger(AcquiredChannelEnvironmentalIssuesGenerator.class);
  private static final int DEFAULT_NUMBER_OF_CHANNELS_PER_STATION = 10;
  private static final double MINIMUM_DATA_INTERVAL_IN_SECONDS = 10;
  private static final double MAXIMUM_DATA_INTERVAL_AS_PERCENTAGE = 0.1;
  private static final double MINIMUM_DATA_GAP_INTERVAL_AS_PERCENTAGE = 0.33;
  private static final double MAXIMUM_DATA_GAP_INTERVAL_AS_PERCENTAGE = 0.5;
  private static final int DATA_CLUSTER_SIZE = 3;

  private final String stationName;
  private final AcquiredChannelEnvironmentIssueType issueType;
  private final Instant startTime;
  private final Instant endTime;

  private final List<AcquiredChannelEnvironmentIssueType> analogTypes = List
      .of(CLOCK_DIFFERENTIAL_IN_MICROSECONDS,
          ENV_LAST_GPS_SYNC_TIME);

  private int numberOfChannelsPerStation;

  private AcquiredChannelEnvironmentalIssuesGenerator(String stationName,
      AcquiredChannelEnvironmentIssueType issueType, Instant startTime, Instant endTime) {
    this.issueType = issueType;
    this.stationName = stationName;
    this.startTime = startTime;
    this.endTime = endTime;
    this.numberOfChannelsPerStation = DEFAULT_NUMBER_OF_CHANNELS_PER_STATION;
  }

  public static AcquiredChannelEnvironmentalIssuesGenerator create(
      StationTimeRangeSohTypeRequest request) {
    return new AcquiredChannelEnvironmentalIssuesGenerator(request.getStationName(),
        request.getType(), request.getTimeRange().getStartTime(),
        request.getTimeRange().getEndTime());
  }

  //      |--------- time window --------- |
  // |--- issue ---| ... more issues... |--- issue ---|
  // issues should be created at pseudo random times over the time window
  // with different issue lengths - variable start/end times
  public List<AcquiredChannelEnvironmentIssue> getHistoricalAcquiredChannelEnvironmentalIssues() {
    Duration totalElapsedTime = Duration.between(startTime, endTime);
    checkState(totalElapsedTime.toSeconds() > MINIMUM_DATA_INTERVAL_IN_SECONDS,
        "Please request data for a longer duration.");
    List<AcquiredChannelEnvironmentIssue> issues = new ArrayList<>();
    var channelList = generateChannelList();
    Duration timeBetweenData;

    logger.info("total elapsed time {}", totalElapsedTime);

    var rand = new SecureRandom();

    for (var channelName : channelList) {
      var counter = 1;
      var dataStart = Duration.ofSeconds(getRandomIntWithinRange(0, 10));
      var s = startTime.minusNanos(dataStart.toNanos());
      var dataIntervalCeiling =
          totalElapsedTime.getSeconds() * MAXIMUM_DATA_INTERVAL_AS_PERCENTAGE;
      if (dataIntervalCeiling > MINIMUM_DATA_INTERVAL_IN_SECONDS) {
        var timeInSecondsBetweenBooleanData = getRandomIntWithinRange(
            (int) MINIMUM_DATA_INTERVAL_IN_SECONDS, (int) dataIntervalCeiling);
        timeBetweenData = Duration
            .ofNanos(timeInSecondsBetweenBooleanData * TimeUnit.SECONDS.toNanos(1));
      } else {
        timeBetweenData = Duration
            .ofNanos(
                Math.round(
                    MINIMUM_DATA_INTERVAL_IN_SECONDS * TimeUnit.SECONDS.toNanos(1)));
      }
      var timeInSecondsBetweenDataClusters = getRandomIntWithinRange(
          (int) (totalElapsedTime.getSeconds() * MINIMUM_DATA_GAP_INTERVAL_AS_PERCENTAGE),
          (int) (totalElapsedTime.getSeconds() * MAXIMUM_DATA_GAP_INTERVAL_AS_PERCENTAGE));
      var timeBetweenClustersOfData = Duration
          .ofNanos(timeInSecondsBetweenDataClusters * TimeUnit.SECONDS.toNanos(1));
      var e = s.plusNanos(timeBetweenData.toNanos());
      var booleanIssueValue = rand.nextBoolean();
      while (!s.isAfter(endTime)) {
        var issue = createIssue(issueType, booleanIssueValue, channelName, s, e);
        issues.add(issue);
        booleanIssueValue = !booleanIssueValue;
        if (counter % DATA_CLUSTER_SIZE == 0) {
          s = e.plusNanos(timeBetweenClustersOfData.toNanos());
          e = s.plusNanos(timeBetweenData.toNanos());
        } else {
          s = e;
          e = e.plusNanos(timeBetweenData.toNanos());
        }
        counter++;
      }
    }

    return issues;
  }

  public void setNumberOfChannelsPerStation(int numberOfChannelsPerStation) {
    this.numberOfChannelsPerStation = numberOfChannelsPerStation;
  }

  private boolean issueTypeIsBoolean(AcquiredChannelEnvironmentIssueType type) {
    for (var t : analogTypes) {
      if (type.equals(t)) {
        return false;
      }
    }

    return true;
  }

  private List<String> generateChannelList() {
    var channelList = new ArrayList<String>();
    var site = stationName.substring(0, 2);
    var channelIterator = 1;
    while (channelIterator <= numberOfChannelsPerStation) {
      if (channelIterator > 9) {
        channelList.add(String.format("%s.%s%s.SHZ", stationName, site, channelIterator));
      } else {
        channelList.add(String.format("%s.%s0%s.SHZ", stationName, site, channelIterator));
      }
      channelIterator++;
    }

    return channelList;
  }

  private AcquiredChannelEnvironmentIssue createIssue(AcquiredChannelEnvironmentIssueType issueType,
      boolean booleanIssueValue, String channelName, Instant s, Instant e) {
    AcquiredChannelEnvironmentIssue issue;
    var rand = new SecureRandom();
    if (issueTypeIsBoolean(issueType)) {
      issue = AcquiredChannelEnvironmentIssueBoolean
          .from(UUID.randomUUID(), channelName, issueType, s, e, booleanIssueValue);
    } else {
      issue = AcquiredChannelEnvironmentIssueAnalog
          .from(UUID.randomUUID(), channelName, issueType, s, e, rand.nextDouble() * 10);
    }

    return issue;
  }

  private int getRandomIntWithinRange(int lowerBound, int upperBound) {
    var rand = new SecureRandom();
    var value = rand.ints(lowerBound, upperBound).findFirst();
    return (value.isPresent() ? value.getAsInt() : lowerBound);
  }

}