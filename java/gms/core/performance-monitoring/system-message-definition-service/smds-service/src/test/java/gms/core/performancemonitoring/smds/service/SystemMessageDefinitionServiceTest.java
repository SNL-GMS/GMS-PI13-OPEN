package gms.core.performancemonitoring.smds.service;

import gms.shared.frameworks.osd.coi.systemmessages.SystemMessageType;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
public class SystemMessageDefinitionServiceTest {

  @Test
  void testCreation() {
    SystemMessageDefinitionService smds = SystemMessageDefinitionService.create();
    assertNotNull(smds);
  }

  @Test
  void testServiceCallReturnsDefinitions() {
    SystemMessageDefinitionService smds = SystemMessageDefinitionService.create();
    String messageDefinitions = smds.getSystemMessageDefinitions("").toString();
    Arrays.stream(SystemMessageType.values())
        .forEach(smt -> {
          assertTrue(messageDefinitions.contains(smt.toString()));
        });
  }
}
