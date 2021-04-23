package gms.dataacquisition.stationreceiver.cd11.common;

import static com.google.common.base.Preconditions.checkArgument;

import gms.dataacquisition.stationreceiver.cd11.common.enums.Cd11DataFormat;
import gms.dataacquisition.stationreceiver.cd11.common.enums.CompressionFormat;
import gms.dataacquisition.stationreceiver.cd11.common.enums.SensorType;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11AcknackFrame;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11AlertFrame;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11ChannelSubframe;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11ChannelSubframeHeader;
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
import gms.dataacquisition.stationreceiver.cd11.common.frames.PartialFrame;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FrameParsingUtility class for parsing cd11 frames
 */
/**
 * FrameParsingUtility class for parsing cd11 frames
 */
public class FrameParsingUtility {
  private static final Logger logger = LoggerFactory.getLogger(FrameParsingUtility.class);

  private FrameParsingUtility(){}

  //constants for size of header/body/trailer fields
  private static final int HEADER_SIZE= (Integer.BYTES * 3) + Long.BYTES + 8 + 8;

  private static final String PARSING_ERROR_MESSAGE
      ="Fewer bytes than expected when reading from Byte Buffer for ";

  private static final String NON_NEGATIVE_ERROR =" value must be non-negative";

  //Given ByteBuffer, output a list of Partial Frames
  public static PartialFrame parseByteBuffer(ByteBuffer buf) {

      boolean isMalformedCd11Frame = false;
      Exception parsingError = null;
      Cd11FrameHeader frameHeader = null;
      Cd11FrameTrailer frameTrailer = null;
      byte[] bodyBytes = null;
      byte[] unparsedBytes = null;

      try {
        frameHeader = parseCd11Header(buf);
        bodyBytes = parseCd11Body(buf, frameHeader);
        frameTrailer = parseCd11Trailer(buf);

        if(buf.remaining()>0){
          throw new ParseCd11FromByteBufferException("More Bytes than expected in byte buffer");
        }
      } catch (ParseCd11FromByteBufferException e) {
        isMalformedCd11Frame = true;
        logger.warn("ParseCd11FromByteBuffer exception, frame will be sent to the malormed topic:", e);

        //we reach this state if there are not enough bytes in the buffer to read expected value
        //read rest of bytes from buffer so that while terminates
        unparsedBytes = new byte[buf.remaining()];
        buf.get(unparsedBytes);
        parsingError = e;
      } catch (IllegalArgumentException e) {
        isMalformedCd11Frame = true;
        logger.warn("Parsing error when creating partial frames. Frame will be sent to the malormed topic", e);
        parsingError = e;

        unparsedBytes = new byte[buf.remaining()];
      }

      return new PartialFrame(frameHeader, frameTrailer, bodyBytes,
          parsingError, isMalformedCd11Frame, unparsedBytes);
    }


  public static Cd11FrameHeader parseCd11Header(ByteBuffer buf){
    //throws ParseCd11FromByteBufferException, IllegalArgumentException
    Cd11FrameHeader cd11FrameHeader;
    try {
      int frameTypeInt = buf.getInt();
      FrameType frameType = FrameType.fromInt(frameTypeInt);
      int trailerOffset = buf.getInt();
      checkArgument((trailerOffset-HEADER_SIZE) >= 0,
          "The offset of the frame trailer must be at least the size of the header");

      // Defined in CD11 spec as 8 bytes
      String frameCreator = readBytesAsString(buf, 8);
      // Defined in CD11 spec as 8 bytes
      String frameDestination = readBytesAsString(buf, 8);
      long sequenceNumber = buf.getLong();
      int series = buf.getInt();

      //create Header
      cd11FrameHeader = new Cd11FrameHeader(frameType, trailerOffset,
          frameCreator, frameDestination, sequenceNumber, series);
    }catch (BufferUnderflowException e){
      throw new ParseCd11FromByteBufferException(PARSING_ERROR_MESSAGE+"Cd11 Frame", e);
    }
    return cd11FrameHeader;
  }

