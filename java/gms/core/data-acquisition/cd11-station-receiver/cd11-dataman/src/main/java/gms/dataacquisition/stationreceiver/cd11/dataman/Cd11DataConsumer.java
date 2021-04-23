package gms.dataacquisition.stationreceiver.cd11.dataman;

import static com.google.common.base.Preconditions.checkNotNull;
import static gms.dataacquisition.stationreceiver.cd11.parser.Cd11RawStationDataFrameUtility.parseAcquiredStationDataPacket;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.net.InetAddresses;
import gms.core.dataacquisition.receiver.DataFrameReceiverConfiguration;
import gms.dataacquisition.stationreceiver.cd11.common.Cd11GapList;
import gms.dataacquisition.stationreceiver.cd11.common.Cd11Socket;
import gms.dataacquisition.stationreceiver.cd11.common.configuration.Cd11SocketConfig;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11AcknackFrame;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11CommandResponseFrame;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11DataFrame;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11Frame;
import gms.dataacquisition.stationreceiver.cd11.dataman.configuration.Cd11DataConsumerConfig;
import gms.dataacquisition.stationreceiver.cd11.dataman.logging.StructuredLoggingWrapper;
import gms.shared.frameworks.osd.coi.dataacquisition.ReceivedStationDataPacket;
import gms.shared.frameworks.osd.coi.datatransferobjects.CoiObjectMapperFactory;
import gms.shared.frameworks.osd.coi.waveforms.RawStationDataFrame;
import gms.shared.utilities.javautilities.gracefulthread.GracefulThread;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.time.Instant;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.logging.log4j.ThreadContext;
import org.slf4j.LoggerFactory;


public class Cd11DataConsumer extends GracefulThread {

  private final StructuredLoggingWrapper logger = StructuredLoggingWrapper
      .create(LoggerFactory.getLogger(Cd11DataConsumer.class));

  public static final String STATION_LOGGING_KEY = "station";
  private final Cd11DataConsumerConfig consumerConfig;
  private final DataFrameReceiverConfiguration receiverConfig;

  private final Cd11Socket cd11Socket;
  private static final Message SHUTDOWNEVENT = new Message(MessageType.SHUTDOWN);

  // Event generators.
  private final Cd11DataConsumerConnectionExpiredThread connectionExpiredEvent;
  private final Cd11DataConsumerNewFrameReceivedThread newFrameReceivedEvent;
  private final Cd11DataConsumerSendAcknackThread sendAcknackEvent;
  private final Cd11DataConsumerPersistGapStateThread persistGapStateEvent;
  private final Cd11DataConsumerRemoveExpiredGaps removeExpiredGapsEvent;

  // Event queue.
  private final BlockingQueue<Message> eventQueue = new LinkedBlockingQueue<>();

  // Gap list.
  private final Cd11GapList cd11GapList;

  // Listening socket, for receiving connections from a Data Provider.
  private ServerSocket serverSocket = null;

  // Producer tasked with publishing RSDF to a Kafka Topic
  private final Producer<String, String> rsdfProducer;

  private final String rsdfOutputTopic;

  // Statistics and state information.
  private static final AtomicLong totalDataFramesReceived = new AtomicLong(0);

  // Log Acknack stuff in a separate file
  private static final String GAPSFILESTRING = "/data-receiver/shared-volume/logs/gapsFile.txt";

  private final ObjectMapper jsonObjectMapper = CoiObjectMapperFactory.getJsonObjectMapper();

  //-------------------- Constructors --------------------

