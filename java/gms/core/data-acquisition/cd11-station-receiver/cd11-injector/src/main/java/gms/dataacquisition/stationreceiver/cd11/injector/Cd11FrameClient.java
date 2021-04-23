package gms.dataacquisition.stationreceiver.cd11.injector;

import gms.dataacquisition.stationreceiver.cd11.common.Cd11FrameFactory;
import gms.dataacquisition.stationreceiver.cd11.common.Cd11GapList;
import gms.dataacquisition.stationreceiver.cd11.common.configuration.Cd11SocketConfig;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11AcknackFrame;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11AlertFrame;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11ByteFrame;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11ConnectionRequestFrame;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11ConnectionResponseFrame;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11DataFrame;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11Frame;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11OptionRequestFrame;
import io.vertx.core.Handler;
import io.vertx.reactivex.core.Future;
import io.vertx.reactivex.core.Promise;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.core.net.NetSocket;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Given a connected socket, manages the CD1.1 connection including: - sending ACK/NACKS to keep the
 * connection alive - timing out the connection if the other side is idle too long - monitors
 * incoming data - closes connection on receipt of ALERT or sends ALERT if this side is closed -
 * provides a means for users to send data
 * <p>
 * NOTE: created for the injector and its particular needs so use on a receiver has not been fully
 * thought through
 */
public class Cd11FrameClient {

  //Per CD1.1 spec on heartbeat
  public static final int HEARTBEAT_INTERVAL_MS = 60000;
  public static final double TIMEOUT_RATIO = 2.5;

  private final InetAddress clientHost;
  private final Integer clientPort;
  private final NetSocket socket;      // socket that is established connection
  private final Vertx vertx;

  private final Cd11FrameFactory cd11FrameFactory;

  private final Logger logger;

  // Timers
  private final long acknackTimerId;

  // Bookkeeping
  private final AtomicLong lastAcknackReceived;

  // just used as a convenient way to track lowest, highest sequence number sent for injection data looping
  private Cd11GapList gapList;

  private Promise<Cd11ConnectionResponseFrame> connRespPromise;

  /**
   * Constructor
   *
   * @param clientHost Address of the created client
   * @param clientPort Port of the created client
   * @param socket the connected {@link NetSocket} that is the connection
   * @param socketConfig Configuration used for creating Cd1.1 Frames
   * @param vertx handle on vertx for timer creation
   * @param sendOptionReq whether to send the option frames
   */
  public Cd11FrameClient(InetAddress clientHost, Integer clientPort, NetSocket socket,
      Cd11SocketConfig socketConfig, Vertx vertx, boolean sendOptionReq) {

    this.clientHost = clientHost;
    this.clientPort = clientPort;
    this.socket = socket;
    this.vertx = vertx;
    this.cd11FrameFactory = Cd11FrameFactory.builderWithDefaults()
            .setAuthenticationKeyIdentifier(socketConfig.getAuthenticationKeyIdentifier())
            .setFrameDestination(socketConfig.getFrameDestination())
            .setFrameCreator(socketConfig.getFrameCreator())
            .setProtocolMinorVersion(socketConfig.getProtocolMinorVersion())
            .setProtocolMajorVersion(socketConfig.getProtocolMajorVersion())
            .setResponderName(socketConfig.getStationOrResponderName())
            .setResponderType(socketConfig.getStationOrResponderType())
            .setServiceType(socketConfig.getServiceType())
            .build();

    // Add station/receiver name to the logger
    String connectionManagerId = String
            .format("%s|%s", Cd11FrameClient.class, cd11FrameFactory.getResponderName());
    logger = LoggerFactory.getLogger(connectionManagerId);

    // Initialize socket bookkeeping
    lastAcknackReceived = new AtomicLong(System.currentTimeMillis());
    gapList = new Cd11GapList();

    socket.exceptionHandler(e -> logger.error("Problem in socket", e));

    // Create the read stream and register a handler
    socket.handler(this::handleSocketRead);

    // AckNack send on idle connection timer
    acknackTimerId = vertx.setPeriodic(HEARTBEAT_INTERVAL_MS, id -> {
      if (System.currentTimeMillis() - lastAcknackReceived.get()
          > TIMEOUT_RATIO * HEARTBEAT_INTERVAL_MS) {
        close("Acknack heartbeat exceeded timeout threshold, closing connection...");
      }

      logger.info("Sending ack/nack to data consumer...");
      sendAcknack()
          .onSuccess(result -> logger.info("Acknack sent successfully"))
          .onFailure(cause -> logger.error("Error sending acknack", cause));
    });

    // Now send option request
    if (sendOptionReq) {
      try {
        Cd11OptionRequestFrame optionReq = cd11FrameFactory
            .createCd11OptionRequestFrame(1, "Station");
        sendFrame(optionReq);
      } catch (IllegalArgumentException ex) {
        logger.error("Unable to send option request", ex);
      }
    }
  }

  public Cd11FrameFactory getCd11FrameFactory() {
    return cd11FrameFactory;
  }

  /**
   * Handler for socket close events
   *
   * @param closeMessage Message to send as alert on close
   */
  public void close(String closeMessage) {
    logger.info("Stopping Cd11 connection: {}", closeMessage);
    sendAlert(closeMessage)
        .onFailure(throwable -> logger.warn("Unable to send alert frame", throwable))
        .onComplete(sendResult -> close());
  }