  public static byte[] parseCd11Body(ByteBuffer buf, Cd11FrameHeader frameHeader){
    //throws ParseCd11FromByteBufferException
    byte[] bodyBytes;
    try {
      int requiredBodyBytes = frameHeader.trailerOffset - HEADER_SIZE;
      bodyBytes = readBytesintoArray(buf, requiredBodyBytes, "size of body bytes");
    }catch (BufferUnderflowException e){
      throw new ParseCd11FromByteBufferException(PARSING_ERROR_MESSAGE+"Cd11 Body Bytes", e);
    }
    return bodyBytes;
  }

  public static Cd11FrameTrailer parseCd11Trailer(ByteBuffer buf){
    //throws ParseCd11FromByteBufferException, IllegalArgumentException

    Cd11FrameTrailer frameTrailer;
    try {
      int trailerAuthKeyIdentifier = buf.getInt();
      int trailerAuthSize = buf.getInt();
      int paddedAuthValSize = FrameUtilities.calculatePaddedLength(trailerAuthSize, Integer.BYTES);
      byte[] authenticationValue = readBytesintoArray(buf, paddedAuthValSize, "trailer auth size");
      long commVerification = buf.getLong();
      frameTrailer = new Cd11FrameTrailer(trailerAuthKeyIdentifier,
          trailerAuthSize,
          authenticationValue, commVerification);
    }catch (BufferUnderflowException e){
      throw new ParseCd11FromByteBufferException(PARSING_ERROR_MESSAGE+"Cd11 Trailer", e);
    }
    return frameTrailer;
  }

  public static Cd11Frame createCd11Frame(PartialFrame partialFrame){

    boolean isMalformed = partialFrame.isMalformed();

    //if malformed, just create a malformed data frame
    if(isMalformed) {
      return  partialFrame;
    }
    try {
      // Construct the appropriate CD 1.1 frame.
      FrameType frameType = partialFrame.getFrameHeader().frameType;
      switch (frameType) {
        case ACKNACK:
          return parseCd11AcknackFrame(partialFrame);
        case ALERT:
          return parseCd11AlertFrame(partialFrame);
        case CD_ONE_ENCAPSULATION:
          //todo implement the actual CD 1 encapsulation frame in frames
          logger.warn(
              "CD 1 encapsulation frame not yet supported, creating a dummy frame with appropriate FrameType");
          return new Cd11DummyFrame(frameType);
        case COMMAND_REQUEST:
        case COMMAND_RESPONSE:
          return parseCd11CommandRequestOrResponseFrame(partialFrame);
        case CONNECTION_REQUEST:
        case CONNECTION_RESPONSE:
          return parseCd11ConnectionRequestOrResponseFrame(partialFrame);
        case DATA:
          return parseCd11DataFrame(partialFrame);
        case OPTION_REQUEST:
        case OPTION_RESPONSE:
          return parseOptionRequestorResponseFrame(partialFrame);
        case CUSTOM_RESET_FRAME:
          return new CustomResetFrame(partialFrame.getFrameBodyBytes());
        default:
          throw new IllegalArgumentException(
              String.format("Frame type does not exist.%s", frameType));
      }
    }catch (ParseCd11FromByteBufferException | IllegalArgumentException |  DateTimeParseException e){
      partialFrame.setParsingError(e);
      partialFrame.setMalformed(true);
      logger.warn("Error parsing partial frame to Cd11Frame. Frame will be sent to malformed topic", e);
      return partialFrame;
    }
  }

  public static Cd11AcknackFrame parseCd11AcknackFrame(PartialFrame partialFrame){
    //throws ParseCd11FromByteBufferException, IllegalArgumentException
    ByteBuffer body = ByteBuffer.wrap(partialFrame.getFrameBodyBytes());
    Cd11AcknackFrame cd11AcknackFrame;

    try {
      //Size of frameset Acked is 20 bytes per the CD11 specification
      String framesetAcked = readBytesAsString(body, 20);
      long lowestSeqNum = body.getLong();
      long highestSeqNum = body.getLong();
      int gapCount = body.getInt();
      checkArgument(gapCount >= 0, "gapCount"+NON_NEGATIVE_ERROR);

      long[] gaps = new long[gapCount * 2];
      for (int i = 0; i < gapCount * 2; i++) {
        gaps[i] = body.getLong();
      }
      cd11AcknackFrame = new Cd11AcknackFrame(framesetAcked, lowestSeqNum, highestSeqNum, gaps, partialFrame);
    }catch (BufferUnderflowException e){
      throw new ParseCd11FromByteBufferException(PARSING_ERROR_MESSAGE+"Cd11Acknack Frame", e);
    }
    return cd11AcknackFrame;
  }

