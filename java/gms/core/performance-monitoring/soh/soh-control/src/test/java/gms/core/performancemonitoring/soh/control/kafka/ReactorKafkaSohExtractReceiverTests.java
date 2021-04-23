package gms.core.performancemonitoring.soh.control.kafka;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Functions;
import gms.core.performancemonitoring.soh.control.TestFixture;
import gms.shared.frameworks.osd.coi.channel.dataacquisitionstatus.AcquiredStationSohExtract;
import gms.shared.frameworks.osd.coi.datatransferobjects.CoiObjectMapperFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.record.TimestampType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOffset;
import reactor.kafka.receiver.ReceiverRecord;
import reactor.kafka.sender.TransactionManager;

@Disabled("Thread yield() calls cause intermittent failures in the pipeline")
class ReactorKafkaSohExtractReceiverTests {

  @Test
  void testReceiverReceivesAsse() throws IOException {

    var extracts = TestFixture.loadExtracts();

    var mockKafkaReceiver = getKafkaReceiver(
        Flux.just(Flux.fromIterable(extracts)
            .delayElements(Duration.ofMillis(5))
        )
    );

    ReactorKafkaSohExtractReceiver kafkaSohExtractReceiver = new ReactorKafkaSohExtractReceiver(
        mockKafkaReceiver,
        Duration.ofHours(20000)
    );

    List<AcquiredStationSohExtract> extractsReceivedList = new ArrayList<>();

    List<ReceiverOffset> receiverOffsets = new ArrayList<>();

    kafkaSohExtractReceiver.receive(
        Duration.ofSeconds(1),
        p -> {
          extractsReceivedList.addAll(p.getT1());
          receiverOffsets.addAll(p.getT2());
        },
        List.of()
    );

    waitWhile(
        () -> extractsReceivedList.size() < extracts.size(),
        2000 + extracts.size() * 5
    );

    try {
      Assertions.assertEquals(
          1,
          receiverOffsets.size()
      );

      Assertions.assertEquals(
          5,
          receiverOffsets.get(0).offset()
      );

      Assertions.assertEquals(
          extracts.size(),
          extractsReceivedList.size()
      );
    } finally {
      kafkaSohExtractReceiver.stop();
    }

  }

  @Test
  void testCacheGetsInitialized() throws IOException {

    var extracts = TestFixture.loadExtracts();
    var extraExtracts = loadAlternateExtracts();

    var mockKafkaReceiver = getKafkaReceiver(
        Flux.just(Flux.fromIterable(extracts)
            .delayElements(Duration.ofMillis(5))
        )
    );

    ReactorKafkaSohExtractReceiver kafkaSohExtractReceiver = new ReactorKafkaSohExtractReceiver(
        mockKafkaReceiver,
        Duration.ofHours(20000)
    );

    List<AcquiredStationSohExtract> extractsReceivedList = new ArrayList<>();

    List<ReceiverOffset> receiverOffsets = new ArrayList<>();

    kafkaSohExtractReceiver.receive(
        Duration.ofSeconds(1),
        p -> {
          extractsReceivedList.addAll(p.getT1());
          receiverOffsets.addAll(p.getT2());
        },
        extraExtracts
    );

    var startMs = System.currentTimeMillis();

    while (extractsReceivedList.size() < extracts.size()
        && 2000 + startMs + extracts.size() * 5 > System.currentTimeMillis()) {
      Thread.yield();
    }

    try {
      Assertions.assertEquals(
          1,
          receiverOffsets.size()
      );

      Assertions.assertEquals(
          5,
          receiverOffsets.get(0).offset()
      );

      Assertions.assertEquals(
          extracts.size() + extraExtracts.size(),
          extractsReceivedList.size()
      );
    } finally {
      kafkaSohExtractReceiver.stop();
    }
  }

