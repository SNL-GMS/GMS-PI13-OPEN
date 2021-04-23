package gms.shared.frameworks.osd.control.event.utils;

import gms.shared.frameworks.osd.coi.CoiTestingEntityManagerFactory;
import gms.shared.frameworks.osd.coi.channel.repository.jpa.dataaccessobjects.ChannelDao;
import gms.shared.frameworks.osd.coi.channel.repository.jpa.dataaccessobjects.LocationDao;
import gms.shared.frameworks.osd.coi.event.FeaturePrediction;
import gms.shared.frameworks.osd.coi.event.FeaturePredictionComponent;
import gms.shared.frameworks.osd.coi.event.repository.jpa.EventLocationDao;
import gms.shared.frameworks.osd.coi.event.repository.jpa.FeaturePredictionComponentDao;
import gms.shared.frameworks.osd.coi.event.repository.jpa.FeaturePredictionDao;
import gms.shared.frameworks.osd.coi.event.repository.jpa.NumericFeaturePredictionDao;
import gms.shared.frameworks.osd.coi.signaldetection.NumericMeasurementValue;
import gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.dataaccessobjects.NumericMeasurementValueDao;
import gms.shared.frameworks.osd.coi.test.utils.UtilsTestFixtures;
import gms.shared.frameworks.osd.control.station.StationRepositoryJpa;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import static gms.shared.frameworks.osd.coi.event.EventTestFixtures.FEATURE_PREDICTION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FeaturePredictionDaoUtilityTests {

  private static EntityManagerFactory entityManagerFactory;

  @BeforeAll
  static void create() {
    entityManagerFactory = CoiTestingEntityManagerFactory.createTesting();
    new StationRepositoryJpa(entityManagerFactory)
        .storeStations(List.of(UtilsTestFixtures.STATION));
  }

  @AfterAll
  static void destroy() {
    entityManagerFactory.close();
  }

  @Test
  void testFromCoiNew() {
    EntityManager entityManager = entityManagerFactory.createEntityManager();

    try {
      entityManager.getTransaction().begin();
      FeaturePrediction<NumericMeasurementValue> expected = FEATURE_PREDICTION;
      FeaturePredictionDao actual = FeaturePredictionDaoUtility.fromCoi(expected, entityManager);

      Assertions.assertNotNull(actual);

      assertEquals(expected.getPhase(), actual.getPhase());

      Set<FeaturePredictionComponent> actualComponents = new HashSet<>();
      Set<FeaturePredictionDao> featurePredictionDaos = actual.getFeaturePredictionComponents();
      for (Object fpcDao : featurePredictionDaos) {
        actualComponents.add(((FeaturePredictionComponentDao) fpcDao).toCoi());
      }

      assertEquals(expected.getFeaturePredictionComponents().size(),
          actualComponents.size());
      Assertions.assertTrue(expected.getFeaturePredictionComponents().containsAll(actualComponents));
    } finally {
      entityManager.getTransaction().rollback();
      entityManager.close();
    }
  }

  @Test
  void testFromCoiExisting() {
    EntityManager entityManager = entityManagerFactory.createEntityManager();
    try {
      NumericFeaturePredictionDao expected = createDaoForFeaturePrediction();
      entityManager.getTransaction().begin();
      entityManager.persist(expected);
      entityManager.getTransaction().commit();

      entityManager.getTransaction().begin();

      NumericFeaturePredictionDao actual =(NumericFeaturePredictionDao) FeaturePredictionDaoUtility
          .fromCoi(FEATURE_PREDICTION, entityManager);

      assertEquals(expected, actual);
    } finally {
      entityManager.getTransaction().rollback();
      entityManager.close();
    }
  }

  @Test
  void testToCoiValidation() {
    assertThrows(NullPointerException.class, () -> FeaturePredictionDaoUtility.toCoi(null));
  }

  @Test
  void testToCoi() {
    EntityManager entityManager = entityManagerFactory.createEntityManager();

    try {
      entityManager.getTransaction().begin();
      assertEquals(FEATURE_PREDICTION, FeaturePredictionDaoUtility.toCoi(
          FeaturePredictionDaoUtility.fromCoi(FEATURE_PREDICTION, entityManager)));
    } finally {
      entityManager.getTransaction().rollback();
      entityManager.close();
    }
  }

  private NumericFeaturePredictionDao createDaoForFeaturePrediction() {
    EntityManager entityManager = entityManagerFactory.createEntityManager();
    ChannelDao channelDao = entityManager.find(ChannelDao.class, UtilsTestFixtures.CHANNEL.getName());

    NumericFeaturePredictionDao dao = new NumericFeaturePredictionDao();
    dao.setId(FeaturePredictionDaoUtility.buildId(FEATURE_PREDICTION));
    dao.setPhase(FEATURE_PREDICTION.getPhase());
    dao.setValue(new NumericMeasurementValueDao(FEATURE_PREDICTION.getPredictedValue().get()));
    dao.setFeaturePredictionComponents(FEATURE_PREDICTION.getFeaturePredictionComponents()
        .stream()
        .map(FeaturePredictionComponentDao::from)
        .collect(Collectors.toSet()));
    dao.setExtrapolated(FEATURE_PREDICTION.isExtrapolated());
    dao.setPredictionType(FEATURE_PREDICTION.getPredictionTypeName());
    dao.setSourceLocation(new EventLocationDao(FEATURE_PREDICTION.getSourceLocation()));
    dao.setReceiverLocation(new LocationDao(FEATURE_PREDICTION.getReceiverLocation()));
    dao.setChannel(channelDao);

    return dao;
  }
}