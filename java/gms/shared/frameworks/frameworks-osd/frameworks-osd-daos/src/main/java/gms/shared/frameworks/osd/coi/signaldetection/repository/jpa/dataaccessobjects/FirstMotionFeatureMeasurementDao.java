package gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.dataaccessobjects;

import gms.shared.frameworks.osd.coi.channel.repository.jpa.dataaccessobjects.ChannelDao;
import gms.shared.frameworks.osd.coi.signaldetection.EnumeratedMeasurementValue.FirstMotionMeasurementValue;
import gms.shared.frameworks.osd.coi.signaldetection.FeatureMeasurement;
import java.util.Objects;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity(name = "feature_measurement_first_motion")
@Table(name = "feature_measurement_first_motion")
public class FirstMotionFeatureMeasurementDao extends FeatureMeasurementDao<FirstMotionMeasurementValue> {

  @Embedded
  private FirstMotionMeasurementValueDao value;

  public FirstMotionFeatureMeasurementDao() {}

  public FirstMotionFeatureMeasurementDao(String id,
      FeatureMeasurement<FirstMotionMeasurementValue> featureMeasurement,
      MeasuredChannelSegmentDescriptorDao descriptor) {
    super(id, featureMeasurement, descriptor);

    value = new FirstMotionMeasurementValueDao(featureMeasurement.getMeasurementValue());
  }

  @Override
  public FirstMotionMeasurementValue toCoiMeasurementValue() {
    return this.value.toCoi();
  }

  public FirstMotionMeasurementValueDao getValue() {
    return value;
  }

  public void setValue(
      FirstMotionMeasurementValueDao value) {
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
    FirstMotionFeatureMeasurementDao that = (FirstMotionFeatureMeasurementDao) o;
    return Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), value);
  }

  @Override
  public String toString() {
    return "FirstMotionFeatureMeasurementDao{" +
        "value=" + value +
        '}';
  }
}
