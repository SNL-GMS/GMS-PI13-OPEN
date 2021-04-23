package gms.dataacquisition.stationreceiver.cd11.dataman;

import gms.dataacquisition.stationreceiver.cd11.common.Cd11Socket;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11ChannelSubframe;
import java.net.InetAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class exists to simply connect to a socket and send 1 Cd11DataFrame. Cd11DataConsumerTest
 * spawns off this class as a separate thread so it can mimic multiple stations sending data frames
 * at the same time.
 */
public class FakeStation implements Runnable {

  private static final Logger logger = LoggerFactory.getLogger(FakeStation.class);

  private Cd11Socket cd11Socket;
  private Cd11ChannelSubframe[] sfArray;
  private final InetAddress dataConsumerWellKnownIpAddress;
  private final Integer dataConsumerAssignedPort;

  public FakeStation(
      Cd11Socket cd11Socket, Cd11ChannelSubframe[] sfArray, InetAddress ip, Integer port) {
    this.cd11Socket = cd11Socket;
    this.sfArray = sfArray;
    this.dataConsumerWellKnownIpAddress = ip;
    this.dataConsumerAssignedPort = port;
  }

  @Override
  public void run() {
    try {
      cd11Socket.connect(
          this.dataConsumerWellKnownIpAddress, this.dataConsumerAssignedPort, 500);
      cd11Socket.sendCd11DataFrame(sfArray, 1);
    } catch (Exception e) {
      logger.error(e.getMessage());
    } finally {
      cd11Socket.disconnect();
    }
  }
}
