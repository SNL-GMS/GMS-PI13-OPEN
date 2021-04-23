package gms.shared.frameworks.soh.repository.systemmessage;

import gms.shared.frameworks.osd.api.systemmessage.SystemMessageRepositoryInterface;
import gms.shared.frameworks.osd.coi.systemmessages.SystemMessage;
import gms.shared.frameworks.osd.dao.systemmessage.SystemMessageDao;
import gms.shared.metrics.CustomMetric;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SystemMessageRepositoryJpa implements
    SystemMessageRepositoryInterface {

  private static final Logger logger = LoggerFactory.getLogger(SystemMessageRepositoryJpa.class);

  private static final CustomMetric<SystemMessageRepositoryJpa, Long> systemMessageRepositoryStoreSystemMessages =
      CustomMetric.create(CustomMetric::incrementer,
          "systemMessageRepositoryStoreSystemMessages_hits:type=Counter", 0L);

  private static final CustomMetric<Long, Long> systemMessageRepositoryStoreSystemMessagesDuration =
      CustomMetric.create(CustomMetric::updateTimingData,
          "systemMessageRepositoryStoreSystemMessagesDuration_duration:type=Value", 0L);

  private final EntityManagerFactory entityManagerFactory;

  public SystemMessageRepositoryJpa(EntityManagerFactory entityManagerFactory) {
    this.entityManagerFactory = entityManagerFactory;
  }

  @Override
  public void storeSystemMessages(Collection<SystemMessage> systemMessages) {
    Validate.notEmpty(systemMessages);

    systemMessageRepositoryStoreSystemMessages.updateMetric(this);
    Instant start = Instant.now();

    EntityManager entityManager = entityManagerFactory.createEntityManager();
    entityManager.getTransaction().begin();

    try {
      for (SystemMessage msg : systemMessages) {
        SystemMessageDao dao = entityManager.find(SystemMessageDao.class, msg.getId());

        if (dao == null) {
          entityManager.persist(SystemMessageDao.from(msg));
        }
      }

      entityManager.getTransaction().commit();
    } catch (PersistenceException e) {
      logger.error(e.getMessage());
      entityManager.getTransaction().rollback();
      throw e;
    } finally {
      entityManager.close();

      Instant finish = Instant.now();
      long timeElapsed = Duration.between(start, finish).toMillis();
      systemMessageRepositoryStoreSystemMessagesDuration.updateMetric(timeElapsed);
    }
  }
}
