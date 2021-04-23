package gms.integration.steps;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.api.model.Network;
import com.google.common.io.Files;
import gms.integration.util.StepUtils;
import gms.shared.frameworks.osd.api.util.ChannelTimeRangeRequest;
import gms.shared.frameworks.osd.api.util.StationTimeRangeRequest;
import gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssue;
import gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssueAnalog;
import gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssueBoolean;
import gms.shared.frameworks.osd.coi.datatransferobjects.CoiObjectMapperFactory;
import gms.shared.frameworks.osd.coi.soh.StationSoh;
import gms.shared.frameworks.osd.coi.waveforms.RawStationDataFrame;
import gms.shared.frameworks.test.utils.services.OsdTableType;
import io.cucumber.java.After;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.io.File;
import java.net.URL;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public class OsdSohPerformanceComponentSteps {

  private static final Logger LOGGER = LoggerFactory.getLogger(OsdSohPerformanceComponentSteps.class);

  private static final String RESOURCE_PATH_PREFIX =
          "gms/integration/data/";
  private static final String FILE_NAME = "fileName";
  private static final String REMOTE_PATH = "remotePath";
  private static final String FILE_CONTENT = "fileContent";

  private GenericContainer dataInjector;

  private Network deploymentNetwork;
  private Environment environment;
  private List<String> stationNames;

  // Requests for querying the OSD for RSDF, StationSoh, ACEI and Soh Extract
  private StationTimeRangeRequest stationTimeRangeRequest;
  private ChannelTimeRangeRequest aceiBooleanRequest;
  private ChannelTimeRangeRequest aceiAnalogRequest;

  // Maps Json objects to corresponding GMS objects
  private static final ObjectMapper objectMapper = CoiObjectMapperFactory.getJsonObjectMapper();

  public OsdSohPerformanceComponentSteps(Environment environment) {
    this.environment = environment;
    this.dataInjector = new GenericContainer(
        String
            .format("%s/gms-common/frameworks-data-injector:%s",
                System.getenv("CI_DOCKER_REGISTRY"),
                    System.getenv("DOCKER_IMAGE_TAG")));
  }

  @After("@osd-consumers")
  public void tearDown() {
    this.dataInjector.close();
  }

  @When("I send {int} batches of {int} messages of type {string} every {string} on the {string} topic using {string} to create messages to be stored in {string}")
  public void iSendBatchesOfMessagesOfSomeTypeOnSomeIntervalOnSomeTopic(
      int batchCount, int numMessages, String messageType, String interval, String topicName,
      String fileName, String tableType) throws Exception {

    // Create the time range request based on the station/channel name
    OsdTableType osdTableType = OsdTableType.valueOf(tableType);

    Map<String, String> injectorFileMap = createInjectorFileMap(fileName);
    injectorFileMap.forEach((k, v) -> LOGGER.info("{} : {}", k, v));

    switch(osdTableType) {
      case ACEI_ANALOG:

        // Create the channel time range request for the ACEI queries (boolean/analog)
        Optional<?> aceiAnalogOpt = Optional.of(objectMapper.readValue(injectorFileMap.get(FILE_CONTENT),
                AcquiredChannelEnvironmentIssueAnalog.class));

        aceiAnalogRequest = createACEIRequest((AcquiredChannelEnvironmentIssue) aceiAnalogOpt.get(), numMessages);

        LOGGER.info("ACEI Analog Request: {}", aceiAnalogRequest);
        break;

      case ACEI_BOOLEAN:

        // Create the channel time range request for the ACEI queries (boolean/analog)
        Optional<?> aceiBooleanOpt = Optional.of(objectMapper.readValue(injectorFileMap.get(FILE_CONTENT),
                AcquiredChannelEnvironmentIssueBoolean.class));

        aceiBooleanRequest = createACEIRequest((AcquiredChannelEnvironmentIssue) aceiBooleanOpt.get(), numMessages);

        LOGGER.info("ACEI Boolean Request: {}", aceiBooleanRequest);
        break;

      case RAW_STATION_DATA_FRAME:

        // RSDF Will have a single object to compare
        Optional<?> rsdfOpt = Optional.of(objectMapper.readValue(injectorFileMap.get(FILE_CONTENT),
                RawStationDataFrame.class));
        RawStationDataFrame rsdf = (RawStationDataFrame) rsdfOpt.get();
        String stationName = rsdf.getMetadata().getStationName();
        Instant startTime = rsdf.getMetadata().getPayloadStartTime();
        Instant endTime = rsdf.getMetadata().getPayloadEndTime();
        stationTimeRangeRequest = StationTimeRangeRequest.create(stationName, startTime, endTime);

        LOGGER.info("RSDF Station Time Range Request:");
        LOGGER.info(stationTimeRangeRequest.toString());

        break;
      case STATION_SOH:

        // StationSoh will have a single object
        stationNames = new ArrayList<>();
        Optional<?> sohOpt = Optional.of(objectMapper.readValue(injectorFileMap.get(FILE_CONTENT),
                StationSoh.class));
        StationSoh stationSoh = (StationSoh) sohOpt.get();
        stationNames.add(stationSoh.getStationName());

        LOGGER.info("StationSoh Station Names:");
        stationNames.forEach(LOGGER::info);

        break;

      default:
        break;
    }

    var pastKafkaTopicOffsets = new ArrayList<String>();

    List<String> newKafkaTopicOffsets = this.environment.deploymentCtxt().getTopicOffset(topicName);
    LOGGER.info("Offsets for topic '{}' Pre write: {}", topicName, String.join(", ", newKafkaTopicOffsets));
    newKafkaTopicOffsets.forEach(nkto -> {
      assertFalse(pastKafkaTopicOffsets.stream().anyMatch(pkto -> pkto.compareTo(nkto) > -1));
      pastKafkaTopicOffsets.add(nkto);
    });

    Instant start = Instant.now();
    // Run the data injector with the new file map
    DataInjectorConfiguration dataInjectorConfiguration = new DataInjectorConfiguration(injectorFileMap,
            numMessages, interval, batchCount, messageType, topicName);
    runDataInjector(dataInjectorConfiguration);
    Instant finish = Instant.now();

    newKafkaTopicOffsets = this.environment.deploymentCtxt().getTopicOffset(topicName);
    LOGGER.info("Offsets for topic '{}' Post write: {}", topicName, String.join(", ", newKafkaTopicOffsets));
    newKafkaTopicOffsets.forEach(nkto -> {
      assertFalse(pastKafkaTopicOffsets.stream().anyMatch(pkto -> pkto.compareTo(nkto) > -1));
      pastKafkaTopicOffsets.add(nkto);
    });
    var lastOffset = pastKafkaTopicOffsets.stream()
        .map(o -> Long.parseLong(o.split(":")[2]))
        .max(Long::compareTo)
        .orElse(0L);

    final var expectedMessages = (long) (batchCount * numMessages);
    assertEquals(expectedMessages, lastOffset, "Processed messages did not have expected count");
    final var elapsed = Duration.between(start, finish).toMillis();
    LOGGER.info("Elapsed time - Message Process: {} ms", elapsed);
    final var intervalNanos = Duration.parse(interval).toMillis();
    final var batchInterval = (batchCount * intervalNanos);
    assertTrue(elapsed >= batchInterval, "");

    assertTrue(true);
  }

  @Then("I can verify {int} batches of {int} messages were received on {string} and stored in {string}, and none of the services crashed during the transmission")
  public void iCanVerifyBatchesOfMessagesWereReceivedOnSomeTopicAndNoneOfTheServicesCrashedDuringTheTransmission(
      int batchCount, int numMessages, String topicName, String tableType) {

    OsdTableType osdTableType = OsdTableType.valueOf(tableType);
    int numStoredMessages = 0;
    int totalMessages = 0;

    Instant start = Instant.now();

    switch(osdTableType) {
      case RAW_STATION_DATA_FRAME:
        List<RawStationDataFrame> rsdfs = environment.getSohRepositoryInterface().
                retrieveRawStationDataFramesByStationAndTime(stationTimeRangeRequest);
        totalMessages = batchCount * numMessages;

        numStoredMessages = rsdfs.size();
        break;

      case STATION_SOH:
        List<StationSoh> stationSohs = environment.getSohRepositoryInterface().
                retrieveByStationId(stationNames);
        totalMessages = batchCount * numMessages;

        numStoredMessages = stationSohs.size();
        break;

      case ACEI_ANALOG:
        totalMessages = batchCount * numMessages;

        List<AcquiredChannelEnvironmentIssueAnalog> aceiAnalogs = environment.getSohRepositoryInterface().
                retrieveAcquiredChannelEnvironmentIssueAnalogByChannelAndTimeRange(aceiAnalogRequest);

        numStoredMessages = aceiAnalogs.size();
        break;

      case ACEI_BOOLEAN:
        totalMessages = 1;

        List<AcquiredChannelEnvironmentIssueBoolean> aceiBooleans = environment.getSohRepositoryInterface().
                retrieveAcquiredChannelEnvironmentIssueBooleanByChannelAndTimeRange(aceiBooleanRequest);

        numStoredMessages = aceiBooleans.size();
        break;

      default:
        break;
    }

    Instant finish = Instant.now();
    long elapsed = Duration.between(start, finish).toMillis();
    LOGGER.info("Elapsed time: {} ms", elapsed);

    LOGGER.info("Topic: {}", topicName);
    LOGGER.info("Number of stored messages: {}", numStoredMessages);
    LOGGER.info("Total Messages = {}", totalMessages);

    assertEquals(totalMessages, numStoredMessages);
  }

  @Then("I can verify {int} batches of {int} messages were received on {string}")
  public void iCanVerifyBatchesOfMessagesWereReceivedOnSomeTopic(
          int batchCount, int numMessages, String topicName) {

    Instant start = Instant.now();

    int expectedMessages = (batchCount * numMessages);
    List<String> newKafkaTopicOffsets = this.environment.deploymentCtxt().getTopicOffset(topicName);
    LOGGER.info("Offsets for topic '{}' Pre Read: {}", topicName, String.join(", ", newKafkaTopicOffsets));
    var lastProcessedOffset = newKafkaTopicOffsets.stream()
        .map(o -> Long.parseLong(o.split(":")[2]))
        .max(Long::compareTo)
        .orElse(0L);

    final List<String> receivedMessages = this.environment.deploymentCtxt().receiveKafkaMessages(topicName,
            expectedMessages, 0);

    Instant finish = Instant.now();
    long elapsed = Duration.between(start, finish).toMillis();
    LOGGER.info("Elapsed time - Message Read: {} ms", elapsed);

    LOGGER.info("Topic: {}", topicName);
    LOGGER.info("Number of sent messages: {}", expectedMessages);
    LOGGER.info("Total Messages Received= {}", receivedMessages.size());

    assertThat("received messages should not be null", receivedMessages, notNullValue());
    assertFalse("received messages should not be empty", receivedMessages.isEmpty());
    assertEquals(expectedMessages, receivedMessages.size(),"received messages did not have expected count");
    assertEquals(expectedMessages, lastProcessedOffset,"processed messages did not have expected count");
  }

  /* ------------------------------------------------------
   * Private Methods
   * ------------------------------------------------------ */

  private Map<String, String> createInjectorFileMap(String fileName) throws Exception {
    // Method for parsing the files into remote paths and file contents (Lists of strings)

    var fileNameArray = fileName.split("/");
    var fileNameVar = fileNameArray[fileNameArray.length - 1];
    String remotePath = String.format("/mockdata/%s", fileNameVar);

    URL url = StepUtils.class.getClassLoader().getResource(RESOURCE_PATH_PREFIX + fileNameVar);
    File jsonFile = Paths.get(url.toURI()).toFile();
    String fileContents = new String(Files.asByteSource(jsonFile).read());

    return Map.of(FILE_NAME, fileName, REMOTE_PATH, remotePath, FILE_CONTENT, fileContents);
  }

  // Run the data injector using ExecutorService for the number of messages coming in
  private void runDataInjector(DataInjectorConfiguration config) throws Exception {

    // File names, remote paths and number of messages
    String fileName = config.fileMap.get(FILE_NAME);
    String remotePath = config.fileMap.get(REMOTE_PATH);
    String numMessages = config.numMessages;
    String interval = config.interval;

    // Task manager for running the data injector
    var taskManager = new SohTaskManager();

    // Batch count and networks
    var batchCountVar = String.valueOf(config.batchCount);
    var network = this.environment.deploymentCtxt().getDeploymentNetwork();
    assertTrue(network.isPresent(), "Deployment Network is unavailable");
    deploymentNetwork = (Network) network.get();

    // Create list of Futures for each DataInjector task
    Future<Boolean> future = taskManager.runTask(fileName, remotePath, numMessages, interval, batchCountVar, config);
    future.get();

    taskManager.shutdown();
  }

  /**
   * Create the ACEI boolean request object using time ranges and the number of messages
   * to add extra milliseconds to the ACEI object
   */
  private ChannelTimeRangeRequest createACEIRequest(AcquiredChannelEnvironmentIssue acei, int numMessages) {

    String channelName = acei.getChannelName();
    Instant startTime = acei.getStartTime().plusMillis(1);         // need to add 1ms to start time
    Instant endTime = acei.getEndTime().plusMillis(numMessages+1); // add numMessages ms to end time

    return ChannelTimeRangeRequest.create(channelName, startTime, endTime);
  }

  // Data injector configuration class
  private static class DataInjectorConfiguration {
    private final Map<String, String> fileMap;
    private final String numMessages;
    private final String interval;
    private final String messageType;
    private final int batchCount;
    private final String topicName;

    public DataInjectorConfiguration(Map<String, String> fileMap, int numMessages,
                                     String interval, int batchCount,
                                     String messageType, String topicName) {
      this.fileMap = fileMap;
      this.numMessages = Integer.toString(numMessages);
      this.interval = interval;
      this.batchCount = batchCount;
      this.messageType = messageType;
      this.topicName = topicName;
    }
  }

  /* ------------------------------------------------------
   * Sub Classes
   * ------------------------------------------------------ */
  class SohTaskManager {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public Future<Boolean> runTask(String fileName, String remotePath, String numMessages,
                                   String interval, String batchCount, DataInjectorConfiguration config) {
      return executor.submit(() -> {


        dataInjector
                .withNetworkMode(deploymentNetwork.getName())
                .withClasspathResourceMapping(fileName, remotePath, BindMode.READ_ONLY)
                .withCommand(
                        "--type", config.messageType,
                        "--batchCount", batchCount,
                        "--batchSize", numMessages,
                        "--interval", interval,
                        "--base", remotePath,
                        "--topic", config.topicName,
                        "--bootstrapServer", "kafka1:9092")
                .start();

        Duration duration = Duration.parse(config.interval);
        final var sleepMs = duration.toMillis();

        int expectedMessages = (config.batchCount * Integer.parseInt(config.numMessages));
        while (getTopicOffset(config) < expectedMessages) {
          Thread.sleep(sleepMs);
        }

        return Boolean.TRUE;
      });
    }

    private Long getTopicOffset(DataInjectorConfiguration config) {
      List<String> newKafkaTopicOffsets = environment.deploymentCtxt().getTopicOffset(config.topicName);
      LOGGER.debug("Offsets for topic '{}' Processing: {}", config.topicName, String.join(", ", newKafkaTopicOffsets));
      return newKafkaTopicOffsets.stream()
          .map(o -> Long.parseLong(o.split(":")[2]))
          .max(Long::compareTo)
          .orElse(0L);
    }

    public void shutdown() {
      executor.shutdown();
    }
  }
}
