package gms.dataacquisition.stationreceiver.cd11.common.frames;

import java.nio.ByteBuffer;
import java.util.Arrays;


/**
 * This custom frame signals to the Data Consumer that a "reset" has occurred, and that the Data
 * Consumer needs to clear its gap list, shutdown, and listen for a new Data Provider connection.
 *
 * NOTE: This frame is **NOT** described in the CD 1.1 protocol.
 */
public class CustomResetFrame extends Cd11Frame {

  public final byte[] frameBody;

  /**
   * Creates a custom "reset" object from a byte frame.
   *
   * @param cd11ByteFrame CD 1.1 frame segments.
   */
  public CustomResetFrame(Cd11ByteFrame cd11ByteFrame) {
    super(cd11ByteFrame);

    ByteBuffer body = cd11ByteFrame.getFrameBodyByteBuffer();
    this.frameBody = body.array();
  }

  public CustomResetFrame(byte[] frameBody) {
    super(FrameType.CUSTOM_RESET_FRAME);

    this.frameBody = frameBody;
  }

  /**
   * Returns this custom "reset" frame as bytes.
   *
   * @return byte[], representing the frame in wire format
   */
  @Override
  public byte[] getFrameBodyBytes() {
    return this.frameBody;
  }

  @Override
  public boolean equals(Object o) {
    return false;
  }

  @Override
  public int hashCode() {
    return 31 * Arrays.hashCode(frameBody);
  }

  @Override
  public String toString() {
    return "CustomResetFrame { " + "frameBodyLength: " + frameBody.length + " " + "}";
  }
}
