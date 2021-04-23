package gms.shared.frameworks.osd.coi.event.repository.jpa;

import gms.shared.frameworks.osd.coi.event.Ellipse;
import gms.shared.frameworks.osd.coi.event.ScalingFactorType;
import gms.shared.frameworks.osd.coi.event.repository.jpa.util.ScalingFactorTypeConverter;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

/**
 * JPA data access object for {@link gms.shared.frameworks.osd.coi.event.Ellipse}
 */

@Entity
@Table(name = "ellipse")
public class EllipseDao {

  @Id
  @Column(name = "id")
  private UUID id;

  @Column(name = "scaling_factor_type", nullable = false)
  @Convert(converter = ScalingFactorTypeConverter.class)
  private ScalingFactorType scalingFactorType;

  @Column(name = "k_weight")
  private double kWeight;

  @Column(name = "confidence_level")
  private double confidenceLevel;

  @Column(name = "major_axis_length")
  private double majorAxisLength;

  @Column(name = "major_axis_trend")
  private double majorAxisTrend;

  @Column(name = "minor_axis_length")
  private double minorAxisLength;

  @Column(name = "minor_axis_trend")
  private double minorAxisTrend;

  @Column(name = "depth_uncertainty")
  private double depthUncertainty;

  @Column(name = "time_uncertainty", nullable = false)
  private Duration timeUncertainty;

  public EllipseDao() {
  }

  public EllipseDao(ScalingFactorType scalingFactorType, double kWeight, double confidenceLevel,
      double majorAxisLength, double majorAxisTrend,
      double minorAxisLength, double minorAxisTrend, double depthUncertainty,
      Duration timeUncertainty) {

    this.id = UUID.randomUUID();
    this.scalingFactorType = scalingFactorType;
    this.kWeight = kWeight;
    this.confidenceLevel = confidenceLevel;
    this.majorAxisLength = majorAxisLength;
    this.majorAxisTrend = majorAxisTrend;
    this.minorAxisLength = minorAxisLength;
    this.minorAxisTrend = minorAxisTrend;
    this.depthUncertainty = depthUncertainty;
    this.timeUncertainty = timeUncertainty;
  }

  /**
   * Create a DAO from the COI.
   * @param ellipse The COI object.
   */
  public EllipseDao(Ellipse ellipse) {
    Objects.requireNonNull(ellipse);
    this.id = UUID.randomUUID();
    this.scalingFactorType = ellipse.getScalingFactorType();
    this.kWeight = ellipse.getkWeight();
    this.confidenceLevel = ellipse.getConfidenceLevel();
    this.majorAxisLength = ellipse.getMajorAxisLength();
    this.majorAxisTrend = ellipse.getMajorAxisTrend();
    this.minorAxisLength = ellipse.getMinorAxisLength();
    this.minorAxisTrend = ellipse.getMinorAxisLength();
    this.depthUncertainty = ellipse.getDepthUncertainty();
    this.timeUncertainty = ellipse.getTimeUncertainty();
  }

  /**
   * Create a COI from this EllipseDao.
   *
   * @return an Ellipse object.
   */
  public Ellipse toCoi() {
    return Ellipse
        .from(this.scalingFactorType, this.kWeight, this.confidenceLevel, this.majorAxisLength,
            this.majorAxisTrend, this.minorAxisLength,
            this.minorAxisTrend, this.depthUncertainty, this.timeUncertainty);
  }

  public ScalingFactorType getScalingFactorType() {
    return scalingFactorType;
  }

  public void setScalingFactorType(
      ScalingFactorType scalingFactorType) {
    this.scalingFactorType = scalingFactorType;
  }

  public double getkWeight() {
    return kWeight;
  }

  public void setkWeight(double kWeight) {
    this.kWeight = kWeight;
  }

  public double getConfidenceLevel() {
    return confidenceLevel;
  }

  public void setConfidenceLevel(double confidenceLevel) {
    this.confidenceLevel = confidenceLevel;
  }

  public double getMajorAxisLength() {
    return majorAxisLength;
  }

  public void setMajorAxisLength(double majorAxisLength) {
    this.majorAxisLength = majorAxisLength;
  }

  public double getMajorAxisTrend() {
    return majorAxisTrend;
  }

  public void setMajorAxisTrend(double majorAxisTrend) {
    this.majorAxisTrend = majorAxisTrend;
  }

  public double getMinorAxisLength() {
    return minorAxisLength;
  }

  public void setMinorAxisLength(double minorAxisLength) {
    this.minorAxisLength = minorAxisLength;
  }

  public double getMinorAxisTrend() {
    return minorAxisTrend;
  }

  public void setMinorAxisTrend(double minorAxisTrend) {
    this.minorAxisTrend = minorAxisTrend;
  }

  public double getDepthUncertainty() {
    return depthUncertainty;
  }

  public void setDepthUncertainty(double depthUncertainty) {
    this.depthUncertainty = depthUncertainty;
  }

  public Duration getTimeUncertainty() {
    return timeUncertainty;
  }

  public void setTimeUncertainty(Duration timeUncertainty) {
    this.timeUncertainty = timeUncertainty;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    EllipseDao that = (EllipseDao) o;
    return id == that.id &&
        Double.compare(that.kWeight, kWeight) == 0 &&
        Double.compare(that.confidenceLevel, confidenceLevel) == 0 &&
        Double.compare(that.majorAxisLength, majorAxisLength) == 0 &&
        Double.compare(that.majorAxisTrend, majorAxisTrend) == 0 &&
        Double.compare(that.minorAxisLength, minorAxisLength) == 0 &&
        Double.compare(that.minorAxisTrend, minorAxisTrend) == 0 &&
        Double.compare(that.depthUncertainty, depthUncertainty) == 0 &&
        scalingFactorType == that.scalingFactorType &&
        timeUncertainty.equals(that.timeUncertainty);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, scalingFactorType, kWeight, confidenceLevel, majorAxisLength,
        majorAxisTrend, minorAxisLength, minorAxisTrend, depthUncertainty, timeUncertainty);
  }

  @Override
  public String toString() {
    return "EllipseDao{" +
        "pk=" + id +
        ", scalingFactorType=" + scalingFactorType +
        ", kWeight=" + kWeight +
        ", confidenceLevel=" + confidenceLevel +
        ", majorAxisLength=" + majorAxisLength +
        ", majorAxisTrend=" + majorAxisTrend +
        ", minorAxisLength=" + minorAxisLength +
        ", minorAxisTrend=" + minorAxisTrend +
        ", depthUncertainty=" + depthUncertainty +
        ", timeUncertainty=" + timeUncertainty +
        '}';
  }
}
