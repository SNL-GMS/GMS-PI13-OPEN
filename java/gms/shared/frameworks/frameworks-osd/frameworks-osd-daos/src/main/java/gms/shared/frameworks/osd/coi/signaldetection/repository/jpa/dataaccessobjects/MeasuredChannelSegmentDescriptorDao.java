package gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.dataaccessobjects;

import gms.shared.frameworks.osd.coi.channel.repository.jpa.dataaccessobjects.ChannelDao;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "measured_channel_segment_descriptors")
public class MeasuredChannelSegmentDescriptorDao {

  // TODO: Revisit composite key - may require finishing waveform consensus and/or channel segment removal
  // may not matter because the thing referenced by the id is not in postgres (waveform)
  // also, embeddables can't contain relationships (or at least shouldn't) and neither should
  // composite keys
  @Id
  private UUID id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "channel_name", referencedColumnName = "name")
  private ChannelDao channel;

  @Column(name = "measured_channel_segment_start_time", nullable =  false, updatable = false)
  private Instant measuredChannelSegmentStartTime;

  @Column(name = "measured_channel_segment_end_time", nullable = false, updatable = false)
  private Instant measuredChannelSegmentEndTime;

  //TODO: make this non nullable/updatable when we understand it better (pending waveform consensus)
  @Column(name = "measured_channel_segment_creation_time")
  private Instant measuredChannelSegmentCreationTime;

  public MeasuredChannelSegmentDescriptorDao() {
    // Empty constructor needed for JPA
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public ChannelDao getChannel() {
    return channel;
  }

  public void setChannel(ChannelDao channel) {
    this.channel = channel;
  }

  public Instant getMeasuredChannelSegmentStartTime() {
    return measuredChannelSegmentStartTime;
  }

  public void setMeasuredChannelSegmentStartTime(Instant measuredChannelSegmentStartTime) {
    this.measuredChannelSegmentStartTime = measuredChannelSegmentStartTime;
  }

  public Instant getMeasuredChannelSegmentEndTime() {
    return measuredChannelSegmentEndTime;
  }

  public void setMeasuredChannelSegmentEndTime(Instant measuredChannelSegmentEndTime) {
    this.measuredChannelSegmentEndTime = measuredChannelSegmentEndTime;
  }

  public Instant getMeasuredChannelSegmentCreationTime() {
    return measuredChannelSegmentCreationTime;
  }

  public void setMeasuredChannelSegmentCreationTime(Instant measuredChannelSegmentCreationTime) {
    this.measuredChannelSegmentCreationTime = measuredChannelSegmentCreationTime;
  }
}
