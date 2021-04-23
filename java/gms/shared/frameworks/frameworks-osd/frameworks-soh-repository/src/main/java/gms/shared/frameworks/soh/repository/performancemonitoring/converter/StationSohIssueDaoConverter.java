package gms.shared.frameworks.soh.repository.performancemonitoring.converter;

import gms.shared.frameworks.osd.coi.channel.dataacquisitionstatus.StationSohIssue;
import gms.shared.frameworks.osd.dao.stationgroupsoh.StationSohIssueDao;
import gms.shared.frameworks.utilities.jpa.EntityConverter;
import java.util.Objects;
import javax.persistence.EntityManager;

public class StationSohIssueDaoConverter implements EntityConverter<StationSohIssueDao, StationSohIssue> {

  @Override
  public StationSohIssueDao fromCoi(StationSohIssue issue, EntityManager entityManager) {
    Objects.requireNonNull(issue);

    StationSohIssueDao issueDao = new StationSohIssueDao();
    issueDao.setRequiresAcknowledgement(issue.getRequiresAcknowledgement());
    issueDao.setAcknowledgedAt(issue.getAcknowledgedAt());

    return issueDao;
  }

  @Override
  public StationSohIssue toCoi(StationSohIssueDao issueDao) {
    Objects.requireNonNull(issueDao);

    return StationSohIssue.builder()
        .setRequiresAcknowledgement(issueDao.isRequiresAcknowledgement())
        .setAcknowledgedAt(issueDao.getAcknowledgedAt())
        .build();
  }

}
