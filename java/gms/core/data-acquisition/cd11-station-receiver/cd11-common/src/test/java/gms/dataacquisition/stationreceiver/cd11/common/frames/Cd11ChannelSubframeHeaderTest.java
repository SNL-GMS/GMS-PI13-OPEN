package gms.dataacquisition.stationreceiver.cd11.common.frames;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import gms.dataacquisition.stationreceiver.cd11.common.FrameUtilities;
import java.nio.ByteBuffer;
import java.time.Instant;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class Cd11ChannelSubframeHeaderTest {

  private static ByteBuffer buffer1;
  private static ByteBuffer badBuffer;
  private static int size1 = 44 + 2;//the 2 is for padding check
  private static int numberOfChannels = 1;
  private static int timeSpan = 2000;
  private static int channelStringLen1 = 10;
  private static String channelString1 = "ABCC01L1XY";
  private static Instant nominalTime = Instant.parse("2017-12-06T02:03:04.098Z");

  @BeforeAll
  public static void setup() {
    buffer1 = ByteBuffer.allocate(size1);
    buffer1.putInt(numberOfChannels);
    buffer1.putInt(timeSpan);
    buffer1.put(FrameUtilities.instantToJd(nominalTime).getBytes());
    buffer1.putInt(channelStringLen1);
    buffer1.put(FrameUtilities.padToLength(channelString1, 12).getBytes());
    buffer1.putShort(
        (short) 0); // needed to test valid channel string padding -- upper half word of channel lenght field
    badBuffer = ByteBuffer.allocate(size1);
    badBuffer.put(buffer1.array());
  }

  @AfterAll
  public static void tearDown() {

  }

  /**
   * Check for successful parsing of the header.
   */
  @Test
  public void testSuccessfulParse() {
    Cd11ChannelSubframeHeader header = new Cd11ChannelSubframeHeader(buffer1.rewind());

    assertEquals(timeSpan, header.frameTimeLength);
    assertEquals(numberOfChannels, header.numOfChannels);
    assertEquals(nominalTime, header.nominalTime);
    assertEquals(FrameUtilities.stripString(channelString1), header.channelString);
  }

  /**
   * Pass the wrong size for the channel string, this should fail.
   */
  @Test
  public void testUnsuccessfulParse2() {
    assertThrows(IllegalStateException.class, () -> {
      buffer1.putInt(28, 50);
      new Cd11ChannelSubframeHeader(buffer1.rewind());
    });
  }

  /**
   * Test the toBytes method.
   */
  @Test
  public void testToBytes() {
    buffer1.rewind();
    Cd11ChannelSubframeHeader header = new Cd11ChannelSubframeHeader(buffer1.rewind());
    assertNotNull(header);
    buffer1.rewind();
    byte[] bytes1 = header.toBytes();
    byte[] bytes2 = new byte[buffer1.remaining()];
    buffer1.get(bytes2);

    for (int i = 0; i < bytes1.length; ++i) {
      assertEquals(bytes1[i], bytes2[i]);
    }

  }
}
