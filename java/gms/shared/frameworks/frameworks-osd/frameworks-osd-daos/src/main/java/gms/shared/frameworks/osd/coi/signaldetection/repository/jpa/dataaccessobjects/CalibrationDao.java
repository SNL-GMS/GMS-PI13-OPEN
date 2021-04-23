package gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.dataaccessobjects;


import com.google.common.base.Preconditions;
import gms.shared.frameworks.osd.coi.signaldetection.Calibration;
import java.time.Duration;
import java.util.Objects;
import javax.persistence.AttributeOverride;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * JPA data access object for {@link Calibration}
 */

@Entity
@Table(name = "calibration")
public class CalibrationDao {

  @Id
  @GeneratedValue
  private long id;

  @Column(name = "calibration_period_sec", nullable = false)
  private double calibrationPeriodSec;

  @Column(name = "calibration_time_shift", nullable = false)
  private Duration calibrationTimeShift;

  @Embedded
  @AttributeOverride(name = "value", column = @Column(name = "calibration_factor_value", nullable = false))
  @AttributeOverride(name = "standardDeviation", column = @Column(name = "calibration_factor_error", nullable = false))
  @AttributeOverride(name = "units", column = @Column(name = "calibration_factor_units", nullable = false))
  private DoubleValueDao calibrationFactor;

  protected CalibrationDao() {
  }

  public CalibrationDao(
      double calibrationPeriodSec,
      Duration calibrationTimeShift,
      DoubleValueDao calibrationFactor) {

    this.calibrationPeriodSec = calibrationPeriodSec;
    this.calibrationTimeShift = calibrationTimeShift;
    this.calibrationFactor = calibrationFactor;
  }

  /**
   * Create a DAO from a COI
   */
  public static CalibrationDao from(Calibration calibration) {
    Preconditions
        .checkNotNull(calibration, "Cannot create CalibrationDao from null Calibration");

    return new CalibrationDao(
        calibration.getCalibrationPeriodSec(),
        calibration.getCalibrationTimeShift(),
        new DoubleValueDao(calibration.getCalibrationFactor()));
  }

  /**
   * Create a COI from a DAO
   */
  public Calibration toCoi() {

    return Calibration
        .from(this.calibrationPeriodSec,
            this.calibrationTimeShift,
            this.calibrationFactor.toCoi());
  }

  public double getCalibrationPeriodSec() {
    return calibrationPeriodSec;
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public void setCalibrationPeriodSec(double calibrationPeriodSec) {
    this.calibrationPeriodSec = calibrationPeriodSec;
  }

  public Duration getCalibrationTimeShift() {
    return calibrationTimeShift;
  }

  public void setCalibrationTimeShift(Duration calibrationTimeShift) {
    this.calibrationTimeShift = calibrationTimeShift;
  }

  public DoubleValueDao getCalibrationFactor() {
    return calibrationFactor;
  }

  public void setCalibrationFactor(
      DoubleValueDao calibrationFactor) {
    this.calibrationFactor = calibrationFactor;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CalibrationDao that = (CalibrationDao) o;
    return id == that.id &&
        Double.compare(that.calibrationPeriodSec, calibrationPeriodSec) == 0 &&
        calibrationTimeShift.equals(that.calibrationTimeShift) &&
        calibrationFactor.equals(that.calibrationFactor);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, calibrationPeriodSec, calibrationTimeShift, calibrationFactor);
  }

  @Override
  public String toString() {
    return "CalibrationDao{" +
        "id=" + id +
        ", calibrationPeriodSec=" + calibrationPeriodSec +
        ", calibrationTimeShift=" + calibrationTimeShift +
        ", calibrationFactor=" + calibrationFactor +
        '}';
  }
}
