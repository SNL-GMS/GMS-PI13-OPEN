package gms.integration.steps;

import com.github.dockerjava.api.model.Network;
import gms.integration.util.StepUtils;
import gms.shared.frameworks.osd.coi.datatransferobjects.CoiObjectMapperFactory;
import gms.shared.frameworks.osd.coi.soh.SohMonitorType;
import gms.shared.frameworks.osd.coi.soh.quieting.SohStatusChange;
import gms.shared.frameworks.osd.coi.soh.quieting.UnacknowledgedSohStatusChange;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.net.URL;
import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
public class SohStatusChangeTestSteps {


  private static final Logger LOGGER = LoggerFactory.getLogger(SohStatusChangeTestSteps.class);

  private static final String SOHSTATUSCHANGEPATH= "gms/integration/data/status_change.json";
  private ObjectMapper objectMapper = CoiObjectMapperFactory.getJsonObjectMapper();
  private String statusChangeStation;
  private String statusChangeChannel;
  private UnacknowledgedSohStatusChange unacknowledgedSohStatusChange =null;
  private SohStatusChange sohStatusChange=null;


  private Network deploymentNetwork;
  private Environment environment;

  public SohStatusChangeTestSteps(Environment environment) {
    this.environment = environment;
  }


  @When("Soh Status Change is stored in the osd")
  public void storeSohStatusChange(){


    try {
      URL stationSohURL = StepUtils.class.getClassLoader().getResource(SOHSTATUSCHANGEPATH);
      unacknowledgedSohStatusChange = objectMapper
          .readValue(stationSohURL, UnacknowledgedSohStatusChange.class);
    }
    catch (Exception e){
      e.printStackTrace();
      Assert.fail("Retrieving Soh Status Change from json file failed: " + e
          .getMessage());
    }

    statusChangeStation= unacknowledgedSohStatusChange.getStation();
    statusChangeChannel= unacknowledgedSohStatusChange.getSohStatusChanges().iterator().next().getChangedChannel();

    try{
      this.environment.getSohRepositoryInterface().storeUnacknowledgedSohStatusChange(List.of(unacknowledgedSohStatusChange));
    }catch (Exception e){
      Assert.fail("Storing Soh Status Change from json file failed: " + e
          .getMessage());
    }
  }

  @Then("Querying for soh status change by station returns the soh status change")
  public void retrieveCorrectSohStatusChange(){

    List<UnacknowledgedSohStatusChange> retrievedUnacknowledgedSohStatusChanges =null;
    try {
      retrievedUnacknowledgedSohStatusChanges = this.environment
          .getSohRepositoryInterface()
          .retrieveUnacknowledgedSohStatusChanges(List.of(statusChangeStation));
    }catch (Exception e){
      Assert.fail("Retrieving soh status change event from osd failed: " + e
          .getMessage());
    }

    UnacknowledgedSohStatusChange retrievedUnacknowledgedSohStatusChange = retrievedUnacknowledgedSohStatusChanges.get(0);
    Collection<SohStatusChange> retrievedSohStatusChanges= retrievedUnacknowledgedSohStatusChange.getSohStatusChanges();
    Collection<SohStatusChange> initialSohStatusChanges= unacknowledgedSohStatusChange.getSohStatusChanges();
    Set<SohStatusChange> retrievedSohStatusChangesSet= new HashSet<>(retrievedSohStatusChanges);
    Set<SohStatusChange> initialSohStatusChangesSet= new HashSet<>(initialSohStatusChanges);
    assertTrue(retrievedSohStatusChangesSet.equals(initialSohStatusChangesSet));


  }

  @And("New soh status change with same station as previous soh status change is stored in osd")
  public void storeNewSohStatusChange(){

    sohStatusChange = SohStatusChange
        .from(Instant.now(), SohMonitorType.ENV_CALIBRATION_UNDERWAY,statusChangeChannel);

    UnacknowledgedSohStatusChange newUnacknowledgedSohStatusChange = UnacknowledgedSohStatusChange
        .from(statusChangeStation, Set.of(sohStatusChange));

    try{
      this.environment.getSohRepositoryInterface().storeUnacknowledgedSohStatusChange(List.of(newUnacknowledgedSohStatusChange));
    }catch (Exception e){
      Assert.fail("Storing new Soh Status Change from json file failed: " + e
          .getMessage());
    }
  }

  @Then("New soh status change is retrieved when queried by station")
  public void retrieveNewSohStatusChange(){
    List<UnacknowledgedSohStatusChange> retrievedUnacknowledgedSohStatusChanges =null;
    try {
      retrievedUnacknowledgedSohStatusChanges = this.environment
          .getSohRepositoryInterface()
          .retrieveUnacknowledgedSohStatusChanges(List.of(statusChangeStation));
    }catch (Exception e){
      Assert.fail("Retrieving new soh status change event from osd failed: " + e
          .getMessage());
    }

    UnacknowledgedSohStatusChange retrievedUnacknowledgedSohStatusChange = retrievedUnacknowledgedSohStatusChanges.get(0);
    SohStatusChange retrievedSohStatusChange= retrievedUnacknowledgedSohStatusChange.getSohStatusChanges().iterator().next();
    assertTrue(retrievedSohStatusChange.equals(sohStatusChange));
  }

}
