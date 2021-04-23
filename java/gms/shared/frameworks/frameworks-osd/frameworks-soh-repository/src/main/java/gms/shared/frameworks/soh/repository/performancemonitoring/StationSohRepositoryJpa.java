package gms.shared.frameworks.soh.repository.performancemonitoring;

import gms.shared.frameworks.osd.api.util.ChannelTimeRangeRequest;
import gms.shared.frameworks.osd.api.util.ChannelTimeRangeSohTypeRequest;
import gms.shared.frameworks.osd.api.util.RepositoryExceptionUtils;
import gms.shared.frameworks.osd.api.util.TimeRangeRequest;
import gms.shared.frameworks.osd.api.waveforms.StationSohRepositoryInterface;
import gms.shared.frameworks.osd.coi.ParameterValidation;
import gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssue;
import gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssue.AcquiredChannelEnvironmentIssueType;
import gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssueAnalog;
import gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssueBoolean;
import gms.shared.frameworks.osd.dao.channel.ChannelDao;
import gms.shared.frameworks.osd.dao.channelsoh.AcquiredChannelEnvironmentIssueAnalogDao;
import gms.shared.frameworks.osd.dao.channelsoh.AcquiredChannelEnvironmentIssueBooleanDao;
import gms.shared.frameworks.osd.dao.channelsoh.AcquiredChannelEnvironmentIssueDao;
import gms.shared.frameworks.soh.repository.performancemonitoring.converter.AcquiredChannelEnvironmentIssueAnalogDaoConverter;
import gms.shared.frameworks.soh.repository.performancemonitoring.converter.AcquiredChannelEnvironmentIssueBooleanDaoConverter;
import gms.shared.frameworks.soh.repository.performancemonitoring.converter.AcquiredChannelEnvironmentIssueDaoConverter;
import gms.shared.frameworks.utilities.jpa.EntityConverter;
import gms.shared.metrics.CustomMetric;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import javax.persistence.criteria.Subquery;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implement interface for storing and retrieving objects related to State of Health (SOH) from the relational
 * database.
 */
public class StationSohRepositoryJpa implements StationSohRepositoryInterface {

  private static final Logger logger = LoggerFactory.getLogger(StationSohRepositoryJpa.class);
  private static final String CHANNEL = "channel";
  private static final String NAME = "name";
  private static final String START_TIME = "startTime";
  private static final String END_TIME = "endTime";
  private static final String NATURAL_ID = "naturalId";
  private static final String TYPE = "type";

  private EntityManagerFactory entityManagerFactory;

  private static final CustomMetric<StationSohRepositoryJpa, Long> sohStoreACEIAnalog =
      CustomMetric.create(CustomMetric::incrementer, "soh_store_acei_analog_hits:type=Counter", 0L);

  private static final CustomMetric<StationSohRepositoryJpa, Long> sohStoreACEIBoolean =
      CustomMetric.create(CustomMetric::incrementer, "soh_store_acei_boolean_hits:type=Counter", 0L);

  private static final CustomMetric<StationSohRepositoryJpa, Long> sohRemoveACEIBoolean =
      CustomMetric.create(CustomMetric::incrementer, "soh_remove_acei_boolean_hits:type=Counter", 0L);

  private static final CustomMetric<StationSohRepositoryJpa, Long> sohRetrieveAnalog =
      CustomMetric.create(CustomMetric::incrementer, "soh_retrieve_analog_hits:type=Counter", 0L);

  private static final CustomMetric<StationSohRepositoryJpa, Long> sohRetrieveBoolean =
      CustomMetric.create(CustomMetric::incrementer, "soh_retrieve_boolean_hits:type=Counter", 0L);

  private static final CustomMetric<StationSohRepositoryJpa, Long> sohRetrieveACEIAnalogId =
      CustomMetric.create(CustomMetric::incrementer, "soh_retrieve_acei_analog_id_hits:type=Counter", 0L);

  private static final CustomMetric<StationSohRepositoryJpa, Long> sohRetrieveACEIBooleanId =
      CustomMetric.create(CustomMetric::incrementer, "soh_retrieve_acei_boolean_id_hits:type=Counter", 0L);

  private static final CustomMetric<StationSohRepositoryJpa, Long> sohRetrieveACEIAnalogTimeType =
      CustomMetric.create(CustomMetric::incrementer, "soh_retrieve_acei_analog_time_type_hits:type=Counter", 0L);

  private static final CustomMetric<StationSohRepositoryJpa, Long> sohRetrieveACEIBooleanTimeType =
      CustomMetric.create(CustomMetric::incrementer, "soh_retrieve_acei_boolean_time_type_hits:type=Counter", 0L);

  private static final CustomMetric<StationSohRepositoryJpa, Long> sohRetrieveACEIBooleanTime =
      CustomMetric.create(CustomMetric::incrementer, "soh_retrieve_acei_boolean_time_hits:type=Counter", 0L);

