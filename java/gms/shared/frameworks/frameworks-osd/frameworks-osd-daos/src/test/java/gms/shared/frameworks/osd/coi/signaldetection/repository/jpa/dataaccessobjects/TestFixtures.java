package gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.dataaccessobjects;

import gms.shared.frameworks.osd.coi.DoubleValue;
import gms.shared.frameworks.osd.coi.PhaseType;
import gms.shared.frameworks.osd.coi.Units;
import gms.shared.frameworks.osd.coi.channel.ChannelBandType;
import gms.shared.frameworks.osd.coi.channel.ChannelDataType;
import gms.shared.frameworks.osd.coi.channel.ChannelInstrumentType;
import gms.shared.frameworks.osd.coi.channel.ChannelOrientationType;
import gms.shared.frameworks.osd.coi.channel.ReferenceChannel;
import gms.shared.frameworks.osd.coi.provenance.InformationSource;
import gms.shared.frameworks.osd.coi.signaldetection.AmplitudePhaseResponse;
import gms.shared.frameworks.osd.coi.signaldetection.BeamDefinition;
import gms.shared.frameworks.osd.coi.signaldetection.Calibration;
import gms.shared.frameworks.osd.coi.signaldetection.FkSpectraDefinition;
import gms.shared.frameworks.osd.coi.signaldetection.FrequencyAmplitudePhase;
import gms.shared.frameworks.osd.coi.signaldetection.Location;
import gms.shared.frameworks.osd.coi.signaldetection.Response;
import gms.shared.frameworks.osd.coi.stationreference.NetworkOrganization;
import gms.shared.frameworks.osd.coi.stationreference.NetworkRegion;
import gms.shared.frameworks.osd.coi.stationreference.ReferenceCalibration;
import gms.shared.frameworks.osd.coi.stationreference.ReferenceNetwork;
import gms.shared.frameworks.osd.coi.stationreference.ReferenceNetworkMembership;
import gms.shared.frameworks.osd.coi.stationreference.ReferenceResponse;
import gms.shared.frameworks.osd.coi.stationreference.ReferenceSite;
import gms.shared.frameworks.osd.coi.stationreference.ReferenceSiteMembership;
import gms.shared.frameworks.osd.coi.stationreference.ReferenceSourceResponse;
import gms.shared.frameworks.osd.coi.stationreference.ReferenceStation;
import gms.shared.frameworks.osd.coi.stationreference.ReferenceStationMembership;
import gms.shared.frameworks.osd.coi.stationreference.RelativePosition;
import gms.shared.frameworks.osd.coi.stationreference.ResponseTypes;
import gms.shared.frameworks.osd.coi.stationreference.StationType;
import gms.shared.frameworks.osd.coi.stationreference.StatusType;
import gms.shared.frameworks.osd.coi.stationreference.repository.ReferenceCalibrationDao;
import gms.shared.frameworks.osd.coi.stationreference.repository.ReferenceSourceResponseDao;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class TestFixtures {

  static final UUID
      UNKNOWN_UUID = UUID.fromString("515bcbe0-2c0d-48ec-83f5-9f11cfe30318"),
      CALIBRATION_ID = UUID.fromString("ce7c377a-b6a4-478f-b3bd-5c934ee6b7ef"),
      DIGITIZER_ID = UUID.fromString("0be27c41-3c14-479a-8f87-66a05e8b3936");

  public static final String comment = "This is a comment.";
  public static final String description = "This is a description.";

  public static final Instant actualTime = Instant.parse("1980-01-02T03:04:05.123Z");
  public static final Instant systemTime = Instant.parse("2010-11-07T06:05:04.321Z");

  public static final InformationSource source = InformationSource.from("Internet",
      actualTime, comment);

  public static final StatusType STATUS = StatusType.ACTIVE;

  // Create an FkSpectraDefinition
  public static final UUID channelID = UUID.fromString("d07aa77a-b6a4-478f-b3cd-5c934ee6b812");

  // Create a Location
  public static final Location location = Location.from(1.2, 3.4, 7.8, 5.6);

  // Create a RelativePosition
  public static final RelativePosition relativePosition = RelativePosition
      .from(1.2, 3.4, 5.6);

  // FkSpectraDefinition
  private static final Duration windowLead = Duration.ofMinutes(3);
  private static final Duration windowLength = Duration.ofMinutes(2);
  private static final double fkSampleRate = 1 / 60.0;

  private static final double lowFrequency = 4.5;
  private static final double highFrequency = 6.0;

  private static final boolean useChannelVerticalOffsets = false;
  private static final boolean normalizeWaveforms = false;
  private static final PhaseType phaseType = PhaseType.P;

  private static final double slowStartX = 5;
  private static final double slowDeltaX = 10;
  private static final int slowCountX = 25;
  private static final double slowStartY = 5;
  private static final double slowDeltaY = 10;
  private static final int slowCountY = 25;

  private static final double waveformSampleRateHz = 10.0;
  private static final double waveformSampleRateToleranceHz = 11.0;

  private static final int minimumWaveformsForBeam = 2;

  private static final Map<UUID, RelativePosition> relativePositions = Map
      .ofEntries(
          Map.entry(TestFixtures.channelID, TestFixtures.relativePosition)
      );

  public static final BeamDefinition BEAM_DEFINITION = BeamDefinition.builder()
      .setPhaseType(PhaseType.P)
      .setAzimuth(22.3)
      .setSlowness(22.8)
      .setNominalWaveformSampleRate(27.8)
      .setWaveformSampleRateTolerance(2.3)
      .setCoherent(false)
      .setSnappedSampling(false)
      .setTwoDimensional(true)
      .setMinimumWaveformsForBeam(minimumWaveformsForBeam)
      .build();

  public static final FkSpectraDefinition FK_SPECTRA_DEFINITION = FkSpectraDefinition.builder()
      .setWindowLead(windowLead)
      .setWindowLength(windowLength)
      .setSampleRateHz(fkSampleRate)
      .setLowFrequencyHz(lowFrequency)
      .setHighFrequencyHz(highFrequency)
      .setUseChannelVerticalOffsets(useChannelVerticalOffsets)
      .setNormalizeWaveforms(normalizeWaveforms)
      .setPhaseType(phaseType)
      .setSlowStartXSecPerKm(slowStartX)
      .setSlowDeltaXSecPerKm(slowDeltaX)
      .setSlowCountX(slowCountX)
      .setSlowStartYSecPerKm(slowStartY)
      .setSlowDeltaYSecPerKm(slowDeltaY)
      .setSlowCountY(slowCountY)
      .setWaveformSampleRateHz(waveformSampleRateHz)
      .setWaveformSampleRateToleranceHz(waveformSampleRateToleranceHz)
      .setMinimumWaveformsForSpectra(2)
      .build();

  public static final FkSpectraDefinition FK_SPECTRA_DEFINITION_2 = FkSpectraDefinition.builder()
      .setWindowLead(windowLead)
      .setWindowLength(windowLength)
      .setSampleRateHz(fkSampleRate)
      .setLowFrequencyHz(lowFrequency + 1)
      .setHighFrequencyHz(highFrequency)
      .setUseChannelVerticalOffsets(useChannelVerticalOffsets)
      .setNormalizeWaveforms(normalizeWaveforms)
      .setPhaseType(phaseType)
      .setSlowStartXSecPerKm(slowStartX)
      .setSlowDeltaXSecPerKm(slowDeltaX)
      .setSlowCountX(slowCountX)
      .setSlowStartYSecPerKm(slowStartY)
      .setSlowDeltaYSecPerKm(slowDeltaY)
      .setSlowCountY(slowCountY)
      .setWaveformSampleRateHz(waveformSampleRateHz)
      .setWaveformSampleRateToleranceHz(waveformSampleRateToleranceHz)
      .setMinimumWaveformsForSpectra(2)
      .build();

  private static final UUID CHANNEL_SEGMENT_ID = UUID
      .fromString("51111111-1111-1111-1111-111111111111");
  private static final UUID PROCESSING_GROUP_ID = UUID
      .fromString("71111111-1111-1111-1111-111111111111");
  public static final Set<UUID> USED_INPUT_CHANNEL_IDS = new HashSet<>(Arrays.asList(
      UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()));

  // channels and digitizers
  public static final String channelName = "CHN01";
  private static final ChannelDataType dataType = ChannelDataType.HYDROACOUSTIC;
  private static final ChannelBandType bandType = ChannelBandType.IMMENSELY_LONG_PERIOD;
  private static final ChannelInstrumentType instrumentType = ChannelInstrumentType.WATER_CURRENT;
  private static final ChannelOrientationType orientationType = ChannelOrientationType.UNKNOWN;
  private static final char orientationCode = orientationType.getCode();
  private static final double
      lat = 12.34, lon = 56.78, elev = 89.90,
      depth = 4321.0, vertAngle = 125.1, horizAngle = 216.2, sampleRate = 40,
      displacementNorth = 2.01, displacementEast = 2.95, displacementVert = 0.56;

  private static final RelativePosition position = RelativePosition.from(
      displacementNorth, displacementEast, displacementVert);

  private static final RelativePosition referencePosition = RelativePosition.from(
      displacementNorth, displacementEast, displacementVert);

  static final ReferenceChannel refChannel = ReferenceChannel.builder()
      .setDataType(dataType)
      .setBandType(bandType)
      .setInstrumentType(instrumentType)
      .setOrientationType(orientationType)
      .setOrientationCode(orientationCode)
      .setName(channelName)
      .setNominalSampleRate(sampleRate)
      .setActualTime(actualTime)
      .setSystemTime(systemTime)
      .setActive(true)
      .setInformationSource(source)
      .setLatitude(lat)
      .setLongitude(lon)
      .setElevation(elev)
      .setDepth(depth)
      .setVerticalAngle(vertAngle)
      .setHorizontalAngle(horizAngle)
      .setUnits(Units.UNITLESS)
      .setComment(comment)
      .setPosition(referencePosition)
      .setLocationCode("0")
      .setAliases(List.of())
      .build();

  ////////////////////////////////////////////////////////////////////////////////////

  // calibrations
  public static final double calibrationFactor = 2.5;
  public static final double calibrationFactorError = 0.9876;
  public static final double calibrationPeriod = 1.0;
  public static final double calibrationTimeShift = 0.0;

  public static final DoubleValue calFactorDoubleValue = DoubleValue
      .from(calibrationFactor, calibrationFactorError, Units.SECONDS);
  public static final Duration calTimeShiftDuration = Duration.ofSeconds(
      (long) calibrationTimeShift);
  public static final Duration calibrationIntervalDuration = Duration.ofSeconds(
      (long) 0.0);

  // create a Calibration
  public static final Calibration calibration = Calibration.from(
      calibrationPeriod, calTimeShiftDuration, calFactorDoubleValue);

  // create a CalibrationDao
  public static final CalibrationDao calibrationDao = CalibrationDao.from(calibration);

  // create a ReferenceCalibration
  public static final ReferenceCalibration refCalibration = ReferenceCalibration.from(
      calTimeShiftDuration, calibration);

  // create a ReferenceCalibrationDao
  public static final ReferenceCalibrationDao refCalibrationDao = ReferenceCalibrationDao.from(
      calibrationIntervalDuration, refCalibration);
  ////////////////////////////////////////////////////////////////////////////////////

  // responses
  public static final DoubleValue amplitude = DoubleValue.from(0.000014254, 0.0, Units.DEGREES);
  public static final DoubleValue phase = DoubleValue.from(350.140599, 0.0, Units.DEGREES);

  // create an AmplitudePhaseResponse -> amplitudePhaseResponse
  public static final AmplitudePhaseResponse amplitudePhaseResponse = AmplitudePhaseResponse
      .from(amplitude, phase);

  // create a FrequencyAmplitudePhase (fapResponse) using amplitudePhaseResponse created above
  public static final double frequency = 0.001000;

  public static final FrequencyAmplitudePhase fapResponse = FrequencyAmplitudePhase.builder()
      .setFrequencies(new double[] {frequency})
      .setAmplitudeResponseUnits(Units.DEGREES)
      .setAmplitudeResponse(new double[] {0.000014254})
      .setAmplitudeResponseStdDev(new double[] {0.0})
      .setPhaseResponseUnits(Units.DEGREES)
      .setPhaseResponse(new double[] {350.140599})
      .setPhaseResponseStdDev(new double[] {0.0})
      .build();

  // create a FrequencyAmplitudePhaseDao
  public static final FrequencyAmplitudePhaseDao fapResponseDao =
      FrequencyAmplitudePhaseDao.from(fapResponse);

  public static final UUID id = UUID.fromString("cccaa77a-b6a4-478f-b3cd-5c934ee6b999");

  // create the Response using fapResponse created above
  public static final Response response = Response.from(
      channelName, calibration, fapResponse);

  public static final byte[] RESPONSE_DATA = new byte[] {(byte) 1, (byte) 2, (byte) 3,
      (byte) 4, (byte) 5};

  public static final List<InformationSource> sourceResponseInfoSourceList = List.of(source);

  // create a ReferenceSourceResponse
  public static final ReferenceSourceResponse refSourceResponse = ReferenceSourceResponse.builder()
      .setSourceResponseData(RESPONSE_DATA)
      .setSourceResponseUnits(Units.NANOMETERS)
      .setSourceResponseTypes(ResponseTypes.FAP)
      .setInformationSources(sourceResponseInfoSourceList)
      .build();

  // create a ReferenceSourceResponseDao
  public static final ReferenceSourceResponseDao refSourceResponseDao =
      ReferenceSourceResponseDao.from(refSourceResponse);

  // create a ReferenceResponse
  public static final ReferenceResponse refResponse = ReferenceResponse.builder()
      .setChannelName(channelName)
      .setActualTime(actualTime)
      .setSystemTime(systemTime)
      .setComment(comment)
      .setSourceResponse(refSourceResponse)
      .setReferenceCalibration(refCalibration)
      .setFapResponse(fapResponse)
      .build();

  public static final InformationSource informationSource = InformationSource.from(
      "Source", actualTime, "Unit Test");

  ////////////////////////////////////////////////////////////////////////////////////

  // sites
  static final String siteName = "SITE33";

  static final ReferenceSite refSite = ReferenceSite.builder()
      .setName(siteName)
      .setDescription(description)
      .setSource(source)
      .setComment(comment)
      .setLatitude(lat)
      .setLongitude(lon)
      .setElevation(elev)
      .setActualChangeTime(actualTime)
      .setSystemChangeTime(systemTime)
      .setActive(true)
      .setPosition(referencePosition)
      .setAliases(List.of())
      .build();

  ////////////////////////////////////////////////////////////////////////////////////

  // stations
  static final String stationName = "STA01";
  static final StationType stationType = StationType.HYDROACOUSTIC;
  static final ReferenceStation refStation = ReferenceStation.builder()
      .setName(stationName)
      .setDescription(description)
      .setStationType(stationType)
      .setSource(source)
      .setComment(comment)
      .setLatitude(lat)
      .setLongitude(lon)
      .setElevation(elev)
      .setActualChangeTime(actualTime)
      .setSystemChangeTime(systemTime)
      .setActive(true)
      .setAliases(List.of())
      .build();
  ////////////////////////////////////////////////////////////////////////////////////

  // networks
  static final String networkName = "NET01";
  public static final NetworkOrganization org = NetworkOrganization.CTBTO;
  public static final NetworkRegion region = NetworkRegion.GLOBAL;
  static final ReferenceNetwork refNetwork = ReferenceNetwork.builder()
      .setName(networkName)
      .setDescription(description)
      .setOrganization(org)
      .setRegion(region)
      .setSource(source)
      .setComment(comment)
      .setActualChangeTime(actualTime)
      .setSystemChangeTime(systemTime)
      .setActive(true)
      .build();

  ////////////////////////////////////////////////////////////////////////////////////
  public static final ReferenceNetworkMembership networkMembership =
      ReferenceNetworkMembership.create("", actualTime, systemTime,
          refNetwork.getEntityId(), refStation.getEntityId(), STATUS);

  public static final ReferenceStationMembership stationMembership
      = ReferenceStationMembership
      .create("", actualTime, systemTime, refStation.getEntityId(), refSite.getEntityId(), STATUS);

  public static final ReferenceSiteMembership siteMembership
      = ReferenceSiteMembership.create("", actualTime, systemTime,
      refSite.getEntityId(), refChannel.getName(), STATUS);
}
