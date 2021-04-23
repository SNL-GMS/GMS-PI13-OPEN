package gms.shared.frameworks.osd.coi.datatransferobjects;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature;
import gms.shared.frameworks.osd.coi.event.Event;
import org.msgpack.jackson.dataformat.MessagePackFactory;

/**
 * Factory for getting properly-configured ObjectMapper's for use by most of the code.
 */
public final class CoiObjectMapperFactory {

  private CoiObjectMapperFactory() {
  }

  /**
   * Gets an ObjectMapper for use in JSON serialization. This ObjectMapper can serialize/deserialize
   * any COI object, and has common modules registered such as for Java 8 Instant.
   *
   * @return an ObjectMapper for use with JSON
   */
  public static ObjectMapper getJsonObjectMapper() {
    return configureObjectMapper(new ObjectMapper());
  }

  /**
   * Gets an ObjectMapper for use in msgpack serialization. This ObjectMapper can
   * serialize/deserialize any COI object, and has common modules registered such as for Java 8
   * Instant.
   *
   * @return an ObjectMapper for use with msgpack
   */
  public static ObjectMapper getMsgpackObjectMapper() {
    return configureObjectMapper(new ObjectMapper(new MessagePackFactory()));
  }

  /**
   * Gets an ObjectMapper for use in YAML serialization. This ObjectMapper can serialize/deserialize
   * any COI object, and has common modules registered such as for Java 8 Instant.
   *
   * @return an ObjectMapper for use with YAML
   */
  public static ObjectMapper getYamlObjectMapper() {
    // Disabling WRITE_DOC_START_MARKER and USE_NATIVE_TYPE_ID features prevents Jackson from
    // outputting lines line
    //   ---!<TYPE_NAME>
    // when serializing classes annotated with @JsonSubtypes.  These lines caused the
    // deserializer to fail.  The WRITE_DOC_START_MARKER is "---" and the USE_NATIVE_TYPE_ID is
    // the "!<...>" statement. Disabling only USE_NATIVE_TYPE_ID results in deserializable YAML, so
    // investigate leaving WRITE_DOC_START_MARKER enabled if disabling it leads to other issues.
    return configureObjectMapper(new ObjectMapper(
        new YAMLFactory()
            .disable(Feature.WRITE_DOC_START_MARKER)
            .disable(Feature.USE_NATIVE_TYPE_ID))
    );
  }

  private static ObjectMapper configureObjectMapper(ObjectMapper objMapper) {
    return registerEventSerializationModule(objMapper.findAndRegisterModules()
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .disable(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS)
        .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));
  }

  private static ObjectMapper registerEventSerializationModule(ObjectMapper objMapper) {
    // Register the Event serialization module.
    final SimpleModule eventSerializationModule = new SimpleModule();
    eventSerializationModule.addSerializer(Event.class, EventDtoConverter.SERIALIZER);
    eventSerializationModule.addDeserializer(Event.class, EventDtoConverter.DESERIALIZER);
    return objMapper.registerModule(eventSerializationModule);
  }
}
