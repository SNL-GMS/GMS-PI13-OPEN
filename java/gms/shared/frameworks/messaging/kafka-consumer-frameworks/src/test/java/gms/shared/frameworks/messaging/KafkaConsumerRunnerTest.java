package gms.shared.frameworks.messaging;

import static java.util.Collections.singleton;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.MockConsumer;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

class KafkaConsumerRunnerTest {

  MockConsumer<String, String> consumer;

  @BeforeEach
  public void setUp() {
    consumer = new MockConsumer<>(OffsetResetStrategy.EARLIEST);
  }

  @Test
  void testRun() throws InterruptedException {
    String subscriptionTopic = "integers";
    Duration pollingInterval = Duration.ofMillis(100);
    List<Integer> expectedIntegers = List.of(5, 18, 23, 9, 72, 109, -10, 6);
    List<Integer> actualIntegers = new ArrayList<>();

    TopicPartition partition = new TopicPartition(subscriptionTopic, 0);
    consumer.subscribe(singleton(subscriptionTopic));
    consumer.rebalance(singleton(partition));
    consumer.updateBeginningOffsets(Map.of(partition, 0L));
    consumer.updateEndOffsets(Map.of(partition, (long) expectedIntegers.size()));

    ConsumerRecord<String, String> inputRecord;
    for (int i = 0; i < expectedIntegers.size(); i++) {
      inputRecord = new ConsumerRecord<>(subscriptionTopic, 0, i, "integer-key",
          expectedIntegers.get(i).toString());
      consumer.addRecord(inputRecord);
    }

    CountDownLatch consumedLatch = new CountDownLatch(expectedIntegers.size());
    KafkaConsumerRunner<Integer> consumerRunner = KafkaConsumerRunner.create(
        consumer, subscriptionTopic, pollingInterval,
        KafkaConsumerRunnerTest::parseInt, records -> {
          actualIntegers.addAll(records);
          records.forEach(record -> consumedLatch.countDown());
        });

    Thread consumerThread = new Thread(consumerRunner);
    assertDoesNotThrow(consumerThread::start);

    assertTrue(consumedLatch.await(2 * pollingInterval.getNano(), TimeUnit.NANOSECONDS));
    assertDoesNotThrow(consumerRunner::shutdown);

    consumerThread.join(500);
    assertTrue(consumer.closed());
    assertFalse(consumerThread.isAlive());
    assertEquals(expectedIntegers, actualIntegers);
  }

  private static Optional<Integer> parseInt(String intString) {
    try {
      return Optional.of(Integer.parseInt(intString));
    } catch (NumberFormatException e) {
      return Optional.empty();
    }
  }
}