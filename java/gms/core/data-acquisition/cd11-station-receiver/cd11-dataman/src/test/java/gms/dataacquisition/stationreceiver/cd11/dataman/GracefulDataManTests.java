package gms.dataacquisition.stationreceiver.cd11.dataman;

import static gms.shared.utilities.javautilities.assertwait.AssertWait.assertTrueWait;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.BDDMockito.given;

import gms.core.dataacquisition.receiver.DataFrameReceiverConfiguration;
import gms.dataacquisition.stationreceiver.cd11.common.Cd11Socket;
import gms.dataacquisition.stationreceiver.cd11.common.configuration.Cd11DataConsumerParameters;
import gms.dataacquisition.stationreceiver.cd11.common.configuration.Cd11SocketConfig;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11AlertFrame;
import gms.dataacquisition.stationreceiver.cd11.dataman.configuration.Cd11DataConsumerConfig;
import gms.dataacquisition.stationreceiver.cd11.dataman.configuration.DataManConfig;
import gms.dataacquisition.stationreceiver.cd11.dataman.configuration.KafkaConnectionConfiguration;
import java.net.InetAddress;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.kafka.clients.producer.MockProducer;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@Disabled("These tests have proven to be unreliable in the pipeline due to the dynamic acquisition "
    + "of ports for socket connections, which can easily conflict in a shared system. "
    + "Tests are disabled until converted to be more robust or rendered OBE and removed entirely.")
@ExtendWith(MockitoExtension.class)
class GracefulDataManTests {

  private static Random randGen;

  static {
    try {
      randGen = SecureRandom.getInstanceStrong();
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
    }
  }

  private static final int PORT_LOWER_BOUND = 49152;
  private static final int PORT_UPPER_BOUND = 65505;
  private static final int CONSUMER_CONFIG_LOCAL_PORT =
      randGen.nextInt(PORT_UPPER_BOUND - PORT_LOWER_BOUND + 1) + PORT_LOWER_BOUND;
  private static final int COSUMER_PARAMS_PORT =
      randGen.nextInt(PORT_UPPER_BOUND - PORT_LOWER_BOUND + 1) + PORT_LOWER_BOUND;

  private static final InetAddress LOOPBACK_ADDRESS = InetAddress.getLoopbackAddress();

  private final Cd11DataConsumerConfig testStation1 = Cd11DataConsumerConfig.builderWithDefaults(
      CONSUMER_CONFIG_LOCAL_PORT,
      "DP1")
      .setExpectedDataProviderIpAddress(LOOPBACK_ADDRESS)
      .setThreadName("TEST_DC_1")
      .setDataConsumerIpAddress(LOOPBACK_ADDRESS)
      .build();

  private final Cd11DataConsumerParameters defaultStationParameters = Cd11DataConsumerParameters
      .builder()
      .setPort(COSUMER_PARAMS_PORT)
      .setStationName("ASAR")
      .setAcquired(true)
      .setFrameProcessingDisabled(false)
      .build();

  private MockProducer<String, String> mockProducer = new MockProducer<>();

  private ExecutorService producerMonitorExecutorService;

  private static final int PRODUCER_MONITOR_TIMEOUT_MS = 1000;

  @Mock
  private DataManConfig mockDatamanConfig;

  @Mock
  private DataFrameReceiverConfiguration mockDataFrameReceiverConfig;

  @Mock
  private KafkaConnectionConfiguration mockKafkaConnectionConfiguration;

  @Test
  void testDataManManageConsumer() throws Exception {
    given(mockDatamanConfig.getCd11StationParameters())
        .willReturn(List.of(defaultStationParameters));

    // Run the DC Manager.
    GracefulCd11DataMan dataMan = GracefulCd11DataMan
        .create(mockDatamanConfig, mockDataFrameReceiverConfig,
            new MockProducerFactory<>(), mockKafkaConnectionConfiguration);
    dataMan.start();
    dataMan.waitUntilThreadInitializes();

    // Test that the thread is running.
    assertTrue(dataMan.isRunning());

    // Count the number of data consumers spawned by default.
    int baselineTotalDCs = dataMan.getTotalDataConsumerThreads();

    // Check that our test station is not currently registered.
    assertFalse(dataMan.isDataConsumerPortRegistered(testStation1.getDataConsumerPort()));

    // Add a data consumer.
    dataMan.addDataConsumer(testStation1, mockProducer);

    // Now check that our test station is registered.
    assertTrueWait(
        () -> dataMan.isDataConsumerPortRegistered(testStation1.getDataConsumerPort()),
        100);

    // Test that the number of Data Consumers registered matches the expectation.
    assertEquals(dataMan.getTotalDataConsumerThreads(), (baselineTotalDCs + 1));
    assertEquals(dataMan.getPorts().size(), (baselineTotalDCs + 1));

    // Connect to the test station.
    Cd11Socket cd11Socket = new Cd11Socket(Cd11SocketConfig.builderWithDefaults()
        .setStationOrResponderName(testStation1.getDataProviderStationName())
        .build());
    cd11Socket.connect(
        testStation1.getDataConsumerIpAddress(), testStation1.getDataConsumerPort(),
        10000,
        testStation1.getDataConsumerIpAddress(), CONSUMER_CONFIG_LOCAL_PORT + 30);

    // Send and receive an Alert frame.
    cd11Socket.sendCd11AlertFrame("Time to shut down Data Consumer.");
    Cd11AlertFrame alertFrame = cd11Socket.read(10000)
        .asFrameType(Cd11AlertFrame.class);
    System.out.println(String.format("Data Consumer's Alert message: %s", alertFrame.message));


    cd11Socket.disconnect();

    // Remove the test station.
    dataMan.removeDataConsumer(testStation1.getDataConsumerPort());

    // Check that one less station is registered.
    assertFalse(dataMan.isDataConsumerPortRegistered(testStation1.getDataConsumerPort()));
    assertEquals(dataMan.getTotalDataConsumerThreads(), baselineTotalDCs);
    assertEquals(dataMan.getPorts().size(), baselineTotalDCs);

    // Test that the DC Manager can be stopped.
    dataMan.stop();
    dataMan.waitUntilThreadStops();
    assertFalse(dataMan.isRunning());
  }

