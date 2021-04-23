package gms.dataacquisition.stationreceiver.cd11.dataman;

import gms.dataacquisition.stationreceiver.cd11.dataman.configuration.KafkaConnectionConfiguration;
import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.clients.producer.Producer;

public class MockProducerFactory<S, T> implements ProducerFactory<S, T> {

  public Producer<S, T> makeProducer(String threadName,
      KafkaConnectionConfiguration kafkaConnectionConfiguration) {
    return new MockProducer<>();
  }
}
