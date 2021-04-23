package gms.core.performancemonitoring.soh.control;

import gms.core.performancemonitoring.soh.control.configuration.ChannelSohDefinition;
import gms.core.performancemonitoring.soh.control.configuration.DurationSohMonitorStatusThresholdDefinition;
import gms.core.performancemonitoring.soh.control.configuration.PercentSohMonitorStatusThresholdDefinition;
import gms.core.performancemonitoring.soh.control.configuration.StationSohDefinition;
import gms.core.performancemonitoring.soh.control.configuration.TimeWindowDefinition;
import gms.core.performancemonitoring.soh.control.reactor.ReactorUtility;
import gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssue;
import gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssue.AcquiredChannelEnvironmentIssueType;
import gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssueBoolean;
import gms.shared.frameworks.osd.coi.soh.ChannelSoh;
import gms.shared.frameworks.osd.coi.soh.DurationSohMonitorValueAndStatus;
import gms.shared.frameworks.osd.coi.soh.PercentSohMonitorValueAndStatus;
import gms.shared.frameworks.osd.coi.soh.SohMonitorType;
import gms.shared.frameworks.osd.coi.soh.SohMonitorValueAndStatus;
import gms.shared.frameworks.osd.coi.soh.SohStatus;
import gms.shared.frameworks.osd.coi.waveforms.WaveformSummary;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


/**
 * Utility class for calculating State of health averages, rollups, summaries, etc.
 */
class ChannelSohCalculationUtility {

  // We must use apache log4j to have custom log levels
  private static final Logger logger = LogManager.getLogger(ChannelSohCalculationUtility.class);

  private static final Level SOH_TIMING_LEVEL = Level.getLevel("SOH_TIMING");

  private static final boolean LOGGER_DEBUG_ENABLED = logger.isDebugEnabled();

  public static final String BACK_OFF_DURATION_MAY_NOT_BE_NEGATIVE = "backOffDuration may not be negative";

  private Instant now;

  private AcquiredSampleTimesByChannel acquiredSampleTimesByChannel;


  /**
   * Instantiate with given "now" time. Used for testing!
   */
  ChannelSohCalculationUtility(Instant now,
      AcquiredSampleTimesByChannel acquiredSampleTimesByChannel) {
    this.now = now;
    this.acquiredSampleTimesByChannel = acquiredSampleTimesByChannel;
  }

  /**
   * Instantiate with what time it is right now.
   */
  ChannelSohCalculationUtility(AcquiredSampleTimesByChannel acquiredSampleTimesByChannel) {
    this.now = Instant.now();
    this.acquiredSampleTimesByChannel = acquiredSampleTimesByChannel;
  }


  /**
   * From a set of AcquiredStationSohExtracts and a single StationSohDefinition, construct a set of
   * ChannelSoh objects for the single station specified in stationSohDefinition.
   *
   * @param waveformSummaryAndReceptionTimesMono map with channel names as keys mapped to each
   * channel's {@link WaveformSummaryAndReceptionTime}s
   * @param aceiBooleanMapMono map with channel names as keys mapped to each channel's {@link
   * AcquiredChannelEnvironmentIssueBoolean}s
   * @param stationSohDefinition definition for single station
   * @return set of ChannelSoh objects
   */
  Mono<Set<ChannelSoh>> buildChannelSohSetMono(
      Mono<Map<String, Set<WaveformSummaryAndReceptionTime>>> waveformSummaryAndReceptionTimesMono,
      Mono<Map<String, Set<AcquiredChannelEnvironmentIssueBoolean>>> aceiBooleanMapMono,
      StationSohDefinition stationSohDefinition,
      Instant stationSohTime
  ) {

    //
    // Get UnicastProcessors that are subscribed to the result of each type of computation. Thus the
    // three types of computation (environment, lag, missing, and timeliness) will happen asynchronously.
    //
    return Flux.zip(
        ReactorUtility.getMonoSubscribedProcessor(
            aceiBooleanMapMono,
            this::stationChannelEnvironmentStatuses,
            stationSohDefinition
        ),
        ReactorUtility.getMonoSubscribedProcessor(
            waveformSummaryAndReceptionTimesMono,
            this::stationChannelLagStatuses,
            stationSohDefinition
        ),
        ReactorUtility.getMonoSubscribedProcessor(
            waveformSummaryAndReceptionTimesMono,
            this::stationChannelMissingStatuses,
            stationSohDefinition
        ),
        ReactorUtility.getMonoSubscribedProcessor(
            waveformSummaryAndReceptionTimesMono,
            (waveformSummaryAndReceptionTimes, definition) ->
                stationChannelTimelinessStatuses(definition, stationSohTime),
            stationSohDefinition
        )
    ).map(
        tuple -> calculateChannelSohSet(
            tuple.getT1(),
            tuple.getT2(),
            tuple.getT3(),
            tuple.getT4(),
            stationSohDefinition
        )
    ).next();
  }