  @Test
  void testDataManStationNotAcquired() {
    Cd11DataConsumerParameters notAcquired = defaultStationParameters.toBuilder()
        .setAcquired(false)
        .build();

    given(mockDatamanConfig.getCd11StationParameters())
        .willReturn(List.of(notAcquired));

    // Run the DC Manager.
    GracefulCd11DataMan dataMan = GracefulCd11DataMan
        .create(mockDatamanConfig, mockDataFrameReceiverConfig,
            new MockProducerFactory<>(), mockKafkaConnectionConfiguration);
    dataMan.start();
    dataMan.waitUntilThreadInitializes();

    // Test that the thread is running.
    assertTrue(dataMan.isRunning());

    // Count the number of data consumers spawned by default.
    int baselineTotalDCs = dataMan.getTotalDataConsumerThreads();

    // Check that our test station is not currently registered.
    assertEquals(0, baselineTotalDCs);
    assertFalse(dataMan.isDataConsumerPortRegistered(defaultStationParameters.getPort()));

    // Test that the DC Manager can be stopped.
    dataMan.stop();
    dataMan.waitUntilThreadStops();
    assertFalse(dataMan.isRunning());
  }

  @Test
  void testDataManStationDisabled() throws Exception {

    given(mockDatamanConfig.getCd11StationParameters())
        .willReturn(List.of(defaultStationParameters));

    // Run the DC Manager.
    GracefulCd11DataMan dataMan = GracefulCd11DataMan
        .create(mockDatamanConfig, mockDataFrameReceiverConfig,
            new MockProducerFactory<>(), mockKafkaConnectionConfiguration);
    dataMan.start();
    dataMan.waitUntilThreadInitializes();

    // Test that the thread is running.
    assertTrue(dataMan.isRunning());

    // Count the number of data consumers spawned by default.
    int baselineTotalDCs = dataMan.getTotalDataConsumerThreads();

    // Check that our test station is not currently registered.
    assertFalse(dataMan.isDataConsumerPortRegistered(testStation1.getDataConsumerPort()));

    Cd11DataConsumerConfig disabledStation = Cd11DataConsumerConfig.builderWithDefaults(
        CONSUMER_CONFIG_LOCAL_PORT,
        "DP1")
        .setExpectedDataProviderIpAddress(LOOPBACK_ADDRESS)
        .setThreadName("TEST_DC_1")
        .setDataConsumerIpAddress(LOOPBACK_ADDRESS)
        .setStationDisabled(true)
        .build();

    // Add a data consumer.
    dataMan.addDataConsumer(disabledStation, mockProducer);

    // Now check that our test station is registered.
    assertTrueWait(
        () -> dataMan.isDataConsumerPortRegistered(testStation1.getDataConsumerPort()),
        100);

    // Test that the number of Data Consumers registered matches the expectation.
    assertEquals(dataMan.getTotalDataConsumerThreads(), (baselineTotalDCs + 1));
    assertEquals(dataMan.getPorts().size(), (baselineTotalDCs + 1));

    // Connect to the test station.
    Cd11Socket cd11Socket = new Cd11Socket(Cd11SocketConfig.builderWithDefaults()
        .setStationOrResponderName(disabledStation.getDataProviderStationName())
        .build());
    cd11Socket.connect(
        disabledStation.getDataConsumerIpAddress(), disabledStation.getDataConsumerPort(),
        10000,
        disabledStation.getDataConsumerIpAddress(), CONSUMER_CONFIG_LOCAL_PORT + 30);

    // Send and receive a Data frame.
    cd11Socket.sendCd11DataFrame(FakeDataFrame.generateFakeChannelSubframes(), 1);

    producerMonitorExecutorService = Executors.newSingleThreadExecutor();

    try {
      expectNoDataPublished();

      // We don't expect to reach here
      cd11Socket.disconnect();
      fail(String
          .format("Expected no data to be published to kafka producer. Instead published:\n%s",
              mockProducer.history()));
    } catch (TimeoutException e) {
      cd11Socket.disconnect();

      // Remove the test station.
      dataMan.removeDataConsumer(disabledStation.getDataConsumerPort());

      // Check that one less station is registered.
      assertFalse(dataMan.isDataConsumerPortRegistered(disabledStation.getDataConsumerPort()));
      assertEquals(dataMan.getTotalDataConsumerThreads(), baselineTotalDCs);
      assertEquals(dataMan.getPorts().size(), baselineTotalDCs);

      // Test that the DC Manager can be stopped.
      dataMan.stop();
      dataMan.waitUntilThreadStops();
      assertFalse(dataMan.isRunning());
    }
  }

  private void expectNoDataPublished()
      throws InterruptedException, java.util.concurrent.ExecutionException, TimeoutException {
    // We expect this method to throw a TimeoutException, since no data should be published
    producerMonitorExecutorService.submit(() -> {
      while (true) {
        if (mockProducer.history().size() > 0) {
          return;
        }
      }
    }).get(PRODUCER_MONITOR_TIMEOUT_MS, TimeUnit.MILLISECONDS);
  }
}
