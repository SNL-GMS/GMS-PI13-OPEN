package gms.core.performancemonitoring.ssam.control.cache;

import gms.shared.frameworks.osd.coi.soh.CapabilitySohRollup;
import gms.shared.frameworks.osd.coi.soh.ChannelSoh;
import gms.shared.frameworks.osd.coi.soh.PercentSohMonitorValueAndStatus;
import gms.shared.frameworks.osd.coi.soh.SohMonitorType;
import gms.shared.frameworks.osd.coi.soh.SohStatus;
import gms.shared.frameworks.osd.coi.soh.StationSoh;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.lang3.tuple.Pair;

//
// Set of test data and track/add operations to exercise StationSohCorrelatingCacheProcessor
//
class StationSohCorrelatingCacheProcessorTestFixtures {

  //
  // Represents a track or add "operation". If we only have a capabilitySohRollup, it will signal
  // that the tester needs to call StationSohCorrelatingCacheProcessor::track. If we only have a
  // stationSoh, it will signal that StationSohCorrelatingCacheProcessor::add needs to be called.
  //
  // This is used to order track and add in any way that we like for a given test case.
  //
  static class CapabilityOrStationSoh {

    final CapabilitySohRollup capabilitySohRollup;
    final StationSoh stationSoh;

    public CapabilityOrStationSoh(
        CapabilitySohRollup capabilitySohRollup) {
      this.capabilitySohRollup = capabilitySohRollup;
      this.stationSoh = null;
    }

    public CapabilityOrStationSoh(StationSoh stationSoh) {
      this.stationSoh = stationSoh;
      this.capabilitySohRollup = null;
    }
  }

  //
  // Below classes are more like "namespaces" that contain various use cases.
  //

  //
  // Simple "feel good" test for the correlator. Just two capability rollups and a single
  // stationSOH
  //
  static class SimpleCorrelationTest {

    private final static UUID stationASohUuid = UUID.randomUUID();

    public static CapabilitySohRollup capabilitySohRollup_sA_sgB = CapabilitySohRollup.create(
        UUID.randomUUID(),
        Instant.now(),
        SohStatus.BAD,
        "StationGroupB",
        Set.of(stationASohUuid),
        Map.of(
            "StationA",
            SohStatus.GOOD
        )
    );

    public static CapabilitySohRollup capabilitySohRollup_sA_sgA = CapabilitySohRollup.create(
        UUID.randomUUID(),
        Instant.now(),
        SohStatus.BAD,
        "StationGroupA",
        Set.of(stationASohUuid),
        Map.of(
            "StationA",
            SohStatus.GOOD
        )
    );

    public static StationSoh stationSohA = StationSoh.from(
        stationASohUuid,
        Instant.now(),
        "StationA",
        Set.of(PercentSohMonitorValueAndStatus.from(
            0.0, SohStatus.MARGINAL, SohMonitorType.MISSING
        )),
            SohStatus.BAD,
        Set.of(
            ChannelSoh.from(
                "StationA.ChannelA",
                    SohStatus.BAD,
                Set.of(PercentSohMonitorValueAndStatus.from(
                    0.0, SohStatus.MARGINAL, SohMonitorType.MISSING
                ))
            )
        ),
        Set.of()
    );

    public static List<CapabilityOrStationSoh> operations = List.of(
        new CapabilityOrStationSoh(capabilitySohRollup_sA_sgA),
        new CapabilityOrStationSoh(capabilitySohRollup_sA_sgB),
        new CapabilityOrStationSoh(stationSohA)
    );

    public static List<Pair<CapabilitySohRollup, List<StationSoh>>> expectedFinalPairs = List.of(
        Pair.of(
            capabilitySohRollup_sA_sgB,
            List.of(stationSohA)
        ),
        Pair.of(
            capabilitySohRollup_sA_sgA,
            List.of(stationSohA)
        )
    );
  }

