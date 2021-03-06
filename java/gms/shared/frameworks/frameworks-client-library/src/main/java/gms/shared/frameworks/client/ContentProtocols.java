package gms.shared.frameworks.client;

import static gms.shared.frameworks.common.ContentType.JSON;
import static gms.shared.frameworks.common.ContentType.MSGPACK;

import com.fasterxml.jackson.databind.ObjectMapper;
import gms.shared.frameworks.common.ContentType;
import gms.shared.frameworks.osd.coi.datatransferobjects.CoiObjectMapperFactory;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.function.Function;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodyHandlers;

/**
 * Provides content protocols from ContentType's.
 */
class ContentProtocols {

  private ContentProtocols() {
  }

  private static final ContentProtocol<String> JSON_PROTOCOL = new Json();
  private static final ContentProtocol<byte[]> MSGPACK_PROTOCOL = new Msgpack();

  private static final Map<ContentType, ContentProtocol> typeToProtocol = Map.of(
      JSON, JSON_PROTOCOL,
      MSGPACK, MSGPACK_PROTOCOL);

  /**
   * Gets a ContentProtocol given a ContentType.
   *
   * @param contentType the content type
   * @param <T> the type param returned - unchecked, but marked here so callers don't have to
   * suppress.
   * @return a ContentProtocol that handles the specified content type
   * @throws IllegalArgumentException if there is no ContentProtocol implementation' for the
   * specified content type
   */
  @SuppressWarnings("unchecked")
  public static <T> ContentProtocol<T> from(ContentType contentType) {
    if (!typeToProtocol.containsKey(contentType)) {
      throw new IllegalArgumentException("Unknown content type: " + contentType);
    }
    return typeToProtocol.get(contentType);
  }

  private static final class Json implements ContentProtocol<String> {

    private final ObjectMapper mapper = CoiObjectMapperFactory.getJsonObjectMapper();

    @Override
    public Function<String, BodyPublisher> bodyEncoder() {
      return BodyPublishers::ofString;
    }

    @Override
    public String serialize(Object data) throws Exception {
      return mapper.writeValueAsString(data);
    }

    @Override
    public BodyHandler<String> bodyHandler() {
      return BodyHandlers.ofString();
    }

    @Override
    public <T> T deserialize(String data, Type type) throws Exception {
      return mapper.readValue(data, mapper.constructType(type));
    }
  }

  private static final class Msgpack implements ContentProtocol<byte[]> {

    private final ObjectMapper mapper = CoiObjectMapperFactory.getMsgpackObjectMapper();

    @Override
    public Function<byte[], BodyPublisher> bodyEncoder() {
      return BodyPublishers::ofByteArray;
    }

    @Override
    public byte[] serialize(Object data) throws Exception {
      return mapper.writeValueAsBytes(data);
    }

    @Override
    public BodyHandler<byte[]> bodyHandler() {
      return BodyHandlers.ofByteArray();
    }

    @Override
    public <T> T deserialize(byte[] data, Type type) throws Exception {
      return mapper.readValue(data, mapper.constructType(type));
    }
  }

}
