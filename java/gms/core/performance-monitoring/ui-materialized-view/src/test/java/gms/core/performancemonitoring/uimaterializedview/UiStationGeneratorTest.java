package gms.core.performancemonitoring.uimaterializedview;

import static gms.core.performancemonitoring.uimaterializedview.utils.MaterializedViewTestFixtures.ALT_UNACK_CHANGE_1;
import static gms.core.performancemonitoring.uimaterializedview.utils.MaterializedViewTestFixtures.ENV_GAP_UNACK_CHANGE_1;
import static gms.core.performancemonitoring.uimaterializedview.utils.MaterializedViewTestFixtures.QUIETED_CHANGE_1;
import static gms.core.performancemonitoring.uimaterializedview.utils.MaterializedViewTestFixtures.STATION_SOH_PARAMETERS;
import static gms.core.performancemonitoring.uimaterializedview.utils.MaterializedViewTestFixtures.UNACK_CHANGE_1;
import static gms.shared.frameworks.osd.coi.SohTestFixtures.ALT_SIMPLE_MARGINAL_STATION_SOH;
import static gms.shared.frameworks.osd.coi.SohTestFixtures.MARGINAL_STATION_GROUP_AND_STATION_CAPABILITY_ROLLUP;
import static gms.shared.frameworks.osd.coi.SohTestFixtures.MARGINAL_STATION_GROUP_BAD_STATION_CAPABILITY_ROLLUP;
import static gms.shared.frameworks.osd.coi.SohTestFixtures.SIMPLE_MARGINAL_STATION_SOH;
import static gms.shared.frameworks.osd.coi.SohTestFixtures.STATION_NEEDS_ATTENTION_MESSAGE_TEMPLATE;
import static gms.shared.frameworks.osd.coi.systemmessages.SystemMessageType.STATION_CAPABILITY_STATUS_CHANGED;
import static gms.shared.frameworks.osd.coi.systemmessages.SystemMessageType.STATION_NEEDS_ATTENTION;

import gms.shared.frameworks.osd.coi.soh.StationSoh;
import gms.shared.frameworks.osd.coi.soh.quieting.UnacknowledgedSohStatusChange;
import gms.shared.frameworks.osd.coi.systemmessages.SystemMessage;
import gms.shared.frameworks.osd.coi.systemmessages.SystemMessageType;
import gms.shared.frameworks.osd.coi.systemmessages.util.StationNeedsAttentionBuilder;
import gms.shared.frameworks.osd.coi.test.utils.UtilsTestFixtures;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.EmitterProcessor;

@Disabled("Thread yield() calls cause intermittent failures in the pipeline")
public class UiStationGeneratorTest {

  @BeforeAll
  static void initializeContributingMap() {
    // Initialize the StationSohContributingUtility before creating a channel soh
    StationSohContributingUtility.getInstance().initialize(STATION_SOH_PARAMETERS);
  }

  @Test
  void testNewSameStationCapabilityStatusDoesNotGenerateMessage() {

    var outputSystemMessages = new ArrayList<SystemMessage>();

    var systemMessagesEmitter = EmitterProcessor.<SystemMessage>create();

    var systemMessagesSink = systemMessagesEmitter.sink();

    var disposable = systemMessagesEmitter.subscribe(outputSystemMessages::add);


    //
    // Starting off, there should be no messages for STATION_CAPABILITY_STATUS_CHANGED
    //
    UiStationGenerator.buildUiStationSohList(
        List.of(SIMPLE_MARGINAL_STATION_SOH),
        List.of(),
        List.of(),
        List.of(MARGINAL_STATION_GROUP_AND_STATION_CAPABILITY_ROLLUP),
        STATION_SOH_PARAMETERS,
        List.of(UtilsTestFixtures.STATION_GROUP),
        systemMessagesSink
    );

    //
    // Wait some time for threads to finish
    //
    var startMs = System.currentTimeMillis();
    while (System.currentTimeMillis() - startMs < 500) {
      Thread.yield();
    }

    Assertions.assertTrue(outputSystemMessages.stream()
        .noneMatch(systemMessage -> systemMessage.getType() == STATION_CAPABILITY_STATUS_CHANGED));

    //
    // Change the status of the station. There should be a message for
    // STATION_CAPABILITY_STATUS_CHANGED
    //
    UiStationGenerator.buildUiStationSohList(
        List.of(SIMPLE_MARGINAL_STATION_SOH),
        List.of(),
        List.of(),
        List.of(MARGINAL_STATION_GROUP_BAD_STATION_CAPABILITY_ROLLUP),
        STATION_SOH_PARAMETERS,
        List.of(UtilsTestFixtures.STATION_GROUP),
        systemMessagesSink
    );

    //
    // Wait some time for threads to finish
    //
    startMs = System.currentTimeMillis();
    while (System.currentTimeMillis() - startMs < 500) {
      Thread.yield();
    }

    Assertions.assertTrue(
        outputSystemMessages.stream().anyMatch(systemMessage ->
            systemMessage.getType() == STATION_CAPABILITY_STATUS_CHANGED)
    );

    // clear this, so its easy to test that there are no new messages
    outputSystemMessages.clear();

    //
    // Another CapabilitySohRollup with the station staying in the same status. There should
    // be no new message for STATION_CAPABILITY_STATUS_CHANGED
    //
    UiStationGenerator.buildUiStationSohList(
        List.of(SIMPLE_MARGINAL_STATION_SOH),
        List.of(),
        List.of(),
        List.of(MARGINAL_STATION_GROUP_BAD_STATION_CAPABILITY_ROLLUP),
        STATION_SOH_PARAMETERS,
        List.of(UtilsTestFixtures.STATION_GROUP),
        systemMessagesSink
    );

    //
    // Wait some time for threads to finish
    //
    startMs = System.currentTimeMillis();
    while (System.currentTimeMillis() - startMs < 500) {
      Thread.yield();
    }

    Assertions.assertFalse(
        outputSystemMessages.stream().anyMatch(systemMessage ->
            systemMessage.getType() == STATION_CAPABILITY_STATUS_CHANGED)
    );

    // Just so that the subscriber isnt waiting around for more SystemMessages that will
    // never come.
    systemMessagesSink.complete();
    disposable.dispose();
  }

  @ParameterizedTest
  @MethodSource("needsAttentionSystemMessagesTestSource")
  void testNeedsAttentionSystemMessages(
      List<StationSoh> stationSohs,
      List<UnacknowledgedSohStatusChange> unacknowledgedSohStatusChanges,
      List<QuietedSohStatusChangeUpdate> quietedSohStatusChangeUpdates,
      Map<SystemMessageType, List<SystemMessage>> expectedMessages
  ) {

    var outputSystemMessages = new ArrayList<SystemMessage>();

    var systemMessagesEmitter = EmitterProcessor.<SystemMessage>create();

    var systemMessagesSink = systemMessagesEmitter.sink();

    var disposable = systemMessagesEmitter.subscribe(outputSystemMessages::add);

    UiStationGenerator.clearPrevious();

    UiStationGenerator.buildUiStationSohList(
        stationSohs,
        unacknowledgedSohStatusChanges,
        quietedSohStatusChangeUpdates,
        List.of(MARGINAL_STATION_GROUP_AND_STATION_CAPABILITY_ROLLUP),
        STATION_SOH_PARAMETERS,
        List.of(UtilsTestFixtures.STATION_GROUP),
        systemMessagesSink
    );

    var expectedMessagesForType = expectedMessages.get(STATION_NEEDS_ATTENTION);

    var actualMessagesForType = outputSystemMessages.stream()
        .filter(systemMessage -> systemMessage.getType() == STATION_NEEDS_ATTENTION)
        .collect(Collectors.toList());

    Assertions.assertEquals(
        expectedMessagesForType.size(),
        actualMessagesForType.size()
    );

    IntStream.range(0, expectedMessagesForType.size()).forEach(i ->
        assertEqualSystemMessages(
            expectedMessagesForType.get(i),
            actualMessagesForType.get(i)
        )
    );

    outputSystemMessages.clear();

    UiStationGenerator.buildUiStationSohList(
        List.of(SIMPLE_MARGINAL_STATION_SOH),
        unacknowledgedSohStatusChanges,
        quietedSohStatusChangeUpdates,
        List.of(),
        STATION_SOH_PARAMETERS,
        List.of(UtilsTestFixtures.STATION_GROUP),
        systemMessagesSink
    );

    //
    // Wait some time for threads to finish
    //
    var startMs = System.currentTimeMillis();
    while (System.currentTimeMillis() - startMs < 500) {
      Thread.yield();
    }

    Assertions.assertTrue(outputSystemMessages.stream()
        .noneMatch(systemMessage -> systemMessage.getType() == STATION_NEEDS_ATTENTION));

    // Just so that the subscriber isnt waiting around for more SystemMessages that will
    // never come.
    systemMessagesSink.complete();
    disposable.dispose();

  }

  private static Stream<Arguments> needsAttentionSystemMessagesTestSource() {

    return Stream.of(
        //
        // Single new unacknowledged change should produce a message
        //
        Arguments.arguments(
            List.of(SIMPLE_MARGINAL_STATION_SOH),
            List.of(UNACK_CHANGE_1),
            List.of(),
            Map.of(
                STATION_NEEDS_ATTENTION,
                List.of(
                    new StationNeedsAttentionBuilder(UNACK_CHANGE_1.getStation())
                        .build())
            )
        ),

        //
        // Two new unacknowledged changes should produce two new messages
        //
        Arguments.arguments(
            List.of(SIMPLE_MARGINAL_STATION_SOH, ALT_SIMPLE_MARGINAL_STATION_SOH),
            List.of(UNACK_CHANGE_1, ALT_UNACK_CHANGE_1),
            List.of(),
            Map.of(
                STATION_NEEDS_ATTENTION,
                List.of(
                    new StationNeedsAttentionBuilder(UNACK_CHANGE_1.getStation())
                        .build(),
                    new StationNeedsAttentionBuilder(ALT_UNACK_CHANGE_1.getStation())
                        .build()
                )
            )
        ),

        //
        // Two new unacknowledged changes, where one is quited, should produce one new message
        //
        Arguments.arguments(
            List.of(SIMPLE_MARGINAL_STATION_SOH, ALT_SIMPLE_MARGINAL_STATION_SOH),
            List.of(ENV_GAP_UNACK_CHANGE_1, ALT_UNACK_CHANGE_1),
            List.of(QUIETED_CHANGE_1),
            Map.of(
                STATION_NEEDS_ATTENTION,
                List.of(
                    new StationNeedsAttentionBuilder(ALT_UNACK_CHANGE_1.getStation())
                        .build()
                )
            )
        )
    );
  }

  private void assertEqualSystemMessages(
      SystemMessage expectedMessage,
      SystemMessage actualMessage
  ) {

    Assertions.assertEquals(
        expectedMessage.getMessage(),
        actualMessage.getMessage()
    );

    Assertions.assertEquals(
        expectedMessage.getMessageTags(),
        actualMessage.getMessageTags()
    );
  }
}
