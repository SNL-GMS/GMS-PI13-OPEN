package gms.dataacquisition.cd11.rsdf.processor;

import static gms.dataacquisition.cd11.rsdf.util.MockUtility.mockChannel;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willReturn;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gms.core.dataacquisition.receiver.DataFrameReceiverConfiguration;
import gms.dataacquisition.cd11.rsdf.util.RsdfUtility;
import gms.shared.frameworks.osd.coi.channel.Channel;
import gms.shared.frameworks.osd.coi.datatransferobjects.CoiObjectMapperFactory;
import gms.shared.frameworks.osd.coi.waveforms.RawStationDataFrame;
import java.io.IOException;
import java.util.Optional;

import gms.shared.utilities.kafka.KafkaConfiguration;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.kafka.sender.SenderRecord;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class ReactorCd11RsdfProcessorTest {

  private static final Logger logger = LoggerFactory.getLogger(ReactorCd11RsdfProcessorTest.class);

  private static final ObjectMapper jsonObjectMapper = CoiObjectMapperFactory.getJsonObjectMapper();

  @Mock
  private DataFrameReceiverConfiguration mockReceiverConfiguration;

  private KafkaConfiguration kafkaConfiguration;

  private ReactorCd11RsdfProcessor processor;

  @Mock
  private ConsumerRecord<String, String> rsdfRecord;


  @BeforeEach
  void setUp() {
    kafkaConfiguration = buildDefaultTestConfiguration();
    processor = new ReactorCd11RsdfProcessor(kafkaConfiguration, mockReceiverConfiguration);
  }

  @Test
  void testRecordsProducesCorrectFlux() throws IOException {
    Channel mockLbtb1Z = mockChannel("LBTB.LBTB1.SHZ");
    Channel mockLbtbbZ = mockChannel("LBTB.LBTBB.BHZ");
    Channel mockLbtbbN = mockChannel("LBTB.LBTBB.BHN");
    Channel mockLbtbbE = mockChannel("LBTB.LBTBB.BHE");

    configureMockConfiguration(mockReceiverConfiguration, mockLbtb1Z, mockLbtbbZ, mockLbtbbN,
        mockLbtbbE);

    RawStationDataFrame inputRsdf = RsdfUtility.getRawStationDataFrame("LBTB-RSDF.json");
    given(rsdfRecord.value()).willReturn(jsonObjectMapper.writeValueAsString(inputRsdf));

    Flux<SenderRecord<String, String, String>> flux = processor.records(Flux.just(rsdfRecord)).cache();

    var stationSohFlux = flux.filter(
        record -> kafkaConfiguration.getOutputStationSohInputTopic().equals(record.topic()));

    var aceiFlux = flux.filter(
        record -> kafkaConfiguration.getOutputAcquiredChannelSohTopic().equals(record.topic()));

    StepVerifier.create(stationSohFlux)
        .expectNextCount(1)
        .verifyComplete();

    StepVerifier.create(aceiFlux)
        .expectNextCount(68)
        .verifyComplete();
  }

  @Disabled
  @Test
  void testRecordsInvalidJsonCompletesInError() {
    given(rsdfRecord.value()).willReturn("bad");

    Flux<SenderRecord<String, String, String>> flux = processor.records(Flux.just(rsdfRecord));

    StepVerifier.create(flux)
        .expectError(JsonProcessingException.class)
        .verify();
  }

  @Test
  void testRecordsInvalidRsdfCompletesInError() throws IOException {
    RawStationDataFrame inputRsdf = RsdfUtility.getRawStationDataFrame("LBTB-RSDF.json")
        .toBuilder().setRawPayload("bad".getBytes()).build();
    given(rsdfRecord.value()).willReturn(jsonObjectMapper.writeValueAsString(inputRsdf));

    Flux<SenderRecord<String, String, String>> flux = processor.records(Flux.just(rsdfRecord));
    StepVerifier.create(flux)
        .expectError(IOException.class)
        .verify();
  }

  /**
   * Build default test Kafka Reactor Configuration
   */
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
