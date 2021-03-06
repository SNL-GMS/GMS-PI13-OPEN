package gms.dataacquisition.stationreceiver.cd11.common.frames;

import gms.dataacquisition.stationreceiver.cd11.common.FrameUtilities;
import java.nio.ByteBuffer;


/**
 * This frame and its companion, the Option Response Frame, are only exchanged as part of the
 * connection establishment process. However, future developments may support using these frames to
 * designate desired parameters such as a start time or list of channels, where multiple Option
 * Request Frames may be sent if necessary.  Therefore, only a single option is implemented at this
 * time. This class will require an update to make it more dynamic should new options be
 * implemented. NOTE: This class implements the "Connection establishment" option only!  This is the
 * only option defined by the current protocol specification.
 */
public class Cd11OptionRequestFrame extends Cd11Frame {

  // See constructor javadoc for description of the fields.
  public final int optionType;
  public final String optionRequest; // Defined in CD11 spec as 8 bytes for a "Connection establishment" option

  /**
   * The byte array length of an option request frame.
   */
  public static final int FRAME_LENGTH = (Integer.BYTES * 3) + 8;

  /**
   * Creates an option object from a byte frame.
   *
   * @param cd11ByteFrame CD 1.1 frame segments.
   */
  public Cd11OptionRequestFrame(Cd11ByteFrame cd11ByteFrame) {
    super(cd11ByteFrame);

    ByteBuffer body = cd11ByteFrame.getFrameBodyByteBuffer();
    body.position(Integer.BYTES);  // Have to shift the buffer to start at the optionType
    this.optionType = body.getInt();
    int optionSize = body.getInt();
    this.optionRequest = FrameUtilities.readBytesAsString(
        body, FrameUtilities.calculatePaddedLength(optionSize, 4));

    this.validate();
  }

  /**
   * Creates an option frame with all arguments.
   *
   * @param optionType numeric identifier of option requested
   * @param optionRequest value of option, padded to be divisible by 4
   * @throws IllegalArgumentException Thrown on invalid input.
   */
  public Cd11OptionRequestFrame(int optionType, String optionRequest) {

    super(FrameType.OPTION_REQUEST);

    this.optionType = optionType;
    this.optionRequest = optionRequest;

    this.validate();
  }


  /**
   * Creates an option frame with all arguments.
   *
   * @param optionType numeric identifier of option requested
   * @param optionRequest value of option, padded to be divisible by 4
   * @param partialFrame a partially parsed frame with a header, trailer and body bytes
   */
  public Cd11OptionRequestFrame(int optionType, String optionRequest, PartialFrame partialFrame) {

    // Initialize the base class.
    super(partialFrame.getFrameHeader(), partialFrame.getFrameTrailer(), partialFrame.getFrameBodyBytes());

    this.optionType = optionType;
    this.optionRequest = optionRequest;
  }

  private void validate() {
    // Validate option type.
    if (this.optionType != 1) {
      throw new IllegalArgumentException("Only OptionType 1 is currently accepted.");
    }

    // Validate option request.
    if (this.optionRequest == null) {
      throw new NullPointerException("Option response value is null.");
    } else if (this.optionRequest.length() < 1 || optionRequest.length() > 8) {
      throw new IllegalArgumentException(String.format(
          "Option request length must be between 1 - 8 characters (received [%1$s]).",
          optionRequest));
    }
  }

  /**
   * Returns this option request frame as bytes.
   *
   * @return byte[], representing the frame in wire format
   */
  @Override
  public byte[] getFrameBodyBytes() {
    int optionRequestPaddedSize = FrameUtilities
        .calculatePaddedLength(this.optionRequest.length(), 4);

    ByteBuffer output = ByteBuffer.allocate(Cd11OptionRequestFrame.FRAME_LENGTH);
    output.putInt(1); // TODO: Hard coded, since we only accept one option at this time.
    output.putInt(this.optionType);
    output.putInt(this.optionRequest.length()); // Unpadded length of the option request.
    output.put(FrameUtilities.padToLength(this.optionRequest, optionRequestPaddedSize).getBytes());

    return output.array();
  }

  // TODO: Define equals method.

  // TODO: Define hashCode method.

  @Override
  public String toString() {
    return "Cd11OptionRequestFrame: " + "optionCount=" + 1
        // TODO: Fix once we accept more than one option.
        + " optionType=" + optionType
        + " optionSize=" + optionRequest.length()
        + " optionRequestLength=" + optionRequest.length();
  }
}
