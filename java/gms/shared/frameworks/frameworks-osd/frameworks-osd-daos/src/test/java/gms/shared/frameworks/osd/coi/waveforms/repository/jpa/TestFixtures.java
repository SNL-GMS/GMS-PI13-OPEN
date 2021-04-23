package gms.shared.frameworks.osd.coi.waveforms.repository.jpa;

import static gms.shared.frameworks.osd.coi.channel.repository.jpa.dataaccessobjects.TestFixtures.channel;

import gms.shared.frameworks.osd.coi.PhaseType;
import gms.shared.frameworks.osd.coi.channel.Channel;
import gms.shared.frameworks.osd.coi.channel.ChannelSegment;
import gms.shared.frameworks.osd.coi.channel.ChannelSegment.Type;
import gms.shared.frameworks.osd.coi.signaldetection.FkSpectraDefinition;
import gms.shared.frameworks.osd.coi.signaldetection.Location;
import gms.shared.frameworks.osd.coi.stationreference.RelativePosition;
import gms.shared.frameworks.osd.coi.waveforms.RawStationDataFrame;
import gms.shared.frameworks.osd.coi.waveforms.RawStationDataFrame.AuthenticationStatus;
import gms.shared.frameworks.osd.coi.waveforms.RawStationDataFrameMetadata;
import gms.shared.frameworks.osd.coi.waveforms.RawStationDataFramePayloadFormat;
import gms.shared.frameworks.osd.coi.waveforms.Waveform;
import gms.shared.frameworks.osd.coi.waveforms.WaveformSummary;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;


public class TestFixtures {

  public static ChannelSegment<Waveform> buildChannelSegment(Channel channel, double sampleRate,
      Instant start, Instant end) {
    int size = (int) (Duration.between(start, end).toSeconds() * sampleRate);
    return ChannelSegment
        .create(channel, "TEST", Type.RAW, List.of(buildWaveform(start, sampleRate, size)));
  }

  public static ChannelSegment<Waveform> buildChannelSegment(Channel channel, Instant start,
      double sampleRate,
      int sampleCount) {
    return ChannelSegment
        .create(channel, "TEST", Type.RAW, List.of(buildWaveform(start, sampleRate, sampleCount)));
  }

  public static Waveform buildWaveform(Instant start, double sampleRate, int samplecount) {
    Random ran = new Random();
    double[] values = new double[samplecount];
    for (int i = 0; i < samplecount; i++) {
      values[i] = ran.nextInt(100000);
    }
    return Waveform.from(start, sampleRate, values);
  }

  public static final double SAMPLE_RATE = 2.0;
  public static final double SAMPLE_RATE2 = 5.0;

  public static final UUID SOH_BOOLEAN_ID = UUID.fromString("5f1a3629-ffaf-4190-b59d-5ca6f0646fd6");
  public static final UUID SOH_ANALOG_ID = UUID.fromString("b12c0b3a-4681-4ee3-82fc-4fcc292aa59f");
  public static final UUID CHANNEL_SEGMENT_ID = UUID
      .fromString("57015315-f7b2-4487-b3e7-8780fbcfb413");
  public static final UUID CHANNEL_SEGMENT_2_ID = UUID
      .fromString("67015315-f7b2-4487-b3e7-8780fbcfb413");
  public static final UUID FRAME_1_STATION_NAME = UUID.fromString(
      "12347cc2-8c86-4fa1-a764-c9b9944614b7");
  public static final UUID FRAME_2_STATION_NAME = UUID.fromString(
      "23447cc2-8c86-4fa1-a764-c9b9944614b7");
  public static final UUID WAVEFORM_SUMMARY_ID = UUID.fromString(
      "b6c47159-1d32-4e18-a861-ea156599d40b");

  // TODO: this test fixtures data had overlapping channel segments,
  // but now changing that breaks the channel availability test.


  public static final String segmentStartDateString = "1970-01-02T03:04:05.123Z";

  public static final Instant SEGMENT_START = Instant.parse(segmentStartDateString);

  public static final double[] WAVEFORM_POINTS = new double[]{1.1, 2.2, 3.3, 4.4, 5.5};
  public static final double[] WAVEFORM_POINTS2 = new double[]{6, 7, 8, 9, 10};

