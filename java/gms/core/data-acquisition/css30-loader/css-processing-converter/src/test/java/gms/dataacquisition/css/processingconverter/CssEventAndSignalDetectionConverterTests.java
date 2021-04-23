package gms.dataacquisition.css.processingconverter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gms.dataacquisition.css.processingconverter.commandline.CssEventAndSignalDetectionConverterArguments;
import gms.shared.frameworks.osd.coi.DoubleValue;
import gms.shared.frameworks.osd.coi.InstantValue;
import gms.shared.frameworks.osd.coi.PhaseType;
import gms.shared.frameworks.osd.coi.Units;
import gms.shared.frameworks.osd.coi.channel.Channel;
import gms.shared.frameworks.osd.coi.event.DepthRestraintType;
import gms.shared.frameworks.osd.coi.event.Ellipse;
import gms.shared.frameworks.osd.coi.event.Event;
import gms.shared.frameworks.osd.coi.event.EventHypothesis;
import gms.shared.frameworks.osd.coi.event.EventLocation;
import gms.shared.frameworks.osd.coi.event.FeaturePrediction;
import gms.shared.frameworks.osd.coi.event.FeaturePredictionComponent;
import gms.shared.frameworks.osd.coi.event.FeaturePredictionCorrectionType;
import gms.shared.frameworks.osd.coi.event.LocationBehavior;
import gms.shared.frameworks.osd.coi.event.LocationRestraint;
import gms.shared.frameworks.osd.coi.event.LocationSolution;
import gms.shared.frameworks.osd.coi.event.LocationUncertainty;
import gms.shared.frameworks.osd.coi.event.MagnitudeModel;
import gms.shared.frameworks.osd.coi.event.MagnitudeType;
import gms.shared.frameworks.osd.coi.event.NetworkMagnitudeBehavior;
import gms.shared.frameworks.osd.coi.event.NetworkMagnitudeSolution;
import gms.shared.frameworks.osd.coi.event.PreferredLocationSolution;
import gms.shared.frameworks.osd.coi.event.RestraintType;
import gms.shared.frameworks.osd.coi.event.ScalingFactorType;
import gms.shared.frameworks.osd.coi.event.SignalDetectionEventAssociation;
import gms.shared.frameworks.osd.coi.event.StationMagnitudeSolution;
import gms.shared.frameworks.osd.coi.signaldetection.AmplitudeMeasurementValue;
import gms.shared.frameworks.osd.coi.signaldetection.EnumeratedMeasurementValue.PhaseTypeMeasurementValue;
import gms.shared.frameworks.osd.coi.signaldetection.FeatureMeasurement;
import gms.shared.frameworks.osd.coi.signaldetection.FeatureMeasurementType;
import gms.shared.frameworks.osd.coi.signaldetection.FeatureMeasurementTypes;
import gms.shared.frameworks.osd.coi.signaldetection.NumericMeasurementValue;
import gms.shared.frameworks.osd.coi.signaldetection.SignalDetection;
import gms.shared.frameworks.osd.coi.signaldetection.SignalDetectionHypothesis;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.swing.text.html.CSS;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class CssEventAndSignalDetectionConverterTests {

  private static CssEventAndSignalDetectionConverter converter;
  private static final double TOLERANCE = 0.000000000001;

  private static Map<Integer, Long> aridToWfid;
  private static Map<Long, Channel> wfidToChannel;

  @BeforeAll
  static void setup() throws IOException {
    final String basePath = "src/test/resources/processingfiles/";
    final String aridToWfidFile = basePath + "Arid2Wfid.json";
    aridToWfid = AridToWfidJsonReader.read(aridToWfidFile);
    final String wfidToChannelFile = basePath + "WfidToChannel.json";
    wfidToChannel = CssEventAndSignalDetectionConverter.readWfidToChanFile(wfidToChannelFile);

    String[] args = {basePath + "ueb_test.event", basePath + "ueb_test.origin",
        basePath + "ueb_test.origerr",
        basePath + "ueb_test.arrival", basePath + "ueb_test.assoc", basePath + "ueb_test.amplitude",
        basePath + "ueb_test.netmag", basePath + "ueb_test.stamag",
        basePath + "ueb_test.wfdisc", aridToWfidFile, wfidToChannelFile};

    converter = new CssEventAndSignalDetectionConverter(args);
  }

  @Test
  void testEventConversion() {
    //Check overall Event's are of expected size
    final Collection<Event> events = converter.getEvents();
    assertNotNull(events);
    assertEquals(104, events.size());

    //Check particular Event
    final UUID expectedOrid = UUID.nameUUIDFromBytes("48836076".getBytes());
    final List<Event> eventsWithOrid48836076 = events.stream()
        .filter(x -> x.getId().equals(expectedOrid))
        .collect(Collectors.toList());
    assertEquals(1, eventsWithOrid48836076.size());
    final Event event = eventsWithOrid48836076.get(0);
    assertEquals(expectedOrid, event.getId());
    assertEquals(0, event.getRejectedSignalDetectionAssociations().size());
    assertEquals(CssEventAndSignalDetectionConverter.MONITORING_ORG,
        event.getMonitoringOrganization());
    assertEquals(1, event.getHypotheses().size());
    assertEquals(1, event.getFinalEventHypothesisHistory().size());
    assertEquals(1, event.getPreferredEventHypothesisHistory().size());
    assertFalse(event.isRejected());

    //Check Event Hypothesis
    final EventHypothesis eventHypothesis = event.getHypotheses().iterator().next();
    assertEquals(expectedOrid, eventHypothesis.getEventId());
    assertFalse(eventHypothesis.isRejected());
    assertEquals(1, eventHypothesis.getLocationSolutions().size());
    assertEquals(event.getId(), eventHypothesis.getEventId());
    assertTrue(eventHypothesis.getParentEventHypotheses().isEmpty());
    assertFalse(eventHypothesis.isRejected());
    assertEquals(event.getId(), eventHypothesis.getId());

    //Check Location Solution
    final LocationSolution locationSolution = eventHypothesis.getLocationSolutions()
        .iterator().next();
    assertEquals(event.getId(), locationSolution.getId());

    //Check Event Location
    final EventLocation eventLoc = locationSolution.getLocation();
    assertEquals(67.5141, eventLoc.getLatitudeDegrees(), TOLERANCE);
    assertEquals(32.8323, eventLoc.getLongitudeDegrees(), TOLERANCE);
    assertEquals(0.0, eventLoc.getDepthKm(), TOLERANCE);
    assertEquals(Instant.ofEpochSecond(1274387032).plusNanos(270000000), eventLoc.getTime());

    //Check Location Restraint
    final LocationRestraint locationRestraint = locationSolution.getLocationRestraint();
    assertEquals(DepthRestraintType.FIXED_AT_SURFACE, locationRestraint.getDepthRestraintType());
    assertFalse(locationRestraint.getDepthRestraintKm().isPresent());
    assertEquals(RestraintType.UNRESTRAINED, locationRestraint.getLatitudeRestraintType());
    assertEquals(RestraintType.UNRESTRAINED, locationRestraint.getLongitudeRestraintType());
    assertFalse(locationRestraint.getLatitudeRestraintDegrees().isPresent());
    assertFalse(locationRestraint.getLongitudeRestraintDegrees().isPresent());
    assertFalse(locationRestraint.getTimeRestraint().isPresent());

    //Check Location Uncertainty
    assertTrue(locationSolution.getLocationUncertainty().isPresent());
    final LocationUncertainty locationUncertainty = locationSolution.getLocationUncertainty().get();
    assertEquals(334.8672, locationUncertainty.getXx(), TOLERANCE);
    assertEquals(220.0772, locationUncertainty.getXy(), TOLERANCE);
    assertEquals(-1.0, locationUncertainty.getXz(), TOLERANCE);
    assertEquals(-19.3451, locationUncertainty.getXt(), TOLERANCE);
    assertEquals(407.1720, locationUncertainty.getYy(), TOLERANCE);
    assertEquals(-1, locationUncertainty.getXz(), TOLERANCE);
    assertEquals(16.4265, locationUncertainty.getYt(), TOLERANCE);
    assertEquals(-1, locationUncertainty.getZz(), TOLERANCE);
    assertEquals(-1, locationUncertainty.getZt(), TOLERANCE);
    assertEquals(4.8457, locationUncertainty.getTt(), TOLERANCE);
    assertEquals(1.1833, locationUncertainty.getStDevOneObservation(), TOLERANCE);

    //Test Ellipsoids (do't get any dta from css so its empty)
    assertTrue(locationUncertainty.getEllipsoids().isEmpty());

    //Test Ellipse
    final Ellipse ellipse = locationUncertainty.getEllipses().iterator().next();
    assertEquals(ScalingFactorType.CONFIDENCE, ellipse.getScalingFactorType());
    assertEquals(0.0, ellipse.getkWeight(), TOLERANCE);
    assertEquals(0.9, ellipse.getConfidenceLevel(), TOLERANCE);
    assertEquals(52.2742, ellipse.getMajorAxisLength(), TOLERANCE);
    assertEquals(40.34, ellipse.getMajorAxisTrend(), TOLERANCE);
    assertEquals(26.0914, ellipse.getMinorAxisLength(), TOLERANCE);
    assertEquals(-1.0, ellipse.getMinorAxisTrend(), TOLERANCE);
    assertEquals(-1, ellipse.getDepthUncertainty(), TOLERANCE);
    assertEquals(Duration.ofNanos((long) (3.624 * 1e9)), ellipse.getTimeUncertainty());

    //Test Feature Prediction, we don't get this data from css so
    assertEquals(0, locationSolution.getFeaturePredictions().size());

    //Check Preferred Location Solution
    assertTrue(eventHypothesis.getPreferredLocationSolution().isPresent());
    assertEquals(PreferredLocationSolution.from(locationSolution),
        eventHypothesis.getPreferredLocationSolution().get());

    //Check Signal Detection Association
    final Set<SignalDetectionEventAssociation> signalDetectionEventAssociations =
        eventHypothesis.getAssociations();
    final int arid = 59210193;
    assertEquals(1, signalDetectionEventAssociations.size());
    final UUID expectedArid = UUID.nameUUIDFromBytes(String.valueOf(arid).getBytes());
    final Set<SignalDetectionEventAssociation> filteredAssociations =
        signalDetectionEventAssociations
            .stream().filter(x -> x.getSignalDetectionHypothesisId().equals(expectedArid))
            .collect(Collectors.toSet());
    assertEquals(1, filteredAssociations.size());
    final SignalDetectionEventAssociation associationArid59210193 = filteredAssociations.iterator()
        .next();
    assertEquals(event.getId(), associationArid59210193.getEventHypothesisId());
    assertEquals(expectedArid, associationArid59210193.getSignalDetectionHypothesisId());
    assertFalse(associationArid59210193.isRejected());

    final Collection<SignalDetection> detections = converter.getSignalDetections();
    assertNotNull(detections);
    assertEquals(139, detections.size());  // some dets not present in Arid2Wfid

    //Check particular Signal Detection
    final List<SignalDetection> detectionsWithArid59210196 = detections.stream()
        .filter(x -> x.getId().equals(expectedArid))
        .collect(Collectors.toList());
    assertEquals(1, detectionsWithArid59210196.size());
    final SignalDetection signalDetection = detectionsWithArid59210196.get(0);
    assertEquals(expectedArid, signalDetection.getId());

    // creation info ID, monitoringOrg match (see constants in converter)
    assertEquals(CssEventAndSignalDetectionConverter.MONITORING_ORG,
        signalDetection.getMonitoringOrganization());
    assertEquals("ARCES", signalDetection.getStationName());

    //test signal detection hypothesis
    assertEquals(1, signalDetection.getSignalDetectionHypotheses().size());
    SignalDetectionHypothesis signalDetectionHypothesis = signalDetection
        .getSignalDetectionHypotheses().get(0);
    assertEquals(expectedArid, signalDetectionHypothesis.getParentSignalDetectionId());
    assertFalse(signalDetectionHypothesis.isRejected());
    assertEquals(signalDetection.getId(), signalDetectionHypothesis.getParentSignalDetectionId());

    final Channel chan = channelForArid(arid);
    signalDetectionHypothesis.getFeatureMeasurements()
        .forEach(fm -> assertEquals(chan, fm.getChannel()));

    // Check Phase measurement
    final FeatureMeasurement<PhaseTypeMeasurementValue> phaseMeasurement =
        assertPhaseMeasurementPresentAndReturn(
            signalDetectionHypothesis);
    final PhaseTypeMeasurementValue phaseValue = phaseMeasurement.getMeasurementValue();
    assertEquals(PhaseType.Pn, phaseValue.getValue());
    assertEquals(-1.0, phaseValue.getConfidence(), TOLERANCE);

    // Check arrival time measurement
    final Instant expectedArrivalTime = Instant.parse("2010-05-20T20:24:44.750Z");
    final FeatureMeasurement<InstantValue> arrivalTimeMeasurement =
        assertArrivalTimePresentAndReturn(
            signalDetectionHypothesis, expectedArrivalTime);
    final InstantValue arrivalTime = arrivalTimeMeasurement.getMeasurementValue();
    assertEquals(expectedArrivalTime, arrivalTime.getValue());
    assertEquals((int) (0.266 * 1e9), arrivalTime.getStandardDeviation().getNano());

    // Check azimuth measurement
    final FeatureMeasurement<NumericMeasurementValue> azimuthMeasurement =
        assertNumericMeasurementPresentAndReturn(
            signalDetectionHypothesis, FeatureMeasurementTypes.RECEIVER_TO_SOURCE_AZIMUTH,
            expectedArrivalTime);
    final NumericMeasurementValue azimuth = azimuthMeasurement.getMeasurementValue();
    assertEquals(122.11, azimuth.getMeasurementValue().getValue(), TOLERANCE);
    assertEquals(1.88, azimuth.getMeasurementValue().getStandardDeviation(), TOLERANCE);
    assertEquals(Units.DEGREES, azimuth.getMeasurementValue().getUnits());

    // Check slowness measurement
    final FeatureMeasurement<NumericMeasurementValue> slownessMeasurement =
        assertNumericMeasurementPresentAndReturn(
            signalDetectionHypothesis, FeatureMeasurementTypes.SLOWNESS, expectedArrivalTime);
    final NumericMeasurementValue slowness = slownessMeasurement.getMeasurementValue();
    assertEquals(14.52, slowness.getMeasurementValue().getValue(), TOLERANCE);
    assertEquals(0.48, slowness.getMeasurementValue().getStandardDeviation(), TOLERANCE);
    assertEquals(Units.SECONDS_PER_DEGREE, slowness.getMeasurementValue().getUnits());

    // Check emergence angle measurement
    final FeatureMeasurement<NumericMeasurementValue> emergenceMeasurement =
        assertNumericMeasurementPresentAndReturn(
            signalDetectionHypothesis, FeatureMeasurementTypes.EMERGENCE_ANGLE,
            expectedArrivalTime);
    final NumericMeasurementValue emergence = emergenceMeasurement.getMeasurementValue();
    assertEquals(61.29, emergence.getMeasurementValue().getValue(), TOLERANCE);
    assertEquals(0.0, emergence.getMeasurementValue().getStandardDeviation(), TOLERANCE);
    assertEquals(Units.DEGREES, emergence.getMeasurementValue().getUnits());

    // Check rectilinearity measurement
    final FeatureMeasurement<NumericMeasurementValue> rectilinearityMeasurement =
        assertNumericMeasurementPresentAndReturn(
            signalDetectionHypothesis, FeatureMeasurementTypes.RECTILINEARITY, expectedArrivalTime);
    final NumericMeasurementValue rect = rectilinearityMeasurement.getMeasurementValue();
    assertEquals(0.887, rect.getMeasurementValue().getValue(), TOLERANCE);
    assertEquals(0.0, rect.getMeasurementValue().getStandardDeviation(), TOLERANCE);
    assertEquals(Units.UNITLESS, rect.getMeasurementValue().getUnits());

    // Check amplitude measurements
    final FeatureMeasurement<AmplitudeMeasurementValue> amplitudeMeasurement_a5_over_2 =
        assertAmplitudeMeasurementPresentAndReturn(signalDetectionHypothesis,
            FeatureMeasurementTypes.AMPLITUDE_A5_OVER_2,
            Instant.ofEpochMilli((long) (1000L * 1274387086.15)));
    final AmplitudeMeasurementValue amplitude1 = amplitudeMeasurement_a5_over_2
        .getMeasurementValue();
    assertEquals(0.31, amplitude1.getAmplitude().getValue(), TOLERANCE);
    assertEquals(0.0, amplitude1.getAmplitude().getStandardDeviation(), TOLERANCE);
    assertEquals(Units.UNITLESS, amplitude1.getAmplitude().getUnits());
    assertEquals(Duration.ofNanos((long) (0.36 * 1e9)), amplitude1.getPeriod());

    // Check period measurement
    final FeatureMeasurement<NumericMeasurementValue> periodMeasurement =
        assertNumericMeasurementPresentAndReturn(
            signalDetectionHypothesis, FeatureMeasurementTypes.PERIOD, expectedArrivalTime);
    final NumericMeasurementValue period = periodMeasurement.getMeasurementValue();
    assertEquals(0.22, period.getMeasurementValue().getValue(), TOLERANCE);
    assertEquals(0.0, period.getMeasurementValue().getStandardDeviation(), TOLERANCE);
    assertEquals(Units.SECONDS, period.getMeasurementValue().getUnits());

    // Check snr measurement
    final FeatureMeasurement<NumericMeasurementValue> snrMeasurement =
        assertNumericMeasurementPresentAndReturn(
            signalDetectionHypothesis, FeatureMeasurementTypes.SNR, expectedArrivalTime);
    final NumericMeasurementValue snr = snrMeasurement.getMeasurementValue();
    assertEquals(13.47, snr.getMeasurementValue().getValue(), TOLERANCE);
    assertEquals(0.0, snr.getMeasurementValue().getStandardDeviation(), TOLERANCE);
    assertEquals(Units.UNITLESS, snr.getMeasurementValue().getUnits());

    //Check Final Event Hypothesis
    assertTrue(event.getFinal().isPresent());
    assertEquals(eventHypothesis,
        event.getFinalEventHypothesisHistory().get(0).getEventHypothesis());

    //Check Preferred
    assertEquals(1, event.getPreferredEventHypothesisHistory().size());
    assertEquals(eventHypothesis,
        event.getPreferredEventHypothesisHistory().get(0).getEventHypothesis());

    //Test Location Behaviors
    final Set<LocationBehavior> behaviors = locationSolution.getLocationBehaviors();
    // each assoc makes 3 location behaviors: time, azimuth, and slowness.
    assertEquals(3 * signalDetectionEventAssociations.size(), behaviors.size());
    final double weight = 0.957;
    final PhaseType phase = phaseValue.getValue();

    // verify time location behavior
    final double timeRes = -0.872;
    final double predictedTimeMillis = arrivalTime.getValue().toEpochMilli() - (timeRes * 1000);
    final Instant predictedTime = Instant.ofEpochMilli((long) predictedTimeMillis);
    verifyLocationBehavior(locBehavior(locationSolution, arrivalTimeMeasurement),
        timeRes, true,
        InstantValue.from(predictedTime, Duration.ZERO),
        FeatureMeasurementTypes.ARRIVAL_TIME,
        weight, chan, eventLoc, phase, fpComponents(predictedTimeMillis / 1000.0, Units.SECONDS));

    // verify azimuth location behavior    
    final double azRes = -1.4;
    final double predictedAz = azimuth.getMeasurementValue().getValue() - azRes;
    final NumericMeasurementValue expectedAzPred = NumericMeasurementValue.from(
        arrivalTime.getValue(), DoubleValue.from(predictedAz, 0.0, Units.DEGREES));
    verifyLocationBehavior(locBehavior(locationSolution, azimuthMeasurement),
        azRes, true, expectedAzPred, FeatureMeasurementTypes.RECEIVER_TO_SOURCE_AZIMUTH,
        weight, chan, eventLoc, phase, fpComponents(predictedAz, Units.DEGREES));

    // verify slowness location behavior
    final double slowRes = 0.77;
    final double predictedSlow = slowness.getMeasurementValue().getValue() - slowRes;
    final NumericMeasurementValue expectedSlowPred = NumericMeasurementValue.from(
        arrivalTime.getValue(), DoubleValue.from(predictedSlow, 0.0, Units.SECONDS_PER_DEGREE));
    verifyLocationBehavior(locBehavior(locationSolution, slownessMeasurement),
        slowRes, false, expectedSlowPred, FeatureMeasurementTypes.SLOWNESS,
        weight, chan, eventLoc, phase, fpComponents(predictedSlow, Units.SECONDS_PER_DEGREE));
  }

  // returns a set of one component for the baseline prediction
  private static Set<FeaturePredictionComponent> fpComponents(double value, Units units) {
    return Set.of(FeaturePredictionComponent.from(
        DoubleValue.from(value, 0.0, units), false,
        FeaturePredictionCorrectionType.BASELINE_PREDICTION));
  }

  private static <T> void verifyLocationBehavior(
      LocationBehavior lb, double residual, boolean defining,
      T prediction, FeatureMeasurementType<T> predictionType,
      double weight, Channel chan, EventLocation eventLoc, PhaseType phase,
      Set<FeaturePredictionComponent> fpComponents) {
    assertEquals(residual, lb.getResidual(), TOLERANCE);
    assertEquals(weight, lb.getWeight(), TOLERANCE);
    assertEquals(defining, lb.isDefining());
    final FeaturePrediction<T> expectedPred = FeaturePrediction.<T>builder()
        .setPhase(phase)
        .setPredictedValue(prediction)
        .setFeaturePredictionComponents(fpComponents)
        .setExtrapolated(false)
        .setPredictionType(predictionType)
        .setSourceLocation(eventLoc)
        .setReceiverLocation(chan.getLocation())
        .setChannelName(chan.getName())
        .setFeaturePredictionDerivativeMap(Map.of())
        .build();
    assertEquals(expectedPred, lb.getFeaturePrediction());
  }

  //   This test could have been embedded into the testEventConversion test, but it would have
//   required rewriting that whole test because the orid tested in that test does not have
//   associated magnitude information.
  // TODO (vickers) disabled because there are various problems with N/A values in mags.
  // There are quite a few TODOs in the CssEventAndSignalDetectionConverter that are
  // skipping CSS data while we await resolution on some data model and DAO related issues.
  // When those are resolved, these tests will need to be updated because not quite so much
  // data should be getting dropped after that resolution.
  @Disabled("Skipping CSS Data because of data model")
  @Test
  void testMagnitudeConversion() {
    //Check overall Event's are of expected size
    final Collection<Event> events = converter.getEvents();

    //Check particular Event
    final UUID expectedOrid = UUID.nameUUIDFromBytes("6356739".getBytes());
    final Event event = events.stream()
        .filter(x -> x.getId().equals(expectedOrid))
        .collect(Collectors.toList())
        .get(0);
    final EventHypothesis eventHypothesis = event.getHypotheses().iterator().next();
    final LocationSolution locationSolution = eventHypothesis.getLocationSolutions()
        .iterator().next();
    final List<NetworkMagnitudeSolution> networkMagnitudeSolutions =
        locationSolution.getNetworkMagnitudeSolutions();

    assertEquals(1, networkMagnitudeSolutions.size());
    assertEquals(18, networkMagnitudeSolutions.get(0).getNetworkMagnitudeBehaviors().size());

    //NetworkMagnitudeSolution
    NetworkMagnitudeSolution networkMagnitudeSolution = networkMagnitudeSolutions.get(0);
    assertEquals(MagnitudeType.MBMLE, networkMagnitudeSolution.getMagnitudeType());
    assertEquals(3.3, networkMagnitudeSolution.getMagnitude(), TOLERANCE);
    assertEquals(0.1, networkMagnitudeSolution.getUncertainty(), TOLERANCE);

    //Check out some NetworkMagnitudeBehaviors
    List<NetworkMagnitudeBehavior> networkMagnitudeBehaviors =
        networkMagnitudeSolution.getNetworkMagnitudeBehaviors();
    assertEquals(18, networkMagnitudeBehaviors.size());

    NetworkMagnitudeBehavior networkMagnitudeBehavior = networkMagnitudeBehaviors.get(0);
    assertTrue(networkMagnitudeBehavior.isDefining());
    assertEquals(0.5, networkMagnitudeBehavior.getResidual(), TOLERANCE);
    assertEquals(0.0, networkMagnitudeBehavior.getWeight(), TOLERANCE);

    StationMagnitudeSolution stationMagnitudeSolution = networkMagnitudeBehavior
        .getStationMagnitudeSolution();
    assertEquals(MagnitudeType.MBMLE, stationMagnitudeSolution.getType());
    assertEquals(MagnitudeModel.VEITH_CLAWSON, stationMagnitudeSolution.getModel());
    assertEquals(PhaseType.P, stationMagnitudeSolution.getPhase());
    assertEquals(3.79, stationMagnitudeSolution.getMagnitude(), TOLERANCE);
    assertTrue(stationMagnitudeSolution.getMagnitudeUncertainty() == 0.28 ||
        stationMagnitudeSolution.getMagnitudeUncertainty() == 0.34);
    assertEquals(0.0, stationMagnitudeSolution.getModelCorrection(), TOLERANCE);
    assertEquals(0.0, stationMagnitudeSolution.getStationCorrection(), TOLERANCE);

    FeatureMeasurement featureMeasurement = stationMagnitudeSolution.getMeasurement();
    assertEquals(FeatureMeasurementTypes.AMPLITUDE_A5_OVER_2_OR,
        featureMeasurement.getFeatureMeasurementType());

    AmplitudeMeasurementValue amplitudeMeasurementValue =
        (AmplitudeMeasurementValue) featureMeasurement
            .getMeasurementValue();
    assertEquals(0.62, amplitudeMeasurementValue.getAmplitude().getValue(), TOLERANCE);
    assertEquals(Units.DEGREES, amplitudeMeasurementValue.getAmplitude().getUnits());
    assertEquals(0.0, amplitudeMeasurementValue.getAmplitude().getStandardDeviation(), TOLERANCE);
  }

  private static FeatureMeasurement<PhaseTypeMeasurementValue> assertPhaseMeasurementPresentAndReturn(
      SignalDetectionHypothesis hyp) {

    Optional<FeatureMeasurement<PhaseTypeMeasurementValue>> phaseMeasurement
        = hyp.getFeatureMeasurement(FeatureMeasurementTypes.PHASE);
    assertNotNull(phaseMeasurement);
    assertTrue(phaseMeasurement.isPresent());
    return phaseMeasurement.get();
  }

  private static FeatureMeasurement<AmplitudeMeasurementValue> assertAmplitudeMeasurementPresentAndReturn(
      SignalDetectionHypothesis hyp, FeatureMeasurementType<AmplitudeMeasurementValue> fmType,
      Instant expectedTime) {

    Optional<FeatureMeasurement<AmplitudeMeasurementValue>> measurement = hyp
        .getFeatureMeasurement(fmType);
    assertNotNull(measurement);
    assertTrue(measurement.isPresent());
    FeatureMeasurement<AmplitudeMeasurementValue> amplitudeMeasurement = measurement.get();
    assertEquals(expectedTime, amplitudeMeasurement.getMeasurementValue().getStartTime());
    return amplitudeMeasurement;
  }

  private static FeatureMeasurement<NumericMeasurementValue> assertNumericMeasurementPresentAndReturn(
      SignalDetectionHypothesis hyp, FeatureMeasurementType<NumericMeasurementValue> fmType,
      Instant expectedTime) {

    Optional<FeatureMeasurement<NumericMeasurementValue>> measurement = hyp
        .getFeatureMeasurement(fmType);
    assertNotNull(measurement);
    assertTrue(measurement.isPresent());
    FeatureMeasurement<NumericMeasurementValue> numericalMeasurement = measurement.get();
    assertEquals(expectedTime, numericalMeasurement.getMeasurementValue().getReferenceTime());
    return numericalMeasurement;
  }

  private static FeatureMeasurement<InstantValue> assertArrivalTimePresentAndReturn(
      SignalDetectionHypothesis hyp, Instant expectedTime) {

    Optional<FeatureMeasurement<InstantValue>> measurement = hyp.getFeatureMeasurement(
        FeatureMeasurementTypes.ARRIVAL_TIME);
    assertNotNull(measurement);
    assertTrue(measurement.isPresent());
    FeatureMeasurement<InstantValue> instantMeasurement = measurement.get();
    assertEquals(expectedTime, instantMeasurement.getMeasurementValue().getValue());
    return instantMeasurement;
  }

  private static LocationBehavior locBehavior(LocationSolution solution, FeatureMeasurement fm) {
    final Optional<LocationBehavior> locationBehaviorOptional = solution.getLocationBehaviors()
        .stream()
        .filter(lb -> lb.getFeatureMeasurement().equals(fm))
        .findAny();
    assertNotNull(locationBehaviorOptional);
    assertTrue(locationBehaviorOptional.isPresent());
    return locationBehaviorOptional.get();
  }

  private static Channel channelForArid(int arid) {
    // lookup channel for this arid
    assertTrue(aridToWfid.containsKey(arid), "Expected to find wfid mapping for arid " + arid);
    final long wfid = aridToWfid.get(arid);
    assertTrue(wfidToChannel.containsKey(wfid),
        "Expected to find Channel mapping for wfid " + wfid);
    return wfidToChannel.get(wfid);
  }
}
