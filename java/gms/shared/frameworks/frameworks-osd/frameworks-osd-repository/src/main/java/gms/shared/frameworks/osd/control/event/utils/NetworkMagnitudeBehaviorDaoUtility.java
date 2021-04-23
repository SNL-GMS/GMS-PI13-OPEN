package gms.shared.frameworks.osd.control.event.utils;

import com.google.common.base.Preconditions;
import gms.shared.frameworks.osd.coi.event.NetworkMagnitudeBehavior;
import gms.shared.frameworks.osd.coi.event.repository.jpa.NetworkMagnitudeBehaviorDao;
import gms.shared.frameworks.osd.coi.signaldetection.AmplitudeMeasurementValue;
import gms.shared.frameworks.osd.coi.signaldetection.FeatureMeasurement;
import gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.dataaccessobjects.FeatureMeasurementDao;
import javax.persistence.EntityManager;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Utility class to handle converting {@link NetworkMagnitudeBehavior}s between their COI and DAO
 * representations, and persisting for reuse.
 */
public class NetworkMagnitudeBehaviorDaoUtility {

  /**
   * Converts the provided {@link NetworkMagnitudeBehavior} to a
   * {@link NetworkMagnitudeBehaviorDao}. If the location solution has already been stored, its
   * corresponding {@link NetworkMagnitudeBehaviorDao} is retrieved and returned.
   *
   * @param netMagBehavior the {@link NetworkMagnitudeBehaviorDao} to convert
   * @param entityManager the {@link EntityManager} used to store and retrieve the
   * {@link NetworkMagnitudeBehaviorDao} and it's dependencies
   * @return the converted {@link NetworkMagnitudeBehaviorDao}
   */
  public static NetworkMagnitudeBehaviorDao fromCoi(NetworkMagnitudeBehavior netMagBehavior,
      EntityManager entityManager) {

    Objects.requireNonNull(netMagBehavior);
    Objects.requireNonNull(entityManager);
    Preconditions.checkState(entityManager.getTransaction().isActive());

    NetworkMagnitudeBehaviorDao netMagBehaviorDao = new NetworkMagnitudeBehaviorDao();
    netMagBehaviorDao.setId(UUID.randomUUID());
    netMagBehaviorDao.setDefining(netMagBehavior.isDefining());
    netMagBehaviorDao.setResidual(netMagBehavior.getResidual());
    netMagBehaviorDao.setWeight(netMagBehavior.getWeight());
    netMagBehaviorDao.setStationMagnitudeSolution(
        StationMagnitudeSolutionDaoUtility.fromCoi(netMagBehavior.getStationMagnitudeSolution(),
            entityManager));

    return netMagBehaviorDao;
  }

  /**
   * Converts the provided {@link NetworkMagnitudeBehaviorDao} to its COI representation
   * @param netMagBehaviorDao the {@link NetworkMagnitudeBehaviorDao} to convert
   * @param measurement The {@link FeatureMeasurement} representing the {@link FeatureMeasurementDao}
   * used in the {@link NetworkMagnitudeBehaviorDao}
   * @return the converted {@link NetworkMagnitudeBehavior}
   */
  public static NetworkMagnitudeBehavior toCoi(NetworkMagnitudeBehaviorDao netMagBehaviorDao,
      FeatureMeasurement<AmplitudeMeasurementValue> measurement) {

    Objects.requireNonNull(netMagBehaviorDao);
    Objects.requireNonNull(measurement);

    return NetworkMagnitudeBehavior.builder()
        .setDefining(netMagBehaviorDao.isDefining())
        .setResidual(netMagBehaviorDao.getResidual())
        .setWeight(netMagBehaviorDao.getWeight())
        .setStationMagnitudeSolution(
            StationMagnitudeSolutionDaoUtility.toCoi(netMagBehaviorDao.getStationMagnitudeSolution(),
                measurement))
        .build();
  }

  /**
   * Gets all {@link FeatureMeasurement}s found in the provided {@link NetworkMagnitudeBehavior}
   * @param behavior the {@link NetworkMagnitudeBehavior} holding the {@link FeatureMeasurement}s to collect
   * @return A {@link Stream} of {@link FeatureMeasurement}s in the {@link NetworkMagnitudeBehavior}
   */
  public static Stream<FeatureMeasurement<?>> getFeatureMeasurements(NetworkMagnitudeBehavior behavior) {
    return StationMagnitudeSolutionDaoUtility
        .getFeatureMeasurements(behavior.getStationMagnitudeSolution());
  }

  /**
   * Gets all {@link FeatureMeasurementDao}s found in the provided {@link NetworkMagnitudeBehaviorDao}
   * @param behaviorDao the {@link NetworkMagnitudeBehaviorDao} holding the {@link FeatureMeasurementDao}s
   * to collect
   * @return A {@link Stream} of {@link FeatureMeasurementDao}s in the {@link NetworkMagnitudeBehaviorDao}
   */
  public static Stream<FeatureMeasurementDao<?>> getFeatureMeasurementDaos(NetworkMagnitudeBehaviorDao behaviorDao) {
    return StationMagnitudeSolutionDaoUtility
        .getFeatureMeasurementDaos(behaviorDao.getStationMagnitudeSolution());
  }

}