  public static final Waveform waveform1 = Waveform.from(
      SEGMENT_START, SAMPLE_RATE, WAVEFORM_POINTS);

  public static final Instant SEGMENT_START2 = waveform1.getEndTime().plusSeconds(1);

  public static final Waveform waveform2 = Waveform.from(
      SEGMENT_START2, SAMPLE_RATE2, WAVEFORM_POINTS2);
  public static final Waveform waveform3 = buildWaveform(
      Instant.ofEpochSecond(55555), 40.0, 400);

  public static final List<Waveform> waveforms = List.of(waveform1);

  public static final List<Waveform> waveforms2 = List.of(waveform2);

  public static final ChannelSegment<Waveform> channelSegment = ChannelSegment.from(
      CHANNEL_SEGMENT_ID, channel, CHANNEL_SEGMENT_ID.toString(),
      ChannelSegment.Type.RAW, waveforms);

  public static final Instant SEGMENT_END = channelSegment.getEndTime();

  public static final ChannelSegment<Waveform> channelSegment2 = ChannelSegment.from(
      CHANNEL_SEGMENT_2_ID, channel, CHANNEL_SEGMENT_2_ID.toString(),
      ChannelSegment.Type.RAW, waveforms2);

  public static final Instant SEGMENT_END2 = channelSegment2.getEndTime();

  public static final Map<String, WaveformSummary> waveformSummaries = Map.of(WAVEFORM_SUMMARY_ID.toString(),
      WaveformSummary.from(WAVEFORM_SUMMARY_ID.toString(), Instant.now(), Instant.now().plusSeconds(20L)));

  public static final RawStationDataFrame frame1 = RawStationDataFrame.builder()
      .setId(UUID.randomUUID())
      .setMetadata(RawStationDataFrameMetadata.builder()
          .setStationName(FRAME_1_STATION_NAME.toString())
          .setChannelNames(List.of(channel.getName()))
          .setPayloadFormat(RawStationDataFramePayloadFormat.CD11)
          .setPayloadStartTime(SEGMENT_START)
          .setPayloadEndTime(SEGMENT_END)
          .setReceptionTime(SEGMENT_END.plusSeconds(10))
          .setAuthenticationStatus(AuthenticationStatus.AUTHENTICATION_SUCCEEDED)
          .setWaveformSummaries(waveformSummaries)
          .build())
      .setRawPayload(new byte[50])
      .build();

  public static final RawStationDataFrame frame2 = RawStationDataFrame.builder()
      .setId(UUID.randomUUID())
      .setMetadata(RawStationDataFrameMetadata.builder()
          .setStationName(FRAME_2_STATION_NAME.toString())
          .setChannelNames(List.of(channel.getName()))
          .setPayloadFormat(RawStationDataFramePayloadFormat.CD11)
          .setPayloadStartTime(SEGMENT_START2)
          .setPayloadEndTime(SEGMENT_END2)
          .setReceptionTime(SEGMENT_END2.plusSeconds(10))
          .setAuthenticationStatus(AuthenticationStatus.AUTHENTICATION_FAILED)
          .setWaveformSummaries(waveformSummaries)
          .build())
      .setRawPayload(new byte[50])
      .build();


  // Create an FkSpectraDefinition
  private static final Duration windowLead = Duration.ofMinutes(3);
  private static final Duration windowLength = Duration.ofMinutes(2);
  private static final double sampleRate = 1/60.0;

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

  public static final UUID channelID = UUID.fromString("d07aa77a-b6a4-478f-b3cd-5c934ee6b812");

  // Create a Location
  public static final Location location = Location.from(1.2, 3.4, 7.8, 5.6);

  // Create a RelativePosition
  public static final RelativePosition relativePosition = RelativePosition
      .from(1.2, 3.4, 5.6);

  private static final Map<UUID, RelativePosition> relativePositions = Map.ofEntries(
      Map.entry(channelID, relativePosition)
  );

  public static final FkSpectraDefinition FK_SPECTRA_DEFINITION = FkSpectraDefinition.builder()
      .setWindowLead(windowLead)
      .setWindowLength(windowLength)
      .setSampleRateHz(sampleRate)
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


  public static final double[][] FK_SPECTRUM_POWER = new double
      [FK_SPECTRA_DEFINITION.getSlowCountY()]
      [FK_SPECTRA_DEFINITION.getSlowCountX()];
}