package gms.dataacquisition.stationreceiver.cd11.connman;

import static gms.shared.utilities.javautilities.assertwait.AssertWait.assertTrueWait;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willReturn;

import com.google.common.net.InetAddresses;
import gms.dataacquisition.stationreceiver.cd11.common.Cd11Socket;
import gms.dataacquisition.stationreceiver.cd11.common.FrameUtilities;
import gms.dataacquisition.stationreceiver.cd11.common.configuration.Cd11SocketConfig;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11ConnectionRequestFrame;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11ConnectionResponseFrame;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11Frame;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11FrameHeader;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11FrameTrailer;
import gms.dataacquisition.stationreceiver.cd11.connman.configuration.Cd11ConnManConfig;
import gms.shared.frameworks.systemconfig.SystemConfig;
import java.net.InetAddress;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
class Cd11ConnManTest {

  private static final short MAJOR_VERSION = 0;
  private static final short MINOR_VERSION = 1;
  private static final int PORT = 111;
  private static final Integer SECOND_PORT = 111;
  private static final String RESPONDER_NAME = "H07N";
  private static final String RESPONDER_TYPE = "IDC";
  private static final String SERVICE_TYPE = "TCP";
  private static final InetAddress IP_ADDRESS = InetAddress.getLoopbackAddress();
  private static final InetAddress SECOND_IP_ADDRESS = InetAddress.getLoopbackAddress();

  private static Cd11ConnectionRequestFrame connRequestFrame;
  private static Cd11FrameHeader frameHeader;

  static {
    try {
      frameHeader = FrameHeaderTestUtility.createHeaderForConnectionRequest(
          "KCC:IDC", "KCC:0");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static Cd11FrameTrailer frameTrailer = new Cd11FrameTrailer(1, new byte[0]);

  @Mock
  private Cd11ConnManConfig mockConnmanConfig;

  @Mock
  private SystemConfig mockSystemConfig;

  @BeforeAll
  static void setup() {
    connRequestFrame = new Cd11ConnectionRequestFrame(
        MAJOR_VERSION, MINOR_VERSION, RESPONDER_NAME, RESPONDER_TYPE, SERVICE_TYPE,
        IP_ADDRESS, PORT, SECOND_IP_ADDRESS, SECOND_PORT);
    connRequestFrame.setFrameHeader(frameHeader);
    connRequestFrame.setFrameTrailer(frameTrailer);
  }

  /**
   * Tests that ConnMan can receive a connection request, and send a connection response.
   */
  @Test
  void requestAndEstablishConnection() throws Exception {
    willReturn("localhost").given(mockSystemConfig).getValue("data-manager-ip-address");
    willReturn(8041).given(mockSystemConfig).getValueAsInt("connection-manager-well-known-port");
    willReturn("127.0.0.1").given(mockSystemConfig).getValue("data-provider-ip-address");

    given(mockConnmanConfig.getResponderName())
        .willReturn(Cd11ConnManConfig.DEFAULT_RESPONDER_NAME);
    given(mockConnmanConfig.getResponderType())
        .willReturn(Cd11ConnManConfig.DEFAULT_RESPONDER_TYPE);
    given(mockConnmanConfig.getServiceType()).willReturn(Cd11ConnManConfig.DEFAULT_SERVICE_TYPE);
    given(mockConnmanConfig.getFrameCreator()).willReturn(Cd11ConnManConfig.DEFAULT_FRAME_CREATOR);
    given(mockConnmanConfig.getFrameDestination())
        .willReturn(Cd11ConnManConfig.DEFAULT_FRAME_DESTINATION);

    final GracefulCd11ConnMan connMan = GracefulCd11ConnMan.create(mockSystemConfig, mockConnmanConfig);

    Thread t1 = new Thread(connMan::start);
    t1.start();

    //int baselineTotalCd11Stations = connMan.getTotalCd11Stations();
    int baselineTotalCd11Stations = 0;

    // Add a CD 1.1 station.
    String newStationName = "BLAH";
    InetAddress newStationExpectedIpAddress = InetAddress.getLoopbackAddress();
    InetAddress newStationDataConsumerIpAddress = InetAddresses.forString("22.22.22.22");
    int newStationDataConsumerPortNumber = 48484;
    connMan.addCd11Station(
        newStationName,
        newStationExpectedIpAddress,
        newStationDataConsumerIpAddress,
        newStationDataConsumerPortNumber);
    assertEquals(connMan.getTotalCd11Stations(), baselineTotalCd11Stations + 1);
    Cd11Station blahStation = connMan.lookupCd11Station(newStationName);
    assertNotNull(blahStation);
    assertEquals(blahStation.expectedDataProviderIpAddress, newStationExpectedIpAddress);
    assertEquals(blahStation.dataConsumerIpAddress, newStationDataConsumerIpAddress);
    assertEquals(blahStation.dataConsumerPort, newStationDataConsumerPortNumber);

    // Check for zero logs.
    assertEquals(0, connMan.getTotalConnectionLogs());

    // Connect to the Connection Manager.
    Cd11Socket cd11Socket = new Cd11Socket(Cd11SocketConfig.builderWithDefaults()
        .setStationOrResponderName(newStationName)
        .build());
    cd11Socket.connect(InetAddresses.forString(mockSystemConfig.getValue("data-provider-ip-address")),
        mockSystemConfig.getValueAsInt("connection-manager-well-known-port"), 2000, 65001);

    // Send a Connection Request frame to the Connection Manager.
    cd11Socket.sendCd11ConnectionRequestFrame();

    // Wait for response from the Connection Manager.
    Cd11Frame resp = cd11Socket.read(2000);
    Cd11ConnectionResponseFrame connResponseFrame =
        resp.asFrameType(Cd11ConnectionResponseFrame.class);

    // Check the CD 1.1 connection response.
    assertEquals(InetAddresses.toAddrString(InetAddresses.fromInteger(connResponseFrame.ipAddress)),
        newStationDataConsumerIpAddress.getHostAddress());
    assertEquals(connResponseFrame.port, newStationDataConsumerPortNumber);

    // Check that one log was made.
    assertTrueWait(() -> connMan.getTotalConnectionLogs() == 1, 1000);

    // Remove the CD 1.1 station.
    connMan.removeCd11Station(newStationName);
    assertEquals(connMan.getTotalCd11Stations(), baselineTotalCd11Stations);

    // close the connMan thread and cd11socket
    connMan.onStop();
    cd11Socket.disconnect();
  }
}

