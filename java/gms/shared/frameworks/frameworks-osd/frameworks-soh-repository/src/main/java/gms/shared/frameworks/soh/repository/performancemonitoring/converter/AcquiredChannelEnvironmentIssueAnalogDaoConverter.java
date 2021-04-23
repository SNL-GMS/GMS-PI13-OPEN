package gms.shared.frameworks.soh.repository.performancemonitoring.converter;

import com.google.common.base.Preconditions;
import gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssueAnalog;
import gms.shared.frameworks.osd.dao.channel.ChannelDao;
import gms.shared.frameworks.osd.dao.channelsoh.AcquiredChannelEnvironmentIssueAnalogDao;
import gms.shared.frameworks.utilities.jpa.EntityConverter;
import java.util.Objects;
import javax.persistence.EntityManager;

public class AcquiredChannelEnvironmentIssueAnalogDaoConverter extends AcquiredChannelEnvironmentIssueDaoConverter
    implements EntityConverter<AcquiredChannelEnvironmentIssueAnalogDao, AcquiredChannelEnvironmentIssueAnalog> {

  @Override
  public AcquiredChannelEnvironmentIssueAnalogDao fromCoi(AcquiredChannelEnvironmentIssueAnalog sohAnalog,
      EntityManager entityManager) {
    Objects.requireNonNull(sohAnalog);
    Objects.requireNonNull(entityManager);
    Preconditions.checkState(entityManager.getTransaction().isActive());

    AcquiredChannelEnvironmentIssueAnalogDao sohAnalogDao = null;

    //TODO - when inserting ACEI in bulk, we always insert, don't update...does this need to be here
    if(updateExisting) {
      sohAnalogDao = entityManager.find(AcquiredChannelEnvironmentIssueAnalogDao.class, sohAnalog.getId());
    }

    if (sohAnalogDao == null) {
      sohAnalogDao = new AcquiredChannelEnvironmentIssueAnalogDao();
      sohAnalogDao.setId(sohAnalog.getId());
    }
    sohAnalogDao.setNaturalId(Objects.hash(
        sohAnalog.getChannelName(),
        sohAnalog.getStartTime(),
        sohAnalog.getType().name()
    ));
    ChannelDao channelDao = entityManager.getReference(ChannelDao.class, sohAnalog.getChannelName());

    sohAnalogDao.setChannel(channelDao);
    sohAnalogDao.setType(sohAnalog.getType());
    sohAnalogDao.setStartTime(sohAnalog.getStartTime());
    sohAnalogDao.setEndTime(sohAnalog.getEndTime());
    sohAnalogDao.setStatus(sohAnalog.getStatus());

    return sohAnalogDao;
  }

  @Override
  public AcquiredChannelEnvironmentIssueAnalog toCoi(
      AcquiredChannelEnvironmentIssueAnalogDao sohAnalogDao) {
    Objects.requireNonNull(sohAnalogDao);

    return AcquiredChannelEnvironmentIssueAnalog.from(sohAnalogDao.getId(),
        sohAnalogDao.getChannel().getName(),
        sohAnalogDao.getType(),
        sohAnalogDao.getStartTime(),
        sohAnalogDao.getEndTime(),
        sohAnalogDao.getStatus());
  }

}