  private static final CustomMetric<StationSohRepositoryJpa, Long> sohRetrieveACEIAnalogTime =
      CustomMetric.create(CustomMetric::incrementer, "soh_retrieve_acei_analog_time_hits:type=Counter", 0L);
  private static final CustomMetric<StationSohRepositoryJpa, Long> sohRetrieveLatestACEIBoolean =
      CustomMetric.create(CustomMetric::incrementer, "soh_retrieve_latest_acei_boolean_hits:type=Counter", 0L);
  private static final CustomMetric<StationSohRepositoryJpa, Long> sohRetrieveLatestACEIAnalog =
      CustomMetric.create(CustomMetric::incrementer, "soh_retrieve_latest_acei_analog_hits:type=Counter", 0L);
  /// metrics for timing analysis

  private static final CustomMetric<Long, Long> sohStoreACEIAnalogDuration =
      CustomMetric.create(CustomMetric::updateTimingData, "soh_store_acei_analog_duration:type=Value", 0L);

  private static final CustomMetric<Long, Long> sohStoreACEIBooleanDuration =
      CustomMetric.create(CustomMetric::updateTimingData, "soh_store_acei_boolean_duration:type=Value", 0L);

  private static final CustomMetric<Long, Long> sohRemoveACEIBooleanDuration =
      CustomMetric.create(CustomMetric::updateTimingData, "soh_remove_acei_boolean_duration:type=Value", 0L);

  private static final CustomMetric<Long, Long> sohRetrieveAnalogDuration =
      CustomMetric.create(CustomMetric::updateTimingData, "soh_retrieve_analog_duration:type=Value", 0L);

  private static final CustomMetric<Long, Long> sohRetrieveBooleanDuration =
      CustomMetric.create(CustomMetric::updateTimingData, "soh_retrieve_boolean_duration:type=Value", 0L);

  private static final CustomMetric<Long, Long> sohRetrieveACEIAnalogIdDuration =
      CustomMetric.create(CustomMetric::updateTimingData, "soh_retrieve_acei_analog_id_duration:type=Value", 0L);

  private static final CustomMetric<Long, Long> sohRetrieveACEIBooleanIdDuration =
      CustomMetric.create(CustomMetric::updateTimingData, "soh_retrieve_acei_boolean_id_duration:type=Value", 0L);

  private static final CustomMetric<Long, Long> sohRetrieveACEIAnalogTimeTypeDuration =
      CustomMetric.create(CustomMetric::updateTimingData, "soh_retrieve_acei_analog_time_type_duration:type=Value", 0L);

  private static final CustomMetric<Long, Long> sohRetrieveACEIBooleanTimeTypeDuration =
      CustomMetric
          .create(CustomMetric::updateTimingData, "soh_retrieve_acei_boolean_time_type_duration:type=Value", 0L);

  private static final CustomMetric<Long, Long> sohRetrieveACEIBooleanTimeDuration =
      CustomMetric.create(CustomMetric::updateTimingData, "soh_retrieve_acei_boolean_time_duration:type=Value", 0L);

  private static final CustomMetric<Long, Long> sohRetrieveACEIAnalogTimeDuration =
      CustomMetric.create(CustomMetric::updateTimingData, "soh_retrieve_acei_analog_time_duration:type=Value", 0L);
  private static final CustomMetric<Long, Long> sohRetrieveLatestACEIBooleanDuration =
      CustomMetric.create(CustomMetric::updateTimingData, "soh_retrieve_latest_acei_boolean_duration:type=Value", 0L);
  private static final CustomMetric<Long, Long> sohRetrieveLatestACEIAnalogDuration =
      CustomMetric.create(CustomMetric::updateTimingData, "soh_retrieve_latest_acei_analog_duration:type=Value", 0L);

  /**
   * Default constructor.
   */
  public StationSohRepositoryJpa(EntityManagerFactory entityManagerFactory) {
    this.entityManagerFactory = entityManagerFactory;
  }

  /**
   * Stores a collection of {@link AcquiredChannelEnvironmentIssueAnalog} state of health objects containing analog
   * values.
   *
   * @param acquiredChannelSohAnalogs The analog SOH object.
   */
  @Override
  public void storeAcquiredChannelSohAnalog(
      Collection<AcquiredChannelEnvironmentIssueAnalog> acquiredChannelSohAnalogs) {
    sohStoreACEIAnalog.updateMetric(this);
    Instant start = Instant.now();
    storeACEI(acquiredChannelSohAnalogs, AcquiredChannelEnvironmentIssueAnalogDao.class);
    Instant finish = Instant.now();
    long timeElapsed = Duration.between(start, finish).toMillis();
    sohStoreACEIAnalogDuration.updateMetric(timeElapsed);
  }

  /**
   * Stores a collection of {@link AcquiredChannelEnvironmentIssueBoolean} state of health objects containing boolean
   * values.
   *
   * @param acquiredChannelSohBooleans The boolean SOH objects to store.
   */
  @Override
  public void storeAcquiredChannelEnvironmentIssueBoolean(
      Collection<AcquiredChannelEnvironmentIssueBoolean> acquiredChannelSohBooleans) {
    sohStoreACEIBoolean.updateMetric(this);
    Instant start = Instant.now();
    storeACEI(acquiredChannelSohBooleans, AcquiredChannelEnvironmentIssueBooleanDao.class);
    Instant finish = Instant.now();
    long timeElapsed = Duration.between(start, finish).toMillis();
    sohStoreACEIBooleanDuration.updateMetric(timeElapsed);
  }

