package gms.dataacquisition.css.processingconverter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class AridToWfidJsonReader {
  
  private AridToWfidJsonReader(){}

  // no special config required - just reading basic JSON.
  private static final ObjectMapper objMapper = new ObjectMapper();

  public static Map<Integer, Long> read(String path) throws IOException {
    final JsonNode jsonRoot = objMapper.readTree(new File(path));
    Objects.requireNonNull(jsonRoot, "Found no JSON object in file " + path);
    final Map<Integer, Long> result = new HashMap<>();
    for (int i = 0; i < jsonRoot.size(); i++) {
      final JsonNode entry = jsonRoot.get(i);
      Objects.requireNonNull(entry, "Found null entry at index " + i);
      final int arid = entry.get("Arid").asInt();
      final long wfid = entry.get("Wfid").asLong();
      result.put(arid, wfid);
    }
    return result;
  }

}
