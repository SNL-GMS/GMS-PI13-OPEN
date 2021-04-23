package gms.integration.steps;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.dockerjava.api.model.Network;
import gms.integration.util.PostgresDbConnection;
import gms.integration.util.StepUtils;
import gms.shared.frameworks.osd.coi.datatransferobjects.CoiObjectMapperFactory;
import gms.shared.frameworks.osd.coi.soh.CapabilitySohRollup;
import gms.shared.frameworks.osd.coi.soh.SohStatus;
import gms.shared.frameworks.osd.coi.soh.StationSoh;
import gms.shared.frameworks.test.utils.services.GmsServiceType;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.io.File;
import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.UUID;
import javax.persistence.PersistenceException;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
public class CapabilitySohRollupOsdTestSteps {


  private static final Logger LOGGER = LoggerFactory.getLogger(CapabilitySohRollupOsdTestSteps.class);

  private static final String STATIONSOHPATH="gms/integration/data/station_soh_cap.json";
  ObjectMapper objectMapper = CoiObjectMapperFactory.getJsonObjectMapper();
  private static final  Set<UUID> stationSohUUIDs=new HashSet<>();
  private static UUID CAPABILITYUUIDGOOD;
  private static UUID CAPABILITYUUIDBAD;
  private static final String STATIONGROUP = "All_1";
  //private GenericContainer dataInjector;
  private Environment environment;
  //used to store how many messages have been received on each topic over the course of all tests
  //this is necessary to know the expected offset in the final assert
  //private static Map<String, Integer> totalNumMessageMap = new HashMap<>();

  public CapabilitySohRollupOsdTestSteps(Environment environment) {
    this.environment = environment;

  }

  @Given("Referenced station sohs are stored in the osd")
  public void storeStationSohs(){

    List<StationSoh> stationSohs=null;
    List<StationSoh> newStationSohs = new ArrayList<>();
    try {
      URL stationSohURL = StepUtils.class.getClassLoader().getResource(STATIONSOHPATH);
      stationSohs = objectMapper
          .readValue(stationSohURL, new TypeReference<>(){});
    }
    catch (Exception e){
      e.printStackTrace();
      Assert.fail("Retrieving station sohs from json file failed: " + e
          .getMessage());
    }

    for(StationSoh stationSoh : stationSohs){
      StationSoh ss = StationSoh.create(
          stationSoh.getTime(),
          stationSoh.getStationName(),
          stationSoh.getSohMonitorValueAndStatuses(),
          stationSoh.getSohStatusRollup(),
          stationSoh.getChannelSohs(),
          stationSoh.getAllStationAggregates()
      );
      newStationSohs.add(ss);
      stationSohUUIDs.add(ss.getId());
    }

    try{
      this.environment.getSohRepositoryInterface().storeStationSoh(newStationSohs);
    }catch (Exception e){
      Assert.fail("Storing station sohs from json file failed: " + e
          .getMessage());
    }
  }

  @And("Capability soh rollup with appropriate values in field stored in osd")
  public void storeCorrectCapabilitySohRollup(){

    CapabilitySohRollup capabilitySohRollup = createCapabilitySohRollup(CapabilityRollupType.VALID);

    try{
      this.environment.getSohRepositoryInterface()
          .storeCapabilitySohRollup(List.of(capabilitySohRollup));
    }catch (Exception e){
      Assert.fail("Storing capability soh rollup failed: " + e
          .getMessage());
    }
  }

  @Then("Querying for capability soh rollup using given station group returns the correct capability soh rollup")
  public void retrieveCorrectCapabilitySohRollup(){

    List<CapabilitySohRollup> retrievedCapabilitySohRollups= this.environment.getSohRepositoryInterface().retrieveCapabilitySohRollupByStationGroup(List.of(STATIONGROUP));

    System.out.println(retrievedCapabilitySohRollups);

    boolean capabilitySohRollupRetrieved=false;
    boolean wrongCapabilityRetrieved=false;

    for(CapabilitySohRollup capabilitySohRollup : retrievedCapabilitySohRollups){

      if(capabilitySohRollup.getId().equals(CAPABILITYUUIDGOOD)){
        capabilitySohRollupRetrieved=true;
      }
      else if(capabilitySohRollup.getId().equals(CAPABILITYUUIDBAD)){
        wrongCapabilityRetrieved=true;
      }
    }
    assertTrue(capabilitySohRollupRetrieved && !wrongCapabilityRetrieved);
  }

  @And("Attempted storage of valid capability soh rollup and capability soh rollup with non-existent {string}")
  public void storageWithBadCapabiltyRollup(String capabilityRollupType){

    List<CapabilitySohRollup> capabilitySohRollups = new LinkedList<>();
    capabilitySohRollups.add(createCapabilitySohRollup(CapabilityRollupType.VALID));
    capabilitySohRollups.add(createCapabilitySohRollup(CapabilityRollupType.valueOf(capabilityRollupType)));

    try{
      this.environment.getSohRepositoryInterface()
          .storeCapabilitySohRollup(capabilitySohRollups);
    }catch (Exception e){
      Assert.fail("Storing capability soh rollup failed: " + e
          .getMessage());
    }
  }

  private CapabilitySohRollup createCapabilitySohRollup(CapabilityRollupType capType){

    Map<String, SohStatus> capStationSohStatusMap= new HashMap<>();
    capStationSohStatusMap.put("AAK", SohStatus.BAD);
    capStationSohStatusMap.put("BOSA", SohStatus.BAD);
    Set<UUID> capStationSohUUIDS= new HashSet<>();
    capStationSohUUIDS.addAll(stationSohUUIDs);
    UUID capUUID = UUID.randomUUID();
    CAPABILITYUUIDBAD = capUUID;
    Instant capTime= Instant.parse("2020-05-27T18:20:50.440491Z");
    String capStationGroup =STATIONGROUP;
    SohStatus capRollupStatus=SohStatus.BAD;

    switch (capType){

      case  STATIONSOH:
        capStationSohUUIDS.add(UUID.randomUUID());
        break;
      case STATIONGROUP:
        capStationGroup="BAD";
        break;
      case STATION:
        capStationSohStatusMap.put("FAK", SohStatus.BAD);
        break;
      case VALID:
      default:
        CAPABILITYUUIDGOOD = capUUID;
        break;
    }

    CapabilitySohRollup capabilitySohRollup = CapabilitySohRollup.create(capUUID, capTime,
        capRollupStatus, capStationGroup, capStationSohUUIDS, capStationSohStatusMap);
    return capabilitySohRollup;
  }


  private enum CapabilityRollupType { VALID, STATIONSOH, STATIONGROUP, STATION }

}
