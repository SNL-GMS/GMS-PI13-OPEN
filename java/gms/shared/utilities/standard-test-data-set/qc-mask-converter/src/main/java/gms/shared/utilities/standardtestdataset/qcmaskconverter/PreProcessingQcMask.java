package gms.shared.utilities.standardtestdataset.qcmaskconverter;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import gms.shared.frameworks.osd.coi.signaldetection.QcMask;
import gms.shared.frameworks.osd.coi.signaldetection.QcMaskVersion;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@AutoValue
public abstract class PreProcessingQcMask {

  public abstract String getSta();
  public abstract String getChan();
  public abstract int getChanId();
  public abstract int getSampRate();
  public abstract int getWfid();
  public abstract String getId();
  public abstract String getChannelName();
  public abstract List<QcMaskVersion> getQcMaskVersions();

  @JsonCreator
  public static PreProcessingQcMask from(
      @JsonProperty("sta") String sta, 
      @JsonProperty("chan") String chan, 
      @JsonProperty("chanId") int chanId, 
      @JsonProperty("sampRate") int sampRate, 
      @JsonProperty("wfid") int wfid,
      @JsonProperty("id") String id, 
      @JsonProperty("channelName") String channelName,
      @JsonProperty("qcMaskVersions") List<QcMaskVersion> qcMaskVersions) {
    return new AutoValue_PreProcessingQcMask(sta, chan, chanId,
        sampRate, wfid, id, channelName, qcMaskVersions);
  }

  QcMask toQcMask(){
    final UUID channelSegmentId = UUID.nameUUIDFromBytes(Double.toString(getWfid()).getBytes());
    List<QcMaskVersion> versionsWithIds = new ArrayList<>();
    for(QcMaskVersion version: getQcMaskVersions()){
      QcMaskVersion versionWithIds = QcMaskVersion.builder()
          .setVersion(version.getVersion())
          .setParentQcMasks(version.getParentQcMasks())
          .setChannelSegmentIds(List.of(channelSegmentId))
          .setType(version.getType().orElseThrow(IllegalArgumentException::new))
          .setCategory(version.getCategory())
          .setRationale(version.getRationale())
          .setStartTime(version.getStartTime().orElseThrow(IllegalArgumentException::new))
          .setEndTime(version.getEndTime().orElseThrow(IllegalArgumentException::new))
          .build();
      versionsWithIds.add(versionWithIds);
    }
    return QcMask.from(UUID.randomUUID(), getChannelName(), versionsWithIds);
  }
}
