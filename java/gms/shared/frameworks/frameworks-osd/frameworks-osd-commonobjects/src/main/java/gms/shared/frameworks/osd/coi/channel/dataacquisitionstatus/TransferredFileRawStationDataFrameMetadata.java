package gms.shared.frameworks.osd.coi.channel.dataacquisitionstatus;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.time.Instant;
import java.util.Collection;
import java.util.List;

/**
 * TransferredRawStationDataFrameMetaData is a binding of TransferredFile's generic metadata object
 * that contains metadata about a transferred RawStationDataFrame.
 */

@AutoValue
public abstract class TransferredFileRawStationDataFrameMetadata {

  public abstract Instant getPayloadStartTime();

  public abstract Instant getPayloadEndTime();

  public abstract String getStationName();

  public abstract ImmutableList<String> getChannelNames();

  public static Builder builder() {
    return new AutoValue_TransferredFileRawStationDataFrameMetadata.Builder();
  }

  public abstract Builder toBuilder();

  /**
   * Creates an instance of TransferredFileRawStationDataFrameMetadata
   *
   * @return a TransferredFileRawStationDataFrameMetadata
   */
  @JsonCreator
  public static TransferredFileRawStationDataFrameMetadata from(
      @JsonProperty("payloadStartTime") Instant payloadStartTime,
      @JsonProperty("payloadEndTime") Instant payloadEndTime,
      @JsonProperty("stationName") String stationName,
      @JsonProperty("channelNames") List<String> channelNames) {

    return builder()
        .setPayloadStartTime(payloadStartTime)
        .setPayloadEndTime(payloadEndTime)
        .setStationName(stationName)
        .setChannelNames(channelNames)
        .build();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setPayloadStartTime(Instant payloadStartTime);

    public abstract Builder setPayloadEndTime(Instant payloadEndTime);

    public abstract Builder setStationName(String stationName);

    abstract ImmutableList<String> getChannelNames();

    abstract Builder setChannelNames(ImmutableList<String> channelNames);

    abstract ImmutableList.Builder<String> channelNamesBuilder();

    public Builder setChannelNames(Collection<String> channelNames) {
      return setChannelNames(ImmutableList.copyOf(channelNames));
    }

    public Builder addChannelName(String channelName) {
      channelNamesBuilder().add(channelName);
      return this;
    }

    abstract TransferredFileRawStationDataFrameMetadata autoBuild();

    public TransferredFileRawStationDataFrameMetadata build() {
      String initializationErrorMessagePrefix =
          "Error creating TransferredFileRawStationDataFrameMetadata, ";
      TransferredFileRawStationDataFrameMetadata
          transferredFileRawStationDataFrameMetadata = autoBuild();
      Preconditions.checkState(
          transferredFileRawStationDataFrameMetadata.getPayloadStartTime() != null,
          "%spayload start time cannot be null",
          initializationErrorMessagePrefix);
      Preconditions.checkState(
          transferredFileRawStationDataFrameMetadata.getPayloadEndTime() != null,
          "%spayload end time cannot be null",
          initializationErrorMessagePrefix);
      Preconditions.checkState(
          !transferredFileRawStationDataFrameMetadata.getStationName().isEmpty(),
          "%sstation name cannot be empty", initializationErrorMessagePrefix);
      Preconditions.checkState(
          !transferredFileRawStationDataFrameMetadata.getChannelNames().isEmpty(),
          "%schannel names cannot be empty", initializationErrorMessagePrefix);

      return transferredFileRawStationDataFrameMetadata;
    }

  }

}
