package gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.dataaccessobjects;

import gms.shared.frameworks.osd.coi.channel.repository.jpa.dataaccessobjects.ChannelDao;
import gms.shared.frameworks.osd.coi.signaldetection.DurationMeasurementValue;
import gms.shared.frameworks.osd.coi.signaldetection.FeatureMeasurement;
import java.util.Objects;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity(name = "feature_measurement_duration")
@Table(name = "feature_measurement_duration")
public class DurationFeatureMeasurementDao extends FeatureMeasurementDao<DurationMeasurementValue> {

  @Embedded
  private DurationMeasurementValueDao value;

  public DurationFeatureMeasurementDao() {
  }

  public DurationFeatureMeasurementDao(String id,
      FeatureMeasurement<DurationMeasurementValue> featureMeasurement,
      MeasuredChannelSegmentDescriptorDao descriptor) {
    super(id, featureMeasurement, descriptor);

    value = new DurationMeasurementValueDao(featureMeasurement.getMeasurementValue());
  }

  @Override
  public DurationMeasurementValue toCoiMeasurementValue() {
    return this.value.toCoi();
  }

  public DurationMeasurementValueDao getValue() {
    return value;
  }

  public void setValue(
      DurationMeasurementValueDao value) {
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
    DurationFeatureMeasurementDao that = (DurationFeatureMeasurementDao) o;
    return Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), value);
  }

  @Override
  public String toString() {
    return "DurationFeatureMeasurementDao{" +
        "value=" + value +
        '}';
  }
}
