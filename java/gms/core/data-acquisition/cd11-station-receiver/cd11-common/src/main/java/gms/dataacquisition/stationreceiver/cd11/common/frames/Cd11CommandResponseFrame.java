package gms.dataacquisition.stationreceiver.cd11.common.frames;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import gms.dataacquisition.stationreceiver.cd11.common.FrameUtilities;
import java.nio.ByteBuffer;
import java.time.Instant;


public class Cd11CommandResponseFrame extends Cd11Frame {

  public final String responderStation;
  public final String site;
  public final String channel;
  public final String locName;
  public final Instant timestamp;
  public final String commandRequestMessage;
  public final String responseMessage;

  /**
   * Creates a command response object from a byte frame.
   *
   * @param cd11ByteFrame CD 1.1 frame segments.
   * @throws IllegalArgumentException Thrown on invalid input.
   */
  public Cd11CommandResponseFrame(Cd11ByteFrame cd11ByteFrame) {
    super(cd11ByteFrame);

    ByteBuffer body = cd11ByteFrame.getFrameBodyByteBuffer();
    this.responderStation = FrameUtilities.readBytesAsString(body, 8);
    this.site = FrameUtilities.readBytesAsString(body, 5);
    this.channel = FrameUtilities.readBytesAsString(body, 3);
    this.locName = FrameUtilities.readBytesAsString(body, 2);
    body.position(body.position() + 2); // Skip two null bytes.
    this.timestamp = FrameUtilities.jdToInstant(FrameUtilities.readBytesAsString(body, 20));
    int commandRequestMessageSize = body.getInt();
    this.commandRequestMessage = FrameUtilities.readBytesAsString(body, commandRequestMessageSize);
    int responseMessageSize = body.getInt();
    this.responseMessage = FrameUtilities.readBytesAsString(body, responseMessageSize);

    this.validate();
  }

  /**
   * Creates a command response object with all arguments.
   *
   * @param responderStation station name
   * @param site site
   * @param channel channel
   * @param locName location name
   * @param timestamp time stamp
   * @param commandRequestMessage original command request sent from the Data Consumer
   * @param responseMessage response message
   * @throws IllegalArgumentException Thrown on invalid input.
   */
  public Cd11CommandResponseFrame(
      String responderStation, String site, String channel, String locName,
      Instant timestamp, String commandRequestMessage, String responseMessage) {

    super(FrameType.COMMAND_RESPONSE);

    this.responderStation = responderStation;
    this.site = site;
    this.channel = channel;
    this.locName = locName;
    this.timestamp = timestamp;
    this.commandRequestMessage = commandRequestMessage;
    this.responseMessage = responseMessage;

    this.validate();
  }

  /**
   * Creates an Cd11CommandRequest frame with all arguments for frame Parsing Utility.
   *
   * @param responderStation station name
   * @param site site
   * @param channel channel
   * @param locName location name
   * @param timestamp time stamp
   * @param commandRequestMessage original command request sent from the Data Consumer
   * @param responseMessage response message
   * @param partialFrame a partially parsed frame with a header, trailer and body bytes
   */
  public Cd11CommandResponseFrame(String responderStation, String site, String channel,
      String locName, Instant timestamp, String commandRequestMessage, String responseMessage,
      PartialFrame partialFrame) {

    // Initialize the base class.
    super(partialFrame.getFrameHeader(), partialFrame.getFrameTrailer(), partialFrame.getFrameBodyBytes());

    // Initialize properties
    this.responderStation = responderStation;
    this.site = site;
    this.channel = channel;
    this.locName = locName;
    this.timestamp = timestamp;
    this.commandRequestMessage = commandRequestMessage;
    this.responseMessage = responseMessage;

  }

  private void validate() {
    checkNotNull(responderStation);
    checkArgument(responderStation.length() <= 8);

    checkNotNull(site);
    checkArgument(site.length() <= 5);

    checkNotNull(channel);
    checkArgument(channel.length() <= 3);

    checkNotNull(locName);
    checkArgument(locName.length() <= 2);

    checkNotNull(timestamp);

    checkArgument(!commandRequestMessage.isBlank());

    checkArgument(!responseMessage.isBlank());
  }

  /**
   * Returns this connection request frame as bytes.
   *
   * @return byte[], representing the frame in wire format
   */
  @Override
  public byte[] getFrameBodyBytes() {
    int frameSize = 8 + 5 + 3 + 2 + 2 + 20 +
        Integer.BYTES + commandRequestMessage.length() +
        Integer.BYTES + responseMessage.length();

    ByteBuffer output = ByteBuffer.allocate(frameSize);
    output.put(FrameUtilities.padToLength(responderStation, 8).getBytes());
    output.put(FrameUtilities.padToLength(site, 5).getBytes());
    output.put(FrameUtilities.padToLength(channel, 3).getBytes());
    output.put(FrameUtilities.padToLength(locName, 2).getBytes());
    output.put((byte) 0); // Null byte.
    output.put((byte) 0); // Null byte.
    output.put(FrameUtilities.instantToJd(timestamp).getBytes());
    output.putInt(commandRequestMessage.length());
    output.put(commandRequestMessage.getBytes());
    output.putInt(responseMessage.length());
    output.put(responseMessage.getBytes());

    return output.array();
  }

  @Override
  public int hashCode() {
    return this.toString().hashCode();
  }

  @Override
  public String toString() {
    return "Cd11CommandResponseFrame { " + "frameType: \"" + frameType + "\", "
        + "responderStation: \"" + responderStation + "\", "
        + "site: \"" + site + "\", "
        + "channel: \"" + channel + "\", "
        + "locName: \"" + locName + "\", "
        + "timestamp: \"" + timestamp + "\", "
        + "commandRequestMessageSize: " + commandRequestMessage.length() + ", "
        + "commandRequestMessage: \"" + commandRequestMessage + "\", "
        + "responseMessageSize: " + responseMessage.length() + ", "
        + "responseMessage: \"" + responseMessage + "\" "
        + "}";
  }
}
