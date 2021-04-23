package gms.core.dataacquisition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import gms.shared.frameworks.osd.api.SohRepositoryInterface;
import gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssue;
import gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssue.AcquiredChannelEnvironmentIssueType;
import gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssueAnalog;
import gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssueBoolean;
import gms.shared.frameworks.osd.coi.datatransferobjects.CoiObjectMapperFactory;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.reactivestreams.Subscriber;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.publisher.UnicastProcessor;
import reactor.kafka.receiver.ReceiverOffset;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

@ExtendWith(MockitoExtension.class)
class AceiMergeProcessorTest {

  private static final ObjectMapper jsonObjectMapper = CoiObjectMapperFactory.getJsonObjectMapper();

  private static final String CHANNEL_NAME = "LBTB.LBTB1.SHZ";

  private SohRepositoryInterface mockOsdRepository;

  @Captor
  ArgumentCaptor<Collection<AcquiredChannelEnvironmentIssueBoolean>> aceisCaptor;

  private AceiMergeProcessor processor;
  private TestAceiReactiveConsumer testAceiReactiveConsumer;

  @BeforeEach
  void setUp() {

    mockOsdRepository = Mockito.mock(SohRepositoryInterface.class);

    AceiMergeProcessorConfiguration configuration = buildDefaultTestConfiguration();
    Table<String, AcquiredChannelEnvironmentIssueType, AceiBooleanRangeMap> aceiRangeTable =
        HashBasedTable.create();

    testAceiReactiveConsumer = new TestAceiReactiveConsumer();

    processor = new AceiMergeProcessor(configuration, mockOsdRepository,
        aceiRangeTable, testAceiReactiveConsumer, true, false);

    processor.start();
  }

  @AfterEach
  void tearDown() {
    processor.stop();
  }

  private AceiMergeProcessorConfiguration buildDefaultTestConfiguration() {
    return AceiMergeProcessorConfiguration.builder()
        .setApplicationId("cd11-rsdf-processor")
        .setBootstrapServers("kafka1:9092,kafka2:9092,kafka3:9092")
        .setInputAceiTopic("soh.acei")
        .setKeySerializer("org.apache.kafka.common.serialization.Serdes$StringSerde")
        .setValueSerializer("org.apache.kafka.common.serialization.Serdes$StringSerde")
        .setNumberOfVerificationAttempts(1)
        .setStreamsCloseTimeoutMs(60000)
        .setConnectionRetryCount(10)
        .setRetryBackoffMs(1000L)
        .setMergeToleranceMs(500L)
        .setBenchmarkLoggingPeriodSeconds(600)
        .setCacheExpirationPeriodSeconds(1200)
        .setStoragePeriodMilliseconds(1000L)
        .setProcessorThreadCount(0)
        .setMaxItemsPerDbInteraction(32)
        .setMaxParallelDbOperations(1)
        .setMinItemsToPerformDbOperations(1)
        .build();
  }

  @Test
  void testStreamAceiBooleanMessage() throws Exception {

    AcquiredChannelEnvironmentIssueType issueType = AcquiredChannelEnvironmentIssueType.VAULT_DOOR_OPENED;
    Instant startTime = Instant.EPOCH;
    Instant endTime = startTime.plusSeconds(5);

    AcquiredChannelEnvironmentIssueBoolean firstAceiPublished = AcquiredChannelEnvironmentIssueBoolean
        .create(CHANNEL_NAME, issueType, startTime, endTime, true);
    AcquiredChannelEnvironmentIssueBoolean secondAceiPublished = AcquiredChannelEnvironmentIssueBoolean
        .create(CHANNEL_NAME, issueType, endTime.plusMillis(10), endTime.plusMillis(5010), true);

    testAceiReactiveConsumer.pipeInput(firstAceiPublished);

    // Give it time to do a storage interval, but don't wait more than 5 seconds.
    long totalWaitMs = 0L;
    while (processor.booleanACEIsInserted() < 1L && totalWaitMs < 5000L) {
      waitFor(10L);
      totalWaitMs += 10L;
    }

    verify(mockOsdRepository)
        .storeAcquiredChannelEnvironmentIssueBoolean(Set.of(firstAceiPublished));

    verify(mockOsdRepository, never())
        .removeAcquiredChannelEnvironmentIssueBooleans(anySet());

    testAceiReactiveConsumer.pipeInput(secondAceiPublished);

    totalWaitMs = 0L;
    while (processor.booleanACEIsInserted() < 2L && totalWaitMs < 5000L) {
      waitFor(10L);
      totalWaitMs += 10L;
    }

    AcquiredChannelEnvironmentIssueBoolean expectedMergedAcei = AcquiredChannelEnvironmentIssueBoolean
        .create(CHANNEL_NAME, issueType, startTime, endTime.plusMillis(5010), true);

    verify(mockOsdRepository, times(2))
        .storeAcquiredChannelEnvironmentIssueBoolean(aceisCaptor.capture());

    List<Collection<AcquiredChannelEnvironmentIssueBoolean>> storeInput = aceisCaptor.getAllValues();
    assertEquals(1, storeInput.get(1).size());
    assertTrue(expectedMergedAcei.hasSameState(storeInput.get(1).iterator().next()));

    verify(mockOsdRepository)
        .removeAcquiredChannelEnvironmentIssueBooleans(Set.of(firstAceiPublished));
  }

