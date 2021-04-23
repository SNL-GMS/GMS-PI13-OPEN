package gms.dataacquisition.cd11.rsdf.processor;

import static gms.dataacquisition.cd11.rsdf.util.MockUtility.mockChannel;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.willReturn;

import com.fasterxml.jackson.databind.ObjectMapper;
import gms.core.dataacquisition.receiver.DataFrameReceiverConfiguration;
import gms.dataacquisition.cd11.rsdf.util.RsdfUtility;
import gms.shared.frameworks.osd.coi.channel.Channel;
import gms.shared.frameworks.osd.coi.datatransferobjects.CoiObjectMapperFactory;
import gms.shared.frameworks.osd.coi.waveforms.RawStationDataFrame;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;
import java.util.stream.Stream;

import gms.shared.utilities.kafka.KafkaConfiguration;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.TopologyTestDriver;
import org.apache.kafka.streams.test.ConsumerRecordFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class KafkaStreamsCd11RsdfProcessorTest {

  private static final ObjectMapper jsonObjectMapper = CoiObjectMapperFactory.getJsonObjectMapper();

  @Mock
  private AdminClient mockAdminClient;

  @Mock
  private ListTopicsResult mockListTopicResult;

  @Mock
  private KafkaFuture<Set<String>> mockFutureNames;

  @Mock
  private DataFrameReceiverConfiguration mockReceiverConfiguration;

  private KafkaConfiguration kafkaConfiguration;

  private KafkaStreamsCd11RsdfProcessor processor;


  @BeforeEach
  void setUp() {
    kafkaConfiguration = buildDefaultTestConfiguration();
    processor = new KafkaStreamsCd11RsdfProcessor(kafkaConfiguration,
        mockReceiverConfiguration);
  }

  @Test
  void testKafkaStateValidation() throws ExecutionException, InterruptedException {
    willReturn(mockListTopicResult).given(mockAdminClient).listTopics();
    willReturn(mockFutureNames).given(mockListTopicResult).names();
    willReturn(Set.of("soh.rsdf", "soh.acei", "soh.extract")).given(mockFutureNames).get();
    assertDoesNotThrow(() -> processor.validateKafkaState(mockAdminClient, 1));
  }

  @Test
  void testKafkaStateValidationThrowsIllegalStateExecption()
      throws ExecutionException, InterruptedException {
    willReturn(mockListTopicResult).given(mockAdminClient).listTopics();
    willReturn(mockFutureNames).given(mockListTopicResult).names();
    willReturn(Set.of("")).given(mockFutureNames).get();
    assertThrows(IllegalStateException.class,
        () -> processor.validateKafkaState(mockAdminClient, 1));
  }

  @Test
  void testStreamProcessing() throws IOException {
    Channel mockLbtb1Z = mockChannel("LBTB.LBTB1.SHZ");
    Channel mockLbtbbZ = mockChannel("LBTB.LBTBB.BHZ");
    Channel mockLbtbbN = mockChannel("LBTB.LBTBB.BHN");
    Channel mockLbtbbE = mockChannel("LBTB.LBTBB.BHE");

    configureMockConfiguration(mockReceiverConfiguration, mockLbtb1Z, mockLbtbbZ, mockLbtbbN,
        mockLbtbbE);

    Topology topology = processor.buildKafkaTopology();
    Properties properties = processor.buildKafkaProperties();

    TopologyTestDriver testDriver = new TopologyTestDriver(topology, properties);
    RawStationDataFrame inputRsdf = RsdfUtility.getRawStationDataFrame("LBTB-RSDF.json");

    StringSerializer stringSerializer = new StringSerializer();
    ConsumerRecordFactory<String, String> factory = new ConsumerRecordFactory<>(
        kafkaConfiguration.getInputRsdfTopic(), stringSerializer, stringSerializer);
    testDriver.pipeInput(factory.create(jsonObjectMapper.writeValueAsString(inputRsdf)));

    StringDeserializer stringDeserializer = new StringDeserializer();
    List<ProducerRecord<String, String>> aceiRecords = Stream.generate(
        supplyProducerRecords(testDriver, kafkaConfiguration.getOutputAcquiredChannelSohTopic(),
            stringDeserializer))
        .takeWhile(Objects::nonNull)
        .collect(toList());

    assertEquals(68, aceiRecords.size());

    List<ProducerRecord<String, String>> stationSohRecords = Stream.generate(
        supplyProducerRecords(testDriver, kafkaConfiguration.getOutputStationSohInputTopic(),
            stringDeserializer))
        .takeWhile(Objects::nonNull)
        .collect(toList());

    assertEquals(1, stationSohRecords.size());

    testDriver.close();
  }

  @Test
  void testStreamProcessingInvalidRsdfSkipsProcessing() {
    Topology topology = processor.buildKafkaTopology();
    Properties properties = processor.buildKafkaProperties();

    TopologyTestDriver testDriver = new TopologyTestDriver(topology, properties);

    StringSerializer stringSerializer = new StringSerializer();
    ConsumerRecordFactory<String, String> factory = new ConsumerRecordFactory<>(
        kafkaConfiguration.getInputRsdfTopic(), stringSerializer, stringSerializer);
    testDriver.pipeInput(factory.create("bad"));

    StringDeserializer stringDeserializer = new StringDeserializer();
    List<ProducerRecord<String, String>> aceiRecords = Stream.generate(
        supplyProducerRecords(testDriver, kafkaConfiguration.getOutputAcquiredChannelSohTopic(),
            stringDeserializer))
        .takeWhile(Objects::nonNull)
        .collect(toList());

    assertEquals(0, aceiRecords.size());

    List<ProducerRecord<String, String>> stationSohRecords = Stream.generate(
        supplyProducerRecords(testDriver, kafkaConfiguration.getOutputStationSohInputTopic(),
            stringDeserializer))
        .takeWhile(Objects::nonNull)
        .collect(toList());

    assertEquals(0, stationSohRecords.size());

    testDriver.close();
  }

  private Supplier<ProducerRecord<String, String>> supplyProducerRecords(
      TopologyTestDriver testDriver, String topic, StringDeserializer deserializer) {
    return () -> testDriver.readOutput(topic, deserializer, deserializer);
  }

  private KafkaConfiguration buildDefaultTestConfiguration() {
    return KafkaConfiguration.builder()
        .setApplicationId("cd11-rsdf-processor")
        .setBootstrapServers("kafka1:9092,kafka2:9092,kafka3:9092")
        .setInputRsdfTopic("soh.rsdf")
        .setMalformedFrameTopic("malformed.frame")
        .setOutputAcquiredChannelSohTopic("soh.acei")
        .setOutputStationSohInputTopic("soh.extract")
        .setKeySerializer("org.apache.kafka.common.serialization.Serdes$StringSerde")
        .setValueSerializer("org.apache.kafka.common.serialization.Serdes$StringSerde")
        .setNumberOfVerificationAttempts(1)
        .setStreamsCloseTimeoutMs(60000)
        .setConnectionRetryCount(10)
        .setRetryBackoffMs(1000L)
        .setSessionTimeout(6000)
        .setMaxPollInterval(2500)
        .setMaxPollRecords(2000)
        .setAutoCommit(false)
        .setHeartbeatInterval(3000)
        .setTransactionTimeout(30000)
        .setAcks("all")
        .setDeliveryTimeout(2500)
        .build();
  }

  private static void configureMockConfiguration(DataFrameReceiverConfiguration configuration,
      Channel... mockChannels) {
    for (Channel mockChannel : mockChannels) {
      String channelName = mockChannel.getName();
      willReturn(Optional.of(channelName)).given(configuration)
          .getChannelName(channelName);
    }
  }

}