  private void storeACEI(Collection<? extends AcquiredChannelEnvironmentIssue> aceis, Class clazz) {
    Validate.notNull(aceis,
        "Cannot store null ACEI objects");
    EntityManager entityManager = entityManagerFactory.createEntityManager();
    Object batchSizeObj = entityManagerFactory.getProperties().get("hibernate.jdbc.batch_size");
    if (batchSizeObj == null) {
      batchSizeObj = "50";
    }
    Integer batchSize = Integer.parseInt(batchSizeObj.toString());
    entityManager.getTransaction().begin();

    try {
      //remove aceis with invalid channel FK
      aceis = (Collection<AcquiredChannelEnvironmentIssue>) filterInvalidChannels(
          entityManager, aceis);

      //remove any duplicates acei 1) id 2) unique constraint) from input stream...maybe this should be an error to
      // the caller?
      HashSet<Object> seen = new HashSet<>();
      aceis.removeIf(acei -> !seen.add(acei.getId()));
      seen.clear();
      aceis.removeIf(acei -> !seen.add(
          Objects.hash(
              acei.getChannelName(),
              acei.getStartTime(),
              acei.getType().name()
          )));
      //this is necessary if we receive aceis that are already in the DB (ie: duplicates from kafka, merge processor
      // doesn't delete all it's inserting)
      //it guarantees the attempted insert will not have a unique constraint collision
      //may consider removing dupes from input string and not from DB...need to understand issue more
      removeAcquiredChannelEnvironmentIssueByNaturalId(aceis, clazz);
      AcquiredChannelEnvironmentIssueDaoConverter aceiConverter = null;
      if (clazz.isAssignableFrom(AcquiredChannelEnvironmentIssueBooleanDao.class)) {
        aceiConverter =
            new AcquiredChannelEnvironmentIssueBooleanDaoConverter();
      } else if (clazz.isAssignableFrom(AcquiredChannelEnvironmentIssueAnalogDao.class)) {
        aceiConverter =
            new AcquiredChannelEnvironmentIssueAnalogDaoConverter();
      }

      aceiConverter.setUpdateExisting(false);
      int curIndex = 0;
      List<AcquiredChannelEnvironmentIssueDao> daoList = new ArrayList<>();
      for (AcquiredChannelEnvironmentIssue soh : aceis) {
        var sohDao = ((EntityConverter) aceiConverter).fromCoi(soh, entityManager);
        entityManager.persist(sohDao);
        daoList.add((AcquiredChannelEnvironmentIssueDao) sohDao);
        if (curIndex > 0 && curIndex % batchSize == 0) {
          try {
            entityManager.getTransaction().commit();
          } catch (Exception ex) {
            processAceiSublist(entityManager, daoList);
          } finally {
            daoList.clear();
          }
          entityManager.getTransaction().begin();
          entityManager.clear();
        }
        curIndex++;
      }
      // The final loop of the batch was never committed, so commit here
      try {
        entityManager.getTransaction().commit();
      } catch (Exception ex) {
        processAceiSublist(entityManager, daoList);
      }
    } finally {
      entityManager.close();
    }
  }
  /**
   * If a batch fails, this method will re-insert each item in the batch 1 by 1
   *
   * @param entityManager
   * @param daoList list of items to insert
   */
  private void processAceiSublist(EntityManager entityManager,
      List<? extends AcquiredChannelEnvironmentIssueDao> daoList) {
    entityManager.getTransaction().rollback();
    for (AcquiredChannelEnvironmentIssueDao dao : daoList) {
      try {
        entityManager.getTransaction().begin();
        entityManager.merge(dao);
        entityManager.getTransaction().commit();
      } catch (Exception ex) {
        logger.error("Error performing transaction: {}",
            RepositoryExceptionUtils.wrap(ex).getMessage());
        entityManager.getTransaction().rollback();
      }
    }
  }
  /**
   * Removes a collection of {@link AcquiredChannelEnvironmentIssueBoolean} state of health objects based on naturalId
   * (channel_name, start_time, type)
   *
   * @param aceiBooleans the collection of aceiBooleans to remove.
   */
  private <T> void removeAcquiredChannelEnvironmentIssueByNaturalId(
      Collection<? extends AcquiredChannelEnvironmentIssue> aceis, Class<T> clazz) {

    EntityManager entityManager = entityManagerFactory.createEntityManager();
    entityManager.getTransaction().begin();
    try {
      //get UUIDs for aceis
      List<Integer> naturalIds = aceis.parallelStream()
          .map(acei -> Objects.hash(
              acei.getChannelName(),
              acei.getStartTime(),
              acei.getType().name()
          ))
          .collect(Collectors.toList());
      var builder = entityManager.getCriteriaBuilder();
      CriteriaDelete<T> delete =
          builder.createCriteriaDelete(clazz);
      Root<T> fromEntity =
          delete.from(clazz);
      delete.where(fromEntity.get(NATURAL_ID).in(naturalIds));
      entityManager.createQuery(delete).executeUpdate();
      entityManager.getTransaction().commit();
    } catch (Exception ex) {
      logger.error("Error performing transaction in removeAcquiredChannelEnvironmentIssueByNaturalId: {}",
          RepositoryExceptionUtils.wrap(ex).getMessage());
      entityManager.getTransaction().rollback();
      throw ex;
    } finally {
      entityManager.close();
    }
  }

