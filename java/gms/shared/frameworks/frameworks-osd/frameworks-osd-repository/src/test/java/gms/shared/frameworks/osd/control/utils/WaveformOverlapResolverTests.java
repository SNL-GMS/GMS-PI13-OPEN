package gms.shared.frameworks.osd.control.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gms.shared.frameworks.osd.coi.waveforms.Waveform;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.Validate;
import org.junit.jupiter.api.Test;

class WaveformOverlapResolverTests {

  private static final double DEFAULT_SAMPLE_RATE = 1.0;

  @Test
  void testResolveEmptyMapReturnsEmptyList() {
    assertEquals(List.of(), WaveformOverlapResolver.resolve(Map.of()));
  }

  @Test
  void testResolveSingleWaveformReturnsWaveformAsIs() {
    final Waveform wf = Waveform.from(Instant.EPOCH, 1.0, new double[]{1.2});
    assertEquals(List.of(wf), WaveformOverlapResolver.resolve(
        Map.of(wf, Instant.EPOCH)));
  }

  @Test
  void testResolveNoOverlapReturnsWaveformAsIs() {
    final Map<Waveform, Instant> input = twoWaveforms(1, DEFAULT_SAMPLE_RATE);
    assertEquals(sorted(input.keySet()), WaveformOverlapResolver.resolve(input));
  }

  @Test
  void testResolveNoOverlapSampleRatesFarApartReturnsWaveformAsIs() {
    final Map<Waveform, Instant> input = twoWaveforms(1, DEFAULT_SAMPLE_RATE + 1000000);
    assertEquals(sorted(input.keySet()), WaveformOverlapResolver.resolve(input));
  }

  @Test
  void testResolveOverlapSampleRatesFarApartThrowsException() {
    final double offSampleRate =
        (1 + (WaveformOverlapResolver.SAMPLE_RATE_PERCENT_TOLERANCE / 100.0) + 1e-9)
            * DEFAULT_SAMPLE_RATE;
    assertIllegalArgumentException("substantially different sample rates",
        twoWaveforms(-1, offSampleRate));
  }

  @Test
  void testResolve() {
    // wf2 overlaps wf1 by a bit over two samples, and is stored more recently.
    // wf3 is sufficiently later than wf2 such that there will remain a gap there.
    // wf4 is one sample after the end of wf3 such that they should end up being contiguous together.
    // wf5 overlaps wf4 by two sample but wf4 is stored more recently so those two samples come from wf4.
    final double[] values1 = new double[]{1.0, 2.0, 3.0};
    final double[] values2 = new double[]{4.0, 5.0, 6.0};
    final double[] values3 = new double[]{7.0, 8.0, 9.0};
    final double[] values4 = new double[]{10.0, 11.0, 12.0};
    final double[] values5 = new double[]{13.0, 14.0, 15.0};
    final Waveform wf1 = Waveform.from(Instant.EPOCH, DEFAULT_SAMPLE_RATE, values1);
    final Waveform wf2 = Waveform
        .from(wf1.getEndTime().minusNanos(wf1.getSamplePeriod().toNanos() + 1),
            DEFAULT_SAMPLE_RATE, values2);
    final Waveform wf3 = Waveform
        .from(wf2.getEndTime().plusSeconds(30), DEFAULT_SAMPLE_RATE, values3);
    final Waveform wf4 = Waveform.from(wf3.getEndTime().plus(wf3.getSamplePeriod()),
        DEFAULT_SAMPLE_RATE, values4);
    final Waveform wf5 = Waveform.from(wf4.getEndTime().minus(wf4.getSamplePeriod()),
        DEFAULT_SAMPLE_RATE, values5);
    final Instant storageTime1 = Instant.EPOCH.plusSeconds(600);
    final Instant storageTime2 = storageTime1.plusNanos(1);
    final Instant storageTime3 = storageTime2.plusNanos(1);
    final Map<Waveform, Instant> input = Map.of(
        wf1, storageTime1,
        wf2, storageTime2,
        wf3, storageTime2,
        wf4, storageTime3,
        wf5, storageTime2);
    final List<Waveform> expected = List.of(
        Waveform
            .from(wf1.getStartTime(), DEFAULT_SAMPLE_RATE, new double[]{1.0, 4.0, 5.0, 6.0}),
        Waveform.from(wf3.getStartTime(), DEFAULT_SAMPLE_RATE,
            new double[]{7.0, 8.0, 9.0, 10.0, 11.0, 12.0, 15.0}));
    assertResolvesAsExpected(input, expected);
  }