  public static Cd11AlertFrame parseCd11AlertFrame(PartialFrame partialFrame) {
    // throws ParseCd11FromByteBufferException, IllegalArgumentException
    ByteBuffer body = ByteBuffer.wrap(partialFrame.getFrameBodyBytes());
    Cd11AlertFrame cd11AlertFrame;
    try {
      int size = body.getInt();
      int paddedLength = FrameUtilities.calculatePaddedLength(size, Integer.BYTES);
      String message = readBytesAsString(body, paddedLength, "Alert frame message length");
      checkArgument(!message.isEmpty());

      cd11AlertFrame = new Cd11AlertFrame(size, message, partialFrame);
    }catch (BufferUnderflowException e){
      throw new ParseCd11FromByteBufferException(PARSING_ERROR_MESSAGE+"Cd11Alert Frame", e);
    }
    return  cd11AlertFrame;
  }

  public static Cd11Frame parseCd11CommandRequestOrResponseFrame(PartialFrame partialFrame){
    //throws ParseCd11FromByteBufferException, IllegalArgument Exception

    ByteBuffer body = ByteBuffer.wrap(partialFrame.getFrameBodyBytes());
    Cd11Frame cd11Frame;

    try {
      String stationName = FrameUtilities.readBytesAsString(body, 8);
      String site = FrameUtilities.readBytesAsString(body, 5);
      String channel = FrameUtilities.readBytesAsString(body, 3);
      String locName = FrameUtilities.readBytesAsString(body, 2);
      body.position(body.position() + 2); // Skip two null bytes.
      Instant timestamp = FrameUtilities.jdToInstant(FrameUtilities.readBytesAsString(body, 20));
      int commandMessageSize = body.getInt();

      String commandMessage = readBytesAsString(body, commandMessageSize,
          "Command message size");
      if (partialFrame.getFrameHeader().frameType == FrameType.COMMAND_RESPONSE) {
        int responseMessageSize = body.getInt();
        String responseMessage = readBytesAsString(body, responseMessageSize, "Response message size");
        cd11Frame = new Cd11CommandResponseFrame(stationName, site, channel, locName, timestamp,
            commandMessage, responseMessage, partialFrame);
      } else {
        cd11Frame = new Cd11CommandRequestFrame(stationName, site, channel, locName, timestamp,
            commandMessage,partialFrame);
      }
    }catch (BufferUnderflowException e){
      throw new ParseCd11FromByteBufferException(PARSING_ERROR_MESSAGE+"Cd11CommandOrResponse Frame", e);
    }
    return cd11Frame;
  }