  /**
   * Removes a collection of {@link AcquiredChannelEnvironmentIssueBoolean} state of health objects
   *
   * @param aceiBooleans the collection of boolean SOH objects to remove.
   */
  @Override
  public void removeAcquiredChannelEnvironmentIssueBooleans(
      Collection<AcquiredChannelEnvironmentIssueBoolean> aceiBooleans) {
    sohRemoveACEIBoolean.updateMetric(this);
    Instant start = Instant.now();
    EntityManager entityManager = entityManagerFactory.createEntityManager();
    entityManager.getTransaction().begin();
    try {
      //get UUIDs for aceis
      List<UUID> uuids = aceiBooleans.parallelStream()
          .map(AcquiredChannelEnvironmentIssueBoolean::getId)
          .collect(Collectors.toList());
      var builder = entityManager.getCriteriaBuilder();
      CriteriaDelete<AcquiredChannelEnvironmentIssueBooleanDao> delete =
          builder.createCriteriaDelete(AcquiredChannelEnvironmentIssueBooleanDao.class);
      Root<AcquiredChannelEnvironmentIssueBooleanDao> fromEntity =
          delete.from(AcquiredChannelEnvironmentIssueBooleanDao.class);
      delete.where(fromEntity.get("id").in(uuids));
      entityManager.createQuery(delete).executeUpdate();
      entityManager.getTransaction().commit();
    } catch (Exception ex) {
      entityManager.getTransaction().rollback();
      throw ex;
    } finally {
      entityManager.close();
    }

    Instant finish = Instant.now();
    long timeElapsed = Duration.between(start, finish).toMillis();
    sohRemoveACEIBooleanDuration.updateMetric(timeElapsed);
  }

  /**
   * If an ACEI references an invalid channel, it can't be stored to the DB and should be removed
   *
   * @param entityManager
   * @param aceis aceis to filter
   * @return filtered list with aceis that have invalid channel removed
   */
  private Collection<?> filterInvalidChannels(EntityManager entityManager,
      Collection<? extends AcquiredChannelEnvironmentIssue> aceis) {

    Query query = entityManager
        .createNamedQuery("Channel.getChannelNames")
        .setHint("org.hibernate.cacheable", true);
    List<String> channelNames = query.getResultList();

    return aceis.parallelStream()
        .filter(acei -> channelNames.contains(acei.getChannelName()))
        .collect(Collectors.toList());
  }

  /**
   * Retrieves all {@link AcquiredChannelEnvironmentIssueAnalog} objects for the provided channel created within the
   * provided time range.
   *
   * @param request The collection of channel names and time range that will bound the {@link
   * AcquiredChannelEnvironmentIssueAnalog}s retrieved.
   * @return All SOH analog objects that meet the query criteria.
   */
  @Override
  public List<AcquiredChannelEnvironmentIssueAnalog> retrieveAcquiredChannelEnvironmentIssueAnalogByChannelAndTimeRange(
      ChannelTimeRangeRequest request) {
    Objects.requireNonNull(request);

    sohRetrieveAnalog.updateMetric(this);
    Instant start = Instant.now();

    List<AcquiredChannelEnvironmentIssueAnalog> temp = querySohByChannelAndTimeRange(
        AcquiredChannelEnvironmentIssueAnalogDao.class,
        AcquiredChannelEnvironmentIssueAnalogDao::toCoi, request.getChannelName(),
        request.getTimeRange().getStartTime(),
        request.getTimeRange().getEndTime());

    Instant finish = Instant.now();
    long timeElapsed = Duration.between(start, finish).toMillis();
    sohRetrieveAnalogDuration.updateMetric(timeElapsed);

    return temp;
  }

  /**
   * Retrieves all {@link AcquiredChannelEnvironmentIssueBoolean} objects for the provided channel created within the
   * provided time range.
   *
   * @param request The collection of channel names and time range that will bound the {@link
   * AcquiredChannelEnvironmentIssueBoolean}s retrieved.
   * @return All SOH boolean objects that meet the query criteria.
   */
  @Override
  public List<AcquiredChannelEnvironmentIssueBoolean> retrieveAcquiredChannelEnvironmentIssueBooleanByChannelAndTimeRange(
      ChannelTimeRangeRequest request) {
    Objects.requireNonNull(request);

    sohRetrieveBoolean.updateMetric(this);
    Instant start = Instant.now();

    List<AcquiredChannelEnvironmentIssueBoolean> temp = querySohByChannelAndTimeRange(
        AcquiredChannelEnvironmentIssueBooleanDao.class,
        AcquiredChannelEnvironmentIssueBooleanDao::toCoi, request.getChannelName(),
        request.getTimeRange().getStartTime(),
        request.getTimeRange().getEndTime());

    Instant finish = Instant.now();
    long timeElapsed = Duration.between(start, finish).toMillis();
    sohRetrieveBooleanDuration.updateMetric(timeElapsed);

    return temp;
  }

