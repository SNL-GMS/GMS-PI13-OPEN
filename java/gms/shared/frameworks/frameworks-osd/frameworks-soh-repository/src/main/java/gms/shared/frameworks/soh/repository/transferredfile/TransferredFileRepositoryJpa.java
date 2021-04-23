package gms.shared.frameworks.soh.repository.transferredfile;

import gms.shared.frameworks.osd.api.transferredfile.TransferredFileRepositoryInterface;
import gms.shared.frameworks.osd.api.util.TimeRangeRequest;
import gms.shared.frameworks.osd.coi.channel.dataacquisitionstatus.TransferredFile;
import gms.shared.frameworks.osd.coi.channel.dataacquisitionstatus.TransferredFileInvoiceMetadata;
import gms.shared.frameworks.osd.coi.channel.dataacquisitionstatus.TransferredFileMetadataType;
import gms.shared.frameworks.osd.coi.channel.dataacquisitionstatus.TransferredFileRawStationDataFrameMetadata;
import gms.shared.frameworks.osd.coi.channel.dataacquisitionstatus.TransferredFileStatus;
import gms.shared.frameworks.osd.dao.transferredfile.TransferredFileDao;
import gms.shared.frameworks.osd.dao.transferredfile.TransferredFileInvoiceDao;
import gms.shared.frameworks.osd.dao.transferredfile.TransferredFileInvoiceMetadataDao;
import gms.shared.frameworks.osd.dao.transferredfile.TransferredFileRawStationDataFrameDao;
import gms.shared.frameworks.osd.dao.transferredfile.TransferredFileRawStationDataFrameMetadataDao;
import gms.shared.metrics.CustomMetric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.NoResultException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * TransferredFileRepository provides permanent, persistent storage of TransferredFiles with status
 * of either SENT or RECEIVED (i.e. those TransferredFiles appearing in a TransferredFileInvoice
 * where the corresponding data file has not been received; those TransferredFiles referencing
 * received data files that have not appeared in a TransferredFileInvoice).  TransferredFiles with
 * status of SENT are used to populate the "Gap List" display.  TransferredFiles with status of
 * SENT_AND_RECEIVED are regularly purged from the TransferredFileRepository.
 * TransferredFileRepository needs to be accessible from multiple applications (i.e. one or more
 * TransferAuditorUtility instances operating in data acquisition sequences and
 * TransferredFileRepositoryService).
 */

/**
 * Defines a class to provide persistence methods for storing, removing, and retrieving {@link
 * TransferredFile} to/from the relational database.
 */
public class TransferredFileRepositoryJpa implements TransferredFileRepositoryInterface {

  public static final String SELECT_T_FROM = "SELECT t FROM ";
  //Set UNKNOWN values to avoid null assignments.
  private final String UNKNOWN_PRIORITY = "UNKNOWN";
  private final Instant UNKNOWN_RECEPTION_TIME = Instant.MIN;
  private final Instant UNKNOWN_TRANSFER_TIME = Instant.MIN;


  private static final Logger logger = LoggerFactory.getLogger(TransferredFileRepositoryJpa.class);

  private static final CustomMetric<TransferredFileRepositoryJpa, Long> transferredFileRepositoryJpaRetrieveAllTransferredFiles =
          CustomMetric.create(CustomMetric::incrementer, "transferredFileRepositoryJpaRetrieveAllTransferredFiles_hits:type=Counter", 0L);

  private static final CustomMetric<TransferredFileRepositoryJpa, Long> transferredFileRepositoryJpaRetrieveByTransferTime =
          CustomMetric.create(CustomMetric::incrementer, "transferredFileRepositoryJpaRetrieveByTransferTime_hits:type=Counter", 0L);

  private static final CustomMetric<TransferredFileRepositoryJpa, Long> transferredFileRepositoryJpaRemoveSentAndReceived =
          CustomMetric.create(CustomMetric::incrementer, "transferredFileRepositoryJpaRemoveSentAndReceived_hits:type=Counter", 0L);

  private static final CustomMetric<TransferredFileRepositoryJpa, Long> transferredFileRepositoryJpaStoreTransferredFiles =
          CustomMetric.create(CustomMetric::incrementer, "transferredFileRepositoryJpaStoreTransferredFiles_hits:type=Counter", 0L);


  private static final CustomMetric<Long, Long> transferredFileRepositoryJpaRetrieveAllTransferredFilesDuration =
          CustomMetric.create(CustomMetric::updateTimingData, "transferredFileRepositoryJpaRetrieveAllTransferredFiles_duration:type=Value", 0L);

  private static final CustomMetric<Long, Long> transferredFileRepositoryJpaRetrieveByTransferTimeDuration =
          CustomMetric.create(CustomMetric::updateTimingData, "transferredFileRepositoryJpaRetrieveByTransferTime_duration:type=Value", 0L);

