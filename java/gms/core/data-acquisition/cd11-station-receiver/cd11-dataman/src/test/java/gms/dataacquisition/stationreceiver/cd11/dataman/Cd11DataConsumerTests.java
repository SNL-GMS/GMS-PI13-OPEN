package gms.dataacquisition.stationreceiver.cd11.dataman;

import gms.core.dataacquisition.receiver.DataFrameReceiverConfiguration;
import gms.dataacquisition.stationreceiver.cd11.common.Cd11Socket;
import gms.dataacquisition.stationreceiver.cd11.common.configuration.Cd11SocketConfig;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11ChannelSubframe;
import gms.dataacquisition.stationreceiver.cd11.dataman.configuration.Cd11DataConsumerConfig;
import org.apache.kafka.clients.producer.MockProducer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willReturn;

@ExtendWith(MockitoExtension.class)
public class Cd11DataConsumerTests {

  private static Logger logger = LoggerFactory.getLogger(Cd11DataConsumerTests.class);

  private static final InetAddress DATA_PROVIDER_IP_ADDRESS = InetAddress.getLoopbackAddress();
  private static final InetAddress DATA_CONSUMER_WELL_KNOWN_IP_ADDRESS = InetAddress
      .getLoopbackAddress();
  private static final Integer DATA_CONSUMER_ASSIGNED_PORT = 8100;
  private static final Short MAJOR_VERSION = 1;
  private static final Short MINOR_VERSION = 1;
  private static final String RSDF_STATION_NAME = "LBTB";
  private static final String STATION_NAME = "H07N";
  private static final String STATION_TYPE = "IDC";
  private static final String SERVICE_TYPE = "TCP";
  private static final String FRAME_CREATOR = "TEST";
  private static final String FRAME_DESTINATION = "IDC";
  private static final Integer AUTHENTICATION_KEY_IDENTIFIER = 0;

  private static final int DATA_FRAME_HEADER_SIZE = 44;

  private static final int DATA_SUBFRAME_SIZE = 100;
  private static final int CHANNEL_LENGTH = 96;
  private static final int AUTHENTICATION_OFFSET = DATA_FRAME_HEADER_SIZE + DATA_SUBFRAME_SIZE - 16;
  private static final byte CHANNEL_DESCRIPTION_AUTHENTICATION = 0;
  private static final byte CHANNEL_DESCRIPTION_TRANSFORMATION = 1;
  private static final byte CHANNEL_DESCRIPTION_SENSOR_TYPE = 0;
  private static final byte CHANNEL_DESCRIPTION_OPTION_FLAG = 0;
  private static final String CHANNEL_DESCRIPTION_SITE_NAME = "STA12";
  private static final String CHANNEL_DESCRIPTION_CHANNEL_NAME = "SHZ";
  private static final String CHANNEL_DESCRIPTION_LOCATION = "01";
  private static final String CHANNEL_DESCRIPTION_DATA_FORMAT = "s4";
  private static final int CHANNEL_DESCRIPTION_CALIB_FACTOR = 0;
  private static final int CHANNEL_DESCRIPTION_CALIB_PER = 0;
  private static final String TIME_STAMP = "2017346 23:21:10.168";
  private static final int SUBFRAME_TIME_LENGTH = 10000;
  private static final int SAMPLES = 8;
  private static final int CHANNEL_STATUS_SIZE = 4;
  private static final int CHANNEL_STATUS = 0;
  private static final int DATA_SIZE = 8;
  private static final byte[] CHANNEL_DATA = new byte[8];
  private static final int SUBFRAME_COUNT = 0;

  private static final int AUTH_KEY = 123;
  private static final int AUTH_SIZE = 8;
  private static final long AUTH_VALUE = 1512076158000L;

  @Mock
  private static DataFrameReceiverConfiguration mockDfrConfig;

  private List<MockProducer<String, String>> mockRsdfProducers;

  private ExecutorService producerMonitorExecutorService;

  private static final int PRODUCER_MONITOR_TIMEOUT_MS = 5000;

  private static final String MOCK_RSDF_OUTPUT_TOPIC = "mock.topic";

  private List<Cd11DataConsumer> consumers;
  private static final int NUM_STATIONS = 10;

