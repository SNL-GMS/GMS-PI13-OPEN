package gms.shared.frameworks.osd.control.event.utils;

import com.google.common.base.Preconditions;
import gms.shared.frameworks.osd.coi.event.Event;
import gms.shared.frameworks.osd.coi.event.FeaturePrediction;
import gms.shared.frameworks.osd.coi.event.LocationSolution;
import gms.shared.frameworks.osd.coi.event.repository.jpa.EventLocationDao;
import gms.shared.frameworks.osd.coi.event.repository.jpa.FeaturePredictionDao;
import gms.shared.frameworks.osd.coi.event.repository.jpa.LocationBehaviorDao;
import gms.shared.frameworks.osd.coi.event.repository.jpa.LocationRestraintDao;
import gms.shared.frameworks.osd.coi.event.repository.jpa.LocationSolutionDao;
import gms.shared.frameworks.osd.coi.event.repository.jpa.LocationUncertaintyDao;
import gms.shared.frameworks.osd.coi.event.repository.jpa.NetworkMagnitudeSolutionDao;
import gms.shared.frameworks.osd.coi.signaldetection.FeatureMeasurement;
import gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.dataaccessobjects.FeatureMeasurementDao;
import javax.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utility class to handle converting {@link LocationSolution}s between their COI and DAO
 * representations, and persisting for reuse.
 */
public class LocationSolutionDaoUtility {

  /**
   * Converts the provided {@link LocationSolution} to a {@link LocationSolutionDao}.  If the
   * location solution has already been stored, its corresponding {@link LocationSolutionDao} is
   * retrieved and returned.
   * @param locationSolution the {@link LocationSolution} to convert
   * @param entityManager the {@link EntityManager} used to store and retrieve the
   * {@link LocationSolutionDao} and it's dependencies
   * @return the converted {@link LocationSolutionDao}
   */
  public static LocationSolutionDao fromCoi(LocationSolution locationSolution,
      EntityManager entityManager) {

    Objects.requireNonNull(locationSolution);
    Objects.requireNonNull(entityManager);
    Preconditions.checkState(entityManager.getTransaction().isActive());

    LocationSolutionDao dao = entityManager.find(LocationSolutionDao.class, locationSolution.getId());

    if (dao == null) {
      LocationSolutionDao solutionDao = new LocationSolutionDao();
      solutionDao.setId(locationSolution.getId());
      solutionDao.setLocation(new EventLocationDao(locationSolution.getLocation()));
      solutionDao.setLocationRestraint(new LocationRestraintDao(locationSolution.getLocationRestraint()));
      locationSolution.getLocationUncertainty().ifPresent(locationUncertainty ->
          solutionDao.setLocationUncertainty(new LocationUncertaintyDao(locationUncertainty)));

      Set<FeaturePredictionDao<?>> featurePredicationDaos = locationSolution.getFeaturePredictions()
          .stream()
          .map(fp -> FeaturePredictionDaoUtility.fromCoi(fp, entityManager))
          .collect(Collectors.toSet());

      solutionDao.setFeaturePredictions(featurePredicationDaos);

      Set<LocationBehaviorDao> behaviorDaos = locationSolution.getLocationBehaviors()
          .stream()
          .map(behavior -> LocationBehaviorDaoUtility.fromCoi(behavior, entityManager))
          .collect(Collectors.toSet());

      solutionDao.setLocationBehaviors(behaviorDaos);

      List<NetworkMagnitudeSolutionDao> netMagSolutionDaos = locationSolution
          .getNetworkMagnitudeSolutions()
          .stream()
          .map(netMagSolution -> NetworkMagnitudeSolutionDaoUtility.fromCoi(netMagSolution,
              entityManager))
          .collect(Collectors.toList());

      solutionDao.setNetworkMagnitudeSolutions(netMagSolutionDaos);
      dao = solutionDao;

      entityManager.merge(dao);
    }

    return dao;
  }