  /**
   * Computes the {@link DurationSohMonitorValueAndStatus} for {@link SohMonitorType#TIMELINESS}
   * from the collection of current (i.e., wall clock) time and the most recent sample time for this
   * channel.
   *
   * @param definition Contains the {@link SohStatus#GOOD} and {@link SohStatus#MARGINAL} thresholds
   * used to determine the status in the returned DurationSohMonitorValueAndStatus.
   * @param latestEndTime the most recent channel time for this channel
   * @param currentTime wall clock time
   * @return DurationSohMonitorValueAndStatus computed from the mostRecentChannelUpdate time and and
   * the currentTime.
   */
  DurationSohMonitorValueAndStatus timeliness(
      DurationSohMonitorStatusThresholdDefinition definition,
      Instant latestEndTime,
      Instant currentTime,
      String channelName) {

    if (latestEndTime.isAfter(currentTime)) {
      logger.debug(
          "Channel:{} has a latestEndTime:{} that is after the current time:{}! Cannot calculate TIMELINESS",
          channelName,
          latestEndTime, currentTime);
      return DurationSohMonitorValueAndStatus.from(
          Duration.between(latestEndTime, currentTime),
          SohStatus.BAD,
          SohMonitorType.TIMELINESS
      );
    }

    Duration value = Duration.between(latestEndTime, currentTime);
    SohStatus status = computeStatusFromThreshold(
        value, definition.getGoodThreshold(), definition.getMarginalThreshold());

    return DurationSohMonitorValueAndStatus.from(value, status, SohMonitorType.TIMELINESS);
  }


  /**
   * Computes {@link DurationSohMonitorValueAndStatus} for {@link SohMonitorType#LAG} from the
   * collection of {@link gms.shared.frameworks.osd.coi.waveforms.WaveformSummary} end times and the
   * associated {@link gms.shared.frameworks.osd.coi.waveforms.RawStationDataFrame} acquisition
   * times.  The lag value contained in the returned DurationSohMonitorValueAndStatus is the average
   * of all lag values.  Each individual lag value is calculated by subtracting the WaveformSummary
   * end time from the RawStationDataFrame acquisition time.
   *
   * @param waveformSummaryAndReceptionTimes Collection of WaveformSummary end times and associated
   * RawStationDataFrame acquisition times.
   * @param stationSohDefinition Contains the calculationInterval and backOffDuration
   * @param durationDefinition Contains the {@link SohStatus#GOOD} and {@link SohStatus#MARGINAL} thresholds
   * used to determine the status in the returned DurationSohMonitorValueAndStatus.
   * @return DurationSohMonitorValueAndStatus computed from the collection of WaveformSummary end
   * times and the associated RawStationDataFrame acquisition times, wrapped in an optional.
   */
  Optional<DurationSohMonitorValueAndStatus> lag(
      StationSohDefinition stationSohDefinition,
      DurationSohMonitorStatusThresholdDefinition durationDefinition,
      Collection<WaveformSummaryAndReceptionTime> waveformSummaryAndReceptionTimes
  ) {

    Objects.requireNonNull(
        waveformSummaryAndReceptionTimes,
        "Null waveformSummaryAndReceptionTimes"
    );

    Objects.requireNonNull(
        durationDefinition,
        "Null durationDefinition"
    );

    Objects.requireNonNull(
        stationSohDefinition,
        "Null stationSohDefinition"
    );

    Validate.isTrue(
        !waveformSummaryAndReceptionTimes.isEmpty(),
        "Empty waveformSummaryAndReceptionTimes"
    );

    return lag(
        stationSohDefinition.getTimeWindowBySohMonitorType().get(SohMonitorType.LAG),
        waveformSummaryAndReceptionTimes,
        Aggregator.getDurationMaximizer(),
        this.now)
        //
        // Create a DurationSohMonitorValueAndStatus using the max duration.
        //
        .map(maxDuration -> {
              Validate.isTrue(!maxDuration.isNegative(), "maxDuration may not be negative");

              return DurationSohMonitorValueAndStatus
                  .from(maxDuration,
                      computeStatusFromThreshold(
                          maxDuration,
                          durationDefinition.getGoodThreshold(),
                          durationDefinition.getMarginalThreshold()
                      ),
                      SohMonitorType.LAG);
            }
        );
  }

  static Optional<Duration> lag(
      TimeWindowDefinition definition,
      Collection<WaveformSummaryAndReceptionTime> waveformSummaryAndReceptionTimes,
      Aggregator<Duration> durationAggregator,
      Instant now
  ) {

    return filterTemporalOverlap(waveformSummaryAndReceptionTimes,
        (WaveformSummaryAndReceptionTime w) -> w.getWaveformSummary().getStartTime(),
        (WaveformSummaryAndReceptionTime w) -> w.getWaveformSummary().getEndTime(),
        definition.getCalculationInterval(),
        definition.getBackOffDuration(),
        now)
        .map(
            waveformSummaryAndReceptionTime -> Duration.between(
                waveformSummaryAndReceptionTime.getWaveformSummary().getEndTime(),
                waveformSummaryAndReceptionTime.getReceptionTime()
            )
        )
        //
        // Compare the durations using reduce to discover the max duration. An empty optional will
        // be returned if the list of waveformSummaryAndReceptionTimes is empty.
        //
        .collect(
            () -> durationAggregator,
            Aggregator::accept,
            Aggregator::combine
        ).aggregate();
  }