  @AfterEach
  void tearDown() {
    if (consumers != null) {
      consumers.forEach(Cd11DataConsumer::stop);
    }
    if (producerMonitorExecutorService != null) {
      producerMonitorExecutorService.shutdownNow();
    }
  }

  @Test
  public void testSendMultipleStations()
      throws InterruptedException, ExecutionException {
    setupCd11Consumers(NUM_STATIONS, false);

    Cd11ChannelSubframe[] sfArray = FakeDataFrame.generateFakeChannelSubframes();

    List<Cd11Socket> cd11Sockets = new ArrayList<>();

    //Get the threads ready, separate loop because we don't want to include this in the time
    Thread[] stationThreads = new Thread[NUM_STATIONS];
    for (short i = 0; i < NUM_STATIONS; i++) {
      String packetName = String
          .format("%s.%s.%s", STATION_NAME + i, CHANNEL_DESCRIPTION_SITE_NAME,
              CHANNEL_DESCRIPTION_CHANNEL_NAME);

      willReturn(Optional.of(packetName)).given(mockDfrConfig).getChannelName(packetName);

      //Generate unique sending and receiving ports
      Integer consumerPortOffset = DATA_CONSUMER_ASSIGNED_PORT + i;

      Cd11Socket cd11Socket = new Cd11Socket(Cd11SocketConfig.builder()
          .setStationOrResponderName(STATION_NAME + i)
          .setStationOrResponderType(STATION_TYPE)
          .setServiceType(SERVICE_TYPE)
          .setFrameCreator(FRAME_CREATOR)
          .setFrameDestination(FRAME_DESTINATION)
          .setAuthenticationKeyIdentifier(AUTHENTICATION_KEY_IDENTIFIER)
          .setProtocolMajorVersion(MAJOR_VERSION)
          .setProtocolMinorVersion(MINOR_VERSION)
          .build());

      FakeStation station = new FakeStation(
          cd11Socket, sfArray, DATA_CONSUMER_WELL_KNOWN_IP_ADDRESS, consumerPortOffset);
      stationThreads[i] = new Thread(station);

      cd11Sockets.add(cd11Socket);
    }

    //Now time how long it takes for the threads to complete
    long startTime = System.currentTimeMillis();
    for (Thread thread : stationThreads) {
      thread.start();
      thread.join();
    }
    long endTime = System.currentTimeMillis();
    long duration = (endTime - startTime);
    logger.info(String.format("Duration = %d", duration));
    assertTrue(duration < 10000);

    cd11Sockets.forEach(Cd11Socket::disconnect);
    checkFramesProcessed(true);
  }

  @Test
  public void testSendDataFrame() throws Exception {
    setupCd11Consumers(1, false);

    given(mockDfrConfig.getChannelName(anyString())).willReturn(Optional.of(String
        .format("%s.%s.%s", STATION_NAME, CHANNEL_DESCRIPTION_SITE_NAME,
            CHANNEL_DESCRIPTION_CHANNEL_NAME)));

    int framesToSend = 3;

    Cd11ChannelSubframe sf = new Cd11ChannelSubframe(initChannelSubframeBytes().rewind());
    Cd11ChannelSubframe[] sfArray = new Cd11ChannelSubframe[]{sf};

    Cd11Socket cd11Socket = new Cd11Socket(Cd11SocketConfig.builder()
        .setStationOrResponderName(STATION_NAME)
        .setStationOrResponderType(STATION_TYPE)
        .setServiceType(SERVICE_TYPE)
        .setFrameCreator(FRAME_CREATOR)
        .setFrameDestination(FRAME_DESTINATION)
        .setAuthenticationKeyIdentifier(AUTHENTICATION_KEY_IDENTIFIER)
        .setProtocolMajorVersion(MAJOR_VERSION)
        .setProtocolMinorVersion(MINOR_VERSION)
        .build());

    cd11Socket.connect(
        DATA_CONSUMER_WELL_KNOWN_IP_ADDRESS, DATA_CONSUMER_ASSIGNED_PORT, 35000);

    for (int i = 0; i < framesToSend; i++) {
      cd11Socket.sendCd11DataFrame(sfArray, (long) i + 1);
    }

    cd11Socket.disconnect();
    checkFramesProcessed(true);
  }

