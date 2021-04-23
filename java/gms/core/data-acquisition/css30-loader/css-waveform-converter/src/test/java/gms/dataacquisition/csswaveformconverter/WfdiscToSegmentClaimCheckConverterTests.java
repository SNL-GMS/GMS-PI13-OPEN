package gms.dataacquisition.csswaveformconverter;

import static gms.dataacquisition.csswaveformconverter.WaveformConverterTestFixtures.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gms.dataacquisition.cssreader.data.WfdiscRecord;
import gms.dataacquisition.cssreader.flatfilereaders.FlatFileWfdiscReader;
import gms.shared.frameworks.osd.coi.channel.ChannelSegment.Type;
import gms.shared.frameworks.osd.coi.dataacquisition.SegmentClaimCheck;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class WfdiscToSegmentClaimCheckConverterTests {

  private WfdiscToSegmentClaimCheckConverter converter = new WfdiscToSegmentClaimCheckConverter();

  private static List<WfdiscRecord> wfdiscRecords;

  private static final String WFDISC_PATH = DATA_PATH + "/wfdisc_davox_s4.txt";
  private static final int WFDISC_COUNT = 4;

  @BeforeAll
  static void setup() throws Exception {
    wfdiscRecords = new FlatFileWfdiscReader().read(WFDISC_PATH);
    assertNotNull(wfdiscRecords);
    assertEquals(WFDISC_COUNT, wfdiscRecords.size());
  }

  @Test
  void testReadEmptyListReturnsEmptyList() {
    assertEquals(List.of(), converter.convert(List.of(), Map.of(), Map.of()));
  }

  @Test
  void testConvert() {
    final List<SegmentClaimCheck> claimChecks = converter.convert(wfdiscRecords,
        Map.of(FULL_CHAN_NAME, DAVOX_HHN_CHANNEL,
            FULL_CHAN_NAME_HHE, DAVOX_HHE_CHANNEL,
            FULL_CHAN_NAME_HNN2, DAVOX_HHN_CHANNEL,
            FULL_CHAN_NAME_HNN3, DAVOX_HHN_CHANNEL),
        Map.of());
    final String expectedChannelName = "DAVOX.HHE";
    //todo determine what is missing in loading of the segments with the new channel refactor
    assertEquals(WFDISC_COUNT, claimChecks.size());
    Optional<SegmentClaimCheck> claimCheckOptional = claimChecks.stream()
        .filter(x -> x.getChannel().getName().equals(expectedChannelName))
        .findFirst();
    assertTrue(claimCheckOptional.isPresent());
    SegmentClaimCheck claimCheck = claimCheckOptional.orElseThrow(() -> new IllegalStateException(
        "No claimCheck found with name " + expectedChannelName));
    assertEquals(UUID.fromString("e72c0d0b-1765-3cd1-bc20-0af28fde8bb2"), claimCheck.getSegmentId());
    assertEquals(Instant.parse("2010-05-20T00:00:00Z"), claimCheck.getStartTime());
    assertEquals(120.0, claimCheck.getSampleRate());
    assertEquals("DAVOX0.w", claimCheck.getWaveformFile());
    assertEquals(15960, claimCheck.getSampleCount());
    assertEquals(0, claimCheck.getfOff());
    assertEquals("s4", claimCheck.getDataType());
    assertEquals(Type.ACQUIRED, claimCheck.getSegmentType());
    assertFalse(claimCheck.isClipped());
  }
}
