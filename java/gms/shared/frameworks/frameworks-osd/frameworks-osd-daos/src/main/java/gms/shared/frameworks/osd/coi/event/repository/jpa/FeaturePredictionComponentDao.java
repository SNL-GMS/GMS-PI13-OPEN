package gms.shared.frameworks.osd.coi.event.repository.jpa;

import gms.shared.frameworks.osd.coi.event.FeaturePredictionComponent;
import gms.shared.frameworks.osd.coi.event.FeaturePredictionCorrectionType;
import gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.dataaccessobjects.DoubleValueDao;
import java.util.Objects;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class FeaturePredictionComponentDao {

  @Id
  @Column(name = "id")
  private UUID id;

  @Column(nullable = false)
  private DoubleValueDao value;

  @Column(name = "is_extrapolated", nullable = false)
  private boolean isExtrapolated;

  @Column(name = "correction_type", nullable = false)
  private FeaturePredictionCorrectionType correctionType;

  /**
   * Default constructor for JPA.
   */
  public FeaturePredictionComponentDao() {}

  private FeaturePredictionComponentDao(
      DoubleValueDao value,
      boolean isExtrapolated,
      FeaturePredictionCorrectionType correctionType) {
    this.id = UUID.randomUUID();
    this.value = value;
    this.isExtrapolated = isExtrapolated;
    this.correctionType = correctionType;
  }

  public static FeaturePredictionComponentDao from(FeaturePredictionComponent component) {

    Objects.requireNonNull(component,
        "Cannot create FeaturePredictionComponentDao from null FeaturePredictionComponent");

    return new FeaturePredictionComponentDao(
        new DoubleValueDao(component.getValue()),
        component.isExtrapolated(),
        component.getPredictionComponentType()
    );
  }

  public FeaturePredictionComponent toCoi() {

    return FeaturePredictionComponent.from(
        this.value.toCoi(),
        this.isExtrapolated,
        this.correctionType
    );
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    FeaturePredictionComponentDao that = (FeaturePredictionComponentDao) o;
    return id == that.id &&
        isExtrapolated == that.isExtrapolated &&
        value.equals(that.value) &&
        correctionType == that.correctionType;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, value, isExtrapolated, correctionType);
  }

  @Override
  public String toString() {
    return "FeaturePredictionComponentDao{" +
        "id=" + id +
        ", value=" + value +
        ", isExtrapolated=" + isExtrapolated +
        ", correctionType=" + correctionType +
        '}';
  }
}