  /**
   * Computes list of {@link PercentSohMonitorValueAndStatus} for environment SohMonitorType (the
   * literal begins with ENV_)
   *
   * @param acquiredChannelEnvironmentIssues Collection] of AcquiredChannelEnvironmentIssueBooleans
   * @param definitionMap Contains the {@link SohStatus#GOOD} and {@link SohStatus#MARGINAL}
   * thresholds used to determine the status in the returned DurationSohMonitorValueAndStatus.
   * @return the PercentMonitorValueAndStatus calculated from the percentage of TRUE statuses inside
   * acquiredChannelEnvironmentIssues.
   */
  List<PercentSohMonitorValueAndStatus> environmentStatus(
      StationSohDefinition stationSohDefinition,
      Collection<AcquiredChannelEnvironmentIssueBoolean> acquiredChannelEnvironmentIssues,
      Map<SohMonitorType, PercentSohMonitorStatusThresholdDefinition> definitionMap
  ) {

    AtomicReference<String> channelName = new AtomicReference<>();

    return partition(
        acquiredChannelEnvironmentIssues.stream()
            //
            // Make sure all ACEIs are for just a single channel.
            //
            .map(acei -> {
              if (channelName.get() == null) {
                channelName.set(acei.getChannelName());
              } else {
                Validate.isTrue(
                    channelName.get().equals(acei.getChannelName()),
                    "All acquiredChannelEnvironmentIssues must be from the same Channel."
                );
              }
              return acei;
            })

            //
            // Filter out ACEI types that are not in config
            //
            .filter(acei -> {
              boolean isValidKey = definitionMap.containsKey(
                  acei.getType().getMatchingSohMonitorType());
              if (!isValidKey) {
                logger.info("Ignoring ACEI type {} not contained in configuration!",
                    acei.getType());
              }
              return isValidKey;
            }),

        AcquiredChannelEnvironmentIssue::getType)
        .entrySet().stream()
        .map(entry ->
            singleEnvironmentStatus(
                entry.getValue(),
                stationSohDefinition,
                definitionMap.get(entry.getKey().getMatchingSohMonitorType()),
                entry.getKey()))
        .collect(Collectors.toList());
  }


  /**
   * Computes {@link PercentSohMonitorValueAndStatus} for {@link SohMonitorType#MISSING} from the
   * start/end times of the acquired waveforms and the calculation interval (a system configuration
   * parameter).
   *
   * @param definition Contains the {@link SohStatus#GOOD} and {@link SohStatus#MARGINAL} thresholds
   * and the calculation interval.
   * @param waveformSummaryAndReceptionTimes Collection of {@link WaveformSummary} start/end times
   * and associated RawStationDataFrame acquisition times.
   * @return PercentSohMonitorValueAndStatus describing the current status with regard to {@link
   * SohStatus#MARGINAL} data.
   */
  PercentSohMonitorValueAndStatus missing(
      final StationSohDefinition stationSohDefinition,
      final PercentSohMonitorStatusThresholdDefinition definition,
      final Collection<WaveformSummaryAndReceptionTime> waveformSummaryAndReceptionTimes
  ) {

    Validate.notNull(definition, "Null definition");
    Validate.notNull(stationSohDefinition, "Null stationSohDefinition");
    Validate.isTrue(
        !stationSohDefinition.getTimeWindowBySohMonitorType().get(SohMonitorType.MISSING)
            .getBackOffDuration().isNegative(), BACK_OFF_DURATION_MAY_NOT_BE_NEGATIVE);
    Validate.notNull(waveformSummaryAndReceptionTimes, "Null waveformSummaryAndReceptionTimes");

    // Create List<WaveformSummary> from WaveformSummaryAndReceptionTimes,
    // filtering out waveforms that do not intersect the computationInterval, and
    // verifying that all waveforms are from the same channel
    Collection<WaveformSummary> filteredWaveforms = new ArrayList<>();
    AtomicReference<String> channelName = new AtomicReference<>();

    filterTemporalOverlap(
        waveformSummaryAndReceptionTimes,
        (WaveformSummaryAndReceptionTime w) -> w.getWaveformSummary().getStartTime(),
        (WaveformSummaryAndReceptionTime w) -> w.getWaveformSummary().getEndTime(),
        stationSohDefinition.getTimeWindowBySohMonitorType().get(SohMonitorType.MISSING)
            .getCalculationInterval(),
        stationSohDefinition.getTimeWindowBySohMonitorType().get(SohMonitorType.MISSING)
            .getBackOffDuration(),
        now
    ).map(WaveformSummaryAndReceptionTime::getWaveformSummary)
        .forEach(w -> {
              if (channelName.get() == null) {
                channelName.set(w.getChannelName());
              } else {
                Validate.isTrue(
                    channelName.get().equals(w.getChannelName()),
                    "All waveformSummaryAndReceptionTimes must be from the same Channel."
                );
              }
              filteredWaveforms.add(w);
            }
        );

    // If no waveform summaries are passed in, return 100% missing and a status of BAD
    if (filteredWaveforms.isEmpty()) {

      return PercentSohMonitorValueAndStatus
          .from(100.0, SohStatus.BAD, SohMonitorType.MISSING);
    }

    Duration backOffInterval = stationSohDefinition.getTimeWindowBySohMonitorType()
        .get(SohMonitorType.MISSING)
        .getBackOffDuration();

    Duration calculationInterval = stationSohDefinition.getTimeWindowBySohMonitorType()
        .get(SohMonitorType.MISSING)
        .getCalculationInterval();

    Instant stopTime = now.minus(backOffInterval);
    Instant startTime = stopTime.minus(calculationInterval);

    logger.debug("MISSING.now: {}", now);
    logger.debug("MISSING.backOffDuration: {}", backOffInterval);
    logger.debug("MISSING.calculationInterval: {}", calculationInterval);
    logger.debug("MISSING.stopTime: {}", stopTime);
    logger.debug("MISSING.startTime: {}", startTime);

    double missingPercentage = computeMissingPercentage(filteredWaveforms.stream(), startTime,
        stopTime);

    return PercentSohMonitorValueAndStatus
        .from(
            missingPercentage,
            computeStatusFromThreshold(
                missingPercentage,
                definition.getGoodThreshold(),
                definition.getMarginalThreshold()),
            SohMonitorType.MISSING);
  }

