package gms.shared.frameworks.osd.coi.event.repository.jpa;

import gms.shared.frameworks.osd.coi.event.LocationBehavior;
import gms.shared.frameworks.osd.coi.signaldetection.FeatureMeasurement;
import gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.dataaccessobjects.FeatureMeasurementDao;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.util.Objects;
import java.util.UUID;


/**
 * JPA data access object for {@link gms.shared.frameworks.osd.coi.event.LocationBehavior}
 */
@Entity
@Table(name = "location_behavior")
public class LocationBehaviorDao {

  @Id
  private UUID id;

  @Column(name = "residual", nullable = false)
  private double residual;

  @Column(name = "weight", nullable = false)
  private double weight;

  @Column(name = "defining", nullable = false)
  private boolean defining;

  @ManyToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "feature_prediction_id")
  private FeaturePredictionDao<?> featurePrediction;

  @ManyToOne
  @JoinColumn(name = "feature_measurement_id", nullable = false)
  private FeatureMeasurementDao<?> featureMeasurement;

  /**
   * Default constructor for JPA.
   */
  public LocationBehaviorDao() {
    // Empty constructor needed for JPA
  }
  public void setId(UUID id) {
    this.id = id;
  }
  public double getResidual() {
    return residual;
  }

  public void setResidual(double residual) {
    this.residual = residual;
  }

  public double getWeight() {
    return weight;
  }

  public void setWeight(double weight) {
    this.weight = weight;
  }

  public boolean isDefining() {
    return defining;
  }

  public void setDefining(boolean defining) {
    this.defining = defining;
  }

  public FeaturePredictionDao<?> getFeaturePrediction() {
    return featurePrediction;
  }

  public void setFeaturePrediction(FeaturePredictionDao<?> featurePrediction) {
    this.featurePrediction = featurePrediction;
  }

  public FeatureMeasurementDao<?> getFeatureMeasurement() {
    return featureMeasurement;
  }

  public void setFeatureMeasurement(
      FeatureMeasurementDao<?> featureMeasurementDao) {

    Objects.requireNonNull(featureMeasurementDao, "Null featureMeasurementDao");

    this.featureMeasurement = featureMeasurementDao;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    LocationBehaviorDao that = (LocationBehaviorDao) o;
    return id == that.id &&
        Double.compare(that.residual, residual) == 0 &&
        Double.compare(that.weight, weight) == 0 &&
        defining == that.defining &&
        Objects.equals(featurePrediction, that.featurePrediction) &&
        Objects.equals(featureMeasurement, that.featureMeasurement);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, residual, weight, defining, featurePrediction, featureMeasurement);
  }

  @Override
  public String toString() {
    return "LocationBehaviorDao{" +
        "primaryKey=" + id +
        ", residual=" + residual +
        ", weight=" + weight +
        ", defining=" + defining +
        ", featurePredictionId=" + featurePrediction +
        ", featureMeasurement=" + featureMeasurement +
        '}';
  }


}
