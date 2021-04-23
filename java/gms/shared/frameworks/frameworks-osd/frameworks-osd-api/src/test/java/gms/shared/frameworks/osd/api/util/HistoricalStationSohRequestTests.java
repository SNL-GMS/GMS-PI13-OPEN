package gms.shared.frameworks.osd.api.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import gms.shared.frameworks.osd.coi.datatransferobjects.CoiObjectMapperFactory;
import gms.shared.frameworks.osd.coi.soh.SohMonitorType;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class HistoricalStationSohRequestTests {

  @Test
  void testSerialization() throws IOException {
    HistoricalStationSohRequest request = HistoricalStationSohRequest.create("TEST",
        Instant.EPOCH.toEpochMilli(), Instant.EPOCH.plusSeconds(5).toEpochMilli(),
        List.of(SohMonitorType.LAG, SohMonitorType.MISSING));
    ObjectMapper mapper = CoiObjectMapperFactory.getJsonObjectMapper();
    assertEquals(request,
        mapper.readValue(mapper.writeValueAsString(request), HistoricalStationSohRequest.class));
  }
}