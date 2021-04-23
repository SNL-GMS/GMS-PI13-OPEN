package gms.shared.frameworks.osd.dao.soh;

import gms.shared.frameworks.osd.coi.soh.SohMonitorType;
import gms.shared.frameworks.osd.coi.soh.SohStatus;
import gms.shared.frameworks.osd.dao.channel.StationDao;
import java.time.Instant;
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
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.NaturalIdCache;
import org.hibernate.annotations.Type;

@Entity
@Table(name = "station_soh")
@org.hibernate.annotations.Cache(
    usage = CacheConcurrencyStrategy.READ_WRITE
)
@NaturalIdCache
public class StationSohDao {

  @Id
  @Column(name = "id", updatable = false, nullable = false)
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "station_soh_sequence")
  @SequenceGenerator(name = "station_soh_sequence", sequenceName = "station_soh_sequence", allocationSize = 5)
  private int id;

  @NaturalId
  @Column(name = "coi_id", updatable = false, nullable = false)
  private UUID coiId;

  @Column(name = "creation_time", nullable = false)
  private Instant creationTime;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "station_name", referencedColumnName = "name")
  private StationDao station;

  //this allows us to retrieve the station_name without an extraneous join
  @Column(name = "station_name", insertable = false, updatable = false)
  private String stationName;

  @Enumerated(EnumType.STRING)
  @Column(name = "soh_status", nullable = false, columnDefinition = "public.soh_status_enum")
  @Type(type = "pgsql_enum")
  private SohStatus sohStatus;

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "stationSoh")
  private Set<SohMonitorValueAndStatusDao> sohMonitorValueAndStatuses;

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "stationSoh")
  private Set<StationAggregateDao> allStationAggregate;

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "stationSoh")
  private Set<ChannelSohDao> channelSohs;

  public StationSohDao() {
    // empty JPA constructor
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public UUID getCoiId() {
    return this.coiId;
  }

  public void setCoiId(final UUID coiId) {
    this.coiId = coiId;
  }

  public Instant getCreationTime() {
    return creationTime;
  }

  public void setCreationTime(Instant creationTime) {
    this.creationTime = creationTime;
  }

  public StationDao getStation() {
    return station;
  }

  public void setStation(StationDao station) {
    this.station = station;
  }

  public SohStatus getSohStatus() {
    return sohStatus;
  }

  public void setSohStatus(SohStatus sohStatus) {
    this.sohStatus = sohStatus;
  }

  public Set<SohMonitorValueAndStatusDao> getSohMonitorValueAndStatuses() {
    return this.sohMonitorValueAndStatuses;
  }

  public void setSohMonitorValueAndStatuses(
      final Set<SohMonitorValueAndStatusDao> sohMonitorValueAndStatuses) {
    this.sohMonitorValueAndStatuses = sohMonitorValueAndStatuses;
  }

  public Set<StationAggregateDao> getAllStationAggregate() {
    return allStationAggregate;
  }

  public void setAllStationAggregate(
    Set<StationAggregateDao> allStationAggregate) {
    this.allStationAggregate = allStationAggregate;
  }

  public Set<ChannelSohDao> getChannelSohs() {
    return channelSohs;
  }

  public void setChannelSohs(Set<ChannelSohDao> channelSohs) {
    this.channelSohs = channelSohs;
  }

  public String getStationName() {
    return this.stationName;
  }

  public void setStationName(String stationName) {
    this.stationName = stationName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    StationSohDao that = (StationSohDao) o;
    return getId() == that.getId();
  }

  @Override
  public int hashCode() {
    return Objects.hash(getId());
  }
}
