package gms.shared.frameworks.osd.station.soh.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import gms.shared.frameworks.messaging.AbstractKafkaConsumerApplication;
import gms.shared.frameworks.osd.coi.datatransferobjects.CoiObjectMapperFactory;
import gms.shared.frameworks.osd.coi.soh.StationSoh;
import gms.shared.frameworks.soh.repository.SohRepository;
import gms.shared.frameworks.soh.repository.SohRepositoryFactory;
import java.io.IOException;
import java.util.Collection;
import java.util.Optional;

public class StationSohStorageConsumer extends AbstractKafkaConsumerApplication<StationSoh> {

  private static final ObjectMapper objectMapper = CoiObjectMapperFactory.getJsonObjectMapper();
  private SohRepository sohRepository;

  public static void main(String[] args) {
    new StationSohStorageConsumer().run();
  }

  protected SohRepository getSohRepository() {
    return sohRepository;
  }

  @Override
  protected String getComponentName() {
    return "osd-station-soh-kafka-consumer";
  }

  @Override
  protected void initialize() {
    super.initialize();
    getLogger().info("Initializing SohRepository...");
    this.sohRepository = SohRepositoryFactory.createSohRepository(getSystemConfig());
  }

  @Override
  protected Optional<StationSoh> parseMessage(String messageString) {
    getLogger().debug("Received StationSoh JSON. Parsing...");
    try {
      return Optional.ofNullable(objectMapper.readValue(messageString, StationSoh.class));
    } catch (IOException ex) {
      getLogger().error("Error parsing StationSoh", ex);
      return Optional.empty();
    }
  }

  @Override
  protected void consumeRecords(Collection<StationSoh> records) {
    if (!records.isEmpty()) {
      getLogger().debug("Storing {} StationSoh records", records.size());
      getExecutorService().submit(() -> getSohRepository().storeStationSoh(records));
    }
  }
}