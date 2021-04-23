package gms.shared.frameworks.soh.repository.performancemonitoring;

import static gms.shared.frameworks.osd.coi.SohTestFixtures.BAD_STATION_SOH;
import static gms.shared.frameworks.osd.coi.SohTestFixtures.MARGINAL_STATION_SOH;
import static gms.shared.frameworks.osd.coi.SohTestFixtures.NOW;
import static gms.shared.frameworks.osd.coi.test.utils.UtilsTestFixtures.STATION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import gms.shared.frameworks.osd.api.util.HistoricalStationSohRequest;
import gms.shared.frameworks.osd.api.util.StationsTimeRangeRequest;
import gms.shared.frameworks.osd.api.util.TimeRangeRequest;
import gms.shared.frameworks.osd.coi.soh.ChannelSoh;
import gms.shared.frameworks.osd.coi.soh.DurationSohMonitorValueAndStatus;
import gms.shared.frameworks.osd.coi.soh.PercentSohMonitorValueAndStatus;
import gms.shared.frameworks.osd.coi.soh.SohMonitorType;
import gms.shared.frameworks.osd.coi.soh.SohMonitorValueAndStatus;
import gms.shared.frameworks.osd.coi.soh.SohStatus;
import gms.shared.frameworks.osd.coi.soh.StationSoh;
import gms.shared.frameworks.osd.coi.station.StationTestFixtures;
import gms.shared.frameworks.osd.coi.test.utils.UtilsTestFixtures;
import gms.shared.frameworks.osd.dao.soh.StationSohDao;
import gms.shared.frameworks.osd.dao.util.CoiEntityManagerFactory;
import gms.shared.frameworks.osd.dto.soh.DurationSohMonitorValues;
import gms.shared.frameworks.osd.dto.soh.HistoricalSohMonitorValues;
import gms.shared.frameworks.osd.dto.soh.HistoricalStationSoh;
import gms.shared.frameworks.osd.dto.soh.PercentSohMonitorValues;
import gms.shared.frameworks.osd.dto.soh.SohMonitorValues;
import gms.shared.frameworks.soh.repository.performancemonitoring.converter.StationSohDaoConverter;
import gms.shared.frameworks.soh.repository.station.StationGroupRepositoryJpa;
import gms.shared.frameworks.soh.repository.util.DbTest;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import org.hibernate.Session;
import org.hibernate.annotations.SQLDeleteAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class PerformanceMonitoringRepositoryJpaTest extends DbTest {
  private static final Logger logger = LoggerFactory.getLogger(PerformanceMonitoringRepositoryJpaTest.class);
  private static int NUM_STATIONS_FOR_HISTORICAL_QUERY = 10;

  private PerformanceMonitoringRepositoryJpa performanceMonitoringRepository;

  @AfterEach
  void testCaseTeardown() {
    EntityManager entityManager = entityManagerFactory.createEntityManager();
    try {
      entityManager.getTransaction().begin();
      Query query = entityManager.createNativeQuery("delete from gms_soh.station_soh");
      query.executeUpdate();
      entityManager.getTransaction().commit();
    }finally{
      entityManager.close();
    }
  }

  @Test
  void testStore() {
    List<UUID> uuids = new PerformanceMonitoringRepositoryJpa(entityManagerFactory)
      .storeStationSoh(List.of(MARGINAL_STATION_SOH));

    assertFalse(uuids.isEmpty());
    assertEquals(1, uuids.size());
    assertTrue(uuids.contains(MARGINAL_STATION_SOH.getId()));

    EntityManager entityManager = entityManagerFactory.createEntityManager();
    try {
      StationSohDao dao = entityManager.unwrap(Session.class)
          .bySimpleNaturalId(StationSohDao.class)
          .load(MARGINAL_STATION_SOH.getId());
      assertNotNull(dao);
      assertEquals(MARGINAL_STATION_SOH, new StationSohDaoConverter().toCoi(dao));
    } finally {
      entityManager.close();
    }
  }

  @ParameterizedTest
  @MethodSource("getRetrieveByStationGroupIdArgs")
  void testRetrieveByStationGroupIdValidation(Class<? extends Exception> expectedException,
    List<String> stationGroupNames) {
    assertThrows(expectedException,
      () -> new PerformanceMonitoringRepositoryJpa(entityManagerFactory).retrieveByStationId(stationGroupNames));
  }

  static Stream<Arguments> getRetrieveByStationGroupIdArgs() {
    return Stream.of(
      arguments(NullPointerException.class, null),
      arguments(IllegalStateException.class, List.of()));
  }

  @Test
  void testRetrieveByStationGroupId() {
    performanceMonitoringRepository = new PerformanceMonitoringRepositoryJpa(entityManagerFactory);
    performanceMonitoringRepository.storeStationSoh(List.of(MARGINAL_STATION_SOH, BAD_STATION_SOH));

    List<StationSoh> stationSohs = performanceMonitoringRepository
      .retrieveByStationId(List.of(STATION.getName()));

    assertEquals(1, stationSohs.size());
    assertTrue(stationSohs.contains(MARGINAL_STATION_SOH));
  }

  @Test
  void testRetrieveByTimeRangeValidation() {
    assertThrows(NullPointerException.class,
      () -> new PerformanceMonitoringRepositoryJpa(entityManagerFactory).retrieveByStationsAndTimeRange(null));
  }

  @Test
  void testRetrieveByTimeRange() {
    performanceMonitoringRepository = new PerformanceMonitoringRepositoryJpa(entityManagerFactory);
    performanceMonitoringRepository.storeStationSoh(List.of(MARGINAL_STATION_SOH, BAD_STATION_SOH));

    StationsTimeRangeRequest request = StationsTimeRangeRequest.create(List.of(STATION.getName()),
      NOW.minusSeconds(60),
      NOW);
    TimeRangeRequest.create(Instant.EPOCH, Instant.EPOCH.plusSeconds(20));
    List<StationSoh> timeRangeStatus = performanceMonitoringRepository
      .retrieveByStationsAndTimeRange(request);
    assertEquals(1, timeRangeStatus.size());
    assertTrue(timeRangeStatus.contains(MARGINAL_STATION_SOH));
  }

  @Test
  void testHistoricalStationSohQuery(){

    performanceMonitoringRepository = new PerformanceMonitoringRepositoryJpa(entityManagerFactory);
    performanceMonitoringRepository.storeStationSoh(populateHistoricalStationSohData());

    Instant start = Instant.now();
    SohMonitorType monitorTypes[] = new SohMonitorType[] { SohMonitorType.MISSING, SohMonitorType.LAG};

    HistoricalStationSohRequest request = HistoricalStationSohRequest.create(
        STATION.getName(), NOW.toEpochMilli(), NOW.toEpochMilli() + 20000 * NUM_STATIONS_FOR_HISTORICAL_QUERY,
        Arrays.asList(monitorTypes));

    logger.info("Station Time Range Soh Type Request: {}", request.toString());

    HistoricalStationSoh historicalStationSoh = performanceMonitoringRepository.retrieveHistoricalStationSoh(request);

    Instant finish = Instant.now();
    long timeElapsed = Duration.between(start, finish).toMillis();

    logger.info("---- SQL Retrieve Historical Station Soh Data: ----");
    logger.info("Elapsed time (ms): {}", timeElapsed);
    assertEquals(historicalStationSoh.getCalculationTimes().length, NUM_STATIONS_FOR_HISTORICAL_QUERY);
    for(int i=0; i<historicalStationSoh.getCalculationTimes().length-1; i++){
      assertTrue(historicalStationSoh.getCalculationTimes()[i] < historicalStationSoh.getCalculationTimes()[i+1],
          "CalculationTimes are not sorted correctly");
    }

    for(HistoricalSohMonitorValues hmv : historicalStationSoh.getMonitorValues()){
      for(Map.Entry<SohMonitorType, SohMonitorValues> es : hmv.getValuesByType().entrySet()){
        if(es.getValue() instanceof DurationSohMonitorValues){
          DurationSohMonitorValues smv = (DurationSohMonitorValues)es.getValue();
          for(int i=0; i<smv.getValues().length-1; i++){
            assertTrue(smv.getValues()[i] < smv.getValues()[i+1],
                "DurationSohMonitorValues are not sorted correctly");
          }
        }
        if(es.getValue() instanceof PercentSohMonitorValues){
          PercentSohMonitorValues smv = (PercentSohMonitorValues)es.getValue();
          for(int i=0; i<smv.getValues().length-1; i++){
            assertTrue(smv.getValues()[i] < smv.getValues()[i+1],
                "PercentSohMonitorValues are not sorted correctly");
          }
        }
      }
    }
  }

  private Collection<StationSoh> populateHistoricalStationSohData() {
    List<StationSoh> stationSohList = new ArrayList<>();
    StationSoh stationSoh = MARGINAL_STATION_SOH;
    for(int i=0; i<NUM_STATIONS_FOR_HISTORICAL_QUERY; i++){
      Set<ChannelSoh> channelSohs = stationSoh.getChannelSohs();
      Set<ChannelSoh> newChannelSohs = new LinkedHashSet<>();

      for(ChannelSoh channelSoh : channelSohs){
        SohMonitorValueAndStatus latency = PercentSohMonitorValueAndStatus.from((
            double)i, SohStatus.GOOD, SohMonitorType.MISSING);
        SohMonitorValueAndStatus missing = DurationSohMonitorValueAndStatus.from(
            Duration.ofSeconds(i), SohStatus.GOOD, SohMonitorType.LAG);

        Set<SohMonitorValueAndStatus<?>> allSmvs = new HashSet<>();
        allSmvs.add(latency);
        allSmvs.add(missing);
        for(SohMonitorValueAndStatus smvs : channelSoh.getAllSohMonitorValueAndStatuses()){
          if(smvs.getMonitorType() != SohMonitorType.MISSING && smvs.getMonitorType() != SohMonitorType.LAG){
            allSmvs.add(smvs);
          }
        }

        newChannelSohs.add(ChannelSoh.from(
            channelSoh.getChannelName(),
            channelSoh.getSohStatusRollup(),
            allSmvs));
      }

      stationSohList.add(
          StationSoh.from(UUID.randomUUID(),
              stationSoh.getTime().plusSeconds(i*20),
              stationSoh.getStationName(),
              stationSoh.getSohMonitorValueAndStatuses(),
              stationSoh.getSohStatusRollup(),
              newChannelSohs,
              stationSoh.getAllStationAggregates()));
    }

    StationSoh invalidTimeRange = StationSoh.create(
        MARGINAL_STATION_SOH.getTime().plusSeconds(60*60*24),
        MARGINAL_STATION_SOH.getStationName(),
        MARGINAL_STATION_SOH.getSohMonitorValueAndStatuses(),
        MARGINAL_STATION_SOH.getSohStatusRollup(),
        MARGINAL_STATION_SOH.getChannelSohs(),
        MARGINAL_STATION_SOH.getAllStationAggregates()
    );
    StationSoh invalidStation = StationSoh.create(
        Instant.ofEpochMilli(NOW.toEpochMilli()),
        "ASAR",
        MARGINAL_STATION_SOH.getSohMonitorValueAndStatuses(),
        MARGINAL_STATION_SOH.getSohStatusRollup(),
        MARGINAL_STATION_SOH.getChannelSohs(),
        MARGINAL_STATION_SOH.getAllStationAggregates()
    );
    stationSohList.add(invalidTimeRange);
    stationSohList.add(invalidStation);
    return stationSohList;
  }
}