package gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.dataaccessobjects;

import java.util.Objects;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "signal_detection")
public class SignalDetectionDao {

  @Id
  @Column(name = "id")
  private UUID id;

  @Column(name = "monitoring_organization")
  private String monitoringOrganization;

  @ManyToOne(optional = false)
  @JoinColumn(name = "station_name", referencedColumnName = "name")
  private StationDao station;

  public SignalDetectionDao(){}

  public SignalDetectionDao(UUID id, String monitoringOrganization, StationDao station) {
    this.id = id;
    this.monitoringOrganization = monitoringOrganization;
    this.station = station;
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getMonitoringOrganization() {
    return monitoringOrganization;
  }

  public void setMonitoringOrganization(String monitoringOrganization) {
    this.monitoringOrganization = monitoringOrganization;
  }

  public StationDao getStation() {
    return station;
  }

  public void setStation(StationDao station) {
    this.station = station;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    SignalDetectionDao that = (SignalDetectionDao) o;

    if (!id.equals(that.id)) {
      return false;
    }
    if (!monitoringOrganization.equals(that.monitoringOrganization)) {
      return false;
    }

    return station.equals(that.station);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, monitoringOrganization, station);
  }

  @Override
  public String toString() {
    return "SignalDetectionDao{" +
        ", signalDetectionId=" + id +
        ", monitoringOrganization='" + monitoringOrganization + '\'' +
        ", stationName=" + station.getName() +
        '}';
  }
}