  /**
   * Queries for JPA entities of type J from a particular channel within a time interval
   *
   * @param entityType JPA entity type (e.g. Class J), not null
   * @param converter converts from a JPA entity type J to the business object type B
   * @param channelName channel name the SOH was measured on.
   * @param startTime Inclusive start from time range for the query.
   * @param endTime Inclusive end from time range for the query.
   * @param <J> type of acquired channel SOH JPA entity (either {@link AcquiredChannelEnvironmentIssueBooleanDao} or
   * {@link AcquiredChannelEnvironmentIssueAnalogDao})
   * @param <B> type of acquired channel SOH business object (either {@link AcquiredChannelEnvironmentIssueBoolean} or
   * {@link AcquiredChannelEnvironmentIssueAnalog})
   * @return All SOH objects that meet the query criteria.
   */
  private <J, B> List<B> querySohByChannelAndTimeRange(
      Class<J> entityType, Function<J, B> converter,
      String channelName, Instant startTime, Instant endTime) {

    Objects.requireNonNull(channelName, "Cannot run query with null channel name");
    Objects.requireNonNull(startTime, "Cannot run query with null start time");
    Objects.requireNonNull(endTime, "Cannot run query with null end time");

    //this allows startTime == endTime
    ParameterValidation.requireFalse(Instant::isAfter, startTime, endTime,
        "Cannot run query with start time greater than end time");
    EntityManager entityManager = entityManagerFactory.createEntityManager();
    try {
      return retrieveDaos(entityManager, entityType, channelName, startTime, endTime)
          .stream()
          .map(converter)
          .collect(Collectors.toList());
    } catch (Exception ex) {
      logger.error("Error retrieving frames: {}", RepositoryExceptionUtils.wrap(ex).getMessage());
      throw ex;
    } finally {
      entityManager.close();
    }
  }

  private <J> List<J> retrieveDaos(EntityManager entityManager, Class<J> entityType,
      String channelName,
      Instant startTime, Instant endTime) {
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    CriteriaQuery<J> query = builder.createQuery(entityType);
    Root<J> fromEntity = query.from(entityType);
    query.select(fromEntity);

    Join<J, ChannelDao> channelJoin = fromEntity.join(CHANNEL);
    Expression<String> channelNameField = channelJoin.get(NAME);
    query.where(builder.and(
        builder.lessThanOrEqualTo(fromEntity.get(START_TIME), endTime),
        builder.greaterThanOrEqualTo(fromEntity.get(END_TIME), startTime),
        builder.equal(channelNameField, channelName)))
        .orderBy(
            builder.asc(channelJoin.get(NAME)),
            builder.asc(fromEntity.get(START_TIME)));

    // Get the SQL query string
    TypedQuery<J> findDaos = entityManager.createQuery(query);

    return findDaos.getResultList();
  }

  /**
   * Retrieve the {@link AcquiredChannelEnvironmentIssueAnalog} with the provided id.  Returns an empty {@link Optional}
   * if no AcquiredChannelSohAnalog has that id.
   *
   * @param acquiredChannelEnvironmentIssueId id for the AcquiredChannelSohAnalog, not null
   * @return Optional AcquiredChannelSohAnalog object with the provided id, not null
   */
  @Override
  public Optional<AcquiredChannelEnvironmentIssueAnalog> retrieveAcquiredChannelEnvironmentIssueAnalogById(
      UUID acquiredChannelEnvironmentIssueId) {
    Objects.requireNonNull(acquiredChannelEnvironmentIssueId,
        "retrieveAcquiredChannelSohAnalogById requires non-null acquiredChannelSohId");

    sohRetrieveACEIAnalogId.updateMetric(this);
    Instant start = Instant.now();

    Optional<AcquiredChannelEnvironmentIssueAnalog> temp = querySohById(AcquiredChannelEnvironmentIssueAnalogDao.class,
        AcquiredChannelEnvironmentIssueAnalogDao::toCoi,
        acquiredChannelEnvironmentIssueId);

    Instant finish = Instant.now();
    long timeElapsed = Duration.between(start, finish).toMillis();
    sohRetrieveACEIAnalogIdDuration.updateMetric(timeElapsed);

    return temp;
  }

