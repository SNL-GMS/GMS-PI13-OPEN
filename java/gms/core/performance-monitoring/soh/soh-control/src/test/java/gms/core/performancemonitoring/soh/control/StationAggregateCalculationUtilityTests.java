package gms.core.performancemonitoring.soh.control;

import gms.core.performancemonitoring.soh.control.configuration.ChannelSohDefinition;
import gms.core.performancemonitoring.soh.control.configuration.StationSohDefinition;
import gms.core.performancemonitoring.soh.control.configuration.TimeWindowDefinition;
import gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssue.AcquiredChannelEnvironmentIssueType;
import gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssueBoolean;
import gms.shared.frameworks.osd.coi.soh.ChannelSoh;
import gms.shared.frameworks.osd.coi.soh.DurationStationAggregate;
import gms.shared.frameworks.osd.coi.soh.PercentStationAggregate;
import gms.shared.frameworks.osd.coi.soh.SohMonitorType;
import gms.shared.frameworks.osd.coi.soh.StationAggregate;
import gms.shared.frameworks.osd.coi.soh.StationAggregateType;
import gms.shared.frameworks.osd.coi.waveforms.WaveformSummary;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

class StationAggregateCalculationUtilityTests {

  @ParameterizedTest
  @MethodSource("lagTestSource")
  void testLag(
      Map<String, Set<WaveformSummaryAndReceptionTime>> waveformSummaryAndReceptionTimeMap,
      StationSohDefinition stationSohDefinition,
      DurationStationAggregate expectedLagAggregate,
      Instant now
  ) {

    var cache = new AcquiredSampleTimesByChannel();
    cache.setLatestChannelToEndTime(Map.of());

    var utility = new StationAggregateCalculationUtility(cache, now);

    Set<StationAggregate<?>> stationAggregates = utility.buildStationAggregateMono(
        Mono.just(waveformSummaryAndReceptionTimeMap),
        Mono.just(Map.of()),
        stationSohDefinition
    ).block();

    var actualLagAggregateOpt = stationAggregates.stream().filter(
        stationAggregate -> stationAggregate.getAggregateType() == StationAggregateType.LAG
    ).findFirst();

    Assertions.assertTrue(
        actualLagAggregateOpt.isPresent()
    );

    Assertions.assertEquals(
        expectedLagAggregate,
        actualLagAggregateOpt.get()
    );
  }

  private static Stream<Arguments> lagTestSource() {

    var waveformSummary1 = WaveformSummary.from(
        "MyLovelyStation.channelName",
        Instant.EPOCH,
        Instant.EPOCH
    );

    var waveformSummary2 = WaveformSummary.from(
        "MyLovelyStation.channelName2",
        Instant.EPOCH,
        Instant.EPOCH
    );

    return Stream.of(
        Arguments.arguments(
            Map.of(
                "MyLovelyStation.channelName",
                Set.of(
                    WaveformSummaryAndReceptionTime
                        .create(waveformSummary1, Instant.ofEpochMilli(10)),
                    WaveformSummaryAndReceptionTime
                        .create(waveformSummary1, Instant.ofEpochMilli(20))
                )
            ),
            getMockStationSohDefinition(
                "MyLovelyStation",
                Set.of("MyLovelyStation.channelName"),
                Map.of(
                    SohMonitorType.LAG,
                    TimeWindowDefinition.create(
                        Duration.ofSeconds(2000),
                        Duration.ofSeconds(0)
                    )
                )
            ),
            DurationStationAggregate.from(
                Duration.ofMillis(15),
                StationAggregateType.LAG
            ),
            Instant.ofEpochMilli(20)
        ),

        Arguments.arguments(
            Map.of(
                "MyLovelyStation.channelName1",
                Set.of(
                    WaveformSummaryAndReceptionTime
                        .create(waveformSummary1, Instant.ofEpochMilli(10))
                ),
                "MyLovelyStation.channelName2",
                Set.of(
                    WaveformSummaryAndReceptionTime
                        .create(waveformSummary2, Instant.ofEpochMilli(20))
                )
            ),
            getMockStationSohDefinition(
                "MyLovelyStation",
                Set.of("MyLovelyStation.channelName1", "MyLovelyStation.channelName2"),
                Map.of(
                    SohMonitorType.LAG,
                    TimeWindowDefinition.create(
                        Duration.ofSeconds(2000),
                        Duration.ofSeconds(0)
                    )
                )
            ),
            DurationStationAggregate.from(
                Duration.ofMillis(15),
                StationAggregateType.LAG
            ),
            Instant.ofEpochMilli(20)
        ),

        Arguments.arguments(
            Map.of(
                "MyLovelyStation.channelName1",
                Set.of(
                    WaveformSummaryAndReceptionTime
                        .create(waveformSummary1, Instant.ofEpochMilli(10)),
                    WaveformSummaryAndReceptionTime
                        .create(waveformSummary1, Instant.ofEpochMilli(20))
                ),
                "MyLovelyStation.channelName2",
                Set.of(
                    WaveformSummaryAndReceptionTime
                        .create(waveformSummary2, Instant.ofEpochMilli(20))
                )
            ),
            getMockStationSohDefinition(
                "MyLovelyStation",
                Set.of("MyLovelyStation.channelName1", "MyLovelyStation.channelName2"),
                Map.of(
                    SohMonitorType.LAG,
                    TimeWindowDefinition.create(
                        Duration.ofSeconds(2000),
                        Duration.ofSeconds(0)
                    )
                )
            ),
            DurationStationAggregate.from(
                Duration.parse("PT0.016666666S"),
                StationAggregateType.LAG
            ),
            Instant.ofEpochMilli(20)
        )
    );
  }

