package gms.shared.frameworks.osd.api.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import gms.shared.frameworks.osd.coi.datatransferobjects.CoiObjectMapperFactory;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ChannelsTimeRangeRequestTest {

  @Test
  void testSerialization() throws IOException {
    ObjectMapper mapper = CoiObjectMapperFactory.getJsonObjectMapper();
    ChannelsTimeRangeRequest request = ChannelsTimeRangeRequest.create(
        List.of("ELK.ELK.BHZ"),
        Instant.parse("2019-09-24T19:00:00Z"),
        Instant.parse("2019-09-24T20:00:00Z"));
    String requestJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(request);
    ChannelsTimeRangeRequest rebuilt = mapper.readValue(requestJson, ChannelsTimeRangeRequest.class);
    assertEquals(request, rebuilt);
  }

}
