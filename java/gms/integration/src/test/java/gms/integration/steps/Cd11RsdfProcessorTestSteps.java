package gms.integration.steps;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Files;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11ByteFrame;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11DataFrame;
import gms.integration.util.StepUtils;
import gms.shared.frameworks.osd.coi.channel.dataacquisitionstatus.AcquiredStationSohExtract;
import gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssue;
import gms.shared.frameworks.osd.coi.datatransferobjects.CoiObjectMapperFactory;
import gms.shared.frameworks.osd.coi.waveforms.RawStationDataFrame;
import gms.shared.frameworks.osd.coi.waveforms.RawStationDataFrameMetadata;
import gms.shared.frameworks.test.utils.services.GmsServiceType;
import io.cucumber.core.internal.gherkin.deps.com.google.gson.JsonElement;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * CD11 Rsdf Processor Gerkin test steps using supplied rsdf objects from a local directory
 * containing rsdf.json files
 */
@Testcontainers
public class Cd11RsdfProcessorTestSteps {

  private static final Logger logger = LoggerFactory.getLogger(Cd11RsdfProcessorTestSteps.class);

  private static final String RESOURCE_PATH_PREFIX =
      "gms/integration/requests/dataacquisition/cd11/rsdf/processor/";

  // JSON Resources for comparing kafka messages
  private static final String ACEI_RESOURCE = "soh-acei.json";
  private static final String EXTRACT_RESOURCE = "soh-extract.json";

  // Keys for extracting classes from the ClassMap
  private static final String ACEI_CLASS_KEY = "AcquiredChannelEnvironmentIssue";
  private static final String EXTRACT_CLASS_KEY = "AcquiredStationSohExtract";
  private static final String RSDF_CLASS_KEY = "RawStationDataFrame";
  private static final String RSDF_METADATA_CLASS_KEY = "RawStationDataFrameMetadata";

  // Class map for separating the rsdf, acei and extract objects
  private static final Map<String, Class<?>> CLASS_MAP = new HashMap<>();

  // Maps Json objects to corresponding GMS objects
  private static final ObjectMapper objectMapper = CoiObjectMapperFactory.getJsonObjectMapper();

  static {
    CLASS_MAP.put(ACEI_CLASS_KEY, AcquiredChannelEnvironmentIssue.class);
    CLASS_MAP.put(EXTRACT_CLASS_KEY, AcquiredStationSohExtract.class);
    CLASS_MAP.put(RSDF_CLASS_KEY, RawStationDataFrame.class);
    CLASS_MAP.put(RSDF_METADATA_CLASS_KEY, RawStationDataFrameMetadata.class);
  }

  // Resource files for the expected ACEI and Soh Extract objects
  String aceiResourceFile = RESOURCE_PATH_PREFIX + ACEI_RESOURCE;
  String extractResourceFile = RESOURCE_PATH_PREFIX + EXTRACT_RESOURCE;

  // Map containing list of resource files for RSDF objects
  private Map<Class<?>, List<?>> listsParsedFromResources = new HashMap<>();
  private List<AcquiredChannelEnvironmentIssue> expectedACEIList = new ArrayList<>();
  private List<AcquiredStationSohExtract> expectedSohExtractList = new ArrayList<>();
  
  private Environment environment;

  public Cd11RsdfProcessorTestSteps(Environment environment) {
    this.environment = environment;
  }

  @Given("an input {string} RSDF resource file contains JSON versions of {string} objects")
  public void anInputResourceContainsJsonVersionsOf(String resourceName, String className)
      throws Exception {
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
    logger.info("Input RSDF: {}", rsdf.toString());

    // Get the raw payload and create Cd11DataFrame
    Cd11DataFrame df;
    try (ByteArrayInputStream input = new ByteArrayInputStream(rsdf.getRawPayload())) {
      DataInputStream rawPayloadInputStream;
      rawPayloadInputStream = new DataInputStream(input);
      Cd11ByteFrame bf = new Cd11ByteFrame(rawPayloadInputStream, () -> true);
      df = new Cd11DataFrame(bf);
    }
    logger.info("Cd11DataFrame:");
    logger.info(df.toString());
    logger.info("\n");

    listsParsedFromResources.put(clazz, Arrays.asList(rsdf));
  }

