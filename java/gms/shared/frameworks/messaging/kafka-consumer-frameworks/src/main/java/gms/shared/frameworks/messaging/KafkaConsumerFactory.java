package gms.shared.frameworks.messaging;

import gms.shared.frameworks.systemconfig.SystemConfig;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.function.Function;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.KafkaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KafkaConsumerFactory {

  //config keys for creating consumer properties
  private static final String APPLICATION_ID = "application-id";
  private static final String KAFKA_BOOTSTRAP_SERVERS = "kafka-bootstrap-servers";
  private static final String KAFKA_KEY_DESERIALIZER = "kafka-key-deserializer";
  private static final String KAFKA_VALUE_DESERIALIZER = "kafka-value-deserializer";
  private static final String KAFKA_CONSUMER_SESSION_TIMEOUT = "kafka-consumer-session-timeout";
  private static final String KAFKA_CONSUMER_HEARTBEAT_INTERVAL = "kafka-consumer-heartbeat-interval";

  private static final String INPUT_TOPIC = "input-topic";
  private static final String BATCH_SIZE_IN_SECONDS = "application-batch-size-in-seconds";

  private static final Logger logger = LoggerFactory.getLogger(KafkaConsumerFactory.class);

  private KafkaConsumerFactory() {
  }

  public static <T> KafkaConsumerRunner<T> createConsumerRunner(SystemConfig systemConfig,
      Function<String, Optional<T>> recordParser, Consumer<Collection<T>> recordConsumer) {

    Properties consumerProperties = buildConsumerProperties(systemConfig);
    String subscriptionTopic = systemConfig.getValue(INPUT_TOPIC);
    Duration pollingInterval = Duration
        .ofSeconds(systemConfig.getValueAsLong(BATCH_SIZE_IN_SECONDS));

    final RetryPolicy<Object> kafkaConnectionPolicy = new RetryPolicy<>()
        .withBackoff(50, 1000, ChronoUnit.MILLIS)
        .withMaxAttempts(100)
        .handle(KafkaException.class)
        .onFailedAttempt(e -> logger.warn("Failed connecting to kafka broker, will try again..."));

    org.apache.kafka.clients.consumer.Consumer<String, String> kafkaConsumer = Failsafe
        .with(kafkaConnectionPolicy).get(() -> new KafkaConsumer<>(consumerProperties));

    return KafkaConsumerRunner.create(kafkaConsumer, subscriptionTopic, pollingInterval,
        recordParser, recordConsumer);
  }

  /**
   * Populates a {@link Properties} using well-known keys to retrieve values from {@link
   * SystemConfig} for use by the {@link Consumer}
   *
   * @param config System configuration access
   * @return Properties for the consumer
   */
  private static Properties buildConsumerProperties(SystemConfig config) {
    // create properties to construct the consumer
    Properties properties = new Properties();
    properties.put(ConsumerConfig.GROUP_ID_CONFIG,
        config.getValue(APPLICATION_ID));
    properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
        config.getValue(KAFKA_BOOTSTRAP_SERVERS));
    properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
        config.getValue(KAFKA_KEY_DESERIALIZER));
    properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
        config.getValue(KAFKA_VALUE_DESERIALIZER));
    properties.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG,
        config.getValue(KAFKA_CONSUMER_SESSION_TIMEOUT));
    properties.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG,
        config.getValue(KAFKA_CONSUMER_HEARTBEAT_INTERVAL));

    //
    // We want more control over  commits to broker list to make sure we handled all messages
    //
    properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

    return properties;
  }

}
