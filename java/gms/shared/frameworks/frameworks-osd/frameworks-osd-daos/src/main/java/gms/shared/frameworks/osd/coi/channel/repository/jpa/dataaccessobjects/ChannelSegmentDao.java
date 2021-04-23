package gms.shared.frameworks.osd.coi.channel.repository.jpa.dataaccessobjects;

import gms.shared.frameworks.osd.coi.channel.ChannelSegment;
import gms.shared.frameworks.osd.coi.channel.repository.jpa.dataaccessobjects.converter.ChannelSegmentTypeConverter;
import gms.shared.frameworks.osd.coi.channel.repository.jpa.dataaccessobjects.converter.TimeseriesTypeConverter;
import gms.shared.frameworks.osd.coi.waveforms.Timeseries;
import org.apache.commons.lang3.Validate;
import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "channel_segment",
    indexes = {
        @Index(name = "channel_name_start_end", columnList = "channel_name, start_time, end_time", unique = true)
    })
public class ChannelSegmentDao {

  @Id
  @Column(name = "id", unique = true)
  private UUID id;

  @ManyToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "channel_name")
  private ChannelDao channel;

  @Column(name = "name")
  private String name;

  @Column(name = "type")
  @Convert(converter = ChannelSegmentTypeConverter.class)
  private ChannelSegment.Type type;

  @Column(name = "timeseries_type")
  @Convert(converter = TimeseriesTypeConverter.class)
  private Timeseries.Type timeseriesType;

  @Column(name = "start_time")
  private Instant startTime;

  @Column(name = "end_time")
  private Instant endTime;

  @ElementCollection
  @CollectionTable(name = "channel_segment_timeseries_ids",
      joinColumns = {@JoinColumn(name = "channel_segment_id", referencedColumnName = "id")})
  @Column(name = "timeseries_id")
  private List<UUID> timeSeriesIds;

  /**
   * Default constructor for use by JPA
   */
  public ChannelSegmentDao() {
  }

  /**
   * Create this DAO from the COI object.
   *
   * @param channelSegment COI object
   */
  public ChannelSegmentDao(ChannelSegment<? extends Timeseries> channelSegment) {
    Validate.notNull(channelSegment);
    this.id = channelSegment.getId();
    this.channel = ChannelDao.from(channelSegment.getChannel());
    this.name = channelSegment.getName();
    this.type = channelSegment.getType();
    this.timeseriesType = channelSegment.getTimeseriesType();
    this.startTime = channelSegment.getStartTime();
    this.endTime = channelSegment.getEndTime();
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

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ChannelSegment.Type getType() {
    return type;
  }

  public void setType(ChannelSegment.Type type) {
    this.type = type;
  }

  public Timeseries.Type getTimeseriesType() {
    return timeseriesType;
  }

  public void setTimeseriesType(Timeseries.Type timeseriesType) {
    this.timeseriesType = timeseriesType;
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

  public List<UUID> getTimeSeriesIds() {
    return timeSeriesIds;
  }

  public void setTimeSeriesIds(List<UUID> timeSeriesIds) {
    this.timeSeriesIds = timeSeriesIds;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, channel, name, type, timeseriesType, startTime, endTime,
        timeSeriesIds);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ChannelSegmentDao that = (ChannelSegmentDao) o;
    return com.google.common.base.Objects.equal(id, that.id) &&
        com.google.common.base.Objects.equal(channel, that.channel) &&
        com.google.common.base.Objects.equal(name, that.name) &&
        type == that.type &&
        timeseriesType == that.timeseriesType &&
        com.google.common.base.Objects.equal(startTime, that.startTime) &&
        com.google.common.base.Objects.equal(endTime, that.endTime) &&
        com.google.common.base.Objects.equal(timeSeriesIds, that.timeSeriesIds);
  }

  @Override
  public String toString() {
    return "ChannelSegmentDao{" +
        "id=" + id +
        ", channel=" + channel +
        ", name='" + name + '\'' +
        ", type=" + type +
        ", timeseriesType=" + timeseriesType +
        ", startTime=" + startTime +
        ", endTime=" + endTime +
        ", timeSeriesIds=" + timeSeriesIds +
        '}';
  }

}
