package gms.shared.frameworks.osd.control.preferences;

import gms.shared.frameworks.osd.api.preferences.UserPreferencesRepositoryInterface;
import gms.shared.frameworks.osd.coi.preferences.UserPreferences;
import gms.shared.frameworks.osd.coi.preferences.repository.UserPreferencesDao;
import gms.shared.frameworks.osd.coi.preferences.repository.WorkspaceLayoutDao;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserPreferencesRepositoryJpa implements UserPreferencesRepositoryInterface {

  private static final Logger logger = LoggerFactory.getLogger(UserPreferencesRepositoryJpa.class);

  private final EntityManagerFactory entityManagerFactory;

  public UserPreferencesRepositoryJpa(EntityManagerFactory entityManagerFactory) {
    this.entityManagerFactory = entityManagerFactory;
  }

  @Override
  public Optional<UserPreferences> getUserPreferencesByUserId(String userId) {
    EntityManager entityManager = entityManagerFactory.createEntityManager();
    try {
      return findByUserId(entityManager, userId).map(UserPreferencesDao::toCoi);
    } finally {
      entityManager.close();
    }
  }

  @Override
  public void setUserPreferences(UserPreferences userPreferences) {
    Objects.requireNonNull(userPreferences, "Cannot store a null user preferences");
    EntityManager entityManager = entityManagerFactory.createEntityManager();
    final Optional<UserPreferencesDao> existing = findByUserId(entityManager, userPreferences.getUserId());
    try {
      entityManager.getTransaction().begin();
      if (existing.isPresent()) {
        final UserPreferencesDao dao = existing.get();
        dao.setDefaultLayoutName(userPreferences.getDefaultLayoutName());
        dao.setSohLayoutName(userPreferences.getSohLayoutName());
        dao.setWorkspaceLayouts(userPreferences.getWorkspaceLayouts().stream()
            .map(WorkspaceLayoutDao::new).collect(Collectors.toList()));
        entityManager.persist(dao);
      } else {
        entityManager.persist(new UserPreferencesDao(userPreferences));
      }
      entityManager.getTransaction().commit();
    } catch (Exception e) {
      logger.error("Error storing user preferences", e);
      entityManager.getTransaction().rollback();
      throw new RuntimeException(e);
    } finally {
      entityManager.close();
    }
  }

  private Optional<UserPreferencesDao> findByUserId(EntityManager entityManager, String userId) {
    Validate.notBlank(userId, "Cannot retrieve user preferences for a null or empty userId");
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();

    CriteriaQuery<UserPreferencesDao> preferencesQuery = builder.createQuery(UserPreferencesDao.class);
    Root<UserPreferencesDao> fromUserPreferences = preferencesQuery.from(UserPreferencesDao.class);
    preferencesQuery.select(fromUserPreferences);

    preferencesQuery.where(builder.equal(fromUserPreferences.get("userId"), userId));

    final List<UserPreferencesDao> results = entityManager
        .createQuery(preferencesQuery).getResultList();
    return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
  }
}
