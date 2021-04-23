package gms.shared.frameworks.osd.control.signaldetection;

import gms.shared.frameworks.osd.api.signaldetection.QcMaskRepositoryInterface;
import gms.shared.frameworks.osd.api.util.ChannelTimeRangeRequest;
import gms.shared.frameworks.osd.api.util.ChannelsTimeRangeRequest;
import gms.shared.frameworks.osd.api.util.TimeRangeRequest;
import gms.shared.frameworks.osd.coi.channel.repository.jpa.dataaccessobjects.ChannelDao;
import gms.shared.frameworks.osd.coi.signaldetection.QcMask;
import gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.dataaccessobjects.QcMaskDao;
import gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.dataaccessobjects.QcMaskVersionDao;
import gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.util.QcMaskDaoConverter;
import gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.util.QcMaskVersionDaoConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

public class QcMaskRepositoryJpa implements QcMaskRepositoryInterface {

  private static final Logger logger = LoggerFactory.getLogger(QcMaskRepositoryJpa.class);
  public static final String VERSION = "version";
  public static final String OWNER_QC_MASK = "ownerQcMask";

  private final EntityManagerFactory entityManagerFactory;

  public QcMaskRepositoryJpa(EntityManagerFactory entityManagerFactory) {
    this.entityManagerFactory = entityManagerFactory;
  }

  @Override
  public void storeQcMasks(Collection<QcMask> qcMasks) {
    Objects.requireNonNull(qcMasks);
    // TODO: what is the proper handling for empty lists?  Some places have errors, others accept
    EntityManager entityManager = entityManagerFactory.createEntityManager();
    entityManager.getTransaction().begin();
    try {
      for (QcMask qcMask : qcMasks) {
        // special case of an update route
        QcMaskDao qcMaskDao = entityManager.find(QcMaskDao.class, qcMask.getId());
        if (qcMaskDao == null) {
          // brand new
          ChannelDao channelDao = entityManager.find(ChannelDao.class, qcMask.getChannelName());
          if (channelDao == null) {
            throw new IllegalStateException("Could not find Channel associated with QcMask");
          }
          final QcMaskDao converted = QcMaskDaoConverter.toDao(qcMask, channelDao);
          entityManager.merge(converted);
          // merging due to the reference to the newly persisted QcMask.  Or we could make
          // QcMask the owner of the relationship with QcMaskVersion
          qcMask.getQcMaskVersions().stream()
              .map(qcMaskVersion -> QcMaskVersionDaoConverter.toDao(converted, qcMaskVersion))
              .forEach(entityManager::merge);
        } else {
          // updates - just add the new qc mask versions
          // get the existing qc masks
          CriteriaBuilder builder = entityManager.getCriteriaBuilder();
          CriteriaQuery<Long> qcMaskVersionsQuery = builder.createQuery(Long.class);
          Root<QcMaskVersionDao> fromQcMaskVersion =
              qcMaskVersionsQuery.from(QcMaskVersionDao.class);
          qcMaskVersionsQuery.select(fromQcMaskVersion.get(VERSION));

          Join<QcMaskVersionDao, QcMaskDao> ownerQcMaskJoin = fromQcMaskVersion.join(OWNER_QC_MASK);
          Expression<UUID> eqOwnerQcMaskId = ownerQcMaskJoin.get("id");
          qcMaskVersionsQuery.where(builder.equal(eqOwnerQcMaskId, qcMaskDao.getId()));
          List<Long> existingVersions =
              entityManager.createQuery(qcMaskVersionsQuery).getResultList();

          // We merge here again because we have a relationship to an existing object
          qcMask.getQcMaskVersions().stream()
              .filter(qcMaskVersion -> existingVersions.contains(qcMaskVersion.getVersion()))
              .map(qcMaskVersion -> QcMaskVersionDaoConverter.toDao(qcMaskDao, qcMaskVersion))
              .forEach(entityManager::merge);
        }
      }

      entityManager.getTransaction().commit();
    } catch (Exception ex) {
      logger.error("Exception storing QcMasks", ex);
      entityManager.getTransaction().rollback();
      throw ex;
    } finally {
      entityManager.close();
    }
  }

