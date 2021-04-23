package gms.shared.frameworks.osd.control.utils;

import com.google.common.base.Functions;
import com.google.common.base.Preconditions;
import gms.shared.frameworks.osd.coi.signaldetection.FeatureMeasurement;
import gms.shared.frameworks.osd.coi.signaldetection.FeatureMeasurementTypes;
import gms.shared.frameworks.osd.coi.signaldetection.SignalDetectionHypothesis;
import gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.dataaccessobjects.FeatureMeasurementDao;
import gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.dataaccessobjects.FeatureMeasurementTypeDao;
import gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.dataaccessobjects.InstantFeatureMeasurementDao;
import gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.dataaccessobjects.PhaseFeatureMeasurementDao;
import gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.dataaccessobjects.SignalDetectionDao;
import gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.dataaccessobjects.SignalDetectionHypothesisDao;
import javax.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class SignalDetectionHypothesisDaoUtility {

  private SignalDetectionHypothesisDaoUtility() {
  }

  public static SignalDetectionHypothesisDao fromCoi(SignalDetectionHypothesis hypothesis,
      EntityManager entityManager) {
    Objects.requireNonNull(hypothesis);
    Objects.requireNonNull(entityManager);
    Preconditions.checkState(entityManager.getTransaction().isActive(),
        "Cannot convert SignalDetectionHypothesis without an active transaction");

    SignalDetectionDao parentDetection = entityManager.find(SignalDetectionDao.class,
        hypothesis.getParentSignalDetectionId());

    Objects.requireNonNull(parentDetection,
        "Cannot create SignalDetectionHypothesisDao from a SignalDetection that does not exist");

    SignalDetectionHypothesisDao parentHypothesis = null;
    if (hypothesis.getParentSignalDetectionHypothesisId().isPresent()) {
      parentHypothesis = entityManager.find(SignalDetectionHypothesisDao.class,
          hypothesis.getParentSignalDetectionHypothesisId().get());
      Objects.requireNonNull(parentHypothesis,
          "Cannot create SignalDetectionHypothesisDao from a parent hypothesis that doesn't exist");
    }

    Map<FeatureMeasurementTypeDao, FeatureMeasurementDao<?>> featureMeasurementDaos =
        hypothesis.getFeatureMeasurements()
            .stream()
            .map(fm -> FeatureMeasurementDaoUtility.fromCoi(fm, entityManager))
            .collect(Collectors.toMap(FeatureMeasurementDao::getFeatureMeasurementType,
                Functions.identity()));

    return new SignalDetectionHypothesisDao(hypothesis.getId(),
        parentDetection,
        hypothesis.getMonitoringOrganization(),
        hypothesis.getStationName(),
        parentHypothesis,
        hypothesis.isRejected(),
        (InstantFeatureMeasurementDao) featureMeasurementDaos.remove(FeatureMeasurementTypeDao.ARRIVAL_TIME),
        (PhaseFeatureMeasurementDao) featureMeasurementDaos.remove(FeatureMeasurementTypeDao.PHASE),
        featureMeasurementDaos.values());
  }

  public static SignalDetectionHypothesisDao toDao(SignalDetectionDao signalDetectionDao,
      SignalDetectionHypothesisDao parentSignalDetectionHypothesis,
      SignalDetectionHypothesis signalDetectionHypothesis,
      EntityManager entityManager) {
    Objects.requireNonNull(signalDetectionDao,
        "Cannot create SignalDetectionHypothesisDao from a null SignalDetectionDao");
    Objects.requireNonNull(signalDetectionHypothesis,
        "Cannot create SignalDetectionHypothesisDao from a null SignalDetectionHypothesis");
    Objects.requireNonNull(entityManager);

    // extract the arrival time and phase measurements ids to provide separately.
    final FeatureMeasurement arrivalTimeMeasurement = signalDetectionHypothesis
        .getFeatureMeasurement(FeatureMeasurementTypes.ARRIVAL_TIME)
        .orElseThrow(() ->
            new IllegalArgumentException(
                "Provided SignalDetectionHypothesis does not contain an ARRIVAL_TIME " +
                    "FeatureMeasurement")
        );

    final FeatureMeasurement phaseMeasurement = signalDetectionHypothesis
        .getFeatureMeasurement(FeatureMeasurementTypes.PHASE)
        .orElseThrow(() ->
            new IllegalArgumentException(
                "Provided SignalDetectionHypothesis does not contain a PHASE FeatureMeasurement")
        );

    InstantFeatureMeasurementDao arrivalTimeMeasurementDao = (InstantFeatureMeasurementDao)
        FeatureMeasurementDaoUtility.fromCoi(arrivalTimeMeasurement, entityManager);
    PhaseFeatureMeasurementDao phaseMeasurementDao = (PhaseFeatureMeasurementDao)
        FeatureMeasurementDaoUtility.fromCoi(phaseMeasurement, entityManager);

    List<FeatureMeasurementDao<?>> otherMeasurementDaos =
        signalDetectionHypothesis.getFeatureMeasurements()
            .stream()
            .filter(fm -> !fm.getFeatureMeasurementType().equals(FeatureMeasurementTypeDao.ARRIVAL_TIME.getCoiType())
                && !fm.getFeatureMeasurementType().equals(FeatureMeasurementTypeDao.PHASE.getCoiType()))
            .map(fm -> FeatureMeasurementDaoUtility.fromCoi(fm, entityManager))
            .collect(Collectors.toList());

    return new SignalDetectionHypothesisDao(
        signalDetectionHypothesis.getId(),
        signalDetectionDao,
        signalDetectionHypothesis.getMonitoringOrganization(),
        signalDetectionHypothesis.getStationName(),
        parentSignalDetectionHypothesis,
        signalDetectionHypothesis.isRejected(),
        arrivalTimeMeasurementDao,
        phaseMeasurementDao,
        otherMeasurementDaos);
  }

  public static SignalDetectionHypothesis fromDao(
      SignalDetectionHypothesisDao signalDetectionHypothesisDao,
      List<FeatureMeasurement<?>> featureMeasurements) {
    Objects.requireNonNull(signalDetectionHypothesisDao,
        "Cannot create SignalDetectionHypothesis from a null SignalDetectionHypothesisDao");
    Objects.requireNonNull(featureMeasurements,
        "Cannot create SignalDetectionHypothesis from null FeatureMeasurements");
    Preconditions.checkState(!featureMeasurements.isEmpty(),
        "Cannot create SignalDetectionHypothesis from empty FeatureMeasurements");
    return SignalDetectionHypothesis
        .from(signalDetectionHypothesisDao.getId(),
            signalDetectionHypothesisDao.getParentSignalDetection().getId(),
            signalDetectionHypothesisDao.getMonitoringOrganization(),
            signalDetectionHypothesisDao.getStationName(),
            signalDetectionHypothesisDao.getParentSignalDetectionHypothesis() == null ?
                null : signalDetectionHypothesisDao.getParentSignalDetectionHypothesis().getId(),
            signalDetectionHypothesisDao.isRejected(),
            featureMeasurements);
  }

  /**
   * Utility method used to find and return the {@link InstantFeatureMeasurementDao} in the
   * provided
   * {@link Set} of {@link FeatureMeasurementDao}s that contains the provided {@link
   * FeatureMeasurement} id.
   *
   * @param featureMeasurementDaos {@link Set} of {@link FeatureMeasurementDao}s that should
   * contain
   * an {@link InstantFeatureMeasurementDao} corresponding with the provided {@link
   * FeatureMeasurement}.
   * @param featureMeasurement {@link FeatureMeasurement} corresponding to the {@link
   * InstantFeatureMeasurementDao} to find and return from the provided {@link Set} of {@link
   * FeatureMeasurementDao}s
   * @throws IllegalArgumentException if the provided {@link FeatureMeasurement} id corresponds
   * with
   * a {@link FeatureMeasurementDao} that is not an instance of {@link
   * InstantFeatureMeasurementDao}.
   */
  private static InstantFeatureMeasurementDao filterInstantFeatureMeasurementDao(
      Set<FeatureMeasurementDao<?>> featureMeasurementDaos, FeatureMeasurement featureMeasurement) {

    // Filter for the FeatureMeasurementDao with the provided featureMeasurementId
    FeatureMeasurementDao genericFeatureMeasurementDao = SignalDetectionHypothesisDaoUtility
        .filterFeatureMeasurementDao(featureMeasurementDaos, featureMeasurement);

    // If the filtered FeatureMeasurementDao is not an instance of InstantFeatureMeasurementDao,
    // throw IllegalStateExample
    if (!(genericFeatureMeasurementDao instanceof InstantFeatureMeasurementDao)) {

      throw new IllegalArgumentException(String.format(
          "%s with provided %s id is not an instance of %s",
          FeatureMeasurementDao.class.getSimpleName(),
          FeatureMeasurement.class.getSimpleName(),
          InstantFeatureMeasurementDao.class.getSimpleName()));
    }

    // Cast FeatureMeasurementDao to InstantFeatureMeasurementDao and return
    return (InstantFeatureMeasurementDao) genericFeatureMeasurementDao;
  }

  /**
   * Utility method used to find and return the {@link PhaseFeatureMeasurementDao} in the provided
   * {@link Set} of {@link FeatureMeasurementDao}s that contains the provided {@link
   * FeatureMeasurement} id.
   *
   * @param featureMeasurementDaos {@link Set} of {@link FeatureMeasurementDao}s that should
   * contain
   * a {@link PhaseFeatureMeasurementDao} corresponding with the provided {@link
   * FeatureMeasurement}.
   * @param featureMeasurement {@link FeatureMeasurement} corresponding to the {@link
   * PhaseFeatureMeasurementDao} to find and return from the provided {@link Set} of {@link
   * FeatureMeasurementDao}s
   * @throws IllegalArgumentException if the provided {@link FeatureMeasurement} id corresponds
   * with
   * a {@link FeatureMeasurementDao} that is not an instance of {@link PhaseFeatureMeasurementDao}.
   */
  private static PhaseFeatureMeasurementDao filterPhaseFeatureMeasurementDao(
      Set<FeatureMeasurementDao<?>> featureMeasurementDaos, FeatureMeasurement featureMeasurement) {

    // Filter for the FeatureMeasurementDao with the provided featureMeasurementId
    FeatureMeasurementDao genericFeatureMeasurementDao = SignalDetectionHypothesisDaoUtility
        .filterFeatureMeasurementDao(featureMeasurementDaos, featureMeasurement);

    // If the filtered FeatureMeasurementDao is not an instance of PhaseFeatureMeasurementDao,
    // throw IllegalStateExample
    if (!(genericFeatureMeasurementDao instanceof PhaseFeatureMeasurementDao)) {

      throw new IllegalArgumentException(String.format(
          "%s with provided %s id is not an instance of %s",
          FeatureMeasurementDao.class.getSimpleName(),
          FeatureMeasurement.class.getSimpleName(),
          PhaseFeatureMeasurementDao.class.getSimpleName()));
    }

    // Cast FeatureMeasurementDao to PhaseFeatureMeasurementDao and return
    return (PhaseFeatureMeasurementDao) genericFeatureMeasurementDao;
  }

  /**
   * Utility method used to find and return the {@link FeatureMeasurementDao} in the provided
   * {@link
   * Set} of {@link FeatureMeasurementDao}s that contains the provided {@link FeatureMeasurement}
   * id.
   *
   * @param featureMeasurementDaos {@link Set} of {@link FeatureMeasurementDao}s that should
   * contain
   * a {@link FeatureMeasurementDao} corresponding with the provided
   * {@link FeatureMeasurementDao}.
   * @param featureMeasurement Corresponding {@link FeatureMeasurement} to find and return from
   * the
   * provided {@link Set} of {@link FeatureMeasurementDao}s
   * @return Single {@link FeatureMeasurementDao}.  Not null.
   * @throws IllegalArgumentException if the provided {@link Set} of
   * {@link FeatureMeasurementDao}s
   * contains zero or more than one {@link FeatureMeasurementDao} with the provided {@link
   * FeatureMeasurement} id.
   */
  private static FeatureMeasurementDao<?> filterFeatureMeasurementDao(
      Set<FeatureMeasurementDao<?>> featureMeasurementDaos, FeatureMeasurement featureMeasurement) {

    // Filter for the FeatureMeasurementDao with the provided featureMeasurementId.  Throw
    // IllegalArgumentException if the number of FeatureMeasurementDaos with the provided
    // featureMeasurementId is not equal to 1.
    return featureMeasurementDaos.stream()
        .filter(fmDao -> fmDao.getId().equals(
            FeatureMeasurementDaoUtility.buildId(featureMeasurement)))
        .reduce((a, b) -> {

          // We are expecting a single FeatureMeasurementDao to be returned by filter().
          // If this reduction gets called, there is more than one FeatureMeasurementDao
          // in the streamed set with the same id, so throw IllegalStateException.
          throw new IllegalArgumentException(
              "Provided featureMeasurementDaos set contains multiple FeatureMeasurementDaos with " +
                  "the same id");
        })
        .orElseThrow(() ->

            // We are expecting a single FeatureMeasurementDao to be returned by the result of
            // filter().
            // If this lambda is called, it means zero FeatureMeasurementDaos were returned by
            // filter().
            new IllegalArgumentException(
                "Provided featureMeasurementDaos set does not contain a FeatureMeasurementDao " +
                    "with the provided id")
        );
  }
}
