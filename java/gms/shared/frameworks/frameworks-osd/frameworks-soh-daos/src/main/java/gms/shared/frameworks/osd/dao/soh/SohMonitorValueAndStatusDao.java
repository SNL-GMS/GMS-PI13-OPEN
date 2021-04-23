package gms.shared.frameworks.osd.dao.soh;

import gms.shared.frameworks.osd.coi.soh.SohMonitorType;
import gms.shared.frameworks.osd.coi.soh.SohStatus;
import java.time.Duration;
import javax.persistence.Convert;
import org.hibernate.annotations.Type;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.util.Objects;

@Entity
@Table(name = "soh_monitor_value_status")
public class SohMonitorValueAndStatusDao {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "soh_monitor_value_status_sequence")
  @SequenceGenerator(name = "soh_monitor_value_status_sequence", sequenceName = "smvs_sequence", allocationSize = 10)
  private int id;

  @Convert(converter = SohStatusConverter.class)
  @Column(name = "status", nullable = false)
  private SohStatus status;

  @Convert(converter = SohMonitorTypeConverter.class)
  @Column(name = "monitor_type", nullable = false)
  private SohMonitorType monitorType;

  @JoinColumn(name = "channel_soh_id", referencedColumnName = "id")
  @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
  private ChannelSohDao channelSoh;

  @JoinColumn(name = "station_soh_id", referencedColumnName = "id")
  @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
  private StationSohDao stationSoh;

  @Column(name = "duration")
  private Integer duration;

  @Column(name = "percent")
  private Float percent;

  public SohMonitorValueAndStatusDao() {
    // no-arg JPA constructor
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public SohStatus getStatus() {
    return status;
  }

  public void setStatus(SohStatus status) {
    this.status = status;
  }

  public SohMonitorType getMonitorType() {
    return monitorType;
  }

  public void setMonitorType(SohMonitorType monitorType) {
    this.monitorType = monitorType;
  }

  public ChannelSohDao getChannelSoh() {
    return channelSoh;
  }

  public void setChannelSoh(ChannelSohDao channelSoh) {
    this.channelSoh = channelSoh;
  }

  public StationSohDao getStationSoh() {
    return this.stationSoh;
  }

  public void setStationSoh(final StationSohDao stationSoh) {
    this.stationSoh = stationSoh;
  }

  public Integer getDuration() {
    return this.duration;
  }

  public void setDuration(final Integer duration) {
    this.duration = duration;
  }

  public Float getPercent() {
    return this.percent;
  }

  public void setPercent(final Float percent) {
    this.percent = percent;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SohMonitorValueAndStatusDao that = (SohMonitorValueAndStatusDao) o;
    return id == that.id &&
        status == that.status &&
        monitorType == that.monitorType;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, status, monitorType);
  }
}
