package gms.dataacquisition.stationreceiver.cd11.dataman;

import gms.dataacquisition.stationreceiver.cd11.dataman.configuration.KafkaConnectionConfiguration;
import java.util.Properties;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;

public class KafkaProducerFactory<S, T> implements ProducerFactory<S, T> {

  /**
   * Constructs a Kafka producer used for publishing to a topic
   * @param kafkaClientId Unique name to be given to the client
   * @param kafkaConnectionConfiguration Configuration for the producer
   * @return A Kafka producer
   */
  public Producer<S, T> makeProducer(String kafkaClientId,
      KafkaConnectionConfiguration kafkaConnectionConfiguration) {
    return new KafkaProducer<>(buildKafkaProperties(kafkaClientId, kafkaConnectionConfiguration));
  }

  private static Properties buildKafkaProperties(String kafkaClientId,
      KafkaConnectionConfiguration kafkaConnectionConfiguration) {
    Properties props = new Properties();
    props.put(ProducerConfig.CLIENT_ID_CONFIG, kafkaClientId);
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConnectionConfiguration.getBootstrapServers());
    props.put(ProducerConfig.RETRIES_CONFIG, kafkaConnectionConfiguration.getRetries());
    props.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, kafkaConnectionConfiguration.getRetryBackoffMs());
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, kafkaConnectionConfiguration.getKeySerializer());
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, kafkaConnectionConfiguration.getValueSerializer());
    return props;
  }
}
