package gms.dataacquisition.kafka.client;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Consumer {

  private static final Logger logger = LoggerFactory.getLogger(Consumer.class);

  private final KafkaConsumer<String, String> kafkaConsumer;
  private final int numberOfMessagesToConsume;

  public Consumer(String topic, int numberOfMessagesToConsume) {
    Properties properties = buildProperties();
    this.numberOfMessagesToConsume = numberOfMessagesToConsume;
    this.kafkaConsumer = new KafkaConsumer<>(properties);
    this.kafkaConsumer.subscribe(Collections.singletonList(topic));
  }

  public void run() {
    var numberOfMessagesConsumed = 0;
    while (numberOfMessagesConsumed < numberOfMessagesToConsume) {
      ConsumerRecords<String, String> records = kafkaConsumer.poll(Duration.ofMillis(100));
      for (ConsumerRecord<String, String> record : records) {
        numberOfMessagesConsumed++;
      }
      if (Thread.currentThread().isInterrupted()) {
        logger.info("received cancel");
        return;
      }
    }
  }

  private Properties buildProperties() {
    Properties properties = new Properties();
    properties.setProperty("bootstrap.servers", "kafka1:9092");
    properties.setProperty("group.id", "extract-consumer");
    properties.setProperty("key.deserializer",
        "org.apache.kafka.common.serialization.StringDeserializer");
    properties.setProperty("value.deserializer",
        "org.apache.kafka.common.serialization.StringDeserializer");
    return properties;
  }

}
