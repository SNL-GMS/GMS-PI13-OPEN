package gms.shared.frameworks.osd.coi.event.repository.jpa;

import gms.shared.frameworks.osd.coi.event.MagnitudeType;
import gms.shared.frameworks.osd.coi.event.repository.jpa.util.MagnitudeTypeConverter;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * JPA data access object for {@link gms.shared.frameworks.osd.coi.event.NetworkMagnitudeSolution}
 */
@Entity
@Table(name = "network_magnitude_solution")
public class NetworkMagnitudeSolutionDao {

  @Id
  private UUID id;

  @Column(name = "magnitude_type", nullable = false)
  @Convert(converter = MagnitudeTypeConverter.class)
  private MagnitudeType magnitudeType;

  @Column(name = "magnitude", nullable = false)
  private double magnitude;

  @Column(name = "uncertainty", nullable = false)
  private double uncertainty;

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "network_magnitude_solution_id", foreignKey = @ForeignKey(name = "network_magnitude_solution_fk"))
  private List<NetworkMagnitudeBehaviorDao> networkMagnitudeBehaviors;

  /**
   * Default constructor for JPA.
   */
  public NetworkMagnitudeSolutionDao() {
    // Empty constructor needed for JPA
  }

  /**
   * Generated getters / setters
   */
  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public MagnitudeType getMagnitudeType() {
    return magnitudeType;
  }

  public void setMagnitudeType(MagnitudeType magnitudeType) {
    this.magnitudeType = magnitudeType;
  }

  public Double getMagnitude() {
    return magnitude;
  }

  public void setMagnitude(Double magnitude) {
    this.magnitude = magnitude;
  }

  public Double getUncertainty() {
    return uncertainty;
  }

  public void setUncertainty(Double uncertainty) {
    this.uncertainty = uncertainty;
  }

  public List<NetworkMagnitudeBehaviorDao> getNetworkMagnitudeBehaviors() {
    return networkMagnitudeBehaviors;
  }

  public void setNetworkMagnitudeBehaviors(
      List<NetworkMagnitudeBehaviorDao> networkMagnitudeBehaviors) {
    this.networkMagnitudeBehaviors = networkMagnitudeBehaviors;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    NetworkMagnitudeSolutionDao that = (NetworkMagnitudeSolutionDao) o;
    return id == that.id &&
        Double.compare(that.magnitude, magnitude) == 0 &&
        Double.compare(that.uncertainty, uncertainty) == 0 &&
        magnitudeType == that.magnitudeType &&
        networkMagnitudeBehaviors.equals(that.networkMagnitudeBehaviors);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, magnitudeType, magnitude, uncertainty, networkMagnitudeBehaviors);
  }

  @Override
  public String toString() {
    return "NetworkMagnitudeSolutionDao{" +
        "id=" + id +
        ", magnitudeType=" + magnitudeType +
        ", magnitude=" + magnitude +
        ", uncertainty=" + uncertainty +
        ", networkMagnitudeBehaviors=" + networkMagnitudeBehaviors +
        '}';
  }
}
