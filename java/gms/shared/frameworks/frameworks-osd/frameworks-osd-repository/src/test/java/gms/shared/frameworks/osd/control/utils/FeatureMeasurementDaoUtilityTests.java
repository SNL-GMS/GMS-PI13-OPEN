package gms.shared.frameworks.osd.control.utils;

import gms.shared.frameworks.osd.coi.CoiTestingEntityManagerFactory;
import gms.shared.frameworks.osd.coi.InstantValue;
import gms.shared.frameworks.osd.coi.channel.Channel;
import gms.shared.frameworks.osd.coi.signaldetection.FeatureMeasurement;
import gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.dataaccessobjects.FeatureMeasurementDao;
import gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.dataaccessobjects.InstantFeatureMeasurementDao;
import gms.shared.frameworks.osd.coi.test.utils.UtilsTestFixtures;
import gms.shared.frameworks.osd.control.station.StationRepositoryJpa;
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
import static gms.shared.frameworks.osd.coi.event.EventTestFixtures.ARRIVAL_TIME_FEATURE_MEASUREMENT;
import static gms.shared.frameworks.osd.coi.event.EventTestFixtures.ARRIVAL_TIME_MEASUREMENT;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class FeatureMeasurementDaoUtilityTests {

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
      FeatureMeasurement<?> featureMeasurement, EntityManager entityManager) {
    try {
      assertThrows(expectedException,
          () -> FeatureMeasurementDaoUtility.fromCoi(featureMeasurement, entityManager));
    } finally {
      if (entityManager != null) {
        entityManager.close();
      }
    }
  }

  static Stream<Arguments> getFromCoiArguments() {
    return Stream.of(
        arguments(NullPointerException.class, null, entityManagerFactory.createEntityManager()),
        arguments(NullPointerException.class, ARRIVAL_TIME_FEATURE_MEASUREMENT, null),
        arguments(IllegalStateException.class, ARRIVAL_TIME_FEATURE_MEASUREMENT,
            entityManagerFactory.createEntityManager()));
  }

  @Test
  void testFromCoiNew() {
    EntityManager entityManager = entityManagerFactory.createEntityManager();

    try {
      entityManager.getTransaction().begin();
      InstantFeatureMeasurementDao dao = (InstantFeatureMeasurementDao) FeatureMeasurementDaoUtility
          .fromCoi(ARRIVAL_TIME_FEATURE_MEASUREMENT, entityManager);

      assertEquals(UtilsTestFixtures.DESCRIPTOR.getMeasuredChannelSegmentCreationTime(),
          dao.getMeasuredChannelSegmentDescriptor().getMeasuredChannelSegmentCreationTime());
      assertEquals(UtilsTestFixtures.DESCRIPTOR.getMeasuredChannelSegmentStartTime(),
          dao.getMeasuredChannelSegmentDescriptor().getMeasuredChannelSegmentStartTime());
      assertEquals(UtilsTestFixtures.DESCRIPTOR.getMeasuredChannelSegmentEndTime(),
          dao.getMeasuredChannelSegmentDescriptor().getMeasuredChannelSegmentEndTime());
      assertEquals(UtilsTestFixtures.DESCRIPTOR.getChannelName(),
          dao.getMeasuredChannelSegmentDescriptor().getChannel().getName());

      assertEquals(ARRIVAL_TIME_FEATURE_MEASUREMENT.getFeatureMeasurementType().getFeatureMeasurementTypeName(),
          dao.getFeatureMeasurementType().getCoiType().getFeatureMeasurementTypeName());
      assertEquals(ARRIVAL_TIME_MEASUREMENT, dao.getValue().toCoi());
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

      InstantFeatureMeasurementDao expected =
          new InstantFeatureMeasurementDao(FeatureMeasurementDaoUtility.buildId(ARRIVAL_TIME_FEATURE_MEASUREMENT),
              ARRIVAL_TIME_FEATURE_MEASUREMENT,
              MeasuredChannelSegmentDescriptorDaoUtility.fromCoi(UtilsTestFixtures.DESCRIPTOR,
                  entityManager));

      entityManager.persist(expected);
      entityManager.getTransaction().commit();

      entityManager.getTransaction().begin();
      InstantFeatureMeasurementDao actual =
          (InstantFeatureMeasurementDao) FeatureMeasurementDaoUtility.fromCoi(
              ARRIVAL_TIME_FEATURE_MEASUREMENT,
              entityManager);

      assertEquals(expected, actual);
    } finally {
      entityManager.getTransaction().rollback();
      entityManager.close();
    }
  }

  @ParameterizedTest
  @MethodSource("getToCoiArguments")
  void testToCoiValidation(FeatureMeasurementDao<?> featureMeasurementDao, Channel channel) {
    assertThrows(NullPointerException.class,
        () -> FeatureMeasurementDaoUtility.toCoi(featureMeasurementDao, channel));
  }

  static Stream<Arguments> getToCoiArguments() {
    return Stream.of(
        arguments(null, UtilsTestFixtures.CHANNEL),
        arguments(new InstantFeatureMeasurementDao(), null));
  }

  @Test
  void testToCoi() {
    EntityManager entityManager = entityManagerFactory.createEntityManager();

    try {
      entityManager.getTransaction().begin();
      InstantFeatureMeasurementDao dao =
          new InstantFeatureMeasurementDao(FeatureMeasurementDaoUtility.buildId(ARRIVAL_TIME_FEATURE_MEASUREMENT),
              ARRIVAL_TIME_FEATURE_MEASUREMENT,
              MeasuredChannelSegmentDescriptorDaoUtility.fromCoi(UtilsTestFixtures.DESCRIPTOR,
                  entityManager));
      entityManager.getTransaction().commit();

      FeatureMeasurement<InstantValue> actual = FeatureMeasurementDaoUtility.toCoi(dao, UtilsTestFixtures.CHANNEL);

      assertEquals(dao.getMeasuredChannelSegmentDescriptor().getChannel().getName(),
          actual.getMeasuredChannelSegmentDescriptor().getChannelName());
      assertEquals(dao.getMeasuredChannelSegmentDescriptor().getMeasuredChannelSegmentCreationTime(),
          actual.getMeasuredChannelSegmentDescriptor().getMeasuredChannelSegmentCreationTime());
      assertEquals(dao.getMeasuredChannelSegmentDescriptor().getMeasuredChannelSegmentStartTime(),
          actual.getMeasuredChannelSegmentDescriptor().getMeasuredChannelSegmentStartTime());
      assertEquals(dao.getMeasuredChannelSegmentDescriptor().getMeasuredChannelSegmentEndTime(),
          actual.getMeasuredChannelSegmentDescriptor().getMeasuredChannelSegmentEndTime());

      assertEquals(dao.getFeatureMeasurementType().getCoiType().getFeatureMeasurementTypeName(),
          actual.getFeatureMeasurementType().getFeatureMeasurementTypeName());
      assertEquals(dao.getValue().toCoi(), actual.getMeasurementValue());
    } finally {
      entityManager.close();
    }
  }
}