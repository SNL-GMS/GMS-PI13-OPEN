package gms.dataacquisition.data.preloader.generator;

import static java.util.stream.Collectors.toList;

import gms.dataacquisition.data.preloader.GenerationSpec;
import gms.shared.frameworks.injector.StationSohIdModifier;
import gms.shared.frameworks.osd.api.SohRepositoryInterface;
import gms.shared.frameworks.osd.coi.signaldetection.Station;
import gms.shared.frameworks.osd.coi.soh.ChannelSoh;
import gms.shared.frameworks.osd.coi.soh.DurationSohMonitorValueAndStatus;
import gms.shared.frameworks.osd.coi.soh.DurationStationAggregate;
import gms.shared.frameworks.osd.coi.soh.PercentSohMonitorValueAndStatus;
import gms.shared.frameworks.osd.coi.soh.PercentStationAggregate;
import gms.shared.frameworks.osd.coi.soh.SohMonitorType;
import gms.shared.frameworks.osd.coi.soh.SohMonitorValueAndStatus;
import gms.shared.frameworks.osd.coi.soh.SohStatus;
import gms.shared.frameworks.osd.coi.soh.StationAggregateType;
import gms.shared.frameworks.osd.coi.soh.StationSoh;
import java.time.Duration;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StationSohDataGenerator extends CoiDataGenerator<StationSoh, StationSohIdModifier> {

  private static final Logger logger = LoggerFactory.getLogger(StationSohDataGenerator.class);

  public StationSohDataGenerator(GenerationSpec generationSpec,
      SohRepositoryInterface sohRepository) {
    super(generationSpec, sohRepository);
  }

  @Override
  protected Collection<String> getSeedNames() {
    return stations()
        .map(Station::getName)
        .distinct()
        .collect(toList());
  }

  @Override
  protected StationSoh generateSeed(String stationName) {
    logger.debug("StationSoh seed generation - STARTING...");
    PercentStationAggregate missingStationAggregate = PercentStationAggregate
        .from(100.0, StationAggregateType.MISSING);
    DurationStationAggregate lagStationAggregate = DurationStationAggregate
        .from(Duration.ofHours(1), StationAggregateType.LAG);

    final var station = stations()
        .filter(s -> stationName.equals(s.getName()))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Station not found: " + stationName));

    final var smvs = getSmvs();

    final var channelSoh = station.getChannels()
        .stream().map(channel -> ChannelSoh.from(channel.getName(),
            SohStatus.BAD, smvs)).collect(Collectors.toSet());

    final var stationSoh = StationSoh
        .create(seedTime, stationName, smvs, SohStatus.MARGINAL, channelSoh,
            Set.of(missingStationAggregate, lagStationAggregate));

    logger.debug("StationSoh seed generation - COMPLETE");
    return stationSoh;
  }

  @Override
  protected StationSohIdModifier getModifier(Duration generationFrequency) {
    return new StationSohIdModifier(generationFrequency);
  }

  private Set<SohMonitorValueAndStatus<?>> getSmvs() {
    DurationSohMonitorValueAndStatus marginalLagSohMonitorValueAndStatus =
        DurationSohMonitorValueAndStatus.from(Duration.ofHours(1),
            SohStatus.MARGINAL,
            SohMonitorType.LAG);
    PercentSohMonitorValueAndStatus marginalMissingSohMonitorValueAndStatus =
        PercentSohMonitorValueAndStatus.from(99.0, SohStatus.MARGINAL, SohMonitorType.MISSING);
    return Set.of(marginalLagSohMonitorValueAndStatus, marginalMissingSohMonitorValueAndStatus);
  }

  @Override
  protected void consumeRecords(Iterable<StationSoh> records) {
    logger.debug("StationSoh consuming records - STARTING...");
    sohRepository.storeStationSoh(convertToSet(records));
    logger.debug("StationSoh consuming records - COMPLETE");
  }


}