  /**
   * Computes the percentage of a specified interval not covered by the start times to end times of
   * the specified waveform summaries.
   *
   * @return a number in the range [0.0 - 100.0]. If the interval of interest has 0-length, 100.0 is
   * returned regardless of the waveform summaries. If the interval has a positive length, 0.0 is
   * returned if the waveform summary list is empty.
   */
  public static double computeMissingPercentage(
      final Stream<WaveformSummary> waveformSummaries,
      final Instant intervalStart,
      final Instant intervalEnd
  ) {

    if (!intervalStart.isBefore(intervalEnd)) {
      return 0.0;
    }

    // Trim to the interval, filter, and the waveforms in increasing order of start time
    var waveformSummariesStream = waveformSummaries
        .map(wfs -> trim(wfs, intervalStart, intervalEnd))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .sorted(Comparator.comparing(WaveformSummary::getStartTime));

    // If no waveform summaries are in the interval, return 100.0 missing.
    var countRef = new AtomicInteger();

    // create stack of non-overlapping waveforms
    Deque<WaveformSummary> stack = new ArrayDeque<>();

    waveformSummariesStream.forEach(waveformSummary -> {
      countRef.incrementAndGet();

      if (stack.isEmpty()) {
        stack.push(waveformSummary);
      }

      // get top waveform from the stack
      WaveformSummary top = stack.peek();

      // if current waveform does not overlap with top waveform, push it onto the stack
      assert top != null;
      if (top.getEndTime().isBefore(waveformSummary.getStartTime())) {
        Validate.isTrue(top.getStartTime().isBefore(waveformSummary.getStartTime()));
        stack.push(waveformSummary);
        // otherwise, merge current waveform with top waveform
      } else if (top.getEndTime().isBefore(waveformSummary.getEndTime())) {
        stack.pop();
        stack
            .push(WaveformSummary
                .from(top.getChannelName(), top.getStartTime(), waveformSummary.getEndTime()));
      }
    });

    if (countRef.get() == 0) {
      return 100.0;
    }

    // get total duration of acquired waveform data
    double acquiredDurationSeconds = stack.stream().reduce(
        0.0,
        (sum, waveformSummary) -> sum + (
            Duration.between(waveformSummary.getStartTime(), waveformSummary.getEndTime()).toNanos()
                / 1.0e9),
        Double::sum
    );

    double intervalSeconds = Duration.between(intervalStart, intervalEnd).toNanos() / 1.0e9;

    return 100.0 * (
        1.0 - acquiredDurationSeconds / intervalSeconds
    );
  }

  /**
   * Calculate the rollup of a set of SohMonitorValueAndStatuses, using the given set of
   * SohMonitorTypes.
   *
   * @param monitorValueAndStatusSet collection of SohMonitorValueStatuses from which to calculate a
   * rollup
   * @param monitorTypesInRollup The monitor types to filter by. SohMonitorValueAndStatus objects
   * with a SohMonitorType not in this collection are ignored in the calculation.
   * @return SohStatus object which represents the final rollup, wrapped in an
   * optional.
   */
  static Optional<SohStatus> rollup(
      Collection<? extends SohMonitorValueAndStatus<?>> monitorValueAndStatusSet,
      Collection<SohMonitorType> monitorTypesInRollup
  ) {

    return monitorValueAndStatusSet.stream()
        //
        // Get only those SohMonitorValueAndStatuses with a SohMonitorType in the provided set.
        //
        .filter(sohMonitorValueAndStatus ->
            monitorTypesInRollup.contains(sohMonitorValueAndStatus.getMonitorType()))
        //
        // Find the SohMonitorValueAndStatus with the lowest (worst) SohStatus.
        //
        .min(Comparator.comparing(SohMonitorValueAndStatus::getStatus))
        .map(SohMonitorValueAndStatus::getStatus);
  }