  /**
   * Retrieve the {@link AcquiredChannelEnvironmentIssueBoolean} with the provided id.  Returns an empty {@link
   * Optional} if no AcquiredChannelSohBoolean has that id.
   *
   * @param acquiredChannelEnvironmentIssueId id for the AcquiredChannelSohBoolean, not null
   * @return Optional AcquiredChannelSohBoolean object with the provided id, not null
   */
  @Override
  public Optional<AcquiredChannelEnvironmentIssueBoolean> retrieveAcquiredChannelEnvironmentIssueBooleanById(
      UUID acquiredChannelEnvironmentIssueId) {
    Objects.requireNonNull(acquiredChannelEnvironmentIssueId,
        "retrieveAcquiredChannelSohBooleanById requires non-null acquiredChannelSohId");

    sohRetrieveACEIBooleanId.updateMetric(this);
    Instant start = Instant.now();

    Optional<AcquiredChannelEnvironmentIssueBoolean> temp = querySohById(
        AcquiredChannelEnvironmentIssueBooleanDao.class,
        AcquiredChannelEnvironmentIssueBooleanDao::toCoi,
        acquiredChannelEnvironmentIssueId);

    Instant finish = Instant.now();
    long timeElapsed = Duration.between(start, finish).toMillis();
    sohRetrieveACEIBooleanIdDuration.updateMetric(timeElapsed);

    return temp;
  }

  @Override
  public List<AcquiredChannelEnvironmentIssueAnalog> retrieveAcquiredChannelEnvironmentIssueAnalogByChannelTimeRangeAndType(
      ChannelTimeRangeSohTypeRequest request) {

    Objects.requireNonNull(request);

    sohRetrieveACEIAnalogTimeType.updateMetric(this);
    Instant start = Instant.now();

    List<AcquiredChannelEnvironmentIssueAnalog> temp = querySohByChannelTimeRangeAndType(
        AcquiredChannelEnvironmentIssueAnalogDao.class,
        new AcquiredChannelEnvironmentIssueAnalogDaoConverter(),
        request.getChannelName(),
        request.getTimeRange().getStartTime(),
        request.getTimeRange().getEndTime(),
        request.getType());

    Instant finish = Instant.now();
    long timeElapsed = Duration.between(start, finish).toMillis();
    sohRetrieveACEIAnalogTimeTypeDuration.updateMetric(timeElapsed);

    return temp;
  }

  @Override
  public List<AcquiredChannelEnvironmentIssueBoolean> retrieveAcquiredChannelSohBooleanByChannelTimeRangeAndType(
      ChannelTimeRangeSohTypeRequest request) {

    Objects.requireNonNull(request);

    sohRetrieveACEIBooleanTimeType.updateMetric(this);
    Instant start = Instant.now();

    List<AcquiredChannelEnvironmentIssueBoolean> temp =
        querySohByChannelTimeRangeAndType(AcquiredChannelEnvironmentIssueBooleanDao.class,
            new AcquiredChannelEnvironmentIssueBooleanDaoConverter(),
            request.getChannelName(),
            request.getTimeRange().getStartTime(),
            request.getTimeRange().getEndTime(),
            request.getType());

    Instant finish = Instant.now();
    long timeElapsed = Duration.between(start, finish).toMillis();
    sohRetrieveACEIBooleanTimeTypeDuration.updateMetric(timeElapsed);

    return temp;
  }

  private <E, C> List<C> querySohByChannelTimeRangeAndType(Class<E> entityType,
      EntityConverter<E, C> converter,
      String channelName,
      Instant startTime,
      Instant endTime,
      AcquiredChannelEnvironmentIssueType type) {

    Objects.requireNonNull(entityType);
    Objects.requireNonNull(converter);
    Objects.requireNonNull(channelName);
    Objects.requireNonNull(startTime);
    Objects.requireNonNull(endTime);
    Objects.requireNonNull(type);

    ParameterValidation.requireFalse(Instant::isAfter, startTime, endTime,
        "Cannot run query when start time is after end time");

    EntityManager entityManager = entityManagerFactory.createEntityManager();

    try {
      CriteriaBuilder builder = entityManager.getCriteriaBuilder();
      CriteriaQuery<E> sohQuery = builder.createQuery(entityType);
      Root<E> fromSoh = sohQuery.from(entityType);
      sohQuery.select(fromSoh);

      List<Predicate> conjunctions = new ArrayList<>();

      Join<E, ChannelDao> channelJoin = fromSoh.join(CHANNEL);
      conjunctions.add(builder.equal(channelJoin.get(NAME), channelName));
      conjunctions.add(builder.greaterThanOrEqualTo(fromSoh.get(START_TIME), startTime));
      conjunctions.add(builder.lessThanOrEqualTo(fromSoh.get(END_TIME), endTime));
      conjunctions.add(builder.equal(fromSoh.get("type"), type));

      sohQuery.where(builder.and(conjunctions.toArray(new Predicate[0])));

      return entityManager.createQuery(sohQuery)
          .getResultStream()
          .map(converter::toCoi)
          .collect(Collectors.toList());
    } finally {
      entityManager.close();
    }
  }