  @Test
  void testMissingAveraging() {

    //
    // The below calculationInterval and backoffDuration, along with a now of 10020 ms after EPOCH,
    // will create a window of 10 to 10010 after Epoch.
    //
    // Use a 10000 ms calculation interval, so we can create 0.25% missing
    var calculationInterval = Duration.ofMillis(10000);
    // And a 10 ms backoff duration.
    var backoffDuration = Duration.ofMillis(10);

    var now = Instant.ofEpochMilli(10020);

    final String STATION_NAME = "SmokinHotStation";

    IntFunction<String> nameGenerator = i -> STATION_NAME + ".channelName" + (i + 1);

    // "Excellent" waveform summaries with missing of only 0.25%
    // Creating 11 of these.
    var excellentWaveFormSummariesMap = IntStream.range(0, 11)
        .mapToObj(i -> Map.entry(
            nameGenerator.apply(i),
            Set.of(
                //
                // Create a waveform summary between 10 MS and 9985 ms after EPOCH. Thus the
                // length of the summary is 9975 ms, meaning there are 25 ms, or 0.25% of
                // the calculation interval, missing
                //
                WaveformSummaryAndReceptionTime.create(
                    WaveformSummary.from(
                        nameGenerator.apply(i),
                        Instant.ofEpochMilli(10),
                        Instant.ofEpochMilli(9985)
                    ), Instant.ofEpochMilli(10010)),

                // Since our calculation window is 10 to 10010 MS after EPOCH, this should be
                // ignored if our logic is correct.
                WaveformSummaryAndReceptionTime.create(
                    WaveformSummary.from(
                        nameGenerator.apply(i),
                        Instant.EPOCH,
                        Instant.ofEpochMilli(10)
                    ),
                    Instant.ofEpochMilli(20)
                )
            )
            )
        ).collect(Collectors.toMap(Entry::getKey, Entry::getValue));

    var stationSohDefinition = getMockStationSohDefinition(
        STATION_NAME,
        // Creating 12 ChannelSohDefinitions, so that the calculation is forced
        // to readjust the average for 100% missing for the 12th channel.
        IntStream.range(0, 12).mapToObj(nameGenerator).collect(Collectors.toSet()),
        Map.of(
            SohMonitorType.MISSING,
            TimeWindowDefinition.create(
                calculationInterval, backoffDuration
            )
        )
    );

    var cache = new AcquiredSampleTimesByChannel();
    cache.setLatestChannelToEndTime(Map.of());

    var utility = new StationAggregateCalculationUtility(cache, now);

    Set<StationAggregate<?>> stationAggregates = utility.buildStationAggregateMono(
        Mono.just(excellentWaveFormSummariesMap),
        Mono.just(Map.of()),
        stationSohDefinition
    ).block();

    Optional<StationAggregate<?>> opt = stationAggregates.stream().filter(
        stationAggregate -> stationAggregate.getAggregateType() == StationAggregateType.MISSING
    ).findFirst();

    Assertions.assertTrue(opt.isPresent());

    Assertions.assertTrue(opt.get() instanceof PercentStationAggregate);

    assertEqual(
        //
        // Since we have 0.25% missing for 11 channels, and 100% missing for 1 channel via exclusion,
        // the average of all 12 channels should be 8.5625%.
        //
        PercentStationAggregate.from(
            8.5625,
            StationAggregateType.MISSING
        ),
        (PercentStationAggregate) opt.get()
    );
  }