  /**
   * Computes a status by calculating the percentage of boolean issues that evaluate to true and
   * comparing it against thresholds in the definition parameter.
   *  @param acquiredChannelEnvironmentIssueStream Collection of boolean issues which must all have
   * the same type as the third parameter.
   * @param definition contains the thresholds and calculation interval.
   * @param acquiredChannelEnvironmentIssueType the type of the issue.
   */
  private PercentSohMonitorValueAndStatus singleEnvironmentStatus(
      Collection<AcquiredChannelEnvironmentIssueBoolean> acquiredChannelEnvironmentIssueStream,
      StationSohDefinition stationSohDefinition,
      PercentSohMonitorStatusThresholdDefinition definition,
      AcquiredChannelEnvironmentIssueType acquiredChannelEnvironmentIssueType
  ) {

    AtomicInteger count = new AtomicInteger();

    // 100 * (number with a status of true)/(total number). Increment counter AFTER filtering.
    Double percentage = 100.0 * filterTemporalOverlap(acquiredChannelEnvironmentIssueStream,
        AcquiredChannelEnvironmentIssue::getStartTime, AcquiredChannelEnvironmentIssue::getEndTime,
        stationSohDefinition.getTimeWindowBySohMonitorType()
            .get(SohMonitorType.valueOf("ENV_" + acquiredChannelEnvironmentIssueType.name()))
            .getCalculationInterval(),
        stationSohDefinition.getTimeWindowBySohMonitorType()
            .get(SohMonitorType.valueOf("ENV_" + acquiredChannelEnvironmentIssueType.name()))
            .getBackOffDuration(),
        now
    ).map(acei -> {
      count.incrementAndGet();
      return acei;
    }).reduce(0.0, (previousSum, acquiredChannelEnvironmentIssue) -> {
      Validate.isTrue(
          acquiredChannelEnvironmentIssue.getType() == acquiredChannelEnvironmentIssueType,
          "singleEnvironmentStatus: All issue types must be the same in the provided set, and match the provided issue type."
      );

      return previousSum + (
          Boolean.TRUE.equals(acquiredChannelEnvironmentIssue.getStatus()) ? 1.0 : 0.0);
    }, Double::sum) / count.get();

    // Check if the count was equal to zero, if it is assign an obvious NaN to the percentage
    // so the next method can detect and assign marginal
    if (count.get() == 0) {
      logger.debug("ACEI count = 0; setting MARGINAL soh status for {}",
          stationSohDefinition.getStationName());

      percentage = null;
    }

    return PercentSohMonitorValueAndStatus.from(
        percentage,
        computeStatusFromThreshold(
            percentage,
            definition.getGoodThreshold(),
            definition.getMarginalThreshold()
        ),
        acquiredChannelEnvironmentIssueType.getMatchingSohMonitorType()
    );
  }

  private Map<String, List<PercentSohMonitorValueAndStatus>> environmentStatus(
      StationSohDefinition stationSohDefinition,
      Map<String, Set<AcquiredChannelEnvironmentIssueBoolean>> partitionedSetByChannelName,
      Map<String, Map<SohMonitorType, PercentSohMonitorStatusThresholdDefinition>> definitions
  ) {

    Map<String, List<PercentSohMonitorValueAndStatus>> environmentStatusMap = new HashMap<>();
    List<String> channelsWithoutDefinitions = new ArrayList<>();

    partitionedSetByChannelName.entrySet().stream()
        .filter(entry -> {
          if (definitions.get(entry.getKey()) == null) {
            if (LOGGER_DEBUG_ENABLED) {
              channelsWithoutDefinitions.add(entry.getKey());
            }
            return false;
          }
          return true;
        })
        .forEach(entry -> environmentStatusMap.put(
            entry.getKey(),
            environmentStatus(
                stationSohDefinition,
                entry.getValue(),
                definitions.get(entry.getKey()))
            )
        );

    if (LOGGER_DEBUG_ENABLED) {
      logger.debug("No PercentSohMonitorValueAndStatusDefinitions found for channels: {}",
          channelsWithoutDefinitions);
    }

    return environmentStatusMap;
  }

  /**
   * Method returns a map of stationName to List of PercentSohMonitorValueAndStatus values
   *
   * @param aceiBooleanMap a map of channel names to boolean environment issues
   * @param stationSohDefinition the stationSohDefinition to use in the calculations
   * @return Map<StationName, List < PercentSohMonitorValueAndStatus>
   */
  private Map<String, List<PercentSohMonitorValueAndStatus>> stationChannelEnvironmentStatuses(
      Map<String, Set<AcquiredChannelEnvironmentIssueBoolean>> aceiBooleanMap,
      StationSohDefinition stationSohDefinition
  ) {

    //
    // Log the station we are looking at. This is useful so that we don't chase ghosts.
    //
    logger.debug("Collecting environment channel definitions for station {}",
        stationSohDefinition.getStationName());

    Map<String, Map<SohMonitorType, PercentSohMonitorStatusThresholdDefinition>>
        environmentDefinitions = new HashMap<>();

    stationSohDefinition.getChannelSohDefinitions().forEach(channelSohDefinition -> {
          Map<SohMonitorType, PercentSohMonitorStatusThresholdDefinition> typeEnvironmentDefinitions
              = new EnumMap<>(SohMonitorType.class);

          channelSohDefinition.getSohMonitorStatusThresholdDefinitionsBySohMonitorType()
              .forEach((monitorType, definition) -> {
                    if (monitorType.isEnvironmentIssue()) {
                      typeEnvironmentDefinitions.put(
                          monitorType,
                          (PercentSohMonitorStatusThresholdDefinition) definition
                      );
                    }
                  }
              );

          environmentDefinitions.put(channelSohDefinition.getChannelName(),
              typeEnvironmentDefinitions);
        }
    );

    return environmentStatus(
        stationSohDefinition,
        aceiBooleanMap,
        environmentDefinitions
    );
  }


