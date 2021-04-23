package gms.integration.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Files;
import gms.dataacquisition.stationreceiver.cd11.common.Cd11FrameFactory;
import gms.dataacquisition.stationreceiver.cd11.common.FrameParsingUtility;
import gms.dataacquisition.stationreceiver.cd11.common.FrameUtilities;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11AcknackFrame;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11AlertFrame;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11ByteFrame;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11CommandResponseFrame;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11DataFrame;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11Frame;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11Frame.FrameType;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11OptionRequestFrame;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11OptionResponseFrame;
import gms.dataacquisition.stationreceiver.cd11.common.frames.CustomResetFrame;
import gms.dataacquisition.stationreceiver.cd11.common.frames.PartialFrame;
import gms.integration.util.StepUtils;
import gms.shared.frameworks.osd.coi.datatransferobjects.CoiObjectMapperFactory;
import gms.shared.frameworks.osd.coi.waveforms.RawStationDataFrame;
import gms.shared.frameworks.test.utils.services.GmsServiceType;
import io.cucumber.core.internal.gherkin.deps.com.google.gson.JsonElement;
import io.cucumber.java.After;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * DataMan Gerkin test steps using supplied rsdf files from a local directory in json format
 */
@Testcontainers
public class DatamanComponentTestSteps {

  private static final Logger logger = LoggerFactory.getLogger(DatamanComponentTestSteps.class);

  private static final String RESOURCE_PATH_PREFIX =
      "gms/integration/requests/dataacquisition/dataman/";

  // JSON Resources for comparing kafka messages
  private static final String KAFKA_RSDF_RESOURCE = "LBTB-KAFKA-RSDF.json";

  // Kafka topics for RSDF Kafka topic
  private static final String RSDF_TOPIC = "soh.rsdf";

  private static final String CD11_DATA_FRAME_CLASS_KEY = "Cd11DataFrame";
  private static final String RSDF_CLASS_KEY = "RawStationDataFrame";

  // Class map for separating the rsdf, acei and extract objects
  private static final Map<String, Class<?>> CLASS_MAP = new HashMap<>();

  // Maps Json objects to corresponding GMS objects
  private static final ObjectMapper objectMapper = CoiObjectMapperFactory.getJsonObjectMapper();

  static {
    CLASS_MAP.put(CD11_DATA_FRAME_CLASS_KEY, Cd11DataFrame.class);
    CLASS_MAP.put(RSDF_CLASS_KEY, RawStationDataFrame.class);
  }

  // Resource files for the expected ACEI and Soh Extract objects
  String kafkaRsdfResourceFile = RESOURCE_PATH_PREFIX + KAFKA_RSDF_RESOURCE;
  private Map<Class<?>, List<?>> listsParsedFromResources = new HashMap<>();
  private Cd11DataFrame cd11DataFrame = null;
  private final int MAXSOCKETWAITTIME = 2000;

  // compose environment
  private static Environment environment = null;
  private Cd11FrameFactory cd11FrameFactory;

  private SocketChannel socketClient = null;
  boolean connected = false;
  boolean disconnected = false;
  boolean reconnected = false;
  boolean dataFramesSent = false;
  final AtomicReference<byte[]> inboundByteArray = new AtomicReference<>();
  private InetSocketAddress datamanSocketAddress;

  private Cd11OptionRequestFrame cd11OptionRequestFrame;

  public DatamanComponentTestSteps(Environment environment) {
    if (this.environment == null) {
      this.environment = environment;
    }
  }

  @Given("a Cd11FrameFactory for station {string}")
  public void aCdFrameFactoryForStation(String stationName) {
    cd11FrameFactory = Cd11FrameFactory.builderWithDefaults()
        .setResponderName(stationName)
        .build();
  }

