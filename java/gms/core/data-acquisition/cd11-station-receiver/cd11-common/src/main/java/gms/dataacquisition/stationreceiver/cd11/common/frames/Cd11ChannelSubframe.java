package gms.dataacquisition.stationreceiver.cd11.common.frames;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import gms.dataacquisition.stationreceiver.cd11.common.FrameUtilities;
import gms.dataacquisition.stationreceiver.cd11.common.enums.Cd11DataFormat;
import gms.dataacquisition.stationreceiver.cd11.common.enums.CompressionFormat;
import gms.dataacquisition.stationreceiver.cd11.common.enums.SensorType;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Channel Subframe contains a channel description, the current channel status, and the actual
 * data. The data must be in a designated data type and may be compressed or uncompressed. The data
 * are followed by a data authentication signature. The number of Channel Subframes must match the
 * number specified in the Channel Subframe Header.
 */
public class Cd11ChannelSubframe {


  // See constructor javadoc for description of the fields.
  public final int channelLength;
  public final int authOffset;
  public final boolean authenticationOn;
  public final CompressionFormat compressionFormat;
  public final SensorType sensorType;
  public final boolean isCalib;
  public final String siteName;
  public final String channelName;
  public final String locationName;
  public final Cd11DataFormat cd11DataFormat;
  public final float calibrationFactor;
  public final float calibrationPeriod;
  public final Instant timeStamp;    // Defined in CD11 spec as 20 byte string, julian date format
  public final int subframeTimeLength;
  public final int samples;
  public final int channelStatusSize;
  public final byte[] channelStatusData;
  public final int dataSize;
  public final byte[] channelData;
  public final int subframeCount;
  public final int authKeyIdentifier;
  public final int authSize;
  public final byte[] authValue;
  public final double sampleRate;
  public final Instant endTime;

  private static final int CHANNEL_DESCRIPTION_LEN = 24;

  private static Logger logger = LoggerFactory.getLogger(Cd11ChannelSubframe.class);

  /**
   * The minimum byte array length of a subframe. This value does not include the following dynamic
   * fields: channelStatusData, channelData or authValue.
   */
  public static final int MINIMUM_FRAME_LENGTH = (Integer.BYTES * 9) +
      CHANNEL_DESCRIPTION_LEN + FrameUtilities.TIMESTAMP_LEN;

