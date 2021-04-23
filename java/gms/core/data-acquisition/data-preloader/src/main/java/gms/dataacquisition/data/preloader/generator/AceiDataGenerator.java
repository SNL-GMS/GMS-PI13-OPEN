package gms.dataacquisition.data.preloader.generator;

import static java.util.stream.Collectors.toList;

import gms.dataacquisition.data.preloader.GenerationSpec;
import gms.shared.frameworks.injector.AceiIdModifier;
import gms.shared.frameworks.osd.api.SohRepositoryInterface;
import gms.shared.frameworks.osd.coi.channel.Channel;
import gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssue;
import java.time.Duration;
import java.util.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AceiDataGenerator<T extends AcquiredChannelEnvironmentIssue<?>> extends
    CoiDataGenerator<T, AceiIdModifier> {

  protected static final Logger logger = LoggerFactory.getLogger(AceiDataGenerator.class);

  public AceiDataGenerator(GenerationSpec generationSpec, SohRepositoryInterface sohRepository) {
    super(generationSpec, sohRepository);
  }

  @Override
  protected Collection<String> getSeedNames() {
    return channels()
        .map(Channel::getName)
        .distinct()
        .collect(toList());
  }

  @Override
  protected AceiIdModifier getModifier(Duration generationFrequency) {
    return new AceiIdModifier(generationFrequency);
  }
}
