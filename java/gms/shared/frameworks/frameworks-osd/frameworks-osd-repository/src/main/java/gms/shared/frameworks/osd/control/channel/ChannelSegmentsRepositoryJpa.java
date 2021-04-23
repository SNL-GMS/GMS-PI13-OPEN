package gms.shared.frameworks.osd.control.channel;

import com.google.common.base.Functions;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableRangeMap;
import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import gms.shared.frameworks.coi.exceptions.DataExistsException;
import gms.shared.frameworks.coi.exceptions.RepositoryException;
import gms.shared.frameworks.osd.api.channel.ChannelRepositoryInterface;
import gms.shared.frameworks.osd.api.channel.ChannelSegmentsRepositoryInterface;
import gms.shared.frameworks.osd.api.channel.util.ChannelSegmentsIdRequest;
import gms.shared.frameworks.osd.api.util.ChannelTimeRangeRequest;
import gms.shared.frameworks.osd.api.util.ChannelsTimeRangeRequest;
import gms.shared.frameworks.osd.api.util.TimeRangeRequest;
import gms.shared.frameworks.osd.coi.channel.Channel;
import gms.shared.frameworks.osd.coi.channel.ChannelSegment;
import gms.shared.frameworks.osd.coi.channel.repository.jpa.dataaccessobjects.ChannelDao;
import gms.shared.frameworks.osd.coi.channel.repository.jpa.dataaccessobjects.ChannelSegmentDao;
import gms.shared.frameworks.osd.coi.waveforms.FkSpectra;
import gms.shared.frameworks.osd.coi.waveforms.Timeseries;
import gms.shared.frameworks.osd.coi.waveforms.Timeseries.Type;
import gms.shared.frameworks.osd.coi.waveforms.Waveform;
import gms.shared.frameworks.osd.coi.waveforms.repository.jpa.FkSpectraDao;
import gms.shared.frameworks.osd.control.waveforms.TimeseriesRepositoryCassandra;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.RollbackException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChannelSegmentsRepositoryJpa implements ChannelSegmentsRepositoryInterface {

  public static final String START_TIME = "startTime";
  private final Logger logger = LoggerFactory.getLogger(ChannelSegmentsRepositoryJpa.class);

  private final EntityManagerFactory entityManagerFactory;
  private final ChannelRepositoryInterface channelRepository;
  private final TimeseriesRepositoryCassandra timeseriesRepository;

  private ChannelSegmentsRepositoryJpa(EntityManagerFactory entityManagerFactory,
      ChannelRepositoryInterface channelRepository,
      TimeseriesRepositoryCassandra timeseriesRepository) {
    this.entityManagerFactory = entityManagerFactory;
    this.channelRepository = channelRepository;
    this.timeseriesRepository = timeseriesRepository;
  }

  public static ChannelSegmentsRepositoryJpa create(EntityManagerFactory entityManagerFactory,
      ChannelRepositoryInterface channelRepository,
      TimeseriesRepositoryCassandra timeseriesRepository) {

    Objects.requireNonNull(entityManagerFactory);
    Objects.requireNonNull(channelRepository);
    Objects.requireNonNull(timeseriesRepository);

    return new ChannelSegmentsRepositoryJpa(entityManagerFactory, channelRepository,
        timeseriesRepository);
  }

  @Override
  public Collection<ChannelSegment<? extends Timeseries>> retrieveChannelSegmentsByIds(
      ChannelSegmentsIdRequest request) {
    Objects.requireNonNull(request);

    EntityManager entityManager = entityManagerFactory.createEntityManager();
    try {
      CriteriaBuilder builder = entityManager.getCriteriaBuilder();
      CriteriaQuery<ChannelSegmentDao> channelSegmentQuery =
          builder.createQuery(ChannelSegmentDao.class);
      Root<ChannelSegmentDao> fromChannelSegment = channelSegmentQuery
          .from(ChannelSegmentDao.class);
      channelSegmentQuery.select(fromChannelSegment);

      // Set up an expression in the criteria equivalent to channel_segment.id in (:ids)
      Expression<UUID> inChannelSegmentIds = fromChannelSegment.get("id");
      channelSegmentQuery.where(inChannelSegmentIds.in(request.getChannelSegmentIds()));
      return populateChannelSegments(entityManager.createQuery(channelSegmentQuery).getResultList());
    } finally {
      entityManager.close();
    }
  }

  @Override
  public Collection<ChannelSegment<Waveform>> retrieveChannelSegmentsByChannelNames(
      ChannelsTimeRangeRequest request) {
    Objects.requireNonNull(request);

    EntityManager entityManager = entityManagerFactory.createEntityManager();

    Map<String, Channel> channelsByName =
        channelRepository.retrieveChannels(request.getChannelNames())
            .stream()
            .collect(Collectors.toMap(Channel::getCanonicalName, Functions.identity()));

    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    CriteriaQuery<ChannelSegmentDao> channelSegmentQuery =
        builder.createQuery(ChannelSegmentDao.class);
    Root<ChannelSegmentDao> fromChannelSegment = channelSegmentQuery.from(ChannelSegmentDao.class);
    channelSegmentQuery.select(fromChannelSegment);

    List<Predicate> conjunctions = new ArrayList<>();

    // create a join with the Channel table, then add query clause equivalent to "where channel
    // .id in (:ids)".  This will then be and-ed with the block of disjunctions below
    Join<ChannelSegmentDao, ChannelDao> channelJoin = fromChannelSegment.join("channel");
    Expression<String> inChannelNames = channelJoin.get("name");
    conjunctions.add(inChannelNames.in(request.getChannelNames()));

    Expression<Timeseries.Type> timeseriesType = fromChannelSegment.get("timeseriesType");
    conjunctions.add(builder.equal(timeseriesType, Timeseries.Type.WAVEFORM));

    TimeRangeRequest timeRangeRequest = request.getTimeRange();
    List<Predicate> disjunctions = new ArrayList<>();
    Path<Instant> startTime = fromChannelSegment.get(START_TIME);
    Path<Instant> endTime = fromChannelSegment.get("endTime");

    Instant rangeStart = timeRangeRequest.getStartTime();
    Instant rangeEnd = timeRangeRequest.getEndTime();

    // Case 1: the channel segment starts within the query range (but doesn't necessarily end)
    disjunctions.add(builder.and(builder.greaterThanOrEqualTo(startTime, rangeStart),
        builder.lessThanOrEqualTo(startTime, rangeEnd)));
    // Case 2: the channel segment ends within the query range (but doesn't necessarily start)
    disjunctions.add(builder.and(builder.greaterThanOrEqualTo(endTime, rangeStart),
        builder.lessThanOrEqualTo(endTime, rangeEnd)));
    // Case 3: the channel segment starts before the query range and ends after it
    disjunctions.add(builder.and(builder.lessThanOrEqualTo(startTime, rangeStart),
        builder.greaterThanOrEqualTo(endTime, rangeEnd)));
    conjunctions.add(builder.or(disjunctions.toArray(new Predicate[0])));

    channelSegmentQuery
        .where(builder.and(conjunctions.toArray(new Predicate[0])));

    Map<String, List<ChannelSegmentDao>> channelSegmentsByChannelName =
        entityManager.createQuery(channelSegmentQuery)
            .getResultStream()
            .collect(Collectors.groupingBy(
                channelSegmentDao -> channelSegmentDao.getChannel().getCanonicalName()));

    Map<String, List<Waveform>> waveformsByCanonicalChannelName = timeseriesRepository
        .retrieveWaveformsByChannelAndTime(channelSegmentsByChannelName.keySet(),
            rangeStart, rangeEnd);

    List<ChannelSegment<Waveform>> channelSegments = new ArrayList<>();
    for (Map.Entry<String, List<ChannelSegmentDao>> entry :
        channelSegmentsByChannelName.entrySet()) {
      long typeCount = entry.getValue().stream()
          .map(ChannelSegmentDao::getType)
          .distinct()
          .count();
      long nameCount = entry.getValue().stream()
          .map(ChannelSegmentDao::getName)
          .distinct()
          .count();
      if (typeCount != 1 || nameCount != 1) {
        logger.error(
            "Retrieved ChannelSegment's for channelName {} and time range [{}, {}] are of " +
                "different segment types or have different names",
            entry.getKey(), rangeStart, rangeEnd);
      } else if (!waveformsByCanonicalChannelName.containsKey(entry.getKey())) {
        logger.error("Could not find waveforms channel {} and time range [{} {}] even though " +
                "ChannelSegments are stored",
            entry.getKey(), rangeStart, rangeEnd);
      } else {
        final ChannelSegmentDao dao = entry.getValue().get(0);
        channelSegments.add(ChannelSegment.create(
            channelsByName.get(entry.getKey()),
            dao.getName(),
            dao.getType(),
            waveformsByCanonicalChannelName.get(entry.getKey())));
      }
    }

    entityManager.close();

    return channelSegments;
  }

  @Override
  public Collection<ChannelSegment<Waveform>> retrieveChannelSegmentsByChannelsAndTimeRanges(
      Collection<ChannelTimeRangeRequest> channelTimeRangeRequests) {

    Objects.requireNonNull(channelTimeRangeRequests);
    Preconditions.checkState(!channelTimeRangeRequests.isEmpty());

    Map<String, List<Range<Instant>>> timeRangesByChannelName = channelTimeRangeRequests.stream()
        .collect(Collectors.groupingBy(ChannelTimeRangeRequest::getChannelName)).entrySet().stream()
        .collect(Collectors.toMap(Entry::getKey, e -> getTimeRanges(e.getValue())));

    Map<String, Channel> channelsByName = channelRepository.retrieveChannels(
        timeRangesByChannelName.keySet())
        .stream().collect(Collectors.toMap(Channel::getName, Functions.identity()));
    Map<String, Channel> channelsByCanonicalName = channelsByName.values().stream()
        .collect(Collectors.toMap(Channel::getCanonicalName, Functions.identity()));

    EntityManager entityManager = entityManagerFactory.createEntityManager();

    Collector<ChannelSegmentDao, ImmutableRangeMap.Builder<Instant, ChannelSegmentDao>,
        RangeMap<Instant, ChannelSegmentDao>> collector =
        Collector.of(ImmutableRangeMap::builder,
            (builder, csDao) -> builder.put(Range.closed(csDao.getStartTime(),
                csDao.getEndTime()), csDao),
            (builder1, builder2) -> builder1.putAll(builder2.build()),
            ImmutableRangeMap.Builder::build,
            Collector.Characteristics.UNORDERED);

    CriteriaQuery<ChannelSegmentDao> channelSegmentQuery = buildChannelSegmentsQuery(entityManager,
        channelTimeRangeRequests, Timeseries.Type.WAVEFORM, false);

    Map<String, RangeMap<Instant, ChannelSegmentDao>> csDaosByRangeAndChannel =
        entityManager.createQuery(channelSegmentQuery)
            .getResultStream()
            .collect(Collectors.groupingBy(csDao -> csDao.getChannel().getName(), collector));

    List<ChannelSegment<Waveform>> channelSegments = timeRangesByChannelName.entrySet().stream()
        .filter(entry -> csDaosByRangeAndChannel.containsKey(entry.getKey()))
        .map(entry -> getChannelSegmentsForRanges(entry.getValue(),
            channelsByName.get(entry.getKey()),
            csDaosByRangeAndChannel.get(entry.getKey())))
        .collect(Collectors.flatMapping(List::stream, Collectors.toList()));

    entityManager.close();

    return channelSegments;
  }

  private List<ChannelSegment<Waveform>> getChannelSegmentsForRanges(List<Range<Instant>> ranges,
      Channel channel,
      RangeMap<Instant, ChannelSegmentDao> channelSegmentsByRange) {

    return ranges.stream()
        .map(range -> getChannelSegmentForRange(range,
            channel,
            new ArrayList<>(channelSegmentsByRange.subRangeMap(range).asMapOfRanges().values())))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(Collectors.toList());
  }

  private Optional<ChannelSegment<Waveform>> getChannelSegmentForRange(Range<Instant> range,
      Channel channel,
      List<ChannelSegmentDao> csDaosForRange) {
    List<Waveform> waveforms = timeseriesRepository
        .retrieveWaveformsByTime(channel.getCanonicalName(),
            range.lowerEndpoint(),
            range.upperEndpoint());

      return buildChannelSegment(channel,
          csDaosForRange,
          List.of(range),
          waveforms);
  }

  private static List<Range<Instant>> getTimeRanges(Collection<ChannelTimeRangeRequest> reqs) {
    return reqs.stream()
        .map(r -> Range.closed(r.getTimeRange().getStartTime(), r.getTimeRange().getEndTime()))
        .collect(Collectors.toList());
  }

  private Optional<ChannelSegment<Waveform>> buildChannelSegment(Channel channel,
      List<ChannelSegmentDao> channelSegmentDaos,
      List<Range<Instant>> ranges,
      List<Waveform> waveforms) {
    long typeCount = channelSegmentDaos.stream()
        .map(ChannelSegmentDao::getType)
        .distinct()
        .count();
    long nameCount = channelSegmentDaos.stream()
        .map(ChannelSegmentDao::getName)
        .distinct()
        .count();
    if (typeCount != 1 || nameCount != 1) {
      logger.error(
          "Retrieved ChannelSegment's for channelName {} and time ranges {} are of " +
              "different segment types or have different names", channel.getName(), ranges);
      return Optional.empty();
    } else if (waveforms.isEmpty()) {
      logger.error("Could not find waveforms channel {} and time ranges{} even though " +
          "ChannelSegments are stored", channel.getName(), ranges);
      return Optional.empty();
    } else {
      return Optional.of(ChannelSegment.create(
          channel,
          channelSegmentDaos.get(0).getName(),
          channelSegmentDaos.get(0).getType(),
          waveforms));
    }
  }

  @Override
  public void storeChannelSegments(Collection<ChannelSegment<Waveform>> segments) {
    Objects.requireNonNull(segments);
    Preconditions.checkState(!segments.isEmpty());

    EntityManager entityManager = entityManagerFactory.createEntityManager();
    entityManager.getTransaction().begin();
    try {
      for (ChannelSegment<Waveform> channelSegment : segments) {
        ChannelSegmentDao channelSegmentDao = setUpNewChannelSegment(entityManager, channelSegment);
        timeseriesRepository.storeWaveforms(channelSegment.getTimeseries(),
            channelSegment.getChannel().getCanonicalName());
        entityManager.merge(channelSegmentDao);
      }
      entityManager.getTransaction().commit();
    } catch (RollbackException ex) {
      entityManager.getTransaction().rollback();
      throw new IllegalStateException("Failed storing segments", ex);
    } finally {
      entityManager.close();
    }
  }

  @Override
  public List<ChannelSegment<FkSpectra>> retrieveFkChannelSegmentsByChannelsAndTime(
      Collection<ChannelTimeRangeRequest> channelTimeRangeRequests) {
    EntityManager entityManager = entityManagerFactory.createEntityManager();
    try {
      CriteriaQuery<ChannelSegmentDao> channelSegmentQuery = buildChannelSegmentsQuery(
          entityManager,
          channelTimeRangeRequests,
          Timeseries.Type.FK_SPECTRA,
          true);
      return entityManager.createQuery(channelSegmentQuery).getResultList()
          .stream().map(this::populateFkSegment)
          .collect(Collectors.toList());
    } finally {
      entityManager.close();
    }
  }

  @Override
  public void storeFkChannelSegments(Collection<ChannelSegment<FkSpectra>> segments) {
    Validate
        .notEmpty(segments, "Must pass in a non-empty collection of FkSpectra channel segments");

    EntityManager entityManager = entityManagerFactory.createEntityManager();
    entityManager.getTransaction().begin();
    try {
      for (ChannelSegment<FkSpectra> channelSegment : segments) {
        if (entityManager.find(ChannelSegmentDao.class, channelSegment.getId()) != null) {
          throw new DataExistsException("Existing ChannelSegment found");
        }

        ChannelSegmentDao channelSegmentDao = setUpNewChannelSegment(entityManager, channelSegment);
        // store the time series data first
        List<FkSpectraDao> fkSpectraDaos = new ArrayList<>();
        List<UUID> timeSeriesIds = new ArrayList<>();
        for (FkSpectra spectra : channelSegment.getTimeseries()) {
          FkSpectraDao fkSpectraDao = FkSpectraDao.fromCoi(spectra);
          UUID timeSeriesId = UUID.randomUUID();
          fkSpectraDao.getTimeSeries().setId(timeSeriesId);
          timeSeriesIds.add(timeSeriesId);
          fkSpectraDaos.add(fkSpectraDao);
        }
        channelSegmentDao.setTimeSeriesIds(timeSeriesIds);
        timeseriesRepository.storeFk(fkSpectraDaos);

        // store the metadata now
        fkSpectraDaos.forEach(entityManager::persist);
        entityManager.merge(channelSegmentDao);
      }
      entityManager.getTransaction().commit();
    } catch (RollbackException | RepositoryException ex) {
      entityManager.getTransaction().rollback();
      throw new IllegalStateException("Failed storing Fk channel segments", ex);
    } finally {
      entityManager.close();
    }
  }

  private ChannelSegmentDao setUpNewChannelSegment(EntityManager entityManager,
      ChannelSegment<? extends Timeseries> channelSegment) {
    ChannelSegmentDao channelSegmentDao = new ChannelSegmentDao(channelSegment);
    final ChannelDao channelDao = entityManager
        .find(ChannelDao.class, channelSegment.getChannel().getName());
    if (channelDao == null) {
      channelRepository.storeChannels(List.of(channelSegment.getChannel()));
    } else {
      channelSegmentDao.setChannel(channelDao);
    }
    return channelSegmentDao;
  }

  /**
   * Populates the provided {@link ChannelSegmentDao}s, retrieves the corresponding {@link
   * Timeseries}, and convert them to {@link ChannelSegment}
   *
   * @param channelSegmentDaos The {@link ChannelSegmentDao}s to convert
   * @return The populated and converted {@link ChannelSegment}s
   */
  private List<ChannelSegment<? extends Timeseries>> populateChannelSegments(
      List<ChannelSegmentDao> channelSegmentDaos) {
    final List<ChannelSegment<? extends Timeseries>> segments =
        new ArrayList<>(channelSegmentDaos.size());
    for (ChannelSegmentDao dao : channelSegmentDaos) {
      final Timeseries.Type timeseriesType = dao.getTimeseriesType();
      final ChannelSegment<? extends Timeseries> segment;
      if (timeseriesType == Type.WAVEFORM) {
        segment = populateWaveformSegment(dao);
      } else if (timeseriesType == Type.FK_SPECTRA) {
        segment = populateFkSegment(dao);
      } else {
        throw new IllegalArgumentException("Unsupported timeseries type " + timeseriesType);
      }
      segments.add(segment);
    }
    return segments;
  }

  private ChannelSegment<Waveform> populateWaveformSegment(ChannelSegmentDao dao) {
    final Channel channel = channelFor(dao);
    final List<Waveform> waveforms = timeseriesRepository.retrieveWaveformsByTime(
        channel.getCanonicalName(),
        dao.getStartTime(),
        dao.getEndTime());
    return ChannelSegment.from(dao.getId(),
        channel,
        dao.getName(),
        dao.getType(),
        waveforms);
  }

  private ChannelSegment<FkSpectra> populateFkSegment(ChannelSegmentDao dao) {
    // Query for the JPA part of the FKSpectra
    EntityManager entityManager = entityManagerFactory.createEntityManager();
    final Channel channel = channelFor(dao);
    CriteriaBuilder spectraBuilder = entityManager.getCriteriaBuilder();
    CriteriaQuery<FkSpectraDao> fkSpectraQuery = spectraBuilder.createQuery(FkSpectraDao.class);
    Root<FkSpectraDao> fromFkSpectra = fkSpectraQuery.from(FkSpectraDao.class);

    // Query for the id of the embedded timeseries object in the FkSpectra object
    Expression<UUID> inTimeseriesId = fromFkSpectra.get("timeSeries").get("id");
    fkSpectraQuery.where(inTimeseriesId.in(dao.getTimeSeriesIds()));

    List<FkSpectraDao> fkSpectraDaos =
        entityManager.createQuery(fkSpectraQuery).getResultList();
    List<FkSpectra> fkSpectras = timeseriesRepository.populateFkSpectra(fkSpectraDaos);
    return ChannelSegment.from(dao.getId(),
        channel,
        dao.getName(),
        dao.getType(),
        fkSpectras);
  }

  private Channel channelFor(ChannelSegmentDao segmentDao) {
    return channelRepository.retrieveChannels(
        List.of(segmentDao.getChannel().getName())).get(0);
  }

  private CriteriaQuery<ChannelSegmentDao> buildChannelSegmentsQuery(EntityManager em,
      Collection<ChannelTimeRangeRequest> channelTimeRangeRequests, Timeseries.Type tsType,
      boolean orderSegmentsByStartTime) {

    CriteriaBuilder builder = em.getCriteriaBuilder();
    CriteriaQuery<ChannelSegmentDao> channelSegmentQuery =
        builder.createQuery(ChannelSegmentDao.class);
    Root<ChannelSegmentDao> fromChannelSegment = channelSegmentQuery.from(ChannelSegmentDao.class);

    Join<ChannelSegmentDao, ChannelDao> channelJoin = fromChannelSegment.join("channel");
    Expression<String> channelName = channelJoin.get("name");

    Expression<Instant> startTime = fromChannelSegment.get(START_TIME);
    Expression<Instant> endTime = fromChannelSegment.get("endTime");

    Expression<Timeseries.Type> timeseriesType = fromChannelSegment.get("timeseriesType");
    channelSegmentQuery.where(builder.and(builder.equal(timeseriesType, tsType),
        builder.or(channelTimeRangeRequests.stream()
            .map(request ->
                builder.and(builder.equal(channelName, request.getChannelName()),
                    builder.greaterThanOrEqualTo(endTime, request.getTimeRange().getStartTime()),
                    builder.lessThanOrEqualTo(startTime, request.getTimeRange().getEndTime())))
            .toArray(Predicate[]::new))));

    if (orderSegmentsByStartTime) {
      channelSegmentQuery.orderBy(builder.asc(fromChannelSegment.get(START_TIME)));
    }

    return channelSegmentQuery;
  }

}
