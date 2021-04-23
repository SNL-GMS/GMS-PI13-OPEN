package gms.shared.frameworks.osd.control.utils;

import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeMap;
import com.google.common.collect.TreeRangeSet;
import gms.shared.frameworks.osd.coi.waveforms.Waveform;
import gms.shared.frameworks.osd.coi.waveforms.util.WaveformUtility;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.Validate;

/**
 * Utility class for resolving waveforms that overlap in time.
 */
public class WaveformOverlapResolver {

  static final double SAMPLE_RATE_PERCENT_TOLERANCE = 0.5;

  private static final double MERGE_SAMPLE_RATE_TOLERANCE = 2.0;
  private static final double MERGE_MIN_GAP_SAMPLE_COUNT_LIMIT = 0.9;

  private WaveformOverlapResolver() {
  }

  /**
   * Resolves waveforms that may overlap in time into a non-overlapping list of waveforms in time
   * order. Overlaps are resolved by taking the newest stored samples.  Ties in storage time are
   * broken arbitrarily.  The resulting de-conflicted waveforms are combined
   * before returning them by calling {@link WaveformUtility#mergeWaveforms}.
   *
   * @param waveformToStorageTime a mapping between waveforms and their storage time
   * @return a list of {@link Waveform} that is non-null, sorted by start time, and contains no
   * overlaps.
   */
  public static List<Waveform> resolve(Map<Waveform, Instant> waveformToStorageTime) {
    Validate.notNull(waveformToStorageTime, "waveformToStorageTime null");
    if (waveformToStorageTime.size() <= 1) {
      return immutableList(waveformToStorageTime.keySet());
    }
    final RangeMap<Instant, Waveform> resolved = TreeRangeMap.create();
    final List<Waveform> newestToOldestWfs = sortedByDescValues(waveformToStorageTime);
    for (Waveform wf : newestToOldestWfs) {
      final Range<Instant> wfRange = wf.computeTimeRange();
      final Map<Range<Instant>, Waveform> overlaps = resolved.subRangeMap(wfRange).asMapOfRanges();
      if (overlaps.isEmpty()) {
        resolved.put(wf.computeTimeRange(), wf);
      } else {
        overlaps.values().forEach(overlap -> validateSampleRatesClose(wf, overlap));
        final RangeSet<Instant> resolvedRanges = removeRanges(wfRange, overlaps.keySet());
        window(wf, resolvedRanges).forEach(x -> resolved.put(x.computeTimeRange(), x));
      }
    }
    return immutableList(WaveformUtility.mergeWaveforms(
        immutableList(resolved.asMapOfRanges().values()),
        MERGE_SAMPLE_RATE_TOLERANCE, MERGE_MIN_GAP_SAMPLE_COUNT_LIMIT));
  }

  /**
   * Returns a list of the map keys sorted in descending order by their corresponding map value
   */
  private static <K, V extends Comparable<V>> List<K> sortedByDescValues(Map<K, V> m) {
    return m.entrySet().stream()
        .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
        .map(Entry::getKey)
        .collect(Collectors.toList());
  }

  /**
   * Removes the collection of ranges from the given range.
   */
  private static RangeSet<Instant> removeRanges(Range<Instant> range,
      Collection<Range<Instant>> removals) {
    final RangeSet<Instant> newRanges = TreeRangeSet.create(Set.of(range));
    // need to use slightlyExpanded because we don't want waveforms to have any touching samples
    // (two samples with same time), but Range removals allow just that.
    newRanges.removeAll(TreeRangeSet.create(slightlyExpanded(removals)));
    return newRanges;
  }

  /**
   * Slightly expands a range by one nano in each direction.  Only works on 'closed' Range's.
   */
  private static Collection<Range<Instant>> slightlyExpanded(Collection<Range<Instant>> ranges) {
    return ranges.stream()
        .map(r -> Range.closed(r.lowerEndpoint().minusNanos(1), r.upperEndpoint().plusNanos(1)))
        .collect(Collectors.toList());
  }

  /**
   * Windows the given waveform around the given set of time ranges
   */
  private static List<Waveform> window(Waveform wf, RangeSet<Instant> rangeSet) {
    return rangeSet.asRanges().stream()
        .map(tr -> wf.window(tr.lowerEndpoint(), tr.upperEndpoint()))
        .collect(Collectors.toList());
  }

  private static List<Waveform> immutableList(Collection<Waveform> c) {
    return Collections.unmodifiableList(new ArrayList<>(c));
  }

  private static void validateSampleRatesClose(Waveform wf1, Waveform wf2) {
    Validate.isTrue(sampleRatesClose(wf1.getSampleRate(), wf2.getSampleRate()),
        String.format("The two Waveform's from [%s, %s] and [%s, %s] overlap "
                + "and have substantially different sample rates %f and %f",
            wf1.getStartTime(), wf1.getEndTime(), wf2.getStartTime(),
            wf2.getEndTime(), wf1.getSampleRate(), wf2.getSampleRate()));
  }

  static boolean sampleRatesClose(double sr1, double sr2) {
    return percentChange(sr1, sr2) <= SAMPLE_RATE_PERCENT_TOLERANCE;
  }

  private static double percentChange(double d1, double d2) {
    final double absDiff = Math.abs(d1 - d2);
    return (absDiff / Math.min(d1, d2)) * 100.0;
  }
}
