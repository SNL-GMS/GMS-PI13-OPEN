package gms.core.dataacquisition;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Collectors.toSet;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.Sets;
import com.google.common.collect.TreeRangeMap;
import gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssue.AcquiredChannelEnvironmentIssueType;
import gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssueBoolean;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AceiBooleanRangeMap {

  private static final Logger logger = LoggerFactory.getLogger(AceiBooleanRangeMap.class);

  private final String channelName;
  private final AcquiredChannelEnvironmentIssueType type;
  private final RangeMap<Instant, AcquiredChannelEnvironmentIssueBoolean> aceiByTimeRange;
  private final Duration mergeTolerance;
  private final boolean keepUUIDs;

  private int numConflicts;
  private final SummaryStatistics summaryStatistics = new SummaryStatistics();

  public AceiBooleanRangeMap(String channelName,
      AcquiredChannelEnvironmentIssueType type,
      Duration mergeTolerance,
      boolean keepUUIDs) {
    this.channelName = checkNotNull(channelName);
    this.type = checkNotNull(type);
    this.aceiByTimeRange = TreeRangeMap.create();
    this.mergeTolerance = checkNotNull(mergeTolerance);
    this.keepUUIDs = keepUUIDs;
  }

  /**
   * Get statistics collected from calling put with an ACEI to be inserted or merged.
   * @param reset
   * @return
   */
  public PutStats getPutStats(boolean reset) {
    PutStats putStats = new PutStats(numConflicts, (int) summaryStatistics.getN(),
        summaryStatistics.getMin(), summaryStatistics.getMax(), summaryStatistics.getMean());
    if (reset) {
      numConflicts = 0;
      summaryStatistics.clear();
    }
    return putStats;
  }

  /**
   * Obtain the internal range map. Package-protected to use in a unit test.
   */
  RangeMap<Instant, AcquiredChannelEnvironmentIssueBoolean> internalMap() {
    return aceiByTimeRange;
  }

  /**
   * Get the number of key-value pairs stored.
   */
  public int size() {
    return aceiByTimeRange.asMapOfRanges().size();
  }

  /**
   * Clear all data.
   */
  public void clear() {
    aceiByTimeRange.clear();
  }

  public void clearBefore(Instant instant) {
    checkNotNull(instant, "instant cannot be null");

    // Collect ranges the fall completely before instant.
    List<Range<Instant>> toClear = new ArrayList<>();
    for (Range<Instant> range : aceiByTimeRange.asMapOfRanges().keySet()) {
      if (range.upperEndpoint().isBefore(instant)) {
        toClear.add(range);
      } else {
        break;
      }
    }

    for (Range<Instant> range : toClear) {
      aceiByTimeRange.remove(range);
    }
  }

  public Update put(AcquiredChannelEnvironmentIssueBoolean acei) {
    checkArgument(acei.getChannelName().equals(channelName),
        "Cannot insert ACEI, channel name %s does not match expected %s",
        acei.getChannelName(), channelName);
    checkArgument(acei.getType().equals(type),
        "Cannot insert ACEI, Type %s does not match expected %s",
        acei.getType(), type);

    return put(acei, true);
  }

  private Update put(AcquiredChannelEnvironmentIssueBoolean acei, boolean checkConflicts) {

    // The range for the acei should not overlap ranges for aceis already in the range map.
    // If it does, just fill in the gaps between existing aceis with the acei values for
    // this acei.
    if (checkConflicts) {

      // Closed-open range from [starttime - endtime)
      Range<Instant> aceiRange = aceiRange(acei);

      // Get a slice of the map only covering that range.
      Map<Range<Instant>, AcquiredChannelEnvironmentIssueBoolean> mapOfRanges =
          aceiByTimeRange.subRangeMap(aceiRange).asMapOfRanges();

      // If it's not empty, then an acei has arrived that overlaps aceis already in the map.
      // This probably isn't a normal situation, but handle it.
      if (!mapOfRanges.isEmpty()) {

        numConflicts++;

        // Need to specially handle the case where the acei overlaps previously handled aceis.
        return putWithConflicts(acei, aceiRange, mapOfRanges);

      }
    }

    // If execution has reached this point, either checkConflicts was false or the acei's range
    // didn't overlap any of the ranges already in the range map.

    // Get the part of the range map that overlaps the insertion range, where the insertion
    // range is [acei.startTime() - mergeTolerance .. acei.endTime() + mergeTolerance]
    RangeMap<Instant, AcquiredChannelEnvironmentIssueBoolean> subRange = aceiByTimeRange
        .subRangeMap(aceiInsertionRange(acei));

    final Map<Range<Instant>, AcquiredChannelEnvironmentIssueBoolean> mapOfRanges =
        subRange.asMapOfRanges();

    AcquiredChannelEnvironmentIssueBoolean aceiToUse = aceiOrCloseCopy(acei);

    // potential cases
    // nothing in subrange => free to insert with no merge.
    if (mapOfRanges.isEmpty()) {
      aceiByTimeRange.put(aceiRange(aceiToUse), aceiToUse);
      return new Update(Set.of(aceiToUse), Collections.emptySet());
    }

    // will have to determine what to keep/remove/merge
    else {

      Update update = mergeWithNeighbors(aceiToUse, mapOfRanges.values());

      AtomicLong mergeGapMillis = new AtomicLong(-1L);

      update.getRemovedAceis().forEach(issue -> {
        Range<Instant> range = aceiByTimeRange.getEntry(issue.getStartTime()).getKey();
        aceiByTimeRange.remove(range);

        // Only collect the merge gap when checkConflicts is true, because that means that this
        // method was not called from putWithConflicts(). Only collect merge gap stats for
        // aceis with no overlap. Only collect the merge gap when it's merged with another acei
        // coming just before its start time.
        if (checkConflicts) {
          // Don't use isBefore() because the end time might be equal to the start time.
          if (!issue.getEndTime().isAfter(aceiToUse.getStartTime())) {
            mergeGapMillis.set(
                Duration.between(issue.getEndTime(), aceiToUse.getStartTime()).toMillis()
            );
          }
        }
      });

      if (checkConflicts) {
        long ms = mergeGapMillis.get();
        if (ms >= 0L) {
          summaryStatistics.addValue(ms);
        }
      }

      update.getInsertedAceis().forEach(issue ->
          aceiByTimeRange.put(aceiRange(issue), issue)
      );

      return update;
    }
  }

  /**
   * This method handles the case of a new ACEI arriving that contradicts ACEIs already merged into
   * the range map.
   */
  @SuppressWarnings("unchecked")
  private Update putWithConflicts(
      AcquiredChannelEnvironmentIssueBoolean acei,
      Range<Instant> aceiRange,
      Map<Range<Instant>, AcquiredChannelEnvironmentIssueBoolean> mapOfRanges) {

    // This contains all possible ACEIs that can be included in accumulated removals
    // when the updates are combined.
    Set<AcquiredChannelEnvironmentIssueBoolean> aceisInInsertionRangeAPriori =
        new HashSet<>(aceiByTimeRange.subRangeMap(
            aceiInsertionRange(acei)).asMapOfRanges().values()
        );

    Set<Range<Instant>> rangeSet = mapOfRanges.keySet();
    Range<Instant>[] rangeArray = rangeSet.toArray(new Range[rangeSet.size()]);

    List<Update> updates = new ArrayList<>();

    if (aceiRange.lowerEndpoint().isBefore(rangeArray[0].lowerEndpoint())) {
      AcquiredChannelEnvironmentIssueBoolean aceiBefore =
          AcquiredChannelEnvironmentIssueBoolean.create(acei.getChannelName(), acei.getType(),
              acei.getStartTime(), rangeArray[0].lowerEndpoint(), acei.getStatus());
      updates.add(put(aceiBefore, false));
    }

    int lim = rangeArray.length - 1;
    for (int i = 0; i < lim; i++) {
      if (rangeArray[i].upperEndpoint().isBefore(rangeArray[i + 1].lowerEndpoint())) {
        AcquiredChannelEnvironmentIssueBoolean aceiBetween =
            AcquiredChannelEnvironmentIssueBoolean.create(acei.getChannelName(), acei.getType(),
                rangeArray[i].upperEndpoint(), rangeArray[i + 1].lowerEndpoint(),
                acei.getStatus());
        updates.add(put(aceiBetween, false));
      }
    }

    if (rangeArray[rangeArray.length - 1].upperEndpoint().isBefore(aceiRange.upperEndpoint())) {
      AcquiredChannelEnvironmentIssueBoolean aceiAfter =
          AcquiredChannelEnvironmentIssueBoolean.create(acei.getChannelName(), acei.getType(),
              rangeArray[rangeArray.length - 1].upperEndpoint(), aceiRange.upperEndpoint(),
              acei.getStatus());
      updates.add(put(aceiAfter, false));
    }

    Update update = combineUpdates(updates, aceisInInsertionRangeAPriori);

    // Log a warning message.
    if (logger.isWarnEnabled()) {
      int numInserts = update.getInsertedAceis().size();
      int numRemovals = update.getRemovedAceis().size();
      logger.warn(
          "Boolean ACEI received that overlaps ranges of previously received ACEIs: {}, {}, {}, {}. {} ACEIs to be removed, {} ACEIs to be inserted",
          acei.getChannelName(), acei.getType(), acei.getStartTime(), acei.getEndTime(),
          numRemovals, numInserts);
    }

    return update;
  }

  /**
   * Combines a list of updates generated from a series of calls to put.
   * @param updates
   * @param aprioriAceis
   * @return
   */
  private Update combineUpdates(List<Update> updates,
      Set<AcquiredChannelEnvironmentIssueBoolean> aprioriAceis) {

    Set<AcquiredChannelEnvironmentIssueBoolean> inserted = new HashSet<>();
    Set<AcquiredChannelEnvironmentIssueBoolean> removed = new HashSet<>();

    for (Update update : updates) {
      // One update may have inserted an acei, then a subsequent update might remove it.
      inserted.removeAll(update.getRemovedAceis());
      inserted.addAll(update.getInsertedAceis());
      // Don't want to include in the removals in the final update aceis that weren't in the
      // range map prior to any of the updates.
      for (AcquiredChannelEnvironmentIssueBoolean acei : update.getRemovedAceis()) {
        if (aprioriAceis.contains(acei)) {
          removed.add(acei);
        }
      }
    }

    return new Update(inserted, removed);
  }

  /**
   * Returns either the acei itself (if keepUUIDs is true) or one that is identical except for the
   * id (if keepUUIDs is false.)
   */
  private AcquiredChannelEnvironmentIssueBoolean aceiOrCloseCopy(
      AcquiredChannelEnvironmentIssueBoolean acei) {
    return keepUUIDs ? acei :
        AcquiredChannelEnvironmentIssueBoolean.create(
            acei.getChannelName(),
            acei.getType(),
            acei.getStartTime(),
            acei.getEndTime(),
            acei.getStatus()
        );
  }

  private static Update mergeWithNeighbors(
      AcquiredChannelEnvironmentIssueBoolean acei,
      Collection<AcquiredChannelEnvironmentIssueBoolean> neighbors) {

    if (neighbors.isEmpty()) {
      return new Update(Set.of(acei), Collections.emptySet());
    }

    Set<AcquiredChannelEnvironmentIssueBoolean> toInsert = new LinkedHashSet<>();
    Set<AcquiredChannelEnvironmentIssueBoolean> toRemove = new LinkedHashSet<>();

    // Add all the neighbors, but may remove one or two if opposite status ones
    // precede of come after acei.
    toRemove.addAll(neighbors);

    Range<Instant> mergedRangeWithSameStatus = Stream.concat(Stream.of(acei), neighbors.stream())
        .filter(nbr -> Objects.equals(acei.getStatus(), nbr.getStatus()))
        .map(AceiBooleanRangeMap::aceiRange)
        .reduce(Range::span)
        // Can't happen, since the list neighbors is checked above.
        .orElseThrow(() -> new IllegalArgumentException("empty neighbor list"));

    boolean oppositeStatus = !acei.getStatus();
    Instant startTimeSameStatus = mergedRangeWithSameStatus.lowerEndpoint();
    Instant endTimeSameStatus = mergedRangeWithSameStatus.upperEndpoint();

    Optional<AcquiredChannelEnvironmentIssueBoolean> oppositeStartingBefore =
        withOppositeStatusStartingBefore(
            startTimeSameStatus, oppositeStatus, neighbors);

    Optional<AcquiredChannelEnvironmentIssueBoolean> oppositeEndingAfter =
        withOppositeStatusEndingAfter(
            endTimeSameStatus, oppositeStatus, neighbors
        );

    if (oppositeStartingBefore.isPresent()) {

      AcquiredChannelEnvironmentIssueBoolean opIssue = oppositeStartingBefore.get();

      if (opIssue.getEndTime().isAfter(startTimeSameStatus)) {
        // Will need to remove opIssue and make a new one with an updated end time.
        toInsert.add(AcquiredChannelEnvironmentIssueBoolean.create(
            opIssue.getChannelName(),
            opIssue.getType(),
            opIssue.getStartTime(),
            startTimeSameStatus,
            opIssue.getStatus()
        ));
      } else {
        // Keep opIssue, since it's period doesn't overlap acei.
        toRemove.remove(opIssue);
      }
    }

    if (oppositeEndingAfter.isPresent()) {

      AcquiredChannelEnvironmentIssueBoolean opIssue = oppositeEndingAfter.get();

      if (opIssue.getStartTime().isBefore(endTimeSameStatus)) {
        toInsert.add(AcquiredChannelEnvironmentIssueBoolean.create(
            opIssue.getChannelName(),
            opIssue.getType(),
            endTimeSameStatus,
            opIssue.getEndTime(),
            opIssue.getStatus()
        ));
      } else {
        // Keep it after all.
        toRemove.remove(opIssue);
      }
    }

    if (startTimeSameStatus.equals(acei.getStartTime()) && endTimeSameStatus
        .equals(acei.getEndTime())) {
      toInsert.add(acei);
    } else {
      toInsert.add(AcquiredChannelEnvironmentIssueBoolean.create(
          acei.getChannelName(),
          acei.getType(),
          startTimeSameStatus,
          endTimeSameStatus,
          acei.getStatus()
      ));
    }

    return new Update(toInsert, toRemove);
  }

  static Optional<AcquiredChannelEnvironmentIssueBoolean> withOppositeStatusStartingBefore(
      Instant startTime,
      boolean oppositeStatus,
      Collection<AcquiredChannelEnvironmentIssueBoolean> neighbors
  ) {

    Optional<AcquiredChannelEnvironmentIssueBoolean> earliestOpposite =
        neighbors.stream()
            .filter(nbr -> oppositeStatus == nbr.getStatus())
            .sorted((issue1, issue2) ->
                issue1.getStartTime().compareTo(issue2.getStartTime()))
            .findFirst();

    return earliestOpposite.isPresent()
        && earliestOpposite.get().getStartTime().compareTo(startTime) < 0 ?
        earliestOpposite : Optional.empty();
  }

  static Optional<AcquiredChannelEnvironmentIssueBoolean> withOppositeStatusEndingAfter(
      Instant endTime,
      boolean oppositeStatus,
      Collection<AcquiredChannelEnvironmentIssueBoolean> neighbors
  ) {

    Optional<AcquiredChannelEnvironmentIssueBoolean> latestOpposite =
        neighbors.stream()
            .filter(nbr -> oppositeStatus == nbr.getStatus())
            .sorted((issue1, issue2) ->
                issue2.getEndTime().compareTo(issue1.getEndTime()))
            .findFirst();

    return latestOpposite.isPresent() && latestOpposite.get().getEndTime().compareTo(endTime) > 0 ?
        latestOpposite : Optional.empty();
  }

  /**
   * Returns a closed range from the issue's start time minus the merge tolerance to its endtime
   * plus the merge tolerance.
   */
  private Range<Instant> aceiInsertionRange(AcquiredChannelEnvironmentIssueBoolean acei) {
    return Range.closed(
        // Substract an addition nanosecond so a CLOSED-OPEN range just before will still be
        // included.
        acei.getStartTime().minus(mergeTolerance).minusNanos(1L),
        acei.getEndTime().plus(mergeTolerance)
    );
  }

  /**
   * Returns the closed range from the start time of the issue to its end time.
   */
  private static Range<Instant> aceiRange(AcquiredChannelEnvironmentIssueBoolean acei) {
    return Range.closedOpen(acei.getStartTime(), acei.getEndTime());
  }

  public Optional<AcquiredChannelEnvironmentIssueBoolean> get(Instant aceiTime) {
    AcquiredChannelEnvironmentIssueBoolean acei = aceiByTimeRange.get(aceiTime);
    // An ACEI might be in the map with the closed-open range [startTime, aceiTime). But if there's
    // no other ACEI associated with the range [aceiTime, endTime), return the one associated with
    // [startTime, aceiTime)
    if (acei == null) {
      // Subtract a nanosecond to put it within the range.
      AcquiredChannelEnvironmentIssueBoolean possibleAcei =
          aceiByTimeRange.get(aceiTime.minusNanos(1L));
      // And confirm that the endtime is aceiTime.
      if (possibleAcei != null && aceiTime.equals(possibleAcei.getEndTime())) {
        acei = possibleAcei;
      }
    }
    return Optional.ofNullable(acei);
  }

  public static class Update {

    private final ImmutableSet<AcquiredChannelEnvironmentIssueBoolean> insertedAceis;
    private final ImmutableSet<AcquiredChannelEnvironmentIssueBoolean> removedAceis;

    public Update(
        Collection<AcquiredChannelEnvironmentIssueBoolean> insertedAceis,
        Collection<AcquiredChannelEnvironmentIssueBoolean> removedAceis) {
      this.insertedAceis = ImmutableSet.copyOf(insertedAceis);
      this.removedAceis = ImmutableSet.copyOf(removedAceis);
    }

    public Set<AcquiredChannelEnvironmentIssueBoolean> getInsertedAceis() {
      return insertedAceis;
    }

    public Stream<AcquiredChannelEnvironmentIssueBoolean> insertedAceis() {
      return getInsertedAceis().stream();
    }

    public Set<AcquiredChannelEnvironmentIssueBoolean> getRemovedAceis() {
      return removedAceis;
    }

    public Stream<AcquiredChannelEnvironmentIssueBoolean> removedAceis() {
      return getRemovedAceis().stream();
    }

    public Update merge(Update other) {
      Set<AcquiredChannelEnvironmentIssueBoolean> mergedInserted = Sets
          .union(getInsertedAceis(), other.getInsertedAceis());

      Set<AcquiredChannelEnvironmentIssueBoolean> mergedRemoved = Sets
          .union(getRemovedAceis(), other.getRemovedAceis());

      Set<AcquiredChannelEnvironmentIssueBoolean> redundantAcei = Sets
          .intersection(mergedInserted, mergedRemoved);

      return new Update(Sets.difference(mergedInserted, redundantAcei).immutableCopy(),
          Sets.difference(mergedRemoved, redundantAcei).immutableCopy());
    }

    public static Update merge(Collection<Update> updates) {
      Set<AcquiredChannelEnvironmentIssueBoolean> mergedInserted = updates.stream()
          .flatMap(Update::insertedAceis)
          .collect(toSet());

      Set<AcquiredChannelEnvironmentIssueBoolean> mergedRemoved = updates.stream()
          .flatMap(Update::removedAceis)
          .collect(toSet());

      Set<AcquiredChannelEnvironmentIssueBoolean> redundantAcei = Sets
          .intersection(mergedInserted, mergedRemoved);

      return new Update(Sets.difference(mergedInserted, redundantAcei).immutableCopy(),
          Sets.difference(mergedRemoved, redundantAcei).immutableCopy());

    }
  }

  /**
   * Encapsulates merge statistics.
   */
  public static class PutStats {

    private final int numConflicts;
    private final int numMerges;
    private final double minMergeGapMillis;
    private final double maxMergeGapMillis;
    private final double meanMergeGapMillis;

    /**
     * Constructor
     *
     * @param numConflicts
     * @param numMerges
     * @param minMergeGapMillis
     * @param maxMergeGapMillis
     * @param meanMergeGapMillis
     */
    public PutStats(int numConflicts, int numMerges, double minMergeGapMillis, double maxMergeGapMillis,
        double meanMergeGapMillis) {
      this.numConflicts = numConflicts;
      this.numMerges = numMerges;
      this.minMergeGapMillis = minMergeGapMillis;
      this.maxMergeGapMillis = maxMergeGapMillis;
      this.meanMergeGapMillis = meanMergeGapMillis;
    }

    /**
     * Get the number of conflicts. This is the number of times put was called with an ACEI whose
     * time range overlapped ACEIs already in the range map.
     * @return
     */
    public int getNumConflicts() {
      return numConflicts;
    }

    /**
     * This is the minimum merge gap when put was called with an ACEI that did not overlap
     * ACEIs already in the range map, but was merged with another ACEI that came
     * immediately before it.
     * @return
     */
    public double getMinMergeGapMillis() {
      return minMergeGapMillis;
    }

    /**
     * This is the maximum merge gap when put was called with an ACEI that did not overlap
     * ACEIs already in the range map, but was merged with another ACEI that came
     * immediately before it.
     * @return
     */
    public double getMaxMergeGapMillis() {
      return maxMergeGapMillis;
    }

    /**
     * This is the mean merge gap when put was called with an ACEI that did not overlap
     * ACEIs already in the range map, but was merged with another ACEI that came
     * immediately before it.
     * @return
     */
    public double getMeanMergeGapMillis() {
      return meanMergeGapMillis;
    }

    /**
     * This is the number of times put was called with an ACEI that did not overlap
     * ACEIs already in the range map, but was merged with another ACEI that came
     * immediately before it.
     * @return
     */
    public int getNumMerges() {
      return numMerges;
    }
  }

}