  @Given("an unused dataman connection")
  public void aUnusedConnection() throws UnknownHostException {
    datamanSocketAddress = getDatamanInetSocketAddress();
    socketClient = null;
    connected = false;
    disconnected = false;
    reconnected = false;
    dataFramesSent = false;
    inboundByteArray.set(null);
  }

  @Given("an input {string} frame resource file contains JSON versions of {string} objects")
  public void anInputResourceContainsJsonVersionsOf(String resourceName, String className)
      throws IOException, URISyntaxException {
    Class<?> clazz = CLASS_MAP.get(className);
    assertNotNull(clazz);
    URL url = StepUtils.class.getClassLoader().getResource(RESOURCE_PATH_PREFIX + resourceName);
    File jsonFile = Paths.get(url.toURI()).toFile();

    //String fileContents = Files.asByteSource(jsonFile).toString();
    String fileContents = new String(Files.asByteSource(jsonFile).read());

    Optional<?> rsdfOpt = Optional.of(objectMapper.readValue(fileContents, clazz));
    assertTrue(rsdfOpt.isPresent());
    RawStationDataFrame rsdf = (RawStationDataFrame) rsdfOpt.get();
    assertNotNull(rsdf);

    logger.info(">>>> Read instance of {} from input resource {}", className, resourceName);
    logger.info("RSDF: " + rsdf.toString());

    listsParsedFromResources.put(clazz, Arrays.asList(rsdf));
  }

  @Given("the {string} object is converted to a {string} object")
  public void theObjectIsConvertedToACd11DataFrame(String rsdfClass, String cd11Frame)
      throws IOException {
    Class<?> clazz = CLASS_MAP.get(rsdfClass);
    List<?> rsdfObjects = listsParsedFromResources.get(clazz);

    // Get the single RSDF from the list
    RawStationDataFrame rsdf = (RawStationDataFrame) rsdfObjects.get(0);

    assertNotNull(rsdf);

    // Convert the RSDF to a Cd11DataFrame
    Cd11ByteFrame byteFrame = new Cd11ByteFrame(
        new DataInputStream(new ByteArrayInputStream(rsdf.getRawPayload())), () -> true);
    cd11DataFrame = new Cd11DataFrame(byteFrame);

    assertNotNull(cd11DataFrame);
  }

  @Given("the dataman socket is connected and sends a list of channel subframes")
  public void theCd11SocketIsConnectedAndSendsSubframes()
      throws IOException {

    final Cd11DataFrame cd11DataFrame = cd11FrameFactory
        .createCd11DataFrame(this.cd11DataFrame.channelSubframes, 1);

    sendDataToDataman(cd11DataFrame.toBytes());

    logger.info("Verifying CD11 Data Frame sent to Dataman...");
    verifyDataSent();
    logger.info("CD11 Data Frame sent to Dataman!");

    logger.info(String
        .format("Cd11 Socket Data Frames (len = %d):", this.cd11DataFrame.channelSubframes.length));

    Arrays.stream(this.cd11DataFrame.channelSubframes).forEach(msg -> logger.info(msg.toString()));
  }

  @Then("within a period of {int} seconds expected {string} message is readable from the kafka topic {string}")
  public void withinGivenPeriodMessagesAreReadableFromTheKafkaTopic(int timeoutSeconds,
      String rsdfClass,
      String topicName) throws Exception {

    logger.info("Kafka Receive RSDFs:");
    final Class<?> clazzAcei = CLASS_MAP.get(rsdfClass);
    assertNotNull(clazzAcei);

    // Check the port exists and is open for Kafka
    Optional<Integer> portOpt = Optional
        .of(this.environment.deploymentCtxt().getServicePort(GmsServiceType.KAFKA_ONE));
    assertTrue(portOpt.isPresent());

    List<String> kafkaMessages = this.environment.deploymentCtxt()
        .receiveKafkaMessages(topicName, 1, timeoutSeconds*1000);
    logger.info("Kafka messages (size = " + kafkaMessages.size() + "):");
    logger.info(kafkaMessages.toString());

    // Create the Rsdf object from json resource file
    JsonElement rsdfEelement = StepUtils.parseJsonResource(kafkaRsdfResourceFile);
    RawStationDataFrame expectedRsdf = createRsdfObject(rsdfEelement);

    // Compare the rsdf objects
    boolean rsdfmessagePassed = compareRsdfObjects(kafkaMessages, expectedRsdf);

    assertTrue(rsdfmessagePassed, "Failed to receive the expected Rsdf messages");
  }

