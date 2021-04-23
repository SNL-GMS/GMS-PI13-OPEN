package gms.integration.steps;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import gms.integration.util.OSDUtil;
import gms.integration.util.ServiceUtility;
import gms.integration.util.StepUtils;
import gms.shared.frameworks.osd.coi.channel.dataacquisitionstatus.AcquiredStationSohExtract;
import gms.shared.frameworks.osd.coi.datatransferobjects.CoiObjectMapperFactory;
import gms.shared.frameworks.osd.coi.signaldetection.Station;
import gms.shared.frameworks.osd.coi.signaldetection.StationGroup;
import gms.shared.frameworks.osd.coi.soh.CapabilitySohRollup;
import gms.shared.frameworks.osd.coi.soh.SohMonitorType;
import gms.shared.frameworks.osd.coi.soh.SohMonitorValueAndStatus;
import gms.shared.frameworks.osd.coi.soh.SohStatus;
import gms.shared.frameworks.osd.coi.soh.StationSoh;
import gms.shared.frameworks.osd.coi.waveforms.RawStationDataFrameMetadata;
import gms.shared.frameworks.osd.coi.waveforms.RawStationDataFramePayloadFormat;
import gms.shared.frameworks.test.utils.services.GmsServiceType;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import java.io.IOException;
import java.net.URL;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public class StationSohControlTestSteps {

  private static final Logger logger = LoggerFactory.getLogger(StationSohControlTestSteps.class);

  private static final String RESOURCE_PATH_PREFIX = "gms/integration/data/";

  private static final String STATION_RESOURCE = "twentystations.json";
  private static final String STATION_GROUP_RESOURCE = "threegroups.json";

  // To map simple class names used in feature files to their actual classes.
  private static final Map<String, Class<?>> CLASS_MAP = new HashMap<>();
  // Each pair contains the resource path and a list of objects of the specified class
  // parsed from the resource.
  private Map<Class<?>, Pair<String, List<?>>> listsParsedFromResources = new HashMap<>();
  // Lists of object parsed from messages in kafka topics.
  private Map<Class<?>, List<?>> listsParsedFromKafkaOutput = new HashMap<>();
  private Environment environment;

  public StationSohControlTestSteps(Environment environment) {
    this.environment = environment;
  }

  // Populate the class map.
  static {
    CLASS_MAP.put("AcquiredStationSohExtract", AcquiredStationSohExtract.class);
    CLASS_MAP.put("StationSoh", StationSoh.class);
    CLASS_MAP.put("CapabilitySohRollup", CapabilitySohRollup.class);
  }

  @Given("The Soh Control Service is alive")
  public void theSohControlServiceIsAlive() throws Exception {
    // The former code called the alive endpoint. But checking that the health status
    // of the container is healthy also does the job and we don't have to wait for the port
    // to open.

    // First of all, the service must have been created.
    assertTrue(this.environment.deploymentCtxt().isServiceCreated(GmsServiceType.SOH_CONTROL));

    // Now, it might take a while for the service's container to arrive in the healthy
    // state.

    // Used as a delay mechanism. SonarCube complains about Thread.sleep().
    final Object waitMonitor = new Object();

    // Give it up to 120 seconds to arrive in the healthy state. Was taking 10 seconds in
    // my local tests.
    final Instant timesUp = Instant.now().plusSeconds(120);
    boolean healthy = false;
    int waitSeconds = 0;
    do {
      synchronized (waitMonitor) {
        // Wait 5 seconds
        waitMonitor.wait(5000L);
      }
      waitSeconds += 5;
      healthy = this.environment.deploymentCtxt().isServiceHealthy(GmsServiceType.SOH_CONTROL);
    } while (!healthy && Instant.now().isBefore(timesUp));

    assertTrue(healthy);

    logger.info(">>>> Waited {} seconds to confirm health", waitSeconds);
  }

  @And("appropriate stations and station groups are stored in and retrievable from the OSD")
  public void appropriateStationsAndStationGroupsAreStoredInTheOSD() throws Exception {

    // This step assumes that no stations or station groups are in the OSD at the beginning
    // of the step.

    // Reads the stations from a resource
    Optional<List<Station>> stationsOpt = StepUtils.readJsonListResource(
            RESOURCE_PATH_PREFIX + STATION_RESOURCE, Station.class
    );

    assertTrue(stationsOpt.isPresent());

    // Reads the station groups from a resource
    Optional<List<StationGroup>> stationGroupsOpt = StepUtils.readJsonListResource(
            RESOURCE_PATH_PREFIX + STATION_GROUP_RESOURCE, StationGroup.class
    );

    assertTrue(stationGroupsOpt.isPresent());

    List<Station> stations = stationsOpt.get();
    List<StationGroup> stationGroups = stationGroupsOpt.get();

    logger.info(">>>> Read {} stations and {} station groups",
            stations.size(), stationGroups.size());

    ServiceUtility.DOCKER_PORT = this.environment.deploymentCtxt()
            .getServicePort(GmsServiceType.OSD_SERVICE);

    Collection<Station> stationsToStore = OSDUtil.stationsNotInOSD(stations);
    Collection<StationGroup> stationGroupsToStore = OSDUtil.stationGroupsNotInOSD(stationGroups);

    // Store stations and station groups in the OSD.
    // These will throw IOExceptions if not successful.
    if (!stationsToStore.isEmpty()) {
      logger.info(">>>> Storing {} stations in the OSD", stationsToStore.size());
      OSDUtil.storeStations(stationsToStore);
    }
    if (!stationGroupsToStore.isEmpty()) {
      logger.info(">>>> Storing {} station groups in the OSD", stationGroupsToStore.size());
      OSDUtil.storeStationGroups(stationGroupsToStore);
    }
  }

  @And("an input {string} resource contains JSON versions of {string} objects")
  public void anInputResourceContainsJsonVersionsOf(String resourceName, String className)
          throws Exception {
    Class<?> clazz = CLASS_MAP.get(className);
    assertNotNull(clazz);

    final String resourcePath = RESOURCE_PATH_PREFIX + resourceName;

    List<?> inputList;
    Optional<List<Object>> inputOpt;

    if (className.equals("StationSoh")) {
      ObjectMapper objectMapper = CoiObjectMapperFactory.getJsonObjectMapper();
      URL stationSohURL = Thread.currentThread().getContextClassLoader()
              .getResource(resourcePath);
      inputList = objectMapper.<List<StationSoh>>readValue(stationSohURL, new TypeReference<>() {
      });
    } else {
      inputOpt = StepUtils.readJsonLines(resourcePath, clazz);
      assertTrue(inputOpt.isPresent());
      inputList = inputOpt.get();
    }

    assertFalse(inputList.isEmpty());

    logger.info(">>>> Read {} instances of {} from input resource {}", inputList.size(),
            className, resourceName);

    listsParsedFromResources.put(clazz, Pair.of(resourcePath, inputList));
  }

  @And("the {string} objects are written to the kafka topic {string}")
  public void theObjectsAreWrittenToTheKafkaTopic(String className, String topicName) {

    // First of all, get the list parsed from a resource in a previous step.
    Class<?> clazz = CLASS_MAP.get(className);

    Pair<String, List<?>> pair = listsParsedFromResources.get(clazz);
    assertNotNull(pair);

    ObjectMapper objectMapper = CoiObjectMapperFactory.getJsonObjectMapper();
    List<String> jsonList = pair.getRight().stream().map(extract -> {
      try {
        modifyMetadataTimeValues((AcquiredStationSohExtract) extract);
        return objectMapper.writeValueAsString(extract);
      } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
      }
    }).collect(Collectors.toList());

    assertTrue(this.environment.deploymentCtxt().sendKafkaMessages(topicName, jsonList));

    final int expectedMsgCount = pair.getRight().size();

    assertEquals(expectedMsgCount, this.environment.deploymentCtxt().receiveKafkaMessages(
            topicName, expectedMsgCount, 0).size());
  }

  /**
   * Update the {@link RawStationDataFrameMetadata} time values so the service processes the data.
   *
   * @param extract the {@link AcquiredStationSohExtract} data from the JSON file.
   */
  private void modifyMetadataTimeValues(AcquiredStationSohExtract extract) {
    int month = Instant.now().atZone(ZoneOffset.UTC).getMonth().getValue();
    int day = Instant.now().atZone(ZoneOffset.UTC).getDayOfMonth();
    int hour = Instant.now().atZone(ZoneOffset.UTC).getHour();

    List<RawStationDataFrameMetadata> rsdf = extract.getAcquisitionMetadata();

    rsdf.forEach(metadata -> {
      metadata = metadata.toBuilder().setReceptionTime(
              metadata.getReceptionTime().atZone(ZoneOffset.UTC).withMonth(month)
                      .withDayOfMonth(day).withHour(hour).toInstant())
              .setPayloadStartTime(
                      metadata.getPayloadStartTime().atZone(ZoneOffset.UTC).withMonth(month)
                              .withDayOfMonth(day).withHour(hour).toInstant())
              .setPayloadEndTime(
                      metadata.getPayloadEndTime().atZone(ZoneOffset.UTC).withMonth(month)
                              .withDayOfMonth(day).withHour(hour).toInstant()).build();

      // Collection is always a size of 1, so no issue here.
      rsdf.set(0, metadata);
    });
  }

  @And("within a period of {int} seconds {string} messages are read from the kafka topic {string}")
  public void withinAGivenPeriodMessagesAreReadableFromTheKafkaTopic(
          int timeoutSeconds,
          String className,
          String topicName) throws Exception {

    final Class<?> clazz = CLASS_MAP.get(className);
    assertNotNull(clazz);

    // Initialize to null just in case.
    listsParsedFromKafkaOutput.remove(clazz);

    List<String> messageList = new ArrayList<>();

    Collection<String> messages = this.environment.deploymentCtxt()
            .receiveKafkaMessages(topicName, 10, timeoutSeconds*1000);

    // Occasionally empty strings are returned. Filter them out.
    messages.forEach(msg -> {
      msg = msg.trim();
      if (!msg.isEmpty()) {
        messageList.add(msg);
      }
    });

    assertFalse(messageList.isEmpty());

    // Now have to translate into the proper object type.
    final ObjectMapper objectMapper = CoiObjectMapperFactory.getJsonObjectMapper();
    List<?> objList = messageList.stream()
            .map(s -> {
              try {
                return objectMapper.readValue(s, clazz);
              } catch (IOException e) {
                logger.error("JSON parsing error: {}", e.getMessage());
                throw new RuntimeException(e);
              }
            })
            .collect(Collectors.toList());

    listsParsedFromKafkaOutput.put(clazz, objList);
  }

  @Then("the {string} output matches the {string} input")
  public void theOutputMatchesTheInput(String outputClassName, String inputClassName) {

    final Class<?> outputClazz = CLASS_MAP.get(outputClassName);
    assertNotNull(outputClazz);

    final Class<?> inputClazz = CLASS_MAP.get(inputClassName);
    assertNotNull(inputClassName);

    List<?> outputList = listsParsedFromKafkaOutput.get(outputClazz);
    List<?> inputList = listsParsedFromResources.get(inputClazz).getRight();

    assertNotNull(outputList);
    assertNotNull(inputList);
    assertFalse(outputList.isEmpty());
    assertFalse(inputList.isEmpty());

    // To perform the following checks properly, the configuration of the control must
    // be known. But this integration test doesn't control how the application is configured.
    //
    if (inputClazz == AcquiredStationSohExtract.class && outputClazz == StationSoh.class) {
      AcquiredStationSohExtract input = (AcquiredStationSohExtract) inputList.get(0);
      StationSoh output = (StationSoh) outputList.get(0);

      assertEquals(RawStationDataFramePayloadFormat.CD11,
              input.getAcquisitionMetadata().get(0).getPayloadFormat()
      );

      assertEquals(input.getAcquisitionMetadata().get(0).getStationName(), output.getStationName());

      List<SohMonitorValueAndStatus<?>> statuses = output
              .getSohMonitorValueAndStatuses().stream()
              .filter(
                      s -> s.getMonitorType().equals(SohMonitorType.MISSING))
              .collect(Collectors.toList());

      assertNotNull(statuses);

      SohMonitorValueAndStatus status = statuses.get(0);

      assertEquals(100.0, status.getValue());
      assertEquals(SohStatus.BAD, status.getStatus());
      assertEquals(SohMonitorType.MISSING, status.getMonitorType());
      assertEquals(SohStatus.BAD, output.getSohStatusRollup());
    }
  }

  @Then("the formats have not changed")
  public void theFormatsHaveNotChanged() {
    // Noop -- if the test gets here, the steps had to pass.
  }
}
