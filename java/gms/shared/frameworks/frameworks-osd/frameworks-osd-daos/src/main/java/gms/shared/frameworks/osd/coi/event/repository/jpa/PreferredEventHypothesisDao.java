package gms.shared.frameworks.osd.coi.event.repository.jpa;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import java.util.Objects;
import java.util.UUID;


/**
 * JPA data access object for {@link gms.shared.frameworks.osd.coi.event.PreferredEventHypothesis}
 * to allow read and write access to the relational database.
 */
@Entity
@Table(name = "preferred_event_hypothesis")
public class PreferredEventHypothesisDao {

  @Id
  @Column(name = "id")
  private UUID id;

  @Column(name = "processing_stage_id", nullable = false)
  private UUID processingStageId;

  @OneToOne(cascade = CascadeType.ALL)
  private EventHypothesisDao eventHypothesis;

  /**
   * Default constructor for JPA
   */
  public PreferredEventHypothesisDao() {
  }

  public PreferredEventHypothesisDao(EventHypothesisDao eventHypothesisDao,
      UUID processingStageId) {

    Objects.requireNonNull(eventHypothesisDao, "Null eventHypothesisDao");
    Objects.requireNonNull(processingStageId, "Null processingStageId");

    this.id = UUID.randomUUID();
    this.eventHypothesis = eventHypothesisDao;
    this.processingStageId = processingStageId;
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public UUID getProcessingStageId() {
    return processingStageId;
  }

  public void setProcessingStageId(UUID processingStageId) {
    this.processingStageId = processingStageId;
  }

  public EventHypothesisDao getEventHypothesis() {
    return eventHypothesis;
  }

  public void setEventHypothesis(
      EventHypothesisDao eventHypothesis) {
    this.eventHypothesis = eventHypothesis;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PreferredEventHypothesisDao that = (PreferredEventHypothesisDao) o;
    return id == that.id &&
        processingStageId.equals(that.processingStageId) &&
        eventHypothesis.equals(that.eventHypothesis);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, processingStageId, eventHypothesis);
  }

  @Override
  public String toString() {
    return "PreferredEventHypothesisDao{" +
        "primaryKey=" + id +
        ", processingStageId=" + processingStageId +
        ", eventHypothesis=" + eventHypothesis +
        '}';
  }
}
