package gms.shared.frameworks.osd.control.event.utils;

import gms.shared.frameworks.osd.coi.CoiTestingEntityManagerFactory;
import gms.shared.frameworks.osd.coi.event.EventTestFixtures;
import gms.shared.frameworks.osd.coi.event.NetworkMagnitudeBehavior;
import gms.shared.frameworks.osd.coi.event.repository.jpa.NetworkMagnitudeBehaviorDao;
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
import static gms.shared.frameworks.osd.coi.event.EventTestFixtures.NETWORK_MAGNITUDE_BEHAVIOR;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class NetworkMagnitudeBehaviorDaoUtilityTests {

  private static EntityManagerFactory entityManagerFactory;

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
      NetworkMagnitudeBehavior netMagBehavior,
      EntityManager entityManager) {

    try {
      assertThrows(expectedException,
          () -> NetworkMagnitudeBehaviorDaoUtility.fromCoi(netMagBehavior, entityManager));
    } finally {
      if (entityManager != null) {
        entityManager.close();
      }
    }
  }

  static Stream<Arguments> getFromCoiArguments() {
    return Stream.of(
        arguments(NullPointerException.class, null, entityManagerFactory.createEntityManager()),
        arguments(NullPointerException.class, NETWORK_MAGNITUDE_BEHAVIOR, null),
        arguments(IllegalStateException.class,
            NETWORK_MAGNITUDE_BEHAVIOR,
            entityManagerFactory.createEntityManager()));
  }

  @Test
  void testFromCoi() {
    EntityManager entityManager = entityManagerFactory.createEntityManager();

    try {
      entityManager.getTransaction().begin();
      NetworkMagnitudeBehaviorDao dao = NetworkMagnitudeBehaviorDaoUtility
          .fromCoi(NETWORK_MAGNITUDE_BEHAVIOR, entityManager);

      assertEquals(NETWORK_MAGNITUDE_BEHAVIOR.isDefining(), dao.isDefining());
      assertEquals(NETWORK_MAGNITUDE_BEHAVIOR.getResidual(), dao.getResidual());
      assertEquals(NETWORK_MAGNITUDE_BEHAVIOR.getWeight(), dao.getWeight());
      assertEquals(NETWORK_MAGNITUDE_BEHAVIOR.getStationMagnitudeSolution(),
          StationMagnitudeSolutionDaoUtility.toCoi(dao.getStationMagnitudeSolution(),
              FeatureMeasurementDaoUtility.toCoi(dao.getStationMagnitudeSolution().getMeasurement(),
                  UtilsTestFixtures.CHANNEL)));
    } finally {
      entityManager.getTransaction().rollback();
      entityManager.close();
    }
  }

  @ParameterizedTest
  @MethodSource("getToCoiArguments")
  void testToCoiValidation(NetworkMagnitudeBehaviorDao netMagBehaviorDao,
      FeatureMeasurement<AmplitudeMeasurementValue> featureMeasurement) {
    assertThrows(NullPointerException.class,
        () -> NetworkMagnitudeBehaviorDaoUtility.toCoi(netMagBehaviorDao, featureMeasurement));
  }

  static Stream<Arguments> getToCoiArguments() {
    return Stream.of(
        arguments(null, AMPLITUDE_FEATURE_MEASUREMENT),
        arguments(new NetworkMagnitudeBehaviorDao(), null));
  }

  @Test
  void testToCoi() {
    EntityManager entityManager = entityManagerFactory.createEntityManager();

    try {
      entityManager.getTransaction().begin();
      assertEquals(NETWORK_MAGNITUDE_BEHAVIOR,
          NetworkMagnitudeBehaviorDaoUtility.toCoi(
              NetworkMagnitudeBehaviorDaoUtility.fromCoi(NETWORK_MAGNITUDE_BEHAVIOR, entityManager),
              EventTestFixtures.AMPLITUDE_FEATURE_MEASUREMENT));
    } finally {
      entityManager.getTransaction().rollback();
      entityManager.close();
    }
  }
}