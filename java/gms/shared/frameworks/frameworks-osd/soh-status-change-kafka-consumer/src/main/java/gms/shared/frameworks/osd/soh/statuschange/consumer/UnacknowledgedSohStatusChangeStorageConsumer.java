package gms.shared.frameworks.osd.soh.statuschange.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import gms.shared.frameworks.messaging.AbstractKafkaConsumerApplication;
import gms.shared.frameworks.osd.coi.datatransferobjects.CoiObjectMapperFactory;
import gms.shared.frameworks.osd.coi.soh.quieting.UnacknowledgedSohStatusChange;
import gms.shared.frameworks.soh.repository.SohRepository;
import gms.shared.frameworks.soh.repository.SohRepositoryFactory;
import java.io.IOException;
import java.util.Collection;
import java.util.Optional;

public class UnacknowledgedSohStatusChangeStorageConsumer extends
    AbstractKafkaConsumerApplication<UnacknowledgedSohStatusChange> {

  private static final ObjectMapper objectMapper = CoiObjectMapperFactory.getJsonObjectMapper();
  private SohRepository sohRepository;

  public static void main(String[] args) {
    new UnacknowledgedSohStatusChangeStorageConsumer().run();
  }

  @Override
  protected String getComponentName() {
    return "soh-status-change-kafka-consumer";
  }

  @Override
  protected void initialize() {
    super.initialize();
    getLogger().info("Initializing SohRepository...");
    sohRepository = SohRepositoryFactory.createSohRepository(getSystemConfig());
  }

  @Override
  protected Optional<UnacknowledgedSohStatusChange> parseMessage(String messageString) {
    getLogger().debug("received UnacknowledgedSohStatusChange. Parsing...");
    try {
      return Optional.ofNullable(objectMapper.readValue(messageString, UnacknowledgedSohStatusChange.class));
    } catch (IOException e) {
      getLogger().error("Error parsing UnacknowledgedSohStatusChange", e);
      return Optional.empty();
    }
  }

  @Override
  protected void consumeRecords(Collection<UnacknowledgedSohStatusChange> records) {
    if (!records.isEmpty()) {
      getLogger().debug("Storing {} UnacknowledgedSohStatusChange records", records.size());
      getExecutorService().submit(() -> sohRepository.storeUnacknowledgedSohStatusChange(records));
    }
  }
}
