package gms.integration.steps;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Files;
import gms.integration.util.StepUtils;
import gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssueAnalog;
import gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssueBoolean;
import gms.shared.frameworks.osd.coi.datatransferobjects.CoiObjectMapperFactory;
import gms.shared.frameworks.osd.coi.soh.StationSoh;
import gms.shared.frameworks.osd.coi.waveforms.RawStationDataFrame;
import gms.shared.frameworks.test.utils.services.OsdTableType;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.net.URL;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
public class OsdSohDBStoragePerformanceComponentSteps<T> {

  private static final Logger LOGGER = LoggerFactory.getLogger(OsdSohDBStoragePerformanceComponentSteps.class);

  //connecting to the DB and initializing cache are time consuming, so tests should be run multiple times to get an avg
  private static final double NUM_TIMES_RUN_TEST = 10;
  private static final int THREAD_POOL_SIZE = 20;

  private static final String RESOURCE_PATH_PREFIX =
          "gms/integration/data/";
  private static final String FILE_NAME = "fileName";
  private static final String REMOTE_PATH = "remotePath";
  private static final String FILE_CONTENT = "fileContent";

  private Environment environment;

  List<List<T>> storageObjects = new ArrayList<>();
  Map<String, String> injectorFileMap;

  // Maps Json objects to corresponding GMS objects
  private static final ObjectMapper objectMapper = CoiObjectMapperFactory.getJsonObjectMapper();

  public OsdSohDBStoragePerformanceComponentSteps(Environment environment) {
    this.environment = environment;
  }

  @When("I create {int} messages of type {string} using {string} to create messages to be stored in {string} in specified duration on average")
  public void iCreateMessagesToBeStoredInTheDB(
          int numMessages, String type, String fileName, String tableType) throws Exception {
    storageObjects.clear();
    // Create the time range request based on the station/channel name
    OsdTableType osdTableType = OsdTableType.valueOf(tableType);

    injectorFileMap = createInjectorFileMap(fileName);
    injectorFileMap.forEach((k, v) -> LOGGER.info("{} : {}", k, v));

    switch(osdTableType) {
      case RAW_STATION_DATA_FRAME:
        createRsdfList(numMessages);
        break;
      case STATION_SOH:
        createStationSohList(numMessages);
        break;
      case ACEI_BOOLEAN:
        createAceiBooleanList(numMessages);
        break;
      case ACEI_ANALOG:
        createAceiAnalogList(numMessages);
        break;
    }
  }

