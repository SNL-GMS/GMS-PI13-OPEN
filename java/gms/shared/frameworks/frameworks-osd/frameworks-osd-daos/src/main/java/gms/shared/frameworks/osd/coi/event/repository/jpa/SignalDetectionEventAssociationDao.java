package gms.shared.frameworks.osd.coi.event.repository.jpa;

import gms.shared.frameworks.osd.coi.event.SignalDetectionEventAssociation;
import org.apache.commons.lang3.Validate;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Objects;
import java.util.UUID;

/**
 * JPA data access object for {@link gms.shared.frameworks.osd.coi.event.SignalDetectionEventAssociation}
 * to allow read and write access to the relational database.
 */
@Entity
@Table(name = "signal_detection_event_association")
public class SignalDetectionEventAssociationDao {

  // TODO: use actual relationships on SignalDetection/Event to handle this
  @Id
  @Column(name = "id", nullable = false)
  private UUID id;

  @Column(name = "event_hypothesis_id", nullable = false)
  private UUID eventHypothesisId;

  @Column(name = "signal_detection_hypothesis_id", nullable = false)
  private UUID signalDetectionHypothesisId;

  @Column(name = "is_rejected")
  private boolean rejected;

  /**
   * Default constructor for JPA.
   */
  public SignalDetectionEventAssociationDao() {
  }

  /**
   * Create a DAO from the COI object.
   *
   * @param signalDetectionEventAssociation The SignalDetectionEventAssociation object.
   */
  public SignalDetectionEventAssociationDao(
      SignalDetectionEventAssociation signalDetectionEventAssociation) throws NullPointerException {
    Validate.notNull(signalDetectionEventAssociation);
    this.id = signalDetectionEventAssociation.getId();
    this.eventHypothesisId = signalDetectionEventAssociation.getEventHypothesisId();
    this.signalDetectionHypothesisId = signalDetectionEventAssociation
        .getSignalDetectionHypothesisId();
    this.rejected = signalDetectionEventAssociation.isRejected();
  }

  /**
   * Convert this DAO into its corresponding COI object.
   *
   * @return A SignalDetectionEventAssociation COI object.
   */
  public SignalDetectionEventAssociation toCoi() {
    return SignalDetectionEventAssociation
        .from(getId(), getEventHypothesisId(), getSignalDetectionHypothesisId(), isRejected());
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public UUID getEventHypothesisId() {
    return eventHypothesisId;
  }

  public void setEventHypothesisId(UUID eventHypothesisId) {
    this.eventHypothesisId = eventHypothesisId;
  }

  public UUID getSignalDetectionHypothesisId() {
    return signalDetectionHypothesisId;
  }

  public void setSignalDetectionHypothesisId(UUID signalDetectionHypothesisId) {
    this.signalDetectionHypothesisId = signalDetectionHypothesisId;
  }

  public boolean isRejected() {
    return rejected;
  }

  public void setRejected(boolean rejected) {
    this.rejected = rejected;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SignalDetectionEventAssociationDao that = (SignalDetectionEventAssociationDao) o;
    return rejected == that.rejected &&
        id.equals(that.id) &&
        eventHypothesisId.equals(that.eventHypothesisId) &&
        signalDetectionHypothesisId.equals(that.signalDetectionHypothesisId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, eventHypothesisId, signalDetectionHypothesisId, rejected);
  }

  @Override
  public String toString() {
    return "SignalDetectionEventAssociationDao{" +
        "id=" + id +
        ", eventHypothesisId=" + eventHypothesisId +
        ", signalDetectionHypothesisId=" + signalDetectionHypothesisId +
        ", isRejected=" + rejected +
        '}';
  }
}
