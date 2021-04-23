package gms.shared.frameworks.osd.rsdf.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import gms.shared.frameworks.messaging.AbstractKafkaConsumerApplication;
import gms.shared.frameworks.osd.coi.datatransferobjects.CoiObjectMapperFactory;
import gms.shared.frameworks.osd.coi.waveforms.RawStationDataFrame;
import gms.shared.frameworks.soh.repository.SohRepository;
import gms.shared.frameworks.soh.repository.SohRepositoryFactory;
import java.io.IOException;
import java.util.Collection;
import java.util.Optional;

public class RsdfStorageConsumer extends AbstractKafkaConsumerApplication<RawStationDataFrame> {

  private static final ObjectMapper objectMapper = CoiObjectMapperFactory.getJsonObjectMapper();
  private SohRepository sohRepository;

  public static void main(String[] args) {
    new RsdfStorageConsumer().run();
  }

  @Override
  protected String getComponentName() {
    return "osd-rsdf-kafka-consumer";
  }

  @Override
  protected void initialize() {
    super.initialize();
    getLogger().info("Initializing SohRepository...");
    this.sohRepository = SohRepositoryFactory.createSohRepository(getSystemConfig());
  }

  @Override
  protected Optional<RawStationDataFrame> parseMessage(String messageString) {
    getLogger().debug("Received RSDF. Parsing...");
    try {
      return Optional.ofNullable(objectMapper.readValue(messageString, RawStationDataFrame.class));
    } catch (IOException e) {
      getLogger().error("Error parsing RSDF", e);
      return Optional.empty();
    }
  }

  @Override
  protected void consumeRecords(Collection<RawStationDataFrame> records) {
    if (!records.isEmpty()) {
      getLogger().debug("Storing {} RSDF records", records.size());
      getExecutorService().submit(() -> sohRepository.storeRawStationDataFrames(records));
    }
  }
}
