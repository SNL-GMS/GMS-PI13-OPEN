package gms.dataacquisition.stationreceiver.cd11.connman;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.net.InetAddresses;
import gms.dataacquisition.stationreceiver.cd11.common.Cd11Socket;
import gms.dataacquisition.stationreceiver.cd11.common.configuration.Cd11SocketConfig;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11ConnectionRequestFrame;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11Frame;
import gms.dataacquisition.stationreceiver.cd11.connman.configuration.Cd11ConnectionConfig;
import gms.shared.utilities.javautilities.gracefulthread.GracefulThread;
import java.io.IOException;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Cd11Connection extends GracefulThread {

  private static final Logger logger = LoggerFactory.getLogger(Cd11Connection.class);

  private final Cd11Socket cd11Socket;
  private final Cd11ConnectionConfig config;
  private final Function<String, Cd11Station> cd11StationLookup;
  private final Consumer<ConnectionLog> connectionLogCallback;

  private Map<String, Boolean> ignoredStationsMap;

  private final String remoteIPAddress;
  private final int remotePort;

  private AtomicReference<ConnectionLog> connectionLog = new AtomicReference<>();

  /**
   * Constructor.
   *
   * @param config                Configuration.
   * @param connectionLogCallback Lambda for returning a connection log to the Connection Manager.
   * @throws NullPointerException if cd11Socket is null
   */
  public Cd11Connection(
      Cd11ConnectionConfig config,
      Socket socket,
      Function<String, Cd11Station> cd11StationLookup,
      Consumer<ConnectionLog> connectionLogCallback,
      Map<String, Boolean> ignoredStationsMap) throws IOException {

    super(config.threadName, false, false);

    // Validate input parameters.
    checkNotNull(socket);
    checkState(!socket.isClosed(), "Socket is closed.");
    checkState(socket.isBound(), "Socket is not bound.");
    checkState(socket.isConnected(), "Socket is not connected.");
    checkNotNull(cd11StationLookup);

    // Store input.
    this.config = checkNotNull(config);
    this.cd11StationLookup = cd11StationLookup;
    this.connectionLogCallback = connectionLogCallback;
    this.ignoredStationsMap = ignoredStationsMap;

    // Create a CD 1.1 socket object.
    this.cd11Socket = new Cd11Socket(Cd11SocketConfig.builder()
        .setStationOrResponderName(config.responderName)
        .setStationOrResponderType(config.responderType)
        .setServiceType(config.serviceType)
        .setFrameCreator(config.frameCreator)
        .setFrameDestination(config.frameDestination)
        .setAuthenticationKeyIdentifier(7)
        .setProtocolMajorVersion((short) 2)
        .setProtocolMinorVersion((short) 0)
        .build());
    this.cd11Socket.connect(socket);

    // Determine the remote IP address and port.
    this.remoteIPAddress = cd11Socket.getRemoteIpAddressAsString();
    this.remotePort = socket.getPort();
  }

  /**
   * Runnable method for this object (threaded).
   */
  @Override
  protected void onStart() {
    try {
      logger.info("Cd11Connection thread is running.");
      // Wait for the next frame to arrive.
      Cd11Frame cd11Frame = this.cd11Socket.read(config.socketReadTimeoutMs);

      // Validate CRC for frame.
      if (!cd11Frame.isValidCRC()) {
        logger.error("CRC check failed for frame!!!");
      }

      // Retrieve the connection request frame.
      Cd11ConnectionRequestFrame connRequestFrame = cd11Frame
          .asFrameType(Cd11ConnectionRequestFrame.class);

      boolean isValidConnectionRequest = false;

      if (ignoredStationsMap.get(connRequestFrame.stationName) == null) {
        //As this station is NOT set to be ignored, begin processing the request

        logger.info("Received connection request for station {} at {}:{}",
            connRequestFrame.stationName, remoteIPAddress, remotePort);

        // Find the station info.
        Cd11Station cd11Station = cd11StationLookup.apply(connRequestFrame.stationName);

        // Check that the station name is known.
        if (cd11Station == null) {
          logger.warn(
              "Connection request received from station {} that has no active configuration; ignoring connection.",
              connRequestFrame.stationName);
        }
        // Check that the request originates from the expected IP Address.
        else {
          // TODO: In the future, this should reject the request (keeping for testing purposes).
          if (!remoteIPAddress.equals(InetAddresses.toAddrString(cd11Station.expectedDataProviderIpAddress))) {
            logger.warn("Connection request received from recognized station {}, "
                    + "but originating from an unexpected IP address (expected {}, received {}).",
                connRequestFrame.stationName, cd11Station.expectedDataProviderIpAddress,
                remoteIPAddress);
          }

          logger.info("Connection Request received from station {}. Redirecting station to {}:{} ",
              connRequestFrame.stationName,
              cd11Station.dataConsumerIpAddress,
              cd11Station.dataConsumerPort);

          // Send out the Connection Response Frame.
          String consumerAddressIp = InetAddresses.toAddrString(cd11Station.dataConsumerIpAddress);
          logger.info("Configured data consumer retrieved from cd11Station, resolved IP: {}",
              consumerAddressIp);
          cd11Socket.sendCd11ConnectionResponseFrame(
              cd11Station.dataConsumerIpAddress, cd11Station.dataConsumerPort,
              null, null);

          logger
              .info("Connection Response Frame sent to station {}.", connRequestFrame.stationName);

          // Indicate the the connection request was valid.
          isValidConnectionRequest = true;
        }
      }

      // Close the socket connection.
      cd11Socket.disconnect();

      // Create the connection log.
      connectionLog.set(new ConnectionLog(
          remoteIPAddress, remotePort,
          connRequestFrame.stationName, isValidConnectionRequest));

      // Send the connection log to the Connection Manager (via the callback).
      if (connectionLogCallback != null) {
        connectionLogCallback.accept(connectionLog.get());
      }

    } catch (IOException e) {
      logger.error("Error reading or parsing frame: ", e);
    } finally {
      cd11Socket.disconnect();
      logger.info("Cd11Connection socket has been disconnected");
    }
  }

  /**
   * Signals the Connection Manager to gracefully shutdown.
   */
  @Override
  public void onStop() {
    cd11Socket.disconnect();
    logger.info("Cd11Connection thread has been stopped and its socket has been disconnected.");
  }

  /**
   * Returns the connection log, or null if one has not been created.
   */
  public ConnectionLog getConnectionLog() {
    return (connectionLog == null) ? null : connectionLog.get();
  }
}
