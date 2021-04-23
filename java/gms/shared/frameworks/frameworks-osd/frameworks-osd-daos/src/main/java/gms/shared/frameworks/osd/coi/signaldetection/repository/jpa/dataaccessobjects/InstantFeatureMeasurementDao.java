package gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.dataaccessobjects;

import gms.shared.frameworks.osd.coi.InstantValue;
import gms.shared.frameworks.osd.coi.channel.repository.jpa.dataaccessobjects.ChannelDao;
import gms.shared.frameworks.osd.coi.signaldetection.FeatureMeasurement;
import java.util.Objects;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity(name = "feature_measurement_instant")
@Table(name = "feature_measurement_instant")
public class InstantFeatureMeasurementDao extends FeatureMeasurementDao<InstantValue> {

  @Embedded
  private InstantValueDao value;

  public InstantFeatureMeasurementDao() {
  }

  public InstantFeatureMeasurementDao(String id,
      FeatureMeasurement<InstantValue> featureMeasurement,
      MeasuredChannelSegmentDescriptorDao descriptor) {
    super(id, featureMeasurement, descriptor);

    value = new InstantValueDao(featureMeasurement.getMeasurementValue());
  }

  @Override
  public InstantValue toCoiMeasurementValue() {
    return this.value.toCoi();
  }

  public InstantValueDao getValue() {
    return value;
  }

  public void setValue(
      InstantValueDao value) {
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
    InstantFeatureMeasurementDao that = (InstantFeatureMeasurementDao) o;
    return Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), value);
  }

  @Override
  public String toString() {
    return "InstantFeatureMeasurementDao{" +
        "value=" + value +
        '}';
  }
}
