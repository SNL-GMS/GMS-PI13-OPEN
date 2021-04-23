package gms.shared.frameworks.osd.control.utils;

import gms.shared.frameworks.osd.coi.CoiTestingEntityManagerFactory;
import gms.shared.frameworks.osd.coi.channel.repository.jpa.dataaccessobjects.ChannelDao;
import gms.shared.frameworks.osd.coi.signaldetection.MeasuredChannelSegmentDescriptor;
import gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.dataaccessobjects.MeasuredChannelSegmentDescriptorDao;
import gms.shared.frameworks.osd.coi.test.utils.UtilsTestFixtures;
import gms.shared.frameworks.osd.control.station.StationRepositoryJpa;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.util.List;
import java.util.stream.Stream;
import static gms.shared.frameworks.osd.coi.test.utils.UtilsTestFixtures.DESCRIPTOR;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class MeasuredChannelSegmentDescriptorDaoUtilityTests {

  private static EntityManagerFactory entityManagerFactory;

  @BeforeAll
  static void setUp() {
    entityManagerFactory = CoiTestingEntityManagerFactory.createTesting();
    new StationRepositoryJpa(entityManagerFactory)
        .storeStations(List.of(UtilsTestFixtures.STATION));
  }

  @AfterAll
  static void tearDown() {
    entityManagerFactory.close();
  }

  @ParameterizedTest
  @MethodSource("getFromCoiArguments")
  void testFromCoiValidation(Class<? extends Exception> expectedException,
      MeasuredChannelSegmentDescriptor descriptor,
      EntityManager entityManager) {

    try {
      assertThrows(expectedException,
          () -> MeasuredChannelSegmentDescriptorDaoUtility.fromCoi(descriptor, entityManager));
    } finally {
      if (entityManager != null) {
        entityManager.close();
      }
    }
  }

  static Stream<Arguments> getFromCoiArguments() {
    return Stream.of(
        arguments(NullPointerException.class, null, entityManagerFactory.createEntityManager()),
        arguments(NullPointerException.class, DESCRIPTOR, null),
        arguments(NullPointerException.class, DESCRIPTOR,
            entityManagerFactory.createEntityManager()));
  }

  @Test
  void testFromCoi() {
    EntityManager entityManager = entityManagerFactory.createEntityManager();

    try {
      entityManager.getTransaction().begin();
      MeasuredChannelSegmentDescriptorDao dao = MeasuredChannelSegmentDescriptorDaoUtility
          .fromCoi(DESCRIPTOR, entityManager);

      assertEquals(DESCRIPTOR.getChannelName(), dao.getChannel().getName());
      assertEquals(DESCRIPTOR.getMeasuredChannelSegmentStartTime(),
          dao.getMeasuredChannelSegmentStartTime());
      assertEquals(DESCRIPTOR.getMeasuredChannelSegmentEndTime(),
          dao.getMeasuredChannelSegmentEndTime());
      assertEquals(DESCRIPTOR.getMeasuredChannelSegmentCreationTime(),
          dao.getMeasuredChannelSegmentCreationTime());
    } finally {
      entityManager.getTransaction().rollback();
      entityManager.close();
    }
  }

  @Test
  void testToCoi() {
    MeasuredChannelSegmentDescriptorDao dao = new MeasuredChannelSegmentDescriptorDao();

    EntityManager entityManager = entityManagerFactory.createEntityManager();
    ChannelDao channelDao = entityManager.find(ChannelDao.class,
        UtilsTestFixtures.CHANNEL.getName());

    dao.setId(MeasuredChannelSegmentDescriptorDaoUtility.buildId(DESCRIPTOR));
    dao.setChannel(channelDao);
    dao.setMeasuredChannelSegmentStartTime(DESCRIPTOR.getMeasuredChannelSegmentStartTime());
    dao.setMeasuredChannelSegmentEndTime(DESCRIPTOR.getMeasuredChannelSegmentEndTime());
    dao.setMeasuredChannelSegmentCreationTime(DESCRIPTOR.getMeasuredChannelSegmentCreationTime());

    MeasuredChannelSegmentDescriptor descriptor = MeasuredChannelSegmentDescriptorDaoUtility
        .toCoi(dao);

    assertEquals(dao.getChannel().getName(), descriptor.getChannelName());
    assertEquals(dao.getMeasuredChannelSegmentStartTime(),
        descriptor.getMeasuredChannelSegmentStartTime());
    assertEquals(dao.getMeasuredChannelSegmentEndTime(),
        descriptor.getMeasuredChannelSegmentEndTime());
    assertEquals(dao.getMeasuredChannelSegmentCreationTime(),
        descriptor.getMeasuredChannelSegmentCreationTime());
  }
}