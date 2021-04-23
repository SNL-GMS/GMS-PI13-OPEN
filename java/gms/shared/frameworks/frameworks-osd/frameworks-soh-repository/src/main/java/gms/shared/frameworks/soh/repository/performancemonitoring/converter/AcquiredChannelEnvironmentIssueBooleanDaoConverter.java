package gms.shared.frameworks.soh.repository.performancemonitoring.converter;

import com.google.common.base.Preconditions;
import gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssueBoolean;
import gms.shared.frameworks.osd.dao.channel.ChannelDao;
import gms.shared.frameworks.osd.dao.channelsoh.AcquiredChannelEnvironmentIssueBooleanDao;
import gms.shared.frameworks.utilities.jpa.EntityConverter;
import java.util.Objects;
import javax.persistence.EntityManager;

public class AcquiredChannelEnvironmentIssueBooleanDaoConverter extends AcquiredChannelEnvironmentIssueDaoConverter
    implements EntityConverter<AcquiredChannelEnvironmentIssueBooleanDao, AcquiredChannelEnvironmentIssueBoolean> {

  private boolean updateExisting = true;
  @Override
  public AcquiredChannelEnvironmentIssueBooleanDao fromCoi(AcquiredChannelEnvironmentIssueBoolean sohBoolean,
      EntityManager entityManager) {

    Objects.requireNonNull(sohBoolean);
    Objects.requireNonNull(entityManager);
    Preconditions.checkState(entityManager.getTransaction().isActive());

    AcquiredChannelEnvironmentIssueBooleanDao sohBooleanDao = null;
    //TODO - when inserting ACEI in bulk, we always insert, don't update...does this need to be here
    if(updateExisting) {
      sohBooleanDao = entityManager.find(AcquiredChannelEnvironmentIssueBooleanDao.class, sohBoolean.getId());
    }
    if (sohBooleanDao == null) {
      sohBooleanDao = new AcquiredChannelEnvironmentIssueBooleanDao();
      sohBooleanDao.setId(sohBoolean.getId());
    }
    sohBooleanDao.setNaturalId(Objects.hash(
            sohBoolean.getChannelName(),
            sohBoolean.getStartTime(),
            sohBoolean.getType().name()
    ));
    ChannelDao channelDao = entityManager.getReference(ChannelDao.class, sohBoolean.getChannelName());

    sohBooleanDao.setChannel(channelDao);
    sohBooleanDao.setType(sohBoolean.getType());
    sohBooleanDao.setStartTime(sohBoolean.getStartTime());
    sohBooleanDao.setEndTime(sohBoolean.getEndTime());
    sohBooleanDao.setStatus(sohBoolean.getStatus());

    return sohBooleanDao;
  }

  @Override
  public AcquiredChannelEnvironmentIssueBoolean toCoi(
      AcquiredChannelEnvironmentIssueBooleanDao sohBooleanDao) {
    Objects.requireNonNull(sohBooleanDao);

    return AcquiredChannelEnvironmentIssueBoolean.from(sohBooleanDao.getId(),
        sohBooleanDao.getChannel().getName(),
        sohBooleanDao.getType(),
        sohBooleanDao.getStartTime(),
        sohBooleanDao.getEndTime(),
        sohBooleanDao.isStatus());
  }

  public boolean isUpdateExisting() {
    return this.updateExisting;
  }

  public void setUpdateExisting(final boolean updateExisting) {
    this.updateExisting = updateExisting;
  }
}
