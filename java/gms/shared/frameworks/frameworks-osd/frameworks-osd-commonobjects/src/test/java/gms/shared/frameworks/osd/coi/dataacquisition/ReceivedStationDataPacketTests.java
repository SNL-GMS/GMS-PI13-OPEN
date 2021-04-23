package gms.shared.mechanisms.objectstoragedistribution.coi.dataacquisition;

import gms.shared.frameworks.osd.coi.util.TestUtilities;
import gms.shared.frameworks.osd.coi.dataacquisition.ReceivedStationDataPacket;
import java.io.IOException;
import java.time.Instant;
import org.junit.jupiter.api.Test;

public class ReceivedStationDataPacketTests {

  @Test
  public void serializationTest() throws IOException {
    TestUtilities.testSerialization(
        ReceivedStationDataPacket.from(new byte[] {(byte) 1},
            Instant.now(), 5L, "station"),
        ReceivedStationDataPacket.class);
  }

}
