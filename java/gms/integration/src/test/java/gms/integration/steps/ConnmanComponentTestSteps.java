package gms.integration.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.net.InetAddresses;
import gms.dataacquisition.stationreceiver.cd11.common.Cd11Socket;
import gms.dataacquisition.stationreceiver.cd11.common.FrameUtilities;
import gms.dataacquisition.stationreceiver.cd11.common.configuration.Cd11SocketConfig;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11ConnectionResponseFrame;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11Frame;

import gms.shared.frameworks.test.utils.services.GmsServiceType;
import io.cucumber.java.Before;
import io.cucumber.java.en.Then;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.junit.jupiter.Testcontainers;


@Testcontainers
public class ConnmanComponentTestSteps {

  private static final Logger logger = LoggerFactory.getLogger(ConnmanComponentTestSteps.class);

  private final int PORTMIN=0;
  private final int PORTMAX=65535;
  private final int MAXSOCKETWAITTIME=2000;
  private final int SERVICE_PORT=8041;
  private final int LOCAL_PORT=65001;

  private Environment environment;

  public ConnmanComponentTestSteps(Environment environment) {
    this.environment = environment;

    String serviceHost = this.environment.deploymentCtxt().getServiceHost(GmsServiceType.CONNMAN);
    logger.info("Service host: {}", serviceHost);
    logger.info("Service port: {}", SERVICE_PORT);
    logger.info("\n");
  }

  @Then("connman responds to connection request correctly for station {string}")
  public void connmanWorks(String stationName) throws UnknownHostException {

    Cd11ConnectionResponseFrame connResponseFrame = null;

    // Connect to the Connection Manager.


    Cd11Socket cd11Socket = new Cd11Socket(Cd11SocketConfig.builderWithDefaults()
        .setStationOrResponderName(stationName)
        .build());
    try {
      cd11Socket
          .connect(InetAddress.getByName(this.environment.deploymentCtxt().getServiceHost(GmsServiceType.CONNMAN)),
                  this.environment.deploymentCtxt().getServicePort(GmsServiceType.CONNMAN),
                  MAXSOCKETWAITTIME,
                  InetAddress.getByName(this.environment.deploymentCtxt().getServiceHost(GmsServiceType.CONNMAN)),
                  LOCAL_PORT);

      logger.info("Local ip addr: {}", cd11Socket.getLocalIpAddressAsString());
      logger.info("Remote ip addr: {}", cd11Socket.getRemoteIpAddressAsString());
      logger.info("Remote port: {}", cd11Socket.getRemotePort());
      logger.info("Cd11 Socket send Cd11ConnectionRequestFrame!");
      logger.info("\n");

      cd11Socket.sendCd11ConnectionRequestFrame();

      // Wait for response from the Connection Manager.
      Cd11Frame resp = cd11Socket.read(MAXSOCKETWAITTIME);
      connResponseFrame =
          resp.asFrameType(Cd11ConnectionResponseFrame.class);
      connResponseFrame.toString();
      logger.info("Connection Response Frame (from ConnMan):");
      logger.info(connResponseFrame.toString());
      logger.info("\n");

    } catch(IOException e){
      e.printStackTrace();
      Assert.fail("connecting to connman should not throw an exception " + e.getMessage());
    }

    assertEquals(InetAddresses.forString(InetAddresses.toAddrString(InetAddresses.fromInteger(connResponseFrame.ipAddress))), InetAddress.getByName(this.environment.deploymentCtxt().getServiceHost(GmsServiceType.CONNMAN)));

    assertTrue(connResponseFrame.port >= PORTMIN && connResponseFrame.port <= PORTMAX);
  }
}
