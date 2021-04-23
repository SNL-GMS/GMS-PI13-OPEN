package gms.dataacquisition.stationreceiver.cd11.common;

import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11AcknackFrame;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11AlertFrame;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11ByteFrame;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11ChannelSubframe;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11CommandRequestFrame;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11CommandResponseFrame;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11ConnectionRequestFrame;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11ConnectionResponseFrame;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11DataFrame;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11DummyFrame;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11Frame;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11Frame.FrameType;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11FrameHeader;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11FrameTrailer;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11OptionRequestFrame;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11OptionResponseFrame;
import gms.dataacquisition.stationreceiver.cd11.common.frames.CustomResetFrame;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Cd11FrameFactory {

  private static final Logger logger = LoggerFactory.getLogger(Cd11FrameFactory.class);

  private AtomicReference<String> framesetAcked = new AtomicReference<>();

  // Frame factory specific constants
  private static final int AUTHENTICATION_KEY_IDENTIFIER = 0;
  private static final String FRAME_CREATOR = "TEST";
  private static final String FRAME_DESTINATION = "0";
  private static final short PROTOCOL_MAJOR_VERSION = 1;
  private static final short PROTOCOL_MINOR_VERSION = 1;
  private static final String RESPONDER_NAME = "DC";
  private static final String RESPONDER_TYPE = "IDC";
  private static final String SERVICE_TYPE = "TCP";

  // private frame factory fields
  private int authenticationKeyIdentifier;
  private String frameCreator;
  private String frameDestination;
  private short protocolMajorVersion;
  private short protocolMinorVersion;
  private String responderName;
  private String responderType;
  private String serviceType;

  public static Cd11FrameFactory.Builder builderWithDefaults() {
    return new Builder()
            .setAuthenticationKeyIdentifier(AUTHENTICATION_KEY_IDENTIFIER)
            .setFrameCreator(FRAME_CREATOR)
            .setFrameDestination(FRAME_DESTINATION)
            .setProtocolMajorVersion(PROTOCOL_MAJOR_VERSION)
            .setProtocolMinorVersion(PROTOCOL_MINOR_VERSION)
            .setResponderName(RESPONDER_NAME)
            .setResponderType(RESPONDER_TYPE)
            .setServiceType(SERVICE_TYPE)
            .setFramesetAcked(String.format("%s:%s", FRAME_CREATOR, FRAME_DESTINATION));
  }

  public int getAuthenticationKeyIdentifier() {
    return authenticationKeyIdentifier;
  }

  public String getFrameCreator() {
    return frameCreator;
  }

  public String getFrameDestination() {
    return frameDestination;
  }

  public short getProtocolMajorVersion() {
    return protocolMajorVersion;
  }

  public short getProtocolMinorVersion() {
    return protocolMinorVersion;
  }

  public String getResponderName() {
    return responderName;
  }

  public String getResponderType() {
    return responderType;
  }

  public String getServiceType() {
    return serviceType;
  }

  public String getFramesetAcked() {return this.framesetAcked.get();}

  public void setFramesetAcked(String framesetAcked) {this.framesetAcked.set(framesetAcked);}

  /**
   * Frame factories are created for each station
   * Constructs a CD 1.1 Frame given a byte frame
   *
   * @param cd11ByteFrame Byte frame to be parsed
   * @return A Cd11Frame parsed from the byte frame
   */
  public Cd11Frame createCd11Frame(Cd11ByteFrame cd11ByteFrame) {

    // Construct the appropriate CD 1.1 frame.
    switch (cd11ByteFrame.getFrameType()) {
      case ACKNACK:
        return new Cd11AcknackFrame(cd11ByteFrame);
      case ALERT:
        return new Cd11AlertFrame(cd11ByteFrame);
      case CD_ONE_ENCAPSULATION:
        //todo implement the actual CD 1 encapsulation frame in frames, also see the method below for `createCd1EncapsulationFrame()`
        logger.warn(
            "CD 1 encapsulation frame not yet supported, creating a dummy frame with appropriate FrameType");
        return new Cd11DummyFrame(cd11ByteFrame.getFrameType());
      case COMMAND_REQUEST:
        //todo verify that Cd11CommandRequestFrame works
        return new Cd11CommandRequestFrame(cd11ByteFrame);
      case COMMAND_RESPONSE:
        //todo verify that Cd11CommandResponseFrame works
        return new Cd11CommandResponseFrame(cd11ByteFrame);
      case CONNECTION_REQUEST:
        return new Cd11ConnectionRequestFrame(cd11ByteFrame);
      case CONNECTION_RESPONSE:
        return new Cd11ConnectionResponseFrame(cd11ByteFrame);
      case DATA:
        return new Cd11DataFrame(cd11ByteFrame);
      case OPTION_REQUEST:
        return new Cd11OptionRequestFrame(cd11ByteFrame);
      case OPTION_RESPONSE:
        //todo verify that Cd11OptionResponseFrame works
        return new Cd11OptionResponseFrame(cd11ByteFrame);
      case CUSTOM_RESET_FRAME:
        return new CustomResetFrame(cd11ByteFrame);
      default:
        throw new IllegalArgumentException("Frame type does not exist.");
    }
  }

  //---------- CD 1.1 Frame Creation Methods ----------

  public Cd11AcknackFrame createCd11AcknackFrame(
      long lowestSeqNum, long highestSeqNum, long[] gaps){
    return this.createCd11AcknackFrame(this.framesetAcked.get(), lowestSeqNum, highestSeqNum, gaps);
  }

  /**
   * Generates a CD 1.1 Acknack frame.
   *
   * @param framesetAcked full name of the frame set being acknowledged (for example, "SG7:0")
   * @param lowestSeqNum lowest valid sequence number sent considered during the current connection
   * for the set (0 until a frame set is no longer empty)
   * @param highestSeqNum highest valid sequence number considered during the current connection for
   * the set (-1 until a frame set is no longer empty)
   * @param gaps each gap contains two long entries for start time and end time
   * @return CD 1.1 Acknack frame.
   * @throws IllegalArgumentException Thrown on invalid input.
   */
  public Cd11AcknackFrame createCd11AcknackFrame(
      String framesetAcked, long lowestSeqNum, long highestSeqNum, long[] gaps) {

    // Create the frame body.
    Cd11AcknackFrame newFrame = new Cd11AcknackFrame(
        framesetAcked, lowestSeqNum, highestSeqNum, gaps);

    // Generate the frame body byte array.
    byte[] frameBodyBytes = newFrame.getFrameBodyBytes();

    // Use the frame body byte array to generate the frame header.
    Cd11FrameHeader frameHeader = createFrameHeader(FrameType.ACKNACK, frameBodyBytes.length, 0);

    // Generate the frame header and body byte arrays.
    ByteBuffer frameHeaderAndBodyByteBuffer = ByteBuffer.allocate(
        Cd11FrameHeader.FRAME_LENGTH + frameBodyBytes.length);
    frameHeaderAndBodyByteBuffer.put(frameHeader.toBytes());
    frameHeaderAndBodyByteBuffer.put(frameBodyBytes);

    // Generate the frame trailer.
    Cd11FrameTrailer frameTrailer = createFrameTrailer(frameHeaderAndBodyByteBuffer.array());

    // Add the frame header and trailer.
    newFrame.setFrameHeader(frameHeader);
    newFrame.setFrameTrailer(frameTrailer);
    return newFrame;
  }

  /**
   * Generates a CD 1.1 Alert frame.
   *
   * @param message The alert message.
   * @return CD 1.1 Alert frame.
   * @throws IllegalArgumentException Thrown on invalid input.
   */
  public Cd11AlertFrame createCd11AlertFrame(String message) {

    // Create the frame body.
    Cd11AlertFrame newFrame = new Cd11AlertFrame(message);

    // Generate the frame body byte array.
    byte[] frameBodyBytes = newFrame.getFrameBodyBytes();

    // Use the frame body byte array to generate the frame header.
    Cd11FrameHeader frameHeader = createFrameHeader(FrameType.ALERT, frameBodyBytes.length, 0);

    // Generate the frame header and body byte arrays.
    ByteBuffer frameHeaderAndBodyByteBuffer = ByteBuffer.allocate(
        Cd11FrameHeader.FRAME_LENGTH + frameBodyBytes.length);
    frameHeaderAndBodyByteBuffer.put(frameHeader.toBytes());
    frameHeaderAndBodyByteBuffer.put(frameBodyBytes);

    // Generate the frame trailer.
    Cd11FrameTrailer frameTrailer = createFrameTrailer(frameHeaderAndBodyByteBuffer.array());

    // Add the frame header and trailer.
    newFrame.setFrameHeader(frameHeader);
    newFrame.setFrameTrailer(frameTrailer);

    return newFrame;
  }

  /**
   * Generates a CD 1.1 Connection Request frame.
   *
   * @param localIpAddress ip address of the requester
   * @param localPort port of the requester
   * @return CD 1.1 Connection Request frame.
   * @throws IllegalArgumentException Thrown on invalid input.
   */
  public Cd11ConnectionRequestFrame createCd11ConnectionRequestFrame(InetAddress localIpAddress,
      Integer localPort) {

    // Create the frame body.
    Cd11ConnectionRequestFrame newFrame = new Cd11ConnectionRequestFrame(
            protocolMajorVersion, protocolMinorVersion,
            responderName, responderType, serviceType,
            localIpAddress, localPort,
            null, null); // TODO: Support secondary IP and Port???

    // Generate the frame body byte array.
    byte[] frameBodyBytes = newFrame.getFrameBodyBytes();

    // Use the frame body byte array to generate the frame header.
    Cd11FrameHeader frameHeader = createFrameHeader(FrameType.CONNECTION_REQUEST,
            Cd11ConnectionRequestFrame.FRAME_LENGTH, 0);

    // Generate the frame header and body byte arrays.
    ByteBuffer frameHeaderAndBodyByteBuffer = ByteBuffer.allocate(
        Cd11FrameHeader.FRAME_LENGTH + frameBodyBytes.length);
    frameHeaderAndBodyByteBuffer.put(frameHeader.toBytes());
    frameHeaderAndBodyByteBuffer.put(frameBodyBytes);

    // Generate the frame trailer.
    Cd11FrameTrailer frameTrailer = createFrameTrailer(frameHeaderAndBodyByteBuffer.array());

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
  public Cd11ConnectionResponseFrame createCd11ConnectionResponseFrame(
      InetAddress ipAddress, int port, InetAddress secondIpAddress, Integer secondsPort) {

    // Create the frame body.
    Cd11ConnectionResponseFrame newFrame = new Cd11ConnectionResponseFrame(
            protocolMajorVersion, protocolMinorVersion,
            responderName, responderType, serviceType,
            ipAddress, port, secondIpAddress, secondsPort);

    // Generate the frame body byte array.
    byte[] frameBodyBytes = newFrame.getFrameBodyBytes();

    // Use the frame body byte array to generate the frame header.
    Cd11FrameHeader frameHeader = createFrameHeader(FrameType.CONNECTION_RESPONSE,
            Cd11ConnectionResponseFrame.FRAME_LENGTH, 0);

    // Generate the frame header and body byte arrays.
    ByteBuffer frameHeaderAndBodyByteBuffer = ByteBuffer.allocate(
        Cd11FrameHeader.FRAME_LENGTH + frameBodyBytes.length);
    frameHeaderAndBodyByteBuffer.put(frameHeader.toBytes());
    frameHeaderAndBodyByteBuffer.put(frameBodyBytes);

    // Generate the frame trailer.
    Cd11FrameTrailer frameTrailer = createFrameTrailer(frameHeaderAndBodyByteBuffer.array());

    // Add the frame header and trailer.
    newFrame.setFrameHeader(frameHeader);
    newFrame.setFrameTrailer(frameTrailer);

    return newFrame;
  }

  /**
   * Generates a CD 1.1 Data frame.
   *
   * @param channelSubframes the sub frames for the {@link Cd11DataFrame}
   * @param sequenceNumber   sequence number for the frame's header
   * @return CD 1.1 Data frame.
   * @throws IllegalArgumentException Thrown on invalid input.
   */
  public Cd11DataFrame createCd11DataFrame(
      Cd11ChannelSubframe[] channelSubframes, long sequenceNumber) {

    // Create the frame body.
    Cd11DataFrame newFrame = new Cd11DataFrame(channelSubframes);

    // Generate the frame body byte array.
    byte[] frameBodyBytes = newFrame.getFrameBodyBytes();

    // Use the frame body byte array to generate the frame header.
    Cd11FrameHeader frameHeader = createFrameHeader(FrameType.DATA, frameBodyBytes.length, sequenceNumber);

    // Generate the frame header and body byte arrays.
    ByteBuffer frameHeaderAndBodyByteBuffer = ByteBuffer.allocate(
        Cd11FrameHeader.FRAME_LENGTH + frameBodyBytes.length);
    frameHeaderAndBodyByteBuffer.put(frameHeader.toBytes());
    frameHeaderAndBodyByteBuffer.put(frameBodyBytes);

    // Generate the frame trailer.
    Cd11FrameTrailer frameTrailer = createFrameTrailer(frameHeaderAndBodyByteBuffer.array());

    // Add the frame header and trailer.
    newFrame.setFrameHeader(frameHeader);
    newFrame.setFrameTrailer(frameTrailer);

    return newFrame;
  }

  /**
   * Generates a CD 1.1 Option Request frame.
   *
   * @param optionType numeric identifier of option requested
   * @param optionRequest value of option, padded to be divisible by 4
   * @return CD 1.1 Option Request frame
   */
  public Cd11OptionRequestFrame createCd11OptionRequestFrame(int optionType, String optionRequest) {

    // Create the frame body.
    Cd11OptionRequestFrame newFrame = new Cd11OptionRequestFrame(optionType, optionRequest);

    // Generate the frame body byte array.
    byte[] frameBodyBytes = newFrame.getFrameBodyBytes();

    // Use the frame body byte array to generate the frame header.
    Cd11FrameHeader frameHeader = createFrameHeader(FrameType.OPTION_REQUEST, frameBodyBytes.length, 0);

    // Generate the frame header and body byte arrays.
    ByteBuffer frameHeaderAndBodyByteBuffer = ByteBuffer.allocate(
        Cd11FrameHeader.FRAME_LENGTH + frameBodyBytes.length);
    frameHeaderAndBodyByteBuffer.put(frameHeader.toBytes());
    frameHeaderAndBodyByteBuffer.put(frameBodyBytes);

    // Generate the frame trailer.
    Cd11FrameTrailer frameTrailer = createFrameTrailer(frameHeaderAndBodyByteBuffer.array());

    // Add the frame header and trailer.
    newFrame.setFrameHeader(frameHeader);
    newFrame.setFrameTrailer(frameTrailer);

    return newFrame;
  }

  /**
   * Generates a CD 1.1 Option Response frame.
   *
   * @param optionType numeric identifier of option requested
   * @param optionResponse value of option, padded to be divisible by 4
   * @return CD 1.1 Option Response frame
   */
  public Cd11OptionResponseFrame createCd11OptionResponseFrame(int optionType,
      String optionResponse) {

    // Create the frame body.
    Cd11OptionResponseFrame newFrame = new Cd11OptionResponseFrame(optionType, optionResponse);

    // Generate the frame body byte array.
    byte[] frameBodyBytes = newFrame.getFrameBodyBytes();

    // Use the frame body byte array to generate the frame header.
    Cd11FrameHeader frameHeader = createFrameHeader(FrameType.OPTION_RESPONSE, frameBodyBytes.length, 0);

    // Generate the frame header and body byte arrays.
    ByteBuffer frameHeaderAndBodyByteBuffer = ByteBuffer.allocate(
        Cd11FrameHeader.FRAME_LENGTH + frameBodyBytes.length);
    frameHeaderAndBodyByteBuffer.put(frameHeader.toBytes());
    frameHeaderAndBodyByteBuffer.put(frameBodyBytes);

    // Generate the frame trailer.
    Cd11FrameTrailer frameTrailer = createFrameTrailer(frameHeaderAndBodyByteBuffer.array());

    // Add the frame header and trailer.
    newFrame.setFrameHeader(frameHeader);
    newFrame.setFrameTrailer(frameTrailer);

    return newFrame;
  }

  /**
   * Generates a CD 1.1 Command Request frame.
   *
   * @param stationName station name for the frame
   * @param site site for the station
   * @param channel channel for the frame
   * @param locName location name
   * @param timestamp time stamp
   * @return CD 1.1 Command Request Frame
   */
  public Cd11CommandRequestFrame createCd11CommandRequestFrame(
      String stationName, String site, String channel, String locName,
      Instant timestamp, String commandMessage) {

    // Create the frame body.
    Cd11CommandRequestFrame newFrame = new Cd11CommandRequestFrame(
        stationName, site, channel, locName, timestamp, commandMessage);

    // Generate the frame body byte array.
    byte[] frameBodyBytes = newFrame.getFrameBodyBytes();

    // Use the frame body byte array to generate the frame header.
    Cd11FrameHeader frameHeader = createFrameHeader(FrameType.COMMAND_REQUEST, frameBodyBytes.length, 0);

    // Generate the frame header and body byte arrays.
    ByteBuffer frameHeaderAndBodyByteBuffer = ByteBuffer.allocate(
        Cd11FrameHeader.FRAME_LENGTH + frameBodyBytes.length);
    frameHeaderAndBodyByteBuffer.put(frameHeader.toBytes());
    frameHeaderAndBodyByteBuffer.put(frameBodyBytes);

    // Generate the frame trailer.
    Cd11FrameTrailer frameTrailer = createFrameTrailer(frameHeaderAndBodyByteBuffer.array());

    // Add the frame header and trailer.
    newFrame.setFrameHeader(frameHeader);
    newFrame.setFrameTrailer(frameTrailer);

    return newFrame;
  }

  /**
   * Generates a CD 1.1 Command Response frame.
   *
   * @param responderStation station name
   * @param site site
   * @param channel channel
   * @param locName location name
   * @param timestamp time stamp
   * @param commandRequestMessage original command request sent from the Data Consumer
   * @param responseMessage response message
   * @return CD 1.1 Command Response Frame
   */
  public Cd11CommandResponseFrame createCd11CommandResponseFrame(
      String responderStation, String site, String channel, String locName,
      Instant timestamp, String commandRequestMessage, String responseMessage) {
    return this.createCd11CommandResponseFrame(responderStation, site, channel, locName, timestamp,
        commandRequestMessage, responseMessage, 0);
  }

  public Cd11CommandResponseFrame createCd11CommandResponseFrame(String responderStation,
      String site, String channel, String locName, Instant timestamp, String commandRequestMessage,
      String responseMessage, int sequenceNumber) {

    // Create the frame body.
    Cd11CommandResponseFrame newFrame = new Cd11CommandResponseFrame(
        responderStation, site, channel, locName, timestamp, commandRequestMessage,
        responseMessage);

    // Generate the frame body byte array.
    byte[] frameBodyBytes = newFrame.getFrameBodyBytes();

    // Use the frame body byte array to generate the frame header.
    Cd11FrameHeader frameHeader = createFrameHeader(FrameType.COMMAND_RESPONSE,
        frameBodyBytes.length, sequenceNumber);

    // Generate the frame header and body byte arrays.
    ByteBuffer frameHeaderAndBodyByteBuffer = ByteBuffer.allocate(
        Cd11FrameHeader.FRAME_LENGTH + frameBodyBytes.length);
    frameHeaderAndBodyByteBuffer.put(frameHeader.toBytes());
    frameHeaderAndBodyByteBuffer.put(frameBodyBytes);

    // Generate the frame trailer.
    Cd11FrameTrailer frameTrailer = createFrameTrailer(frameHeaderAndBodyByteBuffer.array());

    // Add the frame header and trailer.
    newFrame.setFrameHeader(frameHeader);
    newFrame.setFrameTrailer(frameTrailer);

    return newFrame;
  }

  /**
   * Generates a Custom Reset Frame.
   *
   * @return Custom Reset Frame.
   * @throws IllegalArgumentException Thrown on invalid input.
   */
  public CustomResetFrame createCustomResetFrame() {

    // Create the frame body.
    CustomResetFrame newFrame = new CustomResetFrame("".getBytes());

    // Generate the frame body byte array.
    byte[] frameBodyBytes = newFrame.getFrameBodyBytes();

    // Use the frame body byte array to generate the frame header.
    Cd11FrameHeader frameHeader = createFrameHeader(FrameType.CUSTOM_RESET_FRAME, frameBodyBytes.length, 0);

    // Generate the frame header and body byte arrays.
    ByteBuffer frameHeaderAndBodyByteBuffer = ByteBuffer.allocate(
        Cd11FrameHeader.FRAME_LENGTH + frameBodyBytes.length);
    frameHeaderAndBodyByteBuffer.put(frameHeader.toBytes());
    frameHeaderAndBodyByteBuffer.put(frameBodyBytes);

    // Generate the frame trailer.
    Cd11FrameTrailer frameTrailer = createFrameTrailer(frameHeaderAndBodyByteBuffer.array());

    // Add the frame header and trailer.
    newFrame.setFrameHeader(frameHeader);
    newFrame.setFrameTrailer(frameTrailer);

    return newFrame;
  }

  /**
   * Generates a Custom Reset Frame.
   *
   * @return Custom Reset Frame.
   * @throws IllegalArgumentException Thrown on invalid input.
   */
  public Cd11Frame createCd1EncapsulationFrame() {

    // Create the frame body.
    Cd11Frame newFrame = new Cd11DummyFrame(FrameType.CD_ONE_ENCAPSULATION);

    // Generate the frame body byte array.
    byte[] frameBodyBytes = newFrame.getFrameBodyBytes();

    // Use the frame body byte array to generate the frame header.
    Cd11FrameHeader frameHeader = createFrameHeader(FrameType.CD_ONE_ENCAPSULATION, frameBodyBytes.length, 0);

    // Generate the frame header and body byte arrays.
    ByteBuffer frameHeaderAndBodyByteBuffer = ByteBuffer.allocate(
        Cd11FrameHeader.FRAME_LENGTH + frameBodyBytes.length);
    frameHeaderAndBodyByteBuffer.put(frameHeader.toBytes());
    frameHeaderAndBodyByteBuffer.put(frameBodyBytes);

    // Generate the frame trailer.
    Cd11FrameTrailer frameTrailer = createFrameTrailer(frameHeaderAndBodyByteBuffer.array());

    // Add the frame header and trailer.
    newFrame.setFrameHeader(frameHeader);
    newFrame.setFrameTrailer(frameTrailer);

    return newFrame;
  }

  /* ------------------------------------------------------------
   * Private Methods
     ------------------------------------------------------------ */

  /**
   * Private Cd11FrameFactory constructor using the Cd11FrameFactory.Builder
   * @param builder - CD11FrameFactory builder
   */
  private Cd11FrameFactory(Builder builder) {
    this.authenticationKeyIdentifier = builder.authenticationKeyIdentifier;
    this.frameCreator = builder.frameCreator;
    this.frameDestination = builder.frameDestination;
    this.protocolMajorVersion = builder.protocolMajorVersion;
    this.protocolMinorVersion = builder.protocolMinorVersion;
    this.responderName = builder.responderName;
    this.responderType = builder.responderType;
    this.serviceType = builder.serviceType;
    this.framesetAcked.set(builder.framesetAcked);

  }

  /**
   * Create Cd11FrameHeader
   * @param frameType - type
   * @param bodyLength - byte body length
   * @param seqNum - sequence number
   * @return Cd11FrameHeader
   */
  private Cd11FrameHeader createFrameHeader(FrameType frameType, int bodyLength, long seqNum) {
    return new Cd11FrameHeader(
            frameType,Cd11FrameHeader.FRAME_LENGTH + bodyLength,
            frameCreator, frameDestination, seqNum);
  }

  /**
   * Create Cd11FrameTrailer
   * @param frameHeaderAndBody - frame header and body byte buffer
   * @return Cd11FrameTrailer
   */
  private Cd11FrameTrailer createFrameTrailer(byte[] frameHeaderAndBody) {
    return new Cd11FrameTrailer(authenticationKeyIdentifier, frameHeaderAndBody);
  }

  /* ------------------------------------------------------------
   * Private Classes
     ------------------------------------------------------------ */

  public static class Builder {

    // private frame factory fields
    private int authenticationKeyIdentifier;
    private String frameCreator;
    private String frameDestination;
    private short protocolMajorVersion;
    private short protocolMinorVersion;
    private String responderName;
    private String responderType;
    private String serviceType;
    private String framesetAcked;

    public Builder() {
      // Empty Constructor
    }

    public Builder setAuthenticationKeyIdentifier(int authenticationKeyIdentifier) {
      this.authenticationKeyIdentifier = authenticationKeyIdentifier;
      return this;
    }

    public Builder setFrameCreator(String frameCreator) {
      this.frameCreator = frameCreator;
      return this;
    }

    public Builder setFrameDestination(String frameDestination) {
      this.frameDestination = frameDestination;
      return this;
    }

    public Builder setProtocolMajorVersion(short protocolMajorVersion) {
      this.protocolMajorVersion = protocolMajorVersion;
      return this;
    }

    public Builder setProtocolMinorVersion(short protocolMinorVersion) {
      this.protocolMinorVersion = protocolMinorVersion;
      return this;
    }

    public Builder setResponderName(String responderName) {
      this.responderName = responderName;
      return this;
    }

    public Builder setResponderType(String responderType) {
      this.responderType = responderType;
      return this;
    }

    public Builder setServiceType(String serviceType) {
      this.serviceType = serviceType;
      return this;
    }

    public Builder setFramesetAcked(String framesetAcked) {
      this.framesetAcked=framesetAcked;
      return this;
    }

    public Cd11FrameFactory build() {
      return new Cd11FrameFactory(this);
    }
  }
}
