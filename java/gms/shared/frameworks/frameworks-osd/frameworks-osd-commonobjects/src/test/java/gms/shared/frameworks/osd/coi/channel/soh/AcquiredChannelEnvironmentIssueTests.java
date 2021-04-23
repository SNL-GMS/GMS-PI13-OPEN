package gms.shared.frameworks.osd.coi.channel.soh;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import gms.shared.frameworks.osd.coi.util.TestUtilities;
import gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssue.AcquiredChannelEnvironmentIssueType;
import gms.shared.frameworks.osd.coi.datatransferobjects.CoiObjectMapperFactory;
import gms.shared.frameworks.osd.coi.test.utils.UtilsTestFixtures;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link AcquiredChannelEnvironmentIssue} creation and usage semantics Created by trsault on 8/25/17.
 */

public class AcquiredChannelEnvironmentIssueTests {

  private final AcquiredChannelEnvironmentIssueType calib = AcquiredChannelEnvironmentIssueType.CALIBRATION_UNDERWAY;
  private final Instant epoch = Instant.EPOCH;
  private final Instant later = epoch.plusSeconds(30);
  private final String channelName = UtilsTestFixtures.PROCESSING_CHANNEL_1_NAME;

  @Test
  public void testSerializationAnalog() throws Exception {
    TestUtilities.testSerialization(UtilsTestFixtures.channelSohAnalog,
        AcquiredChannelEnvironmentIssueAnalog.class);
  }

  @Test
  public void testSerializationBoolean() throws Exception {
    TestUtilities.testSerialization(UtilsTestFixtures.channelSohBoolean,
        AcquiredChannelEnvironmentIssueBoolean.class);
  }

  @Test
  public void equalsAndHashcodeTest() {
    TestUtilities.checkClassEqualsAndHashcode(AcquiredChannelEnvironmentIssueAnalog.class);
    TestUtilities.checkClassEqualsAndHashcode(AcquiredChannelEnvironmentIssueBoolean.class);
  }

  @Test
  public void testAcquiredChannelSohAnalogCreateNullParameters() throws Exception {
    TestUtilities.checkStaticMethodValidatesNullArguments(
        AcquiredChannelEnvironmentIssueAnalog.class, "create",
        channelName, calib, epoch, later, 0.0);


  }

  @Test
  public void testAcquiredChannelSohAnalogFromNullParameters() throws Exception {
    TestUtilities.checkStaticMethodValidatesNullArguments(
        AcquiredChannelEnvironmentIssueAnalog.class, "from",
        UtilsTestFixtures.CHANNEL_SEGMENT_ID, channelName,
        calib, epoch, later, 0.0);
  }

  @Test
  public void testAcquiredChannelSohBooleanCreateNullParameters() throws Exception {
    TestUtilities.checkStaticMethodValidatesNullArguments(
        AcquiredChannelEnvironmentIssueBoolean.class, "create",
        channelName, calib, epoch, later,
        false);
  }

  @Test
  public void testAcquiredChannelSohBooleanFromNullParameters() throws Exception {
    TestUtilities.checkStaticMethodValidatesNullArguments(
        AcquiredChannelEnvironmentIssueBoolean.class, "from",
        UtilsTestFixtures.SOH_ANALOG_ID, channelName,
        calib, epoch, later,
        false);
  }