  @ParameterizedTest
  @MethodSource("missingTestSource")
  void testMissing(
      Map<String, Set<WaveformSummaryAndReceptionTime>> waveformSummaryAndReceptionTimeMap,
      StationSohDefinition stationSohDefinition,
      PercentStationAggregate expectedMissingAggregate,
      Instant now
  ) {

    var cache = new AcquiredSampleTimesByChannel();
    cache.setLatestChannelToEndTime(Map.of());

    var utility = new StationAggregateCalculationUtility(cache, now);

    Set<StationAggregate<?>> stationAggregates = utility.buildStationAggregateMono(
        Mono.just(waveformSummaryAndReceptionTimeMap),
        Mono.just(Map.of()),
        stationSohDefinition
    ).block();

    Optional<StationAggregate<?>> opt = stationAggregates.stream().filter(
        stationAggregate -> stationAggregate.getAggregateType() == StationAggregateType.MISSING
    ).findFirst();

    Assertions.assertTrue(opt.isPresent());

    Assertions.assertTrue(opt.get() instanceof PercentStationAggregate);

    assertEqual(expectedMissingAggregate, (PercentStationAggregate) opt.get());
  }

  private static Stream<Arguments> missingTestSource() {

    // Use a 100 ms calculation interval
    Duration calculationInterval = Duration.ofMillis(100);
    // And a 10 ms backoff duration.
    Duration backoffDuration = Duration.ofMillis(10);

    Instant now = Instant.ofEpochMilli(120);

    // So the calculation interval will be the epoch ms [10 - 110]

    WaveformSummary wfsBeforeInterval = WaveformSummary.from(
        "EvenBetterLookingStation.channelName1",
        // start time and end time.
        Instant.EPOCH,
        Instant.EPOCH.plusMillis(10)
    );

    WaveformSummary wfsFirst20Pct = WaveformSummary.from(
        "EvenBetterLookingStation.channelName2",
        Instant.EPOCH.plusMillis(10),
        Instant.EPOCH.plusMillis(30)
    );

    WaveformSummary wfsLast40Pct = WaveformSummary.from(
        "EvenBetterLookingStation.channelName1",
        Instant.ofEpochMilli(70),
        Instant.ofEpochMilli(115)
    );

    TimeWindowDefinition timeWindowDefinition = TimeWindowDefinition.create(
        calculationInterval, backoffDuration
    );

    StationSohDefinition stationSohDefinition = getMockStationSohDefinition(
        "EvenBetterLookingStation",
        Set.of("EvenBetterLookingStation.channelName1", "EvenBetterLookingStation.channelName2"),
        Map.of(SohMonitorType.MISSING, timeWindowDefinition)
    );

    return Stream.of(
        Arguments.arguments(
            Map.of(
                "EvenBetterLookingStation.channelName1",
                Set.of(
                    WaveformSummaryAndReceptionTime.create(wfsBeforeInterval,
                        wfsBeforeInterval.getEndTime().plusMillis(10))
                )
            ),
            stationSohDefinition,
            PercentStationAggregate.from(
                100.0,
                StationAggregateType.MISSING
            ),
            now
        ),
        Arguments.arguments(
            Map.of(
                "EvenBetterLookingStation.channelName1",
                Set.of(
                    WaveformSummaryAndReceptionTime.create(wfsBeforeInterval,
                        wfsBeforeInterval.getEndTime().plusMillis(10)),
                    WaveformSummaryAndReceptionTime.create(wfsFirst20Pct,
                        wfsFirst20Pct.getEndTime().plusMillis(10))
                ),
                "EvenBetterLookingStation.channelName2",
                Set.of(
                    WaveformSummaryAndReceptionTime.create(wfsBeforeInterval,
                        wfsBeforeInterval.getEndTime().plusMillis(10))
                )
            ),
            stationSohDefinition,
            PercentStationAggregate.from(
                80.0,
                StationAggregateType.MISSING
            ),
            now
        ),
        Arguments.arguments(
            Map.of(
                "EvenBetterLookingStation.channelName1",
                Set.of(
                    WaveformSummaryAndReceptionTime.create(wfsBeforeInterval,
                        wfsBeforeInterval.getEndTime().plusMillis(10)),
                    WaveformSummaryAndReceptionTime.create(wfsLast40Pct,
                        wfsLast40Pct.getEndTime().plusMillis(5))
                ),
                "EvenBetterLookingStation.channelName2",
                Set.of(
                    WaveformSummaryAndReceptionTime.create(wfsBeforeInterval,
                        wfsBeforeInterval.getEndTime().plusMillis(10))
                )
            ),
            stationSohDefinition,
            PercentStationAggregate.from(
                80.0,
                StationAggregateType.MISSING
            ),
            now
        )
    );
  }

