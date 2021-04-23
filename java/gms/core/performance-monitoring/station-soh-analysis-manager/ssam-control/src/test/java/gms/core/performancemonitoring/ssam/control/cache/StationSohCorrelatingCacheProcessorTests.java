package gms.core.performancemonitoring.ssam.control.cache;

import gms.core.performancemonitoring.ssam.control.cache.StationSohCorrelatingCacheProcessorTestFixtures.CapabilityOrStationSoh;
import gms.core.performancemonitoring.ssam.control.cache.StationSohCorrelatingCacheProcessorTestFixtures.InterweavedTest_C_S_C_S;
import gms.core.performancemonitoring.ssam.control.cache.StationSohCorrelatingCacheProcessorTestFixtures.InterweavedTest_C_S_S_C;
import gms.core.performancemonitoring.ssam.control.cache.StationSohCorrelatingCacheProcessorTestFixtures.SimpleCorrelationTest;
import gms.core.performancemonitoring.ssam.control.cache.StationSohCorrelatingCacheProcessorTestFixtures.StationsFirstTest;
import gms.core.performancemonitoring.ssam.control.cache.StationSohCorrelatingCacheProcessorTestFixtures.StationsLastTest;
import gms.shared.frameworks.osd.coi.soh.CapabilitySohRollup;
import gms.shared.frameworks.osd.coi.soh.StationSoh;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.EmitterProcessor;
import reactor.test.StepVerifier;

class StationSohCorrelatingCacheProcessorTests {

  @ParameterizedTest
  @MethodSource("correlatingTestSource")
  void testCorrelating(
      List<CapabilityOrStationSoh> operations,
      List<Pair<CapabilitySohRollup, Set<StationSoh>>> expectedFinalPairs
  ) {

    var fluxProcessor = EmitterProcessor.<Pair<CapabilitySohRollup, List<StationSoh>>>create();
    var sink = fluxProcessor.sink();

    var processor = StationSohCorrelatingCacheProcessor.create(sink);

    operations.forEach(capabilityOrStationSoh -> {
      if (Objects.nonNull(capabilityOrStationSoh.capabilitySohRollup)) {
        processor.track(capabilityOrStationSoh.capabilitySohRollup);
      } else if (Objects.nonNull(capabilityOrStationSoh.stationSoh)) {
        processor.add(capabilityOrStationSoh.stationSoh);
      } else {
        throw new IllegalArgumentException("Not sure if you want to add or track");
      }
    });

    sink.complete();

    var expectedLinkedList = new LinkedList<>(expectedFinalPairs);

    StepVerifier.create(fluxProcessor)
        .recordWith(
            ArrayList::new
        )
        .expectNextCount(expectedFinalPairs.size())
        .consumeRecordedWith(list -> {
          list.forEach(
              capabilitySohRollupListPair -> {

                var expectedPair = expectedLinkedList.pop();

                Assertions.assertSame(
                    expectedPair.getLeft(), capabilitySohRollupListPair.getLeft()
                );

                Assertions.assertEquals(
                    new HashSet<>(expectedPair.getRight()),

                    //TODO: have the correlation code create a SET instead of a LIST!
                    new HashSet<>(capabilitySohRollupListPair.getRight())
                );
              }
          );
        }).verifyComplete();
  }

  private static Stream<Arguments> correlatingTestSource() {

    return Stream.of(
        Arguments.arguments(
            SimpleCorrelationTest.operations,
            SimpleCorrelationTest.expectedFinalPairs
        ),
        Arguments.arguments(
            InterweavedTest_C_S_C_S.operations,
            InterweavedTest_C_S_C_S.expectedFinalPairs
        ),
        Arguments.arguments(
            InterweavedTest_C_S_S_C.operations,
            InterweavedTest_C_S_S_C.expectedFinalPairs
        ),
        Arguments.arguments(
            StationsFirstTest.operations,
            StationsFirstTest.expectedFinalPairs
        ),
        Arguments.arguments(
            StationsLastTest.operations,
            StationsLastTest.expectedFinalPairs
        )
    );
  }
}
