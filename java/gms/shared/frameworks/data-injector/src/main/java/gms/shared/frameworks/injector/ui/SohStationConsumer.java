package gms.shared.frameworks.injector.ui;

import com.fasterxml.jackson.databind.ObjectMapper;
import gms.core.performancemonitoring.uimaterializedview.QuietedSohStatusChangeUpdate;
import gms.core.performancemonitoring.uimaterializedview.SohQuietAndUnacknowledgedCacheManager;
import gms.shared.frameworks.osd.coi.datatransferobjects.CoiObjectMapperFactory;
import gms.core.performancemonitoring.uimaterializedview.AcknowledgedSohStatusChange;
import gms.shared.frameworks.systemconfig.SystemConfig;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;

import org.apache.kafka.clients.consumer.CommitFailedException;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SohStationConsumer is responsible for managing the quiet list by listening to acknowledged and quiet Kafka topics.
 * When we receive a list of the channel
 */
public class SohStationConsumer {
    private static final Logger logger = LoggerFactory.getLogger(SohStationConsumer.class);
    private static final String ACK_STATION_SOH_INPUT_TOPIC_NAME = "station-soh-analysis-manager.statusChangeInputTopic";
    private static final String QUIET_SOH_INPUT_TOPIC_NAME = "station-soh-analysis-manager.quietedListInputTopic";
    private String quietedTopicName = "soh.quieted-list";
    private String acknowledgeTopicName = "soh.ack-station-soh";
    private SohQuietAndUnacknowledgedCacheManager quietAndUnacknowledgedListsManager;
    private UiStationAndStationGroupsModifier uiStationAndStationGroupsModifier;
    private boolean sendUiUpdate = false;

    /**
     * Constructor for SohStationConsumer
     * @param quietAndUnacknowledgedListsManager - reference to manager to clear unacknowledged changes for a station
     * @param uiStationAndStationGroupsModifier - reference to publish immediate update after receiving
     *                                          an acknowledgement or quiet
     */
    public SohStationConsumer(
        SohQuietAndUnacknowledgedCacheManager quietAndUnacknowledgedListsManager,
        UiStationAndStationGroupsModifier uiStationAndStationGroupsModifier) {
        // Used to send update UI Soh Station after receiving an ack or quiet message
        this.uiStationAndStationGroupsModifier = uiStationAndStationGroupsModifier;

        // Called to clear unacknowledged entries
        this.quietAndUnacknowledgedListsManager = quietAndUnacknowledgedListsManager;

        // Get the consumer topic strings
        try{
            SystemConfig systemConfig = SystemConfig.create("mockProducer");
            this.quietedTopicName = systemConfig.getValue(QUIET_SOH_INPUT_TOPIC_NAME);
            this.acknowledgeTopicName = systemConfig.getValue(ACK_STATION_SOH_INPUT_TOPIC_NAME);
        } catch(Exception ex){
            logger.error("Error using SystemConfig to look up Quiet and Acknowledge topics {}",ex.getMessage());
        }
    }

    /**
     * Sets our Kafka consumer and subscribes to acknowledged and quiet topics. Runs as a listener.
     * Processes Acknowledged and Quiet messages. Sends new UI SOH Station with updates.
     * @param consumer
     */
    public void setConsumer(Consumer<String, String> consumer) {
        long batchInterval = 15;
        
        // subscribe to all the topics we need to persist
        try {
            Runnable runnable = () -> { 
                consumer.subscribe(List.of(this.acknowledgeTopicName, this.quietedTopicName));
                boolean done = false;
                while (!done) {
                    ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(batchInterval));
                    List<String> stationNames = new ArrayList<>();
                    // Process any Acknowledgement messages
                    records.records(this.acknowledgeTopicName).forEach(record -> {
                        Optional<AcknowledgedSohStatusChange> acknowledgedStation = parseAcknowledgeSoh(record.value());
                        if (acknowledgedStation.isPresent()) {
                            String comment = acknowledgedStation.get().getComment().isPresent() ? " : " + acknowledgedStation.get().getComment().get() : "";
                            logger.info(
                                "Received soh acknowledgment for station: {} by {} {}",
                                acknowledgedStation.get().getAcknowledgedStation(),
                                acknowledgedStation.get().getAcknowledgedBy(),
                                comment);

                            //take ack station and add channel/mon pairs to quiet list
                            if(this.quietAndUnacknowledgedListsManager.
                                addAcknowledgedStationToQuietList(acknowledgedStation.get())) {
                                stationNames.add(acknowledgedStation.get().getAcknowledgedStation());
                                this.sendUiUpdate = true;
                            }
                        }
                    });
                    
                    // Process any Quiet Channel messages
                    records.records(this.quietedTopicName).forEach(record -> {
                        Optional<QuietedSohStatusChangeUpdate> quietedChannel =
                            parseQuietedSohStatusChange(record.value());
                        if (quietedChannel.isPresent()) {
                            logger.info(
                                "Received soh quiet for channel: {}/{} {}",
                                quietedChannel.get().getChannelName(),
                                quietedChannel.get().getSohMonitorType(),
                                quietedChannel.get().getComment());

                            // Replace quieted entry no matter what the previous duration/until was set to
                            QuietedSohStatusChangeUpdate quietedChange = quietedChannel.get();
                            if (this.quietAndUnacknowledgedListsManager.addQuietSohStatusChange(quietedChange)) {
                                stationNames.add(quietedChange.getStationName());
                                this.sendUiUpdate = true;
                            }
                        }
                    });
                    // Send new UI SOH Station with update acknowledgement/quiet status changes
                    if (this.sendUiUpdate) {
                        this.uiStationAndStationGroupsModifier.generateAndPublishUiStationSoh(stationNames);
                        stationNames.clear();
                        this.sendUiUpdate = false;
                    }
                    if (records.count() != 0) {
                        logger.info("Finished processing {} records.", records.count());
                    }
                    try {
                        consumer.commitSync();
                    } catch (CommitFailedException e) {
                        logger.error("broker commit failed", e);
                        done = true;
                    }
                }
            };

            final Thread ackSohThread = new Thread(runnable);
            ackSohThread.start();
        } finally {
            logger.info("OSD AcknowledgedSohStatusChange Kafka Consumer timed out of session. Application closing and restarting...");
        }
    }

    /**
     * Takes an acknowledged soh status change JSON and returns an AcknowledgedSohStatusChange
     * @param ackSoh
     * @return Optional<AcknowledgedSohStatusChange>
     */
    private static Optional<AcknowledgedSohStatusChange> parseAcknowledgeSoh(String ackSoh) {
        ObjectMapper mapper = CoiObjectMapperFactory.getJsonObjectMapper();
        try {
            return Optional.ofNullable(mapper.readValue(ackSoh, AcknowledgedSohStatusChange.class));
        } catch (IOException ex) {
             logger.error("Error parsing AcknowledgedSohStatusChange: {}", ex.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Takes the quietedSoh JSON and returns a QuietedSohStatusChange.
     * @param quietedSoh
     * @return Optional<QuietedSohStatusChange>
     */
    private static Optional<QuietedSohStatusChangeUpdate> parseQuietedSohStatusChange(String quietedSoh) {
        ObjectMapper mapper = CoiObjectMapperFactory.getJsonObjectMapper();
        try {
            return Optional.ofNullable(mapper.readValue(quietedSoh, QuietedSohStatusChangeUpdate.class));
        } catch (IOException ex) {
             logger.error(
                 "Error parsing QuietedSohStatusChangeUpdate: {} message string: {} ",
                 ex.getMessage(),
                 quietedSoh);
        }
        return Optional.empty();
    }
}