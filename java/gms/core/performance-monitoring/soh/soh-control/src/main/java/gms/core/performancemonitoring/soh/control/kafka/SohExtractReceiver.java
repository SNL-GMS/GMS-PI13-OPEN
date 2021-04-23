package gms.core.performancemonitoring.soh.control.kafka;

import gms.core.performancemonitoring.soh.control.kafka.KafkaSohExtractConsumerFactory.SohExtractKafkaConsumer;
import gms.shared.frameworks.osd.coi.channel.dataacquisitionstatus.AcquiredStationSohExtract;
import java.time.Duration;
import java.util.List;

/**
 * Defines entities that consume {@link AcquiredStationSohExtract} instances from some source,
 * periodically sending a group of them to a registered subscriber.
 */
public interface SohExtractReceiver {

  /**
   * Begin consumption.
   * @param processingInterval how long to buffer extracts consumed before forwarding
   *   them to a subscriber. This must not be null.
   * @param extractSubscriber the subscriber to which extracts are forwarded. Must not be null.
   * @throws IllegalStateException if called while already consuming messages.
   */
  void receive(
      Duration processingInterval,
      SohExtractKafkaConsumer sohExtractKafkaConsumer,
      List<AcquiredStationSohExtract> cacheData
  );

  /**
   * Halts the emitting of filtered extracts from the cache.
   */
  void stopProcessingInterval();

  /**
   * Halts the emitting of filtered extracts from the cache, and stops the Kafka recevier
   * flux
   */
  void stop();

  /**
   * Call to determine if the receiver is currently consuming.
   * @return true or false.
   */
  boolean isReceiving();

  void setCachingDuration(Duration cachingDuration);

}
