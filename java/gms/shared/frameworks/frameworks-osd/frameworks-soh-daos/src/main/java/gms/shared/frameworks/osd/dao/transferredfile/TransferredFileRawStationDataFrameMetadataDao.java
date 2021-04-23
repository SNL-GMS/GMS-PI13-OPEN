package gms.shared.frameworks.osd.dao.transferredfile;

import gms.shared.frameworks.osd.coi.channel.dataacquisitionstatus.TransferredFileRawStationDataFrameMetadata;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Embeddable
public class TransferredFileRawStationDataFrameMetadataDao {

  @Column(nullable = false, name = "payload_start_time")
  private Instant payloadStartTime;
  @Column(nullable = false, name = "payload_end_time")
  private Instant payloadEndTime;
  @Column(name = "station_name")
  private String stationName;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "transferred_file_rsdf_metadata_channel_names",
      joinColumns = @JoinColumn(name = "channel_name"))
  // wondering why this is being initialized when it's later ignored?  If it's not initialized,
  // hibernate won't populate this collection on queries for some unknown reason...
  private List<String> channelNames = new ArrayList<>();

  public TransferredFileRawStationDataFrameMetadataDao() {
  }

  public TransferredFileRawStationDataFrameMetadataDao(
      TransferredFileRawStationDataFrameMetadata metadata) {
    this.payloadStartTime = metadata.getPayloadStartTime();
    this.payloadEndTime = metadata.getPayloadEndTime();
    this.stationName = metadata.getStationName();
    this.channelNames = metadata.getChannelNames();
  }

  public TransferredFileRawStationDataFrameMetadata toCoi() {
    return TransferredFileRawStationDataFrameMetadata
        .from(this.payloadStartTime, this.payloadEndTime, this.stationName, this.channelNames);
  }

  public Instant getPayloadStartTime() {
    return payloadStartTime;
  }

  public void setPayloadStartTime(Instant payloadStartTime) {
    this.payloadStartTime = payloadStartTime;
  }

  public Instant getPayloadEndTime() {
    return payloadEndTime;
  }

  public void setPayloadEndTime(Instant payloadEndTime) {
    this.payloadEndTime = payloadEndTime;
  }

  public String getStationName() {
    return stationName;
  }

  public void setStationName(String stationName) {
    this.stationName = stationName;
  }

  public List<String> getChannelNames() {
    return channelNames;
  }

  public void setChannelNames(List<String> channelNames) {
    this.channelNames = channelNames;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TransferredFileRawStationDataFrameMetadataDao
        that = (TransferredFileRawStationDataFrameMetadataDao) o;
    return Objects.equals(payloadStartTime, that.payloadStartTime) &&
        Objects.equals(payloadEndTime, that.payloadEndTime) &&
        Objects.equals(stationName, that.stationName) &&
        Objects.equals(channelNames, that.channelNames);
  }

  @Override
  public int hashCode() {
    return Objects.hash(payloadStartTime, payloadEndTime, stationName, channelNames);
  }

  @Override
  public String toString() {
    return "TransferredFileRawStationDataFrameMetadataDao{" +
        "payloadStartTime=" + payloadStartTime +
        ", payloadEndTime=" + payloadEndTime +
        ", stationName=" + stationName +
        ", channelNames=" + channelNames +
        '}';
  }
}