  /**
   * The Constructors make ONE SINGLE subframe, not the mutiple subframes contained within the body
   * of a data frame
   *
   * @param subframe A ByteBuffer containing the frame contents.
   * @throws IllegalArgumentException Thrown on invalid input.
   */
  public Cd11ChannelSubframe(ByteBuffer subframe) {

    checkArgument(subframe.remaining() >= MINIMUM_FRAME_LENGTH,
        "ChannelSubframe minimum size is %s but byte buffer only contains %s bytes.",
        MINIMUM_FRAME_LENGTH, subframe.remaining());

    try {
      // channel length
      this.channelLength = subframe.getInt();
      // authentication offset
      this.authOffset = subframe.getInt();
      // 'Channel description' fields (byte[], parsed into individual fields)
      // byte 1: authentication
      this.authenticationOn = subframe.get() == 1;
      // byte 2: 'transformation', which is the compression format (if any)
      this.compressionFormat = CompressionFormat.of(subframe.get());
      // byte 3: 'sensor type'
      this.sensorType = SensorType.of(subframe.get());
      // byte 4: 'option flag', 1 means calibration
      this.isCalib = subframe.get() == 1;
      // bytes 5-9: site name
      this.siteName = FrameUtilities.readBytesAsString(subframe, 5);
      // bytes 10-12: channel name
      this.channelName = FrameUtilities.readBytesAsString(subframe, 3);
      // bytes 13-14: location name
      this.locationName = FrameUtilities.readBytesAsString(subframe, 2);
      // bytes 15-16: 'uncompressed data format', aka CSS 3.0 Data Type
      String dataTypeName = FrameUtilities.readBytesAsString(subframe, 2);
      this.cd11DataFormat = Cd11DataFormat.fromString(dataTypeName);
      // bytes 17-20: calibration factor.  Only meaningful when isCalib = true.
      this.calibrationFactor = subframe.getFloat();
      // bytes 21-24: calibration period.  Only meaningful when isCalib = true.
      this.calibrationPeriod = subframe.getFloat();
      // timestamp
      String timestampString = FrameUtilities
          .readBytesAsString(subframe, FrameUtilities.TIMESTAMP_LEN);
      this.timeStamp = FrameUtilities.jdToInstant(timestampString);
      // subframe time length
      this.subframeTimeLength = subframe.getInt();
      // (number of) samples
      this.samples = subframe.getInt();
      // 'channel status size' (unpadded length in bytes of next field
      this.channelStatusSize = subframe.getInt();
      //Channel Status Size gives us unpadded length. We must
      //pad to make Channel status size % 4 = 0
      this.channelStatusData = new byte[FrameUtilities
          .calculatePaddedLength(channelStatusSize, Integer.BYTES)];
      subframe.get(this.channelStatusData);
      // 'data size' (unpadded length in bytes of next field)
      this.dataSize = subframe.getInt();
      //Channel data must also be padded
      this.channelData = new byte[FrameUtilities.calculatePaddedLength(dataSize, Integer.BYTES)];
      subframe.get(channelData);
      // 'subframe count' (as assigned by digitizers; zero for digitizers that do not support this)
      this.subframeCount = subframe.getInt();
      // authentication key identifier
      this.authKeyIdentifier = subframe.getInt();
      // 'authentication size' (unpadded length in bytes of next field)
      this.authSize = subframe.getInt();
      // authentication value (DSS signature)
      this.authValue = new byte[FrameUtilities.calculatePaddedLength(authSize, Integer.BYTES)];
      subframe.get(this.authValue);
      this.sampleRate = computeSampleRate(samples, subframeTimeLength);
      this.endTime = computeEndTime(this.timeStamp, this.sampleRate, this.subframeTimeLength);
    } catch (BufferUnderflowException | DateTimeParseException e) {
      throw new IllegalArgumentException("Cannot create Cd11ChannelSubframe", e);
    }

    validate();
  }

  private double computeSampleRate(int samples, int subframeTimeLength) {
    // time length is in milis, need to convert to seconds
    return ((double) samples) / ((double) subframeTimeLength) * 1000.0;
  }

  private Instant computeEndTime(Instant start, double sampleRate, int subframeTimeLength) {
    final int MILLION = 1_000_000;
    final int BILLION = MILLION * 1000;
    final double samplePeriodNanos = 1.0 / sampleRate * BILLION;
    // duration of frame is one sample period less than subframeTimeLength because the first
    // sample begins at zero.
    final double subframeLengthNanos = (double) subframeTimeLength * MILLION;
    final double durationNanos = subframeLengthNanos - samplePeriodNanos;
    final Duration frameDuration = Duration.ofNanos(
        (long) durationNanos);
    return start.plus(frameDuration);
  }