  @When("the dataman socket is connected and sends a ACKNACK request for frame set {string}")
  public void theCd11SocketIsConnectedAndSendsAckNack(String frameSetAcked)
      throws IOException {
    final var lowestSeqNum = 1L;
    final var highestSeqNum = 3L;
    final var gaps = new long[]{lowestSeqNum, highestSeqNum};
    final Cd11AcknackFrame cd11Frame = cd11FrameFactory
        .createCd11AcknackFrame(frameSetAcked, lowestSeqNum, highestSeqNum, gaps);

    sendDataToDataman(cd11Frame.toBytes());
  }

  @Then("an ACKNACK response is received for frame set {string}")
  public void anAcknackResponseIsReceivedForFrameSet(String frameSetAcked) throws IOException {
    verifyDataSent();

    receiveDataFromDataman(inboundByteArray);
    logger.info("Verifying ACKNACK response received from Dataman...");
    final var cd11AcknackFrame = parseAcknackFrame(inboundByteArray.get());
    assertTrue(cd11AcknackFrame.isPresent(), "ACKNACK response was not received from Dataman!");
    final var receivedFrame = cd11AcknackFrame.get();
    assertEquals(FrameType.ACKNACK, receivedFrame.frameType);
    assertEquals(frameSetAcked, receivedFrame.framesetAcked);
    assertTrue(receivedFrame.gapRanges.length < 1);
    logger.info("Verified ACKNACK response received from Dataman!");
  }

  @When("the dataman socket is connected and sends a Alert request")
  public void theCd11SocketIsConnectedAndSendsAlert()
      throws IOException {
    final var message = "Test Alert Message \t\r\n1234567890-=`~!@$%^&*()_+[]{}\\|;':\",./<>?";
    final Cd11AlertFrame cd11Frame = cd11FrameFactory.createCd11AlertFrame(message);

    sendDataToDataman(cd11Frame.toBytes());
  }

  @And("the dataman socket was disconnected")
  public void theDatamanSocketWasDisconnected() throws IOException {
    connectSocketClient(true);
    assertTrue(disconnected);
    assertTrue(reconnected);
  }

  @Then("the dataman socket was sent a {string} message")
  public void messageWasSentToDataMan(String messageType) {
    logger.info("Verifying {} sent to Dataman...", messageType);
    verifyDataSent();
    logger.info("{} sent to Dataman!", messageType);
  }

  @When("an ACKNACK frame for frame set {string} is sent to dataman after sending a MalformedFrame")
  public void anACKNACKFrameCanBeSentAndRetrievedFromDatamanAfterSendingAMalformedFrame(
      String frameSetAcked)
      throws IOException {
    final var seqNum = 1L;
    final var gaps = new long[]{1, 3};
    var cd11Frame = cd11FrameFactory.createCd11AcknackFrame(frameSetAcked, seqNum, seqNum, gaps);

    final var malformedData = "This should be fun...".getBytes();
    final var partialFrame = new PartialFrame(cd11Frame.getFrameHeader(),
        cd11Frame.getFrameTrailer(),
        cd11Frame.getFrameBodyBytes(), new RuntimeException("kersplode"), true,
        malformedData);
    final var unsupportedFrame = partialFrame;
    final var outboundData = Arrays.asList(unsupportedFrame.toBytes(), cd11Frame.toBytes());

    sendDataToDataman(outboundData);
  }

