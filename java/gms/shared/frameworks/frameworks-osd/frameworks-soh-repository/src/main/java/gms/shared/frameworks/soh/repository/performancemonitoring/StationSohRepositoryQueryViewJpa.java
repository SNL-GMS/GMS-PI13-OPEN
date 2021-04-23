package gms.shared.frameworks.soh.repository.performancemonitoring;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import gms.shared.frameworks.osd.api.station.StationRepositoryInterface;
import gms.shared.frameworks.osd.api.util.RepositoryExceptionUtils;
import gms.shared.frameworks.osd.api.util.StationTimeRangeSohTypeRequest;
import gms.shared.frameworks.osd.api.waveforms.StationSohRepositoryQueryViewInterface;
import gms.shared.frameworks.osd.coi.ParameterValidation;
import gms.shared.frameworks.osd.coi.channel.Channel;
import gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssue;
import gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssue.AcquiredChannelEnvironmentIssueType;
import gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssueAnalog;
import gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssueBoolean;
import gms.shared.frameworks.osd.coi.signaldetection.Station;
import gms.shared.frameworks.osd.dao.channel.ChannelDao;
import gms.shared.frameworks.osd.dao.channelsoh.AcquiredChannelEnvironmentIssueAnalogDao;
import gms.shared.frameworks.osd.dao.channelsoh.AcquiredChannelEnvironmentIssueBooleanDao;
import gms.shared.frameworks.osd.dto.soh.HistoricalAcquiredChannelEnvironmentalIssues;
import gms.shared.frameworks.soh.repository.performancemonitoring.converter.AcquiredChannelEnvironmentIssueAnalogDaoConverter;
import gms.shared.frameworks.soh.repository.performancemonitoring.converter.AcquiredChannelEnvironmentIssueBooleanDaoConverter;
import gms.shared.frameworks.soh.repository.performancemonitoring.transform.AcquiredChannelEnvironmentalIssuesTransformer;
import gms.shared.frameworks.utilities.jpa.EntityConverter;
import gms.shared.metrics.CustomMetric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

public class StationSohRepositoryQueryViewJpa implements StationSohRepositoryQueryViewInterface {

  private static final Logger logger = LoggerFactory.getLogger(StationSohRepositoryJpa.class);
  private static final String CHANNEL = "channel";
  private static final String NAME = "name";
  private static final String START_TIME = "startTime";
  private static final String END_TIME = "endTime";
  private static final String TYPE = "type";

  private final EntityManagerFactory entityManagerFactory;
  private final StationRepositoryInterface stationRepository;

  /*
   * Custom metrics for tracking the hits of ACEI queries
   */
  private static final CustomMetric<StationSohRepositoryQueryViewJpa, Long> sohRetrieveACEIStationTimeType =
          CustomMetric.create(CustomMetric::incrementer, "soh_retrieve_acei_time_type_hits:type=Counter", 0L);

  /*
   * Custom metrics for tracking the Duration of ACEI queries
   */
  private static final CustomMetric<Long, Long> sohRetrieveACEIStationTimeTypeDuration =
          CustomMetric.create(CustomMetric::updateTimingData, "soh_retrieve_acei_analog_station_time_type:type=Value", 0L);

  /**
   * Default constructor
   *
   * @param entityManagerFactory {@link EntityManagerFactory}
   */
  public StationSohRepositoryQueryViewJpa(EntityManagerFactory entityManagerFactory,
                                          StationRepositoryInterface stationRepository) {
    this.entityManagerFactory = entityManagerFactory;
    this.stationRepository = stationRepository;
  }

  public static StationSohRepositoryQueryViewJpa create(EntityManagerFactory entityManagerFactory,
                                                    StationRepositoryInterface stationRepository) {

    Objects.requireNonNull(entityManagerFactory);
    Objects.requireNonNull(stationRepository);

    return new StationSohRepositoryQueryViewJpa(entityManagerFactory, stationRepository);
  }

