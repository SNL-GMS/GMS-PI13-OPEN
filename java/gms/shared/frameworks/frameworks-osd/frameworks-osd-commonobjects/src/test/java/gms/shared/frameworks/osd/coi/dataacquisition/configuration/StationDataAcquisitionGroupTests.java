package gms.shared.frameworks.osd.coi.dataacquisition.configuration;

import gms.shared.frameworks.osd.coi.util.TestUtilities;
import gms.shared.frameworks.osd.coi.dataacquisition.StationAndChannelId;
import gms.shared.frameworks.osd.coi.waveforms.AcquisitionProtocol;
import java.io.IOException;
import java.time.Instant;
import org.junit.jupiter.api.Test;


class StationDataAcquisitionGroupTests {

  @Test
  void testSerialization() throws IOException {
    TestUtilities.testSerialization(
        StationDataAcquisitionGroup.builder()
            .addRequestString("request")
            .setProtocol(AcquisitionProtocol.CD11)
            .setProviderIpAddress("127.0.0.1")
            .setProviderPort(4000)
            .setActualChangeTime(Instant.EPOCH)
            .setSystemChangeTime(Instant.EPOCH)
            .putIdByRecievedName("foo", StationAndChannelId.from("station", "channel"))
            .setActive(true)
            .setComment("comment")
            .build(),
        StationDataAcquisitionGroup.class);
  }

}
