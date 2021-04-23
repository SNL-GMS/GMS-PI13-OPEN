package gms.shared.frameworks.osd.coi.dataacquisition.configuration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import gms.shared.frameworks.osd.coi.dataacquisition.StationAndChannelId;
import java.util.Map;

@AutoValue
public abstract class RawStationDataFrameUtilityParameters {

  public abstract Map<String, StationAndChannelId> getChannelToUUIDMap();

  @JsonCreator
  public static RawStationDataFrameUtilityParameters from(
      @JsonProperty("snclToStaAndChanIds") Map<String, StationAndChannelId> snclToStaAndChanIds) {

    return new AutoValue_RawStationDataFrameUtilityParameters(snclToStaAndChanIds);
  }
}