  public static Cd11Frame parseCd11ConnectionRequestOrResponseFrame(PartialFrame partialFrame){
    //throws ParseCd11FromByteBufferException, IllegalArgumentException
    ByteBuffer body = ByteBuffer.wrap(partialFrame.getFrameBodyBytes());
    Cd11Frame cd11Frame;

    try {
      short majorVersion = body.getShort();
      short minorVersion = body.getShort();
      String stationName = FrameUtilities.readBytesAsString(body, 8);
      String stationType = FrameUtilities.readBytesAsString(body, 4);
      String serviceType = FrameUtilities.readBytesAsString(body, 4);
      int ipAddress = body.getInt();
      int port = body.getChar(); // Convert "unsigned short" to a Java "int".
      int secondIpAddress = body.getInt();
      int secondPort = body.getChar(); // Convert "unsigned short" to a Java "int".

      checkArgument(majorVersion >= 0, "Major version of command request/response"
          + "frame must be greather than 0");
      checkArgument(minorVersion >= 0, "Minor version of command request/response"
          + "frame must be greater than 0");
      Cd11Validator.validServiceType(serviceType);
      Cd11Validator.validIpAddress(ipAddress);
      Cd11Validator.validPortNumber(port);
      if (secondIpAddress != 0) { Cd11Validator.validIpAddress(secondIpAddress);}
      Cd11Validator.validPortNumber(secondPort);

      if (partialFrame.getFrameHeader().frameType == FrameType.CONNECTION_RESPONSE) {
        cd11Frame= new Cd11ConnectionResponseFrame(majorVersion, minorVersion, stationName,
            stationType, serviceType, ipAddress, port, secondIpAddress, secondPort,partialFrame);
      }else {
        cd11Frame =new Cd11ConnectionRequestFrame(majorVersion, minorVersion, stationName,
            stationType, serviceType, ipAddress, port, secondIpAddress, secondPort, partialFrame);
      }
    }catch (BufferUnderflowException e){
      throw new ParseCd11FromByteBufferException(PARSING_ERROR_MESSAGE +
          "Cd11ConnectionRequest or Response Frame", e);
    }
    return cd11Frame;
  }

  public static Cd11DataFrame parseCd11DataFrame(PartialFrame partialFrame) {
    //throws ParseCd11FromByteBufferException, IllegalArgumentException, DateTimeParseException

    ByteBuffer body = ByteBuffer.wrap(partialFrame.getFrameBodyBytes());
    Cd11DataFrame cd11DataFrame;
    Cd11ChannelSubframeHeader subframeHeader = parseChannelSubframeHeader(body);

    //there are 10 bytes for each channel subframe in the channel string
    int numOfSubFrames = subframeHeader.channelString.length() / 10;
    List<Cd11ChannelSubframe> tempSubFrames = new ArrayList<>();
    for (int i = 0; i < numOfSubFrames; i++) {
      tempSubFrames.add(parseChannelSubframe(body));
    }

    Cd11ChannelSubframe[] channelSubframes = tempSubFrames.toArray(new Cd11ChannelSubframe[0]);
    if (body.remaining() > 0) {
      logger.warn("Not all bytes of Data Frame body parsed, {} remaining", body.remaining());
    }
    cd11DataFrame = new Cd11DataFrame(subframeHeader, channelSubframes, partialFrame);
    return cd11DataFrame;
  }

  private static Cd11ChannelSubframeHeader parseChannelSubframeHeader(ByteBuffer body){
    //throws ParseCd11FromByteBufferException, IllegalArgumentException
    Cd11ChannelSubframeHeader cd11ChannelSubframeHeader;
    try {
      int numOfChannels = body.getInt();
      checkArgument(numOfChannels > 0,
          "Number of channels for channel subframe header must be > 0, but value is: "
              + numOfChannels);
      int frameTimeLength = body.getInt();
      checkArgument(frameTimeLength > 0,
          "Frame time length for channel subframe header must be > 0, but value is: "
              + frameTimeLength);

      String nominalTimeString = readBytesAsString(body, Cd11ChannelSubframeHeader.NOMTIMELENGTH);
      if (!FrameUtilities.validJulianDate(nominalTimeString)) {
        throw new IllegalArgumentException("Bad formatted nominalTime string");
      }
      Instant nominalTime = FrameUtilities.jdToInstant(nominalTimeString);

      int channelStringCount = body.getInt();
      byte[] channelStringAsBytes = readBytesintoArray(body, channelStringCount,
          "Channel string count for channel subframe header");
      String channelString = new String(channelStringAsBytes, StandardCharsets.UTF_8);

      int offset = 0;
      for (int i = 0; i < numOfChannels; ++i) {
        String subChannelString = channelString.substring(offset, offset + 10);
        offset += 10;
        if (!FrameUtilities.validChannelString(subChannelString)) {
          logger.info("Invalid channelString for subchannel string {}", (i + 1));
        }
      }

      // Check if the station indeed padded the channel string to be word (4 bytes) aligned
      // or if the padding is actually the upper half word of the following field (the channel length
      // field).  If the number of channels is odd, the channel string should be padded with 2 bytes so
      // the padding plus the upper half of the channel length field should be zero (assuming
      // the following channel subframe is <= 65,536 bytes).  If the value is not zero reset the byte
      // position to immediately after the channel string, otherwise reset it to after the padding.
      // Resume parsing the channel subframe data.
      int padding = FrameUtilities.calculateUnpaddedLength(channelStringCount, Integer.BYTES);
      if (padding > 0) {
        int temp = body.getInt();
        if (temp != 0) {
          logger.info("Channel string did not end on a 4 byte boundary.");
          body.position(body.position() - Integer.BYTES);
        } else { body.position(body.position() - padding); }
      }
      cd11ChannelSubframeHeader= new Cd11ChannelSubframeHeader(numOfChannels, frameTimeLength,
          nominalTime, channelStringCount, channelString);
    }catch (BufferUnderflowException e){
      throw new ParseCd11FromByteBufferException(PARSING_ERROR_MESSAGE+"Cd11ChannelSubframeHeader",e);
    }
    return cd11ChannelSubframeHeader;
  }

