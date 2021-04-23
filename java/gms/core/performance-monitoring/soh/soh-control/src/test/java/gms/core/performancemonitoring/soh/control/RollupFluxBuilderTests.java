package gms.core.performancemonitoring.soh.control;

import gms.core.performancemonitoring.soh.control.configuration.CapabilitySohRollupDefinition;
import gms.core.performancemonitoring.soh.control.configuration.StationSohDefinition;
import gms.shared.frameworks.osd.coi.channel.dataacquisitionstatus.AcquiredStationSohExtract;
import gms.shared.frameworks.osd.coi.soh.CapabilitySohRollup;
import gms.shared.frameworks.osd.coi.soh.StationSoh;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Flux;

@Disabled("Thread yield() calls cause intermittent failures in the pipeline")
class RollupFluxBuilderTests {

  private static final Logger logger = LogManager.getLogger(RollupFluxBuilderTests.class);

  private static Stream<Arguments> testSpammer() {

    return IntStream.range(0, 50).mapToObj(
        Arguments::arguments
    );
  }

  @ParameterizedTest
  @MethodSource("testSpammer")
  void testFluxBuilder(int dummy) throws IOException {

    //
    // Just some test to make sure we are building fluxes right and giving them back
    // correctly
    //

    Random random = new Random(0xDEADBEEF);

    List<AcquiredStationSohExtract> extracts = TestFixture.loadExtracts();

    Set<CapabilitySohRollupDefinition> rollupDefinitions = TestFixture
        .computeCapabilitySohRollupDefinitions(extracts, random);

    Set<StationSohDefinition> stationSohDefinitions = TestFixture
        .computeStationSohDefinitions(extracts, random);

    var cache = new AcquiredSampleTimesByChannel();
    cache.setLatestChannelToEndTime(Map.of());

    RollupFluxBuilder rollupFluxBuilder = new RollupFluxBuilder(
        Set.copyOf(extracts),
        stationSohDefinitions,
        rollupDefinitions,
        Duration.ofDays(100),
        cache
    );

    Flux<StationSoh> stationSohFlux = rollupFluxBuilder.getStationSohFlux();

    List<StationSoh> stationSohList = new ArrayList<>();
    List<StationSoh> stationSohDoubledList = new ArrayList<>();

    stationSohFlux.subscribe(
        stationSoh -> {
          stationSohList.add(stationSoh);
          stationSohDoubledList.add(stationSoh);
        }
    );

    Flux<StationSoh> stationSohFlux2 = rollupFluxBuilder.getStationSohFlux();

    stationSohFlux2
        .subscribe(stationSohDoubledList::add);

    AtomicBoolean done1 = new AtomicBoolean(false);

    AtomicBoolean done2 = new AtomicBoolean(false);

    List<CapabilitySohRollup> capabilitySohRollupList = new ArrayList<>();
    List<CapabilitySohRollup> capabilitySohRollupDoubledList = new ArrayList<>();

    Flux<CapabilitySohRollup> capabilitySohRollupFlux
        = rollupFluxBuilder.getCapabilitySohRollupFlux();

    capabilitySohRollupFlux.doOnComplete(
        () -> done1.set(true)
    ).doOnNext(
        capabilitySohRollup -> {
          capabilitySohRollupList.add(capabilitySohRollup);
          capabilitySohRollupDoubledList.add(capabilitySohRollup);
        }
    ).subscribe();

    capabilitySohRollupFlux
        = rollupFluxBuilder.getCapabilitySohRollupFlux();

    capabilitySohRollupFlux.doOnComplete(
        () -> done2.set(true)
    ).doOnNext(capabilitySohRollupDoubledList::add)
        .subscribe();

    var startMs = System.currentTimeMillis();

    boolean done1Printed = false;
    boolean done2Printed = false;

    while (System.currentTimeMillis() - startMs < 500) {
      // Wait for fluxes to complete
      Thread.yield();

      if (done1.get() && !done1Printed) {
        done1Printed = true;
        logger.info(
            "Test {}: StationSohs populated in {} ms",
            dummy + 1,
            System.currentTimeMillis() - startMs
        );
      }
      if (done2.get() && !done2Printed) {
        done2Printed = true;
        logger.info(
            "Test {}: CapabilitySohRollups populated in {} ms",
            dummy + 1,
            System.currentTimeMillis() - startMs
        );
      }
    }

    //
    // The number of StationSohs calculated should match the number of StationSohDefinitions.
    // Otherwise we are in a situation where we are unnecessarily repeating calculations.
    //
    Assertions.assertEquals(stationSohDefinitions.size(), stationSohList.size());

    Assertions.assertTrue(capabilitySohRollupList.size() > 0);

    Assertions.assertEquals(
        stationSohList.size() * 2,
        stationSohDoubledList.size()
    );

    Assertions.assertEquals(
        capabilitySohRollupList.size() * 2,
        capabilitySohRollupDoubledList.size()
    );
  }
}
