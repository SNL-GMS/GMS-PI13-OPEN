package gms.shared.frameworks.configuration.util;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import gms.shared.frameworks.osd.coi.datatransferobjects.CoiObjectMapperFactory;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility operations for Configuration object serialization
 */
public class ObjectSerialization {

  // TODO: replace with the framework's FieldMapUtilities after updating config framework to new COI

  private static final JavaType mapType = TypeFactory.defaultInstance()
      .constructMapType(HashMap.class, String.class, Object.class);

  private static final ObjectMapper objectMapper = CoiObjectMapperFactory.getYamlObjectMapper();

  private ObjectSerialization() {
  }

  public static ObjectMapper getObjectMapper() {
    return objectMapper;
  }

  public static Map<String, Object> toFieldMap(Object o) {
    return objectMapper.convertValue(o, mapType);
  }

  public static <T> T fromFieldMap(Map<String, Object> map, Class<T> clazz) {
    return objectMapper.convertValue(map, clazz);
  }
}
