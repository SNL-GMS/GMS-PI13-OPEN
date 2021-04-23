package gms.shared.frameworks.osd.control.event.utils;

import gms.shared.frameworks.osd.coi.CoiTestingEntityManagerFactory;
import gms.shared.frameworks.osd.coi.event.EventTestFixtures;
import gms.shared.frameworks.osd.coi.event.FeaturePrediction;
import gms.shared.frameworks.osd.coi.event.LocationSolution;
import gms.shared.frameworks.osd.coi.event.repository.jpa.EventLocationDao;
import gms.shared.frameworks.osd.coi.event.repository.jpa.FeaturePredictionDao;
import gms.shared.frameworks.osd.coi.event.repository.jpa.LocationBehaviorDao;
import gms.shared.frameworks.osd.coi.event.repository.jpa.LocationRestraintDao;
import gms.shared.frameworks.osd.coi.event.repository.jpa.LocationSolutionDao;
import gms.shared.frameworks.osd.coi.event.repository.jpa.LocationUncertaintyDao;
import gms.shared.frameworks.osd.coi.event.repository.jpa.NetworkMagnitudeSolutionDao;
import gms.shared.frameworks.osd.coi.signaldetection.FeatureMeasurement;
import gms.shared.frameworks.osd.coi.test.utils.UtilsTestFixtures;
import gms.shared.frameworks.osd.control.station.StationRepositoryJpa;
import gms.shared.frameworks.osd.control.utils.FeatureMeasurementDaoUtility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import static gms.shared.frameworks.osd.coi.event.EventTestFixtures.AMPLITUDE_FEATURE_MEASUREMENT;
import static gms.shared.frameworks.osd.coi.event.EventTestFixtures.ARRIVAL_TIME_FEATURE_MEASUREMENT;
import static gms.shared.frameworks.osd.coi.event.EventTestFixtures.FEATURE_PREDICTION;
import static gms.shared.frameworks.osd.coi.event.EventTestFixtures.LOCATION_SOLUTION;
import static gms.shared.frameworks.osd.coi.event.EventTestFixtures.PHASE_FEATURE_MEASUREMENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class LocationSolutionDaoUtilityTests {

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
  }

  @AfterEach
  void deleteData() {
  }

  @AfterAll
  static void tearDown() {
    entityManagerFactory.close();
  }

  @ParameterizedTest
  @MethodSource("getFromCoiArguments")
  void testFromCoiValidation(Class<? extends Exception> expectedException,
      LocationSolution locationSolution,
      EntityManager entityManager) {
    try {
      assertThrows(expectedException,
          () -> LocationSolutionDaoUtility.fromCoi(locationSolution, entityManager));
    } finally {
      if (entityManager != null) {
        entityManager.close();
      }
    }
  }

  static Stream<Arguments> getFromCoiArguments() {
    return Stream.of(
        arguments(NullPointerException.class,
            null,
            entityManagerFactory.createEntityManager()),
        arguments(NullPointerException.class,
            LOCATION_SOLUTION,
            null),
        arguments(IllegalStateException.class,
            LOCATION_SOLUTION,
            entityManagerFactory.createEntityManager()));
  }

  @Test
  void testFromCoiNew() {
    EntityManager entityManager = entityManagerFactory.createEntityManager();

    try {
      entityManager.getTransaction().begin();
      LocationSolutionDao dao = LocationSolutionDaoUtility.fromCoi(LOCATION_SOLUTION,
          entityManager);

      assertEquals(LOCATION_SOLUTION.getId(), dao.getId());
      assertEquals(LOCATION_SOLUTION.getLocation(), dao.getLocation().toCoi());
      assertEquals(LOCATION_SOLUTION.getLocationRestraint(), dao.getLocationRestraint().toCoi());
      assertEquals(LOCATION_SOLUTION.getLocationUncertainty().isPresent(),
          dao.getLocationUncertainty() != null);
      assertEquals(LOCATION_SOLUTION.getFeaturePredictions().size(),
          dao.getFeaturePredictions().size());
      assertTrue(LOCATION_SOLUTION.getFeaturePredictions().containsAll(dao.getFeaturePredictions()
          .stream()
          .map(FeaturePredictionDaoUtility::toCoi)
          .collect(Collectors.toSet())));
      assertEquals(LOCATION_SOLUTION.getLocationBehaviors().size(),
          dao.getLocationBehaviors().size());
      assertTrue(LOCATION_SOLUTION.getLocationBehaviors().containsAll(dao.getLocationBehaviors()
          .stream()
          .map(behaviorDao -> LocationBehaviorDaoUtility.toCoi(behaviorDao,
              EventTestFixtures.ARRIVAL_TIME_FEATURE_MEASUREMENT))
          .collect(Collectors.toSet())));
      assertEquals(LOCATION_SOLUTION.getNetworkMagnitudeSolutions().size(),
          dao.getNetworkMagnitudeSolutions().size());
      assertTrue(LOCATION_SOLUTION.getNetworkMagnitudeSolutions().containsAll(
          dao.getNetworkMagnitudeSolutions().stream()
              .map(netMagSolutionDao -> NetworkMagnitudeSolutionDaoUtility.toCoi(netMagSolutionDao,
                  featureMeasurementsById))
              .collect(Collectors.toSet())));
    } finally {
      entityManager.getTransaction().rollback();
      entityManager.close();
    }
  }

  @Test
  public void testFromCoiExisting() {
    EntityManager entityManager = entityManagerFactory.createEntityManager();

    try {
      LocationSolutionDao expected = new LocationSolutionDao();
      expected.setId(LOCATION_SOLUTION.getId());
      expected.setLocation(new EventLocationDao(LOCATION_SOLUTION.getLocation()));
      expected.setLocationRestraint(new LocationRestraintDao(LOCATION_SOLUTION.getLocationRestraint()));
      expected.setLocationUncertainty(new LocationUncertaintyDao(EventTestFixtures.LOCATION_UNCERTAINTY));

      entityManager.getTransaction().begin();
      Set<LocationBehaviorDao> locationBehaviorDaos = LOCATION_SOLUTION.getLocationBehaviors()
          .stream()
          .map(behavior -> LocationBehaviorDaoUtility.fromCoi(behavior, entityManager))
          .collect(Collectors.toSet());
      expected.setLocationBehaviors(locationBehaviorDaos);

      Set<FeaturePredictionDao<?>> featurePredictionDaos = LOCATION_SOLUTION.getFeaturePredictions()
          .stream()
          .map(fp -> FeaturePredictionDaoUtility.fromCoi(fp, entityManager))
          .collect(Collectors.toSet());
      expected.setFeaturePredictions(featurePredictionDaos);

      List<NetworkMagnitudeSolutionDao> netMagSolutionDaos =
          LOCATION_SOLUTION.getNetworkMagnitudeSolutions()
              .stream()
              .map(netMagSolution -> NetworkMagnitudeSolutionDaoUtility.fromCoi(netMagSolution,
                  entityManager))
              .collect(Collectors.toList());
      expected.setNetworkMagnitudeSolutions(netMagSolutionDaos);

      entityManager.persist(expected);

      entityManager.getTransaction().commit();

      entityManager.getTransaction().begin();
      LocationSolutionDao actual = LocationSolutionDaoUtility.fromCoi(LOCATION_SOLUTION,
          entityManager);
      entityManager.getTransaction().commit();
      assertEquals(expected.getId(), actual.getId());
      assertEquals(expected.getLocation(), actual.getLocation());
      assertEquals(expected.getLocationRestraint(), actual.getLocationRestraint());
      assertEquals(expected.getLocationUncertainty(), actual.getLocationUncertainty());
      assertEquals(expected.getLocationBehaviors().size(), actual.getLocationBehaviors().size());

      LocationBehaviorDao expectedBehavior = expected.getLocationBehaviors().iterator().next();
      LocationBehaviorDao actualBehavior = actual.getLocationBehaviors().iterator().next();
      assertEquals(expectedBehavior, actualBehavior);
      assertEquals(expectedBehavior.hashCode(), actualBehavior.hashCode());
      assertTrue(expected.getLocationBehaviors().containsAll(actual.getLocationBehaviors()));
      assertEquals(expected.getFeaturePredictions().size(), actual.getFeaturePredictions().size());
      assertTrue(expected.getFeaturePredictions().containsAll(actual.getFeaturePredictions()));
      assertEquals(expected.getNetworkMagnitudeSolutions().size(),
          actual.getNetworkMagnitudeSolutions().size());
      assertTrue(expected.getNetworkMagnitudeSolutions().containsAll(actual.getNetworkMagnitudeSolutions()));
      assertEquals(expected, actual);
    } finally {
      entityManager.close();
    }
  }

  @ParameterizedTest
  @MethodSource("getToCoiArguments")
  void testToCoiValidation(LocationSolutionDao dao,
      Map<UUID, FeaturePrediction<?>> featurePredictionsById,
      Map<String, FeatureMeasurement<?>> featureMeasurementsById) {
    assertThrows(NullPointerException.class,
        () -> LocationSolutionDaoUtility.toCoi(dao, featurePredictionsById, featureMeasurementsById));
  }

  static Stream<Arguments> getToCoiArguments() {
    return Stream.of(
        arguments(null, featurePredictionsById, featureMeasurementsById),
        arguments(new LocationSolutionDao(), null, featureMeasurementsById),
        arguments(new LocationSolutionDao(), featurePredictionsById, null));
  }

  @Test
  void testToCoi() {
    EntityManager entityManager = entityManagerFactory.createEntityManager();

    try {
      entityManager.getTransaction().begin();
      assertEquals(LOCATION_SOLUTION, LocationSolutionDaoUtility.toCoi(
          LocationSolutionDaoUtility.fromCoi(LOCATION_SOLUTION, entityManager),
          featurePredictionsById,
          featureMeasurementsById));
    } finally {
      entityManager.getTransaction().rollback();
      entityManager.close();
    }
  }
}