  @Test
  public void testSendDataFrameIgnore() throws Exception {
    setupCd11Consumers(1, true);

    int framesToSend = 3;

    Cd11ChannelSubframe sf = new Cd11ChannelSubframe(initChannelSubframeBytes().rewind());
    Cd11ChannelSubframe[] sfArray = new Cd11ChannelSubframe[]{sf};

    Cd11Socket cd11Socket = new Cd11Socket(Cd11SocketConfig.builder()
        .setStationOrResponderName(STATION_NAME + 0)
        .setStationOrResponderType(STATION_TYPE)
        .setServiceType(SERVICE_TYPE)
        .setFrameCreator(FRAME_CREATOR)
        .setFrameDestination(FRAME_DESTINATION)
        .setAuthenticationKeyIdentifier(AUTHENTICATION_KEY_IDENTIFIER)
        .setProtocolMajorVersion(MAJOR_VERSION)
        .setProtocolMinorVersion(MINOR_VERSION)
        .build());

    cd11Socket.connect(
        DATA_CONSUMER_WELL_KNOWN_IP_ADDRESS, DATA_CONSUMER_ASSIGNED_PORT, 35000);

    for (int i = 0; i < framesToSend; i++) {
      cd11Socket.sendCd11DataFrame(sfArray, (long) i + 1);
    }

    cd11Socket.disconnect();
    checkFramesProcessed(false);
  }

  @Test
  public void testSendAlertFrame()
      throws InterruptedException, ExecutionException {
    setupCd11Consumers(1, false);

    Cd11Socket cd11Socket = new Cd11Socket(Cd11SocketConfig.builder()
        .setStationOrResponderName(STATION_NAME)
        .setStationOrResponderType(STATION_TYPE)
        .setServiceType(SERVICE_TYPE)
        .setFrameCreator(FRAME_CREATOR)
        .setFrameDestination(FRAME_DESTINATION)
        .setAuthenticationKeyIdentifier(AUTHENTICATION_KEY_IDENTIFIER)
        .setProtocolMajorVersion(MAJOR_VERSION)
        .setProtocolMinorVersion(MINOR_VERSION)
        .build());

    try {
      cd11Socket.connect(
          DATA_CONSUMER_WELL_KNOWN_IP_ADDRESS,
          DATA_CONSUMER_ASSIGNED_PORT,
          35000); // 35 seconds.
      cd11Socket.sendCd11AlertFrame("Terminating connection");
    } catch (Exception e) {
      logger.error(e.getMessage());
    } finally {
      cd11Socket.disconnect();
      checkFramesProcessed(false);
    }
  }

  private void setupCd11Consumers(int numStations, boolean ignoreFirstStation) {
    consumers = new ArrayList<>();
    mockRsdfProducers = new ArrayList<>();

    for (int i = 0; i < numStations; i++) {
      Cd11DataConsumerConfig dcConfig = Cd11DataConsumerConfig
          .builderWithDefaults(DATA_CONSUMER_ASSIGNED_PORT + i, STATION_NAME + i)
          .setThreadName(
              String.format("CD 1.1 Data Consumer (Port: %s)", DATA_CONSUMER_ASSIGNED_PORT + i))
          .setExpectedDataProviderIpAddress(DATA_PROVIDER_IP_ADDRESS)
          .setDataConsumerIpAddress(DATA_CONSUMER_WELL_KNOWN_IP_ADDRESS)
          .setStationDisabled(ignoreFirstStation && i == 0)
          .build();

      MockProducer<String, String> mockProducer = new MockProducer<>(true, null, null);
      mockRsdfProducers.add(mockProducer);
      consumers.add(
          new Cd11DataConsumer(dcConfig, mockDfrConfig, mockProducer, MOCK_RSDF_OUTPUT_TOPIC));
    }

    consumers.forEach(Cd11DataConsumer::start);
    consumers.forEach(Cd11DataConsumer::waitUntilThreadInitializes);
  }

  // Allocates a monitor thread that waits for the producers to have published to their topics
  private void checkFramesProcessed(boolean expectProcessed)
      throws ExecutionException, InterruptedException {
    producerMonitorExecutorService = Executors.newSingleThreadExecutor();

    try {
      runProducerMonitor(expectProcessed).get(PRODUCER_MONITOR_TIMEOUT_MS, TimeUnit.MILLISECONDS);
    } catch (TimeoutException e) {
      fail();
    }
  }

