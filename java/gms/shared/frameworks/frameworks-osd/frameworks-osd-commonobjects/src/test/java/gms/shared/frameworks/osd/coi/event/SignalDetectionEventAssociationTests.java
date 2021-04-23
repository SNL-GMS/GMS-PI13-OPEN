package gms.shared.frameworks.osd.coi.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gms.shared.frameworks.osd.coi.util.TestUtilities;
import java.util.UUID;
import org.junit.jupiter.api.Test;

public class SignalDetectionEventAssociationTests {

  private final UUID id = UUID.randomUUID();
  private final UUID eventHypothesisId = UUID.randomUUID();
  private final UUID signalDetectionHypothesisId = UUID.randomUUID();
  private final boolean isRejected = false;

  @Test
  public void testFrom() {
    final SignalDetectionEventAssociation signalDetectionEventAssociation = SignalDetectionEventAssociation
        .from(id, eventHypothesisId, signalDetectionHypothesisId, isRejected);
    assertEquals(id, signalDetectionEventAssociation.getId());
    assertEquals(eventHypothesisId, signalDetectionEventAssociation.getEventHypothesisId());
    assertEquals(signalDetectionHypothesisId,
        signalDetectionEventAssociation.getSignalDetectionHypothesisId());
    assertEquals(isRejected, signalDetectionEventAssociation.isRejected());
  }

  @Test
  public void testCreate() {
    final SignalDetectionEventAssociation signalDetectionEventAssociation = SignalDetectionEventAssociation
        .create(eventHypothesisId, signalDetectionHypothesisId);
    assertEquals(eventHypothesisId, signalDetectionEventAssociation.getEventHypothesisId());
    assertEquals(signalDetectionHypothesisId,
        signalDetectionEventAssociation.getSignalDetectionHypothesisId());
    assertFalse(signalDetectionEventAssociation.isRejected());
  }

  /**
   * Reject method creates a new SignalDetectionEventAssociation with the same id as the rejected
   * association, but with a separate provenance.
   */
  @Test
  public void testReject() {
    final SignalDetectionEventAssociation signalDetectionEventAssociation = SignalDetectionEventAssociation
        .create(eventHypothesisId, signalDetectionHypothesisId);
    SignalDetectionEventAssociation signalDetectionEventAssociationRejected = signalDetectionEventAssociation
        .reject();
    assertEquals(signalDetectionEventAssociation.getId(),
        signalDetectionEventAssociationRejected.getId());
    assertEquals(signalDetectionEventAssociation.getEventHypothesisId(),
        signalDetectionEventAssociationRejected.getEventHypothesisId());
    assertEquals(signalDetectionEventAssociation.getSignalDetectionHypothesisId(),
        signalDetectionEventAssociationRejected.getSignalDetectionHypothesisId());
    assertFalse(signalDetectionEventAssociation.isRejected());
    assertTrue(signalDetectionEventAssociationRejected.isRejected());
  }

  @Test
  public void testSerialization() throws Exception {
    TestUtilities.testSerialization(EventTestFixtures.SIGNAL_DETECTION_EVENT_ASSOCIATION,
        SignalDetectionEventAssociation.class);
  }

}