  /**
   * Converts the provided {@link LocationSolutionDao} to its COI representation
   * @param locationSolutionDao the {@link LocationSolutionDao} to convert
   * {@link FeaturePredictionDao} used in the {@link LocationSolutionDao}
   * @param featureMeasurementsById A map of {@link FeatureMeasurement} by their IDs, representing
   * the {@link FeatureMeasurementDao} used in the {@link LocationSolutionDao}
   * @return the converted {@link LocationSolution}
   */
  public static LocationSolution toCoi(LocationSolutionDao locationSolutionDao,
      Map<UUID, FeaturePrediction<?>> featurePredictionsById,
      Map<String, FeatureMeasurement<?>> featureMeasurementsById) {

    Objects.requireNonNull(locationSolutionDao);
    Objects.requireNonNull(featurePredictionsById);
    Objects.requireNonNull(featureMeasurementsById);

    return LocationSolution.builder()
        .setId(locationSolutionDao.getId())
        .setLocation(locationSolutionDao.getLocation().toCoi())
        .setLocationRestraint(locationSolutionDao.getLocationRestraint().toCoi())
        .setLocationUncertainty(Optional.ofNullable(
            locationSolutionDao.getLocationUncertainty() == null ?
                null :
                locationSolutionDao.getLocationUncertainty().toCoi()))
        .setFeaturePredictions(locationSolutionDao.getFeaturePredictions().stream()
            .map(FeaturePredictionDao::getId)
            .map(featurePredictionsById::get)
            .collect(Collectors.toList()))
        .setLocationBehaviors(locationSolutionDao.getLocationBehaviors().stream()
            .map(behaviorDao -> LocationBehaviorDaoUtility.toCoi(behaviorDao,
                featureMeasurementsById.get(behaviorDao.getFeatureMeasurement().getId())))
            .collect(Collectors.toList()))
        .setNetworkMagnitudeSolutions(locationSolutionDao.getNetworkMagnitudeSolutions().stream()
            .map(netMagSolutionDao -> NetworkMagnitudeSolutionDaoUtility.toCoi(netMagSolutionDao, featureMeasurementsById))
            .collect(Collectors.toList()))
        .build();
  }

  /**
   * Gets all {@link FeaturePrediction}s found in the provided {@link LocationSolution}
   * @param solution the {@link LocationSolution} holding the {@link FeaturePrediction}s to collect
   * @return A {@link Stream} of {@link FeaturePrediction}s in the {@link LocationSolution}
   */
  public static Stream<FeaturePrediction<?>> getFeaturePredictions(LocationSolution solution) {
    return Stream.concat(solution.getFeaturePredictions().stream(),
        solution.getLocationBehaviors().stream()
            .flatMap(LocationBehaviorDaoUtility::getFeaturePredictions));
  }

  /**
   * Gets all {@link FeaturePredictionDao}s found in the provided {@link LocationSolutionDao}
   * @param solutionDao the {@link LocationSolutionDao} holding the {@link FeaturePredictionDao}s to collect
   * @return A {@link Stream} of {@link FeaturePredictionDao}s in the {@link LocationSolutionDao}
   */
  public static Stream<FeaturePredictionDao<?>> getFeaturePredictionDaos(LocationSolutionDao solutionDao) {
    return Stream.concat(solutionDao.getFeaturePredictions().stream(),
        solutionDao.getLocationBehaviors().stream()
            .flatMap(LocationBehaviorDaoUtility::getFeaturePredictionDaos));
  }

  /**
   * Gets all {@link FeatureMeasurement}s found in the provided {@link LocationSolution}
   * @param solution the {@link LocationSolution} holding the {@link FeatureMeasurement}s to collect
   * @return A {@link Stream} of {@link FeatureMeasurement}s in the {@link LocationSolution}
   */
  public static Stream<FeatureMeasurement<?>> getFeatureMeasurements(LocationSolution solution) {
    return Stream.concat(solution.getLocationBehaviors().stream()
            .flatMap(LocationBehaviorDaoUtility::getFeatureMeasurements),
        solution.getNetworkMagnitudeSolutions().stream()
            .flatMap(NetworkMagnitudeSolutionDaoUtility::getFeatureMeasurements));
  }

  /**
   * Gets all {@link FeatureMeasurementDao}s found in the provided {@link LocationSolutionDao}
   * @param solutionDao the {@link LocationSolutionDao} holding the {@link FeatureMeasurementDao}s
   * to collect
   * @return A {@link Stream} of {@link FeatureMeasurementDao}s in the {@link LocationSolutionDao}
   */
  public static Stream<FeatureMeasurementDao<?>> getFeatureMeasurementDaos(LocationSolutionDao solutionDao) {
    return Stream.concat(solutionDao.getLocationBehaviors().stream()
            .flatMap(LocationBehaviorDaoUtility::getFeatureMeasurementDaos),
        solutionDao.getNetworkMagnitudeSolutions().stream()
            .flatMap(NetworkMagnitudeSolutionDaoUtility::getFeatureMeasurementDaos));
  }
}
