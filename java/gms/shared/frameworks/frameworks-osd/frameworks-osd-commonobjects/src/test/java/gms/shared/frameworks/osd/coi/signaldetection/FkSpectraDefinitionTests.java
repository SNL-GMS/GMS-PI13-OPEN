package gms.shared.frameworks.osd.coi.signaldetection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import gms.shared.frameworks.osd.coi.util.TestUtilities;
import java.time.Duration;
import org.junit.jupiter.api.Test;

public class FkSpectraDefinitionTests {

  private final FkSpectraDefinition definition = SignalDetectionTestFixtures.FK_SPECTRA_DEFINITION;

  @Test
  public void testSerialization() throws Exception {
    TestUtilities.testSerialization(definition,
        FkSpectraDefinition.class);
  }

  @Test
  public void testBuildZeroWindowLength() {
    IllegalStateException e = assertThrows(IllegalStateException.class,
        () -> definition.toBuilder()
      .setWindowLength(Duration.ZERO)
      .build());
    assertEquals("FkSpectraDefinition requires windowLength of Duration > 0", e.getMessage());
  }

  @Test
  public void testBuildNegativeSampleRate() {
    IllegalStateException e = assertThrows(IllegalStateException.class,
        () -> definition.toBuilder()
            .setSampleRateHz(-2)
            .build());
    assertEquals("FkSpectraDefinition requires sampleRate > 0.0", e.getMessage());
  }

  @Test
  public void testBuildNegativeLowFrequency() {
    IllegalStateException e = assertThrows(IllegalStateException.class,
        () -> definition.toBuilder()
            .setLowFrequencyHz(-1)
            .build());
    assertEquals("FkSpectraDefinition requires lowFrequency >= 0.0", e.getMessage());
  }

  @Test
  public void testBuildHighFrequencyLessThanLowFrequency() {
    IllegalStateException e = assertThrows(IllegalStateException.class,
        () -> definition.toBuilder()
            .setLowFrequencyHz(1)
            .setHighFrequencyHz(.5)
            .build());
    assertEquals("FkSpectraDefinition requires lowFrequency < highFrequency", e.getMessage());
  }

  @Test
  public void testBuildNegativeSlowCountX() {
    IllegalStateException e = assertThrows(IllegalStateException.class,
        () -> definition.toBuilder()
            .setSlowCountX(-1)
            .build());
    assertEquals("FkSpectraDefinition requires slowCountX > 0", e.getMessage());
  }

  @Test
  public void testBuildNegativeSlowCountY() {
    IllegalStateException e = assertThrows(IllegalStateException.class,
        () -> definition.toBuilder()
            .setSlowCountY(-1)
            .build());
    assertEquals("FkSpectraDefinition requires slowCountY > 0", e.getMessage());
  }

  @Test
  public void testBuildNegativeWaveformSampleRateHz() {
    IllegalStateException e = assertThrows(IllegalStateException.class,
        () -> definition.toBuilder()
            .setWaveformSampleRateHz(-2)
            .build());
    assertEquals("FkSpectraDefinition requires waveformSampleRateHz > 0.0", e.getMessage());
  }

  @Test
  public void testBuildNegativeWaveformSampleRateToleranceHz() {
    IllegalStateException e = assertThrows(IllegalStateException.class,
        () -> definition.toBuilder()
            .setWaveformSampleRateToleranceHz(-2)
            .build());
    assertEquals("FkSpectraDefinition requires waveformSampleRateToleranceHz >= 0.0", e.getMessage());
  }
}
