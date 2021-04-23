package gms.shared.frameworks.osd.coi.channel.repository.jpa.dataaccessobjects;

import com.google.common.base.Preconditions;
import gms.shared.frameworks.osd.coi.signaldetection.Location;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class LocationDao {
  @Column(name = "latitude", nullable = false)
  private double latitude;

  @Column(name = "longitude", nullable = false)
  private double longitude;

  @Column(name = "depth", nullable = false)
  private double depth;

  @Column(name = "elevation", nullable = false)
  private double elevation;

  public LocationDao() {
  }

  public LocationDao(Location location) {
    if(location != null) {
      this.latitude = location.getLatitudeDegrees();
      this.longitude = location.getLongitudeDegrees();
      this.depth = location.getDepthKm();
      this.elevation = location.getElevationKm();
    }
  }

  /**
   * Create a DAO from the COI {@link Location}.
   * @param location the location to convert
   * @return The Location converted to its DAO format
   */
  public static LocationDao from(Location location) {
    Preconditions.checkNotNull(location, "Cannot create dao from null Location");
    return new LocationDao(location);
  }

  public Location toCoi() {
    return Location.from(this.latitude, this.longitude, this.depth, this.elevation);
  }

  public double getLatitude() {
    return latitude;
  }

  public void setLatitude(double latitude) {
    this.latitude = latitude;
  }

  public double getLongitude() {
    return longitude;
  }

  public void setLongitude(double longitude) {
    this.longitude = longitude;
  }

  public double getDepth() {
    return depth;
  }

  public void setDepth(double depth) {
    this.depth = depth;
  }

  public double getElevation() {
    return elevation;
  }

  public void setElevation(double elevation) {
    this.elevation = elevation;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof LocationDao)) {
      return false;
    }
    LocationDao that = (LocationDao) o;
    return Double.compare(that.latitude, latitude) == 0 &&
        Double.compare(that.longitude, longitude) == 0 &&
        Double.compare(that.depth, depth) == 0 &&
        Double.compare(that.elevation, elevation) == 0;
  }

  @Override
  public int hashCode() {
    return Objects.hash(latitude, longitude, depth, elevation);
  }

  @Override
  public String toString() {
    return "LocationDao{" +
        "latitude=" + latitude +
        ", longitude=" + longitude +
        ", depth=" + depth +
        ", elevation=" + elevation +
        '}';
  }
}
