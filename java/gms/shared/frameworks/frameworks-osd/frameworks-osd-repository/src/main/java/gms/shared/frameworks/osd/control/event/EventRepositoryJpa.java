package gms.shared.frameworks.osd.control.event;

import com.google.common.base.Functions;
import com.google.common.base.Preconditions;
import gms.shared.frameworks.osd.api.channel.ChannelRepositoryInterface;
import gms.shared.frameworks.osd.api.event.EventRepositoryInterface;
import gms.shared.frameworks.osd.api.event.util.FindEventByTimeAndLocationRequest;
import gms.shared.frameworks.osd.coi.channel.Channel;
import gms.shared.frameworks.osd.coi.channel.repository.jpa.dataaccessobjects.LocationDao;
import gms.shared.frameworks.osd.coi.event.Event;
import gms.shared.frameworks.osd.coi.event.FeaturePrediction;
import gms.shared.frameworks.osd.coi.event.repository.jpa.EventDao;
import gms.shared.frameworks.osd.coi.event.repository.jpa.EventHypothesisAssociationDao;
import gms.shared.frameworks.osd.coi.event.repository.jpa.EventHypothesisDao;
import gms.shared.frameworks.osd.coi.event.repository.jpa.FeaturePredictionDao;
import gms.shared.frameworks.osd.coi.event.repository.jpa.LocationSolutionDao;
import gms.shared.frameworks.osd.coi.event.repository.jpa.PreferredEventHypothesisDao;
import gms.shared.frameworks.osd.coi.event.repository.jpa.PreferredLocationSolutionDao;
import gms.shared.frameworks.osd.control.event.utils.EventDaoUtility;
import gms.shared.frameworks.osd.control.event.utils.EventHypothesisDaoUtility;
import gms.shared.frameworks.osd.control.event.utils.FeaturePredictionDaoUtility;
import gms.shared.frameworks.osd.coi.signaldetection.FeatureMeasurement;
import gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.dataaccessobjects.FeatureMeasurementDao;
import gms.shared.frameworks.osd.control.channel.ChannelRepositoryJpa;
import gms.shared.frameworks.osd.control.utils.FeatureMeasurementDaoUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.RollbackException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class EventRepositoryJpa implements EventRepositoryInterface {

  private static final Logger logger = LoggerFactory.getLogger(EventRepositoryJpa.class);

  private static final double DEFAULT_MIN_LATITUDE_DEGREES = -90;
  private static final double DEFAULT_MAX_LATITUDE_DEGREES = 90;
  private static final double DEFAULT_MIN_LONGITUDE_DEGREES = -180;
  private static final double DEFAULT_MAX_LONGITUDE_DEGREES = 180;

  private final EntityManagerFactory entityManagerFactory;
  private final ChannelRepositoryInterface channelRepository;

  public EventRepositoryJpa(EntityManagerFactory entityManagerFactory) {
    this.entityManagerFactory = entityManagerFactory;
    this.channelRepository = new ChannelRepositoryJpa(entityManagerFactory);
  }

  @Override
  public void storeEvents(Collection<Event> events) {
    Objects.requireNonNull(events);

    EntityManager entityManager = entityManagerFactory.createEntityManager();

    try {
      entityManager.getTransaction().begin();

      for (Event event : events) {
        final EventDao eventDao = entityManager.find(EventDao.class, event.getId());
        if (eventDao == null) {
          // new event, persist as is (merging to handle any duplicate feature measurements)
          EventDao newEventDao = EventDaoUtility.fromCoi(event, entityManager);

          newEventDao.getHypotheses().stream()
              .map(hypothesisDao -> {
                EventHypothesisAssociationDao dao = new EventHypothesisAssociationDao();
                dao.setId(UUID.randomUUID());
                dao.setEvent(newEventDao);
                dao.setHypothesis(hypothesisDao);
                return dao;
              })
              .forEach(entityManager::merge);
        } else {
          // event update

          CriteriaBuilder builder = entityManager.getCriteriaBuilder();
          CriteriaQuery<EventHypothesisDao> hypothesisQuery =
              builder.createQuery(EventHypothesisDao.class);
          Root<EventHypothesisAssociationDao> fromAssociation =
              hypothesisQuery.from(EventHypothesisAssociationDao.class);
          hypothesisQuery.select(fromAssociation.get("hypothesis"))
              .where(builder.equal(fromAssociation.join("event").get("id"), event.getId()));

          Map<UUID, EventHypothesisDao> hypothesesById = entityManager.createQuery(hypothesisQuery)
              .getResultStream()
              .collect(Collectors.toMap(EventHypothesisDao::getId, Functions.identity()));

          // Find the new event hypotheses and persist them
          Map<UUID, EventHypothesisDao> newHypotheses = event.getHypotheses().stream()
              .filter(hypothesis -> !hypothesesById.containsKey(hypothesis.getId()))
              .map(hypothesis -> EventHypothesisDaoUtility.fromCoi(hypothesis, entityManager))
              .collect(Collectors.toMap(EventHypothesisDao::getId, Functions.identity()));

          newHypotheses.values().stream()
              .forEach(hypothesis -> {
                EventHypothesisAssociationDao associationDao = new EventHypothesisAssociationDao();
                associationDao.setId(UUID.randomUUID());
                associationDao.setEvent(eventDao);
                associationDao.setHypothesis(hypothesis);
                entityManager.merge(associationDao);
              });

          // merge the two sets for simplicity
          hypothesesById.putAll(newHypotheses);

          Map<UUID, PreferredEventHypothesisDao> preferredEventHypothesesById = eventDao
              .getPreferredEventHypothesisHistory()
              .stream()
              .collect(Collectors.toMap(hypothesis -> hypothesis.getEventHypothesis().getId(),
                  Functions.identity()));

          // Find and add the new preferred event hypotheses
          event.getPreferredEventHypothesisHistory().stream()
              .filter(hypothesis -> !preferredEventHypothesesById.containsKey(hypothesis.getEventHypothesis().getId()))
              .forEach(preferredEventHypothesis -> {
                EventHypothesisDao hypothesis =
                    hypothesesById.get(preferredEventHypothesis.getEventHypothesis().getId());
                PreferredEventHypothesisDao preferredEventHypothesisDao =
                    new PreferredEventHypothesisDao(hypothesis,
                        preferredEventHypothesis.getProcessingStageId());
                entityManager.merge(preferredEventHypothesisDao);
              });

          // Find and add any new final event hypotheses
          Map<UUID, EventHypothesisDao> finalHypothesesById =
              eventDao.getFinalEventHypothesisHistory()
                  .stream()
                  .collect(Collectors.toMap(EventHypothesisDao::getId, Functions.identity()));

          event.getFinalEventHypothesisHistory().stream()
              .filter(finalHypothesis -> !finalHypothesesById.containsKey(finalHypothesis.getEventHypothesis().getId()))
              .forEach(finalHypothesis -> {
                EventHypothesisDao finalHypothesisDao =
                    hypothesesById.get(finalHypothesis.getEventHypothesis().getId());
                eventDao.getFinalEventHypothesisHistory().add(finalHypothesisDao);
              });
        }
      }

      entityManager.getTransaction().commit();
    } catch (RollbackException ex) {
      logger.error("Error committing transaction while storing events", ex);
      entityManager.getTransaction().rollback();
      throw new RuntimeException(ex);
    } finally {
      entityManager.close();
    }
  }

  @Override
  public Collection<Event> findEventsByIds(Collection<UUID> eventIds) {
    Objects.requireNonNull(eventIds);
    Preconditions.checkState(!eventIds.isEmpty());

    EntityManager entityManager = entityManagerFactory.createEntityManager();
    try {
      CriteriaBuilder builder = entityManager.getCriteriaBuilder();
      CriteriaQuery<EventHypothesisAssociationDao> associationQuery =
          builder.createQuery(EventHypothesisAssociationDao.class);
      Root<EventHypothesisAssociationDao> fromAssociation =
          associationQuery.from(EventHypothesisAssociationDao.class);
      associationQuery.select(fromAssociation)
          .where(fromAssociation.join("event").get("id").in(eventIds));

      return buildEvents(entityManager.createQuery(associationQuery).getResultList());
    } finally {
      entityManager.close();
    }
  }

  @Override
  public Collection<Event> findEventsByTimeAndLocation(FindEventByTimeAndLocationRequest request) {
    Objects.requireNonNull(request);

    EntityManager entityManager = entityManagerFactory.createEntityManager();

    try {
      CriteriaBuilder builder = entityManager.getCriteriaBuilder();
      CriteriaQuery<EventHypothesisAssociationDao> associationQuery =
          builder.createQuery(EventHypothesisAssociationDao.class);
      Root<EventHypothesisAssociationDao> fromAssociation =
          associationQuery.from(EventHypothesisAssociationDao.class);
      associationQuery.select(fromAssociation);
      Join<EventHypothesisAssociationDao, EventHypothesisDao> hypothesisJoin =
          fromAssociation.join("hypothesis");
      Join<EventHypothesisDao, PreferredLocationSolutionDao> preferredLocationSolutionDaoJoin =
          hypothesisJoin.join("preferredLocationSolution");
      Join<PreferredLocationSolutionDao, LocationSolutionDao> locationSolutionJoin =
          preferredLocationSolutionDaoJoin.join("locationSolution");
      Join<LocationSolutionDao, LocationDao> locationJoin = locationSolutionJoin.join("location");

      Path<Instant> locationTime = locationJoin.get("time");
      Path<Double> locationLatitudeDegrees = locationJoin.get("latitudeDegrees");
      Path<Double> locationLongitudeDegrees = locationJoin.get("longitudeDegrees");

      Predicate[] conjunctions = new Predicate[6];
      conjunctions[0] = builder.greaterThanOrEqualTo(locationTime, request.getStartTime());
      conjunctions[1] = builder.lessThanOrEqualTo(locationTime, request.getEndTime());
      conjunctions[2] = builder.greaterThanOrEqualTo(locationLatitudeDegrees,
          request.getMinimumLatitude().orElse(DEFAULT_MIN_LATITUDE_DEGREES));
      conjunctions[3] = builder.lessThanOrEqualTo(locationLatitudeDegrees,
          request.getMaximumLatitude().orElse(DEFAULT_MAX_LATITUDE_DEGREES));
      conjunctions[4] = builder.greaterThanOrEqualTo(locationLongitudeDegrees,
          request.getMinimumLongitude().orElse(DEFAULT_MIN_LONGITUDE_DEGREES));
      conjunctions[5] = builder.lessThanOrEqualTo(locationLongitudeDegrees,
          request.getMaximumLongitude().orElse(DEFAULT_MAX_LONGITUDE_DEGREES));
      associationQuery.where(builder.and(conjunctions));

      return buildEvents(entityManager.createQuery(associationQuery).getResultList());
    } finally {
      entityManager.close();
    }
  }

  private List<Event> buildEvents(List<EventHypothesisAssociationDao> associationDaos) {
    List<EventDao> eventDaos = associationDaos.stream()
        .collect(Collectors.groupingBy(EventHypothesisAssociationDao::getEvent,
            Collectors.mapping(EventHypothesisAssociationDao::getHypothesis,
                Collectors.toSet())))
        .entrySet()
        .stream()
        .map(entry -> {
          Set<EventHypothesisDao> hypothesisDaos = entry.getValue();
          EventDao eventDao = entry.getKey();
          eventDao.setHypotheses(hypothesisDaos);
          return eventDao;
        })
        .collect(Collectors.toList());

    Map<String, List<FeatureMeasurementDao<?>>> featureMeasurementsByChannelName =
        eventDaos.stream()
            .flatMap(EventDaoUtility::getFeatureMeasurementDaos)
            .collect(Collectors.groupingBy(featureMeasurementDao ->
                featureMeasurementDao.getMeasuredChannelSegmentDescriptor().getChannel().getName()));

    Map<String, Channel> channelsByName =
        channelRepository.retrieveChannels(featureMeasurementsByChannelName.keySet())
            .stream()
            .collect(Collectors.toMap(Channel::getName, Functions.identity()));

    Map<String, FeatureMeasurement<?>> featureMeasurementsById = featureMeasurementsByChannelName
        .entrySet()
        .stream()
        .flatMap(entry -> entry.getValue()
            .stream()
            .map(featureMeasurement -> FeatureMeasurementDaoUtility.toCoi(featureMeasurement,
                channelsByName.get(entry.getKey()))))
        .distinct()
        .collect(Collectors.toMap(FeatureMeasurementDaoUtility::buildId,
            Functions.identity()));

    Map<UUID, FeaturePrediction<?>> featurePredictionsById = eventDaos.stream()
        .flatMap(EventDaoUtility::getFeaturePredictionDaos)
        .distinct()
        .collect(Collectors.toMap(FeaturePredictionDao::getId,
            FeaturePredictionDaoUtility::toCoi));

    return eventDaos.stream()
        .map(eventDao -> EventDaoUtility.toCoi(eventDao, featurePredictionsById,
            featureMeasurementsById))
        .collect(Collectors.toList());
  }
}
