package gms.shared.frameworks.service;

import static java.lang.String.format;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gms.shared.frameworks.common.GmsCommonRoutes;
import gms.shared.frameworks.common.config.ServerConfig;
import io.netty.buffer.ByteBufAllocator;
import io.netty.handler.codec.http.HttpHeaderNames;
import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.netty.ByteBufFlux;
import reactor.netty.DisposableServer;
import reactor.netty.http.HttpProtocol;
import reactor.netty.http.HttpResources;
import reactor.netty.http.server.HttpServer;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

/**
 * Class for controlling a service. The main entry point of this library.
 */
public class HttpService {

  private static final Logger logger = LoggerFactory.getLogger(HttpService.class);
  public static final String TEXT_PLAIN = "text/plain";

  private boolean isRunning = false;

  private final ServiceDefinition definition;

  private DisposableServer nettyService;

  public HttpService(ServiceDefinition def) {
    this.definition = Objects.requireNonNull(def, "Cannot create HttpService will null definition");
    this.nettyService = null;
  }

  /**
   * Indicates whether the service is currently running.
   *
   * @return true if the service is running, false otherwise.
   */
  public boolean isRunning() {
    return this.isRunning;
  }

  /**
   * Returns the definition of this service.
   *
   * @return the definition
   */
  public ServiceDefinition getDefinition() {
    return this.definition;
  }

  /**
   * Starts the service.  If the service is already running (e.g. this method has been called
   * before), this call throws an exception.  This method configures the HTTP server (e.g. sets
   * port), registers service routes and exception handlers, and launches the service.
   */
  public void start() {
    // if service is running, throw an exception.
    if (isRunning) {
      throw new IllegalStateException("Service is already running");
    }
    logger.info("Starting service...");

    logger.info("Registering healthcheck route at {}", GmsCommonRoutes.HEALTHCHECK_PATH);
    logger.info("Registering upgrade route at {}", GmsCommonRoutes.CONNECTION_UPGRADE_PATH);
    // register routes
    // start the service
    logger.info("Starting the service...");
    this.nettyService = configureServer().route(routes -> {
      routes.get(GmsCommonRoutes.HEALTHCHECK_PATH,
          (req, res) -> res
              .header(HttpHeaderNames.CONTENT_TYPE, TEXT_PLAIN)
              .sendString(Mono.just("alive at " + Instant.now())));
      routes.get(GmsCommonRoutes.CONNECTION_UPGRADE_PATH,
          (req, res) -> res.header(HttpHeaderNames.CONTENT_TYPE, TEXT_PLAIN)
              .sendString(Mono.just("Connection Upgraded to HTTP/2 at " + Instant.now())));
      logger.info("Registering {} routes", this.definition.getRoutes().size());
      for (Route r : this.definition.getRoutes()) {
        logger.info("Registering route with path {}", r.getPath());
        routes.post(r.getPath(), nettyRoute(r.getHandler()));
      }
    }).bindNow();
    isRunning = true;
    logger.info("Service is now running on port {}",
        this.definition.getServerConfig().getPort());
    // Register a handler that stops the service if the JVM is shutting down
    Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
    // start waiting for requests
    (new Thread(() -> this.nettyService.onDispose().block())).start();
  }

  private void waitForRequests() {
    this.nettyService.onDispose().block();
  }

  /**
   * Stops the service.  If the service was not running, this call does nothing.
   */
  public void stop() {
    logger.info("Stopping the service...");
    logger.info("Awaiting the service to be stopped...");
    this.nettyService.disposeNow(Duration.ofSeconds(5));
    // clean up http resources
    HttpResources
        .disposeLoopsAndConnectionsLater(Duration.ofSeconds(10), Duration.ofSeconds(30))
        .block(Duration.ofSeconds(30));
    isRunning = false;
    logger.info("Service is stopped");
  }

  /**
   * Configures the HTTP server
   */
  private HttpServer configureServer() {
    final ServerConfig config = this.definition.getServerConfig();
    return HttpServer.create()
        .protocol(HttpProtocol.H2C, HttpProtocol.HTTP11)
        .port(config.getPort());
  }

  /**
   * Creates a {@link BiFunction} that represents a handler function that would, given a {@link
   * RequestHandler}
   *
   * @param handler the request handler
   * @return a Spark Route function that uses the provided RequestHandler and serialization objects
   */
  private BiFunction<HttpServerRequest, HttpServerResponse, Publisher<Void>> nettyRoute(
      RequestHandler<?> handler) {
    return (nettyRequest, nettyResponse) ->
        nettyRequest.receive()
            .aggregate()
            .asByteArray()
            .defaultIfEmpty(new ByteArrayOutputStream().toByteArray())
            .flatMap(buf -> handleRequest(nettyRequest, nettyResponse, handler, buf));
  }

  /**
   * Handles the given data, sent as a byte[], with the given {@link RequestHandler} object
   *
   * @param nettyRequest  - {@link HttpServerRequest} holding the header information that was
   *                      provided by the incoming request.
   * @param nettyResponse - {@link HttpServerResponse} created by the Netty server
   * @param handler       - {@link RequestHandler} holding the callback to handle the request with
   * @param buf           - data that was aggregated by netty before invoking the handler
   * @return a {@link Mono} object that writes out the serialized HTTP response to the wire.
   */
  private Mono<? extends Void> handleRequest(HttpServerRequest nettyRequest,
      HttpServerResponse nettyResponse, RequestHandler<?> handler, byte[] buf) {
    // wrap the Request
    logger.info("Handling request: {}", nettyRequest);
    final Request request = new NettyRequest(nettyRequest, buf);
    // get the proper deserializer for the Request
    final ObjectMapper deserializer =
        request.clientSentMsgpack() ? this.definition.getMsgpackMapper()
            : this.definition.getJsonMapper();
    // invoke the route handler
    final Response<?> routeResponse = invokeHandler(handler, request, deserializer);
    return writeOutResponse(nettyResponse, routeResponse,
        request.clientAcceptsMsgpack());
  }

  /**
   * This method wraps the appropriate {@link Response} object as a {@link Mono} that is returned
   * across the wire through a given {@link HttpServerResponse} object.
   *
   * @param nettyResponse - {@link HttpServerResponse} object to write the response to
   * @param routeResponse - {@link Response} object returned from our handler
   * @param acceptsMsgPack - true if we are writing msgpack, false otherwise.
   * @return a {@link Mono} object that writes out the serialized HTTP response to the wire.
   */
  private Mono<? extends Void> writeOutResponse(HttpServerResponse nettyResponse,
      Response<?> routeResponse, boolean acceptsMsgPack) {
    // appropriately set the attributes of the spark Response object
    nettyResponse.status(routeResponse.getHttpStatus().getStatusCode());
    nettyResponse.header(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
    // check if error message is set; return the error message as plain text if so.

    Optional<String> errorMessage = routeResponse.getErrorMessage();
    Optional<?> body = routeResponse.getBody();

    if (errorMessage.isPresent()) {
      return sendErrorResponse(nettyResponse, errorMessage.get());
    } else if (body.isPresent()) {
      return acceptsMsgPack ? sendMsgPackResponse(nettyResponse, body.get())
          : sendJsonResponse(nettyResponse, body.get());
    } else {
      throw new IllegalArgumentException(
          format("Invalid response, no error message or body present: %s", routeResponse));
    }
  }

  private Mono<? extends Void> sendErrorResponse(HttpServerResponse nettyResponse,
      String errorMessage) {
    nettyResponse.header(HttpHeaderNames.CONTENT_TYPE, TEXT_PLAIN);
    return nettyResponse
        .sendString(Mono.just(errorMessage))
        .then();
  }

  private Mono<? extends Void> sendMsgPackResponse(HttpServerResponse nettyResponse,
      Object responseBody) {

    ObjectMapper msgpackMapper = definition.getMsgpackMapper();
    try {
      final byte[] data = msgpackMapper
          .writeValueAsBytes(responseBody);
      nettyResponse.header(HttpHeaderNames.CONTENT_TYPE, "application/msgpack");
      nettyResponse
          .header(HttpHeaderNames.CONTENT_LENGTH, Integer.toString(data.length));
      return nettyResponse.sendObject(
          ByteBufFlux.just(ByteBufAllocator.DEFAULT.buffer().writeBytes(data)))
          .then();
    } catch (JsonProcessingException e) {
      return nettyResponse
          .sendObject(ByteBufFlux.just("Error sending MsgPack response", e.getMessage())).then();
    }
  }

  private Mono<? extends Void> sendJsonResponse(HttpServerResponse nettyResponse,
      Object responseBody) {
    ObjectMapper jsonMapper = definition.getJsonMapper();

    try {
      final String data = jsonMapper
          .writeValueAsString(responseBody);
      nettyResponse.header(HttpHeaderNames.CONTENT_TYPE, "application/json");
      nettyResponse
          .header(HttpHeaderNames.CONTENT_LENGTH, Integer.toString(data.length()));
      return nettyResponse.sendString(
          ByteBufFlux.just(data)).then();
    } catch (JsonProcessingException e) {
      return nettyResponse
          .sendObject(ByteBufFlux.just("Error sending MsgPack response", e.getMessage())).then();
    }
  }

  /**
   * Convenience function for calling a request handler on a request safely, returning either the
   * Response from the handler or a server error if the handler throws an exception
   *
   * @param handler the request handler
   * @param request the request
   * @param deserializer the deserializer
   * @return a {@link Response}; if the handler runs without throwing an exception this is just the
   * result of handler.handle(request, deserializer)... if it throws an exception, a server error
   * Response is returned.
   */
  private static Response<?> invokeHandler(RequestHandler<?> handler,
      Request request, ObjectMapper deserializer) {
    try {
      return handler.handle(request, deserializer);
    } catch (Exception ex) {
      logger.error("Route handler threw exception", ex);
      return Response.serverError(ex.getMessage());
    }
  }
}
