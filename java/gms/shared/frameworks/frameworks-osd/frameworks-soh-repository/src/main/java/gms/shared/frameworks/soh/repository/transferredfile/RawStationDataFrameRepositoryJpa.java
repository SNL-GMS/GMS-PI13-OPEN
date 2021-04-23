package gms.shared.frameworks.soh.repository.transferredfile;

import gms.shared.frameworks.osd.api.util.RepositoryExceptionUtils;
import gms.shared.frameworks.osd.api.util.StationTimeRangeRequest;
import gms.shared.frameworks.osd.api.util.TimeRangeRequest;
import gms.shared.frameworks.osd.api.waveforms.RawStationDataFrameRepositoryInterface;
import gms.shared.frameworks.osd.coi.waveforms.RawStationDataFrame;
import gms.shared.frameworks.osd.dao.transferredfile.RawStationDataFrameDao;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class RawStationDataFrameRepositoryJpa implements RawStationDataFrameRepositoryInterface {

  private static final Logger logger = LoggerFactory
      .getLogger(RawStationDataFrameRepositoryJpa.class);
  
  public static final String PAYLOAD_DATA_END_TIME = "payloadDataEndTime";
  public static final String PAYLOAD_DATA_START_TIME = "payloadDataStartTime";
  public static final String STATION_NAME = "stationName";

  private final EntityManagerFactory entityManagerFactory;

  /**
   * Default constructor.
   */
  public RawStationDataFrameRepositoryJpa(EntityManagerFactory entityManagerFactory) {
    this.entityManagerFactory = entityManagerFactory;
  }

  /**
   * Stores {@link RawStationDataFrame}s
   *
   * @param frames Collection of {@link RawStationDataFrame}s to store
   */
  @Override
  public void storeRawStationDataFrames(Collection<RawStationDataFrame> frames) {
    Validate.notNull(frames, "Cannot store null RawStationDataFrames");

    EntityManager entityManager = entityManagerFactory.createEntityManager();
    entityManager.getTransaction().begin();
    try {
      // TODO: Check if these exist before storing
      Query query = entityManager.createNamedQuery("RawStationDataFrameDao.exists");
      for (RawStationDataFrame rsdf : frames) {
        query.setParameter("id", rsdf.getId());
        var daoExists = query.getFirstResult();
        if (daoExists == 0) {
          RawStationDataFrameDao rsdfDao = new RawStationDataFrameDao(rsdf);
          entityManager.persist(rsdfDao);
        }
      }
      entityManager.getTransaction().commit();

    } catch (Exception e) {
      logger.error("Error committing transaction: {}",
          RepositoryExceptionUtils.wrap(e).getMessage());
      entityManager.getTransaction().rollback();
      throw e;
    } finally {
      entityManager.close();
    }
  }

  /**
   * Retrieve RawStationDataFrames that are within the specified time range and station name
   *
   * @param stationTimeRangeRequest request containing time range and station name to query by
   * @return List of RawStationDataFrames for specified time range and station name
   */
  @Override
  public List<RawStationDataFrame> retrieveRawStationDataFramesByStationAndTime(
      StationTimeRangeRequest stationTimeRangeRequest) {

    EntityManager entityManager = entityManagerFactory.createEntityManager();
    CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
    CriteriaQuery<RawStationDataFrameDao> criteriaQuery =
        criteriaBuilder.createQuery(RawStationDataFrameDao.class);
    Root<RawStationDataFrameDao> rsdfRoot = criteriaQuery.from(RawStationDataFrameDao.class);

    Predicate predicate = criteriaBuilder.and(
        criteriaBuilder.lessThanOrEqualTo(rsdfRoot.get(PAYLOAD_DATA_START_TIME),
            stationTimeRangeRequest.getTimeRange().getEndTime()),
        criteriaBuilder.greaterThanOrEqualTo(rsdfRoot.get(PAYLOAD_DATA_END_TIME),
            stationTimeRangeRequest.getTimeRange().getStartTime()),
        criteriaBuilder.equal(rsdfRoot.get(STATION_NAME),
            stationTimeRangeRequest.getStationName()));
    criteriaQuery.where(predicate).orderBy(
        criteriaBuilder.asc(rsdfRoot.get(STATION_NAME)),
        criteriaBuilder.asc(rsdfRoot.get(PAYLOAD_DATA_START_TIME)));

    try {
      return entityManager.createQuery(criteriaQuery).getResultStream()
          .map(RawStationDataFrameDao::toCoi).collect(Collectors.toList());
    } catch (Exception e) {
      logger.error("Error retrieving frames: {}", RepositoryExceptionUtils.wrap(e).getMessage());
      throw e;
    } finally {
      entityManager.close();
    }
  }

  /**
   * Retrieve RawStationDataFrames that are within the specified time range
   *
   * @param timeRangeRequest request containing time range and station name to query by
   * @return List of RawStationDataFrames for specified time range
   */
  @Override
  public List<RawStationDataFrame> retrieveRawStationDataFramesByTime(
      TimeRangeRequest timeRangeRequest) {
    EntityManager entityManager = entityManagerFactory.createEntityManager();
    CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
    CriteriaQuery<RawStationDataFrameDao> criteriaQuery =
        criteriaBuilder.createQuery(RawStationDataFrameDao.class);
    Root<RawStationDataFrameDao> rsdfRoot = criteriaQuery.from(RawStationDataFrameDao.class);

    Predicate predicate = criteriaBuilder.and(
        criteriaBuilder.lessThanOrEqualTo(
            rsdfRoot.get(PAYLOAD_DATA_START_TIME), timeRangeRequest.getEndTime()),
        criteriaBuilder.greaterThanOrEqualTo(
            rsdfRoot.get(PAYLOAD_DATA_END_TIME), timeRangeRequest.getStartTime()));

    criteriaQuery.where(predicate).orderBy(
        criteriaBuilder.asc(rsdfRoot.get(STATION_NAME)),
        criteriaBuilder.asc(rsdfRoot.get(PAYLOAD_DATA_START_TIME)));

    try {
      return entityManager.createQuery(criteriaQuery).getResultStream()
          .map(RawStationDataFrameDao::toCoi).collect(Collectors.toList());
    } catch (Exception e) {
      logger.error("Error retrieving frames: {}", RepositoryExceptionUtils.wrap(e).getMessage());
      throw e;
    } finally {
      entityManager.close();
    }
  }
  
}