  @When("an ACKNACK frame for frame set {string} is sent to dataman after sending malformed data")
  public void anACKNACKFrameCanBeSentAndRetrievedFromDatamanAfterSendingMalformedData(
      String frameSetAcked)
      throws IOException {
    final var seqNum = 1L;
    final var gaps = new long[]{1, 3};

    final Cd11AcknackFrame cd11Frame = cd11FrameFactory
        .createCd11AcknackFrame(frameSetAcked, seqNum, seqNum, gaps);
    final byte[] malformedData = "This should be fun...".getBytes();
    System.out.println("MALFORMED LENGTH: "+malformedData.length);
    System.out.println();
    final var outboundData = Arrays.asList(malformedData);

    sendDataToDataman(outboundData);

    try{
      Thread.sleep(100);
    }catch (InterruptedException e){ }

    sendDataToDataman(Arrays.asList(cd11Frame.toBytes()));
  }

  @When("a CommandResponseFrame with sequence of {int} is sent to Dataman for station {string}")
  public void aCommandResponseFrameWithSequenceOfToUpdateTheGapListForStation(int sequence,
      String stationName) throws IOException {
    final Cd11CommandResponseFrame cd11Frame = cd11FrameFactory
        .createCd11CommandResponseFrame(stationName, "LBTB1", "SHZ", "ss", Instant.now(), "do it!",
            "did it!", sequence);

    sendDataToDataman(cd11Frame.toBytes());
  }

  @Then("a gap list between {int} and {int} is reported from Dataman")
  public void aGapListIsReportedFromDataman(int sequence, int nextSequence) throws IOException {
    receiveDataFromDataman(inboundByteArray);

    verifyDataSent();
    final var cd11AcknackFrame = parseAcknackFrame(inboundByteArray.get());
    assertTrue(cd11AcknackFrame.isPresent(), "ACKNACK response was not received from Dataman!");
    logger.info("Verifying ACKNACK response received from Dataman...");
    final var receivedFrame = cd11AcknackFrame.get();
    assertEquals(FrameType.ACKNACK, receivedFrame.frameType);
    assertEquals(sequence, receivedFrame.lowestSeqNum);
    assertEquals(nextSequence, receivedFrame.highestSeqNum);
    assertEquals(1, receivedFrame.gapCount);
    final long[] gaps = new long[]{sequence + 1, nextSequence};
    assertEquals(gaps.length, receivedFrame.gapRanges.length);
    IntStream.range(0, gaps.length)
        .forEach(i -> assertEquals(gaps[i], receivedFrame.gapRanges[i]));
    logger.info("Verified ACKNACK response received from Dataman!");
  }

  @When("sending an ACKNAK for frame set {string} with highestSeqNum of {int} and no gaps")
  public void sendingAnACKNAKWithHighestSeqNumOfAndNoGapsWillResetDatamansGapList(
      String frameSetAcked, int highestSeqNum)
      throws IOException {
    final var seqNum = 1L;
    final Cd11AcknackFrame acknackFrame = cd11FrameFactory
        .createCd11AcknackFrame(frameSetAcked, seqNum, highestSeqNum, new long[]{});
    connected = false;
    dataFramesSent = false;

    sendDataToDataman(acknackFrame.toBytes());
  }

  @When("a CustomResetFrame is sent")
  public void sendingAnCustomResetFrame() throws IOException {
    CustomResetFrame resetFrame = cd11FrameFactory.createCustomResetFrame();
    connected = false;
    dataFramesSent = false;

    sendDataToDataman(resetFrame.toBytes());
  }

