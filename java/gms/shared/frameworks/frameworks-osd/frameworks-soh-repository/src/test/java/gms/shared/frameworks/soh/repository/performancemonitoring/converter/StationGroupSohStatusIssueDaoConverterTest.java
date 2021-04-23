package gms.shared.frameworks.soh.repository.performancemonitoring.converter;

import gms.shared.frameworks.osd.coi.channel.dataacquisitionstatus.StationSohIssue;
import gms.shared.frameworks.osd.dao.stationgroupsoh.StationSohIssueDao;
import gms.shared.frameworks.soh.repository.util.DbTest;
import org.junit.jupiter.api.Test;
import javax.persistence.EntityManager;
import java.time.Instant;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class StationGroupSohStatusIssueDaoConverterTest extends DbTest {
  
  @Test
  void testFromCoiValidation() {
    EntityManager entityManager = entityManagerFactory.createEntityManager();

    try {
      assertThrows(NullPointerException.class,
          () -> new StationSohIssueDaoConverter().fromCoi(null, entityManager));
    } finally {
      entityManager.close();
    }
  }

  @Test
  void fromCoi() {
    EntityManager entityManager = entityManagerFactory.createEntityManager();

    try {
      StationSohIssue expected = StationSohIssue.from(true, Instant.EPOCH);
      StationSohIssueDao actual = new StationSohIssueDaoConverter().fromCoi(expected, entityManager);

      assertEquals(expected.getRequiresAcknowledgement(), actual.isRequiresAcknowledgement());
      assertEquals(expected.getAcknowledgedAt(), actual.getAcknowledgedAt());
    } finally {
      entityManager.close();
    }
  }

  @Test
  void toCoi() {
    StationSohIssueDao expected = new StationSohIssueDao();
    expected.setId(3);
    expected.setAcknowledgedAt(Instant.EPOCH);
    expected.setRequiresAcknowledgement(false);

    StationSohIssue actual = new StationSohIssueDaoConverter().toCoi(expected);
    assertEquals(expected.getAcknowledgedAt(), actual.getAcknowledgedAt());
    assertEquals(expected.isRequiresAcknowledgement(), actual.getRequiresAcknowledgement());
  }
}