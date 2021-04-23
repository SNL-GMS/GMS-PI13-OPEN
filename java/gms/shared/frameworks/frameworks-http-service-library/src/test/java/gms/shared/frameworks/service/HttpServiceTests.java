package gms.shared.frameworks.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gms.shared.frameworks.common.GmsCommonRoutes;
import gms.shared.frameworks.common.HttpStatus;
import gms.shared.frameworks.osd.coi.datatransferobjects.CoiObjectMapperFactory;
import gms.shared.frameworks.common.HttpStatus.Code;
import gms.shared.frameworks.common.config.ServerConfig;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class HttpServiceTests {

  private static final Set<Route> routes = Set.of(
      // returns the body of the request unchanged.
      // Purposely leave out leading '/' to see that it gets setup correctly anyway.
      Route.create("echoBody",
          (req, deserializer) -> Response.success(req.getBody())),
      // returns the keys from body of the request as an array
      Route.create("/echoKeys", HttpServiceTests::echoKeys),
      // returns the path param 'foo', and if not present sets status to 400
      Route.create("/echoPathParam/{foo}", HttpServiceTests::echoPathParam),
      // returns an error
      Route.create("/error", (req, deserializer) -> Response.error(Code.BAD_GATEWAY, "error")),
      // a route that just always throws an exception
      Route.create("/throw", (req, deser) -> {
        throw new RuntimeException("psyche!");
      })
  );

  private static final ObjectMapper jsonMapper = CoiObjectMapperFactory.getJsonObjectMapper();
  private static final ObjectMapper msgpackMapper = CoiObjectMapperFactory.getMsgpackObjectMapper();
  private static final HttpClient client = HttpClient.newBuilder()
      .executor(Executors.newFixedThreadPool(10))
      .priority(1)
      .build();

  private static int servicePort;

  private static HttpService service;

  @BeforeAll
  static void setup() throws Exception {
    servicePort = getAvailablePort();
    final ServiceDefinition def = ServiceDefinition.builder(
        ServerConfig.from(servicePort, 10, 20, Duration.ofMillis(100)))
        .setRoutes(routes).build();
    assertServiceIsUnreachable();
    service = new HttpService(def);
    service.start();
    assertTrue(service.isRunning());
    assertEquals(def, service.getDefinition());
    upgradeToHttp2();
  }

  @AfterAll
  static void teardown() throws Exception {
    service.stop();
    assertFalse(service.isRunning());
    // call stop again to check that doesn't throw an exception
    service.stop();
  }

  @Test
  void testServiceStartAgain() {
    assertEquals("Service is already running",
        assertThrows(IllegalStateException.class,
            () -> service.start()).getMessage());
  }

  @Test
  void testConstructorNullCheck() {
    assertEquals("Cannot create HttpService will null definition",
        assertThrows(NullPointerException.class,
            () -> new HttpService(null)).getMessage());
  }

  @Test
  void testServiceRequests() throws Exception {
    testEchoBodyRoute();
    testEchoPathParamsRoute();
    testEchoKeysRouteJson();
    testEchoKeysRouteMsgpack();
    testErrorRoute();
  }

  @Test
  void testEchoBodyRoute() throws Exception {
    // test 'echo body' route with JSON
    final String body = "a body";
    HttpResponse<String> response = requestEchoBodyRoute(body, false);
    assertNotNull(response.body());
    assertEquals(HttpStatus.OK_200, response.statusCode());
    assertTrue(response.headers().firstValue("Content-Type").isPresent());
    assertEquals("application/json", response.headers().firstValue("Content-Type").get());
    // response body is wrapped in quotes because of how JSON serialization works
    assertEquals("\"" + body + "\"", response.body());

    var request = HttpRequest
        .newBuilder(URI.create("http://localhost:" + servicePort + "/echoBody"))
        .header("Content-Type", "application/json")
        .header("Accept", "application/msgpack")
        .POST(BodyPublishers.ofString(body))
        .build();
    var msgResponse = client.send(request, BodyHandlers.ofByteArray());
    assertNotNull(msgResponse.body());
    assertEquals(HttpStatus.OK_200, msgResponse.statusCode());
    assertTrue(msgResponse.headers().firstValue("Content-Type").isPresent());
    assertEquals("application/msgpack", msgResponse.headers().firstValue("Content-Type").get());
    final byte[] expected = msgpackMapper.writeValueAsBytes(body);
    assertArrayEquals(expected, msgResponse.body());
  }

  @Test
  void testEchoPathParamsRoute() throws Exception {
    final String param = "foo";
    final HttpResponse<String> response = requestEchoPathParamsRoute(param);
    assertNotNull(response.body());
    assertEquals(HttpStatus.OK_200, response.statusCode());
    assertEquals("\"" + param + "\"", response.body());
  }

  @Test
  void testEchoKeysRouteJson() throws Exception {
    final Map<String, String> m = Map.of("key1", "val1", "key2", "val2");
    final HttpResponse<String> response = requestEchoKeysRoute(jsonMapper.writeValueAsString(m));
    assertNotNull(response.body());
    assertEquals(HttpStatus.OK_200, response.statusCode());
    final String[] responseKeys = jsonMapper.readValue(response.body(), String[].class);
    assertEquals(Set.of("key1", "key2"), new HashSet<>(Arrays.asList(responseKeys)));
  }

  @Test
  void testEchoKeysRouteMsgpack() throws Exception {
    final Map<String, String> m = Map.of("key1", "val1", "key2", "val2");
    final HttpResponse<String> response = requestEchoKeysRouteMsgpack(
        msgpackMapper.writeValueAsBytes(m));
    assertNotNull(response.body());
    assertEquals(HttpStatus.OK_200, response.statusCode());
    final String[] responseKeys = jsonMapper.readValue(response.body(), String[].class);
    assertEquals(Set.of("key1", "key2"), new HashSet<>(Arrays.asList(responseKeys)));
  }

  @Test
  void testErrorRoute() throws Exception {
    var response = HttpClient.newHttpClient()
        .send(HttpRequest.newBuilder(URI.create("http://localhost:" + servicePort
            + "/error")).POST(BodyPublishers.noBody()).build(), BodyHandlers.ofString());

    assertNotNull(response.body());
    assertEquals(Code.BAD_GATEWAY.getStatusCode(), response.statusCode());
    assertEquals("error", response.body());
    assertTrue(response.headers().firstValue("Content-Type").isPresent());
    assertEquals("text/plain", response.headers().firstValue("Content-Type").get());
  }

  @Test
  void testThrowRoute() throws Exception {
    final HttpResponse<String> response = requestThrowRoute();
    assertEquals(Code.INTERNAL_SERVER_ERROR.getStatusCode(), response.statusCode());
    assertTrue(response.body().contains("psyche"),
        "Expected response to contain error message ('psyche')");
  }

  @Test
  void testServiceMakesHealthcheckRoute() throws Exception {
    final var request = HttpRequest.newBuilder(
        URI.create("http://localhost:" + servicePort + GmsCommonRoutes.HEALTHCHECK_PATH))
        .GET()
        .build();
    final HttpResponse<String> response = HttpClient.newHttpClient()
        .send(request, BodyHandlers.ofString());
    assertEquals(Code.OK.getStatusCode(), response.statusCode());
    assertTrue(response.body().contains("alive at"));
  }

  private static void assertServiceIsUnreachable() throws Exception {
    try {
      var response = requestEchoBodyRoute("foo", false);
//      fail("Expected to throw exception by trying to connect to "
//          + "service when it's supposed to be unreachable");
      assertTrue(response.body().isEmpty());
    } catch (Exception ex) {
      // do nothing; expected to throw a unirest exception
    }
  }

  private static HttpResponse<String> requestEchoBodyRoute(String body,
      boolean msgpack)
      throws Exception {
    return client.send(HttpRequest
        .newBuilder(URI.create("http://localhost:" + servicePort + "/echoBody"))
        .header("Content-Type", "application/json")
        .header("Accept", msgpack ? "application/msgpack" : "application/json")
        .POST(BodyPublishers.ofString(body))
        .build(), BodyHandlers.ofString());
  }

  private static HttpResponse<String> requestEchoKeysRoute(String body) throws Exception {
    return client.send(HttpRequest
        .newBuilder(URI.create("http://localhost:" + servicePort + "/echoKeys"))
        .headers("Content-Type", "application/json")
        .POST(BodyPublishers.ofString(body))
        .build(), BodyHandlers.ofString());
  }

  private static HttpResponse<String> requestEchoKeysRouteMsgpack(byte[] body) throws Exception {
    return client
        .send(HttpRequest.newBuilder(URI.create("http://localhost:" + servicePort + "/echoKeys"))
                .header("Content-Type", "application/msgpack")
                .POST(BodyPublishers.ofByteArray(body, 0, body.length))
                .build()
            , BodyHandlers.ofString());
  }

  private static HttpResponse<String> requestEchoPathParamsRoute(String param) throws Exception {
    return client.send(HttpRequest
            .newBuilder(URI.create("http://localhost:" + servicePort + "/echoPathParam/" + param))
            .POST(BodyPublishers.noBody())
            .build()
        , BodyHandlers.ofString());
  }

  private static HttpResponse<String> requestThrowRoute() throws Exception {
    return client.send(HttpRequest.newBuilder(
        URI.create("http://localhost:" + servicePort + "/throw"))
            .POST(BodyPublishers.noBody())
            .build()
        , BodyHandlers.ofString());
  }

  private static Response echoKeys(Request request, ObjectMapper deserializer) {
    try {
      // exercise the functionality to get headers
      Map<String, String> headers = request.getHeaders();
      assertNotNull(headers);
      // assert there are at least some headers;
      // hard to rely on specific ones because it depends on the request
      assertFalse(headers.isEmpty());
      JsonNode json = deserializer.readTree(request.getRawBody());
      List<String> keys = new ArrayList<>();
      json.fieldNames().forEachRemaining(keys::add);
      return Response.success(keys);
    } catch (IOException e) {
      return Response.clientError("Exception on deserialization: " + e.getMessage());
    }
  }

  private static Response echoPathParam(Request request, ObjectMapper deserializer) {
    return request.getPathParam("foo").isPresent() ?
        Response.success(request.getPathParam("foo"))
        : Response.clientError("path parameter foo must be specified");
  }

  private static int getAvailablePort() throws Exception {
    ServerSocket ephemeralSocket = new ServerSocket(0);
    final int port = ephemeralSocket.getLocalPort();
    ephemeralSocket.close();
    return port;
  }

  private static void upgradeToHttp2() {
    try {
      client.send(HttpRequest.newBuilder(
          URI.create("http://localhost:" + servicePort + "/alive"))
          .GET()
          .build(), BodyHandlers.ofString());
    } catch (IOException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }
}
