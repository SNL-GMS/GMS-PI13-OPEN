package gms.shared.frameworks.osd.coi.event.repository.jpa;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "event_hypothesis_association")
public class EventHypothesisAssociationDao {

  @Id
  @Column(name = "id", unique = true, updatable = false, nullable = false)
  private UUID id;

  // Purposely not cascading relationships because of the hierarchical issues involved.  The event
  // and hypothesis must be created and persisted prior to creating this object.
  @OneToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "event_id", referencedColumnName = "id")
  private EventDao event;

  @OneToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "event_hypothesis_id", referencedColumnName = "id")
  private EventHypothesisDao hypothesis;

  public EventDao getEvent() {
    return event;
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public void setEvent(EventDao event) {
    this.event = event;
  }

  public EventHypothesisDao getHypothesis() {
    return hypothesis;
  }

  public void setHypothesis(EventHypothesisDao hypothesis) {
    this.hypothesis = hypothesis;
  }

  @Override
  public String toString() {
    return "EventHypothesisAssociationDao{" +
        "id=" + id +
        ", event=" + event +
        ", hypothesis=" + hypothesis +
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
    EventHypothesisAssociationDao that = (EventHypothesisAssociationDao) o;
    return id.equals(that.id) &&
        event.equals(that.event) &&
        hypothesis.equals(that.hypothesis);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, event, hypothesis);
  }
}