  /**
   * Creates a Cd11DataConsumer
   *
   * @throws NullPointerException if waveformRepo is null
   */
  Cd11DataConsumer(Cd11DataConsumerConfig consumerConfig,
      DataFrameReceiverConfiguration receiverConfig,
      Producer<String, String> rsdfProducer, String rsdfOutputTopic) {
    super(consumerConfig.getThreadName(), true, true);

    this.logger
        .addKeyValueArgument(STATION_LOGGING_KEY, consumerConfig.getDataProviderStationName());

    // Initialize properties.
    this.consumerConfig = checkNotNull(consumerConfig);

    // Create a CD 1.1 client.
    this.cd11Socket = new Cd11Socket(Cd11SocketConfig.builder()
        .setStationOrResponderName(consumerConfig.getResponderName())
        .setStationOrResponderType(consumerConfig.getResponderType())
        .setServiceType(consumerConfig.getServiceType())
        .setFrameCreator(consumerConfig.getFrameCreator())
        .setFrameDestination(consumerConfig.getFrameDestination())
        .setAuthenticationKeyIdentifier(consumerConfig.getAuthenticationKeyIdentifier())
        .setProtocolMajorVersion(consumerConfig.getProtocolMajorVersion())
        .setProtocolMinorVersion(consumerConfig.getProtocolMinorVersion())
        .build());

    // Load the CD 1.1 gap list.
    this.cd11GapList = Cd11DataConsumerConfig
        .loadGapState(consumerConfig.getDataProviderStationName());

    // Initialize the event generators.
    this.newFrameReceivedEvent = new Cd11DataConsumerNewFrameReceivedThread(
        String.format("%s:NewFrameReceivedThread", this.getThreadName()), eventQueue, cd11Socket);
    this.sendAcknackEvent = new Cd11DataConsumerSendAcknackThread(
        String.format("%s:SendAcknackThread", this.getThreadName()), eventQueue, cd11Socket);
    this.connectionExpiredEvent = new Cd11DataConsumerConnectionExpiredThread(
        String.format("%s:ConnectionExpiredThread", this.getThreadName()), eventQueue, cd11Socket,
        this.consumerConfig.getConnectionExpiredTimeLimitSec());
    this.persistGapStateEvent = new Cd11DataConsumerPersistGapStateThread(
        String.format("%s:GapStateThread", this.getThreadName()), eventQueue,
        this.consumerConfig.getStoreGapStateIntervalMinutes());
    this.removeExpiredGapsEvent = new Cd11DataConsumerRemoveExpiredGaps(
        String.format("%s:RemoveExpiredGapsThread", this.getThreadName()), eventQueue);

    this.receiverConfig = receiverConfig;
    this.rsdfProducer = rsdfProducer;
    this.rsdfOutputTopic = rsdfOutputTopic;
  }

  //-------------------- Graceful Thread Methods --------------------

  @Override
  protected boolean onBeforeStart() {
    totalDataFramesReceived.set(0);
    return true;
  }

  /**
   * Start the Data Consumer thread.
   */
  @Override
  protected void onStart() throws Exception {
    ThreadContext.put(STATION_LOGGING_KEY, consumerConfig.getDataProviderStationName());

    // Listen for a connection.
    logger.info("Listening on port {} for a Data Provider to connect.",
        this.consumerConfig.getDataConsumerPort());

    try {
      this.serverSocket = new ServerSocket(this.consumerConfig.getDataConsumerPort());
      logger
          .info("Server socket created on port {}", this.consumerConfig.getDataConsumerPort());
    } catch (IOException e) {
      logger.error(
          "Error binding to socket on port " + this.consumerConfig.getDataConsumerPort(), e);
      return;
    }
    Socket socket;

    socket = makeConnection();
    if (socket == null) {
      logger.info("Server socket accept returned null socket on port {}; ending thread.",
          this.consumerConfig.getDataConsumerPort());
      return;
    }

    // Establish connection.
    logger
        .info("Establishing connection on port {}.", this.consumerConfig.getDataConsumerPort());
    cd11Socket.connect(socket);

    // Determine the IP Address of the connecting Data Provider.
    String dpIpAddress = cd11Socket.getRemoteIpAddressAsString();
    logger.info("Received Data Provider from remote address {}", dpIpAddress);

    // Hard close socket after 3 seconds so we don't get "Bind failed, address eventQueue use" errors.
    socket.setSoLinger(true, 3);

    // Check whether the remote IP Address matches the expected address.
    if (!dpIpAddress.equals(InetAddresses.toAddrString(consumerConfig.getExpectedDataProviderIpAddress()))) {
      logger.warn(
          "Data Provider IP address {} does not match the expected value {}.",
          dpIpAddress, consumerConfig.getExpectedDataProviderIpAddress());

      // TODO: In the future, reject these connections and continue looping!!!
    }

    // Close the ServerSocket, since we have the one connection we were listening for.
    serverSocket.close();
    logger
        .info("Server socket closed on port {}.", this.consumerConfig.getDataConsumerPort());

    serverSocket = null;

    // Start up a thread to listen for incoming data frames.
    newFrameReceivedEvent.start();

    // Start up a thread that triggers an event when it is time to send an Acknack frame.
    sendAcknackEvent.start();

    // Start up a thread that triggers an event when the connection has expired due to lack of contact.
    connectionExpiredEvent.start();

    // Start up a thread that triggers an event when it is time to persist the gap state.
    persistGapStateEvent.start();

    // Start up a thread that triggers an even when it is time to remove expired gaps.
    removeExpiredGapsEvent.start();

    // Enter the event loop.
    executeEventLoop();
  }

