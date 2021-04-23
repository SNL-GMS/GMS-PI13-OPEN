package gms.core.performancemonitoring.ssam.control.dataprovider;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import gms.shared.frameworks.osd.coi.datatransferobjects.CoiObjectMapperFactory;
import gms.shared.frameworks.systemconfig.SystemConfig;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import reactor.core.publisher.Flux;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;

/**
 * FluxProvider that is able to create a Flux of the desired type from a Kafka topic
 * @param <T> The type of object that will be emitted from the flux
 */
public class KafkaFluxProvider<T> implements FluxProvider<T> {

  private static final String APPLICATION_ID = "application-id";
  private static final String KAFKA_BOOTSTRAP_SERVERS = "kafka-bootstrap-servers";
  private static final ObjectMapper MAPPER = CoiObjectMapperFactory.getJsonObjectMapper();

  protected final Flux<T> data;

  protected KafkaFluxProvider(Class<T> fluxType, String topicName, SystemConfig systemConfig) {
    ReceiverOptions<String, String> options = createReceiverOptions(systemConfig).subscription(List.of(topicName));
    data = KafkaReceiver.create(options).receive()
        .doOnNext(record -> record.receiverOffset().commit())
        .map(record -> {
          try {
            return Optional.of(MAPPER.readValue(record.value(), fluxType));
          } catch (JsonProcessingException ex) {
            return Optional.<T>empty();
          }
        })
        .filter(Optional::isPresent)
        .map(Optional::get);
  }

  public static <T> KafkaFluxProvider<T> create(Class<T> fluxType, String topicName, SystemConfig systemConfig) {
    Objects.requireNonNull(fluxType);
    Objects.requireNonNull(topicName);
    Preconditions.checkState(!topicName.isBlank());
    Objects.requireNonNull(systemConfig);
    return new KafkaFluxProvider<>(fluxType, topicName, systemConfig);
  }

  @Override
  public Flux<T> getFlux() {
    return data;
  }

  private ReceiverOptions<String, String> createReceiverOptions(SystemConfig systemConfig) {
    Map<String, Object> props = new HashMap<>();
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
        systemConfig.getValue(KAFKA_BOOTSTRAP_SERVERS));
    props.put(ConsumerConfig.GROUP_ID_CONFIG, systemConfig.getValue(APPLICATION_ID));
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
    return ReceiverOptions.create(props);
  }

}
