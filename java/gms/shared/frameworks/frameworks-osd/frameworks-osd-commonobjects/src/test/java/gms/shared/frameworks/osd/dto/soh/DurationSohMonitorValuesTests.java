package gms.shared.frameworks.osd.dto.soh;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.JsonProcessingException;
import gms.shared.frameworks.osd.coi.datatransferobjects.CoiObjectMapperFactory;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class DurationSohMonitorValuesTests {

  @Test
  public void testJsonSerDes() throws JsonProcessingException {
    DurationSohMonitorValues values = DurationSohMonitorValues
        .create(new long[]{Instant.now().toEpochMilli()});

    String json = CoiObjectMapperFactory.getJsonObjectMapper()
        .writeValueAsString(values);

    assertEquals(values, CoiObjectMapperFactory.getJsonObjectMapper()
        .readValue(json, SohMonitorValues.class));
  }
}