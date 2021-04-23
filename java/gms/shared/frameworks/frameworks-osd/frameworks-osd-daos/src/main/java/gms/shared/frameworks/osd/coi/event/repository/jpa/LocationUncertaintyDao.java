package gms.shared.frameworks.osd.coi.event.repository.jpa;

import gms.shared.frameworks.osd.coi.event.LocationUncertainty;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * JPA data access object for {@link gms.shared.frameworks.osd.coi.event.LocationUncertainty}
 */

@Entity
@Table(name = "location_uncertainty")
public class LocationUncertaintyDao {

  @Id
  @Column(name = "id")
  private UUID id;

  @Column(name = "xx")
  private double xx;

  @Column(name = "xy")
  private double xy;

  @Column(name = "xz")
  private double xz;

  @Column(name = "xt")
  private double xt;

  @Column(name = "yy")
  private double yy;

  @Column(name = "yz")
  private double yz;

  @Column(name = "yt")
  private double yt;

  @Column(name = "zz")
  private double zz;

  @Column(name = "zt")
  private double zt;

  @Column(name = "tt")
  private double tt;

  @Column(name = "st_dev_one_observation")
  private double stDevOneObservation;

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "location_uncertainty_id", referencedColumnName = "id")
  private Set<EllipseDao> ellipses;

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "location_uncertainty_id", referencedColumnName = "id")
  private Set<EllipsoidDao> ellipsoids;

  public LocationUncertaintyDao() {
  }

  /**
   * Create a LocationUncertainty DAO.
   */
  public LocationUncertaintyDao(double xx, double xy, double xz, double xt, double yy,
      double yz, double yt, double zz, double zt, double tt, double stDevOneObservation,
      Set<EllipseDao> ellipses, Set<EllipsoidDao> ellipsoids) {
    this.id = UUID.randomUUID();
    this.xx = xx;
    this.xy = xy;
    this.xz = xz;
    this.xt = xt;
    this.yy = yy;
    this.yz = yz;
    this.yt = yt;
    this.zz = zz;
    this.zt = zt;
    this.tt = tt;
    this.stDevOneObservation = stDevOneObservation;
    this.ellipses = ellipses;
    this.ellipsoids = ellipsoids;
  }

  /**
   * Create a DAO from the COI.
   * @param locationUncertainty
   */
  public LocationUncertaintyDao(LocationUncertainty locationUncertainty) {
    this.id = UUID.randomUUID();
    this.xx = locationUncertainty.getXx();
    this.xy = locationUncertainty.getXy();
    this.xz = locationUncertainty.getXz();
    this.xt = locationUncertainty.getXt();
    this.yy = locationUncertainty.getYy();
    this.yz = locationUncertainty.getYz();
    this.yt = locationUncertainty.getYt();
    this.zz = locationUncertainty.getZz();
    this.zt = locationUncertainty.getZt();
    this.tt = locationUncertainty.getTt();
    this.stDevOneObservation = locationUncertainty.getStDevOneObservation();
    this.ellipses = locationUncertainty.getEllipses().stream()
        .map(EllipseDao::new).collect(Collectors.toSet());
    this.ellipsoids = locationUncertainty.getEllipsoids().stream()
        .map(EllipsoidDao::new).collect(Collectors.toSet());
  }

  /**
   * Create a COI from this DTO.
   *
   * @return a LocationUncertainty object.
   */
  public LocationUncertainty toCoi() {
    return LocationUncertainty.from(xx, xy, xz, xt, yy, yz, yt, zz, zt, tt,
        stDevOneObservation,
        ellipses.stream().map(EllipseDao::toCoi).collect(Collectors.toSet()),
        ellipsoids.stream().map(EllipsoidDao::toCoi).collect(Collectors.toSet()));
  }

  public double getXx() {
    return xx;
  }

  public void setXx(double xx) {
    this.xx = xx;
  }

  public double getXy() {
    return xy;
  }

  public void setXy(double xy) {
    this.xy = xy;
  }

  public double getXz() {
    return xz;
  }

  public void setXz(double xz) {
    this.xz = xz;
  }

  public double getXt() {
    return xt;
  }

  public void setXt(double xt) {
    this.xt = xt;
  }

  public double getYy() {
    return yy;
  }

  public void setYy(double yy) {
    this.yy = yy;
  }

  public double getYz() {
    return yz;
  }

  public void setYz(double yz) {
    this.yz = yz;
  }

  public double getYt() {
    return yt;
  }

  public void setYt(double yt) {
    this.yt = yt;
  }

  public double getZz() {
    return zz;
  }

  public void setZz(double zz) {
    this.zz = zz;
  }

  public double getZt() {
    return zt;
  }

  public void setZt(double zt) {
    this.zt = zt;
  }

  public double getTt() {
    return tt;
  }

  public void setTt(double tt) {
    this.tt = tt;
  }

  public double getStDevOneObservation() {
    return stDevOneObservation;
  }

  public void setStDevOneObservation(double stDevOneObservation) {
    this.stDevOneObservation = stDevOneObservation;
  }

  public Set<EllipseDao> getEllipses() {
    return ellipses;
  }

  public void setEllipses(
      Set<EllipseDao> ellipses) {
    this.ellipses = ellipses;
  }

  public Set<EllipsoidDao> getEllipsoids() {
    return ellipsoids;
  }

  public void setEllipsoids(
      Set<EllipsoidDao> ellipsoids) {
    this.ellipsoids = ellipsoids;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    LocationUncertaintyDao that = (LocationUncertaintyDao) o;
    return id == that.id &&
        Double.compare(that.xx, xx) == 0 &&
        Double.compare(that.xy, xy) == 0 &&
        Double.compare(that.xz, xz) == 0 &&
        Double.compare(that.xt, xt) == 0 &&
        Double.compare(that.yy, yy) == 0 &&
        Double.compare(that.yz, yz) == 0 &&
        Double.compare(that.yt, yt) == 0 &&
        Double.compare(that.zz, zz) == 0 &&
        Double.compare(that.zt, zt) == 0 &&
        Double.compare(that.tt, tt) == 0 &&
        Double.compare(that.stDevOneObservation, stDevOneObservation) == 0 &&
        Objects.equals(ellipses, that.ellipses) &&
        Objects.equals(ellipsoids, that.ellipsoids);
  }

  @Override
  public int hashCode() {
    return Objects
        .hash(id, xx, xy, xz, xt, yy, yz, yt, zz, zt, tt, stDevOneObservation, ellipses,
            ellipsoids);
  }

  @Override
  public String toString() {
    return "LocationUncertaintyDao{" +
        "id=" + id +
        ", xx=" + xx +
        ", xy=" + xy +
        ", xz=" + xz +
        ", xt=" + xt +
        ", yy=" + yy +
        ", yz=" + yz +
        ", yt=" + yt +
        ", zz=" + zz +
        ", zt=" + zt +
        ", tt=" + tt +
        ", stDevOneObservation=" + stDevOneObservation +
        ", ellipses=" + ellipses +
        ", ellipsoids=" + ellipsoids +
        '}';
  }
}