  @ParameterizedTest
  @MethodSource("envIssuesTestSource")
  void testEnvIssues(
      Map<String, Set<AcquiredChannelEnvironmentIssueBoolean>> aceiMap,
      StationSohDefinition stationSohDefinition,
      PercentStationAggregate expectedPercentAggregate,
      Instant now
  ) {
    var waveformSummaryDummy = WaveformSummary.from(
        (String) aceiMap.keySet().toArray()[0],
        Instant.EPOCH,
        Instant.EPOCH
    );

    var cache = new AcquiredSampleTimesByChannel();
    cache.setLatestChannelToEndTime(Map.of());

    var utility = new StationAggregateCalculationUtility(cache, now);

    Set<StationAggregate<?>> stationAggregates = utility.buildStationAggregateMono(
        Mono.just(Map.of(
            (String) aceiMap.keySet().toArray()[0],
            Set.of(WaveformSummaryAndReceptionTime.create(
                waveformSummaryDummy,
                Instant.ofEpochMilli(100)
            ))
        )),
        Mono.just(aceiMap),
        stationSohDefinition
    ).block();

    var actualLagAggregateOpt = stationAggregates.stream().filter(
        stationAggregate -> stationAggregate.getAggregateType()
            == StationAggregateType.ENVIRONMENTAL_ISSUES
    ).findFirst();

    Assertions.assertTrue(
        actualLagAggregateOpt.isPresent()
    );

    Assertions.assertEquals(
        expectedPercentAggregate,
        actualLagAggregateOpt.get()
    );
  }

