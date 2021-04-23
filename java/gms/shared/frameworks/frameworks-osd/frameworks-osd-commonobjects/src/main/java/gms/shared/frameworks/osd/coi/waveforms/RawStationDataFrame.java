package gms.shared.frameworks.osd.coi.waveforms;

import static java.util.UUID.randomUUID;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.auto.value.AutoValue;
import java.util.Arrays;
import java.util.UUID;

/**
 * Represents a frame of data from a station; could be received via various protocols. It includes
 * the start/end time of the data, a reference by ID to the channel the data is for, the time it was
 * received, a raw payload (bytes) - this represents the whole raw frame, and the status of its
 * authentication.
 */
@AutoValue
@JsonSerialize(as = RawStationDataFrame.class)
@JsonDeserialize(builder = AutoValue_RawStationDataFrame.Builder.class)
public abstract class RawStationDataFrame {

  public abstract UUID getId();

  public abstract RawStationDataFrameMetadata getMetadata();

  public abstract byte[] getRawPayload();

  public static Builder builder() {
    return new AutoValue_RawStationDataFrame.Builder();
  }

  public abstract Builder toBuilder();

  /**
   * Enum for the status of authentication of a frame.
   */
  public enum AuthenticationStatus {
    NOT_APPLICABLE,
    AUTHENTICATION_FAILED,
    AUTHENTICATION_SUCCEEDED,
    NOT_YET_AUTHENTICATED
  }

  @AutoValue.Builder
  @JsonPOJOBuilder(withPrefix = "set")
  public interface Builder {

    Builder setId(UUID id);

    default Builder generatedId() {
      return setId(randomUUID());
    }

    Builder setMetadata(RawStationDataFrameMetadata metadata);

    Builder setRawPayload(byte[] rawPayload);

    RawStationDataFrame build();
  }

  // Compares the state and the raw payloads of two RSDF objects
  public boolean hasSameStateAndRawPayload(RawStationDataFrame otherRsdf) {
    if (!this.getMetadata().hasSameState(otherRsdf.getMetadata())) {
      return false;
    }

    return Arrays.equals(this.getRawPayload(), otherRsdf.getRawPayload());
  }
}