  Map<String, DurationSohMonitorValueAndStatus> stationChannelTimelinessStatuses(
      StationSohDefinition stationSohDefinition,
      Instant stationSohTime
  ) {

    Map<String, DurationSohMonitorStatusThresholdDefinition> timelinessDefinitions = new HashMap<>();

    stationSohDefinition.getChannelSohDefinitions().forEach(channelSohDefinition ->
        timelinessDefinitions.put(channelSohDefinition.getChannelName(),
            (DurationSohMonitorStatusThresholdDefinition)
                channelSohDefinition.getSohMonitorStatusThresholdDefinitionsBySohMonitorType()
                    .get(SohMonitorType.TIMELINESS)
        )
    );

    Map<String, DurationSohMonitorValueAndStatus> channelsToTimelinessStatus = new HashMap<>();

    stationSohDefinition.getChannelSohDefinitions()
        .stream()
        .map(ChannelSohDefinition::getChannelName)
        .forEach(channelName -> {
              DurationSohMonitorStatusThresholdDefinition definition =
                  timelinessDefinitions.get(channelName);

              if (definition != null) {
                AtomicReference<DurationSohMonitorValueAndStatus> timelinessStatusRef =
                    new AtomicReference<>();

                acquiredSampleTimesByChannel.getLatestEndTime(channelName).ifPresentOrElse(
                    latestEndTime -> timelinessStatusRef
                        .set(timeliness(definition, latestEndTime, stationSohTime, channelName)),
                    () -> {
                      timelinessStatusRef.set(
                          DurationSohMonitorValueAndStatus.from(
                              null,
                              SohStatus.MARGINAL,
                              SohMonitorType.TIMELINESS
                          )
                      );

                      logger.debug(
                          "Failed to compute timeliness. No latest endTime for channel {}",
                          channelName);
                    }
                );

                channelsToTimelinessStatus.put(
                    channelName,
                    timelinessStatusRef.get()
                );
              } else {
                logger.debug("No duration definition for lag for channel {}", channelName);
              }
            }
        );

    return channelsToTimelinessStatus;
  }

  /**
   * @param waveformSummaryAndReceptionTimes map of channel name to waveform summaries
   * @param stationSohDefinition state of health definition for a station
   * @return map of channel name to lag statuses
   */
  private Map<String, Collection<DurationSohMonitorValueAndStatus>> stationChannelLagStatuses(
      Map<String, Set<WaveformSummaryAndReceptionTime>> waveformSummaryAndReceptionTimes,
      StationSohDefinition stationSohDefinition
  ) {

    Map<String, DurationSohMonitorStatusThresholdDefinition> lagDefinitions = new HashMap<>();

    stationSohDefinition.getChannelSohDefinitions().forEach(channelSohDefinition ->
        lagDefinitions.put(channelSohDefinition.getChannelName(),
            (DurationSohMonitorStatusThresholdDefinition)
                channelSohDefinition.getSohMonitorStatusThresholdDefinitionsBySohMonitorType()
                    .get(SohMonitorType.LAG)
        )
    );

    Map<String, Collection<DurationSohMonitorValueAndStatus>> channelsToLagStatus = new HashMap<>();

    lagDefinitions.forEach(
        (channelName, durationDefinition) -> {

          if (durationDefinition != null) {

            Collection<WaveformSummaryAndReceptionTime> summaryAndReceptionTimesForChannel =
                waveformSummaryAndReceptionTimes.get(channelName);

            if (summaryAndReceptionTimesForChannel != null) {
              Optional<DurationSohMonitorValueAndStatus> lagStatusOpt =
                  lag(stationSohDefinition, durationDefinition,
                      waveformSummaryAndReceptionTimes.get(channelName));

              if (lagStatusOpt.isPresent()) {
                // Add lagStatus to the map.  If no value has been computed for this channel, create
                // a new HashSet and add the lagStatus to it.
                channelsToLagStatus
                    .computeIfAbsent(
                        channelName,
                        k -> new HashSet<>()
                    )
                    .add(lagStatusOpt.get());
              } else {
                logger.log(SOH_TIMING_LEVEL, "could not compute lag status for channel {}",
                    channelName);
                channelsToLagStatus.put(channelName, addLagForMissingChannels());
              }
            } else {
              logger
                  .log(SOH_TIMING_LEVEL, "no waveform summaries and reception times for channel {}",
                      channelName);
              channelsToLagStatus.put(channelName, addLagForMissingChannels());
            }
          } else {
            logger.debug("No duration definition for lag for channel {}", channelName);
          }
        }
    );

    return channelsToLagStatus;
  }

