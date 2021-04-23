package gms.shared.frameworks.osd.dao.soh;


import gms.shared.frameworks.osd.coi.soh.SohStatus;
import gms.shared.frameworks.osd.dao.channel.StationDao;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import org.hibernate.annotations.Type;

@Embeddable
public class CapabilityStationStatusDao {

  @Enumerated(EnumType.STRING)
  @Column(name = "soh_status", nullable = false, columnDefinition = "public.soh_status_enum")
  @Type(type = "pgsql_enum")
  private SohStatus stationSohStatus;

  @OneToOne(fetch= FetchType.LAZY)
  @JoinColumn(name = "station_name", referencedColumnName = "name")
  private StationDao stationDao;

  public SohStatus getStationSohStatus() {return this.stationSohStatus;}

  public void setStationSohStatus(SohStatus sohStatus) {this.stationSohStatus=sohStatus;}

  public StationDao getStationDao() {return this.stationDao;}

  public void setStationDao(StationDao stationDao) {this.stationDao=stationDao;}

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof CapabilityStationStatusDao)) {
      return false;
    }
    CapabilityStationStatusDao that = (CapabilityStationStatusDao) o;
    return Objects.equals(stationSohStatus, that.stationSohStatus) &&
        Objects.equals(stationDao.getName(), that.stationDao.getName());
  }

  @Override
  public int hashCode() {
    return Objects.hash(stationSohStatus, stationDao.getName());
  }

}
