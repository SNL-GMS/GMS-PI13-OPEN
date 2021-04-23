package gms.shared.frameworks.osd.coi.event.repository.jpa;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import java.util.Objects;
import java.util.UUID;

/**
 * JPA data access object for {@link gms.shared.frameworks.osd.coi.event.NetworkMagnitudeBehavior}
 */
@Entity
@Table(name = "network_magnitude_behavior")
public class NetworkMagnitudeBehaviorDao {

  @Id
  private UUID id;

  @Column(name = "defining", nullable = false)
  private boolean defining;

  @Column(name = "residual", nullable = false)
  private double residual;

  @Column(name = "weight", nullable = false)
  private double weight;

  @OneToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "network_magnitude_behavior_id", foreignKey = @ForeignKey(name = "network_magnitude_behavior_fk"))
  private StationMagnitudeSolutionDao stationMagnitudeSolution;

  /**
   * Default constructor for JPA.
   */
  public NetworkMagnitudeBehaviorDao() {
    // Empty constructor needed for JPA
  }

  /** Generated getters / setters */
  /**
   * Generated getters / setters
   */
  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public boolean isDefining() {
    return defining;
  }

  public void setDefining(boolean defining) {
    this.defining = defining;
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

  public StationMagnitudeSolutionDao getStationMagnitudeSolution() {
    return stationMagnitudeSolution;
  }

  public void setStationMagnitudeSolution(StationMagnitudeSolutionDao stationMagnitudeSolution) {
    this.stationMagnitudeSolution = stationMagnitudeSolution;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    NetworkMagnitudeBehaviorDao that = (NetworkMagnitudeBehaviorDao) o;
    return id == that.id &&
        defining == that.defining &&
        Double.compare(that.residual, residual) == 0 &&
        Double.compare(that.weight, weight) == 0 &&
        stationMagnitudeSolution.equals(that.stationMagnitudeSolution);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, defining, residual, weight, stationMagnitudeSolution);
  }

  @Override
  public String toString() {
    return "NetworkMagnitudeBehaviorDao{" +
        "id=" + id +
        ", defining=" + defining +
        ", residual=" + residual +
        ", weight=" + weight +
        ", stationMagnitudeSolution=" + stationMagnitudeSolution +
        '}';
  }
}