  /**
   * Creates data channel subframe with all arguments.
   *
   * @param channelLength length, in bytes and divisible by four of this Channel Subframe, not
   * counting this integer
   * @param authOffset byte offset from the first byte of the frame to the authentication key
   * identifier
   * @param authenticationOn indicates whether authentication is on
   * @param compressionFormat indicates compression format, if any (may be NONE)
   * @param sensorType the type of sensor, e.g. SEISMIC.
   * @param isCalib if true, this data is calibration.
   * @param siteName name of the site
   * @param channelName name of the channel
   * @param locationName name of location
   * @param cd11DataFormat type of the data
   * @param calibrationFactor calibration factor
   * @param calibrationPeriod calibration period
   * @param timeStamp UTC start time for first sample of this channel
   * @param subframeTimeLength time in milliseconds spanned by this channel data
   * @param samples number of samples in Channel Subframe
   * @param channelStatusSize unpadded length in bytes of next field
   * @param channelStatusData status data for channel, padded to be divisible by four
   * @param dataSize unpadded length in bytes of next field
   * @param channelData data for channel, padded to be divisible by four
   * @param subframeCount subframe count as assigned by digitizer; zero for digitizers that do not
   * support this count
   * @param authKeyIdentifier pointer to the credentials with the public key to be used for
   * verifying the authentication value field
   * @param authSize unpadded length in bytes of next field
   * @param authValue DSS signature over the following fields: channel description, timestamp,
   * subframe time length, samples, channel status size, channel status data, data size, channel
   * data, and subframe count. This field is padded as necessary to be divisible by four.
   */
  @JsonCreator
  public Cd11ChannelSubframe(
      @JsonProperty("channelLength") int channelLength,
      @JsonProperty("authOffset") int authOffset,
      @JsonProperty("authenticationOn") boolean authenticationOn,
      @JsonProperty("compressionFormat") CompressionFormat compressionFormat,
      @JsonProperty("sensorType") SensorType sensorType,
      @JsonProperty("isCalib") boolean isCalib,
      @JsonProperty("siteName") String siteName,
      @JsonProperty("channelName") String channelName,
      @JsonProperty("locationName") String locationName,
      @JsonProperty("cd11DataFormat") Cd11DataFormat cd11DataFormat,
      @JsonProperty("calibrationFactor") float calibrationFactor,
      @JsonProperty("calibrationPeriod") float calibrationPeriod,
      @JsonProperty("timeStamp") Instant timeStamp,
      @JsonProperty("subframeTimeLength") int subframeTimeLength,
      @JsonProperty("samples") int samples,
      @JsonProperty("channelStatusSize") int channelStatusSize,
      @JsonProperty("channelStatusData") byte[] channelStatusData,
      @JsonProperty("dataSize") int dataSize,
      @JsonProperty("channelData") byte[] channelData,
      @JsonProperty("subframeCount") int subframeCount,
      @JsonProperty("authKeyIdentifier") int authKeyIdentifier,
      @JsonProperty("authSize") int authSize,
      @JsonProperty("authValue") byte[] authValue) {

    this.channelLength = channelLength;
    this.authOffset = authOffset;
    this.authenticationOn = authenticationOn;
    this.compressionFormat = compressionFormat;
    this.sensorType = sensorType;
    this.isCalib = isCalib;
    this.siteName = FrameUtilities.stripString(siteName);
    this.channelName = FrameUtilities.stripString(channelName);
    this.locationName = FrameUtilities.stripString(locationName);
    this.cd11DataFormat = cd11DataFormat;
    this.calibrationFactor = calibrationFactor;
    this.calibrationPeriod = calibrationPeriod;
    this.timeStamp = timeStamp;
    this.subframeTimeLength = subframeTimeLength;
    this.samples = samples;
    this.channelStatusSize = channelStatusSize;
    this.channelStatusData = channelStatusData;
    this.dataSize = dataSize;
    this.channelData = channelData;
    this.subframeCount = subframeCount;
    this.authKeyIdentifier = authKeyIdentifier;
    this.authSize = authSize;
    this.authValue = authValue;
    this.sampleRate = computeSampleRate(samples, subframeTimeLength);
    this.endTime = computeEndTime(this.timeStamp, this.sampleRate, this.subframeTimeLength);

    validate();
  }

  public Cd11ChannelSubframe(Cd11ChannelSubframe oldChannelSubframe, int newChannelStatusSize,
      byte[] newChannelStatusData) {
    this(oldChannelSubframe.channelLength, oldChannelSubframe.authOffset,
        oldChannelSubframe.authenticationOn, oldChannelSubframe.compressionFormat,
        oldChannelSubframe.sensorType,
        oldChannelSubframe.isCalib, oldChannelSubframe.siteName, oldChannelSubframe.channelName,
        oldChannelSubframe.locationName, oldChannelSubframe.cd11DataFormat,
        oldChannelSubframe.calibrationFactor,
        oldChannelSubframe.calibrationPeriod, oldChannelSubframe.timeStamp,
        oldChannelSubframe.subframeTimeLength,
        oldChannelSubframe.samples, newChannelStatusSize, newChannelStatusData,
        oldChannelSubframe.dataSize,
        oldChannelSubframe.channelData, oldChannelSubframe.subframeCount,
        oldChannelSubframe.authKeyIdentifier,
        oldChannelSubframe.authSize, oldChannelSubframe.authValue);
  }