  private static final CustomMetric<Long, Long> transferredFileRepositoryJpaRemoveSentAndReceivedDuration =
          CustomMetric.create(CustomMetric::updateTimingData, "transferredFileRepositoryJpaRemoveSentAndReceived_duration:type=Value", 0L);

  private static final CustomMetric<Long, Long> transferredFileRepositoryJpaStoreTransferredFilesDuration =
          CustomMetric.create(CustomMetric::updateTimingData, "transferredFileRepositoryJpaStoreTransferredFiles_duration:type=Value", 0L);

  private final EntityManagerFactory entityManagerFactory;

  /**
   * Constructor taking in the EntityManagerFactory
   *
   * @param entityManagerFactory {@link EntityManagerFactory}
   */
  public TransferredFileRepositoryJpa(EntityManagerFactory entityManagerFactory) {
    Objects.requireNonNull(entityManagerFactory,
        "Cannot instantiate TransferredFileRepositoryJpa with null EntityManager");
    this.entityManagerFactory = entityManagerFactory;
  }

  /**
   * Retrieve all TransferredFiles currently stored in the database.
   *
   * @return a List of TransferredFile
   */
  @Override
  public List<TransferredFile> retrieveAllTransferredFiles(
      Collection<String> fileNames) {
    EntityManager entityManager = entityManagerFactory.createEntityManager();

    transferredFileRepositoryJpaRetrieveAllTransferredFiles.updateMetric(this);
    Instant start = Instant.now();

    try {
      return entityManager
          .createQuery("SELECT t FROM TransferredFileDao AS t ORDER BY transferTime ASC",
              TransferredFileDao.class)
          .getResultStream()
          .map(TransferredFileDao::toCoi)
          .collect(Collectors.toList());
    } finally {
      entityManager.close();

      Instant finish = Instant.now();
      long timeElapsed = Duration.between(start, finish).toMillis();
      transferredFileRepositoryJpaRetrieveAllTransferredFilesDuration.updateMetric(timeElapsed);
    }
  }

  /**
   * Retrieve all TransferredFiles within a time range currently stored in the database.
   *
   * @return a List of TransferredFile
   */
  @Override
  public List<TransferredFile> retrieveByTransferTime(TimeRangeRequest request) {
    EntityManager entityManager = entityManagerFactory.createEntityManager();

    transferredFileRepositoryJpaRetrieveByTransferTime.updateMetric(this);
    Instant start = Instant.now();

    try {
      return entityManager.createQuery(
          SELECT_T_FROM + TransferredFileDao.class.getSimpleName()
              + " t WHERE t.transferTime >= :startTime AND t.transferTime <= :endTime",
          TransferredFileDao.class)
          .setParameter("startTime", request.getStartTime())
          .setParameter("endTime", request.getEndTime())
          .getResultStream()
          .map(TransferredFileDao::toCoi)
          .collect(Collectors.toList());
    } finally {
      entityManager.close();

      Instant finish = Instant.now();
      long timeElapsed = Duration.between(start, finish).toMillis();
      transferredFileRepositoryJpaRetrieveByTransferTimeDuration.updateMetric(timeElapsed);
    }
  }

  @Override
  public <T extends TransferredFile> Optional<TransferredFile> find(T file) {
    Objects.requireNonNull(file, "Cannot find from null file");
    return findDao(file).map(TransferredFileDao::toCoi);
  }

  /**
   * Finds the DAO for the given transferred file.  If it's not present in storage, empty is
   * returned.  This method only queries by comparing the 'metadata' attribute.
   *
   * @param tf the file to search for
   * @return the DAO or empty if not found
   * @throws IllegalArgumentException invalid TransferredFile type
   * @throws javax.persistence.PersistenceException database persistence errors
   */
  private Optional<TransferredFileDao> findDao(TransferredFile<?> tf) {
    final TransferredFileMetadataType type = tf.getMetadataType();
    final Class<? extends TransferredFileDao> daoClass;
    final Object metadataDao;
    final EntityManager entityManager = entityManagerFactory.createEntityManager();

    if (type.equals(TransferredFileMetadataType.RAW_STATION_DATA_FRAME)) {
      daoClass = TransferredFileRawStationDataFrameDao.class;
      metadataDao = new TransferredFileRawStationDataFrameMetadataDao(
          (TransferredFileRawStationDataFrameMetadata) tf.getMetadata());
    } else if (type.equals(TransferredFileMetadataType.TRANSFERRED_FILE_INVOICE)) {
      daoClass = TransferredFileInvoiceDao.class;
      metadataDao = new TransferredFileInvoiceMetadataDao(
          (TransferredFileInvoiceMetadata) tf.getMetadata());
    } else {
      throw new IllegalArgumentException("Unknown/unsupported metadata type " + type);
    }

    try {
      return Optional.of(entityManager.createQuery(
          SELECT_T_FROM + daoClass.getSimpleName()
              + " t WHERE t.metadata = :metadata", daoClass)
          .setParameter("metadata", metadataDao)
          .getSingleResult());
    } catch (NoResultException e) {
      return Optional.empty();
    } finally {
      entityManager.close();
    }
  }

