package gms.shared.frameworks.osd.coi.event.repository.jpa;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * JPA data access object for {@link gms.shared.frameworks.osd.coi.event.LocationSolution}
 */
@Entity
@Table(name = "location_solution",
indexes = {
    @Index(name = "new_event_time_lat_long", columnList = "event_time, event_latitude_degrees, event_longitude_degrees")
})
public class LocationSolutionDao {

  @Id
  @Column(name = "id")
  private UUID id;

  @Embedded
  private EventLocationDao location;

  @OneToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "location_restraint_id", referencedColumnName = "id")
  private LocationRestraintDao locationRestraint;

  @OneToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "location_uncertainty_id", referencedColumnName = "id")
  private LocationUncertaintyDao locationUncertainty;

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "location_solution_id", referencedColumnName = "id")
  private Set<LocationBehaviorDao> locationBehaviors;

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinTable(name = "location_solution_feature_predictions",
    joinColumns =  {@JoinColumn(name = "location_solution_id", referencedColumnName = "id")},
    inverseJoinColumns = {@JoinColumn(name = "feature_prediction_id", referencedColumnName = "id")})
  private Set<FeaturePredictionDao<?>> featurePredictions;

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "location_solution_id", foreignKey = @ForeignKey(name = "location_solution_fk"))
  private List<NetworkMagnitudeSolutionDao> networkMagnitudeSolutions;

  /**
   * Default constructor for JPA.
   */
  public LocationSolutionDao() {
    // Empty constructor needed for JPA
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public EventLocationDao getLocation() {
    return location;
  }

  public void setLocation(
      EventLocationDao location) {
    this.location = location;
  }

  public LocationRestraintDao getLocationRestraint() {
    return locationRestraint;
  }

  public void setLocationRestraint(
      LocationRestraintDao locationRestraint) {
    this.locationRestraint = locationRestraint;
  }

  public LocationUncertaintyDao getLocationUncertainty() {
    return locationUncertainty;
  }

  public void setLocationUncertainty(
      LocationUncertaintyDao locationUncertainty) {
    this.locationUncertainty = locationUncertainty;
  }

  public Set<LocationBehaviorDao> getLocationBehaviors() {
    return locationBehaviors;
  }

  public void setLocationBehaviors(
      Set<LocationBehaviorDao> locationBehaviors) {
    this.locationBehaviors = locationBehaviors;
  }

  public Set<FeaturePredictionDao<?>> getFeaturePredictions() {
    return featurePredictions;
  }

  public void setFeaturePredictions(
      Set<FeaturePredictionDao<?>> featurePredictions) {
    this.featurePredictions = featurePredictions;
  }

  public List<NetworkMagnitudeSolutionDao> getNetworkMagnitudeSolutions() {
    return networkMagnitudeSolutions;
  }

  public void setNetworkMagnitudeSolutions(
      List<NetworkMagnitudeSolutionDao> networkMagnitudeSolutions) {
    this.networkMagnitudeSolutions = networkMagnitudeSolutions;
  }

  @Override
  public String toString() {
    return "LocationSolutionDao{" +
        "primaryKey=" + id +
        ", location=" + location +
        ", locationRestraint=" + locationRestraint +
        ", locationUncertainty=" + locationUncertainty +
        ", locationBehaviors=" + locationBehaviors +
        ", featurePredictions=" + featurePredictions +
        ", networkMagnitudeSolutions=" + networkMagnitudeSolutions +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    LocationSolutionDao that = (LocationSolutionDao) o;
    return id == that.id &&
        Objects.equals(location, that.location) &&
        Objects.equals(locationRestraint, that.locationRestraint) &&
        Objects.equals(locationUncertainty, that.locationUncertainty) &&
        Objects.equals(locationBehaviors, that.locationBehaviors) &&
        Objects.equals(featurePredictions, that.featurePredictions) &&
        Objects.equals(networkMagnitudeSolutions, that.networkMagnitudeSolutions);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, location, locationRestraint, locationUncertainty,
        locationBehaviors, featurePredictions, networkMagnitudeSolutions);
  }
}