  @Given("the {string} object is written to the kafka topic {string}")
  public void theObjectIsWrittenToTheKafkaTopic(String rsdfClass, String topicName)
      throws Exception {
    // First of all, get the rsdf parsed from a resource in a previous step.
    Class<?> clazz = CLASS_MAP.get(rsdfClass);
    List<?> objects = listsParsedFromResources.get(clazz);

    assertNotNull(objects);

    Optional<Integer> portOpt = Optional
        .of(this.environment.deploymentCtxt().getServicePort(GmsServiceType.KAFKA_ONE));
    assertTrue(portOpt.isPresent());

    logger.info("Number of objects read from resources file: " + objects.size());

    // Will be something like localhost:9092
    final String bootstrapServers = String
        .format("%s:%d", this.environment.deploymentCtxt().getServiceHost(GmsServiceType.KAFKA_ONE),
            this.environment.deploymentCtxt().getServicePort(GmsServiceType.KAFKA_ONE));

    logger.info("+++++ bootstrap servers: {}, topic name: {}", bootstrapServers, topicName);

    ObjectMapper jsonMapper = CoiObjectMapperFactory.getJsonObjectMapper();

    List<String> jsons = new ArrayList<>();
    for (var object : objects) {
      try {
        jsons.add(jsonMapper.writeValueAsString(object));
      } catch (JsonProcessingException e) {
        e.printStackTrace();
      }
    }

    //send the data out to the topic
    assertTrue(this.environment.deploymentCtxt().isServiceHealthy(GmsServiceType.KAFKA_ONE));
    this.environment.deploymentCtxt().sendKafkaMessages(topicName, jsons);

  }

  @Then("within a period of {int} seconds expected {string} and {string} messages are readable from the kafka topics {string} and {string} respectively")
  public void withinGivenPeriodMessagesAreReadableFromTheKafkaTopic(int timeoutSeconds,
      String aceiClass,
      String extractClass, String aceiTopic,
      String extractTopic) throws Exception {
    final Class<?> clazzAcei = CLASS_MAP.get(aceiClass);
    final Class<?> clazzExtract = CLASS_MAP.get(extractClass);
    assertNotNull(clazzAcei);
    assertNotNull(clazzExtract);

    // Create the expected ACEI list from the json resource file
    JsonElement aceiElement = StepUtils.parseJsonResource(aceiResourceFile);
    expectedACEIList.addAll(StepUtils.createACEIList(aceiElement.getAsJsonArray()));

    // Create the expected Soh Extract object from the json resource file
    JsonElement extractElement = StepUtils.parseJsonResource(extractResourceFile);
    expectedSohExtractList.add(StepUtils.createSohExtract(extractElement));

    // Consume the Acei and Soh Extract messages from the Kafka queue
    List<String> receivedACEIList = this.environment.deploymentCtxt().receiveKafkaMessages(aceiTopic,
            expectedACEIList.size(), timeoutSeconds*1000);
    List<String> receivedSohExtractList = this.environment.deploymentCtxt().receiveKafkaMessages(extractTopic,
            expectedSohExtractList.size(), timeoutSeconds*1000);

    logger.info("Read in {} messages fom the acei topic: ", receivedACEIList.size());
    logger.info("Read in {} messages from the soh extract topic: ", receivedSohExtractList.size());

    logger.info("Received ACEI Objects:");
    receivedACEIList.forEach(acei -> logger.info(acei));

    logger.info("Received Soh Extract objects:");
    receivedSohExtractList.forEach(extract -> logger.info(extract));

    boolean aceiMessagesPassed = compareSohAceiObjects(receivedACEIList);
    boolean sohExtractMessagesPassed = compareSohExtractObjects(receivedSohExtractList);

    assertTrue(sohExtractMessagesPassed, "Failed to receive the expected ACEI messages");
    assertTrue(aceiMessagesPassed, "Failed to receive the expected SoH Extract");
  }


