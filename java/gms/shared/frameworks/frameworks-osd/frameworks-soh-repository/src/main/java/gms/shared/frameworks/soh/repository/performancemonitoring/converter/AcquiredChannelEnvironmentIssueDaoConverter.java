package gms.shared.frameworks.soh.repository.performancemonitoring.converter;

import com.google.common.base.Preconditions;
import gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssue;
import gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssueBoolean;
import gms.shared.frameworks.osd.dao.channel.ChannelDao;
import gms.shared.frameworks.osd.dao.channelsoh.AcquiredChannelEnvironmentIssueBooleanDao;
import gms.shared.frameworks.osd.dao.channelsoh.AcquiredChannelEnvironmentIssueDao;
import gms.shared.frameworks.utilities.jpa.EntityConverter;
import java.util.Objects;
import javax.persistence.EntityManager;

public class AcquiredChannelEnvironmentIssueDaoConverter {
  protected boolean updateExisting = true;

  public boolean isUpdateExisting() {
    return this.updateExisting;
  }

  public void setUpdateExisting(final boolean updateExisting) {
    this.updateExisting = updateExisting;
  }


}
