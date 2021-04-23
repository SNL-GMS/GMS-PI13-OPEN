package gms.dataacquisition.stationreceiver.cd11.dataman;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Files;
import gms.core.dataacquisition.receiver.DataFrameReceiverConfiguration;
import gms.dataacquisition.stationreceiver.cd11.common.Cd11FrameFactory;
import gms.dataacquisition.stationreceiver.cd11.common.Cd11GapList;
import gms.dataacquisition.stationreceiver.cd11.common.FrameUtilities;
import gms.dataacquisition.stationreceiver.cd11.common.Gap;
import gms.dataacquisition.stationreceiver.cd11.common.GapList;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11AcknackFrame;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11AlertFrame;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11ByteFrame;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11CommandResponseFrame;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11DataFrame;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11Frame;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11Frame.FrameType;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11FrameHeader;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11FrameTrailer;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11OptionRequestFrame;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11OptionResponseFrame;
import gms.dataacquisition.stationreceiver.cd11.common.frames.CustomResetFrame;
import gms.shared.frameworks.osd.coi.datatransferobjects.CoiObjectMapperFactory;
import gms.shared.frameworks.osd.coi.waveforms.RawStationDataFrame;
import gms.shared.utilities.kafka.KafkaConfiguration;
import mockit.MockUp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.NettyInbound;
import reactor.netty.NettyOutbound;
import reactor.test.StepVerifier;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class Cd11StationConnectionHandlerTests {

  private static final Logger logger = LoggerFactory
      .getLogger(Cd11StationConnectionHandlerTests.class);

  @Mock
  private static DataFrameReceiverConfiguration mockDfrConfig = mock(
      DataFrameReceiverConfiguration.class);

  @Mock
  private static NettyInbound mockInbound;

  @Mock
  private static NettyOutbound mockOutbound;

  private static final String RESOURCE_PATH_PREFIX =
      "gms/shared/frameworks/processing/configuration/service/configuration-base/";
  private static final String RSDF_RESOURCE = "LBTB-RSDF.json";
  private static final String RSDF_STATION_NAME = "LBTB";
  private static final String RSDF_OUTPUT_TOPIC = "soh.rsdf";
  private static final String MALFORMED_FRAME_TOPIC = "malformed.frame";

  private static final Integer DATA_CONSUMER_PORT = 8100;

  private static final String rsdfResourceFile = RESOURCE_PATH_PREFIX + RSDF_RESOURCE;

  // Maps Json objects to corresponding GMS objects
  private static final ObjectMapper objectMapper = CoiObjectMapperFactory.getJsonObjectMapper();

  private static final KafkaConfiguration reactorConfig = buildDefaultTestConfiguration();

  private static final Cd11FrameFactory frameFactory = Cd11FrameFactory.builderWithDefaults()
      .build();


  private static Cd11StationConnectionHandler handler;

  @BeforeEach
  void setUp() {
    handler = Cd11StationConnectionHandler
        .create(RSDF_STATION_NAME, mockDfrConfig, reactorConfig, new Cd11GapList());
  }

  @Test
  void testHandleAcknackFrame() {
    String frameSetAcked = "STA12345678901234567";
    long seqNum = 1L;
    long[] gaps = new long[]{};

    Cd11GapList listToReset = new Cd11GapList(new GapList(seqNum + 1, seqNum + 1));

    assertNotEquals(new Cd11GapList().getGapList(), listToReset.getGapList());

    Cd11AcknackFrame acknackFrame = frameFactory
        .createCd11AcknackFrame(frameSetAcked, seqNum, seqNum, gaps);

    Flux<Cd11AcknackFrame> acknackHandler = handler
        .handleAcknackFrames(Flux.just(acknackFrame), listToReset);

    StepVerifier
        .create(acknackHandler)
        .expectNext(acknackFrame)
        .expectComplete()
        .verify();

    assertEquals(new Cd11GapList().getGapList(), listToReset.getGapList());
  }

  @Test
  void testHandleBadAcknackFrame() {
    String frameSetAcked = "STA12345678901234567";
    long seqNum = 1L;
    long[] gaps = new long[]{};

    GapList gapList = new GapList(seqNum + 1, seqNum + 1);
    Cd11GapList listToIgnore = new Cd11GapList(gapList);

    Cd11AcknackFrame badFrame = frameFactory
        .createCd11AcknackFrame(frameSetAcked, seqNum, seqNum, gaps);

    Flux<Cd11AcknackFrame> acknackHandler = handler
        .handleAcknackFrames(Flux.just(badFrame), new Cd11GapList());

    //We shouldn't error, but this frame shouldn't reset the gaps list,
    // since it will fail validation
    StepVerifier
        .create(acknackHandler)
        .expectNext(badFrame)
        .expectComplete()
        .verify();

    assertNotEquals(new Cd11GapList().getGapList(), listToIgnore.getGapList());

    Cd11GapList expected = new Cd11GapList(gapList);
    assertEquals(expected.getGapList(), listToIgnore.getGapList());
  }

  @Test
  void testHandleAlertFrame() {
    Cd11AlertFrame alertFrame = frameFactory.createCd11AlertFrame("TEST ALERT.");

    given(mockInbound.withConnection(any())).willReturn(mockInbound);
    given(mockOutbound.withConnection(any())).willReturn(mockOutbound);

    given(mockOutbound.sendByteArray(any())).willReturn(mockOutbound);
    given(mockOutbound.then()).willReturn(Mono.empty());

    Flux<Cd11Frame> alertHandler = Flux.just(alertFrame)
        .log()
        .map(frame -> handler.checkForHaltingFrame(mockInbound, mockOutbound, frame));

    StepVerifier
        .create(alertHandler)
        .expectNext(alertFrame)
        .expectComplete()
        .verify();

    assertTrue(handler.disposableComposite.isDisposed());
  }

  @Test
  void testHandleCommandResponseFrame() {
    long lowSeqNum = 1L;
    long highSeqNum = 3L;

    Cd11GapList listToFill = new Cd11GapList(new GapList(lowSeqNum, highSeqNum));

    assertEquals(1, listToFill.getGapList().getGapsList().size());

    Cd11CommandResponseFrame commandResponseFrame = new Cd11CommandResponseFrame("TEST", "TST",
        "SHZ", "00", Instant.now(), "TEST COMMAND REQUEST", "TEST COMMAND RESPONSE");
    commandResponseFrame
        .setFrameHeader(new Cd11FrameHeader(FrameType.COMMAND_RESPONSE, 1, "TEST", "TEST", 2L));
    commandResponseFrame.setFrameTrailer(new Cd11FrameTrailer(0, 3, new byte[]{}, 0L));

    Flux<Cd11CommandResponseFrame> commandResponseHandler = handler
        .handleCommandResponseFrames(Flux.just(commandResponseFrame), listToFill);

    StepVerifier
        .create(commandResponseHandler)
        .expectNext(commandResponseFrame)
        .expectComplete()
        .verify();

    List<Gap> gapList = List.copyOf(listToFill.getGapList().getGapsList());

    assertEquals(2, gapList.size());
    assertEquals(lowSeqNum, gapList.get(0).getStart());
    assertEquals(lowSeqNum, gapList.get(0).getEnd());
    assertEquals(highSeqNum, gapList.get(1).getStart());
    assertEquals(highSeqNum, gapList.get(1).getEnd());
  }

  @Test
  void testHandleDataFrame() throws IOException {

    RawStationDataFrame rsdfTest = getRawStationDataFrame();

    Cd11Frame cd11Frame;
    try (ByteArrayInputStream input = new ByteArrayInputStream(rsdfTest.getRawPayload())) {
      DataInputStream rawPayloadInputStream;
      rawPayloadInputStream = new DataInputStream(input);
      //TODO: we should migrate away from building Cd11ByteFrames, as they are to be phased out
      Cd11ByteFrame bf = new Cd11ByteFrame(rawPayloadInputStream, () -> true);
      cd11Frame = frameFactory.createCd11Frame(bf);
    }

    // create the flux of cd11 frames
    int expectedMsgs = 1;
    Cd11DataFrame dataFrame = cd11Frame.asFrameType(Cd11DataFrame.class);
    var flux = handler.handleDataFrames(Flux.just(dataFrame), new Cd11GapList())
        .filter(record -> reactorConfig.getInputRsdfTopic().equals(record.topic()));

    // Verify the expected number of RSDF objects
    StepVerifier.create(flux)
        .expectNextCount(expectedMsgs)
        .verifyComplete();

    // Verify that the RSDF objects are for the same station
    StepVerifier.create(flux)
        .consumeNextWith(record -> {
          RawStationDataFrame rsdf = null;
          try {
            rsdf = objectMapper.readValue(record.value(), RawStationDataFrame.class);
          } catch (JsonProcessingException e) {
            logger.error(e.getMessage());
          }
          assert rsdf != null;
          assertEquals(rsdf.getMetadata().getStationName(),
              rsdfTest.getMetadata().getStationName());
        })
        .verifyComplete();
  }

  private RawStationDataFrame getRawStationDataFrame() throws IOException {
    String stringUrl = Thread.currentThread().getContextClassLoader().getResource(rsdfResourceFile)
        .getPath();
    File jsonFile = new File(stringUrl);

    String fileContents = new String(Files.asByteSource(jsonFile).read());

    return objectMapper.readValue(fileContents, RawStationDataFrame.class);
  }

  @Test
  void testHandleOptionRequestFrame() {
    String request = "TEST";
    Cd11OptionRequestFrame optionRequestFrame = frameFactory
        .createCd11OptionRequestFrame(1, FrameUtilities.padToLength(
            request, FrameUtilities.calculatePaddedLength(request.length(), 4)));

    given(mockOutbound.sendByteArray(any())).willReturn(mockOutbound);
    given(mockOutbound.then()).willReturn(Mono.empty());

    Flux<Cd11OptionResponseFrame> optionRequestHandler = handler
        .handleOptionRequestFrames(Flux.just(optionRequestFrame), mockOutbound);

    Cd11OptionResponseFrame expected = frameFactory.createCd11OptionResponseFrame(
        optionRequestFrame.optionType, optionRequestFrame.optionRequest);

    StepVerifier
        .create(optionRequestHandler.log())
        .consumeNextWith(frame -> {
          assertEquals(expected.optionType, frame.optionType);
          assertEquals(expected.optionSize, frame.optionSize);
          assertEquals(expected.optionResponse, frame.optionResponse);
        }).verifyComplete();
  }

  @Test
  void testHandleCustomResetFrame() {

    new MockUp<Cd11GapListUtility>() {
      @mockit.Mock
      public void clearGapState(String stationName) {
      }
    };

    given(mockInbound.withConnection(any())).willReturn(mockInbound);
    given(mockOutbound.withConnection(any())).willReturn(mockOutbound);

    given(mockOutbound.sendByteArray(any())).willReturn(mockOutbound);
    given(mockOutbound.then()).willReturn(Mono.empty());

    CustomResetFrame resetFrame = new CustomResetFrame(new byte[]{});

    Flux<Cd11Frame> customResetFrameHandler = Flux.just(resetFrame)
        .map(frame -> handler.checkForHaltingFrame(mockInbound, mockOutbound, frame));

    StepVerifier
        .create(customResetFrameHandler)
        .expectNext(resetFrame)
        .expectComplete()
        .verify();
  }

  @Test
  void testHandleCustomResetFrameErrorOnGapStateClearFailure() {

    IOException expectedException = new IOException("BOOM");
    new MockUp<Cd11GapListUtility>() {
      @mockit.Mock
      public void clearGapState(String stationName) throws IOException {
        throw expectedException;
      }
    };

    CustomResetFrame resetFrame = new CustomResetFrame(new byte[]{});

    Flux<Cd11Frame> customResetFrameHandler = Flux.just(resetFrame)
        .map(frame -> handler.checkForHaltingFrame(mockInbound, mockOutbound, frame));

    StepVerifier
        .create(customResetFrameHandler)
        .expectErrorSatisfies(e -> assertEquals(expectedException, e))
        .verify();
  }

  /**
   * Build default test Kafka Reactor Configuration
   */
  private static KafkaConfiguration buildDefaultTestConfiguration() {
    return KafkaConfiguration.builder()
        .setApplicationId("cd11-data-consumer")
        .setInputRsdfTopic(RSDF_OUTPUT_TOPIC)
        .setMalformedFrameTopic(MALFORMED_FRAME_TOPIC)
        .setBootstrapServers("kafka1:9092,kafka2:9092,kafka3:9092")
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
}