package gms.shared.frameworks.osd.control.event.utils;

import com.google.common.base.Preconditions;
import gms.shared.frameworks.osd.coi.event.Event;
import gms.shared.frameworks.osd.coi.event.EventHypothesis;
import gms.shared.frameworks.osd.coi.event.FeaturePrediction;
import gms.shared.frameworks.osd.coi.event.PreferredLocationSolution;
import gms.shared.frameworks.osd.coi.event.SignalDetectionEventAssociation;
import gms.shared.frameworks.osd.coi.event.repository.jpa.EventHypothesisDao;
import gms.shared.frameworks.osd.coi.event.repository.jpa.FeaturePredictionDao;
import gms.shared.frameworks.osd.coi.event.repository.jpa.PreferredLocationSolutionDao;
import gms.shared.frameworks.osd.coi.event.repository.jpa.SignalDetectionEventAssociationDao;
import gms.shared.frameworks.osd.coi.signaldetection.FeatureMeasurement;
import gms.shared.frameworks.osd.coi.signaldetection.SignalDetection;
import gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.dataaccessobjects.FeatureMeasurementDao;
import gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.dataaccessobjects.SignalDetectionHypothesisDao;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utility for finding, converting, and storing {@link EventHypothesis} and
 * {@link EventHypothesisDao}
 */
public class EventHypothesisDaoUtility {

  /**
   * Converts an {@link EventHypothesis} to an {@link EventHypothesisDao}. If the corresponding
   * {@link EventHypothesisDao} has already been stored, the existing {@link EventHypothesisDao} is
   * retrieved and returned. Otherwise, a new one is created and persisted. An active transaction
   * in the {@link EntityManager} is required for this function.
   *
   * @param hypothesis the {@link EventHypothesis} to convert
   * @param entityManager the {@link EntityManager} used to persist the converted object and
   * retrieve dependencies
   * @return the {@link EventHypothesisDao} representing the provided {@link EventHypothesis}
   */
  public static EventHypothesisDao fromCoi(EventHypothesis hypothesis,
      EntityManager entityManager) {

    Objects.requireNonNull(hypothesis,
        "Cannot convert a null EventHypothesis");
    Objects.requireNonNull(entityManager,
        "Cannot convert an EventHypothesis using a null EntityManager");
    Preconditions.checkState(entityManager.getTransaction().isActive(),
        "Cannot convert an EventHypothesis without an active transaction");

    EventHypothesisDao dao = entityManager.find(EventHypothesisDao.class, hypothesis.getId());

    if (dao == null) {
      EventHypothesisDao hypothesisDao = new EventHypothesisDao();
      hypothesisDao.setId(hypothesis.getId());
      hypothesisDao.setEventId(hypothesis.getEventId());
      hypothesisDao.setParentEventHypotheses(hypothesis.getParentEventHypotheses());
      hypothesisDao.setRejected(hypothesis.isRejected());
      hypothesisDao.setLocationSolutions(hypothesis.getLocationSolutions().stream()
          .map(locationSolution -> LocationSolutionDaoUtility.fromCoi(locationSolution,
              entityManager))
          .collect(Collectors.toSet()));

      hypothesis.getPreferredLocationSolution().ifPresent(preferredLocationSolution -> {
        PreferredLocationSolutionDao preferredLocationSolutionDao =
            new PreferredLocationSolutionDao(UUID.randomUUID(),
                LocationSolutionDaoUtility.fromCoi(preferredLocationSolution.getLocationSolution(),
                entityManager));
        entityManager.persist(preferredLocationSolutionDao);
        hypothesisDao.setPreferredLocationSolution(preferredLocationSolutionDao);
      });

      // Should this validation live in EventRepositoryJpa
      Set<UUID> sdhIds = hypothesis.getAssociations().stream()
          .map(SignalDetectionEventAssociation::getSignalDetectionHypothesisId)
          .collect(Collectors.toSet());

      // TODO: this has performance consequences...consider making EventHypothesisDao directly
      // reference SignalDetectionHypothesis
      CriteriaBuilder builder = entityManager.getCriteriaBuilder();
      CriteriaQuery<Long> sdhCountQuery =
          builder.createQuery(Long.class);
      Root<SignalDetectionHypothesisDao> fromAssociation =
          sdhCountQuery.from(SignalDetectionHypothesisDao.class);
      sdhCountQuery.select(builder.count(fromAssociation))
          .where(fromAssociation.get("id").in(sdhIds));
      long sdhs = entityManager.createQuery(sdhCountQuery).getSingleResult();
      Preconditions.checkState(sdhIds.size() == sdhs,
          "Cannot create EventHypothesisDao where SignalDetectionEventAssociations reference " +
              "SignalDetectionHypotheses that have not been stored");

      hypothesisDao.setAssociations(hypothesis.getAssociations().stream()
          .map(SignalDetectionEventAssociationDao::new)
          .collect(Collectors.toSet()));

      dao = hypothesisDao;

      entityManager.merge(hypothesisDao);
    }

    return dao;
  }