  private static Cd11ChannelSubframe parseChannelSubframe(ByteBuffer body){
    //throws ParseCd11FromByteBufferException, IllegalArgumentException, DateTimeParseException
    // channel length
    int channelLength = body.getInt();
    // authentication offset
    int authOffset = body.getInt();
    // 'Channel description' fields (byte[], parsed into individual fields)
    // byte 1: authentication
    boolean authenticationOn = (body.get()==1);
    // byte 2: 'transformation', which is the compression format (if any)
    CompressionFormat compressionFormat = CompressionFormat.of(body.get());
    // byte 3: 'sensor type'
    SensorType sensorType = SensorType.of(body.get());
    // byte 4: 'option flag', 1 means calibration
    boolean isCalib = (body.get() == 1);
    // bytes 5-9: site name
    String siteName = FrameUtilities.readBytesAsString(body, 5);
    // bytes 10-12: channel name
    String channelName = FrameUtilities.readBytesAsString(body, 3);
    // bytes 13-14: location name
    String locationName = FrameUtilities.readBytesAsString(body, 2);
    // bytes 15-16: 'uncompressed data format', aka CSS 3.0 Data Type
    String dataTypeName = FrameUtilities.readBytesAsString(body, 2);
    //Cd11DataFormat.fromString throws IllegalArgumentException
    Cd11DataFormat cd11DataFormat = Cd11DataFormat.fromString(dataTypeName);
    // bytes 17-20: calibration factor.  Only meaningful when isCalib = true.
    float calibrationFactor = body.getFloat();
    // bytes 21-24: calibration period.  Only meaningful when isCalib = true.
    float calibrationPeriod = body.getFloat();
    // timestamp
    String timestampString = readBytesAsString(body, FrameUtilities.TIMESTAMP_LEN);
    //Throws IllegalArgumentException, DateTimeParseException
    Instant timeStamp = FrameUtilities.jdToInstant(timestampString);
    // subframe time length
    int subframeTimeLength = body.getInt();
    checkArgument(subframeTimeLength >= 0, "Subframe time length" + NON_NEGATIVE_ERROR);
    // (number of) samples
    int samples = body.getInt();
    checkArgument(samples >= 0, "Cd11 Channel Subframe Samples" + NON_NEGATIVE_ERROR);

    // 'channel status size' (unpadded length in bytes of next field)
    int channelStatusSize = body.getInt();
    int channelStatusDataLength=
        FrameUtilities.calculatePaddedLength(channelStatusSize, Integer.BYTES);

    //Channel Status Size gives us unpadded length. We must pad to make Channel status size % 4 = 0
    byte [] channelStatusData = readBytesintoArray(body, channelStatusDataLength,
        "Cd11 Channel Subframe channel status size");

    // 'data size' (unpadded length in bytes of next field)
    int dataSize = body.getInt();
    int channelDataLength= FrameUtilities.calculatePaddedLength(dataSize, Integer.BYTES);

    //Channel data must also be padded
    byte[] channelData =readBytesintoArray(body, channelDataLength,
        "Cd11 Channel Subframe data size");
    // 'subframe count' (as assigned by digitizers; zero for digitizers that do not support this)
    int subframeCount = body.getInt();
    checkArgument(subframeCount >= 0,
        "SubframeCount must be >= 0, but value is: " + subframeCount);
    // authentication key identifier
    int authKeyIdentifier = body.getInt();
    // 'authentication size' (unpadded length in bytes of next field)
    int authSize = body.getInt();
    checkArgument(authSize >= 0,
        "AuthSize must be >= 0, but value is: " + authSize);
    int authValueLength= FrameUtilities.calculatePaddedLength(authSize, Integer.BYTES);

    // authentication value (DSS signature)
    byte[] authValue = readBytesintoArray(body, authValueLength,
        "Cd11 Channel Subframe Authsize");

    return  new Cd11ChannelSubframe(channelLength, authOffset, authenticationOn,
        compressionFormat, sensorType, isCalib, siteName, channelName, locationName,
        cd11DataFormat, calibrationFactor, calibrationPeriod, timeStamp, subframeTimeLength,
        samples, channelStatusSize, channelStatusData, dataSize, channelData, subframeCount,
        authKeyIdentifier, authSize, authValue);
  }

