package gms.shared.frameworks.osd.control.event.utils;

import com.google.common.base.Preconditions;
import gms.shared.frameworks.osd.coi.event.FeaturePrediction;
import gms.shared.frameworks.osd.coi.event.LocationBehavior;
import gms.shared.frameworks.osd.coi.event.repository.jpa.FeaturePredictionDao;
import gms.shared.frameworks.osd.coi.event.repository.jpa.LocationBehaviorDao;
import gms.shared.frameworks.osd.coi.signaldetection.FeatureMeasurement;
import gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.dataaccessobjects.FeatureMeasurementDao;
import gms.shared.frameworks.osd.control.utils.FeatureMeasurementDaoUtility;
import javax.persistence.EntityManager;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Utility for finding, converting, and storing {@link LocationBehavior} and
 * {@link LocationBehaviorDao}
 */
public class LocationBehaviorDaoUtility {

  /**
   * Converts an {@link LocationBehavior} to an {@link LocationBehaviorDao}. If the corresponding
   * {@link LocationBehaviorDao} has already been stored, the existing {@link LocationBehaviorDao}
   * is retrieved and returned. Otherwise, a new one is created and persisted. An active transaction
   * in the {@link EntityManager} is required for this function.
   * @param behavior the {@link LocationBehavior} to convert
   * @param entityManager the {@link EntityManager} used to persist the converted object, and
   * retrieve the existing object, if it exists
   * @return the {@link FeaturePredictionDao} representing the provided {@link LocationBehavior}
   */
  public static LocationBehaviorDao fromCoi(LocationBehavior behavior,
      EntityManager entityManager) {

    Objects.requireNonNull(behavior);
    Objects.requireNonNull(entityManager);
    Preconditions.checkState(entityManager.getTransaction().isActive());

    LocationBehaviorDao behaviorDao = new LocationBehaviorDao();
    behaviorDao.setId(UUID.randomUUID());
    behaviorDao.setResidual(behavior.getResidual());
    behaviorDao.setWeight(behavior.getWeight());
    behaviorDao.setDefining(behavior.isDefining());
    behaviorDao.setFeaturePrediction(FeaturePredictionDaoUtility.fromCoi(behavior.getFeaturePrediction(), entityManager));
    behaviorDao.setFeatureMeasurement(FeatureMeasurementDaoUtility.fromCoi(behavior.getFeatureMeasurement(), entityManager));

    return behaviorDao;
  }

  /**
   * Converts the provided {@link LocationBehaviorDao} to the COI representation
   * @param behaviorDao the {@link LocationBehaviorDao} to convert
   * {@link LocationBehaviorDao} used in the {@link LocationBehaviorDao}
   * @return the converted {@link LocationBehavior}
   */
  public static LocationBehavior toCoi(LocationBehaviorDao behaviorDao,
      FeatureMeasurement<?> featureMeasurement) {

    Objects.requireNonNull(behaviorDao);
    Objects.requireNonNull(featureMeasurement);

    return LocationBehavior.from(behaviorDao.getResidual(),
        behaviorDao.getWeight(),
        behaviorDao.isDefining(),
        FeaturePredictionDaoUtility.toCoi(behaviorDao.getFeaturePrediction()),
        featureMeasurement);
  }

  /**
   * Gets all {@link FeaturePrediction}s found in the provided {@link LocationBehavior}
   * @param behavior the {@link LocationBehavior} holding the {@link FeaturePrediction}s to collect
   * @return A {@link Stream} of {@link FeaturePrediction}s in the {@link LocationBehavior}
   */
  public static Stream<FeaturePrediction<?>> getFeaturePredictions(LocationBehavior behavior) {
    return Stream.of(behavior.getFeaturePrediction());
  }

  /**
   * Gets all {@link FeaturePredictionDao}s found in the provided {@link LocationBehaviorDao}
   * @param behaviorDao the {@link LocationBehaviorDao} holding the {@link FeaturePredictionDao}s to
   * collect
   * @return A {@link Stream} of {@link FeaturePredictionDao}s in the {@link LocationBehaviorDao}
   */
  public static Stream<FeaturePredictionDao<?>> getFeaturePredictionDaos(LocationBehaviorDao behaviorDao) {
    return Stream.of(behaviorDao.getFeaturePrediction());
  }

  /**
   * Gets all {@link FeatureMeasurement}s found in the provided {@link LocationBehavior}
   * @param  behavior the {@link LocationBehavior} holding the {@link FeatureMeasurement}s to collect
   * @return A {@link Stream} of {@link FeatureMeasurement}s in the {@link LocationBehavior}
   */
  public static Stream<FeatureMeasurement<?>> getFeatureMeasurements(LocationBehavior behavior) {
    return Stream.of(behavior.getFeatureMeasurement());
  }

  /**
   * Gets all {@link FeatureMeasurementDao}s found in the provided {@link LocationBehaviorDao}
   * @param behaviorDao the {@link LocationBehaviorDao} holding the {@link FeatureMeasurementDao}s
   * to collect
   * @return A {@link Stream} of {@link FeatureMeasurementDao}s in the {@link LocationBehaviorDao}
   */
  public static Stream<FeatureMeasurementDao<?>> getFeatureMeasurementDaos(LocationBehaviorDao behaviorDao) {
    return Stream.of(behaviorDao.getFeatureMeasurement());
  }

}
