package gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.dataaccessobjects;

import gms.shared.frameworks.osd.coi.channel.repository.jpa.dataaccessobjects.ChannelDao;
import gms.shared.frameworks.osd.coi.signaldetection.FeatureMeasurement;
import gms.shared.frameworks.osd.coi.signaldetection.NumericMeasurementValue;
import java.util.Objects;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity(name = "feature_measurement_numeric")
@Table(name = "feature_measurement_numeric")
public class NumericFeatureMeasurementDao extends FeatureMeasurementDao<NumericMeasurementValue> {

  @Embedded
  private NumericMeasurementValueDao value;

  public NumericFeatureMeasurementDao() {
  }

  public NumericFeatureMeasurementDao(String id,
      FeatureMeasurement<NumericMeasurementValue> featureMeasurement,
      MeasuredChannelSegmentDescriptorDao descriptor) {
    super(id, featureMeasurement, descriptor);

    value = new NumericMeasurementValueDao(featureMeasurement.getMeasurementValue());
  }

  @Override
  public NumericMeasurementValue toCoiMeasurementValue() {
    return this.value.toCoi();
  }

  public NumericMeasurementValueDao getValue() {
    return value;
  }

  public void setValue(
      NumericMeasurementValueDao value) {
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
    NumericFeatureMeasurementDao that = (NumericFeatureMeasurementDao) o;
    return Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), value);
  }

  @Override
  public String toString() {
    return "NumericFeatureMeasurementDao{" +
        "value=" + value +
        '}';
  }
}
