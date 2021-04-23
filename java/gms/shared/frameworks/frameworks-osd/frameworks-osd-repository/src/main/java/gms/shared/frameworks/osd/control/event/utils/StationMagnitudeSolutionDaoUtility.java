package gms.shared.frameworks.osd.control.event.utils;

import com.google.common.base.Preconditions;
import gms.shared.frameworks.osd.coi.event.StationMagnitudeSolution;
import gms.shared.frameworks.osd.coi.event.repository.jpa.StationMagnitudeSolutionDao;
import gms.shared.frameworks.osd.coi.signaldetection.AmplitudeMeasurementValue;
import gms.shared.frameworks.osd.coi.signaldetection.FeatureMeasurement;
import gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.dataaccessobjects.AmplitudeFeatureMeasurementDao;
import gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.dataaccessobjects.FeatureMeasurementDao;
import gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.dataaccessobjects.StationDao;
import gms.shared.frameworks.osd.control.utils.FeatureMeasurementDaoUtility;
import javax.persistence.EntityManager;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Utility class to handle converting {@link StationMagnitudeSolution}s between their COI and DAO
 * representations, and persisting for reuse.
 */
public class StationMagnitudeSolutionDaoUtility {

  /**
   * Converts the provided {@link StationMagnitudeSolution} to a
   * {@link StationMagnitudeSolutionDao}. If the location solution has already been stored, its
   * corresponding {@link StationMagnitudeSolutionDao} is retrieved and returned.
   *
   * @param staMagSolution the {@link StationMagnitudeSolution} to convert
   * @param entityManager the {@link EntityManager} used to store and retrieve the
   * {@link StationMagnitudeSolutionDao} and it's dependencies
   * @return the converted {@link StationMagnitudeSolutionDao}
   */
  public static StationMagnitudeSolutionDao fromCoi(StationMagnitudeSolution staMagSolution,
      EntityManager entityManager) {

    Objects.requireNonNull(staMagSolution);
    Objects.requireNonNull(entityManager);
    Preconditions.checkState(entityManager.getTransaction().isActive());

    StationDao stationDao = entityManager.find(StationDao.class, staMagSolution.getStationName());

    Objects.requireNonNull(stationDao,
        "Cannot insert StationMagnitudeSolution for a Station that does not exist");

    StationMagnitudeSolutionDao staMagSolutionDao = new StationMagnitudeSolutionDao();
    staMagSolutionDao.setId(UUID.randomUUID());
    staMagSolutionDao.setType(staMagSolution.getType());
    staMagSolutionDao.setModel(staMagSolutionDao.getModel());
    staMagSolutionDao.setStation(stationDao);
    staMagSolutionDao.setPhase(staMagSolution.getPhase());
    staMagSolutionDao.setMagnitude(staMagSolution.getMagnitude());
    staMagSolutionDao.setMagnitudeUncertainty(staMagSolution.getMagnitudeUncertainty());
    staMagSolutionDao.setModel(staMagSolution.getModel());
    staMagSolutionDao.setModelCorrection(staMagSolution.getModelCorrection());
    staMagSolutionDao.setStationCorrection(staMagSolution.getStationCorrection());
    staMagSolutionDao.setMeasurement((AmplitudeFeatureMeasurementDao)
        FeatureMeasurementDaoUtility.fromCoi(staMagSolution.getMeasurement(), entityManager));

    return staMagSolutionDao;
  }

  /**
   * Converts the provided {@link StationMagnitudeSolutionDao} to its COI representation
   * @param staMagSolutionDao the {@link StationMagnitudeSolutionDao} to convert
   * @param measurement The {@link FeatureMeasurement} representing the
   * {@link FeatureMeasurementDao} used in the {@link StationMagnitudeSolutionDao}
   * @return the converted {@link StationMagnitudeSolution}
   */
  public static StationMagnitudeSolution toCoi(StationMagnitudeSolutionDao staMagSolutionDao,
      FeatureMeasurement<AmplitudeMeasurementValue> measurement) {

    Objects.requireNonNull(staMagSolutionDao);
    Objects.requireNonNull(measurement);

    return StationMagnitudeSolution.builder()
        .setType(staMagSolutionDao.getType())
        .setModel(staMagSolutionDao.getModel())
        .setStationName(staMagSolutionDao.getStation().getName())
        .setPhase(staMagSolutionDao.getPhase())
        .setMagnitude(staMagSolutionDao.getMagnitude())
        .setMagnitudeUncertainty(staMagSolutionDao.getMagnitudeUncertainty())
        .setModelCorrection(staMagSolutionDao.getModelCorrection())
        .setStationCorrection(staMagSolutionDao.getStationCorrection())
        .setMeasurement(measurement)
        .build();
  }

  /**
   * Gets all {@link FeatureMeasurement}s found in the provided {@link StationMagnitudeSolution}
   * @param solution the {@link StationMagnitudeSolution} holding the {@link FeatureMeasurement}s to collect
   * @return A {@link Stream} of {@link FeatureMeasurement}s in the {@link StationMagnitudeSolution}
   */
  public static Stream<FeatureMeasurement<?>> getFeatureMeasurements(StationMagnitudeSolution solution) {
    return Stream.of(solution.getMeasurement());
  }

  /**
   * Gets all {@link FeatureMeasurementDao}s found in the provided {@link StationMagnitudeSolutionDao}
   * @param solutionDao the {@link StationMagnitudeSolutionDao} holding the {@link FeatureMeasurementDao}s
   * to collect
   * @return A {@link Stream} of {@link FeatureMeasurementDao}s in the {@link StationMagnitudeSolutionDao}
   */
  public static Stream<FeatureMeasurementDao<?>> getFeatureMeasurementDaos(StationMagnitudeSolutionDao solutionDao) {
    return Stream.of(solutionDao.getMeasurement());
  }

}
