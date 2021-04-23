package gms.dataacquisition.css.stationrefconverter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import gms.shared.frameworks.osd.coi.Units;
import gms.shared.frameworks.osd.coi.channel.Channel;
import gms.shared.frameworks.osd.coi.channel.ChannelBandType;
import gms.shared.frameworks.osd.coi.channel.ChannelDataType;
import gms.shared.frameworks.osd.coi.channel.ChannelInstrumentType;
import gms.shared.frameworks.osd.coi.channel.ChannelOrientationType;
import gms.shared.frameworks.osd.coi.channel.ReferenceChannel;
import gms.shared.frameworks.osd.coi.provenance.InformationSource;
import gms.shared.frameworks.osd.coi.signaldetection.Calibration;
import gms.shared.frameworks.osd.coi.signaldetection.FrequencyAmplitudePhase;
import gms.shared.frameworks.osd.coi.signaldetection.Response;
import gms.shared.frameworks.osd.coi.stationreference.ReferenceCalibration;
import gms.shared.frameworks.osd.coi.stationreference.ReferenceResponse;
import gms.shared.frameworks.osd.coi.stationreference.RelativePosition;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class StationGroupBuilderTests {

  private static final ReferenceCalibration mockRefCalibration = mock(ReferenceCalibration.class);
  private static final Calibration mockCalibration = mock(Calibration.class);

  private static StationGroupBuilder stationGroupBuilder;

  @BeforeAll
  static void setup() throws Exception {
    when(mockRefCalibration.getCalibration()).thenReturn(mockCalibration);
    final ClassLoader classLoader = ClassLoader.getSystemClassLoader();
    CssReferenceReader.process(classLoader.getResource("data").getPath(),
        "test_config.network");

    stationGroupBuilder = new StationGroupBuilder(
        CssReferenceReader.getReferenceNetworkMemberships(),
        CssReferenceReader.getReferenceStationMemberships(),
        CssReferenceReader.getReferenceSiteMemberships(),
        CssReferenceReader.getReferenceNetworksByName().values(),
        CssReferenceReader.getReferenceStationsByName().values(),
        CssReferenceReader.getReferenceSitesByName().values(),
        CssReferenceReader.getReferenceChannelsByName().values(),
        CssReferenceReader.getReferenceResponses());
  }

  @Test
  void testParseRawFromMultipleReferenceChannels() {
    // set activation time to one week ago
    Instant rawChannelActivationTimeOne = Instant.now().minusSeconds(604800);
    // set an activation time to six days ago
    Instant rawChannelActivationTimeTwo = rawChannelActivationTimeOne.plusSeconds(86400);
    String reference = "OffDate: " + rawChannelActivationTimeTwo.toString();
    ReferenceChannel referenceChannel = ReferenceChannel.builder()
        .setName("NVAR/BH1")
        .setDataType(ChannelDataType.SEISMIC)
        .setBandType(ChannelBandType.BROADBAND)
        .setInstrumentType(ChannelInstrumentType.HIGH_GAIN_SEISMOMETER)
        .setOrientationType(ChannelOrientationType.ORTHOGONAL_1)
        .setOrientationCode('1')
        .setLocationCode("")
        .setLatitude(57.7828)
        .setLongitude(-152.5835)
        .setElevation(0.152)
        .setDepth(0.088)
        .setVerticalAngle(90.0)
        .setHorizontalAngle(0.0)
        .setUnits(Units.NANOMETERS_PER_COUNT)
        .setNominalSampleRate(20.0)
        .setActualTime(rawChannelActivationTimeOne)
        .setSystemTime(rawChannelActivationTimeOne)
        .setActive(true)
        .setInformationSource(
            InformationSource.from("External",
                rawChannelActivationTimeOne,
                reference))
        .setComment("Channel is associated with site NVAR")
        .setPosition(
            RelativePosition.from(0.0,
                0.0, 0.0))
        .setAliases(List.of())
        .build();
    // we really want the same reference channel, but we needed to change some attributes
    // so we can discern which reference channel was actually used post filtering to
    // create a channel
    // this is the reference channel we expect to use to create the channel
    ReferenceChannel referenceChannelTwo = referenceChannel.toBuilder()
        .setActualTime(rawChannelActivationTimeTwo)
        .setOrientationType(ChannelOrientationType.ORTHOGONAL_2)
        .setOrientationCode('2')
        .build();

    final ReferenceResponse refResponse = makeRefResponse(referenceChannel);

    Pair<List<Channel>, List<Response>> chansAndResponses = stationGroupBuilder
        .createChannelsAndResponses(List.of(referenceChannel, referenceChannelTwo),
            Map.of(referenceChannel.getName(), refResponse),
            "NVAR", "NV31");
    assertNotNull(chansAndResponses);
    final List<Channel> channels = chansAndResponses.getLeft();
    assertNotNull(channels);
    // we started with two reference channels,
    // but we should only use the one with the most recent actual time
    assertEquals(1, channels.size());
    final Channel onlyChannel = channels.get(0);
    assertEquals('2', onlyChannel.getChannelOrientationCode());
    final List<Response> responses = chansAndResponses.getRight();
    assertNotNull(responses);
    assertEquals(1, responses.size());
    final Response onlyResponse = responses.get(0);
    assertNotNull(onlyResponse);
    assertEquals(onlyChannel.getName(), onlyResponse.getChannelName());
    assertEquals(mockCalibration, onlyResponse.getCalibration());
    assertEquals(refResponse.getFapResponse(), onlyResponse.getFapResponse());
  }

  private static ReferenceResponse makeRefResponse(ReferenceChannel chan) {
    return ReferenceResponse.builder()
        .setChannelName(chan.getName())
        .setActualTime(chan.getActualTime())
        .setSystemTime(chan.getSystemTime())
        .setComment(chan.getComment())
        .setReferenceCalibration(mockRefCalibration)
        .setFapResponse(mock(FrequencyAmplitudePhase.class))
        .build();
  }
}