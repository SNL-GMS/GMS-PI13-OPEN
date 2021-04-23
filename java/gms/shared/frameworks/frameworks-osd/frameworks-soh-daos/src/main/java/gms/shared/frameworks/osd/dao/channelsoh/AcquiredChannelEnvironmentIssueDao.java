package gms.shared.frameworks.osd.dao.channelsoh;

import gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssue.AcquiredChannelEnvironmentIssueType;
import gms.shared.frameworks.osd.dao.channel.ChannelDao;
import gms.shared.frameworks.osd.dao.stationgroupsoh.converter.AcquiredChannelEnvironmentIssueTypeConverter;
import java.time.Instant;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;

@MappedSuperclass
public class AcquiredChannelEnvironmentIssueDao {

  @Id
  @Column(name = "id", nullable = false)
  protected UUID id;

  @Column(name = "natural_id", nullable = false)
  protected int naturalId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "channel_name", referencedColumnName = "name")
  protected ChannelDao channel;

  //this allows us to retrieve the channel_name without an extraneous join
  @Column(name = "channel_name", insertable = false, updatable = false)
  protected String channelName;

  @Column(name = "type", nullable = false)
  @Convert(converter = AcquiredChannelEnvironmentIssueTypeConverter.class)
  protected AcquiredChannelEnvironmentIssueType type;

  @Column(name = "start_time", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
  protected Instant startTime;

  @Column(name = "end_time", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
  protected Instant endTime;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public int getNaturalId() {
    return this.naturalId;
  }

  public void setNaturalId(final int naturalId) {
    this.naturalId = naturalId;
  }

  public ChannelDao getChannel() {
    return channel;
  }

  public void setChannel(ChannelDao channel) {
    this.channel = channel;
  }

  public String getChannelName() {
    return this.channelName;
  }

  public void setChannelName(final String channelName) {
    this.channelName = channelName;
  }
  public AcquiredChannelEnvironmentIssueType getType() {
    return type;
  }

  public void setType(AcquiredChannelEnvironmentIssueType type) {
    this.type = type;
  }

  public Instant getStartTime() {
    return startTime;
  }

  public void setStartTime(Instant startTime) {
    this.startTime = startTime;
  }

  public Instant getEndTime() {
    return endTime;
  }

  public void setEndTime(Instant endTime) {
    this.endTime = endTime;
  }

}