  /**
   * When unable to calculate Lag due to missing data, construct an instance setting the {@link
   * SohStatus} to MARGINAL.
   *
   * @return a set of {@link DurationSohMonitorValueAndStatus} objects.
   */
  private Set<DurationSohMonitorValueAndStatus> addLagForMissingChannels() {
    return Set.of(DurationSohMonitorValueAndStatus
        .from(null, SohStatus.MARGINAL, SohMonitorType.LAG));
  }

  private Map<String, Collection<PercentSohMonitorValueAndStatus>> stationChannelMissingStatuses(
      Map<String, Set<WaveformSummaryAndReceptionTime>> waveformSummaryAndReceptionTimes,
      StationSohDefinition stationSohDefinition
  ) {

    Map<String, Collection<PercentSohMonitorValueAndStatus>> channelsToMissingStatus = new HashMap<>();

    stationSohDefinition.getChannelSohDefinitions().forEach(channelSohDefinition -> {

          String channelName = channelSohDefinition.getChannelName();

          PercentSohMonitorStatusThresholdDefinition percentDefinition =
              (PercentSohMonitorStatusThresholdDefinition) channelSohDefinition
                  .getSohMonitorStatusThresholdDefinitionsBySohMonitorType()
                  .get(SohMonitorType.MISSING);

          if (percentDefinition != null) {

            Collection<WaveformSummaryAndReceptionTime> summaryAndReceptionTimesForChannel =
                waveformSummaryAndReceptionTimes.get(channelName);

            PercentSohMonitorValueAndStatus missingStatus;

            if (summaryAndReceptionTimesForChannel != null) {

              missingStatus = missing(stationSohDefinition, percentDefinition,
                  waveformSummaryAndReceptionTimes.get(channelName));
            } else {

              logger.debug("no waveform summaries and reception times for channel {}", channelName);
              missingStatus = PercentSohMonitorValueAndStatus
                  .from(100.0, SohStatus.BAD, SohMonitorType.MISSING);
            }
            // Add missingStatus to the map.  If no value has been computed for this channel,
            // create a new HashSet and add the missingStatus to it.
            channelsToMissingStatus
                .computeIfAbsent(channelName, k -> new HashSet<>())
                .add(missingStatus);
          } else {

            logger.debug("No percent definition for MISSING for channel {}", channelName);
          }
        }
    );

    return channelsToMissingStatus;
  }

  private Set<ChannelSoh> calculateChannelSohSet(
      Map<String, ? extends Collection<PercentSohMonitorValueAndStatus>> channelsToEnvironmentStatus,
      Map<String, ? extends Collection<DurationSohMonitorValueAndStatus>> channelsToLagStatus,
      Map<String, ? extends Collection<PercentSohMonitorValueAndStatus>> channelsToMissingStatus,
      Map<String, ? extends DurationSohMonitorValueAndStatus> channelsToTimelinessStatus,
      StationSohDefinition stationSohDefinition
  ) {

    Map<String, Set<SohMonitorValueAndStatus<?>>> allChannelStatuses = new HashMap<>();

    channelsToEnvironmentStatus.forEach(
        (k, v) -> allChannelStatuses
            .put(k, v.stream().map(soh -> (SohMonitorValueAndStatus<?>) soh).collect(
                Collectors.toSet())));

    channelsToLagStatus.forEach((k, v) ->
        allChannelStatuses.computeIfAbsent(k, k1 -> new HashSet<>()
        ).addAll(v));

    channelsToTimelinessStatus.forEach((k, v) ->
        allChannelStatuses.computeIfAbsent(k, k1 -> new HashSet<>()
        ).add(v));

    channelsToMissingStatus.forEach((k, v) ->
        allChannelStatuses.computeIfAbsent(k, k1 -> new HashSet<>()
        ).addAll(v));

    final Set<ChannelSoh> channelSohs = new HashSet<>();

    allChannelStatuses.forEach((channelName, monitorValueAndStatusSet) -> {

      Optional<Set<SohMonitorType>> monitorTypesInRollupOpt =
          stationSohDefinition.getChannelSohDefinitions()
              .stream()
              .filter(channelSohDefinition ->
                  Objects.equals(channelName, channelSohDefinition.getChannelName()))
              .map(ChannelSohDefinition::getSohMonitorTypesForRollup)
              .findFirst();

      if (monitorTypesInRollupOpt.isPresent()) {

        Optional<SohStatus> rollupOpt = rollup(
            monitorValueAndStatusSet,
            monitorTypesInRollupOpt.get()
        );

        if (rollupOpt.isPresent()) {

          Set<SohMonitorValueAndStatus<?>> valueAndStatusSet = monitorValueAndStatusSet
              .stream()
              .map(status -> (SohMonitorValueAndStatus<?>) status)
              .collect(Collectors.toSet());

          channelSohs.add(ChannelSoh.from(
              channelName,
              rollupOpt.get(),
              valueAndStatusSet
          ));

        } else {
          logger.debug("No monitor type included in rollup for channel: {}", channelName);
        }

      } else {
        logger.debug("Couldn't get a set of rollup monitor types for channel: {}", channelName);
      }
    });

    return channelSohs;
  }

