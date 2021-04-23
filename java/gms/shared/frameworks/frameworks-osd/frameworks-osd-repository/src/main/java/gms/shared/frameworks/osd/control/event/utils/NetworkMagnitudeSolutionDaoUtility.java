package gms.shared.frameworks.osd.control.event.utils;

import com.google.common.base.Preconditions;
import gms.shared.frameworks.osd.coi.event.NetworkMagnitudeSolution;
import gms.shared.frameworks.osd.coi.event.repository.jpa.NetworkMagnitudeBehaviorDao;
import gms.shared.frameworks.osd.coi.event.repository.jpa.NetworkMagnitudeSolutionDao;
import gms.shared.frameworks.osd.coi.signaldetection.AmplitudeMeasurementValue;
import gms.shared.frameworks.osd.coi.signaldetection.FeatureMeasurement;
import gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.dataaccessobjects.FeatureMeasurementDao;
import javax.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utility class to handle converting {@link NetworkMagnitudeSolution}s between their COI and DAO
 * representations, and persisting for reuse.
 */
public class NetworkMagnitudeSolutionDaoUtility {

  /**
   * Converts the provided {@link NetworkMagnitudeSolution} to a
   * {@link NetworkMagnitudeSolutionDao}. If the location solution has already been stored, its
   * corresponding {@link NetworkMagnitudeSolutionDao} is retrieved and returned.
   *
   * @param netMagSolution the {@link NetworkMagnitudeSolution} to convert
   * @param entityManager the {@link EntityManager} used to store and retrieve the
   * {@link NetworkMagnitudeSolutionDao} and it's dependencies
   * @return the converted {@link NetworkMagnitudeSolutionDao}
   */
  public static NetworkMagnitudeSolutionDao fromCoi(NetworkMagnitudeSolution netMagSolution,
      EntityManager entityManager) {

    Objects.requireNonNull(netMagSolution);
    Objects.requireNonNull(entityManager);
    Preconditions.checkState(entityManager.getTransaction().isActive());

    NetworkMagnitudeSolutionDao netMagSolutionDao = new NetworkMagnitudeSolutionDao();
    netMagSolutionDao.setId(UUID.randomUUID());
    netMagSolutionDao.setMagnitudeType(netMagSolution.getMagnitudeType());
    netMagSolutionDao.setMagnitude(netMagSolution.getMagnitude());
    netMagSolutionDao.setUncertainty(netMagSolution.getUncertainty());

    List<NetworkMagnitudeBehaviorDao> netMagBehaviorDaos = netMagSolution
        .getNetworkMagnitudeBehaviors().stream()
        .map(netMagBehavior ->
            NetworkMagnitudeBehaviorDaoUtility.fromCoi(netMagBehavior, entityManager))
        .collect(Collectors.toList());

    netMagSolutionDao.setNetworkMagnitudeBehaviors(netMagBehaviorDaos);

    return netMagSolutionDao;
  }

  /**
   * Converts the provided {@link NetworkMagnitudeSolutionDao} to its COI representation
   * @param netMagSolutionDao the {@link NetworkMagnitudeSolutionDao} to convert
   * @param featureMeasurementsById A map of {@link FeatureMeasurement} by their IDs, representing
   * the {@link FeatureMeasurementDao} used in the {@link NetworkMagnitudeSolutionDao}
   * @return the converted {@link NetworkMagnitudeSolution}
   */
  public static NetworkMagnitudeSolution toCoi(NetworkMagnitudeSolutionDao netMagSolutionDao,
      Map<String, FeatureMeasurement<?>> featureMeasurementsById) {

    Objects.requireNonNull(netMagSolutionDao);
    Objects.requireNonNull(featureMeasurementsById);

    return NetworkMagnitudeSolution.builder()
        .setMagnitudeType(netMagSolutionDao.getMagnitudeType())
        .setMagnitude(netMagSolutionDao.getMagnitude())
        .setUncertainty(netMagSolutionDao.getUncertainty())
        .setNetworkMagnitudeBehaviors(netMagSolutionDao.getNetworkMagnitudeBehaviors().stream()
            .map(netMagBehaviorDao ->
                NetworkMagnitudeBehaviorDaoUtility.toCoi(netMagBehaviorDao,
                    (FeatureMeasurement<AmplitudeMeasurementValue>) featureMeasurementsById.get(
                        netMagBehaviorDao.getStationMagnitudeSolution().getMeasurement().getId())))
            .collect(Collectors.toSet()))
        .build();
  }

  /**
   * Gets all {@link FeatureMeasurement}s found in the provided {@link NetworkMagnitudeSolution}
   * @param solution the {@link NetworkMagnitudeSolution} holding the {@link FeatureMeasurement}s to collect
   * @return A {@link Stream} of {@link FeatureMeasurement}s in the {@link NetworkMagnitudeSolution}
   */
  public static Stream<FeatureMeasurement<?>> getFeatureMeasurements(NetworkMagnitudeSolution solution) {
    return solution.getNetworkMagnitudeBehaviors().stream()
        .flatMap(NetworkMagnitudeBehaviorDaoUtility::getFeatureMeasurements);
  }

  /**
   * Gets all {@link FeatureMeasurementDao}s found in the provided {@link NetworkMagnitudeSolutionDao}
   * @param solutionDao the {@link NetworkMagnitudeSolutionDao} holding the {@link FeatureMeasurementDao}s
   * to collect
   * @return A {@link Stream} of {@link FeatureMeasurementDao}s in the {@link NetworkMagnitudeSolutionDao}
   */
  public static Stream<FeatureMeasurementDao<?>> getFeatureMeasurementDaos(NetworkMagnitudeSolutionDao solutionDao) {
    return solutionDao.getNetworkMagnitudeBehaviors().stream()
        .flatMap(NetworkMagnitudeBehaviorDaoUtility::getFeatureMeasurementDaos);
  }

}