  private static Stream<Arguments> envIssuesTestSource() {

    return Stream.of(
        Arguments.arguments(
            Map.of(
                "StationA.ChannelA",
                Set.of(
                    AcquiredChannelEnvironmentIssueBoolean.create(
                        "StationA.ChannelA",
                        AcquiredChannelEnvironmentIssueType.CLIPPED,
                        Instant.ofEpochMilli(20),
                        Instant.ofEpochMilli(40),
                        true
                    )
                )
            ),
            getMockStationSohDefinition(
                "StationA",
                Set.of("StationA.ChannelA"),
                Map.of(
                    SohMonitorType.ENV_CLIPPED,
                    TimeWindowDefinition.create(
                        Duration.ofMillis(60),
                        Duration.ZERO
                    )
                )
            ),
            PercentStationAggregate.from(
                100.0,
                StationAggregateType.ENVIRONMENTAL_ISSUES
            ),
            Instant.ofEpochMilli(60)
        ),

        //
        // Two of same type with different values, same channel
        //
        Arguments.arguments(
            Map.of(
                "StationA.ChannelA",
                Set.of(
                    AcquiredChannelEnvironmentIssueBoolean.create(
                        "StationA.ChannelA",
                        AcquiredChannelEnvironmentIssueType.CLIPPED,
                        Instant.ofEpochMilli(20),
                        Instant.ofEpochMilli(40),
                        true
                    ),
                    AcquiredChannelEnvironmentIssueBoolean.create(
                        "StationA.ChannelA",
                        AcquiredChannelEnvironmentIssueType.CLIPPED,
                        Instant.ofEpochMilli(21),
                        Instant.ofEpochMilli(39),
                        false
                    )
                )
            ),
            getMockStationSohDefinition(
                "StationA",
                Set.of("StationA.ChannelA"),
                Map.of(
                    SohMonitorType.ENV_CLIPPED,
                    TimeWindowDefinition.create(
                        Duration.ofMillis(60),
                        Duration.ZERO
                    )
                )
            ),
            PercentStationAggregate.from(
                50.0,
                StationAggregateType.ENVIRONMENTAL_ISSUES
            ),
            Instant.ofEpochMilli(60)
        ),

        //
        // Two of different type with different values, same channel
        //
        Arguments.arguments(
            Map.of(
                "StationA.ChannelA",
                Set.of(
                    AcquiredChannelEnvironmentIssueBoolean.create(
                        "StationA.ChannelA",
                        AcquiredChannelEnvironmentIssueType.CLIPPED,
                        Instant.ofEpochMilli(20),
                        Instant.ofEpochMilli(40),
                        true
                    ),
                    AcquiredChannelEnvironmentIssueBoolean.create(
                        "StationA.ChannelA",
                        AcquiredChannelEnvironmentIssueType.AUTHENTICATION_SEAL_BROKEN,
                        Instant.ofEpochMilli(21),
                        Instant.ofEpochMilli(39),
                        false
                    )
                )
            ),
            getMockStationSohDefinition(
                "StationA",
                Set.of("StationA.ChannelA"),
                Map.of(
                    SohMonitorType.ENV_CLIPPED,
                    TimeWindowDefinition.create(
                        Duration.ofMillis(60),
                        Duration.ZERO
                    ),
                    SohMonitorType.ENV_AUTHENTICATION_SEAL_BROKEN,
                    TimeWindowDefinition.create(
                        Duration.ofMillis(50),
                        Duration.ZERO
                    )
                )
            ),
            PercentStationAggregate.from(
                50.0,
                StationAggregateType.ENVIRONMENTAL_ISSUES
            ),
            Instant.ofEpochMilli(60)
        ),

        //
        // Two of different type with different values, same channel, but one
        // outside calculation window (large back-off duration)
        //
        Arguments.arguments(
            Map.of(
                "StationA.ChannelA",
                Set.of(
                    AcquiredChannelEnvironmentIssueBoolean.create(
                        "StationA.ChannelA",
                        AcquiredChannelEnvironmentIssueType.CLIPPED,
                        Instant.ofEpochMilli(20),
                        Instant.ofEpochMilli(40),
                        true
                    ),
                    AcquiredChannelEnvironmentIssueBoolean.create(
                        "StationA.ChannelA",
                        AcquiredChannelEnvironmentIssueType.AUTHENTICATION_SEAL_BROKEN,
                        Instant.ofEpochMilli(21),
                        Instant.ofEpochMilli(39),
                        false
                    )
                )
            ),
            getMockStationSohDefinition(
                "StationA",
                Set.of("StationA.ChannelA"),
                Map.of(
                    SohMonitorType.ENV_CLIPPED,
                    TimeWindowDefinition.create(
                        Duration.ofMillis(60),
                        Duration.ZERO
                    ),
                    SohMonitorType.ENV_AUTHENTICATION_SEAL_BROKEN,
                    TimeWindowDefinition.create(
                        Duration.ofMillis(50),
                        Duration.ofMillis(61)
                    )
                )
            ),
            PercentStationAggregate.from(
                100.0,
                StationAggregateType.ENVIRONMENTAL_ISSUES
            ),
            Instant.ofEpochMilli(60)
        ),

        //
        // Four of different type with different values, different channels
        //
        Arguments.arguments(
            Map.of(
                "StationA.ChannelA",
                Set.of(
                    AcquiredChannelEnvironmentIssueBoolean.create(
                        "StationA.ChannelA",
                        AcquiredChannelEnvironmentIssueType.CLIPPED,
                        Instant.ofEpochMilli(20),
                        Instant.ofEpochMilli(40),
                        true
                    ),
                    AcquiredChannelEnvironmentIssueBoolean.create(
                        "StationA.ChannelA",
                        AcquiredChannelEnvironmentIssueType.BACKUP_POWER_UNSTABLE,
                        Instant.ofEpochMilli(20),
                        Instant.ofEpochMilli(40),
                        true
                    ),
                    AcquiredChannelEnvironmentIssueBoolean.create(
                        "StationA.ChannelA",
                        AcquiredChannelEnvironmentIssueType.AMPLIFIER_SATURATION_DETECTED,
                        Instant.ofEpochMilli(20),
                        Instant.ofEpochMilli(40),
                        true
                    )
                ),
                "StationA.ChannelB",
                Set.of(
                    AcquiredChannelEnvironmentIssueBoolean.create(
                        "ChannelB",
                        AcquiredChannelEnvironmentIssueType.AUTHENTICATION_SEAL_BROKEN,
                        Instant.ofEpochMilli(21),
                        Instant.ofEpochMilli(39),
                        false
                    )
                )
            ),
            getMockStationSohDefinition(
                "StationA",
                Set.of("StationA.ChannelA", "StationA.ChannelB"),
                Map.of(
                    SohMonitorType.ENV_CLIPPED,
                    TimeWindowDefinition.create(
                        Duration.ofMillis(60),
                        Duration.ZERO
                    ),
                    SohMonitorType.ENV_BACKUP_POWER_UNSTABLE,
                    TimeWindowDefinition.create(
                        Duration.ofMillis(60),
                        Duration.ZERO
                    ),
                    SohMonitorType.ENV_AMPLIFIER_SATURATION_DETECTED,
                    TimeWindowDefinition.create(
                        Duration.ofMillis(60),
                        Duration.ZERO
                    ),
                    SohMonitorType.ENV_AUTHENTICATION_SEAL_BROKEN,
                    TimeWindowDefinition.create(
                        Duration.ofMillis(50),
                        Duration.ZERO
                    )
                )
            ),
            PercentStationAggregate.from(
                75.0,
                StationAggregateType.ENVIRONMENTAL_ISSUES
            ),
            Instant.ofEpochMilli(60)
        ),

        Arguments.arguments(
            Map.of(
                "StationA.ChannelA",
                Set.of(
                    AcquiredChannelEnvironmentIssueBoolean.create(
                        "StationA.ChannelA",
                        AcquiredChannelEnvironmentIssueType.CLIPPED,
                        Instant.ofEpochMilli(20),
                        Instant.ofEpochMilli(40),
                        true
                    ),
                    AcquiredChannelEnvironmentIssueBoolean.create(
                        "StationA.ChannelA",
                        AcquiredChannelEnvironmentIssueType.BACKUP_POWER_UNSTABLE,
                        Instant.ofEpochMilli(20),
                        Instant.ofEpochMilli(40),
                        true
                    ),
                    AcquiredChannelEnvironmentIssueBoolean.create(
                        "StationA.ChannelA",
                        AcquiredChannelEnvironmentIssueType.AMPLIFIER_SATURATION_DETECTED,
                        Instant.ofEpochMilli(20),
                        Instant.ofEpochMilli(40),
                        true
                    )
                ),
                "StationA.ChannelB",
                Set.of(
                    AcquiredChannelEnvironmentIssueBoolean.create(
                        "StationA.ChannelB",
                        AcquiredChannelEnvironmentIssueType.AUTHENTICATION_SEAL_BROKEN,
                        Instant.ofEpochMilli(21),
                        Instant.ofEpochMilli(39),
                        false
                    )
                )
            ),
            getMockStationSohDefinition(
                "StationA",
                Set.of("StationA.ChannelA", "StationA.ChannelB"),
                Map.of(
                    SohMonitorType.ENV_CLIPPED,
                    TimeWindowDefinition.create(
                        Duration.ofMillis(60),
                        Duration.ZERO
                    ),
                    SohMonitorType.ENV_BACKUP_POWER_UNSTABLE,
                    TimeWindowDefinition.create(
                        Duration.ofMillis(60),
                        Duration.ZERO
                    ),
                    SohMonitorType.ENV_AMPLIFIER_SATURATION_DETECTED,
                    TimeWindowDefinition.create(
                        Duration.ofMillis(60),
                        Duration.ZERO
                    ),
                    SohMonitorType.ENV_AUTHENTICATION_SEAL_BROKEN,
                    TimeWindowDefinition.create(
                        Duration.ofMillis(50),
                        Duration.ZERO
                    )
                )
            ),
            PercentStationAggregate.from(
                75.0,
                StationAggregateType.ENVIRONMENTAL_ISSUES
            ),
            Instant.ofEpochMilli(60)
        ),

        Arguments.arguments(
            Map.of(
                "StationA.ChannelA",
                Set.of()
            ),
            getMockStationSohDefinition(
                "StationA",
                Set.of("StationA.ChannelA"),
                Map.of(
                    SohMonitorType.ENV_CLIPPED,
                    TimeWindowDefinition.create(
                        Duration.ofMillis(60),
                        Duration.ZERO
                    )
                )
            ),
            PercentStationAggregate.from(
                null,
                StationAggregateType.ENVIRONMENTAL_ISSUES
            ),
            Instant.ofEpochMilli(60)
        )
    );
  }

