package gms.shared.frameworks.osd.systemmessage.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import gms.shared.frameworks.messaging.AbstractKafkaConsumerApplication;
import gms.shared.frameworks.osd.coi.datatransferobjects.CoiObjectMapperFactory;
import gms.shared.frameworks.osd.coi.systemmessages.SystemMessage;
import gms.shared.frameworks.soh.repository.SohRepository;
import gms.shared.frameworks.soh.repository.SohRepositoryFactory;
import java.io.IOException;
import java.util.Collection;
import java.util.Optional;

public class SystemMessageStorageConsumer extends AbstractKafkaConsumerApplication<SystemMessage> {

  private static final ObjectMapper objectMapper = CoiObjectMapperFactory.getJsonObjectMapper();
  private SohRepository sohRepository;

  public static void main(String[] args) {
    new SystemMessageStorageConsumer().run();
  }

  @Override
  protected String getComponentName() {
    return "osd-systemmessage-kafka-consumer";
  }

  @Override
  protected void initialize() {
    super.initialize();
    getLogger().info("Initializing SohRepository...");
    this.sohRepository = SohRepositoryFactory.createSohRepository(getSystemConfig());
  }

  @Override
  protected Optional<SystemMessage> parseMessage(String messageString) {
    getLogger().debug("Received SystemMessage JSON. Parsing... {}", messageString);

    try {
      return Optional.ofNullable(objectMapper.readValue(messageString, SystemMessage.class));
    } catch (IOException e) {
      getLogger().error("Error parsing SystemMessage.", e);
      return Optional.empty();
    }
  }

  @Override
  protected void consumeRecords(Collection<SystemMessage> records) {
    if (!records.isEmpty()) {
      getLogger().info("Storing {} system messages ...", records.size());
      getLogger().debug("Storing {} SystemMessage records: {}", records.size(), records);
      getExecutorService().submit(() -> sohRepository.storeSystemMessages(records));
    }
  }
}
