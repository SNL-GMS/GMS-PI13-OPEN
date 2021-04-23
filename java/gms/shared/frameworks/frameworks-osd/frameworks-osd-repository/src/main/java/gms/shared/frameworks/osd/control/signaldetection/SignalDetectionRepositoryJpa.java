package gms.shared.frameworks.osd.control.signaldetection;

import com.google.common.base.Functions;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicates;
import gms.shared.frameworks.osd.api.signaldetection.SignalDetectionRepositoryInterface;
import gms.shared.frameworks.osd.api.util.StationsTimeRangeRequest;
import gms.shared.frameworks.osd.coi.channel.Channel;
import gms.shared.frameworks.osd.coi.channel.repository.jpa.dataaccessobjects.ChannelDao;
import gms.shared.frameworks.osd.coi.signaldetection.FeatureMeasurement;
import gms.shared.frameworks.osd.coi.signaldetection.MeasuredChannelSegmentDescriptor;
import gms.shared.frameworks.osd.coi.signaldetection.SignalDetection;
import gms.shared.frameworks.osd.coi.signaldetection.SignalDetectionHypothesis;
import gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.dataaccessobjects.FeatureMeasurementDao;
import gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.dataaccessobjects.InstantFeatureMeasurementDao;
import gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.dataaccessobjects.MeasuredChannelSegmentDescriptorDao;
import gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.dataaccessobjects.PhaseFeatureMeasurementDao;
import gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.dataaccessobjects.SignalDetectionDao;
import gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.dataaccessobjects.SignalDetectionHypothesisDao;
import gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.dataaccessobjects.StationDao;
import gms.shared.frameworks.osd.control.utils.FeatureMeasurementDaoUtility;
import gms.shared.frameworks.osd.control.utils.MeasuredChannelSegmentDescriptorDaoUtility;
import gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.util.SignalDetectionDaoConverter;
import gms.shared.frameworks.osd.control.utils.ChannelUtils;
import gms.shared.frameworks.osd.control.utils.SignalDetectionHypothesisDaoUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.RollbackException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class SignalDetectionRepositoryJpa implements SignalDetectionRepositoryInterface {

  private static final Logger logger = LoggerFactory.getLogger(SignalDetectionRepositoryJpa.class);

  private final EntityManagerFactory entityManagerFactory;

  public SignalDetectionRepositoryJpa(EntityManagerFactory entityManagerFactory) {
    this.entityManagerFactory = entityManagerFactory;
  }

  @Override
  public void storeSignalDetections(Collection<SignalDetection> signalDetections) {

    EntityManager entityManager = entityManagerFactory.createEntityManager();

    try {
      // Find the stations for the signal detections - if any are missing then we cannot continue
      Map<String, List<SignalDetection>> signalDetectionsByStation = signalDetections.stream()
          .collect(Collectors.groupingBy(SignalDetection::getStationName));

      CriteriaBuilder builder = entityManager.getCriteriaBuilder();
      CriteriaQuery<StationDao> stationQuery = builder.createQuery(StationDao.class);
      Root<StationDao> fromStation = stationQuery.from(StationDao.class);
      stationQuery.select(fromStation);

      Path<String> stationName = fromStation.get("name");
      stationQuery.where(stationName.in(signalDetectionsByStation.keySet()));

      List<StationDao> stations = entityManager.createQuery(stationQuery).getResultList();

      if (stations.size() < signalDetectionsByStation.keySet().size()) {
        throw new IllegalStateException("Cannot insert SignalDetection without a corresponding " +
            "station");
      }

      CriteriaQuery<SignalDetectionDao> signalDetectionQuery =
          builder.createQuery(SignalDetectionDao.class);
      Root<SignalDetectionDao> fromSignalDetection =
          signalDetectionQuery.from(SignalDetectionDao.class);
      signalDetectionQuery.select(fromSignalDetection);

      List<UUID> signalDetectionIds = signalDetections.stream()
          .map(SignalDetection::getId)
          .collect(Collectors.toList());
      Path<UUID> signalDetectionId = fromSignalDetection.get("id");

      signalDetectionQuery.where(signalDetectionId.in(signalDetectionIds));

      Map<UUID, SignalDetectionDao> signalDetectionsById =
          entityManager.createQuery(signalDetectionQuery)
              .getResultStream()
              .collect(Collectors.toMap(SignalDetectionDao::getId,
                  Functions.identity()));

      Set<UUID> parentSdhIds = signalDetections.stream()
          .flatMap(sd -> sd.getSignalDetectionHypotheses().stream())
          .map(SignalDetectionHypothesis::getParentSignalDetectionHypothesisId)
          .filter(Optional::isPresent)
          .map(Optional::get)
          .collect(Collectors.toSet());

      Map<UUID, SignalDetectionHypothesisDao> parentSignalDetectionHypotheses = new HashMap<>();
      if (!parentSdhIds.isEmpty()) {
        CriteriaQuery<SignalDetectionHypothesisDao> parentHypothesesQuery =
            builder.createQuery(SignalDetectionHypothesisDao.class);
        Root<SignalDetectionHypothesisDao> fromSdh =
            parentHypothesesQuery.from(SignalDetectionHypothesisDao.class);
        parentHypothesesQuery.select(fromSdh)
            .where(fromSdh.get("parentSignalDetectionHypothesis").get("id").in(parentSdhIds));

        entityManager.createQuery(parentHypothesesQuery)
            .getResultStream()
            .forEach(sdh -> parentSignalDetectionHypotheses.put(sdh.getId(), sdh));
      }

      Set<String> featureMeasurementIds = signalDetections.stream()
          .flatMap(sd -> sd.getSignalDetectionHypotheses().stream())
          .flatMap(sdh -> sdh.getFeatureMeasurements().stream())
          .map(fm -> FeatureMeasurementDaoUtility.buildId(fm))
          .collect(Collectors.toSet());

      Map<String, FeatureMeasurementDao> featureMeasurementsById = new HashMap<>();
      if (!featureMeasurementIds.isEmpty()) {
        CriteriaQuery<FeatureMeasurementDao> fmQuery =
            builder.createQuery(FeatureMeasurementDao.class);
        Root<FeatureMeasurementDao> fromFm = fmQuery.from(FeatureMeasurementDao.class);
        fmQuery.select(fromFm).where(fromFm.get("id").in(featureMeasurementIds));
        entityManager.createQuery(fmQuery)
            .getResultStream()
            .forEach(fm -> featureMeasurementsById.put(fm.getId(), fm));
      }

      Map<UUID, MeasuredChannelSegmentDescriptor> descriptorsById = signalDetections.stream()
          .flatMap(sd -> sd.getSignalDetectionHypotheses().stream())
          .flatMap(sdh -> sdh.getFeatureMeasurements().stream())
          .map(FeatureMeasurement::getMeasuredChannelSegmentDescriptor)
          .distinct()
          .collect(Collectors.toMap(MeasuredChannelSegmentDescriptorDaoUtility::buildId,
              Functions.identity()));

      Set<String> channelNames = descriptorsById.values().stream()
          .map(MeasuredChannelSegmentDescriptor::getChannelName)
          .collect(Collectors.toSet());

      CriteriaQuery<ChannelDao> channelQuery = builder.createQuery(ChannelDao.class);
      Root<ChannelDao> fromChannel = channelQuery.from(ChannelDao.class);
      channelQuery.select(fromChannel)
          .where(fromChannel.get("name").in(channelNames));

      Map<String, ChannelDao> channelsByName = entityManager.createQuery(channelQuery)
          .getResultStream()
          .collect(Collectors.toMap(ChannelDao::getName, Functions.identity()));

      if (channelsByName.size() != channelNames.size()) {
        final Set<String> unknownNames = new HashSet<>(channelNames);
        unknownNames.removeAll(channelsByName.keySet());
        throw new IllegalStateException("Cannot insert signal detection with unknown channel(s): "
            + unknownNames);
      }

      CriteriaQuery<MeasuredChannelSegmentDescriptorDao> descriptorQuery =
          builder.createQuery(MeasuredChannelSegmentDescriptorDao.class);
      Root<MeasuredChannelSegmentDescriptorDao> fromMcsd =
          descriptorQuery.from(MeasuredChannelSegmentDescriptorDao.class);
      descriptorQuery.select(fromMcsd)
          .where(fromMcsd.get("id").in(descriptorsById.keySet()));

      Map<UUID, MeasuredChannelSegmentDescriptorDao> descriptorDaosById =
          entityManager.createQuery(descriptorQuery)
              .getResultStream()
              .collect(Collectors.toMap(MeasuredChannelSegmentDescriptorDao::getId,
                  Functions.identity()));

      entityManager.getTransaction().begin();
      for (StationDao station : stations) {
        List<SignalDetection> signalDetectionsForStation =
            signalDetectionsByStation.get(station.getName());
        for (SignalDetection detection : signalDetectionsForStation) {
          SignalDetectionDao detectionDao;
          if (signalDetectionsById.containsKey(detection.getId())) {
            detectionDao = signalDetectionsById.get(detection.getId());
          } else {
            detectionDao = SignalDetectionDaoConverter.toDao(detection, station);
            entityManager.persist(detectionDao);
          }

          for (SignalDetectionHypothesis hypothesis : detection.getSignalDetectionHypotheses()) {
            SignalDetectionHypothesisDao parent = null;
            if (entityManager.find(SignalDetectionHypothesisDao.class, hypothesis.getId()) == null) {
              // New hypothesis - store it
              if (hypothesis.getParentSignalDetectionHypothesisId().isPresent()) {
                parent = parentSignalDetectionHypotheses
                    .get(hypothesis.getParentSignalDetectionHypothesisId().get());

                if (parent == null) {
                  parent = entityManager.find(SignalDetectionHypothesisDao.class,
                      hypothesis.getParentSignalDetectionHypothesisId().get());

                  if (parent == null) {
                    throw new IllegalStateException("Cannot find parent hypothesis with id: " +
                        hypothesis.getParentSignalDetectionHypothesisId());
                  }
                }
              }

              SignalDetectionHypothesisDao hypothesisDao =
                  SignalDetectionHypothesisDaoUtility.toDao(detectionDao, parent, hypothesis,
                      entityManager);
              entityManager.merge(hypothesisDao);
              parentSignalDetectionHypotheses.put(hypothesisDao.getId(), hypothesisDao);
            }
          }
        }
      }

      entityManager.getTransaction().commit();
    } catch (RollbackException ex) {
      logger.error("Error storing signal detections", ex);
      entityManager.getTransaction().rollback();
      throw ex;
    } finally {
      entityManager.close();
    }
  }

  @Override
  public List<SignalDetection> findSignalDetectionsByIds(Collection<UUID> ids) {
    Objects.requireNonNull(ids);

    EntityManager entityManager = entityManagerFactory.createEntityManager();
    try {
      CriteriaBuilder builder = entityManager.getCriteriaBuilder();
      CriteriaQuery<SignalDetectionHypothesisDao> query =
          builder.createQuery(SignalDetectionHypothesisDao.class);
      Root<SignalDetectionHypothesisDao> fromSdh = query.from(SignalDetectionHypothesisDao.class);
      query.select(fromSdh)
          .where(fromSdh.join("parentSignalDetection").get("id").in(ids));
      Map<SignalDetectionDao, List<SignalDetectionHypothesisDao>> sdhsBySd = entityManager
          .createQuery(query)
          .getResultStream()
          .collect(Collectors.groupingBy(SignalDetectionHypothesisDao::getParentSignalDetection));
      return buildSignalDetections(sdhsBySd, entityManager);
    } finally {
      entityManager.close();
    }
  }

  @Override
  public List<SignalDetection> findSignalDetectionsByStationAndTime(StationsTimeRangeRequest request) {
    Objects.requireNonNull(request);

    EntityManager entityManager = entityManagerFactory.createEntityManager();

    try {
      CriteriaBuilder builder = entityManager.getCriteriaBuilder();

      CriteriaQuery<SignalDetectionHypothesisDao> sdhQuery =
          builder.createQuery(SignalDetectionHypothesisDao.class);
      Root<SignalDetectionHypothesisDao> fromSdh =
          sdhQuery.from(SignalDetectionHypothesisDao.class);
      Join<SignalDetectionHypothesisDao, InstantFeatureMeasurementDao> arrivalMeasurementJoin =
          fromSdh.join("arrivalTimeMeasurement");
      sdhQuery.select(fromSdh)
          .where(builder.and(fromSdh.get("stationName").in(request.getStationNames()),
              builder.greaterThanOrEqualTo(arrivalMeasurementJoin.get("value").get("time"),
                  request.getTimeRange().getStartTime()),
              builder.lessThan(arrivalMeasurementJoin.get("value").get("time"),
                  request.getTimeRange().getEndTime())));

      Map<SignalDetectionDao, List<SignalDetectionHypothesisDao>> sdhsBySd =
          entityManager.createQuery(sdhQuery)
              .getResultStream()
              .collect(Collectors.groupingBy(SignalDetectionHypothesisDao::getParentSignalDetection));
      return buildSignalDetections(sdhsBySd, entityManager);
    } finally {
      entityManager.close();
    }
  }

  @Override
  public void storeSignalDetectionHypotheses(Collection<SignalDetectionHypothesis> signalDetectionHypotheses) {
    Objects.requireNonNull(signalDetectionHypotheses);

    EntityManager entityManager = entityManagerFactory.createEntityManager();
    try {
      entityManager.getTransaction().begin();
      Set<UUID> signalDetectionIds = signalDetectionHypotheses.stream()
          .map(SignalDetectionHypothesis::getParentSignalDetectionId)
          .collect(Collectors.toSet());
      Map<UUID, SignalDetectionDao> signalDetectionsById = signalDetectionIds.stream()
          .map(id -> entityManager.find(SignalDetectionDao.class, id))
          .filter(Predicates.notNull())
          .collect(Collectors.toMap(SignalDetectionDao::getId, Functions.identity()));

      Preconditions.checkState(signalDetectionsById.keySet().containsAll(signalDetectionIds),
          "Cannot store signal detection hypothesis for a signal detection that does not exist");

      signalDetectionHypotheses.stream()
          .map(sdh -> SignalDetectionHypothesisDaoUtility.fromCoi(sdh, entityManager))
          .forEach(entityManager::persist);
      entityManager.getTransaction().commit();
    } catch (RollbackException ex) {
      logger.error("Error storing signal detection hypotheses", ex);
      entityManager.getTransaction().rollback();
    } finally {
      entityManager.close();
    }
  }

  private List<SignalDetection> buildSignalDetections(
      Map<SignalDetectionDao, List<SignalDetectionHypothesisDao>> hypothesisByParent,
      EntityManager entityManager) {
    ChannelUtils channelUtils = new ChannelUtils(entityManager);
    Map<ChannelDao, Channel> channelsByName = new HashMap<>();
    List<SignalDetection> signalDetections = new ArrayList<>();
    try {
      for (Map.Entry<SignalDetectionDao, List<SignalDetectionHypothesisDao>> entry :
          hypothesisByParent.entrySet()) {
        SignalDetectionDao signalDetection = entry.getKey();
        List<SignalDetectionHypothesis> hypotheses = new ArrayList<>();
        for (SignalDetectionHypothesisDao hypothesisDao : entry.getValue()) {
          List<FeatureMeasurement<?>> featureMeasurements = new ArrayList<>();
          // TODO: Extract to it's own method
          // TODO: is there a way to batch all featuremeasurements to reduce calls/catch duplicates?
          InstantFeatureMeasurementDao arrival = hypothesisDao.getArrivalTimeMeasurement();
          Channel channel;
          if (channelsByName.containsKey(arrival.getMeasuredChannelSegmentDescriptor().getChannel())) {
            channel =
                channelsByName.get(arrival.getMeasuredChannelSegmentDescriptor().getChannel());
          } else {
            channel =
                channelUtils.constructChannels(List.of(arrival.getMeasuredChannelSegmentDescriptor().getChannel()),
                    signalDetection.getStation().getName()).get(0);
            channelsByName.put(arrival.getMeasuredChannelSegmentDescriptor().getChannel(), channel);
          }

          featureMeasurements.add(FeatureMeasurementDaoUtility.toCoi(arrival, channel));

          PhaseFeatureMeasurementDao phase = hypothesisDao.getPhaseMeasurement();
          if (channelsByName.containsKey(phase.getMeasuredChannelSegmentDescriptor().getChannel())) {
            channel = channelsByName.get(phase.getMeasuredChannelSegmentDescriptor().getChannel());
          } else {
            channel =
                channelUtils.constructChannels(List.of(phase.getMeasuredChannelSegmentDescriptor().getChannel()),
                    signalDetection.getStation().getName()).get(0);
          }

          featureMeasurements.add(FeatureMeasurementDaoUtility.toCoi(phase, channel));

          for (FeatureMeasurementDao<?> featureMeasurementDao :
              hypothesisDao.getFeatureMeasurements()) {
            if (channelsByName.containsKey(featureMeasurementDao.getMeasuredChannelSegmentDescriptor().getChannel())) {
              channel =
                  channelsByName.get(featureMeasurementDao.getMeasuredChannelSegmentDescriptor().getChannel());
            } else {
              channel =
                  channelUtils.constructChannels(List.of(featureMeasurementDao.getMeasuredChannelSegmentDescriptor().getChannel()),
                      signalDetection.getStation().getName()).get(0);
            }

            featureMeasurements.add(FeatureMeasurementDaoUtility.toCoi(featureMeasurementDao,
                channel));
          }

          hypotheses.add(SignalDetectionHypothesisDaoUtility.fromDao(hypothesisDao,
              featureMeasurements));
        }

        signalDetections.add(SignalDetectionDaoConverter.fromDao(signalDetection, hypotheses));
      }

      return signalDetections;
    } catch (IOException ex) {
      logger.error("Error converting channel", ex);
      throw new UncheckedIOException(ex);
    }
  }
}
