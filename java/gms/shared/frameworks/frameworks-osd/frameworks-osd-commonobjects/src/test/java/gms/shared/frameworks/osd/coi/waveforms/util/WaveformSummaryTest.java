package gms.shared.frameworks.osd.coi.waveforms.util;

import gms.shared.frameworks.osd.coi.util.TestUtilities;
import gms.shared.frameworks.osd.coi.waveforms.WaveformSummary;
import java.time.Instant;
import org.junit.jupiter.api.Test;

public class WaveformSummaryTest {

  private final String id = "TESTNET.STALOC.CHAN";
  private final Instant startTime = Instant.EPOCH;
  private final Instant endTime = Instant.EPOCH.plusMillis(2000);

  @Test
  public void testWaveformSummaryFromChecksNullArguments() throws Exception {
    TestUtilities.checkStaticMethodValidatesNullArguments(
        WaveformSummary.class, "from",
        id, startTime, endTime);
  }
}
