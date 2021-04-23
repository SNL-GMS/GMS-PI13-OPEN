package gms.shared.frameworks.osd.coi.dataacquisition.configuration;

import static com.google.common.base.Preconditions.checkState;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import gms.shared.frameworks.osd.coi.dataacquisition.StationAndChannelId;
import gms.shared.frameworks.osd.coi.waveforms.AcquisitionProtocol;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

@AutoValue
@JsonSerialize(as = StationDataAcquisitionGroup.class)
@JsonDeserialize(builder = AutoValue_StationDataAcquisitionGroup.Builder.class)
public abstract class StationDataAcquisitionGroup {

  public abstract UUID getId();

  public abstract ImmutableList<String> getRequestStrings();

  public abstract AcquisitionProtocol getProtocol();

  public abstract String getProviderIpAddress();

  public abstract int getProviderPort();

  public abstract Instant getActualChangeTime();

  public abstract Instant getSystemChangeTime();

  public abstract ImmutableMap<String, StationAndChannelId> getIdsByReceivedName();

  public abstract boolean isActive();

  public abstract String getComment();

  public static Builder builder(){
    return new AutoValue_StationDataAcquisitionGroup.Builder()
        .generatedId();
  }

  public abstract Builder toBuilder();

  @AutoValue.Builder
  @JsonPOJOBuilder(withPrefix = "set")
  public abstract static class Builder {

    public abstract Builder setId(UUID id);

    public Builder generatedId() {
      return setId(UUID.randomUUID());
    }

    abstract Builder setRequestStrings(ImmutableList<String> requestStrings);

    public Builder setRequestStrings(Collection<String> requestStrings) {
      return setRequestStrings(ImmutableList.copyOf(requestStrings));
    }

    abstract ImmutableList.Builder<String> requestStringsBuilder();

    public Builder addRequestString(String requestString) {
      requestStringsBuilder().add(requestString);
      return this;
    }

    public abstract Builder setProtocol(AcquisitionProtocol protocol);

    public abstract Builder setProviderIpAddress(String providerIpAddress);

    public abstract Builder setProviderPort(int providerPort);

    public abstract Builder setActualChangeTime(Instant actualChangeTime);

    public abstract Builder setSystemChangeTime(Instant systemChangeTime);

    abstract Builder setIdsByReceivedName(
        ImmutableMap<String, StationAndChannelId> idsByReceivedName);

    public Builder setIdsByReceivedName(Map<String, StationAndChannelId> idsByReceivedName) {
      return setIdsByReceivedName(ImmutableMap.copyOf(idsByReceivedName));
    }

    abstract ImmutableMap.Builder<String, StationAndChannelId> idsByReceivedNameBuilder();

    public Builder putIdByRecievedName(String receivedName, StationAndChannelId id) {
      idsByReceivedNameBuilder().put(receivedName, id);
      return this;
    }

    public abstract Builder setActive(boolean active);

    public abstract Builder setComment(String comment);

    abstract StationDataAcquisitionGroup autoBuild();

    public StationDataAcquisitionGroup build() {
      StationDataAcquisitionGroup stationDataAcquisitionGroup = autoBuild();
      checkState(!stationDataAcquisitionGroup.getRequestStrings().isEmpty(),
          "requestStrings cannot be empty or null");

      return stationDataAcquisitionGroup;
    }

  }
}
