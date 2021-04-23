package gms.core.performancemonitoring.soh.control;

import gms.core.performancemonitoring.soh.control.configuration.ChannelSohDefinition;
import gms.core.performancemonitoring.soh.control.configuration.StationSohDefinition;
import gms.core.performancemonitoring.soh.control.configuration.TimeWindowDefinition;
import gms.core.performancemonitoring.soh.control.reactor.ReactorUtility;
import gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssue;
import gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssueBoolean;
import gms.shared.frameworks.osd.coi.soh.DurationStationAggregate;
import gms.shared.frameworks.osd.coi.soh.PercentStationAggregate;
import gms.shared.frameworks.osd.coi.soh.SohMonitorType;
import gms.shared.frameworks.osd.coi.soh.StationAggregate;
import gms.shared.frameworks.osd.coi.soh.StationAggregateType;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class StationAggregateCalculationUtility {

  private static final Logger logger = LogManager
      .getLogger(StationAggregateCalculationUtility.class);

  private final Instant now;

  private final AcquiredSampleTimesByChannel acquiredSampleTimesByChannel;

  public StationAggregateCalculationUtility(
      AcquiredSampleTimesByChannel acquiredSampleTimesByChannel,
      Instant now
  ) {

    this.acquiredSampleTimesByChannel = acquiredSampleTimesByChannel;
    this.now = now;
  }

  /**
   * Build the set of StationAggregate objects for a particular station.
   *
   * @param waveformSummaryAndReceptionTimesMono The map of channel to set of WaveformSummaryAndReceptionTime, for
   * calculations that need waveform data
   * @param aceiBooleanMapMono The map of channel to set of ACEIs, for calculations that need environment data
   * @param stationSohDefinition the StationSohDefinition for the station
   * @return Set of StationAggregates, for LAG, TIMELINESS, MISSING, ENVIRONMENT_ISSUES
   */
  Mono<Set<StationAggregate<?>>> buildStationAggregateMono(
      Mono<Map<String, Set<WaveformSummaryAndReceptionTime>>> waveformSummaryAndReceptionTimesMono,
      Mono<Map<String, Set<AcquiredChannelEnvironmentIssueBoolean>>> aceiBooleanMapMono,
      StationSohDefinition stationSohDefinition
  ) {

    Objects.requireNonNull(
        waveformSummaryAndReceptionTimesMono,
        "waveformSummaryAndReceptionTimesMono is null!"
    );

    Objects.requireNonNull(
        aceiBooleanMapMono,
        "aceiBooleanMapMono is null!"
    );

    Objects.requireNonNull(
        stationSohDefinition,
        "stationSohDefinition is null!"
    );

    return Flux.concat(
        ReactorUtility.getMonoSubscribedProcessor(
            filterWaveformSummaries(
                waveformSummaryAndReceptionTimesMono,
                stationSohDefinition,
                SohMonitorType.LAG
            ).map(map -> map.values().stream().flatMap(Collection::stream)
                .collect(Collectors.toSet())),
            this::lag,
            stationSohDefinition.getTimeWindowBySohMonitorType().get(SohMonitorType.LAG)
        ).doOnError(t -> {

          logger.error(
              "Error computing station aggregate lag",
              t
          );

          throw Exceptions.propagate(t);
        }).filter(Optional::isPresent)
            .map(Optional::get),

        ReactorUtility.getMonoSubscribedProcessor(
            aceiBooleanMapMono.map(
                aceiBolleanMap -> filterAndFlattenAceis(
                    aceiBolleanMap,
                    stationSohDefinition,
                    now
                )
            ),
            this::environmentIssues
        ).doOnError(t -> {

          logger.error(
              "Error computing station aggregate environment issues",
              t
          );

          throw Exceptions.propagate(t);
        }),

        ReactorUtility.getMonoSubscribedProcessor(
            filterWaveformSummaries(
                waveformSummaryAndReceptionTimesMono,
                stationSohDefinition,
                SohMonitorType.MISSING
            ).map(map -> map.values().stream().flatMap(Collection::stream)
                .collect(Collectors.toSet())),
            this::missing,
            stationSohDefinition
        ).doOnError(t -> {

          logger.error(
              "Error computing station aggregate missing",
              t
          );

          throw Exceptions.propagate(t);
        }),

        ReactorUtility.getMonoSubscribedProcessor(
            filterWaveformSummaries(
                waveformSummaryAndReceptionTimesMono,
                stationSohDefinition,
                SohMonitorType.TIMELINESS
            ),
            this::timeliness,
            stationSohDefinition.getChannelsBySohMonitorType().get(SohMonitorType.TIMELINESS)
        ).doOnError(t -> {

          logger.error(
              "Error computing station aggregate timeliness",
              t
          );

          throw Exceptions.propagate(t);
        }).filter(Optional::isPresent)
            .map(Optional::get)
    )
        //Adding a doOnCompleted, because that seems to be the only way doOnError is run?
        .doOnComplete(() -> logger.debug("StationAggregateCalculationUtility flux has completed"))
        .collect(Collectors.toSet());
  }

  /**
   * Calculate the average lag of all channels of the station
   *
   * @param waveformSummaryAndReceptionTimes Set of waveform summaries, with reception times
   * @param timeWindowDefinition TimeWindowDefinition containing the calculation interval and back off duration
   * @return DurationStationAggregate of type LAG containing the average lag.
   */
  private Optional<DurationStationAggregate> lag(
      Collection<WaveformSummaryAndReceptionTime> waveformSummaryAndReceptionTimes,
      TimeWindowDefinition timeWindowDefinition
  ) {

    if (waveformSummaryAndReceptionTimes.isEmpty()) {
      return Optional.empty();
    }

    return ChannelSohCalculationUtility.lag(
        timeWindowDefinition,
        waveformSummaryAndReceptionTimes,
        Aggregator.getDurationAverager(),
        now
    ).map(
        averageDuration -> DurationStationAggregate.from(
            averageDuration, StationAggregateType.LAG
        )
    );
  }

  /**
   * Calculate the percentage of all environmental issues in the given collection that are true (that is, something is
   * wrong)
   *
   * @param acquiredChannelEnvironmentIssueBooleans environment booleans to calculate te average for
   * @return PercentStationAggregate that wraps the percentage value.
   */
  private PercentStationAggregate environmentIssues(
      Stream<AcquiredChannelEnvironmentIssueBoolean> acquiredChannelEnvironmentIssueBooleans
  ) {

    double average = 100.0 * acquiredChannelEnvironmentIssueBooleans
        .mapToInt(
            acquiredChannelEnvironmentIssueBoolean ->
                Boolean.TRUE.equals(acquiredChannelEnvironmentIssueBoolean.getStatus()) ? 1
                    : 0
        ).average().orElse(Double.NaN);

    return PercentStationAggregate.from(
        Double.isNaN(average) ? null : average,
        StationAggregateType.ENVIRONMENTAL_ISSUES
    );
  }

  /**
   * Calculate the missing percentage using the waveform summaries for all channels of the station
   *
   * @param waveformSummaryAndReceptionTimes Collection of waveform summaries, with reception times
   * @param timeWindowDefinition TimeWindowDefinition containing the calculation interval and back off duration
   * @return PercentStationAggregate of type MISSING.
   */
  private PercentStationAggregate missing(
      Collection<WaveformSummaryAndReceptionTime> waveformSummaryAndReceptionTimes,
      StationSohDefinition stationSohDefinition
  ) {

    var timeWindowDefinition = stationSohDefinition
        .getTimeWindowBySohMonitorType().get(SohMonitorType.MISSING);

    var channelsWithData = new HashSet<String>();

    Instant stopTime = now.minus(timeWindowDefinition.getBackOffDuration());
    Instant startTime = stopTime.minus(timeWindowDefinition.getCalculationInterval());

    // Convert to a list of waveform summaries in order to call computeMissingPercentage.
    // No filtering for temporal overlap is done, since that method in
    // ChannelSohCalculationUtility is not accessible.
    var waveformSummaries = waveformSummaryAndReceptionTimes.stream()
        .map(waveformSummaryAndReceptionTime -> {
          channelsWithData
              .add(waveformSummaryAndReceptionTime.getWaveformSummary().getChannelName());
          return waveformSummaryAndReceptionTime.getWaveformSummary();
        });

    // NOTE: since waveformSummaries is a Stream, the lambda passed to the map will not
    // be called until ChannelSohCalculationUtility.computeMissingPercentage processes the stream!
    double unadjustedAverage = ChannelSohCalculationUtility.computeMissingPercentage(
        waveformSummaries, startTime, stopTime);

    //
    // Once we get the unadjusted average, which is effectively the average over the channels that
    // DO have data, We want to adjust that average to account for the channels that had NO data, and
    // therefore did not get counted in the unadjusted average.
    //

    // Count ALL of the channels we care about
    int allChannelsSize = stationSohDefinition.getChannelSohDefinitions().size();

    // This must happen after we call ChannelSohCalculationUtility.computeMissingPercentage!
    int channelsWithDataSize = channelsWithData.size();

    // Add in N 100% values, where N is the number of channels we had no data for
    double channelsWithNoDataSum = (allChannelsSize - channelsWithDataSize) * 100.0;

    // Multiply the unadjusted average by the number of channels with data, to undo the "divide-by-n"
    // piece of the average calculation. Then add in all of the 100% missing channels, and divide
    // by the number of ALL channels to get the correct average over ALL channels.
    double missingPercentage = (channelsWithNoDataSum + channelsWithDataSize * unadjustedAverage) / allChannelsSize;

    return PercentStationAggregate.from(missingPercentage, StationAggregateType.MISSING);
  }

  /**
   * Calculate the timeliness, which is how long in the past we recieved the most recent data
   *
   * @param waveformSummaryAndReceptionTimesMap Map of channel to waveform summary
   * @param channelNames list of channels to consider
   * @return DurationStationAggregate that wraps the timeliness value
   */
  private Optional<DurationStationAggregate> timeliness(
      Map<String, Set<WaveformSummaryAndReceptionTime>> waveformSummaryAndReceptionTimesMap,
      Set<String> channelNames
  ) {

    if (waveformSummaryAndReceptionTimesMap.isEmpty() && acquiredSampleTimesByChannel.isEmpty()) {
      return Optional.empty();
    }
    return channelNames.stream()
        .map(acquiredSampleTimesByChannel::getLatestEndTime)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .max(Comparator.naturalOrder())
        .map(
            latestEndTime -> Duration.between(latestEndTime, now)
        )
        .map(
            timelinessDuration -> DurationStationAggregate.from(
                timelinessDuration,
                StationAggregateType.TIMELINESS
            )
        );
  }

  private static Map<String, Set<WaveformSummaryAndReceptionTime>> filterWaveformSummaries(
      Map<String, Set<WaveformSummaryAndReceptionTime>> waveformSummaryAndReceptionTimeMap,
      StationSohDefinition definition,
      SohMonitorType monitorType
  ) {
    return waveformSummaryAndReceptionTimeMap.entrySet().stream().filter(
        stringSetEntry ->
            definition.getStationName().equals(
                stringSetEntry.getKey().substring(
                    0, stringSetEntry.getKey().indexOf(".")
                )) &&
                definition.getChannelsBySohMonitorType().get(monitorType)
                    .contains(stringSetEntry.getKey()
                    )
    ).collect(
        Collectors.toMap(
            Entry::getKey, Entry::getValue
        )
    );
  }

  private static Mono<Map<String, Set<WaveformSummaryAndReceptionTime>>> filterWaveformSummaries(
      Mono<Map<String, Set<WaveformSummaryAndReceptionTime>>> waveformSummaryAndReceptionTimesMono,
      StationSohDefinition stationSohDefinition,
      SohMonitorType monitorType
  ) {

    return waveformSummaryAndReceptionTimesMono
        .map(
            waveformSummaryMap -> filterWaveformSummaries(
                waveformSummaryMap,
                stationSohDefinition,
                monitorType
            )
        );
  }

  private static Stream<AcquiredChannelEnvironmentIssueBoolean> filterAndFlattenAceis(
      Map<String, Set<AcquiredChannelEnvironmentIssueBoolean>> channelAceiMap,
      StationSohDefinition stationSohDefinition,
      Instant now
  ) {

    //
    // Flatten and filter out the ACEIs by channel, and then by the calculation intervals
    // and backoff durations PER monitor type.
    //
    return channelAceiMap.entrySet().stream()

        //
        // Filter out ACEIs for channels we dont care about
        //
        .filter(
            entry -> stationSohDefinition.getChannelsBySohMonitorType().entrySet().stream()
                .filter(subentry -> subentry.getKey().isEnvironmentIssue())
                .flatMap(subentry -> subentry.getValue().stream())
                .collect(Collectors.toSet())
                .contains(entry.getKey())
        )

        // We only care about the map values now, dont care about channels.
        .map(Entry::getValue)
        .flatMap(Collection::stream)

        //
        // Group the ACEIs by monitor type, so that we can filter per monitor type, by
        // the calculation interval and backoff duration.
        //
        .collect(Collectors.groupingBy(AcquiredChannelEnvironmentIssue::getType))
        .entrySet().stream()
        .flatMap(
            entry -> {

              // Find the TimeWindowDefinition for the monitor type.
              var timeWindowDefinition = stationSohDefinition.getTimeWindowBySohMonitorType()
                  .get(entry.getKey().getMatchingSohMonitorType());

              return ChannelSohCalculationUtility.filterTemporalOverlap(
                  entry.getValue(),
                  AcquiredChannelEnvironmentIssue::getStartTime,
                  AcquiredChannelEnvironmentIssue::getEndTime,
                  timeWindowDefinition.getCalculationInterval(),
                  timeWindowDefinition.getBackOffDuration(),
                  now
              );
            }
        );
  }
}
