package gms.shared.frameworks.osd.capability.soh.rollup.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import gms.shared.frameworks.messaging.AbstractKafkaConsumerApplication;
import gms.shared.frameworks.osd.coi.datatransferobjects.CoiObjectMapperFactory;
import gms.shared.frameworks.osd.coi.soh.CapabilitySohRollup;
import gms.shared.frameworks.soh.repository.SohRepository;
import gms.shared.frameworks.soh.repository.SohRepositoryFactory;
import java.io.IOException;
import java.util.Collection;
import java.util.Optional;


public class CapabilitySohRollupConsumer extends
    AbstractKafkaConsumerApplication<CapabilitySohRollup> {

  private static final ObjectMapper objectMapper = CoiObjectMapperFactory.getJsonObjectMapper();
  private SohRepository sohRepository;

  public static void main(String[] args) {
    new CapabilitySohRollupConsumer().run();
  }

  @Override
  protected String getComponentName() {
    return "capability-soh-rollup-kafka-consumer";
  }

  @Override
  protected void initialize() {
    super.initialize();
    getLogger().info("Initializing SohRepository...");
    this.sohRepository = SohRepositoryFactory.createSohRepository(getSystemConfig());
  }

  @Override
  protected Optional<CapabilitySohRollup> parseMessage(String messageString) {
    getLogger().debug("Received CapabilitySohRollup JSON. Parsing...");
    try {
      return Optional.ofNullable(
          objectMapper.readValue(messageString, CapabilitySohRollup.class));
    } catch (IOException e) {
      getLogger().error("Error parsing CapabilitySohRollup.", e);
      return Optional.empty();
    }
  }

  @Override
  protected void consumeRecords(Collection<CapabilitySohRollup> records) {
    getLogger().debug("Storing {} CapabilitySohRollup records", records.size());
    getExecutorService().submit(() -> sohRepository.storeCapabilitySohRollup(records));
  }
}
