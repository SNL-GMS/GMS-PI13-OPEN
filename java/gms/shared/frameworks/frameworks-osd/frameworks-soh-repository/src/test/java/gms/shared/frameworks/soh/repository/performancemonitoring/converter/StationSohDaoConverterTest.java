package gms.shared.frameworks.soh.repository.performancemonitoring.converter;

import static gms.shared.frameworks.osd.coi.SohTestFixtures.BAD_MISSING_LAG_SEAL_CHANNEL_SOH;
import static gms.shared.frameworks.osd.coi.SohTestFixtures.LAG_STATION_AGGREGATE;
import static gms.shared.frameworks.osd.coi.SohTestFixtures.MARGINAL_LAG_SOH_MONITOR_VALUE_AND_STATUS;
import static gms.shared.frameworks.osd.coi.SohTestFixtures.MARGINAL_MISSING_SOH_MONITOR_VALUE_AND_STATUS;
import static gms.shared.frameworks.osd.coi.SohTestFixtures.MARGINAL_STATION_SOH;
import static gms.shared.frameworks.osd.coi.SohTestFixtures.MISSING_STATION_AGGREGATE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import gms.shared.frameworks.osd.coi.soh.ChannelSoh;
import gms.shared.frameworks.osd.coi.soh.SohMonitorValueAndStatus;
import gms.shared.frameworks.osd.coi.soh.SohStatus;
import gms.shared.frameworks.osd.coi.soh.StationSoh;
import gms.shared.frameworks.osd.coi.test.utils.UtilsTestFixtures;
import gms.shared.frameworks.osd.dao.channel.StationDao;
import gms.shared.frameworks.osd.dao.soh.ChannelSohDao;
import gms.shared.frameworks.osd.dao.soh.SohMonitorValueAndStatusDao;
import gms.shared.frameworks.osd.dao.soh.StationAggregateDao;
import gms.shared.frameworks.osd.dao.soh.StationSohDao;
import gms.shared.frameworks.soh.repository.station.StationRepositoryJpa;
import gms.shared.frameworks.soh.repository.util.DbTest;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.persistence.EntityManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class StationSohDaoConverterTest extends DbTest {

  @BeforeAll
  static void testSuiteSetup() {
    new StationRepositoryJpa(entityManagerFactory)
      .storeStations(List.of(UtilsTestFixtures.STATION));
  }

  @ParameterizedTest
  @MethodSource("getFromCoiArguments")
  void testFromCoiValidation(Class<? extends Exception> expectedException,
    StationSoh stationSoh,
    EntityManager entityManager) {
    try {
      assertThrows(expectedException,
        () -> new StationSohDaoConverter().fromCoi(stationSoh, entityManager));
    } finally {
      if (entityManager != null) {
        entityManager.close();
      }
    }
  }

  static Stream<Arguments> getFromCoiArguments() {
    return Stream.of(
      arguments(NullPointerException.class, null, entityManagerFactory.createEntityManager()),
      arguments(NullPointerException.class, MARGINAL_STATION_SOH, null),
      arguments(IllegalStateException.class,
        StationSoh.create(Instant.EPOCH,
          "Unknown Station",
          Set.of(MARGINAL_MISSING_SOH_MONITOR_VALUE_AND_STATUS, MARGINAL_LAG_SOH_MONITOR_VALUE_AND_STATUS),
            SohStatus.MARGINAL,
          Set.of(BAD_MISSING_LAG_SEAL_CHANNEL_SOH),
          Set.of(MISSING_STATION_AGGREGATE, LAG_STATION_AGGREGATE)),
        entityManagerFactory.createEntityManager())
    );
  }

  @Test
  void testFromCoi() {
    EntityManager entityManager = entityManagerFactory.createEntityManager();
    try {
      entityManager.getTransaction().begin();
      StationSoh expected = MARGINAL_STATION_SOH;
      StationSohDao actual = new StationSohDaoConverter().fromCoi(expected, entityManager);

      assertEquals(expected.getId(), actual.getCoiId());
      assertEquals(expected.getTime(), actual.getCreationTime());
      assertEquals(expected.getStationName(), actual.getStation().getName());
      assertEquals(expected.getSohStatusRollup(), actual.getSohStatus());

      Set<SohMonitorValueAndStatus> actualSmvs =
        actual.getSohMonitorValueAndStatuses()
          .stream()
          .map(smvs -> new SohMonitorValueAndStatusDaoConverter()
            .toCoi(smvs))
          .collect(Collectors.toSet());
      assertEquals(expected.getSohMonitorValueAndStatuses(), actualSmvs);

      Set<ChannelSoh> channelSohs = actual.getChannelSohs()
        .stream()
        .map(channelSoh -> new ChannelSohDaoConverter().toCoi(channelSoh))
        .collect(Collectors.toSet());

      assertEquals(expected.getChannelSohs(), channelSohs);
    } finally {
      entityManager.getTransaction().rollback();
      entityManager.close();
    }
  }

  @Test
  void testToCoiValidation() {
    assertThrows(NullPointerException.class, () -> new StationSohDaoConverter().toCoi(null));
  }

  @Test
  void testToCoi() {
    EntityManager entityManager = entityManagerFactory.createEntityManager();
    try {
      entityManager.getTransaction().begin();
      StationSohDao expected = new StationSohDao();
      expected.setCoiId(MARGINAL_STATION_SOH.getId());
      expected.setCreationTime(MARGINAL_STATION_SOH.getTime());
      expected.setStation(entityManager.find(StationDao.class,
        UtilsTestFixtures.STATION.getName()));
      expected.setSohStatus(MARGINAL_STATION_SOH.getSohStatusRollup());

      Set<SohMonitorValueAndStatusDao> smvsDaos =
        MARGINAL_STATION_SOH.getSohMonitorValueAndStatuses()
          .stream()
          .map(smvs -> new SohMonitorValueAndStatusDaoConverter()
            .fromCoi(smvs, entityManager))
          .collect(Collectors.toSet());
      expected.setSohMonitorValueAndStatuses(smvsDaos);

      Set<ChannelSohDao> channelSohDaos = MARGINAL_STATION_SOH.getChannelSohs()
        .stream()
        .map(channelSoh -> new ChannelSohDaoConverter().fromCoi(channelSoh, entityManager))
        .collect(Collectors.toSet());
      expected.setChannelSohs(channelSohDaos);

      Set<StationAggregateDao> stationAggregateDaos = MARGINAL_STATION_SOH.getAllStationAggregates()
          .stream()
          .map(stationAggregate -> new StationAggregateDaoConverter().fromCoi(stationAggregate, entityManager))
          .collect(Collectors.toSet());
      expected.setAllStationAggregate(stationAggregateDaos);

      StationSoh actual = new StationSohDaoConverter().toCoi(expected);
      assertEquals(MARGINAL_STATION_SOH, actual);
    } finally {
      entityManager.getTransaction().rollback();
      entityManager.close();
    }
  }
}