  //
  // Represents one way to interweave the track and add operations. This is likely to happen
  // in the real world because we are tracking CapabilitySohRollup and adding StationSohs in via
  // two independent Fluxes
  //
  // C_S_C_S - CapabilitySohRollup, followed by a StationSoh, then another CapabilitySohRollup,
  //     then another StationSoh
  //
  static class InterweavedTest_C_S_C_S {

    private final static UUID stationASohUuid = UUID.randomUUID();
    private final static UUID stationBSohUuid = UUID.randomUUID();

    public static CapabilitySohRollup capabilitySohRollup_sA_sgB = CapabilitySohRollup.create(
        UUID.randomUUID(),
        Instant.now(),
        SohStatus.BAD,
        "StationGroupB",
        Set.of(stationASohUuid, stationBSohUuid),
        Map.of(
            "StationA",
            SohStatus.GOOD,
            "StationB",
            SohStatus.BAD
        )
    );

    public static CapabilitySohRollup capabilitySohRollup_sA_sgA = CapabilitySohRollup.create(
        UUID.randomUUID(),
        Instant.now(),
        SohStatus.BAD,
        "StationGroupA",
        Set.of(stationASohUuid),
        Map.of(
            "StationA",
            SohStatus.GOOD
        )
    );

    public static StationSoh stationSohA = StationSoh.from(
        stationASohUuid,
        Instant.now(),
        "StationA",
        Set.of(PercentSohMonitorValueAndStatus.from(
            0.0, SohStatus.MARGINAL, SohMonitorType.MISSING
        )),
            SohStatus.BAD,
        Set.of(
            ChannelSoh.from(
                "StationA.ChannelA",
                    SohStatus.BAD,
                Set.of(PercentSohMonitorValueAndStatus.from(
                    0.0, SohStatus.MARGINAL, SohMonitorType.MISSING
                ))
            )
        ),
        Set.of()
    );

    public static StationSoh stationSohB = StationSoh.from(
        stationBSohUuid,
        Instant.now(),
        "StationB",
        Set.of(PercentSohMonitorValueAndStatus.from(
            0.0, SohStatus.MARGINAL, SohMonitorType.MISSING
        )),
            SohStatus.BAD,
        Set.of(
            ChannelSoh.from(
                "StationB.ChannelA",
                    SohStatus.BAD,
                Set.of(PercentSohMonitorValueAndStatus.from(
                    0.0, SohStatus.MARGINAL, SohMonitorType.MISSING
                ))
            )
        ),
        Set.of()
    );

    public static List<CapabilityOrStationSoh> operations = List.of(
        new CapabilityOrStationSoh(capabilitySohRollup_sA_sgA),
        new CapabilityOrStationSoh(stationSohB),
        new CapabilityOrStationSoh(capabilitySohRollup_sA_sgB),
        new CapabilityOrStationSoh(stationSohA)

    );

    public static List<Pair<CapabilitySohRollup, List<StationSoh>>> expectedFinalPairs = List.of(
        Pair.of(
            capabilitySohRollup_sA_sgB,
            List.of(stationSohA, stationSohB)
        ),
        Pair.of(
            capabilitySohRollup_sA_sgA,
            List.of(stationSohA)
        )
    );
  }

  //
  // Just another way to interweave
  //
  static class InterweavedTest_C_S_S_C {

    private final static UUID stationASohUuid = UUID.randomUUID();
    private final static UUID stationBSohUuid = UUID.randomUUID();

    public static CapabilitySohRollup capabilitySohRollup_sA_sgB = CapabilitySohRollup.create(
        UUID.randomUUID(),
        Instant.now(),
        SohStatus.BAD,
        "StationGroupB",
        Set.of(stationASohUuid, stationBSohUuid),
        Map.of(
            "StationA",
            SohStatus.GOOD,
            "StationB",
            SohStatus.BAD
        )
    );

    public static CapabilitySohRollup capabilitySohRollup_sA_sgA = CapabilitySohRollup.create(
        UUID.randomUUID(),
        Instant.now(),
        SohStatus.BAD,
        "StationGroupA",
        Set.of(stationASohUuid),
        Map.of(
            "StationA",
            SohStatus.GOOD
        )
    );

    public static StationSoh stationSohA = StationSoh.from(
        stationASohUuid,
        Instant.now(),
        "StationA",
        Set.of(PercentSohMonitorValueAndStatus.from(
            0.0, SohStatus.MARGINAL, SohMonitorType.MISSING
        )),
            SohStatus.BAD,
        Set.of(
            ChannelSoh.from(
                "StationA.ChannelA",
                    SohStatus.BAD,
                Set.of(PercentSohMonitorValueAndStatus.from(
                    0.0, SohStatus.MARGINAL, SohMonitorType.MISSING
                ))
            )
        ),
        Set.of()
    );

    public static StationSoh stationSohB = StationSoh.from(
        stationBSohUuid,
        Instant.now(),
        "StationB",
        Set.of(PercentSohMonitorValueAndStatus.from(
            0.0, SohStatus.MARGINAL, SohMonitorType.MISSING
        )),
            SohStatus.BAD,
        Set.of(
            ChannelSoh.from(
                "StationB.ChannelA",
                    SohStatus.BAD,
                Set.of(PercentSohMonitorValueAndStatus.from(
                    0.0, SohStatus.MARGINAL, SohMonitorType.MISSING
                ))
            )
        ),
        Set.of()
    );

    public static List<CapabilityOrStationSoh> operations = List.of(
        new CapabilityOrStationSoh(capabilitySohRollup_sA_sgA),
        new CapabilityOrStationSoh(stationSohB),
        new CapabilityOrStationSoh(stationSohA),
        new CapabilityOrStationSoh(capabilitySohRollup_sA_sgB)
    );

    public static List<Pair<CapabilitySohRollup, List<StationSoh>>> expectedFinalPairs = List.of(
        Pair.of(
            capabilitySohRollup_sA_sgA,
            List.of(stationSohA)
        ),
        Pair.of(
            capabilitySohRollup_sA_sgB,
            List.of(stationSohA, stationSohB)
        )
    );

  }

  //
  // Test the case we we get all of the StationSoh objects first.
  //
  static class StationsFirstTest {

    private final static UUID stationASohUuid = UUID.randomUUID();
    private final static UUID stationBSohUuid = UUID.randomUUID();

    public static CapabilitySohRollup capabilitySohRollup_sA_sgB = CapabilitySohRollup.create(
        UUID.randomUUID(),
        Instant.now(),
        SohStatus.BAD,
        "StationGroupB",
        Set.of(stationASohUuid, stationBSohUuid),
        Map.of(
            "StationA",
            SohStatus.GOOD,
            "StationB",
            SohStatus.BAD
        )
    );

    public static CapabilitySohRollup capabilitySohRollup_sA_sgA = CapabilitySohRollup.create(
        UUID.randomUUID(),
        Instant.now(),
        SohStatus.BAD,
        "StationGroupA",
        Set.of(stationASohUuid),
        Map.of(
            "StationA",
            SohStatus.GOOD
        )
    );

    public static StationSoh stationSohA = StationSoh.from(
        stationASohUuid,
        Instant.now(),
        "StationA",
        Set.of(PercentSohMonitorValueAndStatus.from(
            0.0, SohStatus.MARGINAL, SohMonitorType.MISSING
        )),
            SohStatus.BAD,
        Set.of(
            ChannelSoh.from(
                "StationA.ChannelA",
                    SohStatus.BAD,
                Set.of(PercentSohMonitorValueAndStatus.from(
                    0.0, SohStatus.MARGINAL, SohMonitorType.MISSING
                ))
            )
        ),
        Set.of()
    );

    public static StationSoh stationSohB = StationSoh.from(
        stationBSohUuid,
        Instant.now(),
        "StationB",
        Set.of(PercentSohMonitorValueAndStatus.from(
            0.0, SohStatus.MARGINAL, SohMonitorType.MISSING
        )),
            SohStatus.BAD,
        Set.of(
            ChannelSoh.from(
                "StationB.ChannelA",
                    SohStatus.BAD,
                Set.of(PercentSohMonitorValueAndStatus.from(
                    0.0, SohStatus.MARGINAL, SohMonitorType.MISSING
                ))
            )
        ),
        Set.of()
    );

    public static List<CapabilityOrStationSoh> operations = List.of(
        new CapabilityOrStationSoh(stationSohB),
        new CapabilityOrStationSoh(stationSohA),
        new CapabilityOrStationSoh(capabilitySohRollup_sA_sgA),
        new CapabilityOrStationSoh(capabilitySohRollup_sA_sgB)
    );

    public static List<Pair<CapabilitySohRollup, List<StationSoh>>> expectedFinalPairs = List.of(
        Pair.of(
            capabilitySohRollup_sA_sgA,
            List.of(stationSohA)
        ),
        Pair.of(
            capabilitySohRollup_sA_sgB,
            List.of(stationSohA, stationSohB)
        )
    );
  }

  //
  // Test the case we we get all of the StationSoh objects last.
  //
  static class StationsLastTest {

    private final static UUID stationASohUuid = UUID.randomUUID();
    private final static UUID stationBSohUuid = UUID.randomUUID();

    public static CapabilitySohRollup capabilitySohRollup_sA_sgB = CapabilitySohRollup.create(
        UUID.randomUUID(),
        Instant.now(),
        SohStatus.BAD,
        "StationGroupB",
        Set.of(stationASohUuid, stationBSohUuid),
        Map.of(
            "StationA",
            SohStatus.GOOD,
            "StationB",
            SohStatus.BAD
        )
    );

    public static CapabilitySohRollup capabilitySohRollup_sA_sgA = CapabilitySohRollup.create(
        UUID.randomUUID(),
        Instant.now(),
        SohStatus.BAD,
        "StationGroupA",
        Set.of(stationASohUuid),
        Map.of(
            "StationA",
            SohStatus.GOOD
        )
    );

    public static StationSoh stationSohA = StationSoh.from(
        stationASohUuid,
        Instant.now(),
        "StationA",
        Set.of(PercentSohMonitorValueAndStatus.from(
            0.0, SohStatus.MARGINAL, SohMonitorType.MISSING
        )),
            SohStatus.BAD,
        Set.of(
            ChannelSoh.from(
                "StationA.ChannelA",
                    SohStatus.BAD,
                Set.of(PercentSohMonitorValueAndStatus.from(
                    0.0, SohStatus.MARGINAL, SohMonitorType.MISSING
                ))
            )
        ),
        Set.of()
    );

    public static StationSoh stationSohB = StationSoh.from(
        stationBSohUuid,
        Instant.now(),
        "StationB",
        Set.of(PercentSohMonitorValueAndStatus.from(
            0.0, SohStatus.MARGINAL, SohMonitorType.MISSING
        )),
            SohStatus.BAD,
        Set.of(
            ChannelSoh.from(
                "StationB.ChannelA",
                    SohStatus.BAD,
                Set.of(PercentSohMonitorValueAndStatus.from(
                    0.0, SohStatus.MARGINAL, SohMonitorType.MISSING
                ))
            )
        ),
        Set.of()
    );

    public static List<CapabilityOrStationSoh> operations = List.of(
        new CapabilityOrStationSoh(capabilitySohRollup_sA_sgA),
        new CapabilityOrStationSoh(capabilitySohRollup_sA_sgB),
        new CapabilityOrStationSoh(stationSohB),
        new CapabilityOrStationSoh(stationSohA)
    );

    public static List<Pair<CapabilitySohRollup, List<StationSoh>>> expectedFinalPairs = List.of(
        Pair.of(
            capabilitySohRollup_sA_sgB,
            List.of(stationSohA, stationSohB)
        ),
        Pair.of(
            capabilitySohRollup_sA_sgA,
            List.of(stationSohA)
        )
    );
  }

}
