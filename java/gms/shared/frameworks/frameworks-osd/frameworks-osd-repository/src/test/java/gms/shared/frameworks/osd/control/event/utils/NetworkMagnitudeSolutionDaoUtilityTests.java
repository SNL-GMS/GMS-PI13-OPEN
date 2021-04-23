package gms.shared.frameworks.osd.control.event.utils;

import gms.shared.frameworks.osd.coi.CoiTestingEntityManagerFactory;
import gms.shared.frameworks.osd.coi.event.NetworkMagnitudeSolution;
import gms.shared.frameworks.osd.coi.event.repository.jpa.NetworkMagnitudeSolutionDao;
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
import java.util.Map;
import java.util.stream.Stream;
import static gms.shared.frameworks.osd.coi.event.EventTestFixtures.AMPLITUDE_FEATURE_MEASUREMENT;
import static gms.shared.frameworks.osd.coi.event.EventTestFixtures.ARRIVAL_TIME_FEATURE_MEASUREMENT;
import static gms.shared.frameworks.osd.coi.event.EventTestFixtures.NETWORK_MAGNITUDE_SOLUTION;
import static gms.shared.frameworks.osd.coi.event.EventTestFixtures.PHASE_FEATURE_MEASUREMENT;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class NetworkMagnitudeSolutionDaoUtilityTests {

  private static EntityManagerFactory entityManagerFactory;
  private static final Map<String, FeatureMeasurement<?>> featureMeasurementsById =
      Map.of(FeatureMeasurementDaoUtility.buildId(ARRIVAL_TIME_FEATURE_MEASUREMENT),
          ARRIVAL_TIME_FEATURE_MEASUREMENT,
          FeatureMeasurementDaoUtility.buildId(PHASE_FEATURE_MEASUREMENT),
          PHASE_FEATURE_MEASUREMENT,
          FeatureMeasurementDaoUtility.buildId(AMPLITUDE_FEATURE_MEASUREMENT),
          AMPLITUDE_FEATURE_MEASUREMENT);

  @BeforeAll
  static void setUp() {
    entityManagerFactory = CoiTestingEntityManagerFactory.createTesting();
    new StationRepositoryJpa(entityManagerFactory)
        .storeStations(List.of(UtilsTestFixtures.STATION));
  }

  @AfterAll
  static void tearDown() {
    entityManagerFactory.close();
  }

  @ParameterizedTest
  @MethodSource("getFromCoiArguments")
  void testFromCoiValidation(Class<? extends Exception> expectedException,
      NetworkMagnitudeSolution netMagSolution,
      EntityManager entityManager) {

    try {
      assertThrows(expectedException,
          () -> NetworkMagnitudeSolutionDaoUtility.fromCoi(netMagSolution, entityManager));
    } finally {
      if (entityManager != null) {
        entityManager.close();
      }
    }
  }

  static Stream<Arguments> getFromCoiArguments() {
    return Stream.of(
        arguments(NullPointerException.class, null, entityManagerFactory.createEntityManager()),
        arguments(NullPointerException.class, NETWORK_MAGNITUDE_SOLUTION, null),
        arguments(IllegalStateException.class, NETWORK_MAGNITUDE_SOLUTION,
            entityManagerFactory.createEntityManager()));
  }

  @Test
  void testFromCoi() {
    EntityManager entityManager = entityManagerFactory.createEntityManager();

    try {
      entityManager.getTransaction().begin();
      NetworkMagnitudeSolutionDao dao = NetworkMagnitudeSolutionDaoUtility
          .fromCoi(NETWORK_MAGNITUDE_SOLUTION, entityManager);

      assertEquals(NETWORK_MAGNITUDE_SOLUTION.getMagnitudeType(), dao.getMagnitudeType());
      assertEquals(NETWORK_MAGNITUDE_SOLUTION.getMagnitude(), dao.getMagnitude(), 0.0001);
      assertEquals(NETWORK_MAGNITUDE_SOLUTION.getUncertainty(), dao.getUncertainty(), 0.0001);
      assertEquals(NETWORK_MAGNITUDE_SOLUTION.getNetworkMagnitudeBehaviors().size(),
          dao.getNetworkMagnitudeBehaviors().size());
      assertEquals(NETWORK_MAGNITUDE_SOLUTION.getNetworkMagnitudeBehaviors().size(),
          dao.getNetworkMagnitudeBehaviors().size());
      dao.getNetworkMagnitudeBehaviors().stream()
          .map(netMagBehaviorDao -> NetworkMagnitudeBehaviorDaoUtility.toCoi(netMagBehaviorDao, AMPLITUDE_FEATURE_MEASUREMENT))
          .forEach(netMagBehavior ->
              assertTrue(NETWORK_MAGNITUDE_SOLUTION.getNetworkMagnitudeBehaviors().contains(netMagBehavior)));
    } finally {
      entityManager.getTransaction().rollback();
      entityManager.close();
    }
  }

  @ParameterizedTest
  @MethodSource("getToCoiArguments")
  void testToCoiValidation(NetworkMagnitudeSolutionDao netMagSolutionDao,
      Map<String, FeatureMeasurement<?>> featureMeasurementsById) {
    assertThrows(NullPointerException.class,
        () -> NetworkMagnitudeSolutionDaoUtility.toCoi(netMagSolutionDao, featureMeasurementsById));
  }

  static Stream<Arguments> getToCoiArguments() {
    return Stream.of(
        arguments(null, featureMeasurementsById),
        arguments(new NetworkMagnitudeSolutionDao(), null));
  }

  @Test
  void testToCoi() {
    EntityManager entityManager = entityManagerFactory.createEntityManager();

    try {
      entityManager.getTransaction().begin();
      assertEquals(NETWORK_MAGNITUDE_SOLUTION,
          NetworkMagnitudeSolutionDaoUtility.toCoi(
              NetworkMagnitudeSolutionDaoUtility.fromCoi(NETWORK_MAGNITUDE_SOLUTION, entityManager),
             featureMeasurementsById));
    } finally {
      entityManager.getTransaction().rollback();
      entityManager.close();
    }
  }

}