  @Then("I can verify {int} messages were stored in duration {string} in table {string} and none of the services crashed during the test")
  public void iVerifyNumMessagesAreStoredInTheDBInOneSecond(
          int numMessages, String duration, String tableType) {
    Instant start = Instant.now();
    OsdTableType osdTableType = OsdTableType.valueOf(tableType);

    switch(osdTableType) {
      case RAW_STATION_DATA_FRAME:
        for(List<T> myList : storageObjects) {
          environment.getSohRepositoryInterface().storeRawStationDataFrames(
                  (Collection<RawStationDataFrame>) myList);
        }
        break;
      case STATION_SOH:
        for(List<T> myList : storageObjects) {
          environment.getSohRepositoryInterface().storeStationSoh((Collection<StationSoh>) myList);
        }
        break;
      case ACEI_BOOLEAN:
        for(List<T> myList : storageObjects){
          environment.getSohRepositoryInterface().storeAcquiredChannelEnvironmentIssueBoolean(
                  (Collection<AcquiredChannelEnvironmentIssueBoolean>) myList);
        }
        break;
      case ACEI_ANALOG:
        for(List<T> myList : storageObjects){
          environment.getSohRepositoryInterface().storeAcquiredChannelSohAnalog(
                  (Collection<AcquiredChannelEnvironmentIssueAnalog>) myList);
        }
        break;
    }
    Instant finish = Instant.now();
    long timeElapsed = Duration.between(start, finish).toMillis();
    double avgTime = timeElapsed/NUM_TIMES_RUN_TEST;
    System.out.println(osdTableType + "::avgTimeElapsed: " + avgTime);

    Duration interval = Duration.parse(duration);
    assertTrue(avgTime < interval.toMillis(), osdTableType + " took too long(" + avgTime +") to complete inserts");
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

  private void createAceiBooleanList(int numMessages) throws JsonProcessingException {
    Optional<?> aceiOpt = Optional.of(objectMapper.readValue(injectorFileMap.get(FILE_CONTENT),
            AcquiredChannelEnvironmentIssueBoolean.class));
    AcquiredChannelEnvironmentIssueBoolean acei = (AcquiredChannelEnvironmentIssueBoolean) aceiOpt.get();

    for(int i=0;i<NUM_TIMES_RUN_TEST;i++){
      List<T> curList = new ArrayList<>();
      for(int j=0;j<numMessages;j++){
        curList.add((T)AcquiredChannelEnvironmentIssueBoolean.from(
                UUID.randomUUID(),
                acei.getChannelName(),
                acei.getType(),
                acei.getStartTime().plusMillis(i),
                acei.getEndTime().plusMillis(i+1),
                acei.getStatus()
        ));
      }
      storageObjects.add(curList);
    }
  }
  private void createAceiAnalogList(int numMessages) throws JsonProcessingException {
    Optional<?> aceiOpt = Optional.of(objectMapper.readValue(injectorFileMap.get(FILE_CONTENT),
            AcquiredChannelEnvironmentIssueAnalog.class));
    AcquiredChannelEnvironmentIssueAnalog acei = (AcquiredChannelEnvironmentIssueAnalog) aceiOpt.get();

    for(int i=0;i<NUM_TIMES_RUN_TEST;i++){
      List<T> curList = new ArrayList<>();
      for(int j=0;j<numMessages;j++){
        curList.add((T)AcquiredChannelEnvironmentIssueAnalog.from(
                UUID.randomUUID(),
                acei.getChannelName(),
                acei.getType(),
                acei.getStartTime().plusMillis(i),
                acei.getEndTime().plusMillis(i+1),
                acei.getStatus()
        ));
      }
      storageObjects.add(curList);
    }
  }
  private void createStationSohList(int numMessages) throws JsonProcessingException {
    Optional<?> stationSohOpt = Optional.of(objectMapper.readValue(injectorFileMap.get(FILE_CONTENT),
            StationSoh.class));
    StationSoh stationSoh = (StationSoh) stationSohOpt.get();
    Instant time = null;
    for(int i=0;i<NUM_TIMES_RUN_TEST;i++){
      List<T> curList = new ArrayList<>();
      for(int j=0;j<numMessages;j++){
        if(time == null){
          time = stationSoh.getTime();
        }
        curList.add((T)  StationSoh.from(UUID.randomUUID(),
                time,
                stationSoh.getStationName(),
                stationSoh.getSohMonitorValueAndStatuses(),
                stationSoh.getSohStatusRollup(),
                stationSoh.getChannelSohs(),
                stationSoh.getAllStationAggregates()));
        time = time.plusSeconds(20);
      }
      storageObjects.add(curList);
    }
  }
  private void createRsdfList(int numMessages) throws JsonProcessingException {
    // RSDF Will have a single object to compare
    Optional<?> rsdfOpt = Optional.of(objectMapper.readValue(injectorFileMap.get(FILE_CONTENT),
            RawStationDataFrame.class));
    RawStationDataFrame rsdf = (RawStationDataFrame) rsdfOpt.get();
    for(int i=0;i<NUM_TIMES_RUN_TEST;i++){
      List<T> curList = new ArrayList<>();
      for(int j=0;j<numMessages;j++){
        curList.add((T) RawStationDataFrame.builder()
                .generatedId()
                .setMetadata(rsdf.getMetadata())
                .setRawPayload(rsdf.getRawPayload())
                .build());
      }
      storageObjects.add(curList);
    }
  }
}
