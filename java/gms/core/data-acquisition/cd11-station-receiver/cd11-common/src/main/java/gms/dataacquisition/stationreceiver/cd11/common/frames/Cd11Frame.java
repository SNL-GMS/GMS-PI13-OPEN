package gms.dataacquisition.stationreceiver.cd11.common.frames;

import static com.google.common.base.Preconditions.checkNotNull;

import gms.dataacquisition.stationreceiver.cd11.common.CRC64;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Base class for all CD 1.1 Frame classes.
 */
public abstract class Cd11Frame {

  public final FrameType frameType;
  private final byte[] rawNetworkBytes;
  private Cd11FrameHeader header = null;
  private Cd11FrameTrailer trailer = null;

  private static final Map<Class<?>, FrameType> frameTypeToClass = Collections
      .unmodifiableMap(Map.ofEntries(
          Map.entry(Cd11ConnectionRequestFrame.class, FrameType.CONNECTION_REQUEST),
          Map.entry(Cd11ConnectionResponseFrame.class, FrameType.CONNECTION_RESPONSE),
          Map.entry(Cd11OptionRequestFrame.class, FrameType.OPTION_REQUEST),
          Map.entry(Cd11OptionResponseFrame.class, FrameType.OPTION_RESPONSE),
          Map.entry(Cd11DataFrame.class, FrameType.DATA),
          Map.entry(Cd11AcknackFrame.class, FrameType.ACKNACK),
          Map.entry(Cd11AlertFrame.class, FrameType.ALERT),
          Map.entry(Cd11CommandResponseFrame.class, FrameType.COMMAND_RESPONSE),
          Map.entry(Cd11CommandRequestFrame.class, FrameType.COMMAND_REQUEST),
          Map.entry(CustomResetFrame.class, FrameType.CUSTOM_RESET_FRAME)
      ));

  /**
   * Enumeration for each CD 1.1 frame type.
   */
  public enum FrameType {

    CONNECTION_REQUEST(1),
    CONNECTION_RESPONSE(2),
    OPTION_REQUEST(3),
    OPTION_RESPONSE(4),
    DATA(5),
    ACKNACK(6),
    ALERT(7),
    COMMAND_REQUEST(8),
    COMMAND_RESPONSE(9),
    CD_ONE_ENCAPSULATION(13),
    CUSTOM_RESET_FRAME(26),
    MALFORMED_FRAME( 27);

    private final int value;

    FrameType(final int newValue) {
      value = newValue;
    }

    public int getValue() {
      return value;
    }

    public static FrameType fromInt(int value) {
      switch (value) {
        case 1:
          return CONNECTION_REQUEST;
        case 2:
          return CONNECTION_RESPONSE;
        case 3:
          return OPTION_REQUEST;
        case 4:
          return OPTION_RESPONSE;
        case 5:
          return DATA;
        case 6:
          return ACKNACK;
        case 7:
          return ALERT;
        case 8:
          return COMMAND_REQUEST;
        case 9:
          return COMMAND_RESPONSE;
        case 13:
          return CD_ONE_ENCAPSULATION;
        case 26:
          return CUSTOM_RESET_FRAME;
        case 27:
          return MALFORMED_FRAME;
        default:
          throw new IllegalArgumentException(String.format(
              "Integer value %1$d does not map to a Cd11FrameTypeIdentifier enumeration.", value));
      }
    }

    /**
     * Returns a string containing a comma separated list of valid enumeration values.
     *
     * @return Comma separated list of valid values.
     */
    public static String validValues() {
      StringBuilder validValues = new StringBuilder();
      for (FrameType fti : FrameType.values()) {
        if (validValues.length() > 0) {
          validValues.append(", ");
        }
        validValues.append(fti.toString());
      }
      return validValues.toString();
    }

    /**
     * Returns a string containing a comma separated list of valid integers.
     *
     * @return Comma separated list of valid integers.
     */
    public static String validIntValues() {
      StringBuilder validValues = new StringBuilder();
      for (FrameType fti : FrameType.values()) {
        if (validValues.length() > 0) {
          validValues.append(", ");
        }
        validValues.append(fti.getValue());
      }
      return validValues.toString();
    }
  }

  /**
   * Base class constructor for CD 1.1 frames constructed in-memory.
   *
   * @param frameType The frame type.
   * @throws NullPointerException Thrown when input is null.
   */
  public Cd11Frame(FrameType frameType) {
    checkNotNull(frameType);
    this.frameType = Objects.requireNonNull(frameType);
    this.rawNetworkBytes = new byte[]{}; // Since this was constructed from memory, and not read from the network.
  }

  /**
   * Base class constructor for CD 1.1 frames constructed in-memory.
   *
   * @param frameType The frame type.
   * @throws NullPointerException Thrown when input is null.
   */
  public Cd11Frame(FrameType frameType, Cd11FrameHeader header, Cd11FrameTrailer trailer) {
    this.frameType = Objects.requireNonNull(frameType);
    this.rawNetworkBytes = null; // Since this was constructed from memory, and not read from the network.
    this.setFrameHeader(header);
    this.setFrameTrailer(trailer);
  }

  /**
   * Base class constructor for CD 1.1 frames constructed from a byte array.
   *
   */
  public Cd11Frame(Cd11ByteFrame byteFrame) {
    this.frameType = byteFrame.getFrameType();
    this.rawNetworkBytes = byteFrame.getRawReceivedBytes();
    this.setFrameHeader(new Cd11FrameHeader(byteFrame.getFrameHeaderByteBuffer()));
    this.setFrameTrailer(new Cd11FrameTrailer(
        byteFrame.getFrameTrailerSegment1ByteBuffer(),
        byteFrame.getFrameTrailerSegment2ByteBuffer()));
  }

  /**
   * Construct Cd11Frame from a given Header, Trailer and Body bytes.
   *
   */
  public Cd11Frame(Cd11FrameHeader frameHeader, Cd11FrameTrailer frameTrailer, byte[] bodyBytes, boolean isMalformed){

    byte[] headerBytes=new byte[0];
    byte[] trailerBytes=new byte[0];

    bodyBytes = (bodyBytes == null) ? new byte[0] : bodyBytes;

    if(isMalformed) {
      this.frameType = FrameType.MALFORMED_FRAME;
    }else if(frameHeader != null){
      this.frameType = frameHeader.frameType;
    } else{
      this.frameType = FrameType.MALFORMED_FRAME;
    }

    if(frameHeader!=null) {
      this.setFrameHeader(frameHeader);
      headerBytes = frameHeader.toBytes();
    }else if (!isMalformed){
      throw new IllegalArgumentException(
        "The Frame header cannot be null for frames other than malformed frames.");
    }

    if(frameTrailer!=null){
      this.setFrameTrailer(frameTrailer);
      trailerBytes=frameTrailer.toBytes();
    }else if (!isMalformed){
      throw new IllegalArgumentException(
          "The Frame trailer cannot be null for frames other than malformed frames.");
    }

    this.rawNetworkBytes = ByteBuffer.allocate(headerBytes.length+
        bodyBytes.length+trailerBytes.length)
        .put(headerBytes)
        .put(bodyBytes)
        .put(trailerBytes)
        .array();
  }

  /**
   * Construct Cd11Frame from a given Header, Trailer and Body bytes.
   * frameHeader and frameTrailer must not be null
   */
  public Cd11Frame(Cd11FrameHeader frameHeader, Cd11FrameTrailer frameTrailer, byte[] bodyBytes){


    bodyBytes = (bodyBytes == null) ? new byte[0] : bodyBytes;

    this.frameType = frameHeader.frameType;
    this.setFrameHeader(frameHeader);
    this.setFrameTrailer(frameTrailer);

    byte[] headerBytes = frameHeader.toBytes();
    byte[] trailerBytes = frameTrailer.toBytes();

    this.rawNetworkBytes = ByteBuffer.allocate(headerBytes.length+
        bodyBytes.length+trailerBytes.length)
        .put(headerBytes)
        .put(bodyBytes)
        .put(trailerBytes)
        .array();
  }


  /**
   * True if the frame header has been set, otherwise false.
   *
   * @return True if frame header exists, otherwise false.
   */
  public final boolean frameHeaderExists() {
    return (this.header != null);
  }

  /**
   * True if the frame trailer has been set, otherwise false.
   *
   * @return True if frame trailer exists, otherwise false.
   */
  public final boolean frameTrailerExists() {
    return (this.trailer != null);
  }

  /**
   * Returns the frame header, if one has been set.
   *
   * @return CD 1.1 frame header.
   * @throws IllegalStateException Thrown if header has not yet been set.
   */
  public final Cd11FrameHeader getFrameHeader() {
    if (this.header == null) {
      throw new IllegalStateException("Frame header has not yet been set.");
    }
    return this.header;
  }

  /**
   * Sets the frame header, if it has not already been set.
   *
   * @throws IllegalStateException Thrown if header has already been set.
   */
  public final void setFrameHeader(Cd11FrameHeader cd11FrameHeader) {

    if (this.header != null) {
      throw new IllegalStateException("Frame header has already been set.");
    }

    // Check that the frame type matches.
    if (this.frameType != cd11FrameHeader.frameType && this.frameType != FrameType.MALFORMED_FRAME) {
      throw new IllegalArgumentException(
          "The Frame Type of the header received does not match the frame type of this object.");
    }

    this.header = cd11FrameHeader;
  }

  /**
   * Returns the frame trailer, if one has been set.
   *
   * @return CD 1.1 frame trailer.
   * @throws IllegalStateException Thrown if trailer has not yet been set.
   */
  public final Cd11FrameTrailer getFrameTrailer() {
    if (this.trailer == null) {
      throw new IllegalStateException("Frame trailer has not yet been set.");
    }
    return this.trailer;
  }

  /**
   * Sets the frame trailer, if it has not already been set.
   *
   * @throws IllegalStateException Thrown if trailer has already been set.
   */
  public final void setFrameTrailer(Cd11FrameTrailer cd11FrameTrailer) {
    if (this.trailer != null) {
      throw new IllegalStateException("Frame trailer has already been set.");
    }
    this.trailer = cd11FrameTrailer;
  }

  /**
   * Returns a byte array representing the entire CD 1.1 frame (header, body, and trailer). NOTE:
   * This method can only be called when a fully constructed frame trailer has been set.
   *
   * @return Byte array representing the full CD 1.1 frame.
   * @throws IllegalStateException Thrown when the frame header or trailer have not been set.
   * @throws IOException           Thrown if the full frame cannot be serialized.
   */
  public final byte[] toBytes() throws IOException {
    byte[] headerBytes;
    byte[] trailerBytes;

    if (this.header == null && this.frameType!=FrameType.MALFORMED_FRAME) {
      throw new IllegalStateException("Frame header has not been set.");
    }else if (this.header == null){
      headerBytes=new byte[0];
    }else {
      headerBytes=this.header.toBytes();
    }

    if (this.trailer == null && this.frameType!=FrameType.MALFORMED_FRAME) {
      throw new IllegalStateException("Frame trailer has not been set.");
    }else if (this.trailer == null){
      trailerBytes=new byte[0];
    }else {
      trailerBytes=this.trailer.toBytes();
    }

    ByteArrayOutputStream frameBytes = new ByteArrayOutputStream(headerBytes.length+trailerBytes.length);
    frameBytes.write(headerBytes);
    frameBytes.write(this.getFrameBodyBytes());
    frameBytes.write(trailerBytes);

    return frameBytes.toByteArray();
  }

  /**
   * Returns the raw bytes read from the network, when the CD 1.1 frame object is constructed from
   * network bytes. NOTE: When the CD 1.1 frame is constructed in-memory, this method returns null.
   *
   * @return Byte array of network bytes, or null.
   */
  public final byte[] getRawNetworkBytes() {
    return rawNetworkBytes;
  }

  /**
   * Calculate the CRC over the entire frame and compare with the CRC in the frame footer.
   *
   * @return TRUE if CRC is verified, otherwise FALSE.  If there is an IO error generating the bytes
   * to compute the CRC value, false is returned (i.e. this method does not throw IOException)
   */
  public boolean isValidCRC() {
    try {
      byte[] curRawNetworkBytes = this.getRawNetworkBytes();
      byte[] bytes = (curRawNetworkBytes == null) ? this.toBytes() : curRawNetworkBytes;

      // Replace commverification bytes with all zeros before we computing the CRC.
      for (int i = (bytes.length - Long.BYTES); i < bytes.length; i++) {
        bytes[i] = 0;
      }

      // Compute the CRC value.
      return CRC64.isValidCrc(
          bytes, bytes.length,
          this.getFrameTrailer().commVerification);
    } catch (IOException e) {
      return false;
    }
  }

  public FrameType getFrameType() {
    return frameType;
  }

  /**
   * Validates this Cd11Frame as a specific type of frame, and checks that the header FrameType
   * matches expected for that class of frame.
   *
   * @param clazz the type the frame is expected to be; determines the return type
   * @param <T>   the type of the return value, determined by clazz
   * @return the frame casted into the desired type
   * @throws IllegalArgumentException if the frame or clazz is null, if there is no class
   *                                  implementation known for the type of frame, or if the
   *                                  FrameType in the header of the frame is unknown or not as
   *                                  expected for the requested class.
   */
  public <T> T asFrameType(Class<T> clazz) {
    checkNotNull(clazz);

    //if class is not a Partial Frame class, make sure that its class matches the frame type
    if(clazz!=PartialFrame.class){
      // Get expected FrameType
      FrameType expectedFrameType = Cd11Frame.frameTypeToClass.get(clazz);
      checkNotNull(expectedFrameType,
          "Could not find expected FrameType for class " + clazz.getName());
      // Check that the expected and actual FrameType match, otherwise throw an exception.
      if (this.frameType != expectedFrameType) {
        throw new IllegalArgumentException(String.format(
            "Wrong type of frame (expected frameType %s, received %s).",
            expectedFrameType, this.frameType));
      }
    }
    if (!clazz.isInstance(this)) {
      throw new IllegalArgumentException(String.format(
          "Frame of wrong type; expected %s but got %s",
          clazz.getName(), this.getClass().getName()));
    }
    return clazz.cast(this);
  }

  /**
   * Returns a byte array representing the frame body (must be implemented by inheriting class.
   *
   * @return Byte array representing the frame body.
   */
  public abstract byte[] getFrameBodyBytes();
}
