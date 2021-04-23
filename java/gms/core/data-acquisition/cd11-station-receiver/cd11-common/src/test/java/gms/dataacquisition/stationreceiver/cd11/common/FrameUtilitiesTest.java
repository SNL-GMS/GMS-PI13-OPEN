package gms.dataacquisition.stationreceiver.cd11.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.net.InetAddresses;
import gms.dataacquisition.stationreceiver.cd11.common.configuration.Cd11SocketConfig;
import gms.dataacquisition.stationreceiver.cd11.common.frames.*;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import org.junit.jupiter.api.Test;


/**
 * Collection of useful functions for dealing with CD-1.1 frames.
 */
public class FrameUtilitiesTest {

  /**
   * Parses the next CD 1.1 frame from the socket's data input stream. Note: Called from other unit
   * testing methods.
   *
   * @param dis the socket's data input stream.
   * @return Parsed CD 1.1 frame bytes.
   * @throws IOException Thrown when I/O errors occur, including timeouts.
   */
  private static Cd11ByteFrame readNextCd11ByteFrame(DataInputStream dis)
      throws IOException, InterruptedException {
    long endTimeNs = System.nanoTime() + (5000 * 1000 * 1000L);
    return new Cd11ByteFrame(dis, () -> (System.nanoTime() >= endTimeNs));
  }

  /**
   * Note: Called from other unit testing methods.
   */
  public static Cd11Frame readNextCd11Object(DataInputStream dis)
      throws IOException, InterruptedException {
    return readNextCd11Object(readNextCd11ByteFrame(dis));
  }

  /**
   * Note: Called from other unit testing methods.
   */
  private static Cd11Frame readNextCd11Object(Cd11ByteFrame cd11ByteFrame) {
    switch (cd11ByteFrame.getFrameType()) {
      case ACKNACK:
        return new Cd11AcknackFrame(cd11ByteFrame);
      case ALERT:
        return new Cd11AlertFrame(cd11ByteFrame);
      case CD_ONE_ENCAPSULATION:
        //todo implement the actual CD 1 encapsulation frame in frames
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
      default:
        throw new IllegalArgumentException("Frame type does not exist.");
    }
  }

  @Test
  public void testPadding() {
    assertEquals("ABC\0\0\0\0\0",
        FrameUtilities.padToLength("ABC", 8));
    assertEquals("ABCD",
        FrameUtilities.padToLength("ABCD", 4));
  }

  @Test
  public void testPadLen() {
    assertEquals(8, FrameUtilities.calculatePaddedLength(5, 4));
    assertEquals(8, FrameUtilities.calculatePaddedLength(8, 4));
  }

  @Test
  public void testUnpadLen() {
    assertEquals(3, FrameUtilities.calculateUnpaddedLength(5, 4));
    assertEquals(0, FrameUtilities.calculateUnpaddedLength(4, 4));
  }

  /**
   * Test the conversion of a timestamp to an Instant.  The CD1.1 uses timestamps in this format:
   * "yyyyddd hh:mm:ss.xxx"
   */
  @Test
  public void testTimeTransformation() {
    Instant instant = FrameUtilities.jdToInstant("2017001 13:12:11.123");
    System.out.println(instant);
    LocalDateTime ldt = LocalDateTime.ofInstant(instant, ZoneId.of("UTC"));
    assertEquals(2017, ldt.get(ChronoField.YEAR));
    assertEquals(1, ldt.get(ChronoField.MONTH_OF_YEAR));
    assertEquals(1, ldt.get(ChronoField.DAY_OF_MONTH));
    assertEquals(13, ldt.get(ChronoField.HOUR_OF_DAY));
    assertEquals(12, ldt.get(ChronoField.MINUTE_OF_HOUR));
    assertEquals(11, ldt.get(ChronoField.SECOND_OF_MINUTE));
    assertEquals(123, ldt.get(ChronoField.MILLI_OF_SECOND));
  }

  /**
   * This timestamp is not of the proper length, so it should be rejected.
   */
  @Test
  public void testTimeTransformation2() {
    assertThrows(IllegalArgumentException.class,
        () -> FrameUtilities.jdToInstant("2017001 13:12:11.01"));
  }

  /**
   * This timestamp is not valid (bad hour field), so it should be rejected.
   */
  @Test
  public void testTimeTransformation3() {
    assertThrows(DateTimeParseException.class,
        () -> FrameUtilities.jdToInstant("2017001 33:12:11.000"));
  }

  @Test
  public void testStripString() {
    String str = "ABC \0\0";
    assertEquals("ABC", FrameUtilities.stripString(str));
  }

  @Test
  public void testReadBytesAsString() {
    String str = " ABC  \0\0 ";
    ByteBuffer bb = ByteBuffer.allocate(str.length());
    bb.put(str.getBytes());
    assertEquals("ABC", FrameUtilities.readBytesAsString(bb.rewind(), str.length()));
  }

  @Test
  public void testCloneAndModifyFrame() {
    String origStationOrResponderName = "AAA";

    String newStationOrResponderName = "BBB";
    String newIpAddressString = "1.2.3.4";
    InetAddress newIpAddress = InetAddresses.forString(newIpAddressString);
    InetAddress loopbackAddress = InetAddress.getLoopbackAddress();
    int newPort = 9521;

    Cd11Socket cd11Socket = new Cd11Socket(Cd11SocketConfig.builderWithDefaults()
        .setFrameCreator("CRTR")
        .setFrameDestination("DEST")
        .setStationOrResponderName(origStationOrResponderName)
        .setStationOrResponderType("IDC")
        .setServiceType("TCP")
        .build());

    // Modify a Connection Request frame.
    Cd11ConnectionRequestFrame origConnReqFrame = cd11Socket.createCd11ConnectionRequestFrame();
    Cd11ConnectionRequestFrame modConnReqFrameFrame = FrameUtilities.cloneAndModifyFrame(
        origConnReqFrame, newStationOrResponderName, newIpAddress, newPort);
    assertTrue(
        modConnReqFrameFrame.stationName.equals(newStationOrResponderName) &&
            InetAddresses.fromInteger(modConnReqFrameFrame.ipAddress).equals(newIpAddress) &&
            modConnReqFrameFrame.port == newPort);

    // Modify the Connection Response frame.
    Cd11ConnectionResponseFrame origConnRespFrame = cd11Socket.createCd11ConnectionResponseFrame(
        loopbackAddress, 8282, null, null);
    Cd11ConnectionResponseFrame modConnRespFrameFrame = FrameUtilities.cloneAndModifyFrame(
        origConnRespFrame, newStationOrResponderName, newIpAddress, newPort);
    assertTrue(
        modConnRespFrameFrame.responderName.equals(newStationOrResponderName) &&
            InetAddresses.fromInteger(modConnRespFrameFrame.ipAddress).equals(newIpAddress) &&
            modConnRespFrameFrame.port == newPort);
  }
}
