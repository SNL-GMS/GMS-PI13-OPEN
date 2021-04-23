package gms.shared.frameworks.osd.control.event.utils;

import com.google.common.base.Preconditions;
import gms.shared.frameworks.osd.coi.event.Event;
import gms.shared.frameworks.osd.coi.event.FeaturePrediction;
import gms.shared.frameworks.osd.coi.event.FinalEventHypothesis;
import gms.shared.frameworks.osd.coi.event.PreferredEventHypothesis;
import gms.shared.frameworks.osd.coi.event.repository.jpa.EventDao;
import gms.shared.frameworks.osd.coi.event.repository.jpa.FeaturePredictionDao;
import gms.shared.frameworks.osd.coi.signaldetection.FeatureMeasurement;
import gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.dataaccessobjects.FeatureMeasurementDao;
import javax.persistence.EntityManager;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utility class to handle converting Events between their COI and DAO Representations
 */
public class EventDaoUtility {

  /**
   * Converts an Event to an EventDao, handling reuse of value objects and storage of subordinate
   * objects that are reusable.  This does not persist the event.
   *
   * @param event The {@link Event} to convert
   * @param entityManager The {@link EntityManager}, with an active transaction, that will be used
   * to find and persist value objects in the {@link Event} hierarchy
   * @return the converted {@link EventDao}
   */
  public static EventDao fromCoi(Event event, EntityManager entityManager) {

    Objects.requireNonNull(event);
    Objects.requireNonNull(entityManager);
    Preconditions.checkState(entityManager.getTransaction().isActive());

    EventDao eventDao = new EventDao();

    eventDao.setId(event.getId());
    eventDao.setRejectedSignalDetectionAssociations(event.getRejectedSignalDetectionAssociations());
    eventDao.setMonitoringOrganization(event.getMonitoringOrganization());
    eventDao.setHypotheses(event.getHypotheses().stream()
        .map(hypothesis -> EventHypothesisDaoUtility.fromCoi(hypothesis, entityManager))
        .collect(Collectors.toSet()));

    eventDao.setFinalEventHypothesisHistory(event.getFinalEventHypothesisHistory().stream()
        .map(FinalEventHypothesis::getEventHypothesis)
        .map(hypothesis -> EventHypothesisDaoUtility.fromCoi(hypothesis, entityManager))
        .collect(Collectors.toList()));

    eventDao.setPreferredEventHypothesisHistory(event.getPreferredEventHypothesisHistory().stream()
        .map(preferredEventHypothesis -> PreferredEventHypothesisDaoUtility
            .fromCoi(preferredEventHypothesis, entityManager))
        .collect(Collectors.toList()));

    return eventDao;
  }

  /**
   * Converts an {@link EventDao} to it's COI representation
   *
   * @param eventDao the {@link EventDao} to convert
   * @param featurePredictionsById A map of {@link FeaturePrediction} by their IDs, representing the
   * {@link FeaturePredictionDao} used in the {@link EventDao}
   * @param featureMeasurementsById A map of {@link FeatureMeasurement} by their IDs, representing
   * the {@link FeatureMeasurementDao} used in the {@link EventDao}
   * @return the converted {@link Event}
   */
  public static Event toCoi(EventDao eventDao,
      Map<UUID, FeaturePrediction<?>> featurePredictionsById,
      Map<String, FeatureMeasurement<?>> featureMeasurementsById) {

    Objects.requireNonNull(eventDao);
    Objects.requireNonNull(featurePredictionsById);
    Objects.requireNonNull(featureMeasurementsById);

    return Event.from(eventDao.getId(),
        eventDao.getRejectedSignalDetectionAssociations(),
        eventDao.getMonitoringOrganization(),
        eventDao.getHypotheses().stream()
            .map(hypothesisDao -> EventHypothesisDaoUtility.toCoi(hypothesisDao,
                featurePredictionsById, featureMeasurementsById))
            .collect(Collectors.toSet()),
        eventDao.getFinalEventHypothesisHistory().stream()
            .map(hypothesisDao -> EventHypothesisDaoUtility.toCoi(hypothesisDao,
                featurePredictionsById, featureMeasurementsById))
            .map(FinalEventHypothesis::from)
            .collect(Collectors.toList()),
        eventDao.getPreferredEventHypothesisHistory().stream()
            .map(preferredHypothesisDao -> PreferredEventHypothesis.from(preferredHypothesisDao.getProcessingStageId(),
                EventHypothesisDaoUtility.toCoi(preferredHypothesisDao.getEventHypothesis(),
                    featurePredictionsById, featureMeasurementsById)))
            .collect(Collectors.toList()));
  }

  /**
   * Gets all {@link FeaturePrediction}s found in the provided {@link Event}
   * @param event the {@link Event} holding the {@link FeaturePrediction}s to collect
   * @return A {@link Stream} of {@link FeaturePrediction}s in the {@link Event}
   */
  public static Stream<FeaturePrediction<?>> getFeaturePredictions(Event event) {
    return event.getHypotheses().stream()
        .flatMap(EventHypothesisDaoUtility::getFeaturePredictions);
  }

  /**
   * Gets all {@link FeaturePredictionDao}s found in the provided {@link EventDao}
   * @param eventDao the {@link EventDao} holding the {@link FeaturePredictionDao}s to collect
   * @return A {@link Stream} of {@link FeaturePredictionDao}s in the {@link EventDao}
   */
  public static Stream<FeaturePredictionDao<?>> getFeaturePredictionDaos(EventDao eventDao) {
    return eventDao.getHypotheses().stream()
        .flatMap(EventHypothesisDaoUtility::getFeaturePredictionDaos);
  }

  /**
   * Gets all {@link FeatureMeasurement}s found in the provided {@link Event}
   * @param event the {@link Event} holding the {@link FeatureMeasurement}s to collect
   * @return A {@link Stream} of {@link FeatureMeasurement}s in the {@link Event}
   */
  public static Stream<FeatureMeasurement<?>> getFeatureMeasurements(Event event) {
    return event.getHypotheses().stream()
        .flatMap(EventHypothesisDaoUtility::getFeatureMeasurements);
  }

  /**
   * Gets all {@link FeatureMeasurementDao}s found in the provided {@link EventDao}
   * @param eventDao the {@link EventDao} holding the {@link FeatureMeasurementDao}s to collect
   * @return A {@link Stream} of {@link FeatureMeasurementDao}s in the {@link EventDao}
   */
  public static Stream<FeatureMeasurementDao<?>> getFeatureMeasurementDaos(EventDao eventDao) {
    return eventDao.getHypotheses().stream()
        .flatMap(EventHypothesisDaoUtility::getFeatureMeasurementDaos);
  }

}
