package gms.shared.frameworks.osd.coi.datatransferobjects;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Map;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

public class CoiSerde<T> implements Serde<T> {

  private ObjectMapper mapper;

  private Class<T> coiClass;

  public CoiSerde(Class<T> coiClass) {
    this.mapper = CoiObjectMapperFactory.getJsonObjectMapper();
    this.coiClass = coiClass;
  }

  @Override
  public void configure(Map<String, ?> configs, boolean isKey) {
    // nothing to do
  }

  @Override
  public void close() {
    // nothing to do
  }

  @Override
  public Serializer<T> serializer() {
    return new CoiSerializer();
  }

  @Override
  public Deserializer<T> deserializer() {
    return new CoiDeserializer();
  }

  private class CoiSerializer implements Serializer<T> {

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
      //Don't need to initialize due to use of ObjectMapper
    }

    @Override
    public byte[] serialize(String topic, T data) {
      try {
        return mapper.writeValueAsBytes(data);
      } catch (JsonProcessingException e) {
        throw new IllegalArgumentException(e);
      }
    }

    @Override
    public void close() {
      // nothing to do
    }
  }

  private class CoiDeserializer implements Deserializer<T> {

    @Override
    public void configure(final Map<String, ?> settings, final boolean isKey) {
      //Don't need to initialize due to use of ObjectMapper
    }

    @Override
    public T deserialize(String topic, byte[] data) {
      try {
        return mapper.readValue(data, coiClass);
      } catch (final IOException e) {
        throw new IllegalArgumentException(e);
      }
    }

    @Override
    public void close() {
      // nothing to do
    }
  }
}
