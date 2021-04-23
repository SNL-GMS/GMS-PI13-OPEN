package gms.shared.frameworks.injector;

import gms.shared.frameworks.systemconfig.SystemConfig;
import java.util.Properties;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;

public class KafkaProducerFactory {

  private KafkaProducerFactory() {

  }

  public static KafkaProducer<String, String> getProducer(String clientId,
      String bootstrapServer,
      int retries,
      long retryBackoff) {
    SystemConfig systemConfig = SystemConfig.create("mockProducer");
    KafkaConnectionConfiguration kafkaConfig = KafkaConnectionConfiguration.create(systemConfig);
    Properties props = new Properties();
    props.put(ProducerConfig.CLIENT_ID_CONFIG, clientId);
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
    props.put(ProducerConfig.RETRIES_CONFIG, retries);
    props.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, retryBackoff);
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, kafkaConfig.getKeySerializer());
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, kafkaConfig.getValueSerializer());
//    props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");


    return new KafkaProducer<>(props);
  }

}
