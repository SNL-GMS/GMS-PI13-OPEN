package gms.shared.frameworks.osd.coi.waveforms;

import gms.shared.frameworks.osd.coi.util.TestUtilities;
import java.io.IOException;
import org.junit.jupiter.api.Test;

class RawStationDataFrameTests {

  @Test
  void testSerialization() throws IOException {
    RawStationDataFrame expected = WaveformTestFixtures.RAW_STATION_DATA_FRAME;
    TestUtilities.testSerialization(expected, RawStationDataFrame.class);
  }

}