  /**
   * Converts the provided {@link EventHypothesisDao} to the COI representation
   *
   * @param hypothesisDao the {@link EventHypothesisDao} to convert
   * {@link FeaturePredictionDao} used in the {@link EventHypothesisDao}
   * @param featureMeasurementsById A map of {@link FeatureMeasurement} by their IDs, representing
   * the {@link FeatureMeasurementDao} used in the {@link EventHypothesisDao}
   * @return the converted {@link EventHypothesis}
   */
  public static EventHypothesis toCoi(EventHypothesisDao hypothesisDao,
      Map<UUID, FeaturePrediction<?>> featurePredictionsById,
      Map<String, FeatureMeasurement<?>> featureMeasurementsById) {

    Objects.requireNonNull(hypothesisDao);
    Objects.requireNonNull(featurePredictionsById);
    Objects.requireNonNull(featureMeasurementsById);

    return EventHypothesis.builder()
        .setId(hypothesisDao.getId())
        .setEventId(hypothesisDao.getEventId())
        .setParentEventHypotheses(hypothesisDao.getParentEventHypotheses())
        .setRejected(hypothesisDao.isRejected())
        .setLocationSolutions(hypothesisDao.getLocationSolutions().stream()
            .map(locationSolutionDao -> LocationSolutionDaoUtility.toCoi(locationSolutionDao,
                featurePredictionsById,
                featureMeasurementsById))
            .collect(Collectors.toList()))
        .setPreferredLocationSolution(hypothesisDao.getPreferredLocationSolution() == null ? null :
            PreferredLocationSolution.from(LocationSolutionDaoUtility
                .toCoi(hypothesisDao.getPreferredLocationSolution().getLocationSolution(),
                    featurePredictionsById,
                    featureMeasurementsById)))
        .setAssociations(hypothesisDao.getAssociations().stream()
            .map(SignalDetectionEventAssociationDao::toCoi)
            .collect(Collectors.toList()))
        .build();
  }

  /**
   * Gets all {@link FeaturePrediction}s found in the provided {@link EventHypothesis}
   *
   * @param hypothesis the {@link EventHypothesis} holding the {@link FeaturePrediction}s to collect
   * @return A {@link Stream} of {@link FeaturePrediction}s in the {@link EventHypothesis}
   */
  public static Stream<FeaturePrediction<?>> getFeaturePredictions(EventHypothesis hypothesis) {
    return hypothesis.getLocationSolutions().stream()
        .flatMap(LocationSolutionDaoUtility::getFeaturePredictions);
  }

  /**
   * Gets all {@link FeaturePredictionDao}s found in the provided {@link EventHypothesisDao}
   *
   * @param hypothesisDao the {@link EventHypothesisDao} holding the {@link FeaturePredictionDao}
   * s to collect
   * @return A {@link Stream} of {@link FeaturePredictionDao}s in the {@link EventHypothesisDao}
   */
  public static Stream<FeaturePredictionDao<?>> getFeaturePredictionDaos(EventHypothesisDao hypothesisDao) {
    return hypothesisDao.getLocationSolutions().stream()
        .flatMap(LocationSolutionDaoUtility::getFeaturePredictionDaos);
  }

  /**
   * Gets all {@link FeatureMeasurement}s found in the provided {@link EventHypothesis}
   *
   * @param hypothesis the {@link Event} holding the {@link FeatureMeasurement}s to collect
   * @return A {@link Stream} of {@link FeatureMeasurement}s in the {@link EventHypothesis}
   */
  public static Stream<FeatureMeasurement<?>> getFeatureMeasurements(EventHypothesis hypothesis) {
    return hypothesis.getLocationSolutions().stream()
        .flatMap(LocationSolutionDaoUtility::getFeatureMeasurements);
  }

  /**
   * Gets all {@link FeatureMeasurementDao}s found in the provided {@link EventHypothesisDao}
   *
   * @param hypothesisDao the {@link EventHypothesisDao} holding the {@link FeatureMeasurementDao}s
   * to collect
   * @return A {@link Stream} of {@link FeatureMeasurementDao}s in the {@link EventHypothesisDao}
   */
  public static Stream<FeatureMeasurementDao<?>> getFeatureMeasurementDaos(EventHypothesisDao hypothesisDao) {
    return hypothesisDao.getLocationSolutions().stream()
        .flatMap(LocationSolutionDaoUtility::getFeatureMeasurementDaos);
  }
}
