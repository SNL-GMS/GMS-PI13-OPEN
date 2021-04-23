package gms.integration.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.function.Supplier;

public class JsonSupplier implements Supplier<String> {

  private final ObjectMapper objectMapper;
  private final List<?> objects;
  private int index;

  public JsonSupplier(final ObjectMapper objectMapper, final List<?> objects) {
    this.objectMapper = objectMapper;
    this.objects = objects;
  }

  @Override
  public String get() {
    String s = null;
    if (index >= 0 && index < objects.size()) {
      try {
        s = objectMapper.writeValueAsString(objects.get(this.index++));
      } catch (JsonProcessingException e) {
        // Fall through and return null.
      }
    }
    return s;
  }
}