  /**
   * Queries the JPA entity of type J for an {@link AcquiredChannelEnvironmentIssue} object with the provided identity.
   * Uses the converter to convert from an instance of J to an AcquiredChannelSoh. Output {@link Optional} is empty when
   * the query does not find an entity.
   *
   * @param entityType JPA entity type (e.g. {@link AcquiredChannelEnvironmentIssueBooleanDao}, not null
   * @param converter converts from an entityType object to an AcquiredChannelSoh, not null
   * @param acquiredChannelSohId {@link UUID} of the desired AcquiredChannelSoh, not null
   * @param <D> JPA entity type
   * @return Optional AcquiredChannelSoh, not null
   */
  private <D, B> Optional<B> querySohById(Class<D> entityType, Function<D, B> converter,
      UUID acquiredChannelSohId) {
    EntityManager entityManager = entityManagerFactory.createEntityManager();
    try {
      D entity = entityManager.find(entityType, acquiredChannelSohId);
      return entity == null ? Optional.empty() : Optional.of(converter.apply(entity));
    } catch (NoResultException ex) {
      return Optional.empty();
    } catch (Exception ex) {
      logger.error("Error retrieving frames: {}", RepositoryExceptionUtils.wrap(ex).getMessage());
      throw ex;
    } finally {
      entityManager.close();
    }
  }
  /**
   * Queries the JPA entity of type E for an {@link AcquiredChannelEnvironmentIssue} object with the latest
   * entry for a given channel name.
   * Uses the converter to convert from an instance of E to type C. Output is empty list when
   * the query does not find an entity.
   *
   * @param entityType JPA entity type (e.g. {@link AcquiredChannelEnvironmentIssueBooleanDao}, not null
   * @param converter converts from an entityType object to an Type C, not null
   * @param channelNames {@link String} of the desired AcquiredChannelSoh, not null
   * @return List type C, not null
   */
  private <E, C> List<C> queryLatestSohByChannels(
      Class<E> entityType,
      EntityConverter<E, C> converter,
      List<String> channelNames) {
    EntityManager entityManager = entityManagerFactory.createEntityManager();
    List<Tuple> groupByResults;
    try{
      CriteriaBuilder builder = entityManager.getCriteriaBuilder();
      CriteriaQuery<Tuple> sohMultiQuery = builder.createTupleQuery();
      Root<E> sohTuple = sohMultiQuery.from(entityType);
      Join<E, ChannelDao> channelJoin = sohTuple.join(CHANNEL);
      Expression<String> channelName = channelJoin.get(NAME);

      Expression<Instant> endTime = sohTuple.get(END_TIME);
      Expression<AcquiredChannelEnvironmentIssueType> type = sohTuple.get(TYPE);
      sohMultiQuery.multiselect(channelName, type, builder.greatest(endTime)).groupBy(channelName, type);
      sohMultiQuery.where(channelName.in(channelNames));

      groupByResults = entityManager.createQuery(sohMultiQuery)
          .getResultStream()
          .collect(Collectors.toList());
    } catch (Exception ex) {
      logger.error("Error retrieving latest ACEI: {}",
          RepositoryExceptionUtils.wrap(ex).getMessage());
      throw ex;
    }
    try {
      CriteriaBuilder builder = entityManager.getCriteriaBuilder();
      CriteriaQuery<E> sohQuery = builder.createQuery(entityType);
      Root<E> fromSoh = sohQuery.from(entityType);
      sohQuery.select(fromSoh);

      Join<E, ChannelDao> channelJoin = fromSoh.join(CHANNEL);
      Expression<String> channelName = channelJoin.get(NAME);
      Expression<AcquiredChannelEnvironmentIssueType> type = fromSoh.get(TYPE);
      Expression<Instant> endTime = fromSoh.get(END_TIME);
      sohQuery.where(builder.or(groupByResults.stream()
      .map(resultTupe -> {
        return builder.and(builder.equal(channelName, resultTupe.get(0)),
            builder.equal(type, resultTupe.get(1)),
            builder.equal(endTime, resultTupe.get(2)));
      }).toArray(Predicate[]::new)));

      return entityManager.createQuery(sohQuery)
          .getResultStream()
          .map(converter::toCoi)
          .collect(Collectors.toList());
    } catch (Exception ex) {
      logger.error("Error retrieving latest ACEI: {}",
          RepositoryExceptionUtils.wrap(ex).getMessage());
      throw ex;
    } finally {
      entityManager.close();
    }
  }

  /**
   * Retrieve the {@link AcquiredChannelEnvironmentIssueAnalog} with the provided id.  Returns an empty {@link Optional}
   * if no AcquiredChannelSohAnalog has that id.
   *
   * @param request time range request to find AcquiredChannelEnvironmentIssueAnalogs by, not null
   * @return Optional AcquiredChannelSohAnalog object with the provided id, not null
   */
  @Override
  public List<AcquiredChannelEnvironmentIssueAnalog> retrieveAcquiredChannelEnvironmentIssueAnalogByTime(
      TimeRangeRequest request) {
    Objects.requireNonNull(request);

    sohRetrieveACEIAnalogTime.updateMetric(this);
    Instant start = Instant.now();
    List<AcquiredChannelEnvironmentIssueAnalog> temp = querySohByTimeRange(
        AcquiredChannelEnvironmentIssueAnalogDao.class,
        new AcquiredChannelEnvironmentIssueAnalogDaoConverter(),
        request.getStartTime(),
        request.getEndTime()
    );
    Instant finish = Instant.now();
    long timeElapsed = Duration.between(start, finish).toMillis();
    sohRetrieveACEIAnalogTimeDuration.updateMetric(timeElapsed);
    return temp;
  }

