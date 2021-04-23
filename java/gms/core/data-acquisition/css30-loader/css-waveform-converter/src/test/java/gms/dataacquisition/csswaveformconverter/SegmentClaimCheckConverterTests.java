package gms.dataacquisition.csswaveformconverter;

import static gms.dataacquisition.csswaveformconverter.WaveformConverterTestFixtures.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import gms.dataacquisition.cssreader.data.WfdiscRecord;
import gms.dataacquisition.cssreader.flatfilereaders.FlatFileWfdiscReader;
import gms.shared.frameworks.osd.coi.channel.ChannelSegment;
import gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssue;
import gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssue.AcquiredChannelEnvironmentIssueType;
import gms.shared.frameworks.osd.coi.dataacquisition.SegmentClaimCheck;
import gms.shared.frameworks.osd.coi.dataacquisition.WaveformAcquiredChannelSohPair;
import gms.shared.frameworks.osd.coi.waveforms.Waveform;
import java.io.IOException;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Tests the SegmentClaimCheckConverter.
 */
class SegmentClaimCheckConverterTests {
  private static List<SegmentClaimCheck> segmentClaimChecks;

  /**
   * Populates sgmentClaimChecks wih only wfdisc records matching
   */
  @BeforeAll
  static void initSuite() throws Exception {
    final String wfdiscPath = DATA_PATH + "/wfdisc_davox_s4.txt";
    final List<WfdiscRecord> wfdiscs = new FlatFileWfdiscReader(
        List.of(STATION_NAME), List.of(FREQUENCY), null, null)
        .read(wfdiscPath);
    segmentClaimChecks = new WfdiscToSegmentClaimCheckConverter()
        .convert(wfdiscs, Map.of(FULL_CHAN_NAME, DAVOX_HHN_CHANNEL), Map.of());
  }

  /**
   * Tests the reader with no claim checks, should parse nothing.
   */
  @Test
  void testEmptyClaimCheck() throws IOException {
    final SegmentClaimCheckConverter reader = new SegmentClaimCheckConverter(
        List.of(), DATA_PATH, 1);
    final List<WaveformAcquiredChannelSohPair> allBatches = reader.readAllBatches();
    assertEquals(0, allBatches.size());
  }

  /**
   * Tests that the reader correctly turns claim checks into Channel Segments
   */
  @Test
  void testReadNextBatch() throws IOException {
    final SegmentClaimCheckConverter reader = new SegmentClaimCheckConverter(
        segmentClaimChecks, DATA_PATH, 1);
    final List<WaveformAcquiredChannelSohPair> allBatches = reader.readAllBatches();
    final Set<ChannelSegment<Waveform>> segments = new HashSet<>();
    final Set<AcquiredChannelEnvironmentIssue> sohs = new HashSet<>();
    for (WaveformAcquiredChannelSohPair batch : allBatches) {
      segments.addAll(batch.getWaveforms());
      sohs.addAll(batch.getAcquiredChannelEnvironmentIssues());
    }
    assertNotNull(segments);
    assertEquals(3, segments.size());
    assertNotNull(sohs);
    assertEquals(1, sohs.size());

    ChannelSegment<Waveform> testSegment = null;
    // assert properties of the loaded segments
    for (ChannelSegment<Waveform> cs : segments) {
      assertEquals(DAVOX_HHN_CHANNEL, cs.getChannel());
      assertEquals("segment for channel " + FULL_CHAN_NAME, cs.getName());
      assertEquals(ChannelSegment.Type.ACQUIRED, cs.getType());
      if(cs.getId().equals(UUID.fromString("7a85adb7-1234-3c8a-bcc3-32b692f2e9ae"))) {
        testSegment = cs;
      }
    }
    //assert properties of a segment
    if(testSegment != null){
      assertEquals(Instant.parse("2010-05-20T02:55:32.724Z"), testSegment.getStartTime());
      assertEquals(Instant.parse("2010-05-20T03:14:55.990666666Z"), testSegment.getEndTime());
      assertEquals(1, testSegment.getTimeseries().size());
    }
    else{
      fail("Failed to parse segment " + SEG_ID);
    }

    // assert properties of the loaded soh
    Optional<AcquiredChannelEnvironmentIssue> onlySohOptional = sohs.stream().findFirst();
    AcquiredChannelEnvironmentIssue onlySoh = onlySohOptional.orElseThrow(() -> new IllegalStateException(
        "Failed to retrieve SOH"));
    assertEquals(AcquiredChannelEnvironmentIssueType.CLIPPED, onlySoh.getType());
    assertEquals(true, onlySoh.getStatus());
    assertEquals(Instant.parse("2010-05-20T00:00:00Z"), onlySoh.getStartTime());
    assertEquals(Instant.parse("2010-05-20T00:02:12.991666666Z"), onlySoh.getEndTime());
  }
}