  // Compare Acei objects from json expected and kafka messages
  private boolean compareSohAceiObjects(List<String> kafkaMessages) {

    // Create the JsonElement list from the kafka messages
    List<JsonElement> jsonElementList = StepUtils.createJsonElementList(kafkaMessages);

    logger.info("Soh ACEI messages, expected : received = {} : {}", expectedACEIList.size(), jsonElementList.size());

    // Compare the JSON element list size with the kafka messages size
    if (expectedACEIList.size() != jsonElementList.size()) {
      return false;
    }

    // Create the ACEI object list from kafka messages
    List<AcquiredChannelEnvironmentIssue> receivedAceiList = jsonElementList.
        stream().map(msg -> StepUtils.createACEIObject(msg)).collect(Collectors.toList());

    // Compare ACEI object lists
    Predicate<AcquiredChannelEnvironmentIssue> aceiPredicate = s1 ->
            expectedACEIList.stream().anyMatch(s2 -> s1.hasSameState(s2));
    boolean aceiMatch = receivedAceiList.stream().allMatch(aceiPredicate);
    logger.info("Soh ACEI elements match? {}", aceiMatch);

    return aceiMatch;
  }

  // Compare Soh Extract objects from json expected and kafka messages
  private boolean compareSohExtractObjects(List<String> kafkaMessages) throws Exception {
    logger.info("SohExtract messages, expected : received = {} : {}", expectedSohExtractList.size(),
            kafkaMessages.size());

    // Create the JsonElement list from the kafka messages
    List<JsonElement> jsonElementList = StepUtils.createJsonElementList(kafkaMessages);
    int numberOfMessages = jsonElementList.size();

    // Ensure that JsonElement list only has one object
    if (numberOfMessages != 1) {
      return false;
    }

    // Create Kafka Expected and Received SOH Extract objects from kafka messages
    JsonElement jsonSohExtract = jsonElementList.get(0);

    AcquiredStationSohExtract receivedSohExtract = StepUtils.createSohExtract(jsonSohExtract);
    AcquiredStationSohExtract expectedSohExtract = expectedSohExtractList.get(0);

    // Get the expected lists
    List<AcquiredChannelEnvironmentIssue> expectedAceiList = expectedSohExtract.getAcquiredChannelEnvironmentIssues();
    List<RawStationDataFrameMetadata> expectedRsdfMetaList = expectedSohExtract.getAcquisitionMetadata();

    // First extract the lists
    List<AcquiredChannelEnvironmentIssue> receivedAceiList = receivedSohExtract.getAcquiredChannelEnvironmentIssues();
    List<RawStationDataFrameMetadata> receivedRsdfMetaList = receivedSohExtract.getAcquisitionMetadata();

    // Compare the expected vs received ACEI and RSDF
    Predicate<AcquiredChannelEnvironmentIssue> aceiPredicate = s1 ->
            expectedAceiList.stream().anyMatch(s2 -> s1.hasSameState(s2));
    Predicate<RawStationDataFrameMetadata> rsdfMetaPredicate = s1 ->
            expectedRsdfMetaList.stream().anyMatch(s2 -> s1.hasSameState(s2));
    boolean aceiMatch = receivedAceiList.stream().allMatch(aceiPredicate);
    boolean rsdfMetaMatch = receivedRsdfMetaList.stream().allMatch(rsdfMetaPredicate);
    boolean sohExtractMatch = aceiMatch && rsdfMetaMatch;

    logger.info("SohExtract ACEI elements match? {}", aceiMatch);
    logger.info("SohExtract RSDFMetadata elements match? {}", rsdfMetaMatch);
    logger.info("SohExtract kafka messages match the expected objects? {}", sohExtractMatch);

    return sohExtractMatch;
  }
}