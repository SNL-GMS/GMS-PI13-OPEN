package gms.shared.frameworks.soh.repository.performancemonitoring.converter;

import gms.shared.frameworks.osd.coi.soh.ChannelSoh;
import gms.shared.frameworks.osd.coi.soh.SohMonitorValueAndStatus;
import gms.shared.frameworks.osd.coi.soh.StationAggregate;
import gms.shared.frameworks.osd.coi.soh.StationSoh;
import gms.shared.frameworks.osd.dao.channel.StationDao;
import gms.shared.frameworks.osd.dao.soh.ChannelSohDao;
import gms.shared.frameworks.osd.dao.soh.SohMonitorValueAndStatusDao;
import gms.shared.frameworks.osd.dao.soh.StationAggregateDao;
import gms.shared.frameworks.osd.dao.soh.StationSohDao;
import gms.shared.frameworks.utilities.jpa.EntityConverter;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;

public class StationSohDaoConverter implements EntityConverter<StationSohDao, StationSoh> {

  @Override
  public StationSohDao fromCoi(StationSoh coi, EntityManager entityManager) {
    Objects.requireNonNull(coi);
    Objects.requireNonNull(entityManager);

    var dao = new StationSohDao();

    dao.setCoiId(coi.getId());
    dao.setCreationTime(coi.getTime());

    dao.setStation(entityManager.getReference(StationDao.class, coi.getStationName()));
    dao.setSohStatus(coi.getSohStatusRollup());

    SohMonitorValueAndStatusDaoConverter smvsConverter =
        new SohMonitorValueAndStatusDaoConverter();
    Set<SohMonitorValueAndStatusDao> smvsDaos = coi
        .getSohMonitorValueAndStatuses()
        .stream()
        .map(smvs -> smvsConverter
            .fromCoi(smvs, entityManager))
        .peek(smvsDao -> smvsDao.setStationSoh(dao))
        .collect(Collectors.toSet());

    dao.setSohMonitorValueAndStatuses(smvsDaos);

    ChannelSohDaoConverter channelSohDaoConverter = new ChannelSohDaoConverter();
    Set<ChannelSohDao> channelSohDaos = coi.getChannelSohs()
        .stream()
        .map(channelSoh -> channelSohDaoConverter.fromCoi(channelSoh, entityManager))
        .peek(channelSohDao -> channelSohDao.setStationSoh(dao))
        .collect(Collectors.toSet());

    dao.setChannelSohs(channelSohDaos);

    StationAggregateDaoConverter stationAgConverter = new StationAggregateDaoConverter();
    Set<StationAggregateDao> allStationAggregateDaos = coi.getAllStationAggregates()
        .stream()
        .map(stationAg -> stationAgConverter
            .fromCoi(stationAg, entityManager))
        .peek(stationAgDao -> stationAgDao.setStationSoh(dao))
        .collect(Collectors.toSet());
    dao.setAllStationAggregate(allStationAggregateDaos);

    return dao;
  }

  @Override
  public StationSoh toCoi(StationSohDao entity) {
    Set<SohMonitorValueAndStatus<?>> smvs = entity.getSohMonitorValueAndStatuses()
        .stream()
        .map(smvsDao -> new SohMonitorValueAndStatusDaoConverter().toCoi(smvsDao))
        .collect(Collectors.toSet());

    Set<ChannelSoh> channelSohs = entity.getChannelSohs()
        .stream()
        .map(channelSohDao -> new ChannelSohDaoConverter().toCoi(channelSohDao))
        .collect(Collectors.toSet());

    Set<StationAggregate<?>> allStationAggregates = entity.getAllStationAggregate()
        .stream()
        .map(stationAgDao -> new StationAggregateDaoConverter().toCoi(stationAgDao))
        .collect(Collectors.toSet());

    return StationSoh.from(entity.getCoiId(),
        entity.getCreationTime(),
        entity.getStation().getName(),
        smvs,
        entity.getSohStatus(),
        channelSohs,
        allStationAggregates);
  }
}
