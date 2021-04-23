package gms.shared.frameworks.client.generation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import gms.shared.frameworks.client.ServiceClientJdkHttp;
import gms.shared.frameworks.client.ServiceRequest;
import gms.shared.frameworks.common.ContentType;
import gms.shared.frameworks.common.annotations.Component;
import gms.shared.frameworks.systemconfig.SystemConfig;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ClientGeneratorTests {

  private static final String NO_CONSUMES_OR_PRODUCES = "no-consumes-or-produces";
  private static final String CONSUMES_MSGPACK = "consumes-msgpack";
  private static final String PRODUCES_MSGPACK = "produces-msgpack";
  private static final String VOID_ROUTE = "void";
  private static final String COMPONENT_NAME = "the-component";
  private static final String BASE_PATH = "example";
  private final String HOSTNAME = "some-host";
  private final int PORT = 5555;
  private final Duration TIMEOUT = Duration.ofMillis(123);
  private final String s = "foo";
  private final Type typeOfS = s.getClass();
  private final String baseUrl = String.format("http://%s:%d", HOSTNAME, PORT);
  private ExampleApi api;
  @Mock
  private ServiceClientJdkHttp mockClient;
  @Mock
  private SystemConfig sysConfig;

  @BeforeEach
  void setup() throws Exception {
    doReturn(new URL(baseUrl)).when(sysConfig).getUrl();
    doReturn(TIMEOUT).when(sysConfig).getValueAsDuration(SystemConfig.CLIENT_TIMEOUT);
    api = ClientGenerator.createClient(ExampleApi.class, mockClient, sysConfig);
    assertNotNull(api);
  }

  @Component(COMPONENT_NAME)
  @Path(BASE_PATH)
  interface ExampleApi {

    @Path(NO_CONSUMES_OR_PRODUCES)
    @POST
    String noConsumesOrProduces(String s);

    @Path(CONSUMES_MSGPACK)
    @POST
    @Consumes({ContentType.MSGPACK_NAME})
    String consumesMsgpack(String s);

    @Path(PRODUCES_MSGPACK)
    @POST
    @Produces({ContentType.MSGPACK_NAME})
    String producesMsgpack(String s);

    @Path(VOID_ROUTE)
    @POST
    void voidRoute(String s);
  }

  @Test
  void testCall_noConsumesOrProduces() throws Exception {
    api.noConsumesOrProduces(s);
    verifyClientCall(NO_CONSUMES_OR_PRODUCES,
        ContentType.defaultContentType(), ContentType.defaultContentType());
  }

  @Test
  void testCall_consumesMsgpack() throws Exception {
    api.consumesMsgpack(s);
    verifyClientCall(CONSUMES_MSGPACK, ContentType.MSGPACK, ContentType.defaultContentType());
  }

  @Test
  void testCall_producesMsgpack() throws Exception {
    api.producesMsgpack(s);
    verifyClientCall(PRODUCES_MSGPACK, ContentType.defaultContentType(), ContentType.MSGPACK);
  }

  @Test
  void testCall_voidRoute() throws Exception {
    api.voidRoute(s);
    verify(mockClient).send(ServiceRequest.from(
        constructUrl(VOID_ROUTE), s, TIMEOUT, String.class,
        ContentType.defaultContentType(), ContentType.defaultContentType()));
  }

  private void verifyClientCall(String path, ContentType consumes, ContentType produces)
      throws Exception {
    verify(mockClient).send(ServiceRequest.from(
        constructUrl(path), s, TIMEOUT, typeOfS, consumes, produces));
  }

  private URL constructUrl(String path) throws MalformedURLException {
    return new URL(String.format("%s/%s/%s", baseUrl, BASE_PATH, path));
  }

  @Path(BASE_PATH)
  interface InterfaceWithoutComponentAnnotation {

    @Path(NO_CONSUMES_OR_PRODUCES)
    @POST
    String noConsumesOrProduces(String s);

    @Path(CONSUMES_MSGPACK)
    @POST
    String consumesMsgpack(String s);

    @Path(PRODUCES_MSGPACK)
    @POST
    String producesMsgpack(String s);
  }

  @Test
  void testCallWithoutComponentAnnotationThrows() {
    assertEquals("Client interface must have @Component",
        assertThrows(IllegalArgumentException.class,
            () -> ClientGenerator.createClient(InterfaceWithoutComponentAnnotation.class))
            .getMessage());
  }

  @Component(COMPONENT_NAME)
  @Path("")
  interface ExampleApiWithEmptyBasePath {

    @Path(NO_CONSUMES_OR_PRODUCES)
    @POST
    String noConsumesOrProduces(String s);

    @Path(CONSUMES_MSGPACK)
    @POST
    @Consumes({ContentType.MSGPACK_NAME})
    String consumesMsgpack(String s);

    @Path(PRODUCES_MSGPACK)
    @POST
    @Produces({ContentType.MSGPACK_NAME})
    String producesMsgpack(String s);
  }

  @Test
  void testCallWithEmptyBasePath() throws Exception {
    doReturn(new URL(baseUrl)).when(sysConfig).getUrl();
    doReturn(TIMEOUT).when(sysConfig).getValueAsDuration(SystemConfig.CLIENT_TIMEOUT);
    ExampleApiWithEmptyBasePath apiWithEmptyBasePath = ClientGenerator.createClient(
        ExampleApiWithEmptyBasePath.class, mockClient, sysConfig);
    assertNotNull(apiWithEmptyBasePath);
    apiWithEmptyBasePath.noConsumesOrProduces(s);
    final ContentType defaultCt = ContentType.defaultContentType();
    verify(mockClient).send(ServiceRequest.from(
        new URL(String.format("%s/%s", baseUrl, NO_CONSUMES_OR_PRODUCES)),
        s, TIMEOUT, typeOfS, defaultCt, defaultCt));
  }
}
