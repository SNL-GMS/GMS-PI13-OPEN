package gms.shared.frameworks.soh.repository.performancemonitoring.converter;

import com.google.common.base.Preconditions;
import gms.shared.frameworks.osd.coi.soh.ChannelSoh;
import gms.shared.frameworks.osd.coi.soh.SohMonitorValueAndStatus;
import gms.shared.frameworks.osd.dao.channel.ChannelDao;
import gms.shared.frameworks.osd.dao.soh.ChannelSohDao;
import gms.shared.frameworks.osd.dao.soh.SohMonitorValueAndStatusDao;
import gms.shared.frameworks.utilities.jpa.EntityConverter;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;

public class ChannelSohDaoConverter implements EntityConverter<ChannelSohDao, ChannelSoh> {

  @Override
  public ChannelSohDao fromCoi(ChannelSoh coi, EntityManager entityManager) {
    
    Objects.requireNonNull(coi);
    Objects.requireNonNull(entityManager);
    Preconditions.checkState(entityManager.getTransaction().isActive(),
        "An active transaction is required to convert a ChannelSoh");

    ChannelDao channelDao = entityManager.getReference(ChannelDao.class, coi.getChannelName());
    ChannelSohDao entity = new ChannelSohDao();

    entity.setChannel(channelDao);
    entity.setSohStatus(coi.getSohStatusRollup());

    SohMonitorValueAndStatusDaoConverter monitorValueAndStatusConverter =
        new SohMonitorValueAndStatusDaoConverter();
    Set<SohMonitorValueAndStatusDao> allMonitorValueAndStatusDaos =
        coi.getAllSohMonitorValueAndStatuses()
            .stream()
            .map(smvs -> monitorValueAndStatusConverter.fromCoi(smvs, entityManager))
            .peek(smvsDao -> smvsDao.setChannelSoh(entity))
            .collect(Collectors.toSet());

    entity.setAllMonitorValueAndStatuses(allMonitorValueAndStatusDaos);

    return entity;
  }

  @Override
  public ChannelSoh toCoi(ChannelSohDao entity) {
    Objects.requireNonNull(entity);

    SohMonitorValueAndStatusDaoConverter monitorValueStatusConverter =
        new SohMonitorValueAndStatusDaoConverter();
    Set<SohMonitorValueAndStatus<?>> allMonitorValueAndStatuses =
        entity.getAllMonitorValueAndStatuses()
        .stream()
        .map(monitorValueStatusConverter::toCoi)
        .collect(Collectors.toSet());

    return ChannelSoh.from(entity.getChannel().getName(),
        entity.getSohStatus(),
        allMonitorValueAndStatuses);
  }
}