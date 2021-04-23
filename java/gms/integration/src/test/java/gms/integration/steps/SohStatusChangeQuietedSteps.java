package gms.integration.steps;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import gms.integration.util.StepUtils;
import gms.shared.frameworks.osd.coi.datatransferobjects.CoiObjectMapperFactory;
import gms.shared.frameworks.osd.coi.soh.quieting.QuietedSohStatusChange;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.junit.Assert;
import java.net.URL;
import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SohStatusChangeQuietedSteps {

  Instant retrievalTime=Instant.now();
  Instant endTime=retrievalTime.plusSeconds(300);


  private static final String SOHQUIETPATH= "gms/integration/data/quieted.json";
  private ObjectMapper objectMapper = CoiObjectMapperFactory.getJsonObjectMapper();
  private Environment environment;

  List<QuietedSohStatusChange> quietedSohStatusChanges;


  public SohStatusChangeQuietedSteps(Environment environment) {
    this.environment = environment;
  }


  @When("Quieted soh status change is stored in the osd")
  public void storeQuietedSohStatusChange(){

    try {
      URL quietedURL = StepUtils.class.getClassLoader().getResource(SOHQUIETPATH);
      quietedSohStatusChanges = objectMapper
          .readValue(quietedURL, new TypeReference<>(){});
    }
    catch (Exception e){
      e.printStackTrace();
      Assert.fail("Retrieving station sohs from json file failed: " + e
          .getMessage());
    }

    quietedSohStatusChanges=quietedSohStatusChanges.stream()
        .map(quietedSohStatusChange -> quietedSohStatusChange.toBuilder().setQuietUntil(endTime).build())
        .collect(Collectors.toList());

    try{
      this.environment.getSohRepositoryInterface().storeQuietedSohStatusChangeList(quietedSohStatusChanges);
    }catch (Exception e){
      Assert.fail("Storing Soh Status Change from json file failed: " + e
          .getMessage());
    }
  }

  @Then("Querying by quieted until time after a given instant returns the correct quieted soh status changes")
  public void retrieveCorrectQuietedSohStatusChange(){

      Collection<QuietedSohStatusChange> retrievedquietedSohStatusChanges=null;
      try {
        retrievedquietedSohStatusChanges = this.environment
            .getSohRepositoryInterface()
            .retrieveQuietedSohStatusChangesByTime(retrievalTime);
      }catch (Exception e){
        Assert.fail("Retrieving soh status change event from osd failed: " + e
            .getMessage());
      }
      if(retrievedquietedSohStatusChanges==null){
        Assert.fail("No quieted soh status changes returned");
      }

      Set<QuietedSohStatusChange> retrievedSet = new HashSet<>(retrievedquietedSohStatusChanges);

      if(quietedSohStatusChanges == null){
        Assert.fail("Quieted Soh Status Changes null. No quieted soh status changes"
            + "were initialized to be stored");
      }

      Set<QuietedSohStatusChange> initialSet = new HashSet<>(quietedSohStatusChanges);

    System.out.println(initialSet);
    System.out.println(retrievedSet);

      assertTrue(retrievedSet.equals(initialSet));

  }

  @And("Quieted soh status change is updated and stored in the osd")
  public void updateQuietedSohStatusChanges(){

    quietedSohStatusChanges = quietedSohStatusChanges.stream()
        .map(quietedSohStatusChange -> quietedSohStatusChange.toBuilder().setComment(Optional.of(quietedSohStatusChange.getComment()+" updated")).build())
        .collect(Collectors.toList());

    try{
      this.environment.getSohRepositoryInterface().storeQuietedSohStatusChangeList(quietedSohStatusChanges);
    }catch (Exception e){
      Assert.fail("Storing Soh Status Change from json file failed: " + e
          .getMessage());
    }

  }
}
