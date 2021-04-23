package gms.shared.frameworks.osd.control.event.utils;

import com.google.common.base.Preconditions;
import gms.shared.frameworks.osd.coi.event.PreferredEventHypothesis;
import gms.shared.frameworks.osd.coi.event.repository.jpa.EventHypothesisDao;
import gms.shared.frameworks.osd.coi.event.repository.jpa.PreferredEventHypothesisDao;
import javax.persistence.EntityManager;
import java.util.Objects;

public class PreferredEventHypothesisDaoUtility {

  public static PreferredEventHypothesisDao fromCoi(PreferredEventHypothesis preferredHypothesis,
      EntityManager entityManager) {

    Objects.requireNonNull(preferredHypothesis);
    Objects.requireNonNull(entityManager);
    Preconditions.checkState(entityManager.getTransaction().isActive());

    EventHypothesisDao hypothesisDao = EventHypothesisDaoUtility.fromCoi(
        preferredHypothesis.getEventHypothesis(), entityManager);

    PreferredEventHypothesisDao dao = new PreferredEventHypothesisDao(hypothesisDao,
        preferredHypothesis.getProcessingStageId());

    entityManager.persist(dao);

    return dao;
  }
}
