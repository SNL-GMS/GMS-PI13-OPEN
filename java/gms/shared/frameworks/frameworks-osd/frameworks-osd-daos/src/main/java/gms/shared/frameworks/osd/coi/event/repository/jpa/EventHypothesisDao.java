package gms.shared.frameworks.osd.coi.event.repository.jpa;

import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "event_hypothesis")
public class EventHypothesisDao {

  @Id
  @Column(unique = true)
  private UUID id;

  @Column(name = "event_id")
  private UUID eventId;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "parent_event_hypotheses",
      joinColumns = {@JoinColumn(name = "parent_event_hypothesis_id", referencedColumnName = "id")})
  private Set<UUID> parentEventHypotheses;

  @Column(name = "is_rejected")
  private boolean isRejected;

  @LazyCollection(LazyCollectionOption.FALSE)
  @OneToMany(cascade = CascadeType.ALL)
  private Set<LocationSolutionDao> locationSolutions;

  @OneToOne(cascade = CascadeType.ALL)
  private PreferredLocationSolutionDao preferredLocationSolution;

  @LazyCollection(LazyCollectionOption.FALSE)
  @OneToMany(cascade = CascadeType.ALL)
  @JoinColumn(name = "event_hypothesis_id")
  private Set<SignalDetectionEventAssociationDao> associations;

  /**
   * Default constructor for JPA.
   */
  public EventHypothesisDao() {
    // Empty constructor needed for JPA
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public UUID getEventId() {
    return eventId;
  }

  public void setEventId(UUID eventId) {
    this.eventId = eventId;
  }

  public Set<UUID> getParentEventHypotheses() {
    return parentEventHypotheses;
  }

  public void setParentEventHypotheses(Set<UUID> parentEventHypotheses) {
    this.parentEventHypotheses = parentEventHypotheses;
  }

  public boolean isRejected() {
    return isRejected;
  }

  public void setRejected(boolean rejected) {
    isRejected = rejected;
  }

  public Set<LocationSolutionDao> getLocationSolutions() {
    return locationSolutions;
  }

  public void setLocationSolutions(
      Set<LocationSolutionDao> locationSolutions) {
    this.locationSolutions = locationSolutions;
  }

  public PreferredLocationSolutionDao getPreferredLocationSolution() {
    return preferredLocationSolution;
  }

  public void setPreferredLocationSolution(
      PreferredLocationSolutionDao preferredLocationSolution) {
    this.preferredLocationSolution = preferredLocationSolution;
  }

  public Set<SignalDetectionEventAssociationDao> getAssociations() {
    return associations;
  }

  public void setAssociations(
      Set<SignalDetectionEventAssociationDao> associations) {
    this.associations = associations;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    EventHypothesisDao that = (EventHypothesisDao) o;
    return isRejected == that.isRejected &&
        id.equals(that.id) &&
        eventId.equals(that.eventId) &&
        parentEventHypotheses.equals(that.parentEventHypotheses) &&
        locationSolutions.equals(that.locationSolutions) &&
        preferredLocationSolution.equals(that.preferredLocationSolution) &&
        associations.equals(that.associations);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, eventId, parentEventHypotheses, isRejected, locationSolutions,
        preferredLocationSolution, associations);
  }

  @Override
  public String toString() {
    return "EventHypothesisDao{" +
        "id=" + id +
        ", eventId=" + eventId +
        ", parentEventHypotheses=" + parentEventHypotheses +
        ", isRejected=" + isRejected +
        ", locationSolutions=" + locationSolutions +
        ", preferredLocationSolution=" + preferredLocationSolution +
        ", associations=" + associations +
        '}';
  }
}