  /**
   * Remove all TransferredFiles older than the number of seconds specified by olderThan that have
   * status SENT_AND_RECEIVED
   */
  @Override
  public void removeSentAndReceived(Duration olderThan) {
    Objects.requireNonNull(olderThan,
        "Cannot removeSentAndReceived files when a null duration is provided");
    EntityManager entityManager = entityManagerFactory.createEntityManager();
    if (olderThan.isNegative()) {
      throw new IllegalArgumentException("olderThan must be a positive duration");
    }
    Instant endTime = Instant.now().minus(olderThan);

    transferredFileRepositoryJpaRemoveSentAndReceived.updateMetric(this);
    Instant start = Instant.now();

    try {
      entityManager.getTransaction().begin();
      final List<TransferredFileDao> filesToDelete = entityManager.createQuery(
          SELECT_T_FROM + TransferredFileDao.class.getSimpleName()
              + " t WHERE t.transferTime < :endTime AND t.status = :status",
          TransferredFileDao.class)
          .setParameter("endTime", endTime)
          .setParameter("status", TransferredFileStatus.SENT_AND_RECEIVED)
          .getResultList();
      filesToDelete.forEach(entityManager::remove);
      entityManager.getTransaction().commit();
    } catch (Exception ex) {
      logger.error("Error removing TransferredFiles in time range", ex);
      entityManager.getTransaction().rollback();
      throw ex;
    } finally {
      entityManager.close();

      Instant finish = Instant.now();
      long timeElapsed = Duration.between(start, finish).toMillis();
      transferredFileRepositoryJpaRemoveSentAndReceivedDuration.updateMetric(timeElapsed);
    }
  }

  /**
   * Store TransferredFiles to the database or update their fields.
   */
  @Override
  public List<String> storeTransferredFiles(Collection<TransferredFile<?>> transferredFiles) {
    List<String> result = new ArrayList<>();
    Objects.requireNonNull(transferredFiles, "Cannot store null Transferred Files");
    EntityManager entityManager = entityManagerFactory.createEntityManager();

    transferredFileRepositoryJpaStoreTransferredFiles.updateMetric(this);
    Instant start = Instant.now();

    try {
      entityManager.getTransaction().begin();
      for (TransferredFile<?> t : transferredFiles) {
        final TransferredFileDao dao;
        final Optional<TransferredFileDao> existing = findDao(t);
        // if present, update existing and store.
        if (existing.isPresent()) {
          dao = existing.get();
          updateDao(dao, t);
          entityManager.merge(dao);
        } else { // not present, store anew.
          dao = makeDao(t);
        }
        entityManager.merge(dao);
        result.add(dao.getFilename());
      }
      entityManager.getTransaction().commit();
    } catch (Exception ex) {
      logger.error("Exception trying to store TransferredFile", ex);
      entityManager.getTransaction().rollback();
      result.clear();
    } finally {
      entityManager.close();

      Instant finish = Instant.now();
      long timeElapsed = Duration.between(start, finish).toMillis();
      transferredFileRepositoryJpaStoreTransferredFilesDuration.updateMetric(timeElapsed);
    }
    return result;
  }

  /**
   * Helper method for store that updates fields of TransferredFileDao
   */
  private <DaoClass extends TransferredFileDao> void updateDao(DaoClass dao,
      TransferredFile<?> tf) {
    if (dao != null && tf != null) {
      dao.setFilename(tf.getFileName());
      if (tf.getPriority() != null) {
        dao.setPriority(tf.getPriority().orElse(UNKNOWN_PRIORITY));
      }
      dao.setStatus(tf.getStatus());
      if (tf != null && tf.getReceptionTime() != null) {
        dao.setReceptionTime(tf.getReceptionTime().orElse(UNKNOWN_RECEPTION_TIME));
      }
      if (tf.getTransferTime() != null) {
        dao.setTransferTime(tf.getTransferTime().orElse(UNKNOWN_TRANSFER_TIME));
      }
    }
  }

  @SuppressWarnings("unchecked")
  private TransferredFileDao makeDao(TransferredFile<?> tf) {
    final TransferredFileMetadataType type = tf.getMetadataType();
    if (type.equals(TransferredFileMetadataType.RAW_STATION_DATA_FRAME)) {
      return new TransferredFileRawStationDataFrameDao(
          (TransferredFile<TransferredFileRawStationDataFrameMetadata>) tf);
    } else if (type.equals(TransferredFileMetadataType.TRANSFERRED_FILE_INVOICE)) {
      return new TransferredFileInvoiceDao((TransferredFile<TransferredFileInvoiceMetadata>) tf);
    } else {
      throw new RuntimeException("Unknown/unsupported metadata type " + type);
    }
  }
}

