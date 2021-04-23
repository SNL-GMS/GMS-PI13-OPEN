package gms.core.performancemonitoring.ssam.control.datapublisher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gms.shared.frameworks.osd.coi.datatransferobjects.CoiObjectMapperFactory;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.KafkaOutbound;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderRecord;
import reactor.kafka.sender.SenderResult;
import reactor.kafka.sender.TransactionManager;

@Disabled("Thread yield() calls cause intermittent failures in the pipeline")
class KafkaPublisherTests {

  @Test
  void testKafkaPublisher() {

    var topic = "MY_AMAZING_TOPIC";

    var list = new ArrayList<Instant>();

    var kafkaSender = new MockKafkaSender<>(
        Instant.class,
        list,
        topic
    );

    var instantFlux = Flux.fromIterable(
        List.of(
            Instant.EPOCH,
            Instant.EPOCH.plusSeconds(10),
            Instant.EPOCH.plusSeconds(20)
        )
    );

    var kafkaPublisher = new KafkaPublisher<>(
        instantFlux,
        kafkaSender,
        topic
    );

    kafkaPublisher.start();

    var startMs = System.currentTimeMillis();

    while (System.currentTimeMillis() - startMs < 1000) {
      Thread.yield();
    }

    Assertions.assertEquals(
        List.of(
            Instant.EPOCH,
            Instant.EPOCH.plusSeconds(10),
            Instant.EPOCH.plusSeconds(20)
        ),
        list
    );

  }

  private static class MockKafkaSender<T> implements KafkaSender<String, String> {

    private final List<T> listToPopulate;
    private final Class<T> type;
    private final String topic;

    MockKafkaSender(
        Class<T> type,
        List<T> listToPopulate,
        String topic
    ) {
      this.type = type;
      this.listToPopulate = listToPopulate;
      this.topic = topic;
    }

    @Override
    public <U> Flux<SenderResult<U>> send(
        Publisher<? extends SenderRecord<String, String, U>> records) {

      List<SenderResult<U>> results = new ArrayList<>();
      CountDownLatch latch = new CountDownLatch(1);
      AtomicLong offset = new AtomicLong(0);
      ObjectMapper om = CoiObjectMapperFactory.getJsonObjectMapper();

      records.subscribe(new BaseSubscriber<SenderRecord<String, String, U>>() {
        @Override
        protected void hookOnNext(SenderRecord<String, String, U> value) {

          Assertions.assertEquals(
              topic, value.topic()
          );

          AtomicReference<Exception> exceptionRef = new AtomicReference<>();
          try {
            //productList.add(om.readValue(value.value(), clazz));
            synchronized (listToPopulate) {
              listToPopulate.add(
                  om.readValue(value.value(), type)
              );
            }
          } catch (JsonProcessingException e) {
            exceptionRef.set(e);
          }

          RecordMetadata recordMetadata = new RecordMetadata(
              new TopicPartition(value.topic(), 0),
              0L,
              offset.getAndIncrement(),
              System.currentTimeMillis(),
              0L,
              value.key() != null ? value.key().getBytes().length : 0,
              value.value() != null ? value.value().getBytes().length : 0);

          results.add(new SenderResult<U>() {
            @Override
            public RecordMetadata recordMetadata() {
              return recordMetadata;
            }

            @Override
            public Exception exception() {
              return exceptionRef.get();
            }

            @Override
            public U correlationMetadata() {
              return value.correlationMetadata();
            }
          });
        }

        @Override
        protected void hookOnComplete() {
          latch.countDown();
        }

        @Override
        protected void hookOnError(Throwable throwable) {
          latch.countDown();
        }
      });

      try {
        latch.await();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }

      return Flux.fromIterable(results);
    }

    // The rest of the methods do nothing, since send() is the only one used.

    @Override
    public <T> Flux<Flux<SenderResult<T>>> sendTransactionally(
        Publisher<? extends Publisher<? extends SenderRecord<String, String, T>>> records) {
      return null;
    }

    @Override
    public TransactionManager transactionManager() {
      return null;
    }

    @Override
    public KafkaOutbound<String, String> createOutbound() {
      return null;
    }

    @Override
    public <T> Mono<T> doOnProducer(Function<Producer<String, String>, ? extends T> function) {
      return null;
    }

    @Override
    public void close() {
    }
  }
}