  /**
   * The size of the channel subframe is dynamic because the data length fields are dependent upon
   * the size of the data.
   *
   * @return The size in bytes of the subframe
   */
  public int getSize() {
    return MINIMUM_FRAME_LENGTH + channelStatusData.length
        + channelData.length + authValue.length;
  }

  /**
   * Turns this data subframe into a byte[]
   *
   * @return a byte[] representation of this data subframe
   */
  public byte[] toBytes() {

    ByteBuffer output = ByteBuffer.allocate(getSize());

    output.putInt(channelLength);
    output.putInt(authOffset);
    output.put((byte) (authenticationOn ? 1 : 0));
    output.put(compressionFormat.code);
    output.put(sensorType.code);
    output.put((byte) (isCalib ? 1 : 0));
    output.put(FrameUtilities.padToLength(siteName, 5).getBytes());
    output.put(FrameUtilities.padToLength(channelName, 3).getBytes());
    output.put(FrameUtilities.padToLength(locationName, 2).getBytes());
    output.put(cd11DataFormat.toBytes());
    output.putFloat(calibrationFactor);
    output.putFloat(calibrationPeriod);
    output.put(FrameUtilities.instantToJd(timeStamp).getBytes());
    output.putInt(subframeTimeLength);
    output.putInt(samples);
    output.putInt(channelStatusSize);
    output.put(channelStatusData);
    output.putInt(dataSize);
    output.put(channelData);
    output.putInt(subframeCount);
    output.putInt(authKeyIdentifier);
    output.putInt(authSize);
    output.put(authValue);

    return output.array();
  }

  /**
   * Gets the 'channel string' for this subframe, which consists of 10 bytes: siteName (5 bytes),
   * channelName (3 bytes), locationName (2 bytes).
   *
   * @return the channel string for this subframe
   */
  public String channelString() {
    // Spec: site name is padded to 5 characters
    String paddedSiteName = FrameUtilities.padToLength(this.siteName, 5);
    // Spec: channel name is padded to 3 characters
    String paddedChannelName = FrameUtilities.padToLength(this.channelName, 3);
    // Spec: location name is two characteres
    String paddedLocationName = FrameUtilities.padToLength(this.locationName, 2);
    String channelString = paddedSiteName + paddedChannelName + paddedLocationName;
    checkState(channelString.length() == 10);
    return channelString;
  }