  public static Cd11Frame parseOptionRequestorResponseFrame(PartialFrame partialFrame){
    // throws IOException, IllegalArgumentException, NullPointerException
    ByteBuffer body = ByteBuffer.wrap(partialFrame.getFrameBodyBytes());
    Cd11Frame cd11Frame;

    try {
      body.getInt(); //option count, we ignore this variable currently
      int optionType = body.getInt();
      if (optionType != 1) {
        throw new IllegalArgumentException("Only OptionType 1 is currently accepted.");
      }
      int optionSize = body.getInt();
      if (partialFrame.getFrameHeader().frameType == FrameType.OPTION_RESPONSE) {

        String unpaddedOR = readBytesAsString(body, optionSize);
        String optionResponse = FrameUtilities.padToLength(unpaddedOR, FrameUtilities.calculatePaddedLength(optionSize, 4));

        // Validate option response.
        if (optionResponse.length() < 1 || optionResponse.length() > 8) {
          throw new IllegalArgumentException(String.format(
              "Option response length must be between 1 - 8 characters (received [%1$s]).",
              optionResponse));
        }
        cd11Frame = new Cd11OptionResponseFrame(optionType, optionResponse, partialFrame);
      }else {

        String optionRequest = readBytesAsString(
            body, FrameUtilities.calculatePaddedLength(optionSize, 4));

        if (optionRequest.length() < 1 || optionRequest.length() > 8) {
          throw new IllegalArgumentException(String.format(
              "Option request length must be between 1 - 8 characters (received [%1$s]).",
              optionRequest));
        }

        cd11Frame = new Cd11OptionRequestFrame(optionType, optionRequest, partialFrame);
      }
    }catch (BufferUnderflowException e){
      throw new ParseCd11FromByteBufferException(PARSING_ERROR_MESSAGE+"Cd11OptionRequestorResponse", e);
    }
    return cd11Frame;
  }

  public static byte[] readBytesintoArray(ByteBuffer frameBytesBuffer, int length, String var){
    //throws BufferUnderflowException

    checkArgument(length >= 0, var + NON_NEGATIVE_ERROR);
    byte[] bytearr = new byte[length];
    frameBytesBuffer.get(bytearr);
    return bytearr;
  }

  public static String readBytesAsString(ByteBuffer frameBytesBuffer, int length, String var){
    //throws BufferUnderflowException

    checkArgument(length >= 0, var + NON_NEGATIVE_ERROR);
    return readBytesAsString(frameBytesBuffer, length);
  }

  public static String readBytesAsString(ByteBuffer frameBytesBuffer, int length){
    //throws BufferUnderflowException

    byte[] stringBytes = new byte[length];
    frameBytesBuffer.get(stringBytes);
    String str = new String(stringBytes);
    // In case strings are null-terminated (c style), replace ASCII '0' with empty.
    // Remove all null characters from the string.
    str = str.replace("\0", "");
    // Trim whitespace from the ends of the string.
    str = str.trim();
    return str;
  }
}