package gms.dataacquisition.stationreceiver.cd11.dataman.configuration;

import static com.google.common.base.Preconditions.checkState;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.auto.value.AutoValue;
import com.google.common.net.InetAddresses;
import gms.dataacquisition.stationreceiver.cd11.common.Cd11GapList;
import gms.dataacquisition.stationreceiver.cd11.common.Cd11Validator;
import gms.dataacquisition.stationreceiver.cd11.common.GapList;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@AutoValue
@JsonSerialize(as = Cd11DataConsumerConfig.class)
@JsonDeserialize(builder = AutoValue_Cd11DataConsumerConfig.Builder.class)
public abstract class Cd11DataConsumerConfig {

  private static Logger logger = LoggerFactory.getLogger(Cd11DataConsumerConfig.class);

  private static final InetAddress DEFAULT_DATA_CONSUMER_IP_ADDRESS = InetAddresses.forString("127.0.0.1");
  private static final InetAddress DEFAULT_EXPECTED_DATA_PROVIDER_IP_ADDRESS = InetAddresses.forString("127.0.0.1");
  private static final String DEFAULT_THREAD_NAME = "CD 1.1 Data Consumer";
  private static final String DEFAULT_RESPONDER_NAME = "DC";
  private static final String DEFAULT_RESPONDER_TYPE = "IDC";
  private static final String DEFAULT_SERVICE_TYPE = "TCP";
  private static final String DEFAULT_FRAME_CREATOR = "TEST";
  private static final String DEFAULT_FRAME_DESTINATION = "0";
  private static final short DEFAULT_PROTOCOL_MAJOR_VERSION = 1;
  private static final short DEFAULT_PROTOCOL_MINOR_VERSION = 1;
  private static final int DEFAULT_AUTHENTICATION_KEY_IDENTIFIER = 0;

  private static final long DEFAULT_CONNECTION_EXPIRED_TIME_LIMIT_SEC = 120;
  private static final long DEFAULT_DATA_FRAME_SENDING_INTERVAL_MS = 500;
  private static final long DEFAULT_STORE_GAP_STATE_INTERVAL_MINUTES = 5;
  private static final int DEFAULT_GAP_EXPIRATION_IN_DAYS = -1; // Never expire.

  private static final String GAP_STORAGE_PATH = "shared-volume/gaps/";
  private static final String FILE_EXTENSION = ".json";
  private static final ObjectMapper objectMapper;

  static {
    // Ensure that the fake gap storage path exists.
    File gapsDir = new File(GAP_STORAGE_PATH);
    if (!gapsDir.exists()) {
      gapsDir.mkdirs();
    }

    objectMapper = new ObjectMapper();
    objectMapper.findAndRegisterModules();
    objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
  }


  public abstract InetAddress getDataConsumerIpAddress();

  public abstract int getDataConsumerPort();

  public abstract InetAddress getExpectedDataProviderIpAddress();

  public abstract String getDataProviderStationName();

  public abstract String getThreadName();

  public abstract String getResponderName();

  public abstract String getResponderType();

  public abstract String getServiceType();

  public abstract String getFrameCreator();

  public abstract String getFrameDestination();

  public abstract short getProtocolMajorVersion();

  public abstract short getProtocolMinorVersion();

  public abstract int getAuthenticationKeyIdentifier();

  public abstract long getConnectionExpiredTimeLimitSec();

  public abstract long getDataFrameSendingIntervalMs();

  public abstract long getStoreGapStateIntervalMinutes();

  public abstract int getGapExpirationInDays();

  public abstract boolean getStationDisabled();

  public static Cd11DataConsumerConfig.Builder builder() {
    return new AutoValue_Cd11DataConsumerConfig.Builder();
  }

  public static Cd11DataConsumerConfig.Builder builderWithDefaults(int localPort,
      String dataProviderStationName) {
    return builder()
        .setDataConsumerPort(localPort)
        .setDataProviderStationName(dataProviderStationName)
        .setDataConsumerIpAddress(DEFAULT_DATA_CONSUMER_IP_ADDRESS)
        .setExpectedDataProviderIpAddress(DEFAULT_EXPECTED_DATA_PROVIDER_IP_ADDRESS)
        .setThreadName(DEFAULT_THREAD_NAME)
        .setResponderName(DEFAULT_RESPONDER_NAME)
        .setResponderType(DEFAULT_RESPONDER_TYPE)
        .setServiceType(DEFAULT_SERVICE_TYPE)
        .setFrameCreator(DEFAULT_FRAME_CREATOR)
        .setFrameDestination(DEFAULT_FRAME_DESTINATION)
        .setProtocolMajorVersion(DEFAULT_PROTOCOL_MAJOR_VERSION)
        .setProtocolMinorVersion(DEFAULT_PROTOCOL_MINOR_VERSION)
        .setAuthenticationKeyIdentifier(DEFAULT_AUTHENTICATION_KEY_IDENTIFIER)
        .setConnectionExpiredTimeLimitSec(DEFAULT_CONNECTION_EXPIRED_TIME_LIMIT_SEC)
        .setDataFrameSendingIntervalMs(DEFAULT_DATA_FRAME_SENDING_INTERVAL_MS)
        .setStoreGapStateIntervalMinutes(DEFAULT_STORE_GAP_STATE_INTERVAL_MINUTES)
        .setGapExpirationInDays(DEFAULT_GAP_EXPIRATION_IN_DAYS)
        .setStationDisabled(false);
  }

  public abstract Cd11DataConsumerConfig.Builder toBuilder();

  @AutoValue.Builder
  @JsonPOJOBuilder(withPrefix = "set")
  public abstract static class Builder {

    /**
     * The IP Address of the Data Consumer (default: 0.0.0.0).
     *
     * @param dataConsumerIpAddress IP address.
     * @return Configuration built from this {@link Builder}, not null
     */
    public abstract Builder setDataConsumerIpAddress(InetAddress dataConsumerIpAddress);

    public abstract Builder setDataConsumerPort(int dataConsumerPort);

    /**
     * The IP Address of the Data Provider that is expected to connect to this Data Consumer (used
     * for connection verification only); (default: 127.0.0.1).
     *
     * @param expectedDataProviderIpAddress IP address.
     * @return Configuration built from this {@link Builder}, not null
     */
    public abstract Builder setExpectedDataProviderIpAddress(
        InetAddress expectedDataProviderIpAddress);

    public abstract Builder setDataProviderStationName(String dataProviderStationName);

    /**
     * A name for the CD 1.1 Data Consumer thread (default: "CD 1.1 Data Consumer").
     *
     * @param threadName Thread name.
     * @return Configuration built from this {@link Builder}, not null
     */
    public abstract Builder setThreadName(String threadName);

    /**
     * The name of this responder (as specified by the CD 1.1 Protocol).
     *
     * @param responderName Responder name.
     * @return Configuration built from this {@link Builder}, not null
     */
    public abstract Builder setResponderName(String responderName);

    /**
     * The type of this responder (e.g. IMS, IDC, etc.)
     *
     * @param responderType The responder type (as specified by the CD 1.1 Protocol).
     * @return Configuration built from this {@link Builder}, not null
     */
    public abstract Builder setResponderType(String responderType);

    /**
     * The service type for the consumer i.e. TCP or UDP (default: TCP).
     *
     * @param serviceType TCP or UDP.
     * @return Configuration built from this {@link Builder}, not null
     */
    public abstract Builder setServiceType(String serviceType);

    /**
     * Name of the frame creator (as specified by the CD 1.1 Protocol).
     *
     * @return Configuration built from this {@link Builder}, not null
     */
    public abstract Builder setFrameCreator(String frameCreator);

    /**
     * Frame destination e.g. IMS, IDC, 0, etc (default: 0). (See the CD 1.1 Protocol for details)
     *
     * @param frameDestination Frame destination.
     * @return Configuration built from this {@link Builder}, not null
     */
    public abstract Builder setFrameDestination(String frameDestination);

    /**
     * The major version number of the CD protocol (default: 1).
     *
     * @param protocolMajorVersion Major version number.
     * @return Configuration built from this {@link Builder}, not null
     */
    public abstract Builder setProtocolMajorVersion(short protocolMajorVersion);

    /**
     * The minor version number of the CD protocol (default: 1).
     *
     * @param protocolMinorVersion Minor version number.
     * @return Configuration built from this {@link Builder}, not null
     */
    public abstract Builder setProtocolMinorVersion(short protocolMinorVersion);

    /**
     * Auth key identifier for the CD 1.1 frame trailers.
     *
     * @param authenticationKeyIdentifier Authentication key identifier.
     * @return Configuration built from this {@link Builder}, not null
     */
    public abstract Builder setAuthenticationKeyIdentifier(int authenticationKeyIdentifier);

    /**
     * Maximum amount of time to wait for a CD 1.1 frame to arrive before giving up, in seconds
     * (default: 120).
     *
     * @param connectionExpiredTimeLimitSec Limit in seconds for the connection to be considered
     * expired.
     * @return Configuration built from this {@link Builder}, not null
     */
    public abstract Builder setConnectionExpiredTimeLimitSec(long connectionExpiredTimeLimitSec);