  @ParameterizedTest
  @MethodSource("timelinessTestSource")
  void testTimeliness(
      Map<String, Set<WaveformSummaryAndReceptionTime>> waveformSummaryAndReceptionTimeMap,
      StationSohDefinition stationSohDefinition,
      DurationStationAggregate expectedTimelinessAggregate,
      Instant now
  ) {

    var cache = new AcquiredSampleTimesByChannel();

    waveformSummaryAndReceptionTimeMap.forEach(
        (k, v) -> v.forEach(
            waveformSummaryAndReceptionTime -> cache.update(
                k, waveformSummaryAndReceptionTime.getWaveformSummary().getEndTime()
            )
        )
    );

    var utility = new StationAggregateCalculationUtility(cache, now);

    var stationAggregates = utility.buildStationAggregateMono(
        Mono.just(waveformSummaryAndReceptionTimeMap),
        Mono.just(Map.of()),
        stationSohDefinition
    ).block();

    var actualLagAggregateOpt = stationAggregates.stream().filter(
        stationAggregate -> stationAggregate.getAggregateType()
            == StationAggregateType.TIMELINESS
    ).findFirst();

    Assertions.assertTrue(
        actualLagAggregateOpt.isPresent()
    );

    Assertions.assertEquals(
        expectedTimelinessAggregate,
        actualLagAggregateOpt.get()
    );
  }

