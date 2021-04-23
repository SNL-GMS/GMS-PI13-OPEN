package gms.shared.frameworks.osd.coi.channel.soh;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.UUID;

/**
 * Define a class to represent an analog State-of-Health reading.
 */
public class AcquiredChannelEnvironmentIssueAnalog extends AcquiredChannelEnvironmentIssue<Double> {

  /**
   * Creates an AcquiredChannelSohAnalog anew.
   *
   * @param channelName identifier referencing the ProcessingChannel this SOH is for.
   * @param type The state of health type that will be represented by all of the times and statuses
   * held by this class.
   * @param startTime the start time for the status
   * @param endTime the end time for the status
   * @param status the Status of the State-Of-Health (e.g. a boolean or a float or something)
   * @throws NullPointerException if any arg is null
   * @throws IllegalArgumentException if string arg is empty
   */
  public static AcquiredChannelEnvironmentIssueAnalog create(
      String channelName,
      AcquiredChannelEnvironmentIssueType type,
      Instant startTime,
      Instant endTime,
      double status) {

    return new AcquiredChannelEnvironmentIssueAnalog(UUID.randomUUID(), channelName,
        type, startTime, endTime, status);
  }

  /**
   * Creates an AcquiredChannelSohAnalog from all params for it.
   *
   * @param channelName identifier referencing the ProcessingChannel this SOH is for.
   * @param type The state of health type that will be represented by all of the times and statuses
   * held by this class.
   * @param startTime the start time for the status
   * @param endTime the end time for the status
   * @param status the Status of the State-Of-Health (e.g. a boolean or a float or something)
   * @throws NullPointerException if any arg is null
   * @throws IllegalArgumentException if string arg is empty
   */
  @JsonCreator
  public static AcquiredChannelEnvironmentIssueAnalog from(
      @JsonProperty("id") UUID id,
      @JsonProperty("channelName") String channelName,
      @JsonProperty("type") AcquiredChannelEnvironmentIssueType type,
      @JsonProperty("startTime") Instant startTime,
      @JsonProperty("endTime") Instant endTime,
      @JsonProperty("status") double status) {

    return new AcquiredChannelEnvironmentIssueAnalog(id, channelName,
        type, startTime, endTime, status);
  }

  /**
   * @param id the identifier for this entity
   * @param channelName identifier referencing the ProcessingChannel this SOH is for.
   * @param type The state of health type that will be represented by all of the times and statuses
   * held by this class.
   * @param startTime the start time for the status
   * @param endTime the end time for the status
   * @param status the Status of the State-Of-Health (e.g. a boolean or a float or something)
   * @throws NullPointerException if any arg is null
   * @throws IllegalArgumentException if string arg is empty
   */
  private AcquiredChannelEnvironmentIssueAnalog(UUID id, String channelName, AcquiredChannelEnvironmentIssueType type,
      Instant startTime, Instant endTime, double status) {
    super(id, channelName, type, startTime, endTime, status);
  }
}
