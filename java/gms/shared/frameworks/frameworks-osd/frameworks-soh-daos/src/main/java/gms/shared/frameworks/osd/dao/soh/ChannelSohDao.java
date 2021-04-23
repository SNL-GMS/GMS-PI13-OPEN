package gms.shared.frameworks.osd.dao.soh;

import gms.shared.frameworks.osd.coi.soh.SohMonitorType;
import gms.shared.frameworks.osd.coi.soh.SohStatus;
import gms.shared.frameworks.osd.dao.channel.ChannelDao;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import org.hibernate.annotations.Type;

@Entity
@Table(name = "channel_soh")
public class ChannelSohDao {

  @Id
  @Column(name = "id", updatable = false, nullable = false)
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "channel_soh_sequence")
  @SequenceGenerator(name = "channel_soh_sequence", sequenceName = "channel_soh_sequence", allocationSize = 5)
  private int id;

  @ManyToOne(fetch=FetchType.LAZY)
  @JoinColumn(name = "channel_name", referencedColumnName = "name")
  private ChannelDao channel;

  //this allows us to retrieve the channel_name without an extraneous join
  @Column(name = "channel_name", insertable = false, updatable = false)
  private String channelName;

  @Enumerated(EnumType.STRING)
  @Column(name = "soh_status", nullable = false, columnDefinition = "public.soh_status_enum")
  @Type(type = "pgsql_enum")
  private SohStatus sohStatus;

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY, mappedBy = "channelSoh")
  private Set<SohMonitorValueAndStatusDao> allMonitorValueAndStatuses;

  @JoinColumn(name = "station_soh_id", referencedColumnName = "id")
  @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
  private StationSohDao stationSoh;

  public ChannelSohDao() {
    // No arg hibernate constructor
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
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

  public SohStatus getSohStatus() {
    return sohStatus;
  }

  public void setSohStatus(SohStatus sohStatus) {
    this.sohStatus = sohStatus;
  }

  public Set<SohMonitorValueAndStatusDao> getAllMonitorValueAndStatuses() {
    return allMonitorValueAndStatuses;
  }

  public void setAllMonitorValueAndStatuses(
      Set<SohMonitorValueAndStatusDao> allMonitorValueAndStatuses) {
    this.allMonitorValueAndStatuses = allMonitorValueAndStatuses;
  }

  public StationSohDao getStationSoh() {
    return stationSoh;
  }

  public void setStationSoh(StationSohDao stationSoh) {
    this.stationSoh = stationSoh;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ChannelSohDao that = (ChannelSohDao) o;
    return getChannelName().equals(that.getChannelName());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getChannel().getName());
  }
}
