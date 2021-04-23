package gms.shared.frameworks.osd.control.event.utils;

import gms.shared.frameworks.osd.coi.CoiTestingEntityManagerFactory;
import gms.shared.frameworks.osd.coi.event.EventHypothesis;
import gms.shared.frameworks.osd.coi.event.FeaturePrediction;
import gms.shared.frameworks.osd.coi.event.PreferredLocationSolution;
import gms.shared.frameworks.osd.coi.event.SignalDetectionEventAssociation;
import gms.shared.frameworks.osd.coi.event.repository.jpa.EventHypothesisDao;
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
import static gms.shared.frameworks.osd.coi.event.EventTestFixtures.EVENT_HYPOTHESIS;
import static gms.shared.frameworks.osd.coi.event.EventTestFixtures.FEATURE_PREDICTION;
import static gms.shared.frameworks.osd.coi.event.EventTestFixtures.PHASE_FEATURE_MEASUREMENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class EventHypothesisDaoUtilityTests {

  private static final Map<UUID, FeaturePrediction<?>> featurePredictionsById =
      Map.of(FeaturePredictionDaoUtility.buildId(FEATURE_PREDICTION), FEATURE_PREDICTION);
  private static final Map<String, FeatureMeasurement<?>> featureMeasurementsById =
      Map.of(FeatureMeasurementDaoUtility.buildId(ARRIVAL_TIME_FEATURE_MEASUREMENT),
          ARRIVAL_TIME_FEATURE_MEASUREMENT,
          FeatureMeasurementDaoUtility.buildId(PHASE_FEATURE_MEASUREMENT),
          PHASE_FEATURE_MEASUREMENT,
          FeatureMeasurementDaoUtility.buildId(AMPLITUDE_FEATURE_MEASUREMENT),
          AMPLITUDE_FEATURE_MEASUREMENT);

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
      EventHypothesis hypothesis,
      EntityManager entityManager) {
    try {
      assertThrows(expectedException,
          () -> EventHypothesisDaoUtility.fromCoi(hypothesis, entityManager));
    } finally {
      if (entityManager != null) {
        if (entityManager.getTransaction().isActive()) {
          entityManager.getTransaction().rollback();
        }

        entityManager.close();
      }
    }
  }

  static Stream<Arguments> getFromCoiArguments() {
    EntityManager entityManagerWithTransaction = entityManagerFactory.createEntityManager();
    entityManagerWithTransaction.getTransaction().begin();
    UUID alternateId = UUID.randomUUID();
    return Stream.of(
        arguments(NullPointerException.class, null, entityManagerFactory.createEntityManager()),
        arguments(NullPointerException.class, EVENT_HYPOTHESIS, null),
        arguments(IllegalStateException.class,
            EVENT_HYPOTHESIS,
            entityManagerFactory.createEntityManager()),
        arguments(IllegalStateException.class,
            EVENT_HYPOTHESIS.toBuilder()
                .setId(alternateId)
                .setAssociations(List.of(
                    SignalDetectionEventAssociation.create(alternateId, UUID.randomUUID())))
                .build(),
            entityManagerWithTransaction)
    );
  }

  @Test
  void testFromCoiNew() {
    EntityManager entityManager = entityManagerFactory.createEntityManager();

    try {
      entityManager.getTransaction().begin();
      EventHypothesisDao dao = EventHypothesisDaoUtility.fromCoi(EVENT_HYPOTHESIS, entityManager);

      assertEquals(EVENT_HYPOTHESIS.getId(), dao.getId());
      assertEquals(EVENT_HYPOTHESIS.getEventId(), dao.getEventId());
      assertEquals(EVENT_HYPOTHESIS.getParentEventHypotheses().size(),
          dao.getParentEventHypotheses().size());
      assertTrue(EVENT_HYPOTHESIS.getParentEventHypotheses()
          .containsAll(dao.getParentEventHypotheses()));
      assertEquals(EVENT_HYPOTHESIS.getLocationSolutions().size(),
          dao.getLocationSolutions().size());
      dao.getLocationSolutions().stream()
          .map(locationSolutionDao ->
              LocationSolutionDaoUtility.toCoi(locationSolutionDao,
                  featurePredictionsById,
                  featureMeasurementsById))
          .forEach(locationSolution ->
              assertTrue(EVENT_HYPOTHESIS.getLocationSolutions().contains(locationSolution)));
      assertEquals(EVENT_HYPOTHESIS.getPreferredLocationSolution().isPresent(),
          dao.getPreferredLocationSolution() != null);
      EVENT_HYPOTHESIS.getPreferredLocationSolution().stream()
          .forEach(preferredSolution ->
              assertEquals(preferredSolution, PreferredLocationSolution.from(
                  LocationSolutionDaoUtility.toCoi(dao.getPreferredLocationSolution()
                      .getLocationSolution(), featurePredictionsById, featureMeasurementsById))));
      assertEquals(EVENT_HYPOTHESIS.getAssociations().size(), dao.getAssociations().size());
      dao.getAssociations().stream()
          .map(assocationDao -> assocationDao.toCoi())
          .forEach(association -> EVENT_HYPOTHESIS.getAssociations().contains(association));
    } finally {
      entityManager.getTransaction().rollback();
      entityManager.close();
    }
  }

  @Test
  void testFromCoiExisting() {
    EntityManager entityManager = entityManagerFactory.createEntityManager();

    try {
      entityManager.getTransaction().begin();
      EventHypothesisDao expected = EventHypothesisDaoUtility.fromCoi(EVENT_HYPOTHESIS,
          entityManager);
      entityManager.getTransaction().commit();

      entityManager.getTransaction().begin();
      EventHypothesisDao actual = EventHypothesisDaoUtility.fromCoi(EVENT_HYPOTHESIS,
          entityManager);

      assertEquals(expected.getId(), actual.getId());
      assertEquals(expected.getEventId(), actual.getEventId());
      assertEquals(expected.getParentEventHypotheses().size(),
          actual.getParentEventHypotheses().size());
      assertTrue(expected.getParentEventHypotheses().containsAll(actual.getParentEventHypotheses()));
      assertEquals(expected.isRejected(), actual.isRejected());
      assertEquals(expected.getLocationSolutions().size(), actual.getLocationSolutions().size());

      actual.getLocationSolutions().stream()
          .forEach(locationSolution ->
              assertTrue(actual.getLocationSolutions().contains(locationSolution)));

      assertEquals(expected.getPreferredLocationSolution(), actual.getPreferredLocationSolution());
      assertEquals(expected.getAssociations().size(), actual.getAssociations().size());
      actual.getAssociations().stream()
          .forEach(association -> assertTrue(expected.getAssociations().contains(association)));
    } finally {
      entityManager.getTransaction().rollback();
      entityManager.close();
    }
  }

  @ParameterizedTest
  @MethodSource("getToCoiArguments")
  void testToCoiValidation(EventHypothesisDao hypothesisDao,
      Map<UUID, FeaturePrediction<?>> featurePredictionsById,
      Map<String, FeatureMeasurement<?>> featureMeasurementsById) {
    assertThrows(NullPointerException.class,
        () -> EventHypothesisDaoUtility.toCoi(hypothesisDao, featurePredictionsById, featureMeasurementsById));
  }

  static Stream<Arguments> getToCoiArguments() {
    return Stream.of(
        arguments(null, featurePredictionsById, featureMeasurementsById),
        arguments(new EventHypothesisDao(), null, featureMeasurementsById),
        arguments(new EventHypothesisDao(), featurePredictionsById, null));
  }

  @Test
  void testToCoi() {
    EntityManager entityManager = entityManagerFactory.createEntityManager();

    try {
      entityManager.getTransaction().begin();

      assertEquals(EVENT_HYPOTHESIS,
          EventHypothesisDaoUtility.toCoi(
              EventHypothesisDaoUtility.fromCoi(EVENT_HYPOTHESIS, entityManager),
              featurePredictionsById, featureMeasurementsById));
    } finally {
      entityManager.getTransaction().rollback();
      entityManager.close();
    }
  }
}