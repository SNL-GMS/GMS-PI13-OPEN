package gms.shared.frameworks.osd.dao.soh;

import gms.shared.frameworks.osd.coi.soh.SohStatus;
import gms.shared.frameworks.osd.dao.channel.StationGroupDao;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import org.hibernate.annotations.Type;

@Entity
@NamedQuery(name="CapabilitySohRollupDao.checkExistsByUUID",
    query="SELECT dao FROM CapabilitySohRollupDao dao WHERE dao.id = :capabilitySohRollupDaoId")
@Table(name="capability_soh_rollup")
public class CapabilitySohRollupDao {

  @Id
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @Column(name = "capability_rollup_time", nullable = false)
  private Instant capabilityRollupTime;

  @Enumerated(EnumType.STRING)
  @Column(name = "group_rollup_status", nullable = false, columnDefinition = "public.soh_status_enum")
  @Type(type = "pgsql_enum")
  private SohStatus groupRollupStatus;

  @OneToOne(fetch= FetchType.LAZY)
  @JoinColumn(name = "station_group_name", referencedColumnName = "name")
  private StationGroupDao stationGroupDao;

  @ElementCollection(fetch= FetchType.LAZY)
  @CollectionTable(name="capability_station_soh_uuids",
      joinColumns = {@JoinColumn(name = "capability_rollup_id",
          referencedColumnName = "id")})
  @Column(name="station_soh_id")
  private Set<UUID> stationSohUUIDS;

  @ElementCollection(fetch= FetchType.LAZY)
  @CollectionTable(name="capability_station_soh_status_map",
      joinColumns = {@JoinColumn(name = "capability_rollup_id",
          referencedColumnName = "id")})
  private List<CapabilityStationStatusDao> stationSohStatusMapping;

  public UUID getId() {return this.id;}

  public void setId(UUID id) {this.id=id;}

  public Instant getTime() {return this.capabilityRollupTime;}

  public void setTime(Instant time) {this.capabilityRollupTime=time;}

  public SohStatus getGroupRollupstatus() {return this.groupRollupStatus;}

  public void setGroupRollupstatus(SohStatus status) {this.groupRollupStatus=status;}

  public StationGroupDao getStationGroupDao() {return this.stationGroupDao;}

  public void setStationGroupDao(StationGroupDao stationGroupDao) {this.stationGroupDao=stationGroupDao;}

  public Set<UUID> getStationSohUUIDs() {
    return this.stationSohUUIDS;
  }

  public void setStationSohUUIDS(Set<UUID> stationSohUUIDS) {this.stationSohUUIDS=stationSohUUIDS;}

  public List<CapabilityStationStatusDao> getRollupSohStatusByStation() {return this.stationSohStatusMapping;}

  public void setRollupSohStatusByStation(List<CapabilityStationStatusDao> stationDaoSohStatusMap) {this.stationSohStatusMapping=stationDaoSohStatusMap;}


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof CapabilitySohRollupDao)) {
      return false;
    }
    CapabilitySohRollupDao that = (CapabilitySohRollupDao) o;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

}
