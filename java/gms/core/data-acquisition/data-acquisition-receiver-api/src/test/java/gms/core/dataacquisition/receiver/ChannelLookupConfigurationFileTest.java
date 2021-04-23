package gms.core.dataacquisition.receiver;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import gms.shared.frameworks.osd.coi.datatransferobjects.CoiObjectMapperFactory;
import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ChannelLookupConfigurationFileTest {

  @Test
  void testSerialization() throws IOException {
    ChannelLookupConfigurationFile expected = ChannelLookupConfigurationFile
        .from(Map.of("foo", "bar"));

    ObjectMapper objectMapper = CoiObjectMapperFactory.getJsonObjectMapper();

    assertEquals(expected, objectMapper.readValue(objectMapper.writeValueAsString(expected),
        ChannelLookupConfigurationFile.class));
  }
}