  @Test
  void testResolveMultipleOverlaps() {
    // 4 waveforms: wf2 overlaps the end of wf1, all of wf3, and the beginning of wf4.
    final double[] values1 = new double[]{1.0, 2.0, 3.0};
    final double[] values2 = new double[]{4.0, 5.0, 6.0};
    final double[] values3 = new double[]{7.0, 8.0};
    final double[] values4 = new double[]{9.0, 10.0, 11.0};
    final Waveform wf1 = Waveform.from(Instant.EPOCH, DEFAULT_SAMPLE_RATE, values1);
    final Waveform wf2 = Waveform
        .from(wf1.getEndTime().minusNanos(1), DEFAULT_SAMPLE_RATE, values2);
    final Waveform wf3 = Waveform.from(wf2.getStartTime().plusNanos(1),
        DEFAULT_SAMPLE_RATE, values3);
    final Waveform wf4 = Waveform
        .from(wf2.getEndTime().minusNanos(1), DEFAULT_SAMPLE_RATE, values4);
    final Instant storageTime1 = Instant.EPOCH.plusSeconds(600);
    final Instant storageTime2 = storageTime1.plusNanos(1);
    final Instant storageTime3 = storageTime2.plusNanos(1);
    final Map<Waveform, Instant> input = Map.of(
        wf1, storageTime1,
        wf2, storageTime2,
        wf3, storageTime3,
        wf4, storageTime1
    );
    final List<Waveform> expected = List.of(
        Waveform.from(wf1.getStartTime(), DEFAULT_SAMPLE_RATE,
            new double[]{1.0, 2.0, 7.0, 8.0, 6.0, 10.0, 11.0}));
    assertResolvesAsExpected(input, expected);
  }

  private static void assertResolvesAsExpected(Map<Waveform, Instant> input,
      List<Waveform> expected) {
    final List<Waveform> actual = WaveformOverlapResolver.resolve(input);
    final List<Waveform> withAdjustedSampleRates = withAdjustedSampleRates(actual);
    assertEquals(expected, withAdjustedSampleRates);
  }

  private static void assertIllegalArgumentException(String expectedMsg,
      Map<Waveform, Instant> waveformToStorageTime) {
    final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> WaveformOverlapResolver.resolve(waveformToStorageTime));
    final String errorMsg = ex.getMessage();
    assertTrue(errorMsg.contains(expectedMsg),
        String.format("Expected exception to contain '%s' but was '%s'", expectedMsg, errorMsg));
  }

  private static Map<Waveform, Instant> twoWaveforms(long waveformOffsetNanos, double sampleRate2) {
    final Waveform wf1 = Waveform
        .from(Instant.EPOCH, DEFAULT_SAMPLE_RATE, new double[]{1.0, 2.0, 3.0});
    final Waveform wf2 = Waveform.from(wf1.getEndTime().plusNanos(waveformOffsetNanos),
        sampleRate2, new double[]{4.0, 5.0, 6.0});
    final Instant storageTime = Instant.EPOCH;
    return Map.of(wf1, storageTime, wf2, storageTime);
  }

  private static List<Waveform> withAdjustedSampleRates(List<Waveform> wfs) {
    return wfs.stream().map(wf -> useNominalSampleRateIfClose(wf))
        .collect(Collectors.toList());
  }

  private static Waveform useNominalSampleRateIfClose(Waveform wf) {
    final double sr = wf.getSampleRate();
    Validate.isTrue(WaveformOverlapResolver.sampleRatesClose(sr, DEFAULT_SAMPLE_RATE),
        String.format(
            "Expected waveforms to be close to nominal sample rate, but one has %f while nominal is %f",
            sr, DEFAULT_SAMPLE_RATE));
    return Waveform.from(wf.getStartTime(), DEFAULT_SAMPLE_RATE, wf.getValues());
  }

  private static List<Waveform> sorted(Collection<Waveform> c) {
    final List<Waveform> l = new ArrayList<>(c);
    Collections.sort(l);
    return l;
  }
}