  @Test
  void testCacheExpires() throws IOException {

    var extracts = TestFixture.loadExtracts();
    var alternateExtracts = loadAlternateExtracts();

    var batchEmitter = EmitterProcessor.<Flux<AcquiredStationSohExtract>>create();
    var batchEmitterSink = batchEmitter.sink();

    var mockKafkaReceiver = getKafkaReceiver(batchEmitter);

    ReactorKafkaSohExtractReceiver kafkaSohExtractReceiver = new ReactorKafkaSohExtractReceiver(
        mockKafkaReceiver,
        Duration.ofHours(20000)
    );

    kafkaSohExtractReceiver.receive(
        Duration.ofMillis(100),
        pair -> {
        },
        List.of()
    );

    var totalExpectedSize = extracts.size() + alternateExtracts.size();

    try {
      //
      // Test in the order: cache should not be empty, then cache should be empty, so that
      // there is something in the cache to be filtered, which is the functionality we are testing
      //
      List.of(false, true)
          .forEach(cacheShouldBeEmpty -> {
                waitWhile(
                    () -> true,
                    100
                );

                //
                // Add a new batch of extracts to be emitted
                //
                batchEmitterSink.next(
                    Flux.concat(
                        Flux.fromIterable(extracts),
                        Flux.fromIterable(alternateExtracts)
                    ).delayElements(Duration.ofMillis(2))
                );

                if (cacheShouldBeEmpty) {

                  //
                  // Set ridiculously low cache limit, expecting the cache to be empty
                  //
                  kafkaSohExtractReceiver.setCachingDuration(Duration.ofMillis(1));
                  waitWhile(
                      () -> kafkaSohExtractReceiver.getCachedExtractsSnapshot().size() > 0,
                      2000
                  );
                } else {

                  //
                  // Set ridiculously high cache limit, expecting the cache to be maxed
                  //
                  kafkaSohExtractReceiver.setCachingDuration(Duration.ofHours(200000));
                  waitWhile(
                      () -> kafkaSohExtractReceiver.getCachedExtractsSnapshot().size()
                          < totalExpectedSize,
                      2000
                  );
                }

                assertEquals(
                    cacheShouldBeEmpty ? 0 : totalExpectedSize,
                    kafkaSohExtractReceiver.getCachedExtractsSnapshot().size()
                );

              }
          );
    } finally {
      kafkaSohExtractReceiver.stop();
    }
  }

  private static KafkaReceiver<String, String> getKafkaReceiver(
      Flux<Flux<AcquiredStationSohExtract>> asseBatchFlux
  ) {

    return new KafkaReceiver<>() {
      @Override
      public Flux<ReceiverRecord<String, String>> receive() {
        AtomicInteger offsetRef = new AtomicInteger();

        return asseBatchFlux
            .flatMap(
                Functions.identity()
            )
            .map(acquiredStationSohExtract -> {
                  var objectMapper = CoiObjectMapperFactory.getJsonObjectMapper();

                  String serializedAsse;
                  try {
                    serializedAsse = objectMapper.writeValueAsString(
                        acquiredStationSohExtract
                    );
                  } catch (JsonProcessingException e) {
                    throw new IllegalStateException(e);
                  }

                  int offset = offsetRef.incrementAndGet();

                  return new ReceiverRecord<>(
                      new ConsumerRecord<>(
                          "DUMMY-TOPIC",
                          0,
                          offset,
                          Instant.now().toEpochMilli(),
                          TimestampType.CREATE_TIME,
                          0,
                          0,
                          serializedAsse.getBytes().length,
                          null,
                          serializedAsse
                      ),
                      new ReceiverOffset() {
                        @Override
                        public TopicPartition topicPartition() {
                          return new TopicPartition(
                              "DUMMY-TOPIC",
                              0
                          );
                        }

                        @Override
                        public long offset() {
                          return offset;
                        }

                        @Override
                        public void acknowledge() {
                        }

                        @Override
                        public Mono<Void> commit() {
                          return null;
                        }
                      }
                  );

                }
            );
      }

      @Override
      public Flux<Flux<ConsumerRecord<String, String>>> receiveAutoAck() {
        return null;
      }

      @Override
      public Flux<ConsumerRecord<String, String>> receiveAtmostOnce() {
        return null;
      }

      @Override
      public Flux<Flux<ConsumerRecord<String, String>>> receiveExactlyOnce(
          TransactionManager transactionManager) {
        return null;
      }

      @Override
      public <T> Mono<T> doOnConsumer(Function<Consumer<String, String>, ? extends T> function) {
        return null;
      }
    };
  }

  @Disabled("Thread yield() calls cause intermittent failures in the pipeline")
  static List<AcquiredStationSohExtract> loadAlternateExtracts() throws IOException {

    InputStream is = TestFixture.class.getResourceAsStream("/sohextracts-fakestations1.json");
    ObjectMapper objectMapper = CoiObjectMapperFactory.getJsonObjectMapper();
    JavaType extractListType = objectMapper.getTypeFactory().constructCollectionType(List.class, AcquiredStationSohExtract.class);
    return objectMapper.readValue(is, extractListType);
  }

  private static void waitWhile(Supplier<Boolean> truthSupplier, int maxDurationMillis) {

    var startMs = System.currentTimeMillis();

    while (truthSupplier.get() && maxDurationMillis + startMs > System.currentTimeMillis()) {
      Thread.yield();
    }
  }
}
