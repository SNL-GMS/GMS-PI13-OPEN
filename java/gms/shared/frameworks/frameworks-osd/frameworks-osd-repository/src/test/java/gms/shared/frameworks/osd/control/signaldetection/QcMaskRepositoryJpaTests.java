package gms.shared.frameworks.osd.control.signaldetection;

import gms.shared.frameworks.osd.api.channel.ChannelRepositoryInterface;
import gms.shared.frameworks.osd.api.util.ChannelsTimeRangeRequest;
import gms.shared.frameworks.osd.coi.CoiTestingEntityManagerFactory;
import gms.shared.frameworks.osd.coi.channel.repository.jpa.dataaccessobjects.ChannelDao;
import gms.shared.frameworks.osd.coi.signaldetection.QcMask;
import gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.dataaccessobjects.QcMaskVersionDao;
import gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.util.QcMaskDaoConverter;
import gms.shared.frameworks.osd.control.channel.ChannelRepositoryJpa;
import gms.shared.frameworks.osd.control.station.StationRepositoryJpa;
import gms.shared.frameworks.osd.control.utils.TestFixtures;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.time.Instant;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QcMaskRepositoryJpaTests {

  private EntityManagerFactory entityManagerFactory;
  private QcMaskRepositoryJpa qcMaskRepository;

  @BeforeEach
  void setUp() {
    entityManagerFactory = CoiTestingEntityManagerFactory.createTesting();
    EntityManager entityManager = entityManagerFactory.createEntityManager();

    StationRepositoryJpa stationRepositoryJpa = new StationRepositoryJpa(entityManagerFactory);
    ChannelRepositoryInterface channelRepository = new ChannelRepositoryJpa(entityManagerFactory);

    qcMaskRepository = new QcMaskRepositoryJpa(entityManagerFactory);
    entityManager.getTransaction().begin();
    entityManager.persist(ChannelDao.from(TestFixtures.channel1));
    entityManager.persist(ChannelDao.from(TestFixtures.channel2));
    entityManager.getTransaction().commit();
    entityManager.close();
  }

  @AfterEach
  void tearDown() {
    entityManagerFactory.close();
  }

  @Test
  void storeQcMaskNoChannel() {
    assertThrows(IllegalStateException.class,
        () -> qcMaskRepository.storeQcMasks(List.of(
            TestFixtures.createQcMask(Instant.EPOCH, Instant.EPOCH.plusSeconds(1), 1,
                TestFixtures.channel3))));
  }

  @Test
  void storeQcMasks() {
    List<QcMask> qcMasks = List.of(
        TestFixtures.createQcMask(Instant.EPOCH, Instant.EPOCH.plusSeconds(5), 1,
            TestFixtures.channel1),
        TestFixtures.createQcMask(Instant.EPOCH.plusSeconds(5), Instant.EPOCH.plusSeconds(7), 2,
            TestFixtures.channel2));

    assertDoesNotThrow(() -> qcMaskRepository.storeQcMasks(qcMasks));

    EntityManager entityManager = entityManagerFactory.createEntityManager();
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    CriteriaQuery<QcMaskVersionDao> qcMaskVersionQuery =
        builder.createQuery(QcMaskVersionDao.class);
    Root<QcMaskVersionDao> fromQcMaskVersion = qcMaskVersionQuery.from(QcMaskVersionDao.class);
    qcMaskVersionQuery.select(fromQcMaskVersion);
    List<QcMask> actualMasks = entityManager.createQuery(qcMaskVersionQuery).getResultStream()
        .collect(Collectors.groupingBy(qcMaskVersionDao -> qcMaskVersionDao.getOwnerQcMask()))
        .entrySet().stream()
        .map(entry -> QcMaskDaoConverter.fromDao(entry.getKey(), entry.getValue()))
        .collect(Collectors.toList());

    assertEquals(qcMasks.size(), actualMasks.size());
    assertTrue(qcMasks.containsAll(actualMasks));
    entityManager.close();
  }

  @Test
  void findCurrentByChannelNameAndTimeRange() {
    List<QcMask> qcMasks = List.of(TestFixtures.createQcMask(Instant.EPOCH,
        Instant.EPOCH.plusSeconds(5), 2, TestFixtures.channel1),
        TestFixtures.createQcMask(Instant.EPOCH, Instant.EPOCH.plusSeconds(6), 1,
            TestFixtures.channel2),
        TestFixtures.createQcMask(Instant.EPOCH, Instant.EPOCH.plusSeconds(6), 1,
            TestFixtures.channel1),
        TestFixtures.createQcMask(Instant.EPOCH.plusSeconds(10), Instant.EPOCH.plusSeconds(11), 3
            , TestFixtures.channel1));

    qcMaskRepository.storeQcMasks(qcMasks);

    ChannelsTimeRangeRequest request = ChannelsTimeRangeRequest.create(
        List.of(TestFixtures.channel1.getName(), TestFixtures.channel2.getName()),
        Instant.EPOCH,
        Instant.EPOCH.plusSeconds(5));

    Map<String, List<QcMask>> actualQcMasks = qcMaskRepository.findCurrentQcMasksByChannelNamesAndTimeRange(request);
    assertEquals(1, actualQcMasks.size());

    assertTrue(Collections.binarySearch(actualQcMasks.get(qcMasks.get(0).getChannelName()), qcMasks.get(0), Comparator.comparing(QcMask::getId)) >= 0);
  }

  @Test
  void findCurrentByChannelNamesAndTimeRange() {
    List<QcMask> qcMasks = List.of(TestFixtures.createQcMask(Instant.EPOCH,
        Instant.EPOCH.plusSeconds(5), 2, TestFixtures.channel1),
        TestFixtures.createQcMask(Instant.EPOCH, Instant.EPOCH.plusSeconds(4), 2,
            TestFixtures.channel2),
        TestFixtures.createQcMask(Instant.EPOCH, Instant.EPOCH.plusSeconds(6), 1,
            TestFixtures.channel2),
        TestFixtures.createQcMask(Instant.EPOCH, Instant.EPOCH.plusSeconds(6), 1,
            TestFixtures.channel1),
        TestFixtures.createQcMask(Instant.EPOCH.plusSeconds(10), Instant.EPOCH.plusSeconds(11), 3
            , TestFixtures.channel1));

    qcMaskRepository.storeQcMasks(qcMasks);

    ChannelsTimeRangeRequest request = ChannelsTimeRangeRequest.create(
        List.of(TestFixtures.channel1.getName(), TestFixtures.channel2.getName()),
        Instant.EPOCH,
        Instant.EPOCH.plusSeconds(5));

    Map<String, List<QcMask>> actualQcMasks = qcMaskRepository.findCurrentQcMasksByChannelNamesAndTimeRange(request);
    assertEquals(2, actualQcMasks.size());

    int firstIndex = Collections.binarySearch(actualQcMasks.get(qcMasks.get(0).getChannelName()), qcMasks.get(0), Comparator.comparing(QcMask::getId));
    int secondIndex = Collections.binarySearch(actualQcMasks.get(qcMasks.get(1).getChannelName()), qcMasks.get(1),
        Comparator.comparing(QcMask::getId));
    assertTrue(firstIndex >= 0);
    assertTrue( secondIndex >= 0);
  }

}