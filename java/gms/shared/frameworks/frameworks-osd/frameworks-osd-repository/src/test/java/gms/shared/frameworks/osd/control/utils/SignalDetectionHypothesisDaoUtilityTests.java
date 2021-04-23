package gms.shared.frameworks.osd.control.utils;

import com.google.common.base.Functions;
import gms.shared.frameworks.osd.coi.CoiTestingEntityManagerFactory;
import gms.shared.frameworks.osd.coi.InstantValue;
import gms.shared.frameworks.osd.coi.signaldetection.AmplitudeMeasurementValue;
import gms.shared.frameworks.osd.coi.signaldetection.DurationMeasurementValue;
import gms.shared.frameworks.osd.coi.signaldetection.EnumeratedMeasurementValue;
import gms.shared.frameworks.osd.coi.signaldetection.FeatureMeasurement;
import gms.shared.frameworks.osd.coi.signaldetection.FeatureMeasurementType;
import gms.shared.frameworks.osd.coi.signaldetection.FeatureMeasurementTypes;
import gms.shared.frameworks.osd.coi.signaldetection.MeasuredChannelSegmentDescriptor;
import gms.shared.frameworks.osd.coi.signaldetection.NumericMeasurementValue;
import gms.shared.frameworks.osd.coi.signaldetection.SignalDetectionHypothesis;
import gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.dataaccessobjects.AmplitudeFeatureMeasurementDao;
import gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.dataaccessobjects.DurationFeatureMeasurementDao;
import gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.dataaccessobjects.FeatureMeasurementDao;
import gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.dataaccessobjects.FirstMotionFeatureMeasurementDao;
import gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.dataaccessobjects.InstantFeatureMeasurementDao;
import gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.dataaccessobjects.MeasuredChannelSegmentDescriptorDao;
import gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.dataaccessobjects.NumericFeatureMeasurementDao;
import gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.dataaccessobjects.PhaseFeatureMeasurementDao;
import gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.dataaccessobjects.SignalDetectionDao;
import gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.dataaccessobjects.SignalDetectionHypothesisDao;
import gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.dataaccessobjects.StationDao;
import gms.shared.frameworks.osd.coi.test.utils.UtilsTestFixtures;
import gms.shared.frameworks.osd.control.station.StationRepositoryJpa;
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
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import static gms.shared.frameworks.osd.coi.signaldetection.SignalDetectionTestFixtures.AMPLITUDE_FEATURE_MEASUREMENT;
import static gms.shared.frameworks.osd.coi.signaldetection.SignalDetectionTestFixtures.ARRIVAL_TIME_FEATURE_MEASUREMENT;
import static gms.shared.frameworks.osd.coi.signaldetection.SignalDetectionTestFixtures.PHASE_FEATURE_MEASUREMENT;
import static gms.shared.frameworks.osd.coi.signaldetection.SignalDetectionTestFixtures.SIGNAL_DETECTION;
import static gms.shared.frameworks.osd.coi.signaldetection.SignalDetectionTestFixtures.SIGNAL_DETECTION_HYPOTHESIS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class SignalDetectionHypothesisDaoUtilityTests {

  private static EntityManagerFactory entityManagerFactory;

  @BeforeAll
  static void setUp() {
    entityManagerFactory = CoiTestingEntityManagerFactory.createTesting();
    new StationRepositoryJpa(entityManagerFactory).storeStations(List.of(UtilsTestFixtures.STATION));
  }

  @AfterEach
  void deleteData() {
    EntityManager entityManager = entityManagerFactory.createEntityManager();
    entityManager.getTransaction().begin();
    entityManager.createQuery("DELETE FROM SignalDetectionHypothesisDao").executeUpdate();
    entityManager.createQuery("DELETE FROM feature_measurement_amplitude").executeUpdate();
    entityManager.createQuery("DELETE FROM feature_measurement_duration").executeUpdate();
    entityManager.createQuery("DELETE FROM feature_measurement_first_motion").executeUpdate();
    entityManager.createQuery("DELETE FROM feature_measurement_numeric").executeUpdate();
    entityManager.createQuery("DELETE FROM feature_measurement_instant").executeUpdate();
    entityManager.getTransaction().commit();
    entityManager.close();
  }

  @AfterAll
  static void tearDown() {
    entityManagerFactory.close();
  }

  @ParameterizedTest
  @MethodSource("getFromCoiArguments")
  void testFromCoiValidation(Class<? extends Exception> expectedException,
      SignalDetectionHypothesis hypothesis,
      EntityManager entityManager) {
    try {
      assertThrows(expectedException,
          () -> SignalDetectionHypothesisDaoUtility.fromCoi(hypothesis, entityManager));
    } finally {
      if (entityManager != null) {
        entityManager.close();
      }
    }
  }

  static Stream<Arguments> getFromCoiArguments() {
    return Stream.of(
        arguments(NullPointerException.class, null, entityManagerFactory.createEntityManager()),
        arguments(NullPointerException.class, SIGNAL_DETECTION_HYPOTHESIS, null),
        arguments(IllegalStateException.class, SIGNAL_DETECTION_HYPOTHESIS,
            entityManagerFactory.createEntityManager())
    );
  }

  @Test
  public void testFromCoiNoParentDetection() {
    EntityManager entityManager = entityManagerFactory.createEntityManager();

    try {
      entityManager.getTransaction().begin();
      NullPointerException ex = assertThrows(NullPointerException.class,
          () -> SignalDetectionHypothesisDaoUtility.fromCoi(SIGNAL_DETECTION_HYPOTHESIS,
              entityManager));
      assertTrue(ex.getMessage().contains("Cannot create SignalDetectionHypothesisDao from a " +
          "SignalDetection that does not exist"));
    } finally {
      entityManager.getTransaction().rollback();
      entityManager.close();
    }
  }

  @Test
  public void testFromCoiNoParentHypothesis() {
    EntityManager entityManager = entityManagerFactory.createEntityManager();

    try {
      entityManager.getTransaction().begin();

      StationDao stationDao = entityManager.find(StationDao.class,
          UtilsTestFixtures.STATION.getName());

      SignalDetectionDao detectionDao = new SignalDetectionDao();
      detectionDao.setId(UUID.randomUUID());
      detectionDao.setMonitoringOrganization("Test");
      detectionDao.setStation(stationDao);

      entityManager.persist(detectionDao);
      entityManager.getTransaction().commit();

      entityManager.getTransaction().begin();

      SignalDetectionHypothesis updated = SIGNAL_DETECTION_HYPOTHESIS.toBuilder()
          .generateId()
          .setParentSignalDetectionId(detectionDao.getId())
          .setParentSignalDetectionHypothesisId(Optional.of(UUID.randomUUID()))
          .build();

      NullPointerException ex = assertThrows(NullPointerException.class,
          () -> SignalDetectionHypothesisDaoUtility.fromCoi(updated, entityManager));
      assertTrue(ex.getMessage().contains("Cannot create SignalDetectionHypothesisDao from a " +
          "parent hypothesis that doesn't exist"));
    } finally {
      entityManager.getTransaction().rollback();
      entityManager.close();
    }
  }

  @Test
  void testFromCoiNoParentHypothesisProvided() {
    EntityManager entityManager = entityManagerFactory.createEntityManager();

    try {
      entityManager.getTransaction().begin();
      StationDao stationDao = entityManager.find(StationDao.class,
          UtilsTestFixtures.STATION.getName());

      SignalDetectionDao detectionDao = new SignalDetectionDao();
      detectionDao.setId(UUID.randomUUID());
      detectionDao.setMonitoringOrganization("Test");
      detectionDao.setStation(stationDao);

      entityManager.persist(detectionDao);

      entityManager.getTransaction().commit();

      entityManager.getTransaction().begin();
      SignalDetectionHypothesis hypothesis = SIGNAL_DETECTION_HYPOTHESIS.toBuilder()
          .setParentSignalDetectionId(detectionDao.getId())
          .build();
      SignalDetectionHypothesisDao hypothesisDao =
          SignalDetectionHypothesisDaoUtility.fromCoi(hypothesis, entityManager);
      compareHypothesisToDao(hypothesis, hypothesisDao);
    } finally {
      entityManager.getTransaction().rollback();
      entityManager.close();
    }
  }

  @Test
  void testFromCoiWithParentHypothesis() {
    EntityManager entityManager = entityManagerFactory.createEntityManager();

    try {
      entityManager.getTransaction().begin();
      StationDao stationDao = entityManager.find(StationDao.class, UtilsTestFixtures.STATION.getName());

      SignalDetectionDao detectionDao = new SignalDetectionDao();
      detectionDao.setId(SIGNAL_DETECTION.getId());
      detectionDao.setMonitoringOrganization(SIGNAL_DETECTION.getMonitoringOrganization());
      detectionDao.setStation(stationDao);
      entityManager.persist(detectionDao);

      SignalDetectionHypothesisDao hypothesisDao = new SignalDetectionHypothesisDao(SIGNAL_DETECTION_HYPOTHESIS.getId(),
          detectionDao,
          SIGNAL_DETECTION_HYPOTHESIS.getMonitoringOrganization(),
          SIGNAL_DETECTION_HYPOTHESIS.getStationName(),
          null,
          false,
          (InstantFeatureMeasurementDao) FeatureMeasurementDaoUtility.fromCoi(ARRIVAL_TIME_FEATURE_MEASUREMENT, entityManager),
          (PhaseFeatureMeasurementDao) FeatureMeasurementDaoUtility.fromCoi(PHASE_FEATURE_MEASUREMENT, entityManager),
          List.of());

      entityManager.persist(hypothesisDao);
      entityManager.getTransaction().commit();

      entityManager.getTransaction().begin();
      SignalDetectionHypothesis updated = SIGNAL_DETECTION_HYPOTHESIS.toBuilder()
          .generateId()
          .setParentSignalDetectionHypothesisId(Optional.of(SIGNAL_DETECTION_HYPOTHESIS.getId()))
          .addMeasurement(AMPLITUDE_FEATURE_MEASUREMENT)
          .build();

      SignalDetectionHypothesisDao updatedDao = SignalDetectionHypothesisDaoUtility.fromCoi(updated, entityManager);
    } finally {
      entityManager.getTransaction().rollback();
      entityManager.close();
    }
  }

  private void compareHypothesisToDao(SignalDetectionHypothesis hypothesis,
      SignalDetectionHypothesisDao hypothesisDao) {
    assertNotNull(hypothesisDao);
    hypothesis.getParentSignalDetectionHypothesisId()
        .ifPresentOrElse(id -> assertEquals(id,
            hypothesisDao.getParentSignalDetectionHypothesis().getId()),
            () -> assertTrue(hypothesisDao.getParentSignalDetectionHypothesis() == null));
    assertEquals(hypothesis.getParentSignalDetectionId(),
        hypothesisDao.getParentSignalDetection().getId());
    assertEquals(hypothesis.getMonitoringOrganization(), hypothesisDao.getMonitoringOrganization());
    assertEquals(hypothesis.getStationName(), hypothesisDao.getStationName());
    assertEquals(hypothesis.isRejected(), hypothesisDao.isRejected());

    Map<FeatureMeasurementType<?>, FeatureMeasurementDao<?>> featureMeasurementDaosByType =
        hypothesisDao.getFeatureMeasurements().stream()
            .collect(Collectors.toMap(fmDao -> fmDao.getFeatureMeasurementType().getCoiType(),
                Functions.identity()));

    featureMeasurementDaosByType.put(FeatureMeasurementTypes.ARRIVAL_TIME,
        hypothesisDao.getArrivalTimeMeasurement());

    featureMeasurementDaosByType.put(FeatureMeasurementTypes.PHASE,
        hypothesisDao.getPhaseMeasurement());

    hypothesis.getFeatureMeasurements().stream()
        .forEach(featureMeasurement -> compareFeatureMeasurements(featureMeasurement,
            featureMeasurementDaosByType.get(featureMeasurement.getFeatureMeasurementType())));
  }

  private void compareFeatureMeasurements(FeatureMeasurement featureMeasurement,
      FeatureMeasurementDao featureMeasurementDao) {
    assertEquals(featureMeasurement.getFeatureMeasurementType(),
        featureMeasurementDao.getFeatureMeasurementType().getCoiType());
    compareMeasuredChannelSegmentDescriptors(featureMeasurement.getMeasuredChannelSegmentDescriptor(),
        featureMeasurementDao.getMeasuredChannelSegmentDescriptor());

    Object measurementValue = featureMeasurement.getMeasurementValue();
    Class<?> type = featureMeasurement.getFeatureMeasurementType().getMeasurementValueType();
    if (type.equals(AmplitudeMeasurementValue.class)) {
      assertEquals(measurementValue,
          ((AmplitudeFeatureMeasurementDao) featureMeasurementDao).toCoiMeasurementValue());
    } else if (type.equals(DurationMeasurementValue.class)) {
      assertEquals(measurementValue,
          ((DurationFeatureMeasurementDao) featureMeasurementDao).toCoiMeasurementValue());
    } else if (type.equals(EnumeratedMeasurementValue.FirstMotionMeasurementValue.class)) {
      assertEquals(measurementValue,
          ((FirstMotionFeatureMeasurementDao) featureMeasurementDao).toCoiMeasurementValue());
    } else if (type.equals(NumericMeasurementValue.class)) {
      assertEquals(measurementValue,
          ((NumericFeatureMeasurementDao) featureMeasurementDao).toCoiMeasurementValue());
    } else if (type.equals(EnumeratedMeasurementValue.PhaseTypeMeasurementValue.class)) {
      assertEquals(measurementValue,
          ((PhaseFeatureMeasurementDao) featureMeasurementDao).toCoiMeasurementValue());
    } else if (type.equals(InstantValue.class)) {
      assertEquals(measurementValue,
          ((InstantFeatureMeasurementDao) featureMeasurementDao).toCoiMeasurementValue());
    } else {
      fail();
    }
  }

  private void compareMeasuredChannelSegmentDescriptors(MeasuredChannelSegmentDescriptor descriptor,
      MeasuredChannelSegmentDescriptorDao descriptorDao) {
    assertEquals(descriptor.getChannelName(), descriptorDao.getChannel().getName());
    assertEquals(descriptor.getMeasuredChannelSegmentStartTime(),
        descriptorDao.getMeasuredChannelSegmentStartTime());
    assertEquals(descriptor.getMeasuredChannelSegmentEndTime(),
        descriptorDao.getMeasuredChannelSegmentEndTime());
    assertEquals(descriptor.getMeasuredChannelSegmentCreationTime(),
        descriptorDao.getMeasuredChannelSegmentCreationTime());
  }
}