package gms.integration.steps;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import gms.integration.util.StepUtils;
import gms.shared.frameworks.osd.api.util.ChannelsTimeRangeRequest;
import gms.shared.frameworks.osd.api.util.StationTimeRangeRequest;
import gms.shared.frameworks.osd.api.util.StationTimeRangeSohTypeRequest;
import gms.shared.frameworks.osd.coi.channel.Channel;
import gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssue;
import gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssueAnalog;
import gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssueBoolean;
import gms.shared.frameworks.osd.coi.datatransferobjects.CoiObjectMapperFactory;
import gms.shared.frameworks.osd.coi.signaldetection.Station;
import gms.shared.frameworks.osd.dto.soh.HistoricalAcquiredChannelEnvironmentalIssues;
import gms.shared.frameworks.soh.repository.performancemonitoring.transform.AcquiredChannelEnvironmentalIssuesTransformer;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.net.URL;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
public class AceiQueryComponentSteps {
    private static final Logger logger = LoggerFactory.getLogger(AceiQueryComponentSteps.class);

    private static final String STATION_NAME = "PLCA";
    private static final String ACEI_DATA_PATH = "gms/integration/data/soh_acei.json";
    ObjectMapper objectMapper = CoiObjectMapperFactory.getJsonObjectMapper();
    private static final Instant QUERY_START_TIME = Instant.parse("2020-01-23T23:45:19.597Z");
    private static final Instant QUERY_END_TIME = Instant.parse("2020-01-23T23:45:30.000Z");
    private static final AcquiredChannelEnvironmentIssue.AcquiredChannelEnvironmentIssueType QUERY_SOH_TYPE =
            AcquiredChannelEnvironmentIssue.AcquiredChannelEnvironmentIssueType.GPS_RECEIVER_UNLOCKED;

    private List<String> channelNames = new ArrayList<>();
    private List<AcquiredChannelEnvironmentIssue.AcquiredChannelEnvironmentIssueType> aceiTypes =
            new ArrayList<>();
    private List<HistoricalAcquiredChannelEnvironmentalIssues> historicalACEIList =
            new ArrayList<>();

    private Environment environment;

    public AceiQueryComponentSteps(Environment environment) {
        this.environment = environment;
    }

    @Given("Referenced acei analog and boolean objects are stored in the osd")
    public void storeAceiAnalogAndBooleanObjects() {
//        this.environment.getSohRepositoryInterface().storeAcquiredChannelSohAnalog();
        URL url = StepUtils.class.getClassLoader().getResource(ACEI_DATA_PATH);
        List<AcquiredChannelEnvironmentIssue> aceiList = new ArrayList<>();
        try {
            aceiList = objectMapper.readValue(url, new TypeReference<>() {});
        } catch(Exception e) {
            e.printStackTrace();
            Assert.fail("Retrieving acei objects from json file failed: " + e.getMessage());
        }

        // Create the channels and types to query
        List<AcquiredChannelEnvironmentIssueAnalog> aceiAnalogs = new ArrayList<>();
        List<AcquiredChannelEnvironmentIssueBoolean> aceiBools = new ArrayList<>();
        List<AcquiredChannelEnvironmentIssue> testAceiList = new ArrayList<>();
        for (AcquiredChannelEnvironmentIssue acei : aceiList) {
            AcquiredChannelEnvironmentIssue.AcquiredChannelEnvironmentIssueType type = acei.getType();
            String channelName = acei.getChannelName();
            if (!channelNames.contains(channelName)) {
                channelNames.add(channelName);
            }
            if (!aceiTypes.contains(type)) {
                aceiTypes.add(type);
            }

            // Create the Historical ACEI List for QUERY_SOH_TYPE to compare against
            if (acei.getType().equals(QUERY_SOH_TYPE)) {
                testAceiList.add(acei);
            }

            switch(acei.getClazz()) {
                case "AcquiredChannelEnvironmentIssueAnalog":
                    aceiAnalogs.add((AcquiredChannelEnvironmentIssueAnalog) acei);
                    break;
                case "AcquiredChannelEnvironmentIssueBoolean":
                    aceiBools.add((AcquiredChannelEnvironmentIssueBoolean) acei);
                    break;
                default:
                    break;
            }
        }
        historicalACEIList = AcquiredChannelEnvironmentalIssuesTransformer.
                toHistoricalAcquiredChannelEnvironmentalIssues(testAceiList);

        logger.info("Test Historical ACEI List (length = {}):", historicalACEIList.size());
        historicalACEIList.forEach(hacei -> {
            logger.info("{} : {} : {}", hacei.getChannelName(), hacei.getMonitorType(), hacei.getTrendLine());
        });

        // Save the ACEI bools and analogs to the OSD
        try{
            this.environment.getSohRepositoryInterface().storeAcquiredChannelSohAnalog(aceiAnalogs);
            this.environment.getSohRepositoryInterface().storeAcquiredChannelEnvironmentIssueBoolean(aceiBools);
        }catch (Exception e){
            Assert.fail("Storing acei objects from json file failed: " + e
                    .getMessage());
        }

    }

    @Then("Querying for the referenced acei objects using station, time range and soh type returns acei list")
    public void retrieveAceiAnalogAndBooleanObjects() {

        Instant start = Instant.now();

        StationTimeRangeSohTypeRequest request = StationTimeRangeSohTypeRequest.create(
                STATION_NAME, QUERY_START_TIME, QUERY_END_TIME, QUERY_SOH_TYPE);

        logger.info("Station Time Range Soh Type Request: {}", request.toString());

        List<HistoricalAcquiredChannelEnvironmentalIssues> queryHistoricalACEIList = this.environment.
                getSohRepositoryInterface().retrieveAcquiredChannelEnvironmentIssuesByStationTimeRangeAndType(request);

        logger.info("Query Historical ACEI List (length = {}):", queryHistoricalACEIList.size());
        queryHistoricalACEIList.forEach(hacei -> {
            logger.info("{} : {} : {}", hacei.getChannelName(), hacei.getMonitorType(), hacei.getTrendLine());
        });

        Instant finish = Instant.now();
        long timeElapsed = Duration.between(start, finish).toMillis();

        logger.info("---- SQL Retrieve Historical ACEI Data: ----");
        logger.info("Elapsed time (ms): {}", timeElapsed);

        assertEquals(historicalACEIList.size(), queryHistoricalACEIList.size());
        assertEquals(historicalACEIList, queryHistoricalACEIList);
    }
}
