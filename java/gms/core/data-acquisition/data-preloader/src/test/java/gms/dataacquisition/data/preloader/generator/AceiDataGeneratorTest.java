package gms.dataacquisition.data.preloader.generator;

import gms.dataacquisition.data.preloader.GenerationSpec;
import gms.shared.frameworks.injector.AceiIdModifier;
import gms.shared.frameworks.osd.api.SohRepositoryInterface;
import gms.shared.frameworks.osd.coi.channel.Channel;
import gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssue;
import gms.shared.frameworks.osd.coi.signaldetection.Station;
import gms.shared.frameworks.osd.coi.signaldetection.StationGroup;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

abstract class AceiDataGeneratorTest<G extends AceiDataGenerator<T>, T extends AcquiredChannelEnvironmentIssue<?>> extends
    CoiDataGeneratorTest<G, T, AceiIdModifier> {

  @Override
  protected abstract G getDataGenerator(GenerationSpec generationSpec,
      SohRepositoryInterface sohRepository);

  @Override
  protected List<String> getSeedNames() {
    return stationGroups.stream()
        .flatMap(g -> g.getStations().stream())
        .flatMap(g -> g.getChannels().stream())
        .map(Channel::getName)
        .distinct()
        .collect(Collectors.toList());
  }

  @Override
  protected List<T> getRecordsToSend() {
    return new ArrayList<>();
  }

  @Override
  protected int getWantedNumberOfItemsGenerated() {
    final var numberOfChannels = stationGroups.stream()
        .flatMap(g -> g.getStations().stream())
        .flatMap(s->s.getChannels().stream())
        .map(Channel::getName)
        .distinct()
        .count();

    return (int) Math.ceil((((double) generationDuration.toNanos()) / generationFrequency.toNanos())
        * numberOfChannels);
  }
}