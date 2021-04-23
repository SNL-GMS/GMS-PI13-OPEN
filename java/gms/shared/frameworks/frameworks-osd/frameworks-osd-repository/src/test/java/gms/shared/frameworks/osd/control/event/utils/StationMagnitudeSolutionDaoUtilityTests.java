package gms.shared.frameworks.osd.control.event.utils;

import gms.shared.frameworks.osd.coi.CoiTestingEntityManagerFactory;
import gms.shared.frameworks.osd.coi.event.StationMagnitudeSolution;
import gms.shared.frameworks.osd.coi.event.repository.jpa.StationMagnitudeSolutionDao;
import gms.shared.frameworks.osd.coi.signaldetection.AmplitudeMeasurementValue;
import gms.shared.frameworks.osd.coi.signaldetection.FeatureMeasurement;
import gms.shared.frameworks.osd.coi.test.utils.UtilsTestFixtures;
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
import java.util.stream.Stream;
import static gms.shared.frameworks.osd.coi.event.EventTestFixtures.AMPLITUDE_FEATURE_MEASUREMENT;
import static gms.shared.frameworks.osd.coi.event.EventTestFixtures.STATION_MAGNITUDE_SOLUTION;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class StationMagnitudeSolutionDaoUtilityTests {

  private static final double DOUBLE_TOLERANCE = 0.0001;

  private static EntityManagerFactory entityManagerFactory;

  @BeforeAll
  static void setUp() {
    entityManagerFactory = CoiTestingEntityManagerFactory.createTesting();
    new StationRepositoryJpa(entityManagerFactory)
        .storeStations(List.of(UtilsTestFixtures.STATION));
  }

  @ParameterizedTest
  @MethodSource("getFromCoiArguments")
  void testFromCoiValidation(Class<? extends Exception> expectedException,
      StationMagnitudeSolution staMagSolution,
      EntityManager entityManager) {

    try {
      assertThrows(expectedException,
          () -> StationMagnitudeSolutionDaoUtility.fromCoi(staMagSolution, entityManager));
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
    return Stream.of(
        arguments(NullPointerException.class, null, entityManagerFactory.createEntityManager()),
        arguments(NullPointerException.class, STATION_MAGNITUDE_SOLUTION, null),
        arguments(IllegalStateException.class,
            STATION_MAGNITUDE_SOLUTION,
            entityManagerFactory.createEntityManager()),
        arguments(NullPointerException.class,
            STATION_MAGNITUDE_SOLUTION.toBuilder().setStationName("not a station").build(),
            entityManagerWithTransaction));
  }

  @Test
  public void testFromCoi() {
    EntityManager entityManager = entityManagerFactory.createEntityManager();

    try {
      entityManager.getTransaction().begin();
      StationMagnitudeSolutionDao dao = StationMagnitudeSolutionDaoUtility
          .fromCoi(STATION_MAGNITUDE_SOLUTION, entityManager);
      assertEquals(STATION_MAGNITUDE_SOLUTION.getType(), dao.getType());
      assertEquals(STATION_MAGNITUDE_SOLUTION.getModel(), dao.getModel());
      assertEquals(STATION_MAGNITUDE_SOLUTION.getStationName(), dao.getStation().getName());
      assertEquals(STATION_MAGNITUDE_SOLUTION.getPhase(), dao.getPhase());
      assertEquals(STATION_MAGNITUDE_SOLUTION.getMagnitude(), dao.getMagnitude(), DOUBLE_TOLERANCE);
      assertEquals(STATION_MAGNITUDE_SOLUTION.getMagnitudeUncertainty(),
          dao.getMagnitudeUncertainty(),
          DOUBLE_TOLERANCE);
      assertEquals(STATION_MAGNITUDE_SOLUTION.getModelCorrection(),
          dao.getModelCorrection(),
          DOUBLE_TOLERANCE);
      assertEquals(STATION_MAGNITUDE_SOLUTION.getMeasurement(),
          FeatureMeasurementDaoUtility.toCoi(dao.getMeasurement(), UtilsTestFixtures.CHANNEL));
    } finally {
      entityManager.getTransaction().rollback();
      entityManager.close();
    }
  }

  @ParameterizedTest
  @MethodSource("getToCoiArguments")
  void testToCoiValidation(StationMagnitudeSolutionDao staMagSolutionDao,
      FeatureMeasurement<AmplitudeMeasurementValue> featureMeasurement) {
    assertThrows(NullPointerException.class,
        () -> StationMagnitudeSolutionDaoUtility.toCoi(staMagSolutionDao, featureMeasurement));
  }

  static Stream<Arguments> getToCoiArguments() {
    return Stream.of(
        arguments(null, AMPLITUDE_FEATURE_MEASUREMENT),
        arguments(new StationMagnitudeSolutionDao(), null));
  }

  @Test
  public void testToCoi() {
    EntityManager entityManager = entityManagerFactory.createEntityManager();

    try {
      entityManager.getTransaction().begin();
      assertEquals(STATION_MAGNITUDE_SOLUTION, StationMagnitudeSolutionDaoUtility.toCoi(
          StationMagnitudeSolutionDaoUtility.fromCoi(STATION_MAGNITUDE_SOLUTION, entityManager),
          AMPLITUDE_FEATURE_MEASUREMENT));
    } finally {
      entityManager.getTransaction().rollback();
      entityManager.close();
    }
  }

  @AfterAll
  static void tearDown() {
    entityManagerFactory.close();
  }
}