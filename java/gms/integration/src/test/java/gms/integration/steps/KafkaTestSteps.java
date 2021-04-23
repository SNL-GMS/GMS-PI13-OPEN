package gms.integration.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import gms.shared.frameworks.osd.coi.channel.dataacquisitionstatus.AcquiredStationSohExtract;
import gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssueAnalog;
import gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssueBoolean;
import gms.shared.frameworks.osd.coi.waveforms.RawStationDataFrame;
import gms.shared.frameworks.test.utils.services.GmsServiceType;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public class KafkaTestSteps {

  private final ObjectMapper jsonMapper = new ObjectMapper().findAndRegisterModules()
      .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
      .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
      .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
  private Map<String, Class> typeMap;
  private Environment environment;

  public KafkaTestSteps(Environment environment) {
    this.environment = environment;
    this.typeMap = Map.of(
        "RawStationDataFrame", RawStationDataFrame.class,
        "AcquiredChannelEnvironmentIssueBoolean", AcquiredChannelEnvironmentIssueBoolean.class,
        "AcquiredChannelEnvironmentIssueAnalog", AcquiredChannelEnvironmentIssueAnalog.class,
        "AcquiredStationSohExtract", AcquiredStationSohExtract.class
    );
  }

  @Given("Kafka is up and running")
  public void testKafkaIsUpAndRunning() {
    assertTrue(this.environment.deploymentCtxt().isServiceCreated(GmsServiceType.KAFKA_ONE));
    assertTrue(this.environment.deploymentCtxt().isServiceRunning(GmsServiceType.KAFKA_ONE));
  }

  @And("all the topics have been created")
  public void verifyKafkaTopicGeneration() {
    Set<String> topicStream = new HashSet<>(this.environment.deploymentCtxt().getKafkaTopics());
    topicStream.retainAll(
        Set.of("soh.rsdf", "soh.acei", "soh.extract", "soh.waveform", "soh.station-soh",
            "soh.ack-station-soh"));
    assertTrue(!topicStream.isEmpty());
  }


  @When("I am able to send a {string} in {string} to the {string} topic")
  public void sendMessageToKafkaTest(String dataType, String filePath, String topicName) {
    try {
      var data = jsonMapper.readValue(new File(getClass().getClassLoader()
              .getResource(filePath).toURI()),
          typeMap.get(dataType));
      this.environment.deploymentCtxt().sendKafkaMessages(topicName, List.of(jsonMapper.writeValueAsString(data)));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Then("I am able to receive the same {string} in {string} from the {string} topic")
  public void verifyKafkaMessageWasProperlyWritten(String dataType, String filePath, String topicName)
      throws IOException {
    final List<String> receivedMessages = this.environment.deploymentCtxt().receiveKafkaMessages(topicName,
            1, 0);
    assertEquals(1, receivedMessages.size());
    try {
      var expected = jsonMapper.readValue(new File(getClass().getClassLoader()
              .getResource(filePath).toURI()),
          typeMap.get(dataType));
      var actual = jsonMapper.readValue(receivedMessages.get(0), typeMap.get(dataType));
      assertEquals(expected, actual);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}
