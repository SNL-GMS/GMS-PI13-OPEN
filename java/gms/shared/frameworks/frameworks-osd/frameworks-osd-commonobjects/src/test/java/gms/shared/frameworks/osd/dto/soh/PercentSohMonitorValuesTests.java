package gms.shared.frameworks.osd.dto.soh;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.JsonProcessingException;
import gms.shared.frameworks.osd.coi.datatransferobjects.CoiObjectMapperFactory;
import org.junit.jupiter.api.Test;

class PercentSohMonitorValuesTests {

  @Test
  public void testJsonSerDes() throws JsonProcessingException {
    PercentSohMonitorValues values = PercentSohMonitorValues.create(new double[]{1.0});

    String json = CoiObjectMapperFactory.getJsonObjectMapper()
        .writeValueAsString(values);

    assertEquals(values, CoiObjectMapperFactory.getJsonObjectMapper()
        .readValue(json, SohMonitorValues.class));
  }
}