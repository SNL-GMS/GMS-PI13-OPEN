package gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.dataaccessobjects;

import gms.shared.frameworks.osd.coi.channel.repository.jpa.dataaccessobjects.LocationDao;
import gms.shared.frameworks.osd.coi.signaldetection.Station;
import gms.shared.frameworks.osd.coi.stationreference.StationType;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "station")
public class StationDao {

  @Id
  @Column(name = "name", nullable = false, unique = true)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(name = "station_type")
  private StationType type;

  @Column(name = "description", length = 1024, nullable = false)
  private String description;

  @Embedded
  private LocationDao location;

  public StationDao() {
  }

  private StationDao(
      String name,
      StationType type,
      String description,
      LocationDao location) {
    this.name = name;
    this.type = type;
    this.description = description;
    this.location = location;
  }

  public static StationDao from(Station station) {
    return new StationDao(station.getName(), station.getType(),
        station.getDescription(), new LocationDao(station.getLocation()));
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public StationType getType() {
    return type;
  }

  public void setType(
      StationType type) {
    this.type = type;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public LocationDao getLocation() {
    return location;
  }

  public void setLocation(
      LocationDao location) {
    this.location = location;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof StationDao)) {
      return false;
    }
    StationDao that = (StationDao) o;
    return Objects.equals(name, that.name) &&
        type == that.type &&
        Objects.equals(description, that.description) &&
        Objects.equals(location, that.location);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, type, description, location);
  }

  @Override
  public String toString() {
    return "StationDao{" +
        "name='" + name + '\'' +
        ", type=" + type +
        ", description='" + description + '\'' +
        ", location=" + location +
        '}';
  }
}