  /**
   * Retrieve the {@link AcquiredChannelEnvironmentIssueBoolean} with the provided id.  Returns an empty {@link
   * Optional} if no AcquiredChannelSohBoolean has that id.
   *
   * @param request time range for the AcquiredChannelSohBoolean, not null
   * @return Optional AcquiredChannelSohBoolean object with the provided id, not null
   */
  @Override
  public List<AcquiredChannelEnvironmentIssueBoolean> retrieveAcquiredChannelEnvironmentIssueBooleanByTime(
      TimeRangeRequest request) {
    Objects.requireNonNull(request);

    sohRetrieveACEIBooleanTime.updateMetric(this);
    Instant start = Instant.now();
    List<AcquiredChannelEnvironmentIssueBoolean> temp = querySohByTimeRange(
        AcquiredChannelEnvironmentIssueBooleanDao.class,
        new AcquiredChannelEnvironmentIssueBooleanDaoConverter(),
        request.getStartTime(),
        request.getEndTime()
    );
    Instant finish = Instant.now();
    long timeElapsed = Duration.between(start, finish).toMillis();
    sohRetrieveACEIBooleanTimeDuration.updateMetric(timeElapsed);
    return temp;
  }
  /**
   * Retrieve the list of {@link AcquiredChannelEnvironmentIssueAnalog} with the latest end time for a given channel.
   * Returns an empty {@link List} if no AcquiredChannelSohAnalog for query.
   *
   * @param channelNames time range for the AcquiredChannelSohAnalog, not null
   * @return List AcquiredChannelSohAnalog object with latest end times for given channel
   */
  @Override
  public List<AcquiredChannelEnvironmentIssueAnalog> retrieveLatestAcquiredChannelEnvironmentIssueAnalog(
      List<String> channelNames) {
    sohRetrieveLatestACEIAnalog.updateMetric(this);
    Instant start = Instant.now();
    List<AcquiredChannelEnvironmentIssueAnalog> latestAnalogACEIs = queryLatestSohByChannels(
        AcquiredChannelEnvironmentIssueAnalogDao.class,
        new AcquiredChannelEnvironmentIssueAnalogDaoConverter(),
        channelNames
    );

    Instant finish = Instant.now();
    long timeElapsed = Duration.between(start, finish).toMillis();
    sohRetrieveLatestACEIAnalogDuration.updateMetric(timeElapsed);
    return latestAnalogACEIs;
  }
  /**
   * Retrieve the list of {@link AcquiredChannelEnvironmentIssueBoolean} with the latest end time for a given channel.
   * Returns an empty {@link List} if no AcquiredChannelSohBoolean for query.
   *
   * @param channelNames time range for the AcquiredChannelSohBoolean, not null
   * @return List AcquiredChannelSohBoolean object with latest end times for given channel
   */
  @Override
  public List<AcquiredChannelEnvironmentIssueBoolean> retrieveLatestAcquiredChannelEnvironmentIssueBoolean(
      List<String> channelNames) {
    sohRetrieveLatestACEIBoolean.updateMetric(this);
    Instant start = Instant.now();
    List<AcquiredChannelEnvironmentIssueBoolean> latestBooleanACEIs = queryLatestSohByChannels(
        AcquiredChannelEnvironmentIssueBooleanDao.class,
        new AcquiredChannelEnvironmentIssueBooleanDaoConverter(),
        channelNames
    );
    Instant finish = Instant.now();
    long timeElapsed = Duration.between(start, finish).toMillis();
    sohRetrieveLatestACEIBooleanDuration.updateMetric(timeElapsed);
    return latestBooleanACEIs;
  }

  private <E, C> List<C> querySohByTimeRange(
      Class<E> entityType,
      EntityConverter<E, C> converter,
      Instant startTime,
      Instant endTime) {
    Objects.requireNonNull(entityType);
    Objects.requireNonNull(converter);
    Objects.requireNonNull(startTime);
    Objects.requireNonNull(endTime);

    ParameterValidation.requireFalse(Instant::isAfter, startTime, endTime,
        "Cannot run query when start time is after end time");

    EntityManager entityManager = entityManagerFactory.createEntityManager();
    try {
      CriteriaBuilder builder = entityManager.getCriteriaBuilder();
      CriteriaQuery<E> sohQuery = builder.createQuery(entityType);
      Root<E> fromSoh = sohQuery.from(entityType);
      sohQuery.select(fromSoh);

      List<Predicate> conjunctions = new ArrayList<>();
      conjunctions.add(builder.greaterThanOrEqualTo(fromSoh.get(START_TIME), startTime));
      conjunctions.add(builder.lessThanOrEqualTo(fromSoh.get(END_TIME), endTime));

      sohQuery.where(builder.and(conjunctions.toArray(new Predicate[0])));

      return entityManager.createQuery(sohQuery)
          .getResultStream()
          .map(converter::toCoi)
          .collect(Collectors.toList());
    } finally {
      entityManager.close();
    }
  }
}

