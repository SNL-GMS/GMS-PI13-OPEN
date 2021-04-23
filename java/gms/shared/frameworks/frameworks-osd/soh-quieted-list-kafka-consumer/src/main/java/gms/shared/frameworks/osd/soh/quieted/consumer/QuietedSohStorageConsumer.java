package gms.shared.frameworks.osd.soh.quieted.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import gms.shared.frameworks.messaging.AbstractKafkaConsumerApplication;
import gms.shared.frameworks.osd.coi.datatransferobjects.CoiObjectMapperFactory;
import gms.shared.frameworks.osd.coi.soh.quieting.QuietedSohStatusChange;
import gms.shared.frameworks.soh.repository.SohRepository;
import gms.shared.frameworks.soh.repository.SohRepositoryFactory;
import java.io.IOException;
import java.util.Collection;
import java.util.Optional;


public class QuietedSohStorageConsumer extends
    AbstractKafkaConsumerApplication<QuietedSohStatusChange> {

    private static final ObjectMapper objectMapper = CoiObjectMapperFactory.getJsonObjectMapper();
    private SohRepository sohRepository;

    public static void main(String[] args) {
        new QuietedSohStorageConsumer().run();
    }

    @Override
    protected String getComponentName() {
        return "soh-quieted-list-kafka-consumer";
    }

    @Override
    protected void initialize() {
        super.initialize();
        getLogger().info("Initializing SohRepository...");
        this.sohRepository = SohRepositoryFactory.createSohRepository(getSystemConfig());
    }

    @Override
    protected Optional<QuietedSohStatusChange> parseMessage(String messageString) {
        getLogger().debug("Received QuietedSohStatusChange JSON. Parsing...");
        try {
            return Optional.ofNullable(
                objectMapper.readValue(messageString, QuietedSohStatusChange.class));
        } catch (IOException e) {
            getLogger().error("Error parsing QuietedSohStatusChange.", e);
            return Optional.empty();
        }
    }


    @Override
    protected void consumeRecords(Collection<QuietedSohStatusChange> records) {
        if (!records.isEmpty()) {
            getLogger().debug("Storing {} QuietedSohStatusChange records", records.size());
            getExecutorService().submit(() -> sohRepository.storeQuietedSohStatusChangeList(records));
        }
    }
}
