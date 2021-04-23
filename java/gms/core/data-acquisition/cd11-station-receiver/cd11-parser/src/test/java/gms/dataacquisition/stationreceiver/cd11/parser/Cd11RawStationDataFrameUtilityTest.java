package gms.dataacquisition.stationreceiver.cd11.parser;

import static gms.dataacquisition.stationreceiver.cd11.parser.Cd11RawStationDataFrameUtility.parseAcquiredStationDataPacket;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.willReturn;

import gms.core.dataacquisition.receiver.DataFrameReceiverConfiguration;
import gms.shared.frameworks.osd.coi.dataacquisition.ReceivedStationDataPacket;
import gms.shared.frameworks.osd.coi.waveforms.RawStationDataFrame;
import gms.shared.frameworks.osd.coi.waveforms.RawStationDataFramePayloadFormat;
import gms.shared.frameworks.osd.coi.waveforms.WaveformSummary;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class Cd11RawStationDataFrameUtilityTest {

  @Mock
  private DataFrameReceiverConfiguration mockConfig;

  private static final String stationName = "LBTB";
  private static final String lbt1ShzName = stationName + ".LBTB1.SHZ";
  private static final List<String> expectedChannelNames = List.of(lbt1ShzName,
      stationName + ".LBTBB.BHZ",
      stationName + ".LBTBB.BHN",
      stationName + ".LBTBB.BHE");
  private final static Instant expectedStartTime = Instant.parse("2019-06-06T17:26:00Z");
  private final static Instant expectedFrameEndTime = Instant.parse("2019-06-06T17:26:10Z");
  private final static Instant expectedTimeSeriesEndTime = Instant
      .parse("2019-06-06T17:26:09.975Z");

  private static ReceivedStationDataPacket packet;

  @BeforeEach
  void setup() throws Exception {
    final byte[] cd11_packet_bytes = Files
        .readAllBytes(Paths.get("src/test/resources/LBTB_cd11_data_frame_raw_bytes"));
    packet = ReceivedStationDataPacket.from(cd11_packet_bytes, Instant.now(), 5L, stationName);
  }

  @Test
  void testParseAcquiredStationDataPacketBadBytesThrows() {
    final Predicate<IllegalArgumentException> validator = e -> e.getMessage()
        .equals("Could not read frame from cd11 packet");
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> parseAcquiredStationDataPacket(mockConfig,
            ReceivedStationDataPacket.from(new byte[5], Instant.now(), 5L, "station")));
    assertTrue(validator.test(ex));
    ex = assertThrows(IllegalArgumentException.class,
        () -> parseAcquiredStationDataPacket(mockConfig,
            ReceivedStationDataPacket.from(new byte[5000], Instant.now(), 5L, "station")));
    assertTrue(validator.test(ex));
  }

  @Test
  void testParseAcquiredStationDataPacket() {
    initConfigMock();
    final RawStationDataFrame rsdf = parseAcquiredStationDataPacket(mockConfig, packet);
    assertNotNull(rsdf);
    assertEquals(packet.getStationIdentifier(), rsdf.getMetadata().getStationName());
    assertEquals(expectedChannelNames, rsdf.getMetadata().getChannelNames());
    assertEquals(RawStationDataFramePayloadFormat.CD11, rsdf.getMetadata().getPayloadFormat());
    assertEquals(expectedStartTime, rsdf.getMetadata().getPayloadStartTime());
    assertEquals(expectedFrameEndTime, rsdf.getMetadata().getPayloadEndTime());
    assertEquals(packet.getReceptionTime(), rsdf.getMetadata().getReceptionTime());
    assertArrayEquals(packet.getPacket(), rsdf.getRawPayload());
    for (String chan : expectedChannelNames) {
      assertEquals(WaveformSummary.from(chan, expectedStartTime, expectedTimeSeriesEndTime),
          rsdf.getMetadata().getWaveformSummaries().get(chan));
    }
  }

  /**
   * Sets up the mock config return values for both parser methods that use it
   */
  private void initConfigMock() {
    //setup mock config behavior
    willReturn(Optional.of("LBTB.LBTB1.SHZ"))
        .given(mockConfig).getChannelName("LBTB.LBTB1.SHZ");
    willReturn(Optional.of("LBTB.LBTBB.BHZ"))
        .given(mockConfig).getChannelName("LBTB.LBTBB.BHZ");
    willReturn(Optional.of("LBTB.LBTBB.BHN"))
        .given(mockConfig).getChannelName("LBTB.LBTBB.BHN");
    willReturn(Optional.of("LBTB.LBTBB.BHE"))
        .given(mockConfig).getChannelName("LBTB.LBTBB.BHE");
  }
}