  private Future<Boolean> runProducerMonitor(boolean expectedProcessed) {
    return producerMonitorExecutorService.submit(() -> {
      while (true) {
        //Determine if we pass the check for processed RSDFs published to topics.
        // If we expect items to be published, then there should be no empty histories in any of the mock producers
        // However, if we do not expect them to be published, then none of the producers should have any history
        boolean passedCheck = !mockRsdfProducers.stream().map(producer -> producer.history().size())
            .collect(Collectors.toList()).contains(expectedProcessed ? 0 : 1);
        if (passedCheck) {
          return true;
        }
      }
    });
  }

  private static ByteBuffer initChannelSubframeBytes() {
    ByteBuffer sf = ByteBuffer.allocate(DATA_SUBFRAME_SIZE);

    for (int i = 0; i < CHANNEL_DATA.length; i++) {
      CHANNEL_DATA[i] = (byte) i;
    }

    // Subframe
    sf.putInt(CHANNEL_LENGTH);
    sf.putInt(AUTHENTICATION_OFFSET);
    sf.put(CHANNEL_DESCRIPTION_AUTHENTICATION);
    sf.put(CHANNEL_DESCRIPTION_TRANSFORMATION);
    sf.put(CHANNEL_DESCRIPTION_SENSOR_TYPE);
    sf.put(CHANNEL_DESCRIPTION_OPTION_FLAG);
    sf.put(CHANNEL_DESCRIPTION_SITE_NAME.getBytes());
    sf.put(CHANNEL_DESCRIPTION_CHANNEL_NAME.getBytes());
    sf.put(CHANNEL_DESCRIPTION_LOCATION.getBytes());
    sf.put(CHANNEL_DESCRIPTION_DATA_FORMAT.getBytes());
    sf.putInt(CHANNEL_DESCRIPTION_CALIB_FACTOR);
    sf.putInt(CHANNEL_DESCRIPTION_CALIB_PER);
    sf.put(TIME_STAMP.getBytes());
    sf.putInt(SUBFRAME_TIME_LENGTH);
    sf.putInt(SAMPLES);
    sf.putInt(CHANNEL_STATUS_SIZE);
    sf.putInt(CHANNEL_STATUS);
    sf.putInt(DATA_SIZE);
    sf.put(CHANNEL_DATA);
    sf.putInt(SUBFRAME_COUNT);
    sf.putInt(AUTH_KEY);
    sf.putInt(AUTH_SIZE);
    sf.putLong(AUTH_VALUE);

    return sf;
  }

  /**
   * Build default test Kafka Reactor Configuration
   */
  /*
  private ReactorKafkaConfiguration buildDefaultTestConfiguration() {
    return ReactorKafkaConfiguration.builder()
            .setApplicationId("cd11-data-consumer")
            .setBootstrapServers("kafka1:9092,kafka2:9092,kafka3:9092")
            .setInputRsdfTopic(RSDF_OUTPUT_TOPIC)
            .setOutputAcquiredChannelSohTopic("soh.acei")
            .setOutputStationSohInputTopic("soh.extract")
            .setKeySerializer("org.apache.kafka.common.serialization.Serdes$StringSerde")
            .setValueSerializer("org.apache.kafka.common.serialization.Serdes$StringSerde")
            .setNumberOfVerificationAttempts(1)
            .setStreamsCloseTimeoutMs(60000)
            .setConnectionRetryCount(10)
            .setRetryBackoffMs(1000L)
            .build();
  }
  */

  /**
   * Build single Cd11 Data Consumer configuration
   * @return
   */
  private Cd11DataConsumerConfig buildDataConsumerConfiguration() {
    return Cd11DataConsumerConfig
            .builderWithDefaults(DATA_CONSUMER_ASSIGNED_PORT,
                    RSDF_STATION_NAME)
            .setThreadName(
                    String.format("CD 1.1 Data Consumer (Port: %s)", DATA_CONSUMER_ASSIGNED_PORT))
            .setExpectedDataProviderIpAddress(DATA_PROVIDER_IP_ADDRESS)
            .setDataConsumerIpAddress(DATA_CONSUMER_WELL_KNOWN_IP_ADDRESS)
            .setStationDisabled(false)
            .build();

  }
}
