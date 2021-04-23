package gms.shared.frameworks.osd.coi.stationgroupsoh.repository.jpa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import gms.shared.frameworks.osd.coi.channel.dataacquisitionstatus.StationSohIssue;
import org.junit.jupiter.api.Test;

public class StationGroupSohStatusIssueDaoTest {

  @Test
  public void testFromCoi() {
    StationSohIssue stnSohIss = TestFixtures.acknowledged;

    StationSohIssueDao dao = StationSohIssueDao.from(stnSohIss);
    assertNotNull(dao);

    assertEquals(TestFixtures.acknowledged.getRequiresAcknowledgement(),
        dao.isRequiresAcknowledgement());
    assertEquals(TestFixtures.acknowledged.getAcknowledgedAt(),
        dao.getAcknowledgedAt());
  }

  @Test
  public void testToCoi() {
    StationSohIssue stnSohIss = TestFixtures.acknowledged;
    StationSohIssueDao dao = StationSohIssueDao.from(stnSohIss);

    assertEquals(stnSohIss, dao.toCoi());
  }

}