  @Then("dataman's gap list is reset for frame set {string}")
  public void datamansGapListIsReset(String frameSetAcked) throws IOException {
    verifyDataSent();

    receiveDataFromDataman(inboundByteArray);
    final var cd11AcknackFrame = parseAcknackFrame(inboundByteArray.get());
    assertTrue(cd11AcknackFrame.isPresent(), "ACKNACK response was not received from Dataman!");
    logger.info("Verifying ACKNACK response received from Dataman...");
    final var receivedFrame = cd11AcknackFrame.get();
    assertEquals(FrameType.ACKNACK, receivedFrame.frameType);
    assertEquals(frameSetAcked, receivedFrame.framesetAcked);
    assertEquals(0, receivedFrame.lowestSeqNum);
    assertEquals(-1, receivedFrame.highestSeqNum);
    assertEquals(0, receivedFrame.gapCount);
    assertEquals(0, receivedFrame.gapRanges.length);
    logger.info("Verified ACKNACK response received from Dataman!");
  }

  @When("the dataman socket is connected and sends an Option request")
  public void theCd11SocketIsConnectedAndSendsOptionRequest()
      throws IOException {
    String optionRequest = "No Mayo";
    cd11OptionRequestFrame = cd11FrameFactory.createCd11OptionRequestFrame(1,
        FrameUtilities.padToLength(optionRequest,
            FrameUtilities.calculatePaddedLength(optionRequest.length(), 4)));

    sendDataToDataman(cd11OptionRequestFrame.toBytes());
  }

  @Then("an OPTION response is received from dataman")
  public void anOPTIONResponseIsReceivedFromDataman() throws IOException {
    receiveDataFromDataman(inboundByteArray);

    Optional<Cd11OptionResponseFrame> responseFrame = readOptionResponseFrame(
        inboundByteArray.get());

    assertEquals(cd11OptionRequestFrame.optionType, responseFrame.get().optionType);
    assertEquals(cd11OptionRequestFrame.optionRequest, responseFrame.get().optionResponse);
  }

  private Optional<Cd11OptionResponseFrame> readOptionResponseFrame(byte[] frameByteArray) {

    Cd11ByteFrame byteFrame;
    try {
      byteFrame = new Cd11ByteFrame(
          new DataInputStream(new ByteArrayInputStream(frameByteArray)), () -> true);
      logger.info("Byte frame type: {}", byteFrame.getFrameType());
    } catch (IOException e) {
      logger.error("Failed to parse Alert Frame", e);
      byteFrame = null;
    }
    if (byteFrame != null && byteFrame.getFrameType() == FrameType.OPTION_RESPONSE) {
      final var frame = new Cd11OptionResponseFrame(byteFrame);
      return Optional.of(frame);
    }
    return Optional.empty();
  }


  @When("the dataman socket is connected and sends a {string}")
  public void theDatamanSocketIsConnectedAndSendsAFrame_type(String frameType)
      throws IOException {
    Cd11Frame frame = getNoOpCd11Frame(frameType);
    connected = false;
    dataFramesSent = false;

    sendDataToDataman(frame.toBytes());
  }

  private Cd11Frame getNoOpCd11Frame(String frameType) {
    Cd11Frame frame;
    switch (frameType) {
      case "CD ONE ENCAPSULATION":
        return cd11FrameFactory.createCd1EncapsulationFrame();
      case "COMMAND REQUEST":
        frame = cd11FrameFactory
            .createCd11CommandRequestFrame("LBTB", "LBTB1", "SHZ", "ss", Instant.now(), "do it!");
        break;
      case "CONNECTION REQUEST":
        frame = cd11FrameFactory.createCd11ConnectionRequestFrame(datamanSocketAddress.getAddress(),
            datamanSocketAddress.getPort());
        break;
      case "CONNECTION RESPONSE":
        frame = cd11FrameFactory
            .createCd11ConnectionResponseFrame(datamanSocketAddress.getAddress(),
                datamanSocketAddress.getPort(), datamanSocketAddress.getAddress(),
                datamanSocketAddress.getPort());
        break;
      case "OPTION RESPONSE":
        frame = cd11FrameFactory.createCd11OptionResponseFrame(1, "12345678");
        break;
      default:
        throw new IllegalArgumentException("frame type not recognized");
    }
    return frame;
  }

  // Compare the Rsdf objects from the kafka queue and the expected message
  private boolean compareRsdfObjects(List<String> kafkaMessages, RawStationDataFrame expectedRsdf) {

    // Create the JsonElement list from the kafka messages
    List<JsonElement> jsonElementList = StepUtils.createJsonElementList(kafkaMessages);

    // Ensure that JsonElement list only has one object
    if (jsonElementList.size() != 1) {
      return false;
    }

    // Create Kafka SOH Extract object from kafka messages
    JsonElement jsonRsdf = jsonElementList.get(0);
    RawStationDataFrame kafkaRsdf = createRsdfObject(jsonRsdf);

    // Compare the expected and kafka rsdf objects

    return expectedRsdf.hasSameStateAndRawPayload(kafkaRsdf);
  }

  // Create the Rsdf object from json element
  private RawStationDataFrame createRsdfObject(JsonElement jsonElement) {

    String rsdfString = jsonElement.getAsJsonObject().toString();

    Optional<RawStationDataFrame> rsdfOpt;
    try {
      rsdfOpt = Optional.of(
          objectMapper.readValue(rsdfString, RawStationDataFrame.class));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    return rsdfOpt.get();
  }

  private Optional<Cd11AcknackFrame> parseAcknackFrame(byte[] frameByteArray) {
    logger.info("Parsing ACKNACK response received from Dataman...");
    Cd11ByteFrame byteFrame = null;
    try {
      byteFrame = new Cd11ByteFrame(
          new DataInputStream(new ByteArrayInputStream(frameByteArray)), () -> true);
      logger.info("Byte frame type: {}", byteFrame.getFrameType());
    } catch (IOException e) {
      logger.warn("Failed to parse ACKNACK Frame", e);
    }
    if (byteFrame != null && byteFrame.getFrameType() == FrameType.ACKNACK) {
      final var frame = new Cd11AcknackFrame(byteFrame);
      logger.info("ACKNACK response received from Dataman: {}", frame.toString());
      return Optional.of(frame);
    }
    return Optional.empty();
  }

  private Optional<Cd11AlertFrame> parseAlertFrame(byte[] frameByteArray) {
    logger.info("Parsing Alert response received from Dataman...");
    Cd11ByteFrame byteFrame;
    try {
      byteFrame = new Cd11ByteFrame(
          new DataInputStream(new ByteArrayInputStream(frameByteArray)), () -> true);
      logger.info("Byte frame type: {}", byteFrame.getFrameType());
    } catch (IOException e) {
      logger.error("Failed to parse Alert Frame", e);
      byteFrame = null;
    }
    if (byteFrame != null && byteFrame.getFrameType() == FrameType.ALERT) {
      final var frame = new Cd11AlertFrame(byteFrame);
      logger.info("Alert response received from Dataman: {}", frame.toString());
      return Optional.of(frame);
    }
    return Optional.empty();
  }

  private void sendDataToDataman(byte[] outboundData) throws IOException {
    this.sendDataToDataman(Collections.singletonList(outboundData));
  }

  private void sendDataToDataman(List<? extends byte[]> outboundData)
      throws IOException {

    connectSocketClient(false);

    if (socketClient.isConnected() || socketClient.finishConnect()) {
      connected = true;
      // Send messages to server
      logger.info("NIO TCP CLIENT - OUTBOUND STARTED...");
      for (byte[] d : outboundData) {
        if (!socketClient.isConnected()) {
          logger.info("NIO TCP CLIENT - RECONNECTING...");
          socketClient.connect(datamanSocketAddress);
          logger.info("NIO TCP CLIENT - RECONNECTED");
        }
        final var buffer = ByteBuffer.wrap(d);
        socketClient.write(buffer);
        logger.info("NIO TCP CLIENT - SENT SOME DATA");
        buffer.clear();
      }
      dataFramesSent = true;
      logger.info("NIO TCP CLIENT - OUTBOUND COMPLETE");
    }
  }

  private void receiveDataFromDataman(AtomicReference<byte[]> inbound) throws IOException {
    if (inbound != null) {
      connectSocketClient(false);
      inbound.set(null);
      logger.info("NIO TCP CLIENT - INBOUND STARTED...");
      final var byteBuffer = ByteBuffer.allocate(10000);
      socketClient.read(byteBuffer);
      System.out.println("BYTE BUFFER "+byteBuffer.array());
      inbound.set(byteBuffer.array());
      logger.info("NIO TCP CLIENT - GOT SOME DATA");
      logger.info("NIO TCP CLIENT - INBOUND COMPLETE");
    }
  }

  private void connectSocketClient(boolean checkForDisconnect) throws IOException {
    if (socketClient != null && socketClient.isConnected()) {

      int read = 0;
      if (checkForDisconnect) {
        try {
          logger.warn("NIO TCP CLIENT - CHECKING CONNECTION STATUS...");
          Optional<Cd11AlertFrame> cd11AlertFrame = Optional.empty();
          while (cd11AlertFrame.isEmpty()) {
            inboundByteArray.set(null);
            receiveDataFromDataman(inboundByteArray);
            cd11AlertFrame = parseAlertFrame(inboundByteArray.get());
          }
          String alertMessage = cd11AlertFrame.get().message.toLowerCase();
          logger.info("Cd11 Alert frame message: {}", alertMessage);
          if (alertMessage.contains("shutting down connection")) {
            logger.warn("NIO TCP CLIENT - SHUTDOWN DETECTED...");
            read = -1;
            disconnectSocketClient();
            socketClient = null;
            // this sleep is needed to allow old dataman (with shaky event model) to complete its
            // shutdown and restart cycle
            Thread.sleep(2000);
          }
        } catch (IOException | InterruptedException e) {
          logger.warn("NIO TCP CLIENT - FAILED TO CHECK CONNECTION STATUS", e);
          read = -1;
        }
      }

      if (read >= 0) {
        logger.info("NIO TCP CLIENT - ALREADY CONNECTED");
      } else {
        logger.info("NIO TCP CLIENT - RECONNECTING...");
        connectSocketClient(false);
        reconnected = true;
        logger.info("NIO TCP CLIENT - RECONNECTED");
      }
    } else {
      logger.info("NIO TCP CLIENT - CONNECTING...");
      socketClient = SocketChannel.open(datamanSocketAddress);
      logger.info("NIO TCP CLIENT - CONNECTED");
    }
  }

  private InetSocketAddress getDatamanInetSocketAddress() throws UnknownHostException {
    final var deploymentCtxt = this.environment.deploymentCtxt();
    final var datamanAddress = InetAddress
        .getByName(deploymentCtxt.getServiceHost(GmsServiceType.DATAMAN));
    final var datamanPort = this.environment.deploymentCtxt()
        .getServicePort(GmsServiceType.DATAMAN);

    return new InetSocketAddress(datamanAddress, datamanPort);
  }

  @After("@dataman")
  public void disconnectSocketClient() throws IOException {
    logger.info("NIO TCP CLIENT - DISCONNECTING...");
    if (socketClient.isConnected()) {
      socketClient.close();
      logger.info("NIO TCP CLIENT - DISCONNECTED");
      disconnected = true;
    } else {
      logger.info("NIO TCP CLIENT - ALREADY DISCONNECTED");
    }
  }

  private void verifyDataSent() {
    logger.info("Verifying connection was established...");
    assertTrue(connected);
    logger.info("Verified connection was established!");
    logger.info("Verifying data frame was sent...");
    assertTrue(dataFramesSent);
    logger.info("Verified data frame was sent!");
  }
}