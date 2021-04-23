package gms.shared.frameworks.osd.control.signaldetection;

import com.google.common.base.Functions;
import gms.shared.frameworks.osd.api.station.StationRepositoryInterface;
import gms.shared.frameworks.osd.api.util.StationsTimeRangeRequest;
import gms.shared.frameworks.osd.coi.CoiTestingEntityManagerFactory;
import gms.shared.frameworks.osd.coi.DoubleValue;
import gms.shared.frameworks.osd.coi.InstantValue;
import gms.shared.frameworks.osd.coi.PhaseType;
import gms.shared.frameworks.osd.coi.Units;
import gms.shared.frameworks.osd.coi.channel.Channel;
import gms.shared.frameworks.osd.coi.channel.ChannelBandType;
import gms.shared.frameworks.osd.coi.channel.ChannelDataType;
import gms.shared.frameworks.osd.coi.channel.ChannelGroup;
import gms.shared.frameworks.osd.coi.channel.ChannelInstrumentType;
import gms.shared.frameworks.osd.coi.channel.ChannelOrientationType;
import gms.shared.frameworks.osd.coi.channel.ChannelProcessingMetadataType;
import gms.shared.frameworks.osd.coi.channel.Orientation;
import gms.shared.frameworks.osd.coi.signaldetection.AmplitudeMeasurementValue;
import gms.shared.frameworks.osd.coi.signaldetection.DurationMeasurementValue;
import gms.shared.frameworks.osd.coi.signaldetection.EnumeratedMeasurementValue;
import gms.shared.frameworks.osd.coi.signaldetection.FeatureMeasurement;
import gms.shared.frameworks.osd.coi.signaldetection.FeatureMeasurementType;
import gms.shared.frameworks.osd.coi.signaldetection.FeatureMeasurementTypes;
import gms.shared.frameworks.osd.coi.signaldetection.Location;
import gms.shared.frameworks.osd.coi.signaldetection.MeasuredChannelSegmentDescriptor;
import gms.shared.frameworks.osd.coi.signaldetection.NumericMeasurementValue;
import gms.shared.frameworks.osd.coi.signaldetection.SignalDetection;
import gms.shared.frameworks.osd.coi.signaldetection.SignalDetectionHypothesis;
import gms.shared.frameworks.osd.coi.signaldetection.SignalDetectionTestFixtures;
import gms.shared.frameworks.osd.coi.signaldetection.Station;
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
import gms.shared.frameworks.osd.coi.stationreference.RelativePosition;
import gms.shared.frameworks.osd.coi.stationreference.StationType;
import gms.shared.frameworks.osd.coi.test.utils.UtilsTestFixtures;
import gms.shared.frameworks.osd.control.station.StationRepositoryJpa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import static gms.shared.frameworks.osd.coi.signaldetection.SignalDetectionTestFixtures.AMPLITUDE_FEATURE_MEASUREMENT;
import static gms.shared.frameworks.osd.coi.signaldetection.SignalDetectionTestFixtures.ARRIVAL_TIME_FEATURE_MEASUREMENT;
import static gms.shared.frameworks.osd.coi.signaldetection.SignalDetectionTestFixtures.DESCRIPTOR;
import static gms.shared.frameworks.osd.coi.signaldetection.SignalDetectionTestFixtures.PHASE_FEATURE_MEASUREMENT;
import static gms.shared.frameworks.osd.coi.signaldetection.SignalDetectionTestFixtures.SIGNAL_DETECTION_HYPOTHESIS;
import static gms.shared.frameworks.osd.coi.signaldetection.SignalDetectionTestFixtures.SIGNAL_DETECTION_HYPOTHESIS_ID;
import static gms.shared.frameworks.osd.coi.signaldetection.SignalDetectionTestFixtures.SIGNAL_DETECTION;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class SignalDetectionRepositoryJpaTests {

  private EntityManagerFactory entityManagerFactory;
  private SignalDetectionRepositoryJpa signalDetectionRepository;

  @BeforeEach
  void setUp() {
    entityManagerFactory = CoiTestingEntityManagerFactory.createTesting();
    signalDetectionRepository = new SignalDetectionRepositoryJpa(entityManagerFactory);
    EntityManager entityManager = entityManagerFactory.createEntityManager();
    StationRepositoryInterface stationRepository = new StationRepositoryJpa(entityManagerFactory);
    stationRepository.storeStations(List.of(UtilsTestFixtures.STATION));
    entityManager.close();
  }

  @AfterEach
  void tearDown() {
    entityManagerFactory.close();
  }

  @Test
  void testStore() {
    assertDoesNotThrow(
        () -> signalDetectionRepository.storeSignalDetections(List.of(SIGNAL_DETECTION)));

    EntityManager entityManager = entityManagerFactory.createEntityManager();
    SignalDetectionDao signalDetectionDao = entityManager.find(SignalDetectionDao.class,
        SIGNAL_DETECTION.getId());
    assertNotNull(signalDetectionDao);

    assertEquals(SIGNAL_DETECTION.getMonitoringOrganization(),
        signalDetectionDao.getMonitoringOrganization());
    assertEquals(SIGNAL_DETECTION.getStationName(), signalDetectionDao.getStation().getName());

    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    CriteriaQuery<SignalDetectionHypothesisDao> signalDetectionHypothesisQuery =
        builder.createQuery(SignalDetectionHypothesisDao.class);
    Root<SignalDetectionHypothesisDao> fromSignalDetectionHypothesis =
        signalDetectionHypothesisQuery.from(SignalDetectionHypothesisDao.class);
    signalDetectionHypothesisQuery.select(fromSignalDetectionHypothesis);
    Expression<SignalDetectionDao> parentSignalDetection = fromSignalDetectionHypothesis.get(
        "parentSignalDetection");
    signalDetectionHypothesisQuery.where(builder.equal(parentSignalDetection, signalDetectionDao));

    List<SignalDetectionHypothesisDao> sdhDaos =
        entityManager.createQuery(signalDetectionHypothesisQuery)
            .getResultList();

    assertEquals(SIGNAL_DETECTION.getSignalDetectionHypotheses().size(), sdhDaos.size());
  }

  @Test
  void testUpdateNewHypothesis() {
    signalDetectionRepository.storeSignalDetections(List.of(SIGNAL_DETECTION));

    SIGNAL_DETECTION.addSignalDetectionHypothesis(
        SignalDetectionTestFixtures.SIGNAL_DETECTION_HYPOTHESIS_ID,
        List.of(ARRIVAL_TIME_FEATURE_MEASUREMENT, PHASE_FEATURE_MEASUREMENT,
            AMPLITUDE_FEATURE_MEASUREMENT));

    assertDoesNotThrow(
        () -> signalDetectionRepository.storeSignalDetections(List.of(SIGNAL_DETECTION)));

    EntityManager entityManager = entityManagerFactory.createEntityManager();

    SignalDetectionDao signalDetectionDao = entityManager.find(SignalDetectionDao.class,
        SIGNAL_DETECTION.getId());
    assertNotNull(signalDetectionDao);

    assertEquals(SIGNAL_DETECTION.getMonitoringOrganization(),
        signalDetectionDao.getMonitoringOrganization());
    assertEquals(SIGNAL_DETECTION.getStationName(), signalDetectionDao.getStation().getName());

    List<UUID> hypothesisIds = SIGNAL_DETECTION.getSignalDetectionHypotheses().stream()
        .map(SignalDetectionHypothesis::getId)
        .collect(Collectors.toList());

    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    CriteriaQuery<SignalDetectionHypothesisDao> hypothesisQuery =
        builder.createQuery(SignalDetectionHypothesisDao.class);
    Root<SignalDetectionHypothesisDao> fromSignalDetectionHypothesis =
        hypothesisQuery.from(SignalDetectionHypothesisDao.class);
    hypothesisQuery.select(fromSignalDetectionHypothesis);

    Path<UUID> id = fromSignalDetectionHypothesis.get("id");
    hypothesisQuery.where(id.in(hypothesisIds));

    Map<UUID, SignalDetectionHypothesisDao> hypothesesById =
        entityManager.createQuery(hypothesisQuery)
            .getResultStream()
            .collect(Collectors.toMap(SignalDetectionHypothesisDao::getId,
                Functions.identity()));

    assertEquals(SIGNAL_DETECTION.getSignalDetectionHypotheses().size(),
        hypothesesById.values().size());

    SIGNAL_DETECTION.getSignalDetectionHypotheses()
        .forEach(sdh -> compareHypothesisToDao(sdh, hypothesesById.get(sdh.getId())));

    entityManager.close();
  }

  @Test
  public void testFindSignalDetectionsById() {
    signalDetectionRepository.storeSignalDetections(List.of(SIGNAL_DETECTION));

    List<SignalDetection> actualSignalDetections = signalDetectionRepository
        .findSignalDetectionsByIds(List.of(SIGNAL_DETECTION.getId()));

    assertEquals(1, actualSignalDetections.size());
    SignalDetection expectedDetection = SIGNAL_DETECTION;
    SignalDetection actualDetection = actualSignalDetections.get(0);

    assertEquals(expectedDetection.getId(), actualDetection.getId());
    assertEquals(expectedDetection.getMonitoringOrganization(),
        actualDetection.getMonitoringOrganization());
    assertEquals(expectedDetection.getStationName(), actualDetection.getStationName());
    assertEquals(expectedDetection.getSignalDetectionHypotheses().size(),
        actualDetection.getSignalDetectionHypotheses().size());

    Map<UUID, SignalDetectionHypothesis> actualHypotheses =
        actualDetection.getSignalDetectionHypotheses()
            .stream()
            .collect(Collectors.toMap(SignalDetectionHypothesis::getId, Functions.identity()));

    for (SignalDetectionHypothesis expectedHypothesis :
        expectedDetection.getSignalDetectionHypotheses()) {
      SignalDetectionHypothesis actualHypothesis = actualHypotheses.get(expectedHypothesis.getId());
      assertNotNull(actualHypothesis);

      assertEquals(expectedHypothesis.getParentSignalDetectionId(),
          actualHypothesis.getParentSignalDetectionId());
      assertEquals(expectedHypothesis.getMonitoringOrganization(),
          actualHypothesis.getMonitoringOrganization());
      assertEquals(expectedHypothesis.getStationName(), actualHypothesis.getStationName());

      expectedHypothesis.getParentSignalDetectionHypothesisId().ifPresentOrElse(
          id -> actualHypothesis.getParentSignalDetectionHypothesisId()
              .ifPresentOrElse(actualId -> assertEquals(id, actualId), () -> fail()),
          () -> assertFalse(actualHypothesis.getParentSignalDetectionHypothesisId().isPresent()));

      assertEquals(expectedHypothesis.isRejected(), actualHypothesis.isRejected());

      for (FeatureMeasurement<?> featureMeasurement : actualHypothesis.getFeatureMeasurements()) {
        Optional<? extends FeatureMeasurement<?>> expectedFeatureMeasurement =
            expectedHypothesis.getFeatureMeasurement(featureMeasurement.getFeatureMeasurementType());

        assertTrue(expectedFeatureMeasurement.isPresent());
        compareFeatureMeasurements(expectedFeatureMeasurement.get(), featureMeasurement);
      }
    }
  }

  @Test
  void testFindSignalDetectionsByStationAndTimeValidation() {
    assertThrows(NullPointerException.class,
        () -> signalDetectionRepository.findSignalDetectionsByStationAndTime(null));
  }

  @Test
  void testFindSignalDetectionsByStationAndTime() {
    MeasuredChannelSegmentDescriptor outOfRangeDescriptor =
        MeasuredChannelSegmentDescriptor.builder()
            .setChannelName(UtilsTestFixtures.CHANNEL.getName())
            .setMeasuredChannelSegmentStartTime(Instant.EPOCH.plusSeconds(6000))
            .setMeasuredChannelSegmentEndTime(Instant.EPOCH.plusSeconds(9000))
            .setMeasuredChannelSegmentCreationTime(Instant.EPOCH.plusSeconds(10000))
            .build();

    FeatureMeasurement<InstantValue> outOfRangeArrival =
        FeatureMeasurement.from(UtilsTestFixtures.CHANNEL,
            outOfRangeDescriptor,
            FeatureMeasurementTypes.ARRIVAL_TIME,
            InstantValue.from(Instant.EPOCH.plusSeconds(7000), Duration.ofMillis(5)));

    FeatureMeasurement<EnumeratedMeasurementValue.PhaseTypeMeasurementValue> outOfRangePhase =
        FeatureMeasurement.from(UtilsTestFixtures.CHANNEL,
            outOfRangeDescriptor,
            FeatureMeasurementTypes.PHASE,
            EnumeratedMeasurementValue.PhaseTypeMeasurementValue.from(PhaseType.P, 0.5));

    SignalDetection outOfRangeDetection = SignalDetection.create("test",
        UtilsTestFixtures.STATION.getName(),
        List.of(outOfRangeArrival, outOfRangePhase));

    Channel otherChannel = Channel.from("Other Channel", "Other Channel Canonical Name",
        "Other description",
        "Other station",
        ChannelDataType.DIAGNOSTIC_SOH,
        ChannelBandType.BROADBAND,
        ChannelInstrumentType.HIGH_GAIN_SEISMOMETER,
        ChannelOrientationType.EAST_WEST,
        'E',
        Units.COUNTS_PER_NANOMETER,
        40.0,
        Location.from(25.0,
            -100.0,
            90.0,
            5000.0),
        Orientation.from(60.0,
            135.0),
        List.of(),
        Map.of(),
        Map.of(ChannelProcessingMetadataType.CHANNEL_GROUP, "OTHER GROUP"));

    Station otherStation = Station.from("Other station",
        StationType.HYDROACOUSTIC,
        "Other test station",
        Map.of(otherChannel.getName(), RelativePosition.from(20.0, 10.0, 5.0)),
        Location.from(123.4, 87, 32, 10),
        List.of(ChannelGroup.from("Other test group",
            "Another test",
            Location.from(110.0, 90.0, 30, 11),
            ChannelGroup.Type.PROCESSING_GROUP,
            List.of(otherChannel))),
        List.of(otherChannel));

    MeasuredChannelSegmentDescriptor otherDescriptor = MeasuredChannelSegmentDescriptor.builder()
        .setChannelName(otherChannel.getName())
        .setMeasuredChannelSegmentStartTime(Instant.EPOCH)
        .setMeasuredChannelSegmentEndTime(Instant.EPOCH.plusSeconds(60))
        .setMeasuredChannelSegmentCreationTime(Instant.EPOCH.plusSeconds(90))
        .build();

    FeatureMeasurement<InstantValue> otherArrival = FeatureMeasurement.from(otherChannel,
        otherDescriptor,
        FeatureMeasurementTypes.ARRIVAL_TIME,
        InstantValue.from(Instant.EPOCH.plusSeconds(50), Duration.ofMillis(10)));

    FeatureMeasurement<EnumeratedMeasurementValue.PhaseTypeMeasurementValue> otherPhase =
        FeatureMeasurement.from(otherChannel,
            otherDescriptor,
            FeatureMeasurementTypes.PHASE,
            EnumeratedMeasurementValue.PhaseTypeMeasurementValue.from(PhaseType.P, 0.5));

    SignalDetection otherDetection = SignalDetection.create("Test org",
        otherStation.getName(),
        List.of(otherArrival, otherPhase));

    StationRepositoryInterface stationRepository = new StationRepositoryJpa(entityManagerFactory);
    stationRepository.storeStations(List.of(otherStation));
    signalDetectionRepository.storeSignalDetections(List.of(SIGNAL_DETECTION,
        outOfRangeDetection,
        otherDetection));

    List<SignalDetection> foundDetections = signalDetectionRepository
        .findSignalDetectionsByStationAndTime(StationsTimeRangeRequest.create(List.of(UtilsTestFixtures.STATION.getName()),
            Instant.EPOCH, Instant.EPOCH.plusSeconds(4000)));

    assertEquals(1, foundDetections.size());
    assertEquals(SIGNAL_DETECTION, foundDetections.get(0));
  }

  @Test
  void testStoreSignalDetectionHypothesesValidation() {
    assertThrows(NullPointerException.class,
        () -> signalDetectionRepository.storeSignalDetectionHypotheses(null));
  }

  @Test
  void testStoreSignalDetectionHypotheses() {
    signalDetectionRepository.storeSignalDetections(List.of(SIGNAL_DETECTION));

    SignalDetectionHypothesis updated = SIGNAL_DETECTION_HYPOTHESIS.toBuilder()
        .generateId()
        .setParentSignalDetectionHypothesisId(Optional.of(SIGNAL_DETECTION_HYPOTHESIS_ID))
        .addMeasurement(FeatureMeasurement.from(UtilsTestFixtures.CHANNEL,
            DESCRIPTOR,
            FeatureMeasurementTypes.SLOWNESS,
            NumericMeasurementValue.from(Instant.EPOCH.plusSeconds(2),
                DoubleValue.from(3.0, 1.0, Units.SECONDS_PER_DEGREE))))
        .build();

    assertDoesNotThrow(() -> signalDetectionRepository.storeSignalDetectionHypotheses(List.of(updated)));

    EntityManager entityManager = entityManagerFactory.createEntityManager();
    try {
      SignalDetectionHypothesisDao sdhDao = entityManager.find(SignalDetectionHypothesisDao.class,
          updated.getId());

      compareHypothesisToDao(updated, sdhDao);
    } finally {
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

  private void compareFeatureMeasurements(FeatureMeasurement<?> expected,
      FeatureMeasurement<?> actual) {
    Class<?> type = expected.getFeatureMeasurementType().getMeasurementValueType();
    assertEquals(type, actual.getFeatureMeasurementType().getMeasurementValueType());
    compareChannels(expected.getChannel(), actual.getChannel());
    assertEquals(expected.getMeasuredChannelSegmentDescriptor(),
        actual.getMeasuredChannelSegmentDescriptor());
    Object actualValue = actual.getMeasurementValue();
    Object expectedValue = expected.getMeasurementValue();
    assertEquals(actualValue, expectedValue);
  }

  private void compareChannels(Channel expected, Channel actual) {
    assertEquals(expected.getName(), actual.getName());
    assertEquals(expected.getCanonicalName(), actual.getCanonicalName());
    assertEquals(expected.getDescription(), actual.getDescription());
    assertEquals(expected.getStation(), actual.getStation());
    assertEquals(expected.getChannelDataType(), actual.getChannelDataType());
    assertEquals(expected.getChannelBandType(), actual.getChannelBandType());
    assertEquals(expected.getChannelInstrumentType(), actual.getChannelInstrumentType());
    assertEquals(expected.getChannelOrientationType(), actual.getChannelOrientationType());
    assertEquals(expected.getChannelOrientationCode(), actual.getChannelOrientationCode());
    assertEquals(expected.getUnits(), actual.getUnits());
    assertEquals(expected.getNominalSampleRateHz(), actual.getNominalSampleRateHz());
    assertEquals(expected.getLocation(), actual.getLocation());
    assertEquals(expected.getOrientationAngles(), actual.getOrientationAngles());
    assertEquals(expected.getConfiguredInputs(), actual.getConfiguredInputs());
    assertEquals(expected.getProcessingDefinition(), actual.getProcessingDefinition());
    assertEquals(expected.getProcessingMetadata(), actual.getProcessingMetadata());
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