  private void close() {
    logger.info("Closing socket and cancelling heartbeat timer");
    boolean cancelResult = vertx.cancelTimer(acknackTimerId);
    logger.info("Cancelling timer. Result: {}", cancelResult);
    socket.close();
  }

  /**
   * Handler for socket read events
   *
   * @param record Complete CD1.1 Frame bytes as a {@link Buffer}
   */
  private void handleSocketRead(Buffer record) {
    logger.debug("Handling socket read");

    try {
      Cd11ByteFrame byteFrame = new Cd11ByteFrame(
          new DataInputStream(new ByteArrayInputStream(record.getBytes())), () -> true);
      Cd11Frame cd11Frame = cd11FrameFactory.createCd11Frame(byteFrame);
      logger.debug("Received {} Frame: {}", cd11Frame.frameType, cd11Frame.getFrameHeader());

      switch (cd11Frame.frameType) {
        case ACKNACK:
          Cd11AcknackFrame acknackFrame = (Cd11AcknackFrame) cd11Frame;
          logger.info("Acknack {} received, updating timeout", acknackFrame.framesetAcked);
          lastAcknackReceived.set(System.currentTimeMillis());
          break;
        case ALERT:
          Cd11AlertFrame alertFrame = (Cd11AlertFrame) cd11Frame;
          logger.info("Alert message received: {}. Closing client...", alertFrame.message);
          close();
          break;
        case CONNECTION_RESPONSE:
          Cd11ConnectionResponseFrame connectionResponseFrame = (Cd11ConnectionResponseFrame) cd11Frame;
          logger
              .info("Connection response received from {}", connectionResponseFrame.responderName);
          if (connRespPromise == null) {
            logger.error("Received response frame from {} without sending request frame",
                connectionResponseFrame.responderName);
          } else {
            connRespPromise.complete(connectionResponseFrame);
          }
          break;
        default:
          // Currently do nothing on ACK/NACKS because for injection/playback the action taken
          // by the data producer is implicit in the recorded data.  In other words don't resend
          // data due to ACK/NACKS here because the recorded data already has that resend in it.
          logger.debug("Doing nothing for {} frame", cd11Frame.frameType);
          break;
      }
    } catch (Exception e) {
      logger.error("Unable to create Cd11 Frame from bytes", e);
    }
  }

  private Future<Void> sendAcknack() {
    long[] gaps = {};
    Cd11Frame frame = cd11FrameFactory.createCd11AcknackFrame("None", 0, 0, gaps);
    return sendFrame(frame);
  }

  /**
   * Close the connection per protocol, sending an ALERT frame
   *
   * @param message Message to be sent to peer in ALERT frame
   */
  private Future<Void> sendAlert(String message) {
    return sendFrame(cd11FrameFactory.createCd11AlertFrame(message));
  }

  /**
   * Sends the provided frame over the connection
   *
   * @param frame {@link Cd11Frame} to be sent
   */
  public Future<Void> sendFrame(Cd11Frame frame) {
    Promise<Void> promise = Promise.promise();
      try {
        Buffer buffer = Buffer.buffer(frame.toBytes());
        logger.info("Sending {} Frame {} bytes in length", frame.frameType, buffer.length());
        socket.write(buffer, res -> {
          if (res.succeeded()) {
            promise.complete();
          } else {
            logger.warn("Failed to write to socket", res.cause());
            promise.fail(res.cause());
          }
        });
      } catch (IOException e) {
        logger.warn("Error constructing Frame buffer");
        promise.fail(e);
      }
      return promise.future();
  }

  public Future<Void> sendData(byte[] payload) {
    try {
      Cd11ByteFrame byteFrame = new Cd11ByteFrame(
          new DataInputStream(new ByteArrayInputStream(payload)),
          () -> true);
      Cd11DataFrame dataFrame = (Cd11DataFrame) cd11FrameFactory.createCd11Frame(byteFrame);
      return sendData(dataFrame);
    } catch (IOException e) {
      return Future.failedFuture(e);
    }
  }

  public Future<Void> sendData(Cd11DataFrame dataFrame) {
    // Record the sequence number even if the write failed
    // at this point the data is lost
    gapList.addSequenceNumber(dataFrame.getFrameHeader().sequenceNumber);
    return sendFrame(dataFrame);
  }

  public Future<Cd11ConnectionResponseFrame> sendConnectionRequest() {
    connRespPromise = Promise.promise();
    Cd11ConnectionRequestFrame connectionRequestFrame = cd11FrameFactory
        .createCd11ConnectionRequestFrame(clientHost, clientPort);
    sendFrame(connectionRequestFrame).onFailure(connRespPromise::fail);
    return connRespPromise.future();
  }

  public void setOnClose(Handler<Void> onClose) {
    socket.closeHandler(onClose);
  }

  /**
   * Retuns the highest sequence number sent on this connection
   *
   * @return the sequence number
   */
  public long getHighestSentSequenceNumber() {
    return gapList.getHighestSequenceNumber();
  }

  /**
   * Returns the lowest sequence number sent on this connection
   *
   * @return the sequence number
   */
  public long getLowestSentSequenceNumber() {
    return gapList.getLowestSequenceNumber();
  }

  /**
   * Resets sequence number book-keeping
   */
  public void resetSequenceNumberBookkeeping() {
    // TODO: maybe the gaplist should have a clear()/reset()
    gapList = new Cd11GapList();
  }
}