  private static Stream<Arguments> timelinessTestSource() {

    var waveformSummary1 = WaveformSummary.from(
        "MyLovelyStation.channelName",
        Instant.EPOCH,
        Instant.ofEpochMilli(20)
    );

    var waveformSummary2 = WaveformSummary.from(
        "MyLovelyStation.channelName",
        Instant.EPOCH,
        Instant.ofEpochMilli(30)
    );

    var waveformSummary3 = WaveformSummary.from(
        "MyLovelyStation.channelNameA",
        Instant.EPOCH,
        Instant.ofEpochMilli(50)
    );

    return Stream.of(
        Arguments.arguments(
            Map.of(
                "MyLovelyStation.channelName",
                Set.of(
                    WaveformSummaryAndReceptionTime
                        .create(waveformSummary1, Instant.ofEpochMilli(100)),
                    WaveformSummaryAndReceptionTime
                        .create(waveformSummary2, Instant.ofEpochMilli(200))
                )
            ),
            getMockStationSohDefinition(
                "MyLovelyStation",
                Set.of("MyLovelyStation.channelName"),
                Map.of()
            ),
            DurationStationAggregate.from(
                Duration.ofMillis(20),
                StationAggregateType.TIMELINESS
            ),
            Instant.ofEpochMilli(50)
        ),

        Arguments.arguments(
            Map.of(
                "MyLovelyStation.channelName",
                Set.of(
                    WaveformSummaryAndReceptionTime
                        .create(waveformSummary1, Instant.ofEpochMilli(100)),
                    WaveformSummaryAndReceptionTime
                        .create(waveformSummary2, Instant.ofEpochMilli(200))
                ),
                "MyLovelyStation.channelNameA",
                Set.of(
                    WaveformSummaryAndReceptionTime
                        .create(waveformSummary3, Instant.ofEpochMilli(100))
                )
            ),
            getMockStationSohDefinition(
                "MyLovelyStation",
                Set.of("MyLovelyStation.channelName", "MyLovelyStation.channelNameA"),
                Map.of()
            ),
            DurationStationAggregate.from(
                Duration.ofMillis(0),
                StationAggregateType.TIMELINESS
            ),
            Instant.ofEpochMilli(50)
        )
    );
  }

