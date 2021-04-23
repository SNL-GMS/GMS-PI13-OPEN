package gms.shared.frameworks.osd.coi.event.repository.jpa;

import gms.shared.frameworks.osd.coi.event.DepthRestraintType;
import gms.shared.frameworks.osd.coi.event.LocationRestraint;
import gms.shared.frameworks.osd.coi.event.RestraintType;
import gms.shared.frameworks.osd.coi.event.repository.jpa.util.DepthRestraintTypeConverter;
import gms.shared.frameworks.osd.coi.event.repository.jpa.util.RestraintTypeConverter;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * JPA data access object for
 * {@link gms.shared.frameworks.osd.coi.event.LocationRestraint}
 */
@Entity
@Table(name = "location_restraint")
public class LocationRestraintDao {

  @Id
  @Column(name = "id")
  private UUID id;

  @Column(name="latitude_restraint_type", nullable = false)
  @Convert(converter = RestraintTypeConverter.class)
  private RestraintType latitudeRestraintType;

  @Column(name="latitude_restraint_degrees")
  private Double latitudeRestraintDegrees;

  @Column(name="longitude_restraint_type", nullable = false)
  @Convert(converter = RestraintTypeConverter.class)
  private RestraintType longitudeRestraintType;

  @Column(name="longitude_restraint_degrees")
  private Double longitudeRestraintDegrees;

  @Column(name="depth_restraint_type", nullable = false)
  @Convert(converter = DepthRestraintTypeConverter.class)
  private DepthRestraintType depthRestraintType;

  @Column(name="depth_restraint_km")
  private Double depthRestraintKm;

  @Column(name="time_restraint_type", nullable = false)
  @Convert(converter = RestraintTypeConverter.class)
  private RestraintType timeRestraintType;

  @Column(name="time_restraint")
  private Instant timeRestraint;

  /**
   * Default constructor for JPA.
   */
  public LocationRestraintDao() {
  }


  /**
   * Create a DAO from a COI object.
   * @param locationRestraint
   */
  public LocationRestraintDao(LocationRestraint locationRestraint) {
    Objects.requireNonNull(locationRestraint);
    this.id = UUID.randomUUID();
    this.latitudeRestraintType = locationRestraint.getLatitudeRestraintType();
    this.latitudeRestraintDegrees = locationRestraint.getLatitudeRestraintDegrees().isPresent() ? locationRestraint.getLatitudeRestraintDegrees().get() : null;
    this.longitudeRestraintType = locationRestraint.getLongitudeRestraintType();
    this.longitudeRestraintDegrees = locationRestraint.getLongitudeRestraintDegrees().isPresent() ? locationRestraint.getLongitudeRestraintDegrees().get() : null;
    this.depthRestraintType = locationRestraint.getDepthRestraintType();
    this.depthRestraintKm = locationRestraint.getDepthRestraintKm().isPresent() ? locationRestraint.getDepthRestraintKm().get() : null;
    this.timeRestraintType = locationRestraint.getTimeRestraintType();
    this.timeRestraint = locationRestraint.getTimeRestraint().isPresent() ? locationRestraint.getTimeRestraint().get() : null;
  }

  /**
   * Create a COI from this DAO.
   * @return LocationRestraint object.
   */
  public LocationRestraint toCoi() {
    return LocationRestraint.from(
        this.latitudeRestraintType,
        this.latitudeRestraintDegrees,
        this.longitudeRestraintType,
        this.longitudeRestraintDegrees,
        this.depthRestraintType,
        this.depthRestraintKm,
        this.timeRestraintType,
        this.timeRestraint);
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public RestraintType getLatitudeRestraintType() {
    return latitudeRestraintType;
  }

  public void setLatitudeRestraintType(
      RestraintType latitudeRestraintType) {
    this.latitudeRestraintType = latitudeRestraintType;
  }

  public Double getLatitudeRestraintDegrees() {
    return latitudeRestraintDegrees;
  }

  public void setLatitudeRestraintDegrees(Double latitudeRestraintDegrees) {
    this.latitudeRestraintDegrees = latitudeRestraintDegrees;
  }

  public RestraintType getLongitudeRestraintType() {
    return longitudeRestraintType;
  }

  public void setLongitudeRestraintType(
      RestraintType longitudeRestraintType) {
    this.longitudeRestraintType = longitudeRestraintType;
  }

  public Double getLongitudeRestraintDegrees() {
    return longitudeRestraintDegrees;
  }

  public void setLongitudeRestraintDegrees(Double longitudeRestraintDegrees) {
    this.longitudeRestraintDegrees = longitudeRestraintDegrees;
  }

  public DepthRestraintType getDepthRestraintType() {
    return depthRestraintType;
  }

  public void setDepthRestraintType(
      DepthRestraintType depthRestraintType) {
    this.depthRestraintType = depthRestraintType;
  }

  public Double getDepthRestraintKm() {
    return depthRestraintKm;
  }

  public void setDepthRestraintKm(Double depthRestraintKm) {
    this.depthRestraintKm = depthRestraintKm;
  }

  public RestraintType getTimeRestraintType() {
    return timeRestraintType;
  }

  public void setTimeRestraintType(
      RestraintType timeRestraintType) {
    this.timeRestraintType = timeRestraintType;
  }

  public Instant getTimeRestraint() {
    return timeRestraint;
  }

  public void setTimeRestraint(Instant timeRestraint) {
    this.timeRestraint = timeRestraint;
  }

  @Override
  public String toString() {
    return "LocationRestraintDao{" +
        "primaryKey=" + id +
        ", latitudeRestraintType=" + latitudeRestraintType +
        ", latitudeRestraintDegrees=" + latitudeRestraintDegrees +
        ", longitudeRestraintType=" + longitudeRestraintType +
        ", longitudeRestraintDegrees=" + longitudeRestraintDegrees +
        ", depthRestraintType=" + depthRestraintType +
        ", depthRestraintKm=" + depthRestraintKm +
        ", timeRestraintType=" + timeRestraintType +
        ", timeRestraint=" + timeRestraint +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    LocationRestraintDao that = (LocationRestraintDao) o;
    return id == that.id &&
        latitudeRestraintType == that.latitudeRestraintType &&
        Objects.equals(latitudeRestraintDegrees, that.latitudeRestraintDegrees) &&
        longitudeRestraintType == that.longitudeRestraintType &&
        Objects.equals(longitudeRestraintDegrees, that.longitudeRestraintDegrees) &&
        depthRestraintType == that.depthRestraintType &&
        Objects.equals(depthRestraintKm, that.depthRestraintKm) &&
        timeRestraintType == that.timeRestraintType &&
        Objects.equals(timeRestraint, that.timeRestraint);
  }

  @Override
  public int hashCode() {
    return Objects
        .hash(id, latitudeRestraintType, latitudeRestraintDegrees, longitudeRestraintType,
            longitudeRestraintDegrees, depthRestraintType, depthRestraintKm, timeRestraintType,
            timeRestraint);
  }
}