    /**
     * The amount of time to pause before sending one data frame after another, in milliseconds
     * (default: 500).
     *
     * @param dataFrameSendingIntervalMs Interval in milliseconds for the data frames to be sent
     * in.
     * @return Configuration built from this {@link Builder}, not null
     */
    public abstract Builder setDataFrameSendingIntervalMs(long dataFrameSendingIntervalMs);

    public abstract Builder setStoreGapStateIntervalMinutes(long storeGapStateIntervalMinutes);

    public abstract Builder setGapExpirationInDays(int expirationInDays);

    /**
     * The flag controlling if this stations frame data is processed
     *
     * @param stationDisabled boolean flag indicating disabled/enabled
     * @return Configuration built from this {@link Builder}, not null
     */
    public abstract Builder setStationDisabled(boolean stationDisabled);

    public abstract Cd11DataConsumerConfig autoBuild();

    public Cd11DataConsumerConfig build() {
      Cd11DataConsumerConfig dataConsumerConfig = autoBuild();

      // Validate dataConsumerIpAddress
      Cd11Validator.validIpAddress(dataConsumerConfig.getDataConsumerIpAddress());

      // Validate dataConsumerPort
      Cd11Validator.validNonZeroPortNumber(dataConsumerConfig.getDataConsumerPort());

      // Validate expectedDataProviderIpAddress
      Cd11Validator.validIpAddress(dataConsumerConfig.getExpectedDataProviderIpAddress());

      // Validate dataProviderStationName
      checkState(!dataConsumerConfig.getDataProviderStationName().isBlank());
      checkState(dataConsumerConfig.getDataProviderStationName().length() <= 8);

      // Validate threadName
      checkState(!dataConsumerConfig.getThreadName().isBlank());

      // Validate stationName
      Cd11Validator.validStationOrResponderName(dataConsumerConfig.getResponderName());

      // Validate stationType
      Cd11Validator.validStationOrResponderType(dataConsumerConfig.getResponderType());

      // Validate serviceType
      Cd11Validator.validServiceType(dataConsumerConfig.getServiceType());

      // Validate frameCreator
      Cd11Validator.validFrameCreator(dataConsumerConfig.getFrameCreator());

      // Validate frameDestination
      Cd11Validator.validFrameDestination(dataConsumerConfig.getFrameDestination());

      // Validate protocolMajorVersion
      checkState(
          dataConsumerConfig.getProtocolMajorVersion() >= 0,
          "Invalid value assigned to protocolMajorVersion.");

      // Validate protocolMajorVersion
      checkState(
          dataConsumerConfig.getProtocolMinorVersion() >= 0,
          "Invalid value assigned to protocolMinorVersion.");

      // Validate connectionExpiredTimeLimitSec
      checkState(
          dataConsumerConfig.getConnectionExpiredTimeLimitSec() > 0,
          "Value of connectionExpiredTimeLimitSec must be greater than 1.");

      // Validate dataFrameSendingIntervalMs
      checkState(
          dataConsumerConfig.getDataFrameSendingIntervalMs() >= 0,
          "Invalid value assigned to dataFrameSendingIntervalMs.");

      // Validate the storeGapStateIntervalMinutes
      checkState(dataConsumerConfig.getStoreGapStateIntervalMinutes() > 0);

      return dataConsumerConfig;
    }
  }


  //GapList loaders/writers
  public static Cd11GapList loadGapState(String stationName) {
    Path path = Paths.get(GAP_STORAGE_PATH + stationName + FILE_EXTENSION);
    if (Files.exists(path)) {
      try {
        String contents = new String(Files.readAllBytes(path));
        return new Cd11GapList(objectMapper.readValue(contents, GapList.class));
      } catch (IOException e) {
        logger.error("Error deserializing GapList", e);
        return new Cd11GapList();
      }
    } else {
      return new Cd11GapList();
    }
  }

  public static void persistGapState(String stationName, GapList gapList)
      throws IOException {
    String path = GAP_STORAGE_PATH + stationName + FILE_EXTENSION;
    try (PrintWriter out = new PrintWriter(path)) {
      // Set file permissions.
      File file = new File(path);
      boolean permissionsSet = file.setReadable(true, false);
      permissionsSet = permissionsSet && file.setWritable(true, false);
      permissionsSet = permissionsSet && file.setExecutable(false, false);

      if (!permissionsSet) {
        logger.warn("Failed to set permissions on gaps file.");
      }
      objectMapper.writeValue(file, gapList);
    }
  }

  public static void clearGapState(String stationName) throws IOException {
    Path path = Paths.get(GAP_STORAGE_PATH + stationName + FILE_EXTENSION);
    Files.delete(path);
  }
}
