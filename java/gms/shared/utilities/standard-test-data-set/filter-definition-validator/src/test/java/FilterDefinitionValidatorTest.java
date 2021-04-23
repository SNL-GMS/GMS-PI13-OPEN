import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import gms.shared.frameworks.osd.coi.signaldetection.FilterDefinition;
import gms.shared.utilities.standardtestdataset.filterdefinitionvalidator.FilterDefinitionValidator;
import java.util.List;
import org.junit.jupiter.api.Test;

public class FilterDefinitionValidatorTest {
  
  /**
   * Reads a valid Filter Definition file, should be able to deserialize
   * 
   */
  @Test
  public void testValidation() {
    final FilterDefinitionValidator filterDefinitionValidator;
    filterDefinitionValidator = new FilterDefinitionValidator("src/test/resources/testFilterDefinitions.json");
    List<FilterDefinition> validatedFilterDefinitions = filterDefinitionValidator.getConvertedFilterDefinitions();
    assertNotNull(validatedFilterDefinitions);
    assertEquals(4, validatedFilterDefinitions.size());
  }
}