  /**
   * Validates this object. Throws an exception if there are any problems with it's fields.
   *
   * @throws IllegalArgumentException Thrown on invalid input.
   */
  private void validate() {

    checkArgument(this.channelLength >= MINIMUM_FRAME_LENGTH,
        "ChannelSubframe.ChannelLength must be > minimum frame length of " +
            MINIMUM_FRAME_LENGTH + ", but value is: " + this.channelLength);

    checkArgument(this.channelLength % 4 == 0,
        "channelLength must be divisible by 4");

    checkArgument(authOffset >= MINIMUM_FRAME_LENGTH,
        "ChannelSubframe.AuthOffset must be > minimum frame length of " +
            MINIMUM_FRAME_LENGTH + ", but value is: " + authOffset);

    checkNotNull(this.compressionFormat);
    checkNotNull(this.sensorType);
    checkArgument(!this.siteName.isEmpty());
    checkArgument(!this.channelName.isEmpty());

    // They currently forwards us data with null location names, so we should log the info and move on.
    if (this.locationName == null) {
      logger.debug("Null location name.");
    }

    checkNotNull(this.cd11DataFormat);
    checkNotNull(this.timeStamp);

    checkArgument(subframeTimeLength >= 0,
        "ChannelSubframe.SubframeTimeLength must be >= 0, but value is: " + subframeTimeLength);

    checkArgument(samples >= 0,
        "ChannelSubframe.Samples must be >= 0, but value is: " + samples);

    checkArgument(channelStatusSize >= 0,
        "ChannelSubframe.ChannelStatusSize must be >= 0, but value is: " + channelStatusSize);

    checkArgument(dataSize >= 0,
        "ChannelSubframe.DataSize must be >= 0, but value is: " + dataSize);

    checkArgument(subframeCount >= 0,
        "ChannelSubframe.SubframeCount must be >= 0, but value is: " + subframeCount);

    checkArgument(authSize >= 0,
        "ChannelSubframe.AuthSize must be >= 0, but value is: " + authSize);

    checkNotNull(this.authValue);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Cd11ChannelSubframe that = (Cd11ChannelSubframe) o;
    return channelLength == that.channelLength &&
        authOffset == that.authOffset &&
        authenticationOn == that.authenticationOn &&
        isCalib == that.isCalib &&
        Float.compare(that.calibrationFactor, calibrationFactor) == 0 &&
        Float.compare(that.calibrationPeriod, calibrationPeriod) == 0 &&
        subframeTimeLength == that.subframeTimeLength &&
        samples == that.samples &&
        channelStatusSize == that.channelStatusSize &&
        dataSize == that.dataSize &&
        subframeCount == that.subframeCount &&
        authKeyIdentifier == that.authKeyIdentifier &&
        authSize == that.authSize &&
        Double.compare(that.sampleRate, sampleRate) == 0 &&
        compressionFormat == that.compressionFormat &&
        sensorType == that.sensorType &&
        siteName.equals(that.siteName) &&
        channelName.equals(that.channelName) &&
        locationName.equals(that.locationName) &&
        cd11DataFormat == that.cd11DataFormat &&
        timeStamp.equals(that.timeStamp) &&
        Arrays.equals(channelStatusData, that.channelStatusData) &&
        Arrays.equals(channelData, that.channelData) &&
        Arrays.equals(authValue, that.authValue) &&
        endTime.equals(that.endTime);
  }

  @Override
  public int hashCode() {
    int result = Objects
        .hash(channelLength, authOffset, authenticationOn, compressionFormat, sensorType, isCalib,
            siteName, channelName, locationName, cd11DataFormat, calibrationFactor, calibrationPeriod,
            timeStamp, subframeTimeLength, samples, channelStatusSize, dataSize, subframeCount,
            authKeyIdentifier, authSize, sampleRate, endTime);
    result = 31 * result + Arrays.hashCode(channelStatusData);
    result = 31 * result + Arrays.hashCode(channelData);
    result = 31 * result + Arrays.hashCode(authValue);
    return result;
  }

  @Override
  public String toString() {
    return "Cd11ChannelSubframe { " + "channelLength: " + channelLength + ", "
        + "authOffset: " + authOffset + ", "
        + "authenticationOn: " + ((authenticationOn) ? "true" : "false") + ", "
        + "compressionFormat: " + compressionFormat + ", "
        + "sensorType: " + sensorType + ", "
        + "isCalib: " + ((isCalib) ? "true" : "false") + ", "
        + "siteName: \"" + siteName + "\", "
        + "channelName: \"" + channelName + "\", "
        + "locationName: \"" + locationName + "\", "
        + "cd11DataFormat: \"" + cd11DataFormat + "\", "
        + "calibrationFactor: " + calibrationFactor + ", "
        + "calibrationPeriod: " + calibrationPeriod + ", "
        + "timeStamp: " + timeStamp + ", "
        + "subframeTimeLength: " + subframeTimeLength + ", "
        + "samples: " + samples + ", "
        + "channelStatusSize: " + channelStatusSize + ", "
        + "channelStatusData: \"" + Arrays.toString(channelStatusData) + "\", "
        + "dataSize: " + dataSize + ", "
        + "channelData: \"" + Arrays.toString(channelData) + "\", "
        + "subframeCount: " + subframeCount + ", "
        + "authKeyIdentifier: " + authKeyIdentifier + ", "
        + "authSize: " + authSize + ", "
        + "authValue: \"" + Arrays.toString(authValue) + "\" "
        + "}";
  }
}
