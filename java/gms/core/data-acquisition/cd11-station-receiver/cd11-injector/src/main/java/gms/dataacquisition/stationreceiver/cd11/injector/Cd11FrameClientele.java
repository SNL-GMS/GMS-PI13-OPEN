package gms.dataacquisition.stationreceiver.cd11.injector;

import com.google.common.net.InetAddresses;
import gms.dataacquisition.stationreceiver.cd11.common.configuration.Cd11SocketConfig;
import io.vertx.core.net.NetClientOptions;
import io.vertx.reactivex.core.Future;
import io.vertx.reactivex.core.Promise;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.net.NetClient;
import io.vertx.reactivex.core.net.NetSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cd11FrameClientele represents the core functionality of interfacing with Connman and Dataman
 * through establishing connections and creating {@link Cd11FrameClient}s
 */
class Cd11FrameClientele {

  //What are the IP/Port of this connection request (unsure if either matters and/or checked by connman)
  private static final String CONN_REQUEST_ORIGIN_IP = "127.0.0.1";
  private static final int CONN_REQUEST_ORIGIN_PORT = 32864;
  public static final int CONNMAN_RESET_TIMEOUT_MS = 10000;

  private final Vertx vertx;

  private static final Logger logger = LoggerFactory.getLogger(Cd11FrameClientele.class);
  private final Cd11SocketConfig.Builder socketConfigBuilder;
  private final NetClient netClient;

  /**
   * Constructor
   *
   * @param frameCreator as per CD1.1 spec, the identifier for this frame creator (such as IDC)
   * @param frameDestination as per CD1.1 spec, the frame destination string identifier
   * @param vertx vertx context for network connections/async
   */
  Cd11FrameClientele(String frameCreator, String frameDestination, Vertx vertx) {
    this.vertx = vertx;
    socketConfigBuilder = Cd11SocketConfig.builderWithDefaults()
        .setFrameCreator(frameCreator)
        .setFrameDestination(frameDestination);

    // Set socket options
    NetClientOptions options = new NetClientOptions()
        .setReconnectAttempts(10)
        .setReconnectInterval(1000)
        .setConnectTimeout(CONNMAN_RESET_TIMEOUT_MS);
    // Create the client with an inline handler
    netClient = vertx.createNetClient(options);
  }

  /**
   * Establishes a CD1.1 Connection Manager
   *
   * @param address Address to connect to
   * @param port Port to connect on
   * @param stationId Id/name of the station we are establishing a connman connection for
   * @return Future containing the actual CD1.1 Connection Manager used to later communicate via a
   * CD1.1 connection
   */
  public Future<Cd11FrameClient> establishCd11Connection(String address, int port,
      String stationId) {
    return establishConnection(address, port)
        .flatMap(socket -> buildFrameClient(socket, stationId));
  }

  private Future<Cd11FrameClient> buildFrameClient(NetSocket socket, String stationId) {
    Cd11FrameClient cd11FrameClient = new Cd11FrameClient(
        InetAddresses.forString(CONN_REQUEST_ORIGIN_IP), CONN_REQUEST_ORIGIN_PORT, socket,
        socketConfigBuilder.setStationOrResponderName(stationId).build(), vertx,
        false);
    return Future.succeededFuture(cd11FrameClient);
  }

  private Future<NetSocket> establishConnection(String address, int port) {
    Promise<NetSocket> promise = Promise.promise();
    netClient.connect(port, address, socketResult -> {
      if (socketResult.succeeded()) {
        logger.info("Successfully established connection to {}:{}", address, port);
        promise.complete(socketResult.result());
      } else {
        logger.error("Failed to establish socket connection", socketResult.cause());
        promise.fail(socketResult.cause());
      }
    });

    return promise.future();
  }


}
