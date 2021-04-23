package gms.dataacquisition.data.preloader.generator;

import gms.dataacquisition.data.preloader.GenerationSpec;
import gms.shared.frameworks.osd.api.SohRepositoryInterface;
import gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssue.AcquiredChannelEnvironmentIssueType;
import gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssueBoolean;

public class AceiBooleanDataGenerator extends
    AceiDataGenerator<AcquiredChannelEnvironmentIssueBoolean> {


  public AceiBooleanDataGenerator(GenerationSpec generationSpec,
      SohRepositoryInterface sohRepository) {
    super(generationSpec, sohRepository);
  }

  @Override
  protected void consumeRecords(Iterable<AcquiredChannelEnvironmentIssueBoolean> records) {
    logger.debug("ACEI consuming records - STARTING...");

    sohRepository.storeAcquiredChannelEnvironmentIssueBoolean(convertToSet(records));

    logger.debug("ACEI consuming records - COMPLETE");
  }

  @Override
  protected AcquiredChannelEnvironmentIssueBoolean generateSeed(String channelName) {
    logger.debug("ACEI seed generation - STARTING...");

    final var type = AcquiredChannelEnvironmentIssueType.VAULT_DOOR_OPENED;
    final var status = false;

    final var acei = AcquiredChannelEnvironmentIssueBoolean
        .create(channelName,
            type,
            seedTime,
            seedTime.plus(generationFrequency),
            status);

    logger.debug("ACEI seed generation - COMPLETE");

    return acei;
  }

}
