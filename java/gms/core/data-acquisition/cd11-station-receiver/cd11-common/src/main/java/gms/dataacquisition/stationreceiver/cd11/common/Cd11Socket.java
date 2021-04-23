package gms.dataacquisition.stationreceiver.cd11.common;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.net.InetAddresses;
import gms.dataacquisition.stationreceiver.cd11.common.configuration.Cd11SocketConfig;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11AcknackFrame;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11ByteFrame;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11ChannelSubframe;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11ConnectionRequestFrame;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11ConnectionResponseFrame;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11Frame;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11Frame.FrameType;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11FrameHeader;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11FrameTrailer;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11OptionResponseFrame;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public final class Cd11Socket {

  private static final Logger logger = LoggerFactory.getLogger(Cd11Socket.class);

  private Socket socket = null;
  private DataInputStream socketIn = null;
  private DataOutputStream socketOut = null;
  private AtomicLong lastContactTimeNs = new AtomicLong(0);
  private AtomicLong lastAcknackSentTimeNs = new AtomicLong(0);
  private AtomicLong lastDataSentTimeNs = new AtomicLong(0);

  private final Cd11SocketConfig config;
  private static final String SOCKETNOTOPEN = "Socket connection is not open.";

  private final Object readLock = new Object();
  private final Object writeLock = new Object();

  private String framesetAcked;
  private Cd11FrameFactory cd11FrameFactory;

  /**
   * CD 1.1 client and server base class.
   *
   * @param config Configuration object. Not null.
   */
  public Cd11Socket(Cd11SocketConfig config) {
    checkNotNull(config, "Cd11SocketConfig cannot be null");

    // Initialize properties and validate input.
    this.config = config;
    this.framesetAcked = String
        .format("%s:%s", config.getFrameCreator(), config.getFrameDestination());

      this.cd11FrameFactory = Cd11FrameFactory.builderWithDefaults()
          .setAuthenticationKeyIdentifier(config.getAuthenticationKeyIdentifier())
          .setFrameDestination(config.getFrameDestination())
          .setFrameCreator(config.getFrameCreator())
          .setProtocolMinorVersion(config.getProtocolMinorVersion())
          .setProtocolMajorVersion(config.getProtocolMajorVersion())
          .setResponderName(config.getStationOrResponderName())
          .setResponderType(config.getStationOrResponderType())
          .setServiceType(config.getServiceType())
          .build();
  }

  public String getRemoteIpAddressAsString() {
    checkNotNull(this.socket, "CD11Socket cannot be null");
    return InetAddresses.toAddrString(this.socket.getInetAddress());
  }

  public String getLocalIpAddressAsString() {
    return new InetSocketAddress(this.socket.getLocalAddress(), this.socket.getLocalPort())
        .getHostString();
  }

  public int getRemotePort() {
    return this.socket.getPort();
  }

  //---------- Sockets and Networking Methods ----------

  /**
   * Returns true if the socket is connected to the Data Consumer, false otherwise.
   *
   * @return True if socket is connected to the Data Consumer.
   */
  public boolean isConnected() {
    // Check that the socket is fully connected.
    return (
        this.socket != null &&
            this.socketIn != null &&
            this.socketOut != null &&
            !this.socket.isClosed() &&
            this.socket.isBound() &&
            this.socket.isConnected() &&
            !this.socket.isInputShutdown() &&
            !this.socket.isOutputShutdown());
  }

  /**
   * Returns true if data has been received from the Data Consumer, and is ready to be read.
   *
   * @return True if data is ready to be read from the socket.
   * @throws IllegalStateException if the socket's connection is not open
   * @throws IOException if there is an I/O error while checking the sockets availability
   */
  public boolean isNextFrameReadyToRead() throws IOException {
    // Check that the socket is ready to use.
    if (!this.isConnected()) {
      throw new IllegalStateException(SOCKETNOTOPEN);
    }

    return (this.socketIn.available() > Cd11FrameHeader.FRAME_LENGTH);
  }

  /**
   * Establishes a socket connection to the Data Consumer, and initializes its I/O streams.
   *
   * @param dataConsumerWellKnownIpAddress Data Consumer's "Well Known" IP Address.
   * @param dataConsumerWellKnownPort Data Consumer's "Well Known" port number.
   * @param maxWaitTimeMs Continuously attempt to connect until this limit is exceeded.
   * <p>
   * - Less than 0: Wait forever.
   * <p>
   * - Equal to 0: Only try to connect once.
   * <p>
   * - Greater than 0: Wait for the specified period of time.
   * @throws IOException Thrown on socket, I/O stream, or network connection errors.
   */
  public void connect(
      InetAddress dataConsumerWellKnownIpAddress, int dataConsumerWellKnownPort,
      long maxWaitTimeMs) throws IOException {
    this.connect(
        dataConsumerWellKnownIpAddress, dataConsumerWellKnownPort,
        maxWaitTimeMs, null, 0);
  }

  /**
   * Establishes a socket connection to the Data Consumer, and initializes its I/O streams.
   *
   * @param dataConsumerWellKnownIpAddress Data Consumer's "Well Known" IP Address.
   * @param dataConsumerWellKnownPort Data Consumer's "Well Known" port number.
   * @param maxWaitTimeMs Continuously attempt to connect until this limit is exceeded.
   * <p>
   * - Less than 0: Wait forever.
   * <p>
   * - Equal to 0: Only try to connect once.
   * <p>
   * - Greater than 0: Wait for the specified period of time.
   * @param localPort Local port to bind to (if 0, use ephemeral port).
   * @throws IOException Thrown on socket, I/O stream, or network connection errors.
   */
  public void connect(
      InetAddress dataConsumerWellKnownIpAddress, int dataConsumerWellKnownPort,
      long maxWaitTimeMs, int localPort) throws IOException {
    this.connect(
        dataConsumerWellKnownIpAddress, dataConsumerWellKnownPort,
        maxWaitTimeMs, null, localPort);
  }

  /**
   * Establishes a socket connection to the Data Consumer, and initializes its I/O streams.
   *
   * @param dataConsumerWellKnownIpAddress Data Consumer's "Well Known" IP Address.
   * @param dataConsumerWellKnownPort Data Consumer's "Well Known" port number.
   * @param maxWaitTimeMs Continuously attempt to connect until this limit is exceeded.
   * <p>
   * - Less than 0: Wait forever.
   * <p>
   * - Equal to 0: Only try to connect once.
   * <p>
   * - Greater than 0: Wait for the specified period of time.
   * @param localIpAddress Local IP address to bind to (if null, then listen on all IPs).
   * @param localPort Local port to bind to (if 0, use ephemeral port).
   * @throws IOException Thrown on socket, I/O stream, or network connection errors.
   */
  public void connect(
      InetAddress dataConsumerWellKnownIpAddress, int dataConsumerWellKnownPort,
      long maxWaitTimeMs,
      InetAddress localIpAddress, int localPort) throws IOException {

    // Validate arguments.
    Cd11Validator.validIpAddress(dataConsumerWellKnownIpAddress);
    Cd11Validator.validPortNumber(dataConsumerWellKnownPort);
    if (localIpAddress != null) {
      Cd11Validator.validIpAddress(localIpAddress);
      Cd11Validator.validNonZeroPortNumber(localPort);
    } else {
      Cd11Validator.validPortNumber(localPort);
    }

    long endTimeNs = (maxWaitTimeMs < 0) ? 0 : System.nanoTime() + (maxWaitTimeMs * 1000 * 1000);
    while (true) {
      try {
        bindToLocalSocket(dataConsumerWellKnownIpAddress, dataConsumerWellKnownPort, localIpAddress,
            localPort);

        // Set the SoLinger.
        this.socket.setSoLinger(true, 3);

        // Connection established, now break from the loop.
        break;
      } catch (IOException e) {
        if (maxWaitTimeMs < 0 || System.nanoTime() < endTimeNs) {
          // Try again.
          logger.debug(
              "Data Provider connection attempt to {}:{} failed (will retry).",
              dataConsumerWellKnownIpAddress, dataConsumerWellKnownPort, e);
        } else {
          logger.error(
              "Data Provider could not connect to {}:{} within the maximum wait time limit ({} ms).",
              dataConsumerWellKnownIpAddress, dataConsumerWellKnownPort, maxWaitTimeMs, e);
          throw e;
        }
      } catch (Exception e) {
        logger.error(
            "Exception while Data Provider was attempting to open socket connection to {}:{}.",
            dataConsumerWellKnownIpAddress, dataConsumerWellKnownPort);
        throw e;
      }
    }

    // Connect to the socket's I/O streams.
    this.connectSocketIoStreams();

    // Initialize the "last contact", "last Acknack sent", and "last data sent" time stamps.
    long tstamp = System.nanoTime();
    lastContactTimeNs.set(tstamp);
    lastAcknackSentTimeNs.set(tstamp);
    lastDataSentTimeNs.set(tstamp);

    // Log the connection info.
    logger.debug("CD11Client established connection from {}:{} to {}:{}",
        // Local IP/port first, since we are initiating the connection.
        getLocalIpAddressAsString(),
        this.socket.getLocalPort(),
        // Remote IP/port second.
        getRemoteIpAddressAsString(),
        this.socket.getPort());
  }

  private void bindToLocalSocket(InetAddress dataConsumerWellKnownIpAddress,
      int dataConsumerWellKnownPort, InetAddress localIpAddress, int localPort) throws IOException {
    // Bind to the local socket, and connect to the remote machine.
    if (localIpAddress == null && localPort == 0) {
      // Listen on all IPs, and use an ephemeral port.
      this.socket = new Socket(
          dataConsumerWellKnownIpAddress,
          dataConsumerWellKnownPort);
    } else if (localIpAddress == null) {
      // Listen on all IPs, but specify the local port.
      this.socket = new Socket(
          dataConsumerWellKnownIpAddress,
          dataConsumerWellKnownPort,
          null,
          localPort);
    } else {
      // Specify the local IP and port.
      this.socket = new Socket(
          dataConsumerWellKnownIpAddress,
          dataConsumerWellKnownPort,
          localIpAddress,
          localPort);
    }
  }

  /**
   * Establishes a socket connection to the Data Provider, and initializes its I/O streams.
   *
   * @param newSocket The socket connect to the data provider.
   * @throws IOException Thrown on socket, I/O stream, or network connection errors.
   */
  public void connect(Socket newSocket) throws IOException {
    // Validate input.
    if (newSocket == null) {
      throw new NullPointerException("Socket object received is null.");
    }
    if (newSocket.isClosed() || !newSocket.isConnected() || !newSocket.isBound()) {
      throw new IllegalArgumentException(
          "Socket is either closed, or was never connected to a remote address.");
    }

    // Accept the socket.
    this.socket = newSocket;

    // Connect to the socket's I/O streams.
    this.connectSocketIoStreams();

    // Initialize the "last contact", "last Acknack sent", and "last data sent" time stamps.
    long tstamp = System.nanoTime();
    lastContactTimeNs.set(tstamp);
    lastAcknackSentTimeNs.set(tstamp);
    lastDataSentTimeNs.set(tstamp);

    // Log the connection info.
    logger.debug("CD11Client established connection from {}:{} to {}:{}.",
        // Remote IP/port first, since we are receiving the connection.
        getRemoteIpAddressAsString(),
        this.socket.getPort(),
        // Local IP/port second.
        getLocalIpAddressAsString(),
        this.socket.getLocalPort());
  }

  /**
   * Connects this object to a socket's I/O streams.
   *
   * @throws IOException Thrown on socket, I/O stream, or network connection errors.
   */
  private void connectSocketIoStreams() throws IOException {
    // Connect to the output data streams.
    try {
      this.socketOut = new DataOutputStream(socket.getOutputStream());
    } catch (IOException e) {
      throw new IOException("Socket output stream could not be established.", e);
    }

    // Connect to the input data streams.
    try {
      this.socketIn = new DataInputStream(socket.getInputStream());
    } catch (IOException e) {
      throw new IOException("Socket input stream could not be established.", e);
    }
  }

  /**
   * Gracefully closes the socket, and its I/O streams.
   */
  public void disconnect() {
    if (this.socket != null) {
      try {
        this.socket.close();
      } catch (Exception e) {
        logger.warn("Socket could not be closed.", e);
      } finally {
        this.socket = null;
      }
    }

    if (this.socketIn != null) {
      try {
        this.socketIn.close();
      } catch (Exception e) {
        logger.warn("Input stream could not be closed.", e);
      } finally {
        this.socketIn = null;
      }
    }

    if (this.socketOut != null) {
      try {
        this.socketOut.close();
      } catch (Exception e) {
        logger.warn("Output stream could not be closed.", e);
      } finally {
        this.socketOut = null;
      }
    }

    // Reset the "last contact", "last Acknack sent", and "last Data sent" time stamps.
    lastContactTimeNs.set(0);
    lastAcknackSentTimeNs.set(0);
    lastDataSentTimeNs.set(0);
  }

  /**
   * Sends a CD 1.1 frame to the Data Consumer.
   *
   * @param cd11Frame The {@link Cd11Frame} to write
   * @throws IOException Thrown on byte serialization, or socket errors.
   */
  public void write(Cd11Frame cd11Frame) throws IOException {

    synchronized (writeLock) {
      // Check that the socket is ready to use.
      if (!this.isConnected()) {
        throw new IllegalStateException(SOCKETNOTOPEN);
      }

      socketOut.write(cd11Frame.toBytes());
      socketOut.flush();
    }

    // Check if an Acknack or Data frame was just sent out.
    if (cd11Frame.frameType == FrameType.ACKNACK) {
      // Update the "last Acknack sent" time stamp.
      lastAcknackSentTimeNs.set(System.nanoTime());
    } else if (cd11Frame.frameType == FrameType.DATA) {
      // Update the "last Data sent" time stamp.
      lastDataSentTimeNs.set(System.nanoTime());
    }
  }

  /**
   * Receives a CD 1.1 frame from the Data Consumer. NOTE: This operation is meant for use in
   * single-threaded processing.
   *
   * @param maxWaitTimeMs The maximum amount of time to wait for a data frame to arrive.
   * @return CD 1.1 frame.
   * @throws IOException Thrown on socket, parsing, validation, or object construction error; or if
   * the maximum wait time was exceeded after some data had been read from the socket.
   */
  public Cd11Frame read(long maxWaitTimeMs) throws IOException {
    checkState(maxWaitTimeMs > 0, "Max wait time must be greater than zero.");

    long endTimeNs = System.nanoTime() + (maxWaitTimeMs * 1000 * 1000);
    return read(() -> (System.nanoTime() >= endTimeNs));
  }

  /**
   * Receives a CD 1.1 frame from the Data Consumer. NOTE: This operation is meant for use in
   * multi-threaded processing.
   *
   * @param haltReadOperation The maximum amount of time to wait for frame data to arrive.
   * @return CD 1.1 object received from the Data Consumer.
   * @throws java.io.InterruptedIOException Thrown if the maximum wait time was exceeded before any
   * data had arrived.
   * @throws IOException Thrown on socket, parsing, validation, or object construction error; or if
   * the haltReadOperation was triggered after some data had been read from the socket.
   */
  public Cd11Frame read(BooleanSupplier haltReadOperation)
      throws IOException {

    Cd11ByteFrame cd11ByteFrame;

    synchronized (readLock) {
      // Check that the socket is ready to use.
      if (!this.isConnected()) {
        throw new IllegalStateException(SOCKETNOTOPEN);
      }

      // Parse the CD 1.1 frame.
      cd11ByteFrame = new Cd11ByteFrame(this.socketIn, haltReadOperation);
    }

    // Update the "last contact" time stamp.
    lastContactTimeNs.set(System.nanoTime());

    Cd11Frame cd11Frame = cd11FrameFactory.createCd11Frame(cd11ByteFrame);

    if (cd11Frame.frameType == FrameType.ACKNACK) {
      //Set the Frame Creator from the first frame we receive on the data consumer (always option request)
      this.framesetAcked = ((Cd11AcknackFrame) cd11Frame).framesetAcked;
    }

    return cd11Frame;
  }

  //---------- CD 1.1 Frame Creation Methods ----------

  /**
   * Generates a CD 1.1 Connection Request frame.
   *
   * @return CD 1.1 Connection Request frame.
   * @throws IllegalArgumentException Thrown on invalid input.
   */
  public Cd11ConnectionRequestFrame createCd11ConnectionRequestFrame() {
    //TODO: This method is essentially duplicated in Cd11FrameFactory, but requires arguments that
    // can only be retrieved from this.socket, which is currently private.

    // Create the frame body.
    Cd11ConnectionRequestFrame newFrame = new Cd11ConnectionRequestFrame(
        config.getProtocolMajorVersion(), config.getProtocolMinorVersion(),
        config.getStationOrResponderName(), config.getStationOrResponderType(),
        config.getServiceType(),
        (socket == null) ? InetAddresses.forString("0.0.0.0") : this.socket.getLocalAddress(),
        (socket == null) ? 0 : socket.getLocalPort(),
        null, null); // TODO: Support secondary IP and Port???

    // Generate the frame body byte array.
    byte[] frameBodyBytes = newFrame.getFrameBodyBytes();

    // Use the frame body byte array to generate the frame header.
    Cd11FrameHeader frameHeader = new Cd11FrameHeader(
        FrameType.CONNECTION_REQUEST,
        Cd11FrameHeader.FRAME_LENGTH + Cd11ConnectionRequestFrame.FRAME_LENGTH,
        config.getFrameCreator(),
        config.getFrameDestination(),
        0);

    // Generate the frame header and body byte arrays.
    ByteBuffer frameHeaderAndBodyByteBuffer = ByteBuffer.allocate(
        Cd11FrameHeader.FRAME_LENGTH + frameBodyBytes.length);
    frameHeaderAndBodyByteBuffer.put(frameHeader.toBytes());
    frameHeaderAndBodyByteBuffer.put(frameBodyBytes);

    // Generate the frame trailer.
    Cd11FrameTrailer frameTrailer = new Cd11FrameTrailer(
        config.getAuthenticationKeyIdentifier(), frameHeaderAndBodyByteBuffer.array());

    // Add the frame header and trailer.
    newFrame.setFrameHeader(frameHeader);
    newFrame.setFrameTrailer(frameTrailer);

    return newFrame;
  }

  /**
   * Generates a CD 1.1 Connection Response frame.
   *
   * @param ipAddress ip address of the requester
   * @param port port of the requester
   * @param secondIpAddress reserved for possible multicasting use
   * @param secondsPort reserved for possible multicasting use
   * @return CD 1.1 Connection Response frame.
   * @throws IllegalArgumentException Thrown on invalid input.
   */
  public Cd11ConnectionResponseFrame createCd11ConnectionResponseFrame(InetAddress ipAddress,
      int port, InetAddress secondIpAddress, Integer secondsPort) {

    return cd11FrameFactory.createCd11ConnectionResponseFrame(
        ipAddress, port, secondIpAddress, secondsPort);
  }

  //---------- CD 1.1 Frame Create-and-Send Methods ----------

  /**
   * Generates and sends a CD 1.1 Acknack frame.
   *
   * @param framesetAcked full name of the frame set being acknowledged (for example, "SG7:0")
   * @param lowestSeqNum lowest valid sequence number sent considered during the current connection
   * for the set (0 until a frame set is no longer empty)
   * @param highestSeqNum highest valid sequence number considered during the current connection for
   * the set (-1 until a frame set is no longer empty)
   * @param gaps each gap contains two long entries for start time and end time
   * @throws IOException If there is an I/O exception from the socket
   */
  public void sendCd11AcknackFrame(
      String framesetAcked, long lowestSeqNum, long highestSeqNum, long[] gaps)
      throws IOException {

    this.write(
        cd11FrameFactory.createCd11AcknackFrame(framesetAcked, lowestSeqNum, highestSeqNum, gaps));
  }

  /**
   * Generates and sends a CD 1.1 Alert frame.
   *
   * @param message The alert message.
   * @throws IOException If there is an I/O exception from the socket
   */
  public void sendCd11AlertFrame(String message)
      throws IOException {
    // Create and send the object.
    this.write(cd11FrameFactory.createCd11AlertFrame(message));
  }

  /**
   * Generates and sends a CD 1.1 Connection Request frame.
   * @throws IOException If there is an I/O exception from the socket
   */
  public void sendCd11ConnectionRequestFrame()
      throws IOException {
    // Create and send the object.
    this.write(this.createCd11ConnectionRequestFrame());
  }

  /**
   * Generates and sends a CD 1.1 Connection Response frame.
   *
   * @param ipAddress ip address of the requester
   * @param port port of the requester
   * @param secondIpAddress reserved for possible multicasting use
   * @param secondPort reserved for possible multicasting use
   * @throws IllegalArgumentException Thrown on invalid input.
   * @throws IOException If there is an I/O exception from the socket
   */
  public void sendCd11ConnectionResponseFrame(
      InetAddress ipAddress, int port, InetAddress secondIpAddress, Integer secondPort)
      throws IOException {
    // Create and send the object.
    this.write(
        this.createCd11ConnectionResponseFrame(ipAddress, port, secondIpAddress, secondPort));
  }

  /**
   * Generates and sends a CD 1.1 Data frame.
   *
   * @param channelSubframes the subframes for the channel to send
   * @param sequenceNumber the sequence for the frame header.
   * @throws IllegalArgumentException Thrown on invalid input.
   * @throws IOException If there is an I/O exception from the socket
   */
  public void sendCd11DataFrame(Cd11ChannelSubframe[] channelSubframes, long sequenceNumber)
      throws IOException {
    // Create and send the object.
    this.write(cd11FrameFactory.createCd11DataFrame(channelSubframes, sequenceNumber));
  }

  /**
   * Generates and sends a CD 1.1 Option Response frame.
   *
   * @param optionType the option type of the response frame
   * @param optionResponse the response for the response frame
   * @throws IllegalArgumentException Thrown on invalid input.
   * @throws IOException If there is an I/O exception from the socket
   */
  public void sendCd11OptionResponseFrame(int optionType, String optionResponse)
      throws IOException {
    // Create and send the object.
    Cd11OptionResponseFrame frame = cd11FrameFactory
        .createCd11OptionResponseFrame(optionType, optionResponse);
    logger.info("Sending option response: {}", frame);
    this.write(frame);
  }

  //---------- Misc ----------

  /**
   * Returns the name of the station or responder.
   *
   * @return Name of station or responder.
   */
  public String getStationOrResponderName() {
    return config.getStationOrResponderName();
  }

  /**
   * The total number of seconds since the last frame was read from the socket. (NOTE: This method
   * is thread safe.)
   *
   * @return Seconds since last contact.
   */
  public long secondsSinceLastContact() {
    long lastTimestamp = lastContactTimeNs.get();
    return (lastTimestamp > 0) ? (System.nanoTime() - lastTimestamp) / 1000000000 : 0;
  }

  /**
   * The total number of seconds since the last Acknack frame was sent out (from the local socket to
   * the remote socket). (NOTE: This method is thread safe.)
   *
   * @return Seconds since last Acknack sent.
   */
  public long secondsSinceLastAcknackSent() {
    long lastTimestamp = lastAcknackSentTimeNs.get();
    return (lastTimestamp > 0) ? (System.nanoTime() - lastTimestamp) / 1000000000 : 0;
  }

  /**
   * The total number of milliseconds since the last Data frame was sent out (from the local socket
   * to the remote socket). (NOTE: This method is thread safe.)
   *
   * @return Milliseconds since last Data sent.
   */
  public long millisecondsSinceLastDataSent() {
    long lastTimestamp = lastDataSentTimeNs.get();
    return (lastTimestamp > 0) ? (System.nanoTime() - lastTimestamp) / 1000000 : 0;
  }

  /**
   * Returns the frame creator, registered in the constructor.
   *
   * @return frame creator
   */
  public synchronized String getFrameCreator() {
    return config.getFrameCreator();
  }

  /**
   * Returns the framesetAcked,
   *
   * @return framesetAcked
   */
  public synchronized String getFramesetAcked() {
    return this.framesetAcked;
  }

  public void send(byte[] packet) throws IOException {
    synchronized (writeLock) {
      // Check that the socket is ready to use.
      if (!this.isConnected()) {
        throw new IllegalStateException(SOCKETNOTOPEN);
      }

      socketOut.write(packet);
      socketOut.flush();
    }

  }

}