  /**
   * Cannot simply use Assertions.assertEquals of these, since a small delta in the values will make it fail.
   *
   * @param expected
   * @param actual
   */
  private static void assertEqual(PercentStationAggregate expected,
      PercentStationAggregate actual) {

    Assertions.assertSame(expected.getAggregateType(), actual.getAggregateType());
    Assertions.assertSame(expected.stationValueType(), actual.stationValueType());

    Assertions.assertEquals(expected.getValue().isPresent(), actual.getValue().isPresent());

    if (expected.getValue().isPresent()) {
      double expectedValue = expected.getValue().get();
      double actualValue = actual.getValue().get();
      Assertions.assertEquals(expectedValue, actualValue, 1e-9);
    }
  }

  private static StationSohDefinition getMockStationSohDefinition(
      String stationName,
      Set<String> channelNames,
      Map<SohMonitorType, TimeWindowDefinition> timeWindowDefinitionMap
  ) {

    StationSohDefinition mockStationSohDefinition = Mockito.mock(StationSohDefinition.class);

    Mockito.when(mockStationSohDefinition.getStationName()).thenReturn(stationName);

    //ChannelSohDefinition mockChannelSohDefinition = Mockito.mock(ChannelSohDefinition.class);

    Map<String, ChannelSohDefinition> fakeChannelSohDefinitionMap = channelNames.stream().map(
        channelName -> {
          ChannelSohDefinition mockChannelSohDefinition = Mockito.mock(ChannelSohDefinition.class);

          Mockito.when(mockChannelSohDefinition.getChannelName()).thenReturn(channelName);

          return Map.entry(
              channelName,
              mockChannelSohDefinition
          );
        }
    ).collect(Collectors.toMap(Entry::getKey, Entry::getValue));

    Mockito.when(mockStationSohDefinition.getChannelDefinitionMap()).thenReturn(
        fakeChannelSohDefinitionMap
    );

    Mockito.when(mockStationSohDefinition.getChannelSohDefinitions()).thenReturn(
        new HashSet<>(fakeChannelSohDefinitionMap.values())
    );

    var channelsByMonitorType = new HashMap<SohMonitorType, Set<String>>();

    //
    // Just associate all types with all channels
    //
    channelsByMonitorType.putAll(
        Map.of(
            SohMonitorType.LAG, channelNames,
            SohMonitorType.TIMELINESS, channelNames,
            SohMonitorType.MISSING, channelNames
        )
    );

    channelsByMonitorType.putAll(
        SohMonitorType.validTypes().stream().filter(SohMonitorType::isEnvironmentIssue)
            .map(
                sohMonitorType -> Map.entry(
                    sohMonitorType,
                    channelNames
                )
            ).collect(Collectors.toMap(Entry::getKey, Entry::getValue))
    );

    Mockito.when(mockStationSohDefinition.getChannelsBySohMonitorType()).thenReturn(
        channelsByMonitorType
    );

    var augmentedTimeWindowDefinitionMap = new HashMap<SohMonitorType, TimeWindowDefinition>();

    //
    // Assuming here that the definitions always contain all SohMonitorTypes, because that
    // should be what the config mechanism is doing.
    //
    augmentedTimeWindowDefinitionMap.put(
        SohMonitorType.LAG,
        TimeWindowDefinition.create(
            Duration.ZERO,
            Duration.ZERO
        )
    );

    augmentedTimeWindowDefinitionMap.put(
        SohMonitorType.MISSING,
        TimeWindowDefinition.create(
            Duration.ZERO,
            Duration.ZERO
        )
    );

    augmentedTimeWindowDefinitionMap.put(
        SohMonitorType.TIMELINESS,
        TimeWindowDefinition.create(
            Duration.ZERO,
            Duration.ZERO
        )
    );

    augmentedTimeWindowDefinitionMap.putAll(
        timeWindowDefinitionMap
    );

    Mockito.when(mockStationSohDefinition.getTimeWindowBySohMonitorType()).thenReturn(
        augmentedTimeWindowDefinitionMap
    );

    return mockStationSohDefinition;
  }
}