  @Test
  public void testSerializeDeserializeAcquiredChannelSoh() throws Exception {

    AcquiredChannelEnvironmentIssueBoolean sohBoolean = AcquiredChannelEnvironmentIssueBoolean.from(
        UUID.randomUUID(),
        UtilsTestFixtures.PROCESSING_CHANNEL_1_NAME,
        AcquiredChannelEnvironmentIssueType.VAULT_DOOR_OPENED,
        Instant.ofEpochSecond(1),
        Instant.ofEpochSecond(11),
        true);
    AcquiredChannelEnvironmentIssueBoolean sohBoolean2 = AcquiredChannelEnvironmentIssueBoolean.from(
        UUID.randomUUID(),
        UtilsTestFixtures.PROCESSING_CHANNEL_1_NAME,
        AcquiredChannelEnvironmentIssueType.VAULT_DOOR_OPENED,
        Instant.ofEpochSecond(11),
        Instant.ofEpochSecond(21),
        false);
    List<AcquiredChannelEnvironmentIssueBoolean> sohBooleanList = List.of(sohBoolean, sohBoolean2);

    AcquiredChannelEnvironmentIssueAnalog sohAnalog = AcquiredChannelEnvironmentIssueAnalog.from(
        UUID.randomUUID(),
        UtilsTestFixtures.PROCESSING_CHANNEL_1_NAME,
        AcquiredChannelEnvironmentIssueType.VAULT_DOOR_OPENED,
        Instant.ofEpochSecond(1),
        Instant.ofEpochSecond(11),
        15.123);
    AcquiredChannelEnvironmentIssueAnalog sohAnalog2 = AcquiredChannelEnvironmentIssueAnalog.from(
        UUID.randomUUID(),
        UtilsTestFixtures.PROCESSING_CHANNEL_1_NAME,
        AcquiredChannelEnvironmentIssueType.VAULT_DOOR_OPENED,
        Instant.ofEpochSecond(11),
        Instant.ofEpochSecond(21),
        16.456);
    List<AcquiredChannelEnvironmentIssueAnalog> sohAnalogList = List.of(sohAnalog, sohAnalog2);

    List<AcquiredChannelEnvironmentIssue> sohList = List.of(sohBoolean, sohBoolean2, sohAnalog, sohAnalog2);

    ObjectMapper objectMapper = CoiObjectMapperFactory.getJsonObjectMapper();

    // Boolean SOH.
    String json = objectMapper.writeValueAsString(sohBoolean);
    AcquiredChannelEnvironmentIssue soh = objectMapper.readValue(json, AcquiredChannelEnvironmentIssue.class);
    assertEquals(sohBoolean, (AcquiredChannelEnvironmentIssueBoolean) soh);
    assertEquals(sohBoolean, objectMapper.readValue(json, AcquiredChannelEnvironmentIssueBoolean.class));
    assertEquals(sohBoolean,
        objectMapper.readValue(json, new TypeReference<AcquiredChannelEnvironmentIssueBoolean>() {
        }));

    // Analog SOH.
    json = objectMapper.writeValueAsString(sohAnalog);
    soh = objectMapper.readValue(json, AcquiredChannelEnvironmentIssue.class);
    assertEquals(sohAnalog, (AcquiredChannelEnvironmentIssueAnalog) soh);
    assertEquals(sohAnalog, objectMapper.readValue(json, AcquiredChannelEnvironmentIssueAnalog.class));
    assertEquals(sohAnalog,
        objectMapper.readValue(json, new TypeReference<AcquiredChannelEnvironmentIssueAnalog>() {
        }));

    // List of Boolean SOH.
    json = objectMapper.writeValueAsString(sohBooleanList);
    List<AcquiredChannelEnvironmentIssueBoolean> sohBools = objectMapper.readValue(
        json,
        new TypeReference<List<AcquiredChannelEnvironmentIssueBoolean>>() {
        });
    assertEquals(sohBooleanList, sohBools);

    // List of Analog SOH.
    json = objectMapper.writeValueAsString(sohAnalogList);
    List<AcquiredChannelEnvironmentIssueAnalog> sohAnalogs = objectMapper.readValue(
        json,
        new TypeReference<List<AcquiredChannelEnvironmentIssueAnalog>>() {
        });
    assertEquals(sohAnalogList, sohAnalogs);

    // List of Analog and Boolean SOH.
    json = objectMapper.writeValueAsString(sohList);
    List<AcquiredChannelEnvironmentIssue> sohs = objectMapper.readValue(
        json,
        new TypeReference<List<AcquiredChannelEnvironmentIssue>>() {
        });
    assertEquals(sohList, sohs);
  }
}
