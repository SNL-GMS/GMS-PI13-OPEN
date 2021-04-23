package gms.shared.frameworks.osd.coi.event.repository.jpa;

import gms.shared.frameworks.osd.coi.InstantValue;
import gms.shared.frameworks.osd.coi.event.FeaturePrediction;
import gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.dataaccessobjects.InstantValueDao;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import java.util.Objects;
import java.util.Optional;

@Entity(name = "feature_prediction_duration_value")
public class InstantFeaturePredictionDao extends FeaturePredictionDao<InstantValue> {

  @Embedded
  private InstantValueDao value;

  public InstantFeaturePredictionDao() {
    // Empty constructor needed for JPA
  }

  @Override
  public Optional<InstantValue> toCoiPredictionValue() {
    return Optional.ofNullable(this.value).map(InstantValueDao::toCoi);
  }

  public InstantValueDao getValue() {
    return value;
  }

  public void setValue(
      InstantValueDao value) {
    this.value = value;
  }

  @Override
  public boolean update(FeaturePrediction<InstantValue> featurePrediction) {

    Optional<InstantValue> predictedValueOptional = featurePrediction.getPredictedValue();

    if (predictedValueOptional.isPresent()) {

      if (Objects.nonNull(this.value)) {

        return value.update(predictedValueOptional.get());
      } else {

        this.value = new InstantValueDao(predictedValueOptional.get());
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
    InstantFeaturePredictionDao that = (InstantFeaturePredictionDao) o;
    return value.equals(that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }

  @Override
  public String toString() {
    return "InstantFeaturePredictionDao{" +
        "value=" + value +
        '}';
  }
}
