package gms.core.performancemonitoring.ssam.control.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import java.time.Duration;
import java.util.List;
import java.util.Set;


@AutoValue
public abstract class StationSohMonitoringDisplayParameters {

  public abstract Duration getRedisplayPeriod();

  public abstract Duration getAcknowledgementQuietDuration();

  public abstract List<Duration> getAvailableQuietDurations();

  public abstract Duration getSohStationStaleDuration();

  public abstract List<Duration> getSohHistoricalDurations();


  @JsonCreator
  public static StationSohMonitoringDisplayParameters from(
      @JsonProperty("redisplayPeriod") Duration redisplayPeriod,
      @JsonProperty("acknowledgementQuietDuration") Duration acknowledgementQuietDuration,
      @JsonProperty("availableQuietDurations") List<Duration> availableQuietDurations,
      @JsonProperty("sohStationStaleDuration") Duration sohStationStaleDuration,
      @JsonProperty("sohHistoricalDurations") List<Duration> sohHistoricalDurations) {
    return new AutoValue_StationSohMonitoringDisplayParameters(redisplayPeriod,
        acknowledgementQuietDuration, availableQuietDurations, sohStationStaleDuration, sohHistoricalDurations);
  }

}
