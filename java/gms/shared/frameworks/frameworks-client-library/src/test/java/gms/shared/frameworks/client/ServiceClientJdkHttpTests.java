package gms.shared.frameworks.client;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.binaryEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.matching.ContentPattern;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import gms.shared.frameworks.client.ServiceClientJdkHttp.BadRequest;
import gms.shared.frameworks.client.ServiceClientJdkHttp.ConnectionFailed;
import gms.shared.frameworks.client.ServiceClientJdkHttp.InternalServerError;
import gms.shared.frameworks.common.ContentType;
import gms.shared.frameworks.osd.coi.datatransferobjects.CoiObjectMapperFactory;
import java.net.URL;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ServiceClientJdkHttpTests {

  private static final ObjectMapper jsonMapper = CoiObjectMapperFactory.getJsonObjectMapper();
  private static final ObjectMapper msgpackMapper = CoiObjectMapperFactory.getMsgpackObjectMapper();

  private static final String
      CLIENT_ERROR_PATH = "/client-error", CLIENT_ERROR_MSG = "client error!",
      SERVER_ERROR_PATH = "/server-error", SERVER_ERROR_MSG = "server error!";
  private static final Duration timeout = Duration.ofSeconds(5);
  private WireMockServer wireMockServer;
  private int port;
  private String baseUrl;
  private final ServiceClientJdkHttp basicClient = ServiceClientJdkHttp.create();

  private static final JavaType someObjectOptionalStringType;

  static {
    final TypeFactory typeFactory = jsonMapper.getTypeFactory();
    someObjectOptionalStringType = typeFactory.constructParametricType(
        SomeObject.class, typeFactory.constructParametricType(Optional.class, String.class));
  }


  @BeforeEach
  void setUp() {
    wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
    wireMockServer.start();
    port = wireMockServer.port();
    baseUrl = "http://localhost:" + port;
    mockServerError(CLIENT_ERROR_PATH, 400, CLIENT_ERROR_MSG);
    mockServerError(SERVER_ERROR_PATH, 500, SERVER_ERROR_MSG);
  }

  @AfterEach
  void tearDown() {
    wireMockServer.stop();
  }

  @Test
  void testCreateNoArgs() {
    assertNotNull(ServiceClientJdkHttp.create());
  }

  @ParameterizedTest
  @EnumSource(ContentType.class)
  void testRequestCustomObject(ContentType requestFormat) throws Exception {
    final String path = "/echo"; // server echoes the object back to you
    final SomeObject<Optional<String>> obj
        = SomeObject.create(5, "bar", Optional.of("foo"));
    for (ContentType responseFormat : ContentType.values()) {
      final StubMapping stub = mockServerSuccess(path, obj, obj,
          requestFormat, responseFormat);
      wireMockServer.addStubMapping(stub);
      final SomeObject<Optional<String>> returnObj = basicClient.sendSingleRequest(
          ServiceRequest.from(new URL(baseUrl + path), obj, timeout,
          someObjectOptionalStringType, requestFormat, responseFormat));
      assertEquals(obj, returnObj);
      // remove stub so the next iteration can't use it
      wireMockServer.removeStub(stub);
    }
  }

  @Test
  void testServerReturnsStatus4xxThrowsBadRequestWithBodyAsMsg() {
    assertRequestThrows(CLIENT_ERROR_PATH, BadRequest.class, CLIENT_ERROR_MSG);
  }

  @Test
  void testServerReturnsStatus5xxThrowsInternalServerErrorWithBodyAsMsg() {
    assertRequestThrows(SERVER_ERROR_PATH, InternalServerError.class, SERVER_ERROR_MSG);
  }

  private static StubMapping mockServerSuccess(String path, Object request, Object response,
      ContentType requestFormat, ContentType responseFormat)
      throws Exception {
    // taking the easy way out and doing everything as if there's only two ContentType's in this test
    final boolean requestMsgpack = requestFormat.equals(ContentType.MSGPACK);
    final boolean responseMsgpack = responseFormat.equals(ContentType.MSGPACK);
    final ContentPattern pat = requestMsgpack ?
        binaryEqualTo(msgpackMapper.writeValueAsBytes(request))
        : equalTo(jsonMapper.writeValueAsString(request));
    MappingBuilder bob = accept(contentType(post(urlEqualTo(path)), requestFormat), responseFormat)
        .withRequestBody(pat);
    bob = responseMsgpack ?
        bob.willReturn(ok().withBody(msgpackMapper.writeValueAsBytes(response)))
        : bob.willReturn(ok().withBody(jsonMapper.writeValueAsString(response)));
    return bob.build();
  }

  private static MappingBuilder accept(MappingBuilder mb, ContentType format) {
    return mb.withHeader("Accept", containing(format.toString()));
  }

  private static MappingBuilder contentType(MappingBuilder mb, ContentType format) {
    return mb.withHeader("Content-Type", containing(format.toString()));
  }

  private void mockServerError(String url, int status, String responseMsg) {
    wireMockServer.givenThat(post(urlEqualTo(url))
        .willReturn(aResponse().withStatus(status).withBody(responseMsg)));
  }

  private <T extends RuntimeException> void assertRequestThrows(
      String path, Class<T> exceptionClass, String msg) {

    assertEquals(msg, assertThrows(exceptionClass,
        () -> basicClient.sendSingleRequest(ServiceRequest.builder(
            new URL(baseUrl + path), "foo", timeout, constructType(String.class))
            .build()))
        .getMessage());
  }

  private static JavaType constructType(Class<?> clazz) {
    return jsonMapper.constructType(clazz);
  }
}