  @Test
  void testStreamAceiAnalogMessage() throws Exception{

    AcquiredChannelEnvironmentIssueType issueType = AcquiredChannelEnvironmentIssueType.MEAN_AMPLITUDE;
    Instant startTime = Instant.EPOCH;
    Instant endTime = startTime.plusSeconds(5);

    AcquiredChannelEnvironmentIssueAnalog firstAceiPublished = AcquiredChannelEnvironmentIssueAnalog
        .create(CHANNEL_NAME, issueType, startTime, endTime, 1.0);

    testAceiReactiveConsumer.pipeInput(firstAceiPublished);

    long totalWaitMs = 0L;
    while (processor.analogACEIsInserted() < 1L && totalWaitMs < 5000L) {
      waitFor(10L);
      totalWaitMs += 10L;
    }

    verify(mockOsdRepository).storeAcquiredChannelSohAnalog(Set.of(firstAceiPublished));

    AcquiredChannelEnvironmentIssueAnalog secondAceiPublished = AcquiredChannelEnvironmentIssueAnalog
        .create(CHANNEL_NAME, issueType, endTime.plusMillis(10), endTime.plusMillis(5010), 1.1);

    testAceiReactiveConsumer.pipeInput(secondAceiPublished);

    totalWaitMs = 0L;
    while (processor.analogACEIsInserted() < 2L && totalWaitMs < 5000L) {
      waitFor(10L);
      totalWaitMs += 10L;
    }

    verify(mockOsdRepository).storeAcquiredChannelSohAnalog(Set.of(secondAceiPublished));

  }

  static class TestAceiReactiveConsumer implements AceiReactiveConsumer {

    private FluxSink<Tuple2<AcquiredChannelEnvironmentIssue<?>, ReceiverOffset>> booleanSink;
    private FluxSink<Tuple2<AcquiredChannelEnvironmentIssue<?>, ReceiverOffset>> analogSink;
    private AtomicLong offsetRef = new AtomicLong(0L);

    @Override
    public void consume(
        Consumer<Tuple2<AcquiredChannelEnvironmentIssue<?>, ReceiverOffset>> booleanConsumer,
        Consumer<Tuple2<AcquiredChannelEnvironmentIssue<?>, ReceiverOffset>> analogConsumer) {

      UnicastProcessor<Tuple2<AcquiredChannelEnvironmentIssue<?>, ReceiverOffset>> booleanProcessor =
          UnicastProcessor.create();
      booleanSink = booleanProcessor.sink();

      UnicastProcessor<Tuple2<AcquiredChannelEnvironmentIssue<?>, ReceiverOffset>> analogProcessor =
          UnicastProcessor.create();
      analogSink = analogProcessor.sink();

      booleanProcessor.subscribe(booleanConsumer);
      analogProcessor.subscribe(analogConsumer);
    }

    @Override
    public void stop() {
      // noop
    }

    void pipeInput(AcquiredChannelEnvironmentIssue<?> ... aceis) {
      for (AcquiredChannelEnvironmentIssue<?> acei: aceis) {
        if (acei instanceof AcquiredChannelEnvironmentIssueBoolean) {
          booleanSink.next(Tuples.of(acei, new TestReceiverOffset(offsetRef.getAndIncrement())));
        } else if (acei instanceof AcquiredChannelEnvironmentIssueAnalog) {
          analogSink.next(Tuples.of(acei, new TestReceiverOffset(offsetRef.getAndIncrement())));
        }
      }
    }
  }

  private static void waitFor(long msec) throws InterruptedException {
    Object ob = new Object();
    synchronized (ob) {
      ob.wait(msec);
    }
  }

  private static class TestReceiverOffset implements ReceiverOffset {

    private static final TopicPartition STATIC_TOPIC_PARTITION =
        new TopicPartition("test", 0);

    private final long offset;

    TestReceiverOffset(long offset) {
      this.offset = offset;
    }

    @Override
    public TopicPartition topicPartition() {
      return STATIC_TOPIC_PARTITION;
    }

    @Override
    public long offset() {
      return offset;
    }

    @Override
    public void acknowledge() {
      // noop
    }

    @Override
    public Mono<Void> commit() {
      return Mono.empty();
    }
  }
}