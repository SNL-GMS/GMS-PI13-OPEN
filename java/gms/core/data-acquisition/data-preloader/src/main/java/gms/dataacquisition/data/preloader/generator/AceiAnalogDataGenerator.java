package gms.dataacquisition.data.preloader.generator;

import gms.dataacquisition.data.preloader.GenerationSpec;
import gms.shared.frameworks.osd.api.SohRepositoryInterface;
import gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssue.AcquiredChannelEnvironmentIssueType;
import gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssueAnalog;

public class AceiAnalogDataGenerator extends
    AceiDataGenerator<AcquiredChannelEnvironmentIssueAnalog> {


  public AceiAnalogDataGenerator(GenerationSpec generationSpec,
      SohRepositoryInterface sohRepository) {
    super(generationSpec, sohRepository);
  }

  @Override
  protected void consumeRecords(Iterable<AcquiredChannelEnvironmentIssueAnalog> records) {
    logger.debug("ACEI consuming records - STARTING...");

    sohRepository.storeAcquiredChannelSohAnalog(convertToSet(records));

    logger.debug("ACEI consuming records - COMPLETE");
  }

  @Override
  protected AcquiredChannelEnvironmentIssueAnalog generateSeed(String channelName) {
    logger.debug("ACEI seed generation - STARTING...");

    final var type = AcquiredChannelEnvironmentIssueType.CLOCK_DIFFERENTIAL_IN_MICROSECONDS;
    final var status = 0.0;

    final var acei = AcquiredChannelEnvironmentIssueAnalog
        .create(channelName,
            type,
            seedTime,
            seedTime.plus(generationFrequency),
            status);

    logger.debug("ACEI seed generation - COMPLETE");

    return acei;
  }

}
