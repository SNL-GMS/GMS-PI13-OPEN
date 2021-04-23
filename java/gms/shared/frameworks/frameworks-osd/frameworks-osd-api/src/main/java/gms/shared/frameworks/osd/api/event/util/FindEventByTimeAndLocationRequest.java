package gms.shared.frameworks.osd.api.event.util;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import java.time.Instant;
import java.util.Optional;

@AutoValue
public abstract class FindEventByTimeAndLocationRequest {

  public abstract Instant getStartTime();

  public abstract Instant getEndTime();

  public abstract Optional<Double> getMinimumLatitude();

  public abstract Optional<Double> getMaximumLatitude();

  public abstract Optional<Double> getMinimumLongitude();

  public abstract Optional<Double> getMaximumLongitude();

  @JsonCreator
  public static FindEventByTimeAndLocationRequest from(
      @JsonProperty("startTime") Instant startTime,
      @JsonProperty("endTime") Instant endTime,
      @JsonProperty("minimumLatitude") double minimumLatitude,
      @JsonProperty("maximumLatitude") double maximumLatitude,
      @JsonProperty("minimumLongitude") double minimumLongitude,
      @JsonProperty("maximumLongitude") double maximumLongitude) {
    return new AutoValue_FindEventByTimeAndLocationRequest(startTime,
        endTime,
        Optional.ofNullable(minimumLatitude),
        Optional.ofNullable(maximumLatitude),
        Optional.ofNullable(minimumLongitude),
        Optional.ofNullable(maximumLongitude));
  }
}
