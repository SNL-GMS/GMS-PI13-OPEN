package gms.dataacquisition.stationreceiver.cd11.common.frames;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import gms.dataacquisition.stationreceiver.cd11.common.FrameUtilities;
import java.nio.ByteBuffer;
import java.time.Instant;


public class Cd11CommandRequestFrame extends Cd11Frame {

  public final String stationName;
  public final String site;
  public final String channel;
  public final String locName;
  public final Instant timestamp;
  public final String commandMessage;

  /**
   * Creates an command request object from a byte frame.
   *
   * @param cd11ByteFrame CD 1.1 frame segments.
   * @throws IllegalArgumentException Thrown on invalid input.
   */
  public Cd11CommandRequestFrame(Cd11ByteFrame cd11ByteFrame) {
    super(cd11ByteFrame);

    ByteBuffer body = cd11ByteFrame.getFrameBodyByteBuffer();
    this.stationName = FrameUtilities.readBytesAsString(body, 8);
    this.site = FrameUtilities.readBytesAsString(body, 5);
    this.channel = FrameUtilities.readBytesAsString(body, 3);
    this.locName = FrameUtilities.readBytesAsString(body, 2);
    body.position(body.position() + 2); // Skip two null bytes.
    this.timestamp = FrameUtilities.jdToInstant(FrameUtilities.readBytesAsString(body, 20));
    int commandMessageSize = body.getInt();
    this.commandMessage = FrameUtilities.readBytesAsString(body, commandMessageSize);

    this.validate();
  }

  /**
   * Creates an command request object with all arguments.
   *
   * @param stationName station name
   * @param site site
   * @param channel channel
   * @param locName location name
   * @param timestamp time stamp
   * @param commandMessage command message
   * @throws IllegalArgumentException Thrown on invalid input.
   */
  public Cd11CommandRequestFrame(
      String stationName, String site, String channel, String locName,
      Instant timestamp, String commandMessage) {

    super(FrameType.COMMAND_REQUEST);

    this.stationName = stationName;
    this.site = site;
    this.channel = channel;
    this.locName = locName;
    this.timestamp = timestamp;
    this.commandMessage = commandMessage;

    this.validate();
  }

  /**
   * Creates an Cd11CommandRequest frame with all arguments for frame Parsing Utility.
   *
   * @param stationName station name
   * @param site site
   * @param channel channel
   * @param locName location name
   * @param timestamp time stamp
   * @param commandMessage command message
   * @param partialFrame a partially parsed frame with a header, trailer and body bytes
   */
  public Cd11CommandRequestFrame(String stationName, String site, String channel,
      String locName, Instant timestamp, String commandMessage, PartialFrame partialFrame) {

    // Initialize the base class.
    super(partialFrame.getFrameHeader(), partialFrame.getFrameTrailer(), partialFrame.getFrameBodyBytes());

    // Initialize properties
    this.stationName = stationName;
    this.site = site;
    this.channel = channel;
    this.locName = locName;
    this.timestamp = timestamp;
    this.commandMessage = commandMessage;

  }

  private void validate() {
    checkNotNull(stationName);
    checkArgument(stationName.length() <= 8);

    checkNotNull(site);
    checkArgument(site.length() <= 5);

    checkNotNull(channel);
    checkArgument(channel.length() <= 3);

    checkNotNull(locName);
    checkArgument(locName.length() <= 2);

    checkNotNull(timestamp);
    checkArgument(!commandMessage.isBlank());
  }

  /**
   * Returns this command request frame as bytes.
   *
   * @return byte[], representing the frame in wire format
   */
  @Override
  public byte[] getFrameBodyBytes() {
    int frameSize = 8 + 5 + 3 + 2 + 2 + 20 + Integer.BYTES + commandMessage.length();

    ByteBuffer output = ByteBuffer.allocate(frameSize);
    output.put(FrameUtilities.padToLength(stationName, 8).getBytes());
    output.put(FrameUtilities.padToLength(site, 5).getBytes());
    output.put(FrameUtilities.padToLength(channel, 3).getBytes());
    output.put(FrameUtilities.padToLength(locName, 2).getBytes());
    output.put((byte) 0); // Null byte.
    output.put((byte) 0); // Null byte.
    output.put(FrameUtilities.instantToJd(timestamp).getBytes());
    output.putInt(commandMessage.length());
    output.put(commandMessage.getBytes());

    return output.array();
  }

  @Override
  public int hashCode() {
    return this.toString().hashCode();
  }

  @Override
  public String toString() {
    return "Cd11CommandRequestFrame { " + "frameType: \"" + frameType + "\", "
        + "stationName: \"" + stationName + "\", "
        + "site: \"" + site + "\", "
        + "channel: \"" + channel + "\", "
        + "locName: \"" + locName + "\", "
        + "timestamp: \"" + timestamp + "\", "
        + "commandMessageSize: " + commandMessage.length() + ", "
        + "commandMessage: \"" + commandMessage + "\" "
        + "}";
  }
}
