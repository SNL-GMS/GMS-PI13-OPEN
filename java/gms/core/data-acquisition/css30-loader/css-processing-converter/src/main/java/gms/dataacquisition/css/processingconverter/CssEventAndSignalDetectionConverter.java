package gms.dataacquisition.css.processingconverter;

import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableCollection;
import static java.util.Collections.unmodifiableMap;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import gms.dataacquisition.css.processingconverter.commandline.CssEventAndSignalDetectionConverterArguments;
import gms.dataacquisition.cssreader.data.AmplitudeRecord;
import gms.dataacquisition.cssreader.data.AmplitudeRecordP3;
import gms.dataacquisition.cssreader.data.ArrivalRecord;
import gms.dataacquisition.cssreader.data.ArrivalRecordP3;
import gms.dataacquisition.cssreader.data.AssocRecord;
import gms.dataacquisition.cssreader.data.AssocRecordP3;
import gms.dataacquisition.cssreader.data.EventRecord;
import gms.dataacquisition.cssreader.data.EventRecordP3;
import gms.dataacquisition.cssreader.data.NetmagRecord;
import gms.dataacquisition.cssreader.data.NetmagRecordP3;
import gms.dataacquisition.cssreader.data.OrigErrRecord;
import gms.dataacquisition.cssreader.data.OrigErrRecordP3;
import gms.dataacquisition.cssreader.data.OriginRecord;
import gms.dataacquisition.cssreader.data.OriginRecordP3;
import gms.dataacquisition.cssreader.data.StamagRecord;
import gms.dataacquisition.cssreader.data.StamagRecordP3;
import gms.dataacquisition.cssreader.data.WfdiscRecord;
import gms.dataacquisition.cssreader.data.WfdiscRecord32;
import gms.dataacquisition.cssreader.flatfilereaders.GenericFlatFileReader;
import gms.shared.frameworks.osd.coi.DoubleValue;
import gms.shared.frameworks.osd.coi.InstantValue;
import gms.shared.frameworks.osd.coi.PhaseType;
import gms.shared.frameworks.osd.coi.Units;
import gms.shared.frameworks.osd.coi.channel.Channel;
import gms.shared.frameworks.osd.coi.datatransferobjects.CoiObjectMapperFactory;
import gms.shared.frameworks.osd.coi.event.Ellipse;
import gms.shared.frameworks.osd.coi.event.Event;
import gms.shared.frameworks.osd.coi.event.EventHypothesis;
import gms.shared.frameworks.osd.coi.event.EventLocation;
import gms.shared.frameworks.osd.coi.event.FeaturePrediction;
import gms.shared.frameworks.osd.coi.event.FeaturePredictionComponent;
import gms.shared.frameworks.osd.coi.event.FeaturePredictionCorrectionType;
import gms.shared.frameworks.osd.coi.event.FinalEventHypothesis;
import gms.shared.frameworks.osd.coi.event.LocationBehavior;
import gms.shared.frameworks.osd.coi.event.LocationRestraint;
import gms.shared.frameworks.osd.coi.event.LocationSolution;
import gms.shared.frameworks.osd.coi.event.LocationUncertainty;
import gms.shared.frameworks.osd.coi.event.MagnitudeType;
import gms.shared.frameworks.osd.coi.event.NetworkMagnitudeBehavior;
import gms.shared.frameworks.osd.coi.event.NetworkMagnitudeSolution;
import gms.shared.frameworks.osd.coi.event.PreferredEventHypothesis;
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
import gms.shared.frameworks.osd.coi.signaldetection.MeasuredChannelSegmentDescriptor;
import gms.shared.frameworks.osd.coi.signaldetection.NumericMeasurementValue;
import gms.shared.frameworks.osd.coi.signaldetection.SignalDetection;
import gms.shared.frameworks.osd.coi.signaldetection.SignalDetectionHypothesis;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CssEventAndSignalDetectionConverter {

  private static final Logger logger = LoggerFactory.getLogger(
      CssEventAndSignalDetectionConverter.class);

  // These are the lists returned from reading the CSS flat files.
  private final Collection<EventRecord> eventRecords;
  private final Map<Integer, OriginRecord> originRecordsByOrid;
  private final Map<Integer, OrigErrRecord> origerrRecordsByOrid;
  private final ListMultimap<Integer, AssocRecord> assocRecordsByOrid;
  private final ListMultimap<Integer, AmplitudeRecord> amplitudeRecordsByArid;
  private final Map<Integer, AmplitudeRecord> amplitudeRecordsByAmpid;
  private final ListMultimap<Integer, NetmagRecord> netmagRecordsByOrid;
  private final ListMultimap<Integer, StamagRecord> stamagRecordsByMagid;
  private final Map<Integer, ArrivalRecord> arrivalRecordsByArid;
  private final Map<Integer, Long> aridToWfid;
  private final Map<Long, WfdiscRecord> wfdiscsByWfid;
  private final Map<Long, Channel> wfidToChannel;

  private final Set<Integer> aridsWithNoWfid = new HashSet<>();
  private final Set<Long> wfidsWithNoChannel = new HashSet<>();
  private final Set<Long> wfidsWithNoWfdisc = new HashSet<>();

  private final Collection<Event> events = new ArrayList<>();
  private final Collection<SignalDetection> signalDetections = new ArrayList<>();
  public static final UUID UNKNOWN_ID = UUID
      .fromString("00000000-0000-0000-0000-000000000000");
  public static final String MONITORING_ORG = "CTBTO";
  private static final Map<String, FeatureMeasurementType<AmplitudeMeasurementValue>> AMPTYPE_TO_AMP_FM_TYPE = Map
      .of(
          "A5/2", FeatureMeasurementTypes.AMPLITUDE_A5_OVER_2,
          "ANL/2", FeatureMeasurementTypes.AMPLITUDE_ANL_OVER_2,
          "ALR/2", FeatureMeasurementTypes.AMPLITUDE_ALR_OVER_2,
          "A5/2-OR", FeatureMeasurementTypes.AMPLITUDE_A5_OVER_2_OR,
          "ANP/2", FeatureMeasurementTypes.AMPLITUDE_ANP_OVER_2,
          "FKSNR", FeatureMeasurementTypes.AMPLITUDE_FKSNR,
          "LRM0", FeatureMeasurementTypes.AMPTLIUDE_LRM0,
          "noiLRM0", FeatureMeasurementTypes.AMPLITUDE_NOI_LRM0,
          "RMSAMP", FeatureMeasurementTypes.AMPLITUDE_RMSAMP,
          "SBSNR", FeatureMeasurementTypes.AMPLITUDE_SBSNR);

  private static final Map<String, Units> CSS_AMP_UNITS_TO_UNITS_ENUM = Map.of(
      "nm", Units.NANOMETERS, "nm/s", Units.NANOMETERS_PER_SECOND);

  /**
   * Testing only constructor, when there is no JSON to pull into the params.
   */
  CssEventAndSignalDetectionConverter(String[] files) throws IOException {
    this.aridToWfid = unmodifiableMap(AridToWfidJsonReader.read(files[9]));
    this.wfdiscsByWfid = unmodifiableMap(
        toMapByWfid(GenericFlatFileReader.read(files[8], WfdiscRecord32.class)));
    this.wfidToChannel = unmodifiableMap(readWfidToChanFile(files[10]));
    this.eventRecords = Objects
        .requireNonNull(GenericFlatFileReader.read(files[0], EventRecordP3.class));
    this.originRecordsByOrid = Objects
        .requireNonNull(GenericFlatFileReader.read(files[1], OriginRecordP3.class))
        .stream().collect(Collectors.toMap(OriginRecord::getOrid, Function.identity()));
    this.origerrRecordsByOrid = Objects
        .requireNonNull(GenericFlatFileReader.read(files[2], OrigErrRecordP3.class))
        .stream().collect(Collectors.toMap(OrigErrRecord::getOriginId, Function.identity()));
    this.arrivalRecordsByArid = Objects
        .requireNonNull(GenericFlatFileReader.read(files[3], ArrivalRecordP3.class))
        .stream().collect(Collectors.toMap(ArrivalRecord::getArid, Function.identity()));
    Collection<AssocRecord> assocRecords = GenericFlatFileReader
        .read(files[4], AssocRecordP3.class);
    Objects.requireNonNull(assocRecords);
    this.assocRecordsByOrid = ArrayListMultimap.create();
    for (AssocRecord assoc : assocRecords) {
      this.assocRecordsByOrid.put(assoc.getOriginId(), assoc);
    }
    Collection<AmplitudeRecord> amplitudeRecords = GenericFlatFileReader
        .read(files[5], AmplitudeRecordP3.class);
    Objects.requireNonNull(amplitudeRecords);
    this.amplitudeRecordsByArid = ArrayListMultimap.create();
    this.amplitudeRecordsByAmpid = new HashMap<>();
    for (AmplitudeRecord amp : amplitudeRecords) {
      this.amplitudeRecordsByArid.put(amp.getArid(), amp);
      this.amplitudeRecordsByAmpid.put(amp.getAmpid(), amp);
    }
    Collection<NetmagRecord> netmagRecords = GenericFlatFileReader
        .read(files[6], NetmagRecordP3.class);
    Objects.requireNonNull(netmagRecords);
    this.netmagRecordsByOrid = ArrayListMultimap.create();
    for (NetmagRecord netmag : netmagRecords) {
      this.netmagRecordsByOrid.put(netmag.getOrid(), netmag);
    }
    Collection<StamagRecord> stamagRecords = GenericFlatFileReader
        .read(files[7], StamagRecordP3.class);
    Objects.requireNonNull(stamagRecords);
    this.stamagRecordsByMagid = ArrayListMultimap.create();
    for (StamagRecord stamag : stamagRecords) {
      this.stamagRecordsByMagid.put(stamag.getMagid(), stamag);
    }
    this.doConversion();
  }

  CssEventAndSignalDetectionConverter(CssEventAndSignalDetectionConverterArguments args)
      throws IOException {
    try {
      this.aridToWfid = unmodifiableMap(AridToWfidJsonReader.read(args.getAridToWfidFile()));
      this.wfdiscsByWfid = unmodifiableMap(
          toMapByWfid(GenericFlatFileReader.read(args.getWfdiscFile(), WfdiscRecord32.class)));
      this.wfidToChannel = unmodifiableMap(readWfidToChanFile(args.getWfidToChannelFile()));
      this.eventRecords = Objects
          .requireNonNull(GenericFlatFileReader.read(args.getEventFile(), EventRecordP3.class));
      this.originRecordsByOrid = Objects
          .requireNonNull(GenericFlatFileReader.read(args.getOriginFile(), OriginRecordP3.class))
          .stream().collect(Collectors.toMap(OriginRecord::getOrid, Function.identity()));
      this.origerrRecordsByOrid = Objects
          .requireNonNull(GenericFlatFileReader.read(args.getOrigerrFile(), OrigErrRecordP3.class))
          .stream().collect(Collectors.toMap(OrigErrRecord::getOriginId, Function.identity()));
      this.arrivalRecordsByArid = Objects
          .requireNonNull(GenericFlatFileReader.read(args.getArrivalFile(), ArrivalRecordP3.class))
          .stream().collect(Collectors.toMap(ArrivalRecord::getArid, Function.identity()));
      Collection<AssocRecord> assocRecords = GenericFlatFileReader
          .read(args.getAssocFile(), AssocRecordP3.class);
      Objects.requireNonNull(assocRecords);
      this.assocRecordsByOrid = ArrayListMultimap.create();
      for (AssocRecord assoc : assocRecords) {
        this.assocRecordsByOrid.put(assoc.getOriginId(), assoc);
      }
      Collection<AmplitudeRecord> amplitudeRecords = GenericFlatFileReader
          .read(args.getAmplitudeFile(), AmplitudeRecordP3.class);
      Objects.requireNonNull(amplitudeRecords);
      this.amplitudeRecordsByArid = ArrayListMultimap.create();
      this.amplitudeRecordsByAmpid = new HashMap<>();
      for (AmplitudeRecord amp : amplitudeRecords) {
        this.amplitudeRecordsByArid.put(amp.getArid(), amp);
        this.amplitudeRecordsByAmpid.put(amp.getAmpid(), amp);
      }
      Collection<NetmagRecord> netmagRecords = GenericFlatFileReader
          .read(args.getNetmagFile(), NetmagRecordP3.class);
      Objects.requireNonNull(netmagRecords);
      this.netmagRecordsByOrid = ArrayListMultimap.create();
      for (NetmagRecord netmag : netmagRecords) {
        this.netmagRecordsByOrid.put(netmag.getOrid(), netmag);
      }
      Collection<StamagRecord> stamagRecords = GenericFlatFileReader
          .read(args.getStamagFile(), StamagRecordP3.class);
      Objects.requireNonNull(stamagRecords);
      this.stamagRecordsByMagid = ArrayListMultimap.create();
      for (StamagRecord stamag : stamagRecords) {
        this.stamagRecordsByMagid.put(stamag.getMagid(), stamag);
      }
      this.doConversion();
    } catch (Exception e) {
      throw new IOException(e.getMessage());
    }
  }

  public Collection<Event> getEvents() {
    return unmodifiableCollection(this.events);
  }

  public Collection<SignalDetection> getSignalDetections() {
    return unmodifiableCollection(this.signalDetections);
  }

  private void doConversion() {
    // data structures for keeping track of problematic data to log errors about
    final Set<Integer> missingOrids = new HashSet<>();
    final Set<Integer> oridsWithNoAssocs = new HashSet<>();
    final Set<AssocRecord> assocRecordsWithNoMatchingArrivals = new HashSet<>();
    final Set<Integer> aridsWithNoAmplitude = new HashSet<>();

    for (EventRecord rec : eventRecords) {
      final int evid = rec.getEventId();
      final int orid = rec.getOriginId();

      //if present, one row in the origError always produce 1 locationUncertainty with 1 ellipse
      final LocationUncertainty locationUncertainty = createUncertaintyForOrid(orid);

      if (!this.originRecordsByOrid.containsKey(orid)) {
        missingOrids.add(orid);
        continue;
      }

      final OriginRecord associatedOrigin = this.originRecordsByOrid.get(orid);
      final LocationRestraint locationRestraint = LocationRestraint.from(
          RestraintType.UNRESTRAINED,
          null,
          RestraintType.UNRESTRAINED,
          null,
          associatedOrigin.getDtype(),
          null,
          RestraintType.UNRESTRAINED,
          null);

      final EventLocation eventLocation = EventLocation.from(
          associatedOrigin.getLat(), associatedOrigin.getLon(), associatedOrigin.getDepth(),
          associatedOrigin.getTime());

      final UUID eventHypothesisId = UUID.nameUUIDFromBytes(String.valueOf(orid).getBytes());
      final Set<SignalDetectionEventAssociation> signalDetectionEventAssociations = new HashSet<>();
      final Set<LocationBehavior> locationBehaviors = new HashSet<>();

      final List<AssocRecord> assocs = assocRecordsByOrid.get(orid);

      if (assocs.isEmpty()) {
        oridsWithNoAssocs.add(orid);
        continue;
      }

      //Each AssocRecord Row produces:
      // - 3 LocationBehaviors corresponding to time/azimuth/slowness
      // - 1 SignalDetectionEventAssociation
      // - 1 SignalDetection that has one SignalDetectionHypothesis and a lot of feature
      // measurements
      for (AssocRecord assocRecord : assocs) {
        final int arid = assocRecord.getArrivalId();
        //Get arrival info. Each assoc has both an orid and arid. For each orid find associated
        // arids
        if (!this.arrivalRecordsByArid.containsKey(arid)) {
          assocRecordsWithNoMatchingArrivals.add(assocRecord);
          continue;
        }
        if (!this.amplitudeRecordsByArid.containsKey(arid)) {
          aridsWithNoAmplitude.add(arid);
        }
        // Read location behaviors from the assoc record, traverse and read signal detection from
        // arrival
        final ArrivalRecord arrival = this.arrivalRecordsByArid.get(arid);
        Optional<Pair<SignalDetection, Set<LocationBehavior>>> detectionAndBehaviors
            = readDetectionAndLocationBehaviors(assocRecord, arrival,
            amplitudeRecordsByArid.get(arid));
        if (detectionAndBehaviors.isPresent()) {
          locationBehaviors.addAll(detectionAndBehaviors.get().getRight());
          final SignalDetection det = detectionAndBehaviors.get().getLeft();
          this.signalDetections.add(det);
          // create association between signal detection and event
          signalDetectionEventAssociations.add(SignalDetectionEventAssociation.create(
              eventHypothesisId, det.getId()));
        }
      }

      final LocationSolution locationSolution = LocationSolution.builder()
          .setId(UUID.nameUUIDFromBytes(String.valueOf(orid).getBytes()))
          .setLocation(eventLocation)
          .setLocationRestraint(locationRestraint)
          .setLocationUncertainty(Optional.ofNullable(locationUncertainty))
          .setLocationBehaviors(locationBehaviors)
          .setFeaturePredictions(emptySet())
          .setNetworkMagnitudeSolutions(createNetworkMagnitudeSolutions(orid))
          .build();

      final EventHypothesis eventHypothesis = EventHypothesis.from(
          UUID.nameUUIDFromBytes(String.valueOf(evid).getBytes()),
          eventHypothesisId,
          Set.of(),  // no parent hypothesis (CSS has no concept of event hypothesis)
          false, Set.of(locationSolution),
          PreferredLocationSolution.from(locationSolution), signalDetectionEventAssociations);

      final Event event = Event.from(
          UUID.nameUUIDFromBytes(String.valueOf(evid).getBytes()),
          Set.of(), MONITORING_ORG, Set.of(eventHypothesis),
          List.of(FinalEventHypothesis.from(eventHypothesis)),
          List.of(PreferredEventHypothesis.from(UNKNOWN_ID, eventHypothesis)));

      this.events.add(event);
    }
    logErrorIfNotEmpty(missingOrids, "Couldn't find Origin's with orid's");
    logErrorIfNotEmpty(oridsWithNoAssocs, "No assocs found for orid's");
    logErrorIfNotEmpty(assocRecordsWithNoMatchingArrivals,
        "No arrival records found corresponding to assoc record's");
    logErrorIfNotEmpty(aridsWithNoWfid, "No mapping to wfid found for arids");
    logErrorIfNotEmpty(wfidsWithNoChannel, "No mapping to channel found for wfids");
    logErrorIfNotEmpty(wfidsWithNoWfdisc, "No wfdisc found for wfids");
    logErrorIfNotEmpty(aridsWithNoAmplitude, "No mapping to amplitude found for arids");
  }


  // Note: this method returns null if there is no origerr for the provided orid.
  // this is allowable and LocationSolution can take LocationUncertainty as null.
  private LocationUncertainty createUncertaintyForOrid(int orid) {
    if (!this.origerrRecordsByOrid.containsKey(orid)) {
      return null;  // no associated origin error
    }

    final OrigErrRecord origErr = this.origerrRecordsByOrid.get(orid);
    final Ellipse ellipse = Ellipse.from(ScalingFactorType.CONFIDENCE, 0.0, origErr.getConf(),
        origErr.getSmajax(), origErr.getStrike(), origErr.getSminax(), -1.0,
        origErr.getSdepth(), Duration.ofNanos((long) (origErr.getStime() * 1e9)));

    return LocationUncertainty.from(
        origErr.getSxx(), origErr.getSxy(), origErr.getSxz(), origErr.getStx(),
        origErr.getSyy(), origErr.getSyz(), origErr.getSty(), origErr.getSzz(),
        origErr.getStz(), origErr.getStt(), origErr.getSdobs(), Set.of(ellipse),
        Set.of()); //empty set for ellipsoids, don't get this data from css
  }

  private Optional<Pair<SignalDetection, Set<LocationBehavior>>> readDetectionAndLocationBehaviors(
      AssocRecord assoc, ArrivalRecord arrival, List<AmplitudeRecord> amplitudes) {
    final Optional<Pair<Channel, MeasuredChannelSegmentDescriptor>> channelInfo
        = channelInfoForArid(arrival.getArid());
    if (!channelInfo.isPresent()) {
      return Optional.empty();
    }
    final Channel chan = channelInfo.get().getLeft();
    final MeasuredChannelSegmentDescriptor segmentDesc = channelInfo.get().getRight();
    final Instant time = arrival.getTime();

    final FeatureMeasurement<InstantValue> arrivalMeasurement
        = FeatureMeasurement.from(chan, segmentDesc, FeatureMeasurementTypes.ARRIVAL_TIME,
        InstantValue.from(time, Duration.ofNanos((long) (arrival.getDeltim() * 1e9))));

    final FeatureMeasurement<NumericMeasurementValue> azimuthMeasurement
        = FeatureMeasurement
        .from(chan, segmentDesc, FeatureMeasurementTypes.RECEIVER_TO_SOURCE_AZIMUTH,
            NumericMeasurementValue
                .from(time, DoubleValue.from(arrival.getAzimuth(), arrival.getDelaz(),
                    Units.DEGREES)));

    final FeatureMeasurement<NumericMeasurementValue> emergenceAngleMeasurement
        = FeatureMeasurement.from(chan, segmentDesc, FeatureMeasurementTypes.EMERGENCE_ANGLE,
        // CSS does not have uncertainty for emergence angle
        NumericMeasurementValue.from(time, DoubleValue.from(arrival.getEma(), 0.0,
            Units.DEGREES)));

    final FeatureMeasurement<NumericMeasurementValue> periodMeasurement
        = FeatureMeasurement.from(chan, segmentDesc, FeatureMeasurementTypes.PERIOD,
        // CSS does not have uncertainty for period
        NumericMeasurementValue.from(time, DoubleValue.from(arrival.getPer(), 0.0,
            Units.SECONDS)));

    final FeatureMeasurement<NumericMeasurementValue> rectilinearityMeasurement
        = FeatureMeasurement.from(chan, segmentDesc, FeatureMeasurementTypes.RECTILINEARITY,
        // CSS does not have uncertainty for rectilinearity
        NumericMeasurementValue.from(time, DoubleValue.from(arrival.getRect(), 0.0,
            Units.UNITLESS)));

    final FeatureMeasurement<NumericMeasurementValue> slownessMeasurement
        = FeatureMeasurement.from(chan, segmentDesc, FeatureMeasurementTypes.SLOWNESS,
        NumericMeasurementValue.from(time, DoubleValue.from(arrival.getSlow(), arrival.getDelslo(),
            Units.SECONDS_PER_DEGREE)));

    final FeatureMeasurement<NumericMeasurementValue> snrMeasurement
        = FeatureMeasurement.from(chan, segmentDesc, FeatureMeasurementTypes.SNR,
        // CSS does not have uncertainty for SNR
        NumericMeasurementValue.from(time, DoubleValue.from(arrival.getSnr(), 0.0,
            Units.UNITLESS)));

    PhaseType phaseType = PhaseType.UNKNOWN;
    try {
      phaseType = PhaseType.valueOf(assoc.getPhase());
    } catch (Exception e) {
      logger.error("Phasetype {} not recognized", assoc.getPhase());
    }
    final FeatureMeasurement<PhaseTypeMeasurementValue> phaseMeasurement
        = FeatureMeasurement.from(
        chan, segmentDesc, FeatureMeasurementTypes.PHASE,
        PhaseTypeMeasurementValue.from(phaseType, assoc.getBelief()));

    final List<FeatureMeasurement<?>> measurements = new ArrayList<>(
        List.of(arrivalMeasurement, azimuthMeasurement, emergenceAngleMeasurement,
            periodMeasurement, rectilinearityMeasurement, slownessMeasurement,
            snrMeasurement, phaseMeasurement));

    for (AmplitudeRecord amplitude : amplitudes) {
      if (AMPTYPE_TO_AMP_FM_TYPE.containsKey(amplitude.getAmptype())) {
        measurements.add(FeatureMeasurement.from(chan, segmentDesc,
            AMPTYPE_TO_AMP_FM_TYPE.get(amplitude.getAmptype()),
            AmplitudeMeasurementValue.from(amplitude.getAmptime(), amplitude.getPer(),
                DoubleValue.from(amplitude.getAmp(), 0.0,
                    getAmplitudeUnits(amplitude)))));
      }
    }
    final UUID signalDetectionId = UUID.nameUUIDFromBytes(
        String.valueOf(arrival.getArid()).getBytes());
    final SignalDetectionHypothesis signalDetectionHypothesis
        = SignalDetectionHypothesis.from(signalDetectionId, signalDetectionId,
        MONITORING_ORG, arrival.getSta(), null, // no parent hypothesis
        false, measurements);

    final SignalDetection detection = SignalDetection.from(signalDetectionId, MONITORING_ORG,
        arrival.getSta(), List.of(signalDetectionHypothesis));

    final Set<LocationBehavior> locBehaviors;
    final OriginRecord associatedOrigin = this.originRecordsByOrid.get(assoc.getOriginId());
    if (associatedOrigin == null) {
      logger.warn("Could not find origin {} associated with arid {}", assoc.getOriginId(),
          assoc.getArrivalId());
      locBehaviors = Set.of();
    } else {
      final EventLocation eventLoc = location(associatedOrigin);

      //Time behavior
      final double timeRes = assoc.getTimeres();
      final double predictedTimeMillis = time.toEpochMilli() - (timeRes * 1000);
      final Instant predictedTime = Instant.ofEpochMilli((long) predictedTimeMillis);
      final FeaturePrediction<InstantValue> arrivalTimePred = featurePred(
          fpComponents(predictedTimeMillis / 1000, Units.SECONDS),
          InstantValue.from(predictedTime, Duration.ZERO),
          FeatureMeasurementTypes.ARRIVAL_TIME, phaseType, eventLoc, chan);
      final LocationBehavior timeLocBehavior = LocationBehavior.from(
          timeRes, assoc.getWgt(), assoc.getTimedef(),
          arrivalTimePred, arrivalMeasurement);

      //Azimuth behavior
      final double azRes = assoc.getAzres();
      final double predictedAz = arrival.getAzimuth() - azRes;
      final FeaturePrediction<NumericMeasurementValue> azPred = featurePred(
          fpComponents(predictedAz, Units.DEGREES),
          NumericMeasurementValue.from(time, DoubleValue.from(predictedAz, 0.0, Units.DEGREES)),
          FeatureMeasurementTypes.RECEIVER_TO_SOURCE_AZIMUTH, phaseType, eventLoc, chan);
      final LocationBehavior azLocBehavior = LocationBehavior.from(
          azRes, assoc.getWgt(), assoc.getAzdef(),
          azPred, azimuthMeasurement);

      //Slowness behavior
      final double slowRes = assoc.getSlores();
      final double predictedSlow = arrival.getSlow() - slowRes;
      final FeaturePrediction<NumericMeasurementValue> slowPred = featurePred(
          fpComponents(predictedSlow, Units.SECONDS_PER_DEGREE),
          NumericMeasurementValue.from(time,
              DoubleValue.from(predictedSlow, 0.0, Units.SECONDS_PER_DEGREE)),
          FeatureMeasurementTypes.SLOWNESS, phaseType, eventLoc, chan);
      final LocationBehavior slowLocBehavior = LocationBehavior.from(
          slowRes, assoc.getWgt(), assoc.getSlodef(),
          slowPred, slownessMeasurement);

      locBehaviors = Set.of(timeLocBehavior, azLocBehavior, slowLocBehavior);
    }
    return Optional.of(Pair.of(detection, locBehaviors));
  }

  // returns a set of one component for the baseline prediction
  private static Set<FeaturePredictionComponent> fpComponents(double value, Units units) {
    return Set.of(FeaturePredictionComponent.from(
        DoubleValue.from(value, 0.0, units), false,
        FeaturePredictionCorrectionType.BASELINE_PREDICTION));
  }

  private static <T> FeaturePrediction<T> featurePred(Set<FeaturePredictionComponent> components,
      T predictedValue, FeatureMeasurementType<T> type, PhaseType phase, EventLocation eventLoc,
      Channel chan) {
    return FeaturePrediction.<T>builder()
        .setPhase(phase)
        .setPredictedValue(predictedValue)
        .setFeaturePredictionComponents(components)
        .setExtrapolated(false)
        .setPredictionType(type)
        .setSourceLocation(eventLoc)
        .setReceiverLocation(chan.getLocation())
        .setChannelName(chan.getName())
        .build();
  }

  private static void logErrorIfNotEmpty(Collection<?> c, String msg) {
    if (!c.isEmpty()) {
      logger.error("{} : {}", msg, c);
    }
  }

  private static Units getAmplitudeUnits(AmplitudeRecord record) {
    return Optional.ofNullable(CSS_AMP_UNITS_TO_UNITS_ENUM.get(record.getUnits()))
        .orElse(Units.UNITLESS);
  }

  /**
   * Given an orid, find the related netmag and stamag information and create the
   * NetworkMagnitudeSolution, NetworkMagnitudeBehavior, and StationMagnitudeSolution objects to
   * associate with that event's location solution
   *
   * @param orid origin for the event being read
   * @return Collection of NetworkMagnitudeSolution objects to associate with the orid's location
   * solution
   */
  private Collection<NetworkMagnitudeSolution> createNetworkMagnitudeSolutions(Integer orid) {
    final Collection<NetworkMagnitudeSolution> networkMagnitudeSolutions = new ArrayList<>();

    List<NetmagRecord> netmagRecords = this.netmagRecordsByOrid.get(orid);
    // Netmag rows are associated to an event via orid
    // SonarLint doesn't like all the continue statements that are in this loop, but they're
    // necessary for the data handling needed for this parsing.
    for (NetmagRecord netmagRecord : netmagRecords) {
      // TODO: Remove this check once FeatureMeasurement issues are resolved
      // The issue is that multiple station magnitudes can refer to the same amplitude value.
      // When those station magnitudes are part of the same event, each station magnitude has
      // its own copy of that amplitude as a FeatureMeasurement. When those objects are persisted,
      // Hibernate throws an error saying that there are two objects that resolve to essentially
      // the same row in the database, and the data is not written. By only using MB and MS,
      // we are using different amplitude measurements taken on different parts of the waveform,
      // so we shouldn't have this collision.
      if (netmagRecord.getMagtype() != MagnitudeType.MBMLE
          && netmagRecord.getMagtype() != MagnitudeType.MSMLE) {
        logger.warn("CSS netmag row does not have magtype mb_mle or ms_mle. "
            + "Skipping netmag row: {}", netmagRecord);
        continue;
      }
      final List<NetworkMagnitudeBehavior> networkMagnitudeBehaviors = new ArrayList<>();

      List<StamagRecord> stamagRecords = this.stamagRecordsByMagid.get(netmagRecord.getMagid());
      // Stamag records are associated with netmag records via magid
      // SonarLint doesn't like all the continue statements that are in this loop, but they're
      // necessary for the data handling needed for this parsing.
      for (StamagRecord stamagRecord : stamagRecords) {

        // TODO: Remove this check once FeatureMeasurement issues are resolved
        // See comments above similar type for netmag for more information
        if (stamagRecord.getMagtype() != MagnitudeType.MBMLE
            && stamagRecord.getMagtype() != MagnitudeType.MSMLE) {
          logger.warn("CSS stamag row does not have magtype mb_mle or ms_mle. "
              + "Skipping stamag row: {}", stamagRecord);
          continue;
        }

        // The FeatureMeasurement is looked up via stamag's reference to amplitude
        final AmplitudeRecord amplitudeRecord = this.amplitudeRecordsByAmpid
            .get(stamagRecord.getAmpid());
        if (amplitudeRecord == null) {
          logger.warn("CSS stamag row does not have a corresponding amplitude row. "
              + ".Skipping stamag row: {}", stamagRecord);
          continue;
        }

        final Optional<Pair<Channel, MeasuredChannelSegmentDescriptor>> channelInfo
            = channelInfoForArid(stamagRecord.getArid());
        if (!channelInfo.isPresent()) {
          continue;
        }

        FeatureMeasurementType<AmplitudeMeasurementValue> ampType =
            AMPTYPE_TO_AMP_FM_TYPE.get(amplitudeRecord.getAmptype());
        // TODO Fix
        final FeatureMeasurement<AmplitudeMeasurementValue> amplitudeFeatureMeasurement =
            FeatureMeasurement.from(
                channelInfo.get().getLeft(),
                channelInfo.get().getRight(),
                ampType,
                AmplitudeMeasurementValue.from(Instant.EPOCH, amplitudeRecord.getPer(),
                    DoubleValue.from(amplitudeRecord.getAmp(), 0.0, Units.DEGREES)));

        // TODO: Remove this check once data model issues are resolved
        // TODO #2: This check is commented out since it was preventing loading data; instead,
        // the stamag uncertainty is an NAValue, set it to 0.28
        // Currently, the data model for magnitude information does not allow for "inapplicable"
        // values to be set; uncertainty is required, cannot be null, and must fall within a
        // specified range.
        // if (stamagRecord.getUncertainty() == StamagRecord.getUncertaintyNaValue()) {
        //   logger.warn("CSS stamag row has uncertainty set to NAValue ({}). Skipping stamag
        //   row: {}",
        //              StamagRecord.getUncertaintyNaValue(), stamagRecord);
        //   continue;
        // }
        double stamagRecordUncertainty = determineStamagUncertaintyValue(stamagRecord);
        StationMagnitudeSolution stationMagnitudeSolution =
            StationMagnitudeSolution.builder()
                .setMagnitude(stamagRecord.getMagnitude())
                .setMagnitudeUncertainty(stamagRecordUncertainty)
                // Use 0.0 since CSS data does not have station correction information
                .setStationCorrection(0.0)
                .setStationName(stamagRecord.getSta())  // TODO Not sure this is right
                .setModel(stamagRecord.getMmodel())
                // Use 0.0 since CSS data does not have model correction information
                .setModelCorrection(0.0)
                .setPhase(PhaseType.valueOf(stamagRecord.getPhase()))
                .setType(stamagRecord.getMagtype())
                .setMeasurement(amplitudeFeatureMeasurement)
                .build();

        // TODO: Remove this check once data model issues are resolved
        // Currently, the data model for magnitude information does not allow for "inapplicable"
        // values to be set; residual is required, cannot be null, and must fall within a
        // specified range.
        if (stamagRecord.getMagres() == StamagRecord.getResNaValue()) {
          logger.warn("CSS stamag row has residual value set to NAValue ({}). Skipping stamag "
              + "row: {}", StamagRecord.getResNaValue(), stamagRecord);
          continue;
        }

        networkMagnitudeBehaviors.add(
            NetworkMagnitudeBehavior.builder()
                .setDefining(stamagRecord.getMagdef())
                .setResidual(stamagRecord.getMagres() == StamagRecord.getResNaValue() ?
                    0.0 : stamagRecord.getMagres())
                .setWeight(0.0)
                .setStationMagnitudeSolution(stationMagnitudeSolution)
                .build());
      }

      // TODO: Remove this check once data model issues are resolved
      // Currently, the data model for magnitude information does not allow for "inapplicable"
      // values to be set; uncertainty is required, cannot be null, and must fall within a
      // specified range.
      if (netmagRecord.getUncertainty() == NetmagRecord.getUncertaintyNaValue()) {
        logger.warn("CSS netmag row has uncertainty set to NAValue ({}). Skipping netmag row: {}",
            NetmagRecord.getUncertaintyNaValue(), netmagRecord);
        continue;
      }

      networkMagnitudeSolutions.add(
          NetworkMagnitudeSolution.builder()
              .setMagnitude(netmagRecord.getMagnitude())
              .setUncertainty(netmagRecord.getUncertainty())
              .setMagnitudeType(netmagRecord.getMagtype())
              .setNetworkMagnitudeBehaviors(networkMagnitudeBehaviors)
              .build());
    }
    return networkMagnitudeSolutions;
  }

  private Optional<Pair<Channel, MeasuredChannelSegmentDescriptor>> channelInfoForArid(int arid) {
    final Long wfid = this.aridToWfid.get(arid);
    if (wfid == null) {
      aridsWithNoWfid.add(arid);
      return Optional.empty();
    }

    final Channel chan = this.wfidToChannel.get(wfid);
    if (chan == null) {
      this.wfidsWithNoChannel.add(wfid);
      return Optional.empty();
    }

    final WfdiscRecord wfdisc = this.wfdiscsByWfid.get(wfid);
    if (wfdisc == null) {
      this.wfidsWithNoWfdisc.add(wfid);
      return Optional.empty();
    }
    final MeasuredChannelSegmentDescriptor segmentDesc = MeasuredChannelSegmentDescriptor.builder()
        .setChannelName(chan.getName())
        .setMeasuredChannelSegmentStartTime(wfdisc.getTime())
        .setMeasuredChannelSegmentEndTime(wfdisc.getEndtime())
        .setMeasuredChannelSegmentCreationTime(Instant.EPOCH)
        .build();
    return Optional.of(Pair.of(chan, segmentDesc));
  }

  //TODO: remove this once the data model supports null type values
  // This is here as a stop gap solution for getting magnitudes into the OSD
  private double determineStamagUncertaintyValue(StamagRecord stamagRecord) {
    if (stamagRecord.getUncertainty() != StamagRecord.getUncertaintyNaValue()) {
      return stamagRecord.getUncertainty();
    }

    if (stamagRecord.getMagtype().equals(MagnitudeType.MBMLE)) {
      return new SecureRandom().nextBoolean() ? 0.28 : 0.34;
    } else {
      return new SecureRandom().nextBoolean() ? 0.32 : 0.35;
    }
  }

  private static <T extends WfdiscRecord> Map<Long, WfdiscRecord> toMapByWfid(
      Collection<T> wfdiscs) {
    return wfdiscs.stream()
        .collect(Collectors.toMap(WfdiscRecord::getWfid, Function.identity()));
  }

  static Map<Long, Channel> readWfidToChanFile(String path) {
    final ObjectMapper mapper = CoiObjectMapperFactory.getJsonObjectMapper();
    final JavaType t = mapper.getTypeFactory()
        .constructMapType(HashMap.class, Long.class, Channel.class);
    try {
      return mapper.readValue(new File(path), t);
    } catch (IOException e) {
      throw new RuntimeException("Could not read channel file at path " + path, e);
    }
  }

  private static EventLocation location(OriginRecord origin) {
    return EventLocation.from(
        origin.getLat(), origin.getLon(),
        origin.getDepth(), origin.getTime());
  }
}
