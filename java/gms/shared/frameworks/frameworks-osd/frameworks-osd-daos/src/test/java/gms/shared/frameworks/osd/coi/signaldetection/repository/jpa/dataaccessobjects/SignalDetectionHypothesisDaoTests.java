package gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.dataaccessobjects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gms.shared.frameworks.osd.coi.DoubleValue;
import gms.shared.frameworks.osd.coi.InstantValue;
import gms.shared.frameworks.osd.coi.PhaseType;
import gms.shared.frameworks.osd.coi.Units;
import gms.shared.frameworks.osd.coi.signaldetection.EnumeratedMeasurementValue.PhaseTypeMeasurementValue;
import gms.shared.frameworks.osd.coi.signaldetection.FeatureMeasurement;
import gms.shared.frameworks.osd.coi.signaldetection.FeatureMeasurementTypes;
import gms.shared.frameworks.osd.coi.signaldetection.NumericMeasurementValue;
import gms.shared.frameworks.osd.coi.signaldetection.SignalDetectionHypothesis;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


public class SignalDetectionHypothesisDaoTests {

  private SignalDetectionHypothesis updatedHypothesis;
  private SignalDetectionHypothesisDao hypothesisDao;

//  @BeforeEach
//  public void testUpdateDifferentIds() {
//    UUID creationInfoId = UUID.randomUUID();
//    SignalDetectionDao sdDao = new SignalDetectionDao(UUID.randomUUID(),
//        "test",
//        UUID.randomUUID(),
//        creationInfoId);
//
//    FeatureMeasurement<InstantValue> arrivalMeasurement = FeatureMeasurement.from(
//        UUID.randomUUID(),
//        FeatureMeasurementTypes.ARRIVAL_TIME,
//        InstantValue.from(Instant.EPOCH, Duration.ofSeconds(1)));
//
//    FeatureMeasurement<PhaseTypeMeasurementValue> phaseMeasurement = FeatureMeasurement.from(
//        UUID.randomUUID(),
//        FeatureMeasurementTypes.PHASE,
//        PhaseTypeMeasurementValue.from(PhaseType.P, 0.95));
//
//    hypothesisDao = new SignalDetectionHypothesisDao(UUID.randomUUID(),
//        sdDao,
//        false,
//        new InstantFeatureMeasurementDao(arrivalMeasurement),
//        new PhaseFeatureMeasurementDao(phaseMeasurement),
//        Collections.emptyList(),
//        creationInfoId);
//
//    DoubleValue otherValue = DoubleValue.from(1.0, 0.9, Units.UNITLESS);
//
//    FeatureMeasurement<NumericMeasurementValue> otherMeasurement = FeatureMeasurement.from(
//        UUID.randomUUID(),
//        FeatureMeasurementTypes.SLOWNESS,
//        NumericMeasurementValue.from(Instant.EPOCH, otherValue));
//
//    updatedHypothesis = SignalDetectionHypothesis
//        .builder(hypothesisDao.getSignalDetectionHypothesisId(),
//            sdDao.getSignalDetectionId(),
//            hypothesisDao.isRejected(),
//            creationInfoId)
//        .addMeasurement(arrivalMeasurement)
//        .addMeasurement(phaseMeasurement)
//        .addMeasurement(otherMeasurement)
//        .build();
//  }
//
//  @Test
//  public void testUpdateDifferentId() {
//    SignalDetectionHypothesis otherHypothesis = SignalDetectionHypothesis.builder(UUID.randomUUID(),
//        hypothesisDao.getParentSignalDetection().getSignalDetectionId(),
//        true,
//        hypothesisDao.getCreationInfoId())
//        .build();
//
//    assertThrows(IllegalStateException.class, () -> hypothesisDao.update(otherHypothesis));
//  }
//
//  @Test
//  public void testUpdate() {
//    hypothesisDao.update(updatedHypothesis);
//
//    assertEquals(updatedHypothesis.getId(), hypothesisDao.getSignalDetectionHypothesisId());
//    assertEquals(updatedHypothesis.getParentSignalDetectionId(), hypothesisDao.getParentSignalDetection().getSignalDetectionId());
//    assertEquals(updatedHypothesis.isRejected(), hypothesisDao.isRejected());
//    assertEquals(updatedHypothesis.getCreationInfoId(), hypothesisDao.getCreationInfoId());
//    assertEquals(updatedHypothesis.getFeatureMeasurements().size(), hypothesisDao.getFeatureMeasurements().size() + 2);
//
//    Optional<FeatureMeasurement<InstantValue>> expectedArrival = updatedHypothesis.getFeatureMeasurement(FeatureMeasurementTypes.ARRIVAL_TIME);
//    assertTrue(expectedArrival.isPresent());
//    assertEquals(expectedArrival.get(), hypothesisDao.getArrivalTimeMeasurement().toCoi());
//
//    Optional<FeatureMeasurement<PhaseTypeMeasurementValue>> expectedPhase = updatedHypothesis.getFeatureMeasurement(FeatureMeasurementTypes.PHASE);
//    assertTrue(expectedPhase.isPresent());
//    assertEquals(expectedPhase.get(), hypothesisDao.getPhaseMeasurement().toCoi());
//
//    Map<String, FeatureMeasurement<?>> featureMeasurementByTypeName = updatedHypothesis.getFeatureMeasurements()
//        .stream()
//        .collect(Collectors.toMap(FeatureMeasurement::getFeatureMeasurementTypeName, Function.identity()));
//
//    for (FeatureMeasurementDao<?> measurementDao : hypothesisDao.getFeatureMeasurements()) {
//      FeatureMeasurement<?> updatedMeasurement = featureMeasurementByTypeName.get(measurementDao.getFeatureMeasurementType().getCoiType().getFeatureMeasurementTypeName());
//      assertNotNull(updatedMeasurement);
//      assertEquals(updatedMeasurement, measurementDao.toCoi());
//    }
//  }

}
