package gms.shared.frameworks.osd.control.event;

import com.google.common.base.Functions;
import gms.shared.frameworks.osd.api.event.util.FindEventByTimeAndLocationRequest;
import gms.shared.frameworks.osd.api.signaldetection.SignalDetectionRepositoryInterface;
import gms.shared.frameworks.osd.api.station.StationRepositoryInterface;
import gms.shared.frameworks.osd.coi.CoiTestingEntityManagerFactory;
import gms.shared.frameworks.osd.coi.event.Event;
import gms.shared.frameworks.osd.coi.event.EventHypothesis;
import gms.shared.frameworks.osd.coi.event.FeaturePrediction;
import gms.shared.frameworks.osd.coi.event.PreferredLocationSolution;
import gms.shared.frameworks.osd.coi.event.repository.jpa.EventDao;
import gms.shared.frameworks.osd.coi.event.repository.jpa.EventHypothesisAssociationDao;
import gms.shared.frameworks.osd.control.event.utils.EventDaoUtility;
import gms.shared.frameworks.osd.control.event.utils.FeaturePredictionDaoUtility;
import gms.shared.frameworks.osd.coi.signaldetection.FeatureMeasurement;
import gms.shared.frameworks.osd.coi.test.utils.UtilsTestFixtures;
import gms.shared.frameworks.osd.control.signaldetection.SignalDetectionRepositoryJpa;
import gms.shared.frameworks.osd.control.station.StationRepositoryJpa;
import gms.shared.frameworks.osd.control.utils.FeatureMeasurementDaoUtility;
import gms.shared.frameworks.osd.control.utils.TestFixtures;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class EventRepositoryJpaTest {

  private static EntityManagerFactory entityManagerFactory;

  @BeforeAll
  static void setUp() {
    entityManagerFactory = CoiTestingEntityManagerFactory.createTesting();
  }

  @BeforeEach
  public void setUpIndividual() {
    StationRepositoryInterface stationRepository = new StationRepositoryJpa(entityManagerFactory);
    stationRepository.storeStations(List.of(UtilsTestFixtures.STATION));
    SignalDetectionRepositoryInterface signalDetectionRepository =
        new SignalDetectionRepositoryJpa(entityManagerFactory);
    signalDetectionRepository.storeSignalDetections(List.of(TestFixtures.SIGNAL_DETECTION));
  }

  @AfterEach
  void tearDown() {
    EntityManager entityManager = entityManagerFactory.createEntityManager();
    entityManager.createQuery("FROM EventHypothesisAssociationDao")
        .getResultStream()
        .forEach(entityManager::remove);
    entityManager.close();
  }

  @AfterAll
  static void destroy() {
    entityManagerFactory.close();
  }

  @Test
  void testStoreEventValidation() {
    EventRepositoryJpa eventRepository = new EventRepositoryJpa(entityManagerFactory);
    assertThrows(NullPointerException.class, () -> eventRepository.storeEvents(null));
  }

  @Test
  void testStoreEventsNew() {
    EventRepositoryJpa eventRepository = new EventRepositoryJpa(entityManagerFactory);
    EntityManager entityManager = entityManagerFactory.createEntityManager();
    try {
      assertDoesNotThrow(() -> eventRepository.storeEvents(List.of(TestFixtures.EVENT)));
      validateEvent(TestFixtures.EVENT, entityManager);
    } finally {
      entityManager.close();
    }
  }

  @Test
  void testStoreEventsUpdate() {
    EventRepositoryJpa eventRepository = new EventRepositoryJpa(entityManagerFactory);
    EntityManager entityManager = entityManagerFactory.createEntityManager();
    try {
      Event baseEvent = TestFixtures.UNSTORED_EVENT;
      EventHypothesis baseHypothesis = baseEvent.getOverallPreferred();
      eventRepository.storeEvents(List.of(baseEvent));

      baseEvent.addEventHypothesis(baseHypothesis,
          Set.of(TestFixtures.SIGNAL_DETECTION_HYPOTHESIS.getId()),
          Set.of(TestFixtures.LOCATION_SOLUTION_7),
          PreferredLocationSolution.from(TestFixtures.LOCATION_SOLUTION_7));

      assertDoesNotThrow(() -> eventRepository.storeEvents(List.of(baseEvent)));
      validateEvent(baseEvent, entityManager);
    } finally {
      entityManager.close();
    }
  }

  @ParameterizedTest
  @MethodSource("eventsByIdValidationInput")
  void testFindEventsByIdsValidation(Class<? extends Exception> exceptionClass,
      Collection<UUID> input) {
    EventRepositoryJpa eventRepository = new EventRepositoryJpa(entityManagerFactory);
    assertThrows(exceptionClass, () -> eventRepository.findEventsByIds(input));
  }

  static Stream<Arguments> eventsByIdValidationInput() {
    return Stream.of(
        arguments(NullPointerException.class, null),
        arguments(IllegalStateException.class, List.of()));
  }

  @Test
  void findEventsByIds() {
    EventRepositoryJpa eventRepository = new EventRepositoryJpa(entityManagerFactory);
    eventRepository.storeEvents(List.of(TestFixtures.EVENT, TestFixtures.EVENT_2));
    Map<UUID, Event> actualEventsById =
        eventRepository.findEventsByIds(List.of(TestFixtures.EVENT.getId(),
            TestFixtures.EVENT_2.getId()))
            .stream()
            .collect(Collectors.toMap(Event::getId, Functions.identity()));

    assertEquals(TestFixtures.EVENT, actualEventsById.get(TestFixtures.EVENT.getId()));
    assertEquals(TestFixtures.EVENT_2, actualEventsById.get(TestFixtures.EVENT_2.getId()));
  }

  @Test
  void testFindEventsByTimeAndLocationValidation() {
    EventRepositoryJpa eventRepository = new EventRepositoryJpa(entityManagerFactory);
    assertThrows(NullPointerException.class,
        () -> eventRepository.findEventsByTimeAndLocation(null));
  }

  @Test
  void findEventsByTimeAndLocation() {
    EventRepositoryJpa eventRepository = new EventRepositoryJpa(entityManagerFactory);
    eventRepository.storeEvents(List.of(TestFixtures.EVENT, TestFixtures.EVENT_2));
    double minLatitude = Math.min(TestFixtures.LOCATION.getLatitudeDegrees(),
        TestFixtures.LOCATION_2.getLatitudeDegrees()) - 1;
    double maxLatitude = Math.max(TestFixtures.LOCATION.getLatitudeDegrees(),
        TestFixtures.LOCATION_2.getLatitudeDegrees()) + 1;
    double minLongitude = Math.min(TestFixtures.LOCATION.getLongitudeDegrees(),
        TestFixtures.LOCATION_2.getLongitudeDegrees()) - 1;
    double maxLongitude = Math.max(TestFixtures.LOCATION.getLongitudeDegrees(),
        TestFixtures.LOCATION_2.getLongitudeDegrees()) + 1;
    Instant startTime = Stream.of(TestFixtures.LOCATION.getTime(),
        TestFixtures.LOCATION_2.getTime())
        .min(Instant::compareTo)
        .orElseThrow(IllegalStateException::new);
    Instant endTime = Stream.of(TestFixtures.LOCATION.getTime(), TestFixtures.LOCATION_2.getTime())
        .max(Instant::compareTo)
        .orElseThrow(IllegalStateException::new);

    FindEventByTimeAndLocationRequest request = FindEventByTimeAndLocationRequest
        .from(startTime.minusSeconds(1),
            endTime.plusSeconds(1),
            minLatitude,
            maxLatitude,
            minLongitude,
            maxLongitude);

    Map<UUID, Event> actualEventsById = eventRepository.findEventsByTimeAndLocation(request)
        .stream()
        .collect(Collectors.toMap(Event::getId, Functions.identity()));

    assertEquals(2, actualEventsById.values().size());
    assertEquals(TestFixtures.EVENT, actualEventsById.get(TestFixtures.EVENT.getId()));
    assertEquals(TestFixtures.EVENT_2, actualEventsById.get(TestFixtures.EVENT_2.getId()));
  }

  @Test
  void findEventsByTimeAndLocationOutOfRange() {
    EventRepositoryJpa eventRepository = new EventRepositoryJpa(entityManagerFactory);
    eventRepository.storeEvents(List.of(TestFixtures.EVENT, TestFixtures.EVENT_2));
    double minLatitude = Math.min(TestFixtures.LOCATION.getLatitudeDegrees(),
        TestFixtures.LOCATION_2.getLatitudeDegrees()) + 1;
    double maxLatitude = Math.max(TestFixtures.LOCATION.getLatitudeDegrees(),
        TestFixtures.LOCATION_2.getLatitudeDegrees()) - 1;
    double minLongitude = Math.min(TestFixtures.LOCATION.getLongitudeDegrees(),
        TestFixtures.LOCATION_2.getLongitudeDegrees()) + 1;
    double maxLongitude = Math.max(TestFixtures.LOCATION.getLongitudeDegrees(),
        TestFixtures.LOCATION_2.getLongitudeDegrees()) - 1;
    Instant startTime = Stream.of(TestFixtures.LOCATION.getTime(),
        TestFixtures.LOCATION_2.getTime())
        .min(Instant::compareTo)
        .orElseThrow(IllegalStateException::new);
    Instant endTime = Stream.of(TestFixtures.LOCATION.getTime(), TestFixtures.LOCATION_2.getTime())
        .max(Instant::compareTo)
        .orElseThrow(IllegalStateException::new);

    FindEventByTimeAndLocationRequest request = FindEventByTimeAndLocationRequest
        .from(startTime.minusSeconds(1),
            endTime.plusSeconds(1),
            minLatitude,
            maxLatitude,
            minLongitude,
            maxLongitude);

    Map<UUID, Event> actualEventsById = eventRepository.findEventsByTimeAndLocation(request)
        .stream()
        .collect(Collectors.toMap(Event::getId, Functions.identity()));

    assertEquals(0, actualEventsById.values().size());
  }

  private void validateEvent(Event expected, EntityManager entityManager) {
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    CriteriaQuery<EventHypothesisAssociationDao> associationQuery =
        builder.createQuery(EventHypothesisAssociationDao.class);
    Root<EventHypothesisAssociationDao> fromAssociation =
        associationQuery.from(EventHypothesisAssociationDao.class);
    associationQuery.select(fromAssociation)
        .where(builder.equal(fromAssociation.join("event").get("id"),
            expected.getId()));

    List<EventHypothesisAssociationDao> associationDaos =
        entityManager.createQuery(associationQuery)
            .getResultList();

    assertEquals(expected.getHypotheses().size(), associationDaos.size());

    EventDao eventDao = entityManager.find(EventDao.class, expected.getId());

    assertNotNull(eventDao);

    eventDao.setHypotheses(associationDaos.stream()
        .map(EventHypothesisAssociationDao::getHypothesis)
        .collect(Collectors.toSet()));

    Map<String, FeatureMeasurement<?>> featureMeasurementDaosById =
        EventDaoUtility.getFeatureMeasurementDaos(eventDao)
            .map(featureMeasurement -> FeatureMeasurementDaoUtility.toCoi(featureMeasurement,
                UtilsTestFixtures.CHANNEL))
            .distinct()
            .collect(Collectors.toMap(FeatureMeasurementDaoUtility::buildId,
                Functions.identity()));

    Map<UUID, FeaturePrediction<?>> featurePredictionDaosById =
        EventDaoUtility.getFeaturePredictionDaos(eventDao)
            .map(FeaturePredictionDaoUtility::toCoi)
            .distinct()
            .collect(Collectors.toMap(FeaturePredictionDaoUtility::buildId,
                Functions.identity()));

    Event actualEvent = EventDaoUtility.toCoi(eventDao, featurePredictionDaosById,
        featureMeasurementDaosById);

    assertEquals(expected, actualEvent);
  }
}