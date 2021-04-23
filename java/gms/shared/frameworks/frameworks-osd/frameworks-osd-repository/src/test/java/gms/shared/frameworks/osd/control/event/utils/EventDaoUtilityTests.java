package gms.shared.frameworks.osd.control.event.utils;

import gms.shared.frameworks.osd.coi.CoiTestingEntityManagerFactory;
import gms.shared.frameworks.osd.coi.event.Event;
import gms.shared.frameworks.osd.coi.event.FeaturePrediction;
import gms.shared.frameworks.osd.coi.event.repository.jpa.EventDao;
import gms.shared.frameworks.osd.coi.signaldetection.FeatureMeasurement;
import gms.shared.frameworks.osd.coi.signaldetection.SignalDetectionTestFixtures;
import gms.shared.frameworks.osd.coi.test.utils.UtilsTestFixtures;
import gms.shared.frameworks.osd.control.signaldetection.SignalDetectionRepositoryJpa;
import gms.shared.frameworks.osd.control.station.StationRepositoryJpa;
import gms.shared.frameworks.osd.control.utils.FeatureMeasurementDaoUtility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import static gms.shared.frameworks.osd.coi.event.EventTestFixtures.AMPLITUDE_FEATURE_MEASUREMENT;
import static gms.shared.frameworks.osd.coi.event.EventTestFixtures.ARRIVAL_TIME_FEATURE_MEASUREMENT;
import static gms.shared.frameworks.osd.coi.event.EventTestFixtures.EVENT;
import static gms.shared.frameworks.osd.coi.event.EventTestFixtures.FEATURE_PREDICTION;
import static gms.shared.frameworks.osd.coi.event.EventTestFixtures.PHASE_FEATURE_MEASUREMENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class EventDaoUtilityTests {

  private static final Map<UUID, FeaturePrediction<?>> featurePredictionsById = Map.of(
      FeaturePredictionDaoUtility.buildId(FEATURE_PREDICTION), FEATURE_PREDICTION);

  private static final Map<String, FeatureMeasurement<?>> featureMeasurementsById = Map.of(
      FeatureMeasurementDaoUtility.buildId(ARRIVAL_TIME_FEATURE_MEASUREMENT), ARRIVAL_TIME_FEATURE_MEASUREMENT,
      FeatureMeasurementDaoUtility.buildId(PHASE_FEATURE_MEASUREMENT), PHASE_FEATURE_MEASUREMENT,
      FeatureMeasurementDaoUtility.buildId(AMPLITUDE_FEATURE_MEASUREMENT), AMPLITUDE_FEATURE_MEASUREMENT);

  private static EntityManagerFactory entityManagerFactory;

  @BeforeAll
  static void setUp() {
    entityManagerFactory = CoiTestingEntityManagerFactory.createTesting();
    new StationRepositoryJpa(entityManagerFactory)
        .storeStations(List.of(UtilsTestFixtures.STATION));
    new SignalDetectionRepositoryJpa(entityManagerFactory)
        .storeSignalDetections(List.of(SignalDetectionTestFixtures.SIGNAL_DETECTION));
  }

  @AfterAll
  static void tearDown() {
    entityManagerFactory.close();
  }

  @ParameterizedTest
  @MethodSource("getFromCoiArguments")
  void testFromCoiValidation(Class<? extends Exception> expectedException,
      Event event, EntityManager entityManager) {
    try {
      assertThrows(expectedException,
          () -> EventDaoUtility.fromCoi(event, entityManager));
    } finally {
      if (entityManager != null) {
        entityManager.close();
      }
    }
  }

  static Stream<Arguments> getFromCoiArguments() {
    return Stream.of(
        arguments(NullPointerException.class, null, entityManagerFactory.createEntityManager()),
        arguments(NullPointerException.class, EVENT, null),
        arguments(IllegalStateException.class,
            EVENT,
            entityManagerFactory.createEntityManager()));
  }

  @Test
  void testFromCoi() {
    EntityManager entityManager = entityManagerFactory.createEntityManager();

    try {
      entityManager.getTransaction().begin();
      EventDao dao = EventDaoUtility.fromCoi(EVENT, entityManager);

      assertEquals(EVENT.getId(), dao.getId());
      assertEquals(EVENT.getRejectedSignalDetectionAssociations(),
          dao.getRejectedSignalDetectionAssociations());
      assertEquals(EVENT.getMonitoringOrganization(), dao.getMonitoringOrganization());
      assertEquals(EVENT.getHypotheses().size(), dao.getHypotheses().size());

      dao.getHypotheses().stream()
          .map(hypothesisDao ->
              EventHypothesisDaoUtility.toCoi(hypothesisDao, featurePredictionsById, featureMeasurementsById))
          .forEach(hypothesis -> assertTrue(EVENT.getHypotheses().contains(hypothesis)));
    } finally {
      entityManager.getTransaction().rollback();
      entityManager.close();
    }
  }

  @ParameterizedTest
  @MethodSource("getToCoiArguments")
  void testToCoiValidation(EventDao eventDao,
      Map<UUID, FeaturePrediction<?>> featurePredictionsById,
      Map<String, FeatureMeasurement<?>> featureMeasurementsById) {
    assertThrows(NullPointerException.class,
        () -> EventDaoUtility.toCoi(eventDao, featurePredictionsById, featureMeasurementsById));
  }

  static Stream<Arguments> getToCoiArguments() {
    return Stream.of(
        arguments(null, featurePredictionsById, featureMeasurementsById),
        arguments(new EventDao(), null, featureMeasurementsById),
        arguments(new EventDao(), featurePredictionsById, null));
  }

  @Test
  void testToCoi() {
    EntityManager entityManager = entityManagerFactory.createEntityManager();

    try {
      entityManager.getTransaction().begin();

      assertEquals(EVENT, EventDaoUtility.toCoi(EventDaoUtility.fromCoi(EVENT, entityManager),
          featurePredictionsById,
          featureMeasurementsById));
    } finally {
      entityManager.getTransaction().rollback();
      entityManager.close();
    }
  }
}