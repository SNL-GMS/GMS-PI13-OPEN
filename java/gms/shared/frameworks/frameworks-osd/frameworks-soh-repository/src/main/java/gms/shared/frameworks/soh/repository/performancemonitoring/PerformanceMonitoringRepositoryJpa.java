package gms.shared.frameworks.soh.repository.performancemonitoring;

import com.google.common.base.Preconditions;
import gms.shared.frameworks.osd.api.performancemonitoring.PerformanceMonitoringRepositoryInterface;
import gms.shared.frameworks.osd.api.util.HistoricalStationSohRequest;
import gms.shared.frameworks.osd.api.util.RepositoryExceptionUtils;
import gms.shared.frameworks.osd.api.util.StationsTimeRangeRequest;
import gms.shared.frameworks.osd.coi.soh.SohMonitorType;
import gms.shared.frameworks.osd.coi.soh.StationSoh;
import gms.shared.frameworks.osd.dao.channel.StationDao;
import gms.shared.frameworks.osd.dao.soh.StationSohDao;
import gms.shared.frameworks.osd.dto.soh.HistoricalStationSoh;
import gms.shared.frameworks.soh.repository.performancemonitoring.converter.StationSohDaoConverter;
import gms.shared.frameworks.soh.repository.performancemonitoring.transform.StationSohObjectArrayTransformer;
import gms.shared.metrics.CustomMetric;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkState;

public class PerformanceMonitoringRepositoryJpa implements
    PerformanceMonitoringRepositoryInterface {

  private static final Logger logger = LoggerFactory
      .getLogger(PerformanceMonitoringRepositoryJpa.class);
  public static final String STATION_ATTRIBUTE = "station";
  public static final String CHANNEL_SOH_ATTRIBUTE = "station";
  public static final String STATION_NAME_ATTRIBUTE = "stationName";
  public static final String CREATION_TIME_ATTRIBUTE = "creationTime";
  public static final String NAME_ATTRIBUTE = "name";

  private final EntityManagerFactory entityManagerFactory;

  private static final CustomMetric<PerformanceMonitoringRepositoryJpa, Long> performanceMonitoringRetrieveStationId =
      CustomMetric.create(CustomMetric::incrementer,
          "performance_monitoring_retrieve_station_id_hits:type=Counter", 0L);

  private static final CustomMetric<PerformanceMonitoringRepositoryJpa, Long> performanceMonitoringRetrieveStationTime =
      CustomMetric.create(CustomMetric::incrementer,
          "performanceMonitoringRetrieveStationTime:type=Counter", 0L);

  private static final CustomMetric<PerformanceMonitoringRepositoryJpa, Long> performanceMonitoringStoreStationSOH =
          CustomMetric.create(CustomMetric::incrementer, "performance_monitoring_store_station_soh_hits:type=Counter", 0L);

  private static final CustomMetric<Long, Long> performanceMonitoringRetrieveStationIdDuration =
          CustomMetric.create(CustomMetric::updateTimingData, "performance_monitoring_retrieve_station_id_duration:type=Value", 0L);

  private static final CustomMetric<Long, Long> performanceMonitoringRetrieveStationTimeDuration =
          CustomMetric.create(CustomMetric::updateTimingData, "performance_monitoring_retrieve_station_time_duration:type=Value", 0L);

  private static final CustomMetric<Long, Long> performanceMonitoringStoreStationSOHDuration =
          CustomMetric.create(CustomMetric::updateTimingData, "performance_monitoring_store_station_soh_duration:type=Value", 0L);

  /**
   * Constructor taking in the EntityManagerFactory
   *
   * @param entityManagerFactory {@link EntityManagerFactory}
   */
  public PerformanceMonitoringRepositoryJpa(EntityManagerFactory entityManagerFactory) {
    Objects.requireNonNull(entityManagerFactory,
        "Cannot instantiate PerformanceMonitoringRepositoryJpa with null EntityManager");
    this.entityManagerFactory = entityManagerFactory;
  }

  /**
   * Retrieve the latest {@link StationSoh} by station group ids. There are multiple
   * {@link StationSoh} objects per station group id; this method returns the "latest"
   * {@link StationSoh} object for a given station group id where "latest" equals the
   * {@link StationSoh} object for that station group id with the max end time.
   *
   * @return a List of {@link StationSoh}, or an empty list if none found.
   */
  @Override
  public List<StationSoh> retrieveByStationId(List<String> stationNames) {
    Objects.requireNonNull(stationNames);
    Preconditions.checkState(!stationNames.isEmpty());
    EntityManager em = entityManagerFactory.createEntityManager();

    performanceMonitoringRetrieveStationId.updateMetric(this);
    Instant start = Instant.now();


    try {
      CriteriaBuilder builder = em.getCriteriaBuilder();
      CriteriaQuery<StationSohDao> stationSohQuery = builder.createQuery(StationSohDao.class);
      Root<StationSohDao> fromStationSoh = stationSohQuery.from(StationSohDao.class);
      stationSohQuery.select(fromStationSoh);

      Join<StationSohDao, StationDao> stationJoin = fromStationSoh.join(STATION_ATTRIBUTE);
      Expression<String> stationName = stationJoin.get(NAME_ATTRIBUTE);

      Expression<Instant> endTime = fromStationSoh.get(CREATION_TIME_ATTRIBUTE);

      stationSohQuery.where(builder.or(stationNames.stream()
          .map(name -> {
            Subquery<Instant> maxCreationTimeQuery = stationSohQuery.subquery(Instant.class);
            Root<StationSohDao> subFromStatus =
                maxCreationTimeQuery.from(StationSohDao.class);
            Expression<Instant> subEndTime = subFromStatus.get(CREATION_TIME_ATTRIBUTE);
            Join<StationSohDao, StationDao> subStationJoin =
                subFromStatus.join(
                        STATION_ATTRIBUTE);
            Expression<String> subStationName = subStationJoin.get(NAME_ATTRIBUTE);
            maxCreationTimeQuery.select(builder.greatest(subEndTime));
            return builder.and(
                builder.equal(stationName, name),
                builder.equal(endTime, maxCreationTimeQuery.where(builder.equal(subStationName,
                    name))));
          }).toArray(Predicate[]::new)));

      StationSohDaoConverter converter = new StationSohDaoConverter();
      return em.createQuery(stationSohQuery)
          .getResultStream()
          .map(converter::toCoi)
          .collect(Collectors.toList());
    } catch (Exception ex) {
      logger.error("Error retrieving station group SOH status: {}",
          RepositoryExceptionUtils.wrap(ex).getMessage());
      throw ex;
    } finally {
      em.close();

      Instant finish = Instant.now();
      long timeElapsed = Duration.between(start, finish).toMillis();
      performanceMonitoringRetrieveStationIdDuration.updateMetric(timeElapsed);
    }
  }

  /**
   * Retrieve the {@link StationSoh}(s) within a time range (inclusive) that are currently stored in
   * the database. If a start and end time is not provided, retrieve the most recently stored {@link
   * StationSoh}.
   *
   * @param request Request containing station names and a time range
   * @return a List of {@link StationSoh}(s)
   */
  @Override
  public List<StationSoh> retrieveByStationsAndTimeRange(StationsTimeRangeRequest request) {
    Objects.requireNonNull(request);

    performanceMonitoringRetrieveStationTime.updateMetric(this);
    Instant start = Instant.now();

    EntityManager entityManager = entityManagerFactory.createEntityManager();
    try {
      CriteriaBuilder builder = entityManager.getCriteriaBuilder();
      CriteriaQuery<StationSohDao> stationSohQuery =
          builder.createQuery(StationSohDao.class);
      Root<StationSohDao> fromStationSoh = stationSohQuery.from(StationSohDao.class);

      Join<StationSohDao, StationDao> stationJoin = fromStationSoh.join(STATION_ATTRIBUTE);
      Expression<String> stationName = stationJoin.get(NAME_ATTRIBUTE);

      stationSohQuery.select(fromStationSoh)
          .where(builder.and(builder.greaterThanOrEqualTo(fromStationSoh.get(
                  CREATION_TIME_ATTRIBUTE), request.getTimeRange().getStartTime()),
              builder.lessThanOrEqualTo(fromStationSoh.get(CREATION_TIME_ATTRIBUTE),
                  request.getTimeRange().getEndTime()),
              stationName.in(request.getStationNames())));

      return entityManager.createQuery(stationSohQuery)
          .getResultStream()
          .map(new StationSohDaoConverter()::toCoi)
          .collect(Collectors.toList());
    } finally {
      entityManager.close();

      Instant finish = Instant.now();
      long timeElapsed = Duration.between(start, finish).toMillis();
      performanceMonitoringRetrieveStationTimeDuration.updateMetric(timeElapsed);
    }
  }

  /**
   * Store the provided {@link StationSoh}(s).
   *
   * @return A list of UUIDs that correspond to the {@link StationSoh}(s) that were
   * successfully stored.
   * @param stationSohs The {@link StationSoh}(s) to store
   */
  @Override
  public List<UUID> storeStationSoh(Collection<StationSoh> stationSohs) {
    Objects.requireNonNull(stationSohs);
    List<UUID> uuids = new ArrayList<>();
    EntityManager entityManager = entityManagerFactory.createEntityManager();

    //TODO - may look into dynamically setting this to a lower number, but batch size is shared across persistence context
    Object batchSizeObj = entityManagerFactory.getProperties().get("hibernate.jdbc.batch_size");
    if(batchSizeObj == null){
      batchSizeObj = "50";
    }
    Integer batchSize = Integer.parseInt(batchSizeObj.toString());

    performanceMonitoringStoreStationSOH.updateMetric(this);
    Instant start = Instant.now();
    try {
      entityManager.getTransaction().begin();
      StationSohDaoConverter converter = new StationSohDaoConverter();
      int count = 0;
      for (StationSoh stationSoh : stationSohs) {
        StationSohDao dao = converter.fromCoi(stationSoh, entityManager);
        entityManager.persist(dao);
        uuids.add(stationSoh.getId());
        if (count > 0 && count % batchSize == 0) {
          entityManager.getTransaction().commit();
          entityManager.getTransaction().begin();

          entityManager.clear();
        }
        count++;
      }
      entityManager.getTransaction().commit();
    } catch (Exception ex) {
      logger.error("Exception trying to store StationSoh", ex);
      entityManager.getTransaction().rollback();
      throw ex;
    } finally {
      entityManager.close();

      Instant finish = Instant.now();
      long timeElapsed = Duration.between(start, finish).toMillis();
      performanceMonitoringStoreStationSOHDuration.updateMetric(timeElapsed);
    }
    return uuids;
  }

  /**
   * Retrieves a HistoricalStationSoh DTO object corresponding to the provided Station ID and
   * collection of SohMonitorTypes provided in the request body.
   * <p>
   * The returned HistoricalStationSoh object contains SOH monitor values from StationSoh objects
   * with calculation time attributes in the time range provided (both start and end times are
   * inclusive), and aggregates the HistoricalSohMonitorValue objects by value and all associations
   * to Station and Channel are by identifier.
   *
   * @return A {@link HistoricalStationSoh} object that conforms to the provided parameters
   */
  @Override
  public HistoricalStationSoh retrieveHistoricalStationSoh(HistoricalStationSohRequest request) {
    Preconditions.checkNotNull(request, "Request cannot be null");

    List<SohMonitorType> unsupported = request.getSohMonitorTypes().stream()
            .filter(sohMonitorType ->
                    !SohMonitorType.validTypes().contains(sohMonitorType)).collect(Collectors.toList());

    if (!unsupported.isEmpty()) {
      logger.warn(
              "Unsupported monitor types provided. No SOH will be provided for these types: {}. Supported types are {}.",
              unsupported, SohMonitorType.validTypes());
    }

    request = request.toBuilder()
            .setSohMonitorTypes(request.getSohMonitorTypes().stream()
                    .filter(SohMonitorType.validTypes()::contains).collect(
                            Collectors.toList()))
            .build();

    return queryHistoricalStationSoh(request);
  }

  /**
   * Performs query to DB.  We are using nativeQuery to select only specific columns we need.
   * I wasn't able to do it with CriteriaQuery or JPQL due to inheritance table on SMVS
   *
   * @param request contains request with values to pass into query
   * @return HistoricalStationSoh contains processed results of query
   */
  private HistoricalStationSoh queryHistoricalStationSoh(HistoricalStationSohRequest request) {
    HistoricalStationSoh historicalStationSoh = null;
    EntityManager em = entityManagerFactory.createEntityManager();

    try {

      javax.persistence.Query query = em.createNativeQuery(
              "SELECT stationSoh.station_name, stationSoh.creation_time, channelSoh.channel_name, " +
                      "smvs.monitor_type, smvs.duration, smvs.percent, smvs.status " +
                      "FROM gms_soh.station_soh stationSoh " +
                      "INNER JOIN gms_soh.channel_soh channelSoh " +
                      "ON stationSoh.id = channelSoh.station_soh_id " +
                      "INNER JOIN gms_soh.soh_monitor_value_status smvs " +
                      "ON channelSoh.id = smvs.channel_soh_id " +
                      "WHERE stationSoh.station_name like :station_name " +
                      "AND stationSoh.creation_time >= :start_time " +
                      "AND stationSoh.creation_time <= :end_time " +
                      "AND smvs.monitor_type in (:monitorTypeIds) " +
                      "ORDER BY creation_time ASC");

      //Postgres enums are not supported, so need to convert to id
      List<Short> monitorTypeDbIds =
              request.getSohMonitorTypes().stream()
                      .map(SohMonitorType::getDbId)
                      .collect(Collectors.toList());

      query.setParameter("station_name", request.getStationName());
      query.setParameter("start_time", Instant.ofEpochMilli(request.getStartTime()));
      query.setParameter("end_time", Instant.ofEpochMilli(request.getEndTime()));
      query.setParameter("monitorTypeIds", monitorTypeDbIds);

      historicalStationSoh = StationSohObjectArrayTransformer.createHistoricalStationSoh(request.getStationName(), query.getResultList());

    }catch (Exception ex) {
      logger.error("Error retrieving historical SOH: {}",
              RepositoryExceptionUtils.wrap(ex).getMessage());
      throw ex;
    } finally {
      em.close();
    }

    return historicalStationSoh;
  }
}
