package gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.dataaccessobjects;

import gms.shared.frameworks.osd.coi.signaldetection.EnumeratedMeasurementValue.PhaseTypeMeasurementValue;
import gms.shared.frameworks.osd.coi.signaldetection.FeatureMeasurement;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.Objects;

@Entity(name = "feature_measurement_phase")
@Table(name = "feature_measurement_phase")
public class PhaseFeatureMeasurementDao extends FeatureMeasurementDao<PhaseTypeMeasurementValue> {

  @Embedded
  private PhaseTypeMeasurementValueDao value;

  public PhaseFeatureMeasurementDao() {}

  public PhaseFeatureMeasurementDao(String id,
      FeatureMeasurement<PhaseTypeMeasurementValue> featureMeasurement,
      MeasuredChannelSegmentDescriptorDao descriptor) {
    super(id, featureMeasurement, descriptor);

    value = new PhaseTypeMeasurementValueDao(featureMeasurement.getMeasurementValue());
  }

  @Override
  public PhaseTypeMeasurementValue toCoiMeasurementValue() {
    return this.value.toCoi();
  }

  public PhaseTypeMeasurementValueDao getValue() {
    return value;
  }

  public void setValue(
      PhaseTypeMeasurementValueDao value) {
    this.value = value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    PhaseFeatureMeasurementDao that = (PhaseFeatureMeasurementDao) o;
    return Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), value);
  }

  @Override
  public String toString() {
    return "PhaseFeatureMeasurementDao{" +
        "value=" + value +
        '}';
  }
}
