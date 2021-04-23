package gms.dataacquisition.stationreceiver.cd11.connman;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import gms.dataacquisition.stationreceiver.cd11.common.Cd11Socket;
import gms.dataacquisition.stationreceiver.cd11.common.configuration.Cd11SocketConfig;
import gms.dataacquisition.stationreceiver.cd11.connman.configuration.Cd11ConnectionConfig;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;


public class Cd11ConnectionTest {

  private final InetAddress SERVER_ADDRESS = InetAddress.getLoopbackAddress();
  private final int WELL_KNOWN_PORT = 63756;
  private final String STATION_NAME = "ABCD";

  private Socket socket = null;
  private ConnectionLog connectionLog;

  /**
   * Tests that the Cd11Connection handler functions, and produces logs.
   */
  @Test
  public void testCd11ConnectionFunctionality() throws Exception {
    // Start the socket listener.
    CountDownLatch dpConnectedLatch = new CountDownLatch(1);
    ServerSocket fakeConnectionManager = new ServerSocket(
        WELL_KNOWN_PORT, 0, SERVER_ADDRESS);
    Thread serverSocketThread = new Thread(() -> {
      try {
        Socket s = fakeConnectionManager.accept();
        s.setSoLinger(true, 3);
        this.setSocket(s);
        fakeConnectionManager.close();
        dpConnectedLatch.countDown();
      } catch (Exception e) {
        fail("Socket failed to listen.");
      }
    });
    serverSocketThread.start();

    // Connect to the fake Connection Manager.
    Cd11Socket fakeDataProvider = new Cd11Socket(Cd11SocketConfig.builderWithDefaults()
        .setStationOrResponderName(STATION_NAME)
        .build());
    fakeDataProvider.connect(SERVER_ADDRESS, WELL_KNOWN_PORT, 5000);
    fakeDataProvider.sendCd11ConnectionRequestFrame();
    assertTrue(dpConnectedLatch.await(500, TimeUnit.MILLISECONDS));
    assertNotNull(socket);

    // Hand the socket to the Cd11Connection handler.
    Cd11ConnectionConfig config = Cd11ConnectionConfig.builder().build();
    Cd11Connection cd11Connection = new Cd11Connection(
        config,
        socket,
        this::StationLookup,
        this::setConnectionLog,
        Map.of());
    cd11Connection.start();
    cd11Connection.waitUntilThreadStops();

    // Check that a connection log was generated.
    assertNotNull(this.connectionLog);
  }

  private void setSocket(Socket s) {
    this.socket = s;
  }

  private Cd11Station StationLookup(String stationName) {
    if (stationName.equals(STATION_NAME)) {
      return null;
    } else {
      return new Cd11Station(SERVER_ADDRESS, SERVER_ADDRESS, 59999);
    }
  }

  private void setConnectionLog(ConnectionLog connectionLog) {
    this.connectionLog = connectionLog;
  }
}