  @Override
  public Collection<QcMask> findCurrentQcMasksByChannelIdAndTimeRange(ChannelTimeRangeRequest request) {
    Objects.requireNonNull(request);
    EntityManager entityManager = entityManagerFactory.createEntityManager();
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    CriteriaQuery<QcMaskVersionDao> qcMaskVersionQuery =
        builder.createQuery(QcMaskVersionDao.class);
    Root<QcMaskVersionDao> fromQcMaskVersion = qcMaskVersionQuery.from(QcMaskVersionDao.class);
    qcMaskVersionQuery.select(fromQcMaskVersion);

    List<Predicate> conjunctions = new ArrayList<>();

    // select by channel name
    Join<QcMaskVersionDao, QcMaskDao> ownerQcMaskJoin = fromQcMaskVersion.join(OWNER_QC_MASK);
    Join<QcMaskDao, ChannelDao> channelJoin = ownerQcMaskJoin.join("channel");
    Expression<String> channelName = channelJoin.get("name");
    conjunctions.add(builder.equal(channelName, request.getChannelName()));

    // select max version
    Subquery<Long> maxVersionQuery = qcMaskVersionQuery.subquery(Long.class);
    Root<QcMaskVersionDao> subFromQcMaskVersion = maxVersionQuery.from(QcMaskVersionDao.class);
    Expression<Long> maxVersion = subFromQcMaskVersion.get(VERSION);
    maxVersionQuery.select(builder.max(maxVersion));

    // select sub query by owner qc mask
    Expression<UUID> subVersionOwner = subFromQcMaskVersion.get(OWNER_QC_MASK).get("id");
    Expression<UUID> versionOwner = fromQcMaskVersion.get(OWNER_QC_MASK).get("id");
    maxVersionQuery.where(builder.equal(subVersionOwner, versionOwner));

    // select by time range
    TimeRangeRequest timeRange = request.getTimeRange();
    Expression<Instant> afterStartTime = fromQcMaskVersion.get("startTime");
    conjunctions.add(builder.greaterThanOrEqualTo(afterStartTime,
        request.getTimeRange().getStartTime()));

    Expression<Instant> beforeEndTime = fromQcMaskVersion.get("endTime");
    conjunctions.add(builder.lessThanOrEqualTo(beforeEndTime, request.getTimeRange().getEndTime()));

    qcMaskVersionQuery.where(builder.and(conjunctions.toArray(new Predicate[conjunctions.size()])));
    try {
      return entityManager.createQuery(qcMaskVersionQuery).getResultStream()
          .collect(Collectors.groupingBy(QcMaskVersionDao::getOwnerQcMask))
          .entrySet().stream()
          .map(entry -> QcMaskDaoConverter.fromDao(entry.getKey(), entry.getValue()))
          .collect(Collectors.toList());
    } finally {
      entityManager.close();
    }
  }

  @Override
  public Map<String, List<QcMask>> findCurrentQcMasksByChannelNamesAndTimeRange(ChannelsTimeRangeRequest request) {
    Objects.requireNonNull(request);
    EntityManager entityManager = entityManagerFactory.createEntityManager();
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    CriteriaQuery<QcMaskVersionDao> qcMaskVersionQuery =
        builder.createQuery(QcMaskVersionDao.class);
    Root<QcMaskVersionDao> fromQcMaskVersion = qcMaskVersionQuery.from(QcMaskVersionDao.class);
    qcMaskVersionQuery.select(fromQcMaskVersion);

    List<Predicate> conjunctions = new ArrayList<>();

    // select by channel name
    Join<QcMaskVersionDao, QcMaskDao> ownerQcMaskJoin = fromQcMaskVersion.join(OWNER_QC_MASK);
    Join<QcMaskDao, ChannelDao> channelJoin = ownerQcMaskJoin.join("channel");
    Expression<String> channelName = channelJoin.get("name");
    conjunctions.add(channelName.in(request.getChannelNames()));

    // select max version
    Subquery<Long> maxVersionQuery = qcMaskVersionQuery.subquery(Long.class);
    Root<QcMaskVersionDao> subFromQcMaskVersion = maxVersionQuery.from(QcMaskVersionDao.class);
    Expression<Long> maxVersion = subFromQcMaskVersion.get(VERSION);
    maxVersionQuery.select(builder.max(maxVersion));

    // select sub query by owner qc mask
    Expression<UUID> subVersionOwner = subFromQcMaskVersion.get(OWNER_QC_MASK).get("id");
    Expression<UUID> versionOwner = fromQcMaskVersion.get(OWNER_QC_MASK).get("id");
    maxVersionQuery.where(builder.equal(subVersionOwner, versionOwner));

    // select by time range
    Expression<Instant> afterStartTime = fromQcMaskVersion.get("startTime");
    conjunctions.add(builder.greaterThanOrEqualTo(afterStartTime,
        request.getTimeRange().getStartTime()));

    Expression<Instant> beforeEndTime = fromQcMaskVersion.get("endTime");
    conjunctions.add(builder.lessThanOrEqualTo(beforeEndTime, request.getTimeRange().getEndTime()));

    qcMaskVersionQuery.where(builder.and(conjunctions.toArray(new Predicate[conjunctions.size()])));

    try {
      return entityManager.createQuery(qcMaskVersionQuery).getResultStream()
          .collect(Collectors.groupingBy(QcMaskVersionDao::getOwnerQcMask))
          .entrySet().stream()
          .map(entry -> QcMaskDaoConverter.fromDao(entry.getKey(), entry.getValue()))
          .collect(Collectors.groupingBy(QcMask::getChannelName));
    } finally {
      entityManager.close();
    }
  }
}
