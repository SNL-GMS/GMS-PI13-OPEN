package gms.shared.frameworks.osd.coi.event.repository.jpa;

import java.util.Objects;
import java.util.UUID;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Table;

/**
 * JPA data access object for {@link gms.shared.frameworks.osd.coi.event.PreferredLocationSolution}
 */
@Entity
@Table(name = "preferred_location_solution")
public class PreferredLocationSolutionDao {

  @Id
  @Column(name = "id")
  private UUID id;

  @OneToOne(cascade = CascadeType.PERSIST)
  private LocationSolutionDao locationSolution;


  /**
   * Default constructor for JPA.
   */
  public PreferredLocationSolutionDao() {
  }

  /**
   * Create a DAO from the COI object.
   */
  public PreferredLocationSolutionDao(UUID id, LocationSolutionDao locationSolutionDao) {
    Objects.requireNonNull(id);
    Objects.requireNonNull(locationSolutionDao);

    this.id = id;
    this.locationSolution = locationSolutionDao;
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public LocationSolutionDao getLocationSolution() {
    return locationSolution;
  }

  public void setLocationSolution(
      LocationSolutionDao locationSolution) {
    this.locationSolution = locationSolution;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PreferredLocationSolutionDao that = (PreferredLocationSolutionDao) o;
    return getId() == that.getId() &&
        Objects.equals(getLocationSolution(), that.getLocationSolution());
  }

  @Override
  public int hashCode() {

    return Objects.hash(getId(), getLocationSolution());
  }

  @Override
  public String toString() {
    return "PreferredLocationSolutionDao{" +
        "primaryKey=" + id +
        ", locationSolution=" + locationSolution +
        '}';
  }
}