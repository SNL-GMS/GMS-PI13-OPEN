package gms.dataacquisition.stationreceiver.cd11.common.frames;

import static com.google.common.base.Preconditions.checkArgument;

import gms.dataacquisition.stationreceiver.cd11.common.Cd11Validator;
import gms.dataacquisition.stationreceiver.cd11.common.FrameUtilities;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Objects;


/**
 * Represents the CD-1.1 'Connection Request' frame. To establish a connection, a data provider
 * submits a Connection Request Frame to a well-known port. Major and minor version numbers
 * represent a protocol compliance specification. The major version value represents the first-order
 * revision number. The minor version value represents the second-order revision number. Connection
 * Request Frames employ a standard frame header containing a sequence number. Given the transitory
 * nature of the Connection Request Frame, sequence numbers are not assigned to these frames.
 */
public class Cd11ConnectionRequestFrame extends Cd11Frame {

  // See constructor javadoc for description of the fields.
  public final short majorVersion;
  public final short minorVersion;
  public final String stationName;    // Defined in CD11 spec as 8 bytes
  public final String stationType;    // Defined in CD11 spec as 4 bytes
  public final String serviceType;    // Defined in CD11 spec as 4 bytes
  public final int ipAddress;
  public final int port;
  public final int secondIpAddress;
  public final int secondPort;

  /**
   * The byte array length of a connection request frame.
   */
  public static final int FRAME_LENGTH = (Short.BYTES * 4) + (Integer.BYTES * 2) + 8 + 4 + 4;

  /**
   * Creates a connection request object from a byte frame.
   *
   * @param cd11ByteFrame CD 1.1 frame segments.
   * @throws IllegalArgumentException Thrown on invalid input.
   */
  public Cd11ConnectionRequestFrame(Cd11ByteFrame cd11ByteFrame) {
    super(cd11ByteFrame);

    ByteBuffer body = cd11ByteFrame.getFrameBodyByteBuffer();
    this.majorVersion = body.getShort();
    this.minorVersion = body.getShort();
    this.stationName = FrameUtilities.readBytesAsString(body, 8);
    this.stationType = FrameUtilities.readBytesAsString(body, 4);
    this.serviceType = FrameUtilities.readBytesAsString(body, 4);
    this.ipAddress = body.getInt();
    this.port = body.getChar(); // Convert "unsigned short" to a Java "int".
    this.secondIpAddress = body.getInt();
    this.secondPort = body.getChar(); // Convert "unsigned short" to a Java "int".

    validate();
  }

  /**
   * Creates a connection request frame with all arguments.
   *
   * @param majorVersion major protocol version requested by sender of the frame
   * @param minorVersion minor protocol version requested by the sender of the frame
   * @param stationName station requesting the connection
   * @param stationType requester type
   * @param serviceType TCP, UDP, and so on
   * @param ipAddress IP address of the requester (i.e. the Data Provider sending the request)
   * @param port port of the requester (i.e. the Data Provider sending the request)
   * @param secondIpAddress reserved for possible multicasting use
   * @param secondPort reserved for possible multicasting use
   * @throws IllegalArgumentException Thrown on invalid input.
   */
  public Cd11ConnectionRequestFrame(
      short majorVersion, short minorVersion,
      String stationName, String stationType, String serviceType,
      InetAddress ipAddress, int port,
      InetAddress secondIpAddress, Integer secondPort) {

    // Initialize base class.
    super(FrameType.CONNECTION_REQUEST);

    // Initialize properties and validate input.
    this.majorVersion = majorVersion;
    this.minorVersion = minorVersion;
    this.stationName = stationName;
    this.stationType = stationType;
    this.serviceType = serviceType;
    this.ipAddress = Cd11Validator.validIpAddress(ipAddress);
    this.port = port;
    this.secondIpAddress = (secondIpAddress != null) ?
        Cd11Validator.validIpAddress(secondIpAddress) : 0;
    this.secondPort = (secondPort != null) ? secondPort : 0;

    validate();
  }

  /**
   * Creates an connection request frame with all arguments for frame Parsing Utility.
   *
   * @param majorVersion major protocol version requested by sender of the frame
   * @param minorVersion minor protocol version requested by the sender of the frame
   * @param stationName station requesting the connection
   * @param stationType requester type
   * @param serviceType TCP, UDP, and so on
   * @param ipAddress IP address of the requester (i.e. the Data Provider sending the request)
   * @param port port of the requester (i.e. the Data Provider sending the request)
   * @param secondIpAddress reserved for possible multicasting use
   * @param secondPort reserved for possible multicasting use
   * @param partialFrame a partially parsed frame with a header, trailer and body bytes
   */
  public Cd11ConnectionRequestFrame(short majorVersion, short minorVersion, String stationName,
      String stationType, String serviceType, int ipAddress, int port,
      int secondIpAddress, int secondPort, PartialFrame partialFrame) {

    // Initialize the base class.
    super(partialFrame.getFrameHeader(), partialFrame.getFrameTrailer(), partialFrame.getFrameBodyBytes());

    // Initialize properties
    this.majorVersion = majorVersion;
    this.minorVersion = minorVersion;
    this.stationName = stationName;
    this.stationType = stationType;
    this.serviceType = serviceType;
    this.ipAddress = Cd11Validator.validIpAddress(ipAddress);
    this.port = port;
    this.secondIpAddress = secondIpAddress;
    this.secondPort = secondPort;
  }

  /**
   * Validates this object. Throws an exception if there are any problems with it's fields.
   */
  private void validate() {
    checkArgument(majorVersion >= 0);
    checkArgument(minorVersion >= 0);
    Cd11Validator.validStationOrResponderName(stationName);
    Cd11Validator.validServiceType(serviceType);
    Cd11Validator.validIpAddress(ipAddress);
    Cd11Validator.validPortNumber(port);
    if (secondIpAddress != 0) {
      Cd11Validator.validIpAddress(secondIpAddress);
    }
    Cd11Validator.validPortNumber(secondPort);
  }

  /**
   * Returns this connection request frame as bytes.
   *
   * @return byte[], representing the frame in wire format
   */
  @Override
  public byte[] getFrameBodyBytes() {
    ByteBuffer output = ByteBuffer.allocate(Cd11ConnectionRequestFrame.FRAME_LENGTH);
    output.putShort(this.majorVersion);
    output.putShort(this.minorVersion);
    output.put(FrameUtilities.padToLength(this.stationName, 8).getBytes());
    output.put(FrameUtilities.padToLength(this.stationType, 4).getBytes());
    output.put(FrameUtilities.padToLength(this.serviceType, 4).getBytes());
    output.putInt(this.ipAddress);
    output.putChar((char) this.port);
    output.putInt(this.secondIpAddress);
    output.putChar((char) this.secondPort);

    return output.array();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Cd11ConnectionRequestFrame that = (Cd11ConnectionRequestFrame) o;

    if (majorVersion != that.majorVersion) {
      return false;
    }
    if (minorVersion != that.minorVersion) {
      return false;
    }
    if (ipAddress != that.ipAddress) {
      return false;
    }
    if (port != that.port) {
      return false;
    }
    if (secondIpAddress != that.secondIpAddress) {
      return false;
    }
    if (secondPort != that.secondPort) {
      return false;
    }
    if (!Objects.equals(stationName, that.stationName)) {
      return false;
    }
    if (!Objects.equals(stationType, that.stationType)) {
      return false;
    }
    return Objects.equals(serviceType, that.serviceType);
  }

  @Override
  public int hashCode() {
    int result = majorVersion;
    result = 31 * result + (int) minorVersion;
    result = 31 * result + (stationName != null ? stationName.hashCode() : 0);
    result = 31 * result + (stationType != null ? stationType.hashCode() : 0);
    result = 31 * result + (serviceType != null ? serviceType.hashCode() : 0);
    result = 31 * result + ipAddress;
    result = 31 * result + port;
    result = 31 * result + secondIpAddress;
    result = 31 * result + secondPort;
    return result;
  }

  @Override
  public String toString() {
    return "Cd11CommandRequestFrame { " + "frameType: \"" + frameType + "\", "
        + "stationName: \"" + stationName + "\", "
        + "stationType: \"" + stationType + "\", "
        + "serviceType: \"" + serviceType + "\", "
        + "protocolMajorVersion: " + majorVersion + ", "
        + "protocolMinorVersion: " + minorVersion + ", "
        + "ipAddress: \"" + ipAddress + "\", "
        + "port: " + port + ", "
        + "secondIpAddress: \"" + secondIpAddress + "\", "
        + "secondPort: " + secondPort + " "
        + "}";
  }
}
