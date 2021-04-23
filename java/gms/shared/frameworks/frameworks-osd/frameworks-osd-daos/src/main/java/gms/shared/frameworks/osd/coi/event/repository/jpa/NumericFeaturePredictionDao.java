package gms.shared.frameworks.osd.coi.event.repository.jpa;

import gms.shared.frameworks.osd.coi.event.FeaturePrediction;
import gms.shared.frameworks.osd.coi.signaldetection.NumericMeasurementValue;
import gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.dataaccessobjects.NumericMeasurementValueDao;
import java.util.Objects;
import java.util.Optional;
import javax.persistence.Embedded;
import javax.persistence.Entity;

@Entity(name = "feature_prediction_numeric_value")
public class NumericFeaturePredictionDao extends FeaturePredictionDao<NumericMeasurementValue> {

  @Embedded
  private NumericMeasurementValueDao value;

  public NumericFeaturePredictionDao() {
    // Empty constructor needed for JPA
  }

  @Override
  public Optional<NumericMeasurementValue> toCoiPredictionValue() {
    return Optional.ofNullable(value).map(NumericMeasurementValueDao::toCoi);
  }

  public NumericMeasurementValueDao getValue() {
    return value;
  }

  public void setValue(
      NumericMeasurementValueDao value) {
    this.value = value;
  }

  @Override
  public boolean update(FeaturePrediction<NumericMeasurementValue> updatedValue) {

    Optional<NumericMeasurementValue> predictedValueOptional = updatedValue.getPredictedValue();

    if (predictedValueOptional.isPresent()) {

      if (Objects.nonNull(this.value)) {

        return value.update(predictedValueOptional.get());
      } else {

        this.value = new NumericMeasurementValueDao(predictedValueOptional.get());
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
    NumericFeaturePredictionDao that = (NumericFeaturePredictionDao) o;
    return Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }

  @Override
  public String toString() {
    return "NumericFeaturePredictionDao{" +
        "value=" + value +
        '}';
  }
}
