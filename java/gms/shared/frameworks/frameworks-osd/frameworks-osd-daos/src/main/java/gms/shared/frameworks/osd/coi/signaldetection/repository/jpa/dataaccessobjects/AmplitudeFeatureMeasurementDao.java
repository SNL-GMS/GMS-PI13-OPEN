package gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.dataaccessobjects;

import gms.shared.frameworks.osd.coi.channel.repository.jpa.dataaccessobjects.ChannelDao;
import gms.shared.frameworks.osd.coi.signaldetection.AmplitudeMeasurementValue;
import gms.shared.frameworks.osd.coi.signaldetection.FeatureMeasurement;
import java.util.Objects;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity(name = "feature_measurement_amplitude")
@Table(name = "feature_measurement_amplitude")
public class AmplitudeFeatureMeasurementDao extends FeatureMeasurementDao<AmplitudeMeasurementValue> {

  @Embedded
  private AmplitudeMeasurementValueDao value;

  public AmplitudeFeatureMeasurementDao() {}

  public AmplitudeFeatureMeasurementDao(String id,
      FeatureMeasurement<AmplitudeMeasurementValue> featureMeasurement,
      MeasuredChannelSegmentDescriptorDao descriptor) {
    super(id, featureMeasurement, descriptor);

    value = new AmplitudeMeasurementValueDao(featureMeasurement.getMeasurementValue());
  }

  @Override
  public AmplitudeMeasurementValue toCoiMeasurementValue() {
    return this.value.toCoi();
  }

  public AmplitudeMeasurementValueDao getValue() {
    return value;
  }

  public void setValue(
      AmplitudeMeasurementValueDao value) {
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
    AmplitudeFeatureMeasurementDao that = (AmplitudeFeatureMeasurementDao) o;
    return Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), value);
  }

  @Override
  public String toString() {
    return "AmplitudeFeatureMeasurementDao{" +
        "value=" + value +
        '}';
  }

}
