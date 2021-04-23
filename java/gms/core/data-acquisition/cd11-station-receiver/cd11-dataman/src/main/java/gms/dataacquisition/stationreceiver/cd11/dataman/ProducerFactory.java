package gms.dataacquisition.stationreceiver.cd11.dataman;

import gms.dataacquisition.stationreceiver.cd11.dataman.configuration.KafkaConnectionConfiguration;
import org.apache.kafka.clients.producer.Producer;

public interface ProducerFactory<S, T> {

  Producer<S, T> makeProducer(String threadName,
      KafkaConnectionConfiguration kafkaConnectionConfiguration);
}
