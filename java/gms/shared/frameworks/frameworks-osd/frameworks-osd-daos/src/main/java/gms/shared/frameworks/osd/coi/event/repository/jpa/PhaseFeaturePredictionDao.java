package gms.shared.frameworks.osd.coi.event.repository.jpa;

import gms.shared.frameworks.osd.coi.event.FeaturePrediction;
import gms.shared.frameworks.osd.coi.signaldetection.EnumeratedMeasurementValue;
import gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.dataaccessobjects.PhaseTypeMeasurementValueDao;
import java.util.Objects;
import java.util.Optional;
import javax.persistence.AttributeOverride;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;

@Entity(name = "feature_prediction_phase")
public class PhaseFeaturePredictionDao extends FeaturePredictionDao<EnumeratedMeasurementValue.PhaseTypeMeasurementValue> {

  @Embedded
  @AttributeOverride(name = "phase", column = @Column(name = "predicted_phase"))
  private PhaseTypeMeasurementValueDao value;

  public PhaseFeaturePredictionDao() {
    // Empty constructor needed for JPA
  }

  @Override
  public Optional<EnumeratedMeasurementValue.PhaseTypeMeasurementValue> toCoiPredictionValue() {
    return Optional.ofNullable(value).map(PhaseTypeMeasurementValueDao::toCoi);
  }

  public PhaseTypeMeasurementValueDao getValue() {
    return value;
  }

  public void setValue(
      PhaseTypeMeasurementValueDao value) {
    this.value = value;
  }

  @Override
  public boolean update(FeaturePrediction<EnumeratedMeasurementValue.PhaseTypeMeasurementValue> updatedValue) {

    Optional<EnumeratedMeasurementValue.PhaseTypeMeasurementValue> predictedValueOptional = updatedValue.getPredictedValue();

    if (predictedValueOptional.isPresent()) {

      if (Objects.nonNull(this.value)) {

        return value.update(predictedValueOptional.get());
      } else {

        this.value = new PhaseTypeMeasurementValueDao(predictedValueOptional.get());
        return true;
      }
    } else {

      if (Objects.isNull(this.value)) {

        return false;
      } else {

        this.value = null;
        return true;
      }
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PhaseFeaturePredictionDao that = (PhaseFeaturePredictionDao) o;
    return Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }

  @Override
  public String toString() {
    return "PhaseFeaturePredictionDao{" +
        "value=" + value +
        '}';
  }
}
