package gms.integration.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.dockerjava.api.model.Network;
import gms.shared.frameworks.test.utils.services.GmsServiceType;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public class DataBurstTestSteps {

  private static final Logger logger = LoggerFactory.getLogger(DataBurstTestSteps.class);

  private final GenericContainer dataInjector = new GenericContainer(
      "${CI_DOCKER_REGISTRY}/gms-common/frameworks-data-injector:latest");

  private final GenericContainer kafkaClient = new GenericContainer(
      "${CI_DOCKER_REGISTRY}/gms-common/kafka-client:latest");

  private Network deploymentNetwork;
  private Environment environment;

  public DataBurstTestSteps(Environment environment) {
    this.environment = environment;
  }

  @Given("The RSDF streams processor is healthy")
  public void theRSDFStreamsProcessorIsHealthy() {
    assertTrue(
        this.environment.deploymentCtxt().isServiceCreated(GmsServiceType.RSDF_STREAMS_PROCESSOR));
    assertTrue(
        this.environment.deploymentCtxt().isServiceRunning(GmsServiceType.RSDF_STREAMS_PROCESSOR));
    var network = this.environment.deploymentCtxt().getDeploymentNetwork();
    assertTrue(network.isPresent(), "Deployment Network is unavailable");
    deploymentNetwork = (Network) network.get();
    logger.info("Updated deploymentNetwork to {}", deploymentNetwork);
  }

  @When("I send {string} of {string} messages on the {string} topic, receiving on the {string} topic, using {string} to create messages with {float} and {float}")
  public void iSendMessagesEveryIntervalUnitOnTheTopic(String batchConfiguration,
      String messageType, String sourceTopic, String destinationTopic, String pathToFileWithName,
      float lagTolerancePercentage, float latencyTolerancePercentage)
      throws InterruptedException, ExecutionException {
    var totalExpectedMessageCount = 0;
    var batchConfigurationsStrings = batchConfiguration.split(":");
    var batchConfigurations = new BatchConfiguration[batchConfigurationsStrings.length];

    for (var i = 0; i < batchConfigurationsStrings.length; i++) {
      batchConfigurations[i] = new BatchConfiguration(batchConfigurationsStrings[i]);
    }

    for (BatchConfiguration configuration : batchConfigurations) {
      totalExpectedMessageCount += (Integer.parseInt(configuration.batchSize) * Integer
          .parseInt(configuration.batchCount));
    }

    var fileNameArray = pathToFileWithName.split("/");
    var fileName = fileNameArray[fileNameArray.length - 1];
    var remotePath = String.format("/mockdata/%s", fileName);
    var sampledLag = new ArrayList<Integer>();
    var timer = new Timer("Timer");

    // may make groupName configurable
    // periodically get the lag
    var lagMonitor = new TimerTask() {
      public void run() {
        Map<String, Map<Integer, Integer>> lagObject = environment.deploymentCtxt()
            .getConsumerGroupLag("cd11-rsdf-processor");
        var lag = lagObject.get(sourceTopic).get(0);
        sampledLag.add(lag);
      }
    };

    timer.scheduleAtFixedRate(lagMonitor, 0, 200L);

    var taskMetadata = new TaskMetadata[batchConfigurations.length];
    var futuresLists = new Future[batchConfigurations.length];
    var taskManager = new TaskManager();

    var consumedMessageThreshold = 0;
    for (var i = 0; i < batchConfigurations.length; i++) {
      var expectedMessagesForIteration = (Integer.parseInt(batchConfigurations[i].batchSize)
          * Integer
          .parseInt(batchConfigurations[i].batchCount));
      consumedMessageThreshold += expectedMessagesForIteration;
      futuresLists[i] = taskManager
          .runTask(batchConfigurations[i], destinationTopic, pathToFileWithName, remotePath,
              messageType, sourceTopic, consumedMessageThreshold);
    }

    var sampledLagLengthAtBaseline = 0;
    var baselineLag = 0.0;

    for (var i = 0; i < futuresLists.length; i++) {
      taskMetadata[i] = (TaskMetadata) futuresLists[i].get();
      var elapsedTime = Duration
          .between(taskMetadata[i].startTime, taskMetadata[i].endTime);
      logger.info(
          "task {} monitored consumption of {} messages in {} seconds ({} ms), with average latency {} ms",
          i, taskMetadata[i].numberOfConsumedMessages, elapsedTime.getSeconds(),
          elapsedTime.toMillis(),
          elapsedTime.toMillis() / taskMetadata[i].numberOfConsumedMessages);
      if (i == 0) {
        sampledLagLengthAtBaseline = sampledLag.size();
        baselineLag = averageLag(sampledLag);
      }
    }

    taskManager.shutdown();

    var allLatency = 0;
    var subSampledLag = sampledLag.subList(sampledLagLengthAtBaseline, sampledLag.size());
    var averageLag = averageLag(subSampledLag);

    var initialBatchSize = Integer.parseInt(batchConfigurations[0].batchSize);
    var thresholdLag = getThresholdValue(baselineLag, lagTolerancePercentage, initialBatchSize);
    var elapsedTime = Duration
        .between(taskMetadata[0].startTime, taskMetadata[0].endTime);
    var baselineLatencyMs = elapsedTime.toMillis() / taskMetadata[0].numberOfConsumedMessages;
    var thresholdLatency = getThresholdValue(baselineLatencyMs, latencyTolerancePercentage,
        initialBatchSize);

    for (var i = 1; i < taskMetadata.length; i++) {
      elapsedTime = Duration.between(taskMetadata[i].startTime, taskMetadata[i].endTime);
      var iterationLatency = elapsedTime.toMillis() / taskMetadata[i].numberOfConsumedMessages;
      assertTrue(iterationLatency <= thresholdLatency);
      allLatency += iterationLatency;
    }

    var overallLag = averageLag / taskMetadata.length;
    var overallLatency = allLatency / taskMetadata.length;

    logger.info("Total Message Count: {}", totalExpectedMessageCount);
    logger.info("Acceptable Average Lag: {}", thresholdLag);
    logger.info("Acceptable Average Latency (ms): {}", thresholdLatency);
    logger.info("Overall Average Lag: {}", overallLag);
    logger.info("Overall Average Latency (ms): {}", overallLatency);

    assertTrue(overallLag <= thresholdLag);
    assertTrue(overallLatency <= thresholdLatency);
  }

  @Then("I can verify, using a specific {string}, that all messages were received on the {string}, and none of the services crashed during the transmission")
  public void iCanVerifyNoneOfTheServicesCrashedDuringTheTransmission(String batchConfiguration,
      String destinationTopic) {
    var totalExpectedMessageCount = 0;
    var batchConfigurationsStrings = batchConfiguration.split(":");
    var batchConfigurations = new BatchConfiguration[batchConfigurationsStrings.length];

    for (var i = 0; i < batchConfigurationsStrings.length; i++) {
      batchConfigurations[i] = new BatchConfiguration(batchConfigurationsStrings[i]);
    }

    for (BatchConfiguration configuration : batchConfigurations) {
      totalExpectedMessageCount += (Integer.parseInt(configuration.batchSize) * Integer
          .parseInt(configuration.batchCount));
    }

    var consumedMessages = this.environment.deploymentCtxt()
        .receiveKafkaMessages(destinationTopic, totalExpectedMessageCount, 0);
    assertFalse(List.of(
        GmsServiceType.KAFKA_ONE,
        GmsServiceType.ETCD,
        GmsServiceType.OSD_SERVICE,
        GmsServiceType.POSTGRES_SERVICE,
        GmsServiceType.POSTGRES_EXPORTER,
        GmsServiceType.PROCESSING_CONFIG_SERVICE,
        GmsServiceType.RSDF_STREAMS_PROCESSOR)
        .stream()
        .map(service -> this.environment.deploymentCtxt()
            .isServiceHealthy(service))
        .collect(Collectors.toList())
        .contains(false));
    assertEquals(totalExpectedMessageCount, consumedMessages.size());
  }

  private double averageLag(List<Integer> list) {
    var lagArray = new int[list.size()];
    for (var i = 0; i < list.size(); i++) {
      lagArray[i] = list.get(i);
    }

    return Arrays.stream(lagArray).average().orElse(0.0);
  }

  private double getThresholdValue(double baselineValue, float percentage, int initialBatchSize) {
    if (baselineValue == 0) {
      baselineValue = initialBatchSize;
    }

    if (percentage > 100) {
      return baselineValue * (percentage / 100);
    }

    return baselineValue * (1 + (percentage / 100));
  }

  class TaskManager {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public Future<TaskMetadata> runTask(BatchConfiguration batchConfiguration,
        String destinationTopic, String pathToFileWithName, String remotePath,
        String messageType, String sourceTopic, int consumedMessageThreshold) {
      return executor.submit(() -> {
        var batchSize = batchConfiguration.batchSize;
        var batchCount = batchConfiguration.batchCount;
        var expectedMessagesForIteration =
            Integer.parseInt(batchSize) * Integer.parseInt(batchCount);

        kafkaClient.withNetworkMode(deploymentNetwork.getName())
            .withCommand(
                "-t", destinationTopic, "-m", String.valueOf(expectedMessagesForIteration)
            )
            .start();

        // start the injector
        dataInjector
            .withNetworkMode(deploymentNetwork.getName())
            .withClasspathResourceMapping(pathToFileWithName, remotePath, BindMode.READ_ONLY)
            .withCommand(
                "--type", messageType, "--batchSize", batchSize, "--interval",
                batchConfiguration.interval,
                "--batchCount", batchCount, "--base", remotePath, "--topic",
                sourceTopic, "--bootstrapServer", "kafka1:9092"
            )
            .start();

        var startTime = Instant.now();

        var beginningOffsetSet = false;
        var beginningOffset = 0;
        var consumedOffset = 0;
        while (consumedOffset < consumedMessageThreshold) {
          List<String> response = environment.deploymentCtxt()
              .getTopicOffset(destinationTopic);
          String[] tokens = response.get(0).split(":");
          if (tokens.length > 0) {
            try {
              consumedOffset = Integer.parseInt(tokens[tokens.length - 1]);
            } catch (NumberFormatException e) {
              logger.warn("No offset available", e);
            }
            if (!beginningOffsetSet) {
              beginningOffset = consumedOffset;
              beginningOffsetSet = true;
            }
          }
          Thread.sleep(500);
        }

        // not sure if we need to do this
        dataInjector.stop();
        kafkaClient.stop();

        return new TaskMetadata(startTime, Instant.now(), (consumedOffset - beginningOffset));
      });
    }

    public void shutdown() {
      executor.shutdown();
    }
  }

  // I had problems with AutoValue and generated sources in the Integration project,
  // so we're defining TaskMetadata here for now.
  private static class TaskMetadata {

    private final int numberOfConsumedMessages;
    private final Instant startTime;
    private final Instant endTime;

    public TaskMetadata(Instant startTime, Instant endTime, int numberOfConsumedMessages) {
      this.numberOfConsumedMessages = numberOfConsumedMessages;
      this.startTime = startTime;
      this.endTime = endTime;
    }

  }

  private static class BatchConfiguration {

    private final String batchCount;
    private final String batchSize;
    private final String interval;

    public BatchConfiguration(String batchConfiguration) {
      var batchConfigurationParameters = batchConfiguration.split(",");
      batchSize = batchConfigurationParameters[0];
      batchCount = batchConfigurationParameters[1];
      interval = batchConfigurationParameters[2];
    }
  }
}