  private void executeEventLoop() throws IOException {
    while (this.keepThreadRunning()) {
      // Check that all event threads are running.
      if (!eventQueue.contains(SHUTDOWNEVENT) && (
          !connectionExpiredEvent.isRunning() ||
              !newFrameReceivedEvent.isRunning() ||
              !sendAcknackEvent.isRunning() ||
              !persistGapStateEvent.isRunning() ||
              !removeExpiredGapsEvent.isRunning())) {
        logger.error(
            "One or more event threads shut down unexpectedly, shutting down the Data Consumer.");
        this.shutdownGracefully();
        return; // Exit the thread.
      }

      // Read the next message from the event queue.
      Message mt;
      try {
        logger.info("Taking event from queue");
        mt = eventQueue.take(); // NOTE: This is a blocking call!
        if (logger.getWrappedLogger().isDebugEnabled()) {
          logger.debug(String.format("Got event from queue: %1$s", mt.messageType));
        }
      } catch (InterruptedException e) {
        logger.debug(String.format(
            "InterruptedException thrown in thread %1$s, closing thread.", this.getThreadName()),
            e);
        Thread.currentThread().interrupt();
        this.shutdownGracefully();
        return;
      }
      processMessage(mt);
    }
  }

  private void processMessage(Message mt) throws IOException {
    // Process the queue message.
    switch (mt.messageType) {

      case NEW_FRAME_RECEIVED:
        //TODO check in netty that not disabled before processing station data
        if (!consumerConfig.getStationDisabled()) {
          this.processNewFrame(mt.cd11Frame);
        } else {
          logger.info("Frame processing is disabled for station {}. Dropping frame.",
              this.consumerConfig.getDataProviderStationName());
        }
        break;

      case PERSIST_GAP_STATE:
        // TODO: In the future, the gap state should be persisted to the OSD.
        Cd11DataConsumerConfig
            .persistGapState(this.consumerConfig.getDataProviderStationName(),
                this.cd11GapList.getGapList());
        break;

      case REMOVE_EXPIRED_GAPS:
        if (consumerConfig.getGapExpirationInDays() > 0) {
          cd11GapList.removeExpiredGaps(consumerConfig.getGapExpirationInDays());
        }
        break;

      case SEND_ACKNACK:
        this.sendAcknack(cd11Socket);
        break;

      case SHUTDOWN:
        logger.info("Shutting down due to receiving Shutdown event on queue");
        this.shutdownGracefully();
        return; // Exit the thread.

      default:
        String errMsg = "Invalid MessageType received (this should never occur).";
        logger.error(errMsg);
        throw new IllegalStateException(errMsg);
    }
  }

  private Socket makeConnection() throws SocketException {
    Socket socket;
    // Check whether the thread is shutting down.
    if (this.shutThreadDown()) {
      logger.info("Shutting down gracefully");
      this.shutdownGracefully();
      return null; // Exit the thread.                                //this will probably cause probs
    }

    // Indicate that this GracefulThread is now initialized.
    this.setThreadAsInitialized();

    // Wait for the desired connection to arrive.
    try {
      logger.info("Server socket on port {} accepting connection (Blocking)",
          this.consumerConfig.getDataConsumerPort());
      socket = serverSocket.accept();
      logger.info("Server socket on port {} accepted connection successfully",
          this.consumerConfig.getDataConsumerPort());
    } catch (SocketException e) {
      // Check whether the thread is shutting down.
      if (this.shutThreadDown()) {
        // This is expected; SocketException was throw to break the blocking call and allow the thread to shut down.
        logger.error("Socket error; shutting down", e);
        this.shutdownGracefully();
        return null; // Exit the thread.
      }
      throw e; // This is unexpected, and should be logged as an error.

    } catch (IOException e) {
      logger.error("IOException in accept from Server Socket. Shutting down...", e);
      this.shutdownGracefully();
      return null;
    }
    return socket;
  }

  /**
   * Indicate that the Data Consumer thread needs to stop.
   */
  @Override
  protected void onStop() {
    // Unblock the server socket listening for Data Provider connections (if currently in use).
    if (this.serverSocket != null) {
      try {
        this.serverSocket.close();
        logger
            .info("Server socket closed on port {}.", this.consumerConfig.getDataConsumerPort());
        this.rsdfProducer.close();
      } catch (Exception e) {
        logger
            .error("Error in onStop on port {}:", this.consumerConfig.getDataConsumerPort(), e);
        // Ignore.
      }
    }
    ThreadContext.remove(STATION_LOGGING_KEY);
  }

  /**
   * Runs when GracefulThread catches an unhandled exception. (NOTE: This should never occur in
   * practice, since we are taking care to catch all exceptions.)
   *
   * @param thread Thread that threw the uncaught exception.
   * @param throwable Exception object.
   */
  @Override
  protected void onUncaughtException(Thread thread, Throwable throwable) {
    logger.error("Uncaught exception in data consumer", throwable);
    // Shut down gracefully.
    try {
      this.shutdownGracefully();
    } catch (Exception ex) {
      logger.error("Exception shutting down data consumer from uncaught exception", ex);
    }
  }

  //-------------------- Private Methods --------------------

  /**
   * Processes a newly arrived CD 1.1 frame.
   */
  private void processNewFrame(Cd11Frame cd11Frame) {
    switch (cd11Frame.frameType) {

      case ACKNACK:
        //We only use the Acknack to check for a reset, see if highest seq num is below current low.
        Cd11AcknackFrame acknackFrame = cd11Frame.asFrameType(Cd11AcknackFrame.class);
        logger
            .debug("Received ACKNACK frame. Frame Set Acked: {}", acknackFrame.framesetAcked);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(GAPSFILESTRING, true))) {
          writer.write(String.format("%s: %s: Received Acknack with low: %s, high: %s, gaps: %s %n",
              this.getThreadName(),
              Instant.now(), cd11GapList.getLowestSequenceNumber(),
              cd11GapList.getHighestSequenceNumber(), Arrays.toString(cd11GapList.getGaps())));
        } catch (IOException e) {
          logger.info("Couldn't write to gapsFile ", e);
        }
        cd11GapList.checkForReset(acknackFrame);
        break;

      case ALERT:
        logger.info("Received ALERT frame");
        try {
          eventQueue.put(new Message(MessageType.SHUTDOWN));
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          this.stop();
        }
        break;

      case CD_ONE_ENCAPSULATION:
        // TODO: Handle CD 1 Encapsulation frames.
        logger.warn("Received CD_ONE_ENCAPSULATION frame, which is not yet supported!");
        break;

      case COMMAND_REQUEST:
        logger.warn(
            "Received COMMAND_REQUEST frame, which should never have been sent by the Data Provider! Ignoring this frame.");

        break;

      case COMMAND_RESPONSE:
        logger.warn(
            "Received COMMAND_RESPONSE frame, recording the sequence number but ignoring the frame!");

        // Update the gaps list.
        Cd11CommandResponseFrame commandResponseFrame = cd11Frame
            .asFrameType(Cd11CommandResponseFrame.class);
        cd11GapList.addSequenceNumber(commandResponseFrame);

        break;

      case CONNECTION_REQUEST:
        logger.warn(
            "Received CONNECTION_REQUEST frame, which should never have been sent by the Data Provider! Ignoring this frame.");
        break;

      case CONNECTION_RESPONSE:
        logger.warn(
            "Received CONNECTION_RESPONSE frame, which should never have been sent by the Data Provider! Ignoring this frame.");
        break;

      case DATA:
        logger.info("Received DATA frame");
        handleDataFrame(cd11Frame);
        break;

      case OPTION_REQUEST:
        // Respond to the option request.
        handleOptionRequestFrame();
        logger.info("Received OPTION_REQUEST frame, ignoring frame.");
        break;

      case OPTION_RESPONSE:
        logger.info("Received OPTION_RESPONSE frame, ignoring frame.");
        break;

      case CUSTOM_RESET_FRAME:
        logger
            .info("Received CUSTOM_RESET_FRAME frame, clearing gap list and shutting down.");

        // Remove the "persist gap state" event from the queue, if one currently exist.
        boolean successfulRemove = eventQueue.remove(new Message(MessageType.PERSIST_GAP_STATE));
        if (!successfulRemove) {
          logger.error("Failed to remove persist game state event from event queue.");
        }

        // Clear the gap state.
        try {
          Cd11DataConsumerConfig.clearGapState(this.consumerConfig.getDataProviderStationName());
        } catch (IOException e) {
          logger.error("Cannot clear the gap state.", e);
        }

        this.cd11GapList.resetGapsList();

        // Shut down.
        try {
          eventQueue.put(new Message(MessageType.SHUTDOWN));
        } catch (Exception e) {
          this.stop();
        }
        break;

      default:
        String msg = "Invalid CD 1.1 frame messageType received (this should never occur).";
        logger.error(msg);
        throw new IllegalStateException(msg);
    }
  }

  /**
   * Sends an acknack frame.
   *
   * @param cd11Socket CD 1.1 Socket cd11Frame.
   */
  private void sendAcknack(Cd11Socket cd11Socket) throws Cd11DataConsumerException {
    /*Because we periodically write the Cd11GapList to the "framestore" there is no need
    //to directly query it every time. This is much faster, and if we ever go down
    //we will at most only re-request data smaller than the framestore write interval
    Send the Acknack frame. */
    try {
      cd11Socket.sendCd11AcknackFrame(
          cd11Socket.getFramesetAcked(),
          cd11GapList.getLowestSequenceNumber(),
          cd11GapList.getHighestSequenceNumber(),
          cd11GapList.getGaps());
    } catch (Exception e) {
      throw new Cd11DataConsumerException("Error sending acknack frame", new IOException(e));
    }

    String setAcknackMessage = String
        .format("%s: %s: Sent Acknack with low: %s, high: %s, gaps: %s %n", this.getThreadName(),
            Instant.now(), cd11GapList.getLowestSequenceNumber(),
            cd11GapList.getHighestSequenceNumber(), Arrays.toString(cd11GapList.getGaps()));
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(GAPSFILESTRING, true))) {
      writer.write(setAcknackMessage);
    } catch (Exception e) {
      logger.error("Couldn't write to gapsFile.", e);
    }
  }

  private void handleDataFrame(Cd11Frame cd11Frame) {
    Cd11DataFrame dataFrame = cd11Frame.asFrameType(Cd11DataFrame.class);
    // Increment the total number of data frames received.
    totalDataFramesReceived.incrementAndGet();
    try {
      final ReceivedStationDataPacket packet = ReceivedStationDataPacket.from(
          cd11Frame.getRawNetworkBytes(),
          Instant.now(),
          dataFrame.getFrameHeader().sequenceNumber,
          consumerConfig.getDataProviderStationName());
      final RawStationDataFrame frame = parseAcquiredStationDataPacket(receiverConfig,
          packet);

      //Publish frame to kafka
      logger.info("Publishing raw station data frame for channels {}",
          frame.getMetadata().getChannelNames());
      ProducerRecord<String, String> record =
          new ProducerRecord<>(rsdfOutputTopic, frame.getId().toString(),
              jsonObjectMapper.writeValueAsString(frame));

      rsdfProducer.send(record).get();

      // Update gaps upon successful storage.
      cd11GapList.addSequenceNumber(dataFrame);

      // Log successful storage.
      logger
          .info("DataFrame {} published successfully.", dataFrame.getFrameHeader().sequenceNumber);
    } catch (JsonProcessingException | ExecutionException e) {
      logger.error("Could not convert/publish CD 1.1 Data Frame", e);
    } catch (InterruptedException e) {
      logger.error("Publishing CD 1.1 Data Frame interrupted", e);
      Thread.currentThread().interrupt();
    } finally {
      rsdfProducer.flush();
    }
  }


  private void handleOptionRequestFrame() {
    //TODO: Do we need to read the content of the option request, and act on it? Currently, we just send a dull response, and ignore the content.
    try {
      // Right now the only option messageType is 1 with the station name as the option response.
      cd11Socket.sendCd11OptionResponseFrame(1, cd11Socket.getStationOrResponderName());

      logger.info("Option Response Frame Sent");
    } catch (Exception e) {
      logger.error(ExceptionUtils.getStackTrace(e));
    }
  }

  /**
   * Shuts down all event threads, and closes the CD 1.1 Socket connection.
   */
  private void shutdownGracefully() {
    // Signal that this thread needs to stop running.
    this.stop();

    // Attempt to send a CD 1.1 Alert frame to the Data Consumer.
    try {
      if (cd11Socket.isConnected()) {
        cd11Socket.sendCd11AlertFrame("Shutting down.");
      }
    } catch (Exception e) {
      // Do nothing.
    }

    // Stop all running threads.
    connectionExpiredEvent.stop();
    newFrameReceivedEvent.stop();
    sendAcknackEvent.stop();
    persistGapStateEvent.stop();
    removeExpiredGapsEvent.stop();
    connectionExpiredEvent.waitUntilThreadStops();
    newFrameReceivedEvent.waitUntilThreadStops();
    sendAcknackEvent.waitUntilThreadStops();
    persistGapStateEvent.waitUntilThreadStops();
    removeExpiredGapsEvent.waitUntilThreadInitializes();

    // Collect all "last error messages" written by this thread, and all of its event threads.
    String errorMessages = GracefulThread.aggregateErrorMessages(
        this,
        connectionExpiredEvent,
        newFrameReceivedEvent,
        sendAcknackEvent,
        persistGapStateEvent,
        removeExpiredGapsEvent);

    if (isNotEmpty(errorMessages)) {
      this.setErrorMessage(errorMessages);
    }

    // Disconnect the CD 1.1 Socket.
    cd11Socket.disconnect();
  }

  //-------------------- Statistics and State Info Methods --------------------

  /**
   * The port number that the CD 1.1 Data Consumer is using to receive data from the Data Provider.
   *
   * @return Port number.
   */
  public int getCd11ListeningPort() {
    return this.consumerConfig.getDataConsumerPort();
  }

  /**
   * Returns the total number of data frames received, since the Data Consumer thread was started.
   *
   * @return Total data frames received.
   */
  public long getTotalDataFramesReceived() {
    return totalDataFramesReceived.get();
  }
}

class Cd11DataConsumerException extends IOException {

  private final String error;
  private final IOException e;

  public Cd11DataConsumerException(String mess, IOException ex) {
    error = mess;
    e = ex;
  }

  @Override
  public String toString() {
    return error + " in " + e.getMessage();
  }
}
