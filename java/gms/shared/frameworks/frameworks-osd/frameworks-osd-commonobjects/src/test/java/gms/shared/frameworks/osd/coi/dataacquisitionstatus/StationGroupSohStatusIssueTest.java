package gms.shared.frameworks.osd.coi.dataacquisitionstatus;

import gms.shared.frameworks.osd.coi.util.TestUtilities;
import gms.shared.frameworks.osd.coi.channel.dataacquisitionstatus.StationSohIssue;
import org.junit.jupiter.api.Test;

class StationGroupSohStatusIssueTest {

  @Test
  void testSerialization() throws Exception {
    TestUtilities.testSerialization(DataAcquisitionStatusTestFixtures.acknowledged,
        StationSohIssue.class);
    TestUtilities.testSerialization(DataAcquisitionStatusTestFixtures.notAcknowledged,
        StationSohIssue.class);
  }

}