  /**
   * Retrieves historical environmental issue DTO objects based on a Station ID (i.e. Station name),
   * a time range, and an AcquiredChannelEnvironmentIssueType.
   *
   * @param request The station name, type, and time range that will bound the {@link
   *                AcquiredChannelEnvironmentIssue}s retrieved.
   * @return a {@link List} of {@link HistoricalAcquiredChannelEnvironmentalIssues}
   */
  @Override
  public List<HistoricalAcquiredChannelEnvironmentalIssues> retrieveAcquiredChannelEnvironmentIssuesByStationTimeRangeAndType(
      StationTimeRangeSohTypeRequest request) {

    sohRetrieveACEIStationTimeType.updateMetric(this);
    checkNotNull(request, "Request cannot be null");
    checkArgument(checkIfValidEnvironmentIssue(request.getType()),
        "Please provide a valid AcquiredChannelEnvironmentIssueType");

    Instant start = Instant.now();

    // Get the station name and initialize the ACEI list
    String stationName = request.getStationName();

    // Step 1: Retrieve the Station channels using the StationRepository
    List<Station> storedStations = stationRepository.retrieveAllStations(List.of(stationName));
    List<String> channelNames = storedStations.get(0).getChannels().
            stream().map(Channel::getName).collect(Collectors.toList());

    // Step 2: Query the ACEI Analog table using station channels, time and type
    List<AcquiredChannelEnvironmentIssueAnalog> aceiAnalogList = querySohByStationTimeRangeAndType(
            AcquiredChannelEnvironmentIssueAnalogDao.class,
            new AcquiredChannelEnvironmentIssueAnalogDaoConverter(),
            channelNames,
            request.getTimeRange().getStartTime(),
            request.getTimeRange().getEndTime(),
            request.getType());

    // Step 3: Query the ACEI Boolean table using station channels, time and type
    List<AcquiredChannelEnvironmentIssueBoolean> aceiBooleanList = querySohByStationTimeRangeAndType(
            AcquiredChannelEnvironmentIssueBooleanDao.class,
            new AcquiredChannelEnvironmentIssueBooleanDaoConverter(),
            channelNames,
            request.getTimeRange().getStartTime(),
            request.getTimeRange().getEndTime(),
            request.getType());

    // Final ACEI list for boolean and analog
    List<AcquiredChannelEnvironmentIssue> aceiList = new ArrayList<>(aceiAnalogList);
    aceiList.addAll(new ArrayList<AcquiredChannelEnvironmentIssue>(aceiBooleanList));

    List<HistoricalAcquiredChannelEnvironmentalIssues> historicalAceis = AcquiredChannelEnvironmentalIssuesTransformer
            .toHistoricalAcquiredChannelEnvironmentalIssues(aceiList);

    Instant finish = Instant.now();
    long timeElapsed = Duration.between(start, finish).toMillis();
    sohRetrieveACEIStationTimeTypeDuration.updateMetric(timeElapsed);

    return historicalAceis;
  }

  /* ----------------------------------------------------------------
     Private Methods
     ---------------------------------------------------------------- */

  /**
   * Query ACEI soh by station name, time range and soh type
   * @param entityType - Object type (boolean/analog)
   * @param converter - DAO converter
   * @param channelNames - station id
   * @param startTime - request start instant
   * @param endTime - request end instant
   * @param sohType - type of flag to query
   * @param <J> - input object type
   * @param <B> - converter object type
   * @return List<J>
   */
  private <J, B> List<B> querySohByStationTimeRangeAndType(
          Class<J> entityType, EntityConverter<J, B> converter,
          List<String> channelNames, Instant startTime, Instant endTime,
          AcquiredChannelEnvironmentIssueType sohType) {

    Objects.requireNonNull(channelNames, "Cannot run query with null channel names");
    Objects.requireNonNull(startTime, "Cannot run query with null start time");
    Objects.requireNonNull(endTime, "Cannot run query with null end time");
    Objects.requireNonNull(sohType, "Cannot run query with null soh type");

    //this allows startTime == endTime
    ParameterValidation.requireFalse(Instant::isAfter, startTime, endTime,
            "Cannot run query with start time greater than end time");

    EntityManager entityManager = entityManagerFactory.createEntityManager();
    try {
      CriteriaBuilder builder = entityManager.getCriteriaBuilder();
      CriteriaQuery<J> aceiQuery = builder.createQuery(entityType);
      Root<J> fromAcei = aceiQuery.from(entityType);
      aceiQuery.select(fromAcei);

      List<Predicate> conjunctions = new ArrayList<>();

      // Create a join with the Channel table, then add query clause equivalent to "where channel
      // .id in (:ids)".  This will then be and-ed with the block of disjunctions below
      Join<J, ChannelDao> channelJoin = fromAcei.join(CHANNEL);
      Expression<String> inChannelNames = channelJoin.get(NAME);
      conjunctions.add(inChannelNames.in(channelNames));

      // Create the time range request start < end
      conjunctions.add(builder.and(builder.lessThanOrEqualTo(fromAcei.get(START_TIME), endTime),
              builder.greaterThanOrEqualTo(fromAcei.get(END_TIME), startTime)));

      // Query the soh type for the ACEI table
      conjunctions.add(builder.equal(fromAcei.get(TYPE), sohType));

      // Build the final SQL query
      aceiQuery.where(builder.and(conjunctions.toArray(new Predicate[0])));

      TypedQuery<J> findAcei = entityManager.createQuery(aceiQuery);
      String daoQueryString = findAcei.unwrap(org.hibernate.query.Query.class).getQueryString();

      logger.info("Find ACEI Query (Station, time, type): {}", daoQueryString);
      return findAcei.getResultStream()
              .map(converter::toCoi)
              .collect(Collectors.toList());
    } catch (Exception ex) {
      logger.error("Error retrieving frames: {}", RepositoryExceptionUtils.wrap(ex).getMessage());
      entityManager.getTransaction().rollback();
      throw ex;
    } finally {
      entityManager.close();
    }
  }

  private boolean checkIfValidEnvironmentIssue(
      AcquiredChannelEnvironmentIssueType acquiredChannelEnvironmentIssueType) {
    for (AcquiredChannelEnvironmentIssueType issueType : AcquiredChannelEnvironmentIssueType
        .values()) {
      if (issueType.equals(acquiredChannelEnvironmentIssueType)) {
        return true;
      }
    }

    return false;
  }
}