  /**
   * Trim the WaveformSummary start/end times to the given min/max start/end times
   *
   * @param waveformSummary waveform whose start/end times may need to be trimmed
   * @param minStartTime minimum start time, which must come before the maximum end time.
   * @param maxEndTime maximum end time, which must come after the minimum start time.
   * @return waveformSummary whose start/end times are trimmed to the given min/max start/end times
   * wrapped in an optional. If the waveform summary's interval does not overlap the interval, or
   * its start time is not before its end time, the returned optional will be empty.
   */
  public static Optional<WaveformSummary> trim(
      WaveformSummary waveformSummary,
      Instant minStartTime,
      Instant maxEndTime
  ) {

    // Validate that the interval is positive and greater than 0 in length.
    Validate.isTrue(minStartTime.isBefore(maxEndTime),
        "minStartTime is not before maxEndTime");

    // Take care of the cases:
    // 1. The waveform summary's start time doesn't come before its end time.
    // 2. The waveform summary's entire interval precedes the interest of interest.
    // 3. The interval of interest precedes the waveform summary's interval.
    if (!waveformSummary.getStartTime().isBefore(waveformSummary.getEndTime()) ||
        !waveformSummary.getEndTime().isAfter(minStartTime) ||
        !waveformSummary.getStartTime().isBefore(maxEndTime)) {
      return Optional.empty();
    }

    // To indicate whether the start or end times of the waveformSummary have been
    // trimmed.
    boolean trimmed = false;

    Instant startTime = waveformSummary.getStartTime();
    if (startTime.isBefore(minStartTime)) {
      startTime = minStartTime;
      trimmed = true;
    }

    Instant endTime = waveformSummary.getEndTime();
    if (endTime.isAfter(maxEndTime)) {
      endTime = maxEndTime;
      trimmed = true;
    }

    // Only need to return a new WaveformSummary if one of the times has been trimmed.
    return Optional.of(
        trimmed ? WaveformSummary.from(waveformSummary.getChannelName(), startTime, endTime) :
            waveformSummary
    );
  }

  /**
   * Computes the {@link SohStatus} of the {@link DurationSohMonitorValueAndStatus} for a {@link
   * SohMonitorType}. It does this by comparing a value against two thresholds. The higher the
   * value, the worse the status returned.
   *
   * @param value This value will be used to determine whether the SohStatus for the MonitorType is
   * BAD, MARGINAL, or GOOD.
   * @param goodStatusThreshold The value that value must be less than or equal to be evaluated as
   * GOOD.
   * @param marginalStatusThreshold The value that value must be less than or equal to in order to
   * be considered at least MARGINAL. If value exceeds this threshold a status of BAD is returned.
   * @return The {@link SohStatus} as determined by threshold values.
   */
  private static <T extends Comparable<T>> SohStatus computeStatusFromThreshold(
      T value,
      T goodStatusThreshold,
      T marginalStatusThreshold
  ) {

    Validate.notNull(goodStatusThreshold, "Null goodStatusThreshold");
    Validate.notNull(marginalStatusThreshold, "Null marginalStatusThreshold");

    SohStatus sohStatus;

    if (value == null) {
      sohStatus = SohStatus.MARGINAL;
      return sohStatus;
    }

    if (value.compareTo(goodStatusThreshold) <= 0) {

      sohStatus = SohStatus.GOOD;
      // Set values that are NaN to Marginal
    } else if (value.compareTo(marginalStatusThreshold) <= 0) {

      sohStatus = SohStatus.MARGINAL;
    } else {

      sohStatus = SohStatus.BAD;
    }

    return sohStatus;
  }

  /*
   * Filter out objects whose start/end times do not overlap the calculation interval
   */
  static <T> Stream<T> filterTemporalOverlap(
      Collection<T> objects,
      Function<T, Instant> getStartTime,
      Function<T, Instant> getEndTime,
      Duration calculationInterval,
      Duration backOffDuration,
      Instant now
  ) {

    Validate.notNull(objects, "objects may not be null");
    Validate.notNull(calculationInterval, "calculationInterval may not be null");
    Validate.isTrue(!calculationInterval.isNegative(), "calculationInterval may not be negative");
    Validate.notNull(backOffDuration, "backOffDuration may not be null");
    Validate.isTrue(!backOffDuration.isNegative(), BACK_OFF_DURATION_MAY_NOT_BE_NEGATIVE);

    Instant calculationIntervalStopTime = now.minus(backOffDuration);
    Instant calculationIntervalStartTime = calculationIntervalStopTime.minus(calculationInterval);

    return objects.stream()
        .filter(w -> getStartTime.apply(w).isBefore(calculationIntervalStopTime) &&
            getEndTime.apply(w).isAfter(calculationIntervalStartTime));
  }

  static <T, U> Map<U, Collection<T>> partition(
      Stream<T> theStream,
      Function<T, U> classifier
  ) {

    return theStream.collect(Collectors.groupingBy(classifier)).entrySet().stream()
        .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
  }

}
