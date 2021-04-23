package gms.utilities.waveformreader;

import org.junit.jupiter.api.Test;

public class E1FormatWaveformReaderTest {
    private final WaveformReaderInterface reader = new E1FormatWaveformReader();
    private final String WFE1_FILE = "/css/WFS4/I22FR.e1.w";
    private final int SAMPLES_TO_READ = 10;
    private final int SAMPLES_TO_SKIP = 0;
    private final double[] REF_SAMPLES = {-11129, -10996, -10919, -10813, -10713, -10681, -10617, -10674, -10598, -10356 };

    @Test
    public void testReadTestData() throws Exception {
        WaveformReaderTestUtil
            .testReadTestData(reader, this.getClass().getResourceAsStream(WFE1_FILE),
                SAMPLES_TO_READ, SAMPLES_TO_SKIP, REF_SAMPLES);
    }
}

