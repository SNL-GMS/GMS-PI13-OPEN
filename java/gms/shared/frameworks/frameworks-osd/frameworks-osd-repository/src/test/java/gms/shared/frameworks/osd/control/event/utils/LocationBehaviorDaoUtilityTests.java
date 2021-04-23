package gms.shared.frameworks.osd.control.event.utils;

import gms.shared.frameworks.osd.coi.CoiTestingEntityManagerFactory;
import gms.shared.frameworks.osd.coi.event.LocationBehavior;
import gms.shared.frameworks.osd.coi.event.repository.jpa.LocationBehaviorDao;
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
import java.util.UUID;
import java.util.stream.Stream;
import static gms.shared.frameworks.osd.coi.event.EventTestFixtures.ARRIVAL_TIME_FEATURE_MEASUREMENT;
import static gms.shared.frameworks.osd.coi.event.EventTestFixtures.LOCATION_BEHAVIOR;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class LocationBehaviorDaoUtilityTests {

  private static final double TOLERANCE = 0.000001;

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
      LocationBehavior behavior,
      EntityManager entityManager) {
    try {
      assertThrows(expectedException,
          () -> LocationBehaviorDaoUtility.fromCoi(behavior, entityManager));
    } finally {
      if (entityManager != null) {
        entityManager.close();
      }
    }
  }

  static Stream<Arguments> getFromCoiArguments() {
    return Stream.of(
        arguments(NullPointerException.class, null, entityManagerFactory.createEntityManager()),
        arguments(NullPointerException.class, LOCATION_BEHAVIOR, null),
        arguments(IllegalStateException.class,
            LOCATION_BEHAVIOR,
            entityManagerFactory.createEntityManager()));
  }

  @Test
  void fromCoiNew() {
    EntityManager entityManager = entityManagerFactory.createEntityManager();

    try {
      entityManager.getTransaction().begin();
      LocationBehaviorDao dao = LocationBehaviorDaoUtility.fromCoi(LOCATION_BEHAVIOR,
          entityManager);

      assertEquals(LOCATION_BEHAVIOR.getResidual(), dao.getResidual(), TOLERANCE);
      assertEquals(LOCATION_BEHAVIOR.getWeight(), dao.getWeight(), TOLERANCE);
      assertEquals(LOCATION_BEHAVIOR.isDefining(), dao.isDefining());
      assertEquals(LOCATION_BEHAVIOR.getFeaturePrediction(),
          FeaturePredictionDaoUtility.toCoi(dao.getFeaturePrediction()));
      assertEquals(LOCATION_BEHAVIOR.getFeatureMeasurement(),
          FeatureMeasurementDaoUtility.toCoi(dao.getFeatureMeasurement(),
              UtilsTestFixtures.CHANNEL));
    } finally {
      entityManager.getTransaction().rollback();
      entityManager.close();
    }
  }

  @ParameterizedTest
  @MethodSource("getToCoiArguments")
  void testToCoiValidation(LocationBehaviorDao dao, FeatureMeasurement<?> featureMeasurement) {
    assertThrows(NullPointerException.class,
        () -> LocationBehaviorDaoUtility.toCoi(dao, featureMeasurement));
  }

  static Stream<Arguments> getToCoiArguments() {
    return Stream.of(
        arguments(null, ARRIVAL_TIME_FEATURE_MEASUREMENT),
        arguments(new LocationBehaviorDao(), null));
  }

  @Test
  void toCoi() {
    EntityManager entityManager = entityManagerFactory.createEntityManager();

    try {
      LocationBehaviorDao dao = new LocationBehaviorDao();
      dao.setId(UUID.randomUUID());
      dao.setResidual(LOCATION_BEHAVIOR.getResidual());
      dao.setWeight(LOCATION_BEHAVIOR.getWeight());
      dao.setDefining(LOCATION_BEHAVIOR.isDefining());
      entityManager.getTransaction().begin();
      dao.setFeaturePrediction(FeaturePredictionDaoUtility.fromCoi(LOCATION_BEHAVIOR.getFeaturePrediction(), entityManager));
      dao.setFeatureMeasurement(FeatureMeasurementDaoUtility.fromCoi(LOCATION_BEHAVIOR.getFeatureMeasurement(), entityManager));
      entityManager.getTransaction().commit();

      LocationBehavior actual = LocationBehaviorDaoUtility.toCoi(dao, LOCATION_BEHAVIOR.getFeatureMeasurement());

      assertEquals(LOCATION_BEHAVIOR, actual);
    } finally {
      entityManager.close();
    }
  }
}