package gms.shared.frameworks.soh.repository.performancemonitoring;

import gms.shared.frameworks.osd.api.util.ChannelTimeRangeRequest;
import gms.shared.frameworks.osd.api.util.ChannelTimeRangeSohTypeRequest;
import gms.shared.frameworks.osd.api.util.TimeRangeRequest;
import gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssue.AcquiredChannelEnvironmentIssueType;
import gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssueAnalog;
import gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssueBoolean;
import gms.shared.frameworks.osd.coi.test.utils.UtilsTestFixtures;
import gms.shared.frameworks.osd.dao.channelsoh.AcquiredChannelEnvironmentIssueAnalogDao;
import gms.shared.frameworks.osd.dao.channelsoh.AcquiredChannelEnvironmentIssueBooleanDao;
import gms.shared.frameworks.osd.dao.util.CoiEntityManagerFactory;
import gms.shared.frameworks.soh.repository.station.StationRepositoryJpa;
import gms.shared.frameworks.soh.repository.util.DbTest;
import java.math.BigInteger;
import javax.persistence.Query;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import javax.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.testcontainers.junit.jupiter.Testcontainers;

import static gms.shared.frameworks.osd.coi.dataacquisitionstatus.DataAcquisitionStatusTestFixtures.ACQUIRED_CHANNEL_SOH_ANALOG;
import static gms.shared.frameworks.osd.coi.dataacquisitionstatus.DataAcquisitionStatusTestFixtures.ACQUIRED_CHANNEL_SOH_ANALOG_TWO;
import static gms.shared.frameworks.osd.coi.dataacquisitionstatus.DataAcquisitionStatusTestFixtures.ACQUIRED_CHANNEL_SOH_BOOLEAN;
import static gms.shared.frameworks.osd.coi.dataacquisitionstatus.DataAcquisitionStatusTestFixtures.NOW;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
class StationSohRepositoryJpaTest extends DbTest {

  @AfterEach
  void testCaseTeardown() {
    EntityManager entityManager = entityManagerFactory.createEntityManager();
    try {
      entityManager.getTransaction().begin();
      Query query = entityManager.createNativeQuery("delete from gms_soh.channel_env_issue_analog");
      query.executeUpdate();
      query = entityManager.createNativeQuery("delete from gms_soh.channel_env_issue_boolean");
      query.executeUpdate();
      entityManager.getTransaction().commit();
    }finally{
      entityManager.close();
    }
  }

  @Test
  void testStoreAcquiredChannelSohAnalogValidation() {
    assertThrows(NullPointerException.class,
        () -> new StationSohRepositoryJpa(entityManagerFactory).storeAcquiredChannelSohAnalog(null));
  }

  @Test
  void testStoreAcquiredChannelSohAnalog() {
    StationSohRepositoryJpa stationSohRepositoryJpa =
        new StationSohRepositoryJpa(entityManagerFactory);
    stationSohRepositoryJpa.storeAcquiredChannelSohAnalog(List.of(ACQUIRED_CHANNEL_SOH_ANALOG, ACQUIRED_CHANNEL_SOH_ANALOG));
    stationSohRepositoryJpa.storeAcquiredChannelSohAnalog(List.of(ACQUIRED_CHANNEL_SOH_ANALOG, ACQUIRED_CHANNEL_SOH_ANALOG_TWO));

    EntityManager entityManager = entityManagerFactory.createEntityManager();
    AcquiredChannelEnvironmentIssueAnalogDao actual = entityManager.find(
        AcquiredChannelEnvironmentIssueAnalogDao.class,
        ACQUIRED_CHANNEL_SOH_ANALOG.getId());
    AcquiredChannelEnvironmentIssueAnalog twoModified =
        AcquiredChannelEnvironmentIssueAnalog.from(
            UUID.randomUUID(),
            ACQUIRED_CHANNEL_SOH_ANALOG_TWO.getChannelName(),
            ACQUIRED_CHANNEL_SOH_ANALOG_TWO.getType(),
            ACQUIRED_CHANNEL_SOH_ANALOG_TWO.getStartTime(),
            ACQUIRED_CHANNEL_SOH_ANALOG_TWO.getEndTime().plusSeconds(1),
            ACQUIRED_CHANNEL_SOH_ANALOG_TWO.getStatus());
    stationSohRepositoryJpa.storeAcquiredChannelSohAnalog(List.of(twoModified));
    AcquiredChannelEnvironmentIssueAnalogDao actualTwo = entityManager.find(
        AcquiredChannelEnvironmentIssueAnalogDao.class,
        twoModified.getId());

    try {
      entityManager.getTransaction().begin();
      Query query = entityManager.createNativeQuery("select count(*) from gms_soh.channel_env_issue_analog");
      int resultSize = ((BigInteger)query.getSingleResult()).intValue();
      assertEquals(2, resultSize);
      entityManager.getTransaction().commit();
    }finally{
      entityManager.close();
    }

    assertNotNull(actual);

    assertEquals(ACQUIRED_CHANNEL_SOH_ANALOG.getChannelName(), actual.getChannel().getName());
    assertEquals(ACQUIRED_CHANNEL_SOH_ANALOG.getType(), actual.getType());
    assertEquals(ACQUIRED_CHANNEL_SOH_ANALOG.getStartTime(), actual.getStartTime());
    assertEquals(ACQUIRED_CHANNEL_SOH_ANALOG.getEndTime(), actual.getEndTime());
    assertEquals(ACQUIRED_CHANNEL_SOH_ANALOG.getStatus(), actual.getStatus(), 0.0001);

    //validate we can modify an existing ACEI
    assertEquals(ACQUIRED_CHANNEL_SOH_ANALOG_TWO.getEndTime().plusSeconds(1), actualTwo.getEndTime());

    entityManager.close();
  }

  @Test
  void testStoreAcquiredChannelSohBooleanValidation() {
    assertThrows(NullPointerException.class,
        () -> new StationSohRepositoryJpa(entityManagerFactory).storeAcquiredChannelEnvironmentIssueBoolean(null));
  }

  @Test
  void testStoreAcquiredChannelSohBoolean() {
    StationSohRepositoryJpa stationSohRepositoryJpa =
        new StationSohRepositoryJpa(entityManagerFactory);
    stationSohRepositoryJpa.storeAcquiredChannelEnvironmentIssueBoolean(List.of(ACQUIRED_CHANNEL_SOH_BOOLEAN));

    EntityManager entityManager = entityManagerFactory.createEntityManager();
    AcquiredChannelEnvironmentIssueBooleanDao actual = entityManager.find(
        AcquiredChannelEnvironmentIssueBooleanDao.class,
        ACQUIRED_CHANNEL_SOH_BOOLEAN.getId());

    assertNotNull(actual);

    assertEquals(ACQUIRED_CHANNEL_SOH_BOOLEAN.getChannelName(), actual.getChannel().getName());
    assertEquals(ACQUIRED_CHANNEL_SOH_BOOLEAN.getType(), actual.getType());
    assertEquals(ACQUIRED_CHANNEL_SOH_BOOLEAN.getStartTime(), actual.getStartTime());
    assertEquals(ACQUIRED_CHANNEL_SOH_BOOLEAN.getEndTime(), actual.getEndTime());
    assertEquals(ACQUIRED_CHANNEL_SOH_BOOLEAN.getStatus(), actual.isStatus());

    entityManager.close();
  }

  @Test
  void testRemoveAcquiredChannelSohBooleans() {
    StationSohRepositoryJpa stationSohRepositoryJpa =
        new StationSohRepositoryJpa(entityManagerFactory);
    stationSohRepositoryJpa.storeAcquiredChannelEnvironmentIssueBoolean(List.of(ACQUIRED_CHANNEL_SOH_BOOLEAN));

    stationSohRepositoryJpa.removeAcquiredChannelEnvironmentIssueBooleans(List.of(ACQUIRED_CHANNEL_SOH_BOOLEAN));

    EntityManager entityManager = entityManagerFactory.createEntityManager();
    assertNull(entityManager.find(AcquiredChannelEnvironmentIssueBooleanDao.class,
        ACQUIRED_CHANNEL_SOH_BOOLEAN.getId()));
    entityManager.close();
  }

  @Test
  void testRetrieveAcquiredChannelSohAnalogByChannelAndTimeRangeValidation() {
    assertThrows(NullPointerException.class,
        () -> new StationSohRepositoryJpa(entityManagerFactory)
            .retrieveAcquiredChannelEnvironmentIssueAnalogByChannelAndTimeRange(null));
  }

  @Test
  void testRetrieveAcquiredChannelSohAnalogByChannelAndTimeRange() {
    StationSohRepositoryJpa stationSohRepositoryJpa =
        new StationSohRepositoryJpa(entityManagerFactory);
    stationSohRepositoryJpa.storeAcquiredChannelSohAnalog(List.of(ACQUIRED_CHANNEL_SOH_ANALOG));

    ChannelTimeRangeRequest request =
        ChannelTimeRangeRequest.create(UtilsTestFixtures.CHANNEL.getName(),
            NOW.minusSeconds(700),
            NOW);
    List<AcquiredChannelEnvironmentIssueAnalog> result = stationSohRepositoryJpa
        .retrieveAcquiredChannelEnvironmentIssueAnalogByChannelAndTimeRange(request);
    assertEquals(1, result.size());
    assertTrue(result.contains(ACQUIRED_CHANNEL_SOH_ANALOG));
  }

  @Test
  void testRetrieveLatestAcquiredChannelEnvironmentIssueAnalog() {
    StationSohRepositoryJpa stationSohRepositoryJpa = new StationSohRepositoryJpa(entityManagerFactory);
    List<AcquiredChannelEnvironmentIssueAnalog> allList = new java.util.ArrayList<>(
        List.copyOf(UtilsTestFixtures.latestAnalogs));
    allList.addAll(UtilsTestFixtures.earlierAnalogs);
    stationSohRepositoryJpa.storeAcquiredChannelSohAnalog(allList);
    List<AcquiredChannelEnvironmentIssueAnalog> result = stationSohRepositoryJpa
        .retrieveLatestAcquiredChannelEnvironmentIssueAnalog(List.of(
            UtilsTestFixtures.CHANNEL.getName(), UtilsTestFixtures.CHANNEL_TWO.getName()));
    assertEquals(2, result.size());
    assertTrue(result.contains(UtilsTestFixtures.latestAnalogs.get(0)));
    assertTrue(result.contains(UtilsTestFixtures.latestAnalogs.get(1)));
  }

  @Test
  void testRetrieveLatestAcquiredChannelEnvironmentIssueBoolean() {
    StationSohRepositoryJpa stationSohRepositoryJpa = new StationSohRepositoryJpa(entityManagerFactory);
    List<AcquiredChannelEnvironmentIssueBoolean> allList = new java.util.ArrayList<>(
        List.copyOf(UtilsTestFixtures.latestBooleans));
    allList.addAll(UtilsTestFixtures.earlierBoolean);
    stationSohRepositoryJpa.storeAcquiredChannelEnvironmentIssueBoolean(allList);
    List<AcquiredChannelEnvironmentIssueBoolean> result = stationSohRepositoryJpa
        .retrieveLatestAcquiredChannelEnvironmentIssueBoolean(List.of(
            UtilsTestFixtures.CHANNEL.getName(), UtilsTestFixtures.CHANNEL_TWO.getName()));
    assertEquals(2, result.size());
    assertTrue(result.contains(UtilsTestFixtures.latestBooleans.get(0)));
    assertTrue(result.contains(UtilsTestFixtures.latestBooleans.get(1)));
  }

  @Test
  void testRetrieveAcquiredChannelSohBooleanByChannelAndTimeRangeValidation() {
    assertThrows(NullPointerException.class,
        () -> new StationSohRepositoryJpa(entityManagerFactory)
            .retrieveAcquiredChannelEnvironmentIssueBooleanByChannelAndTimeRange(null));
  }

  @Test
  void testRetrieveAcquiredChannelsohBooleanByChannelAndTimeRange() {
    StationSohRepositoryJpa stationSohRepositoryJpa =
        new StationSohRepositoryJpa(entityManagerFactory);
    stationSohRepositoryJpa.storeAcquiredChannelEnvironmentIssueBoolean(List.of(ACQUIRED_CHANNEL_SOH_BOOLEAN));

    ChannelTimeRangeRequest request =
        ChannelTimeRangeRequest.create(UtilsTestFixtures.CHANNEL.getName(),
            NOW.minusSeconds(700),
            NOW);
    List<AcquiredChannelEnvironmentIssueBoolean> result = stationSohRepositoryJpa
        .retrieveAcquiredChannelEnvironmentIssueBooleanByChannelAndTimeRange(request);
    assertEquals(1, result.size());
    assertTrue(result.contains(ACQUIRED_CHANNEL_SOH_BOOLEAN));
  }

  @Test
  void testRetrieveAcquiredChannelSohAnalogByIdValidation() {
    assertThrows(NullPointerException.class,
        () -> new StationSohRepositoryJpa(entityManagerFactory)
            .retrieveAcquiredChannelEnvironmentIssueAnalogById(null));
  }

  @Test
  void testRetrieveAcquiredChannelSohAnalogByIdEmpty() {
    Optional<AcquiredChannelEnvironmentIssueAnalog> result = new StationSohRepositoryJpa(
        entityManagerFactory)
        .retrieveAcquiredChannelEnvironmentIssueAnalogById(UUID.randomUUID());

    assertFalse(result.isPresent());
  }

  @Test
  void testRetrieveAcquiredChannelSohAnalogByIdPresent() {
    StationSohRepositoryJpa stationSohRepositoryJpa =
        new StationSohRepositoryJpa(entityManagerFactory);
    stationSohRepositoryJpa.storeAcquiredChannelSohAnalog(List.of(ACQUIRED_CHANNEL_SOH_ANALOG));

    Optional<AcquiredChannelEnvironmentIssueAnalog> result = stationSohRepositoryJpa
        .retrieveAcquiredChannelEnvironmentIssueAnalogById(ACQUIRED_CHANNEL_SOH_ANALOG.getId());

    assertTrue(result.isPresent());
    assertEquals(ACQUIRED_CHANNEL_SOH_ANALOG, result.get());
  }

  @Test
  void testRetrieveAcquiredChannelSohBooleanByIdValidation() {
    assertThrows(NullPointerException.class,
        () -> new StationSohRepositoryJpa(entityManagerFactory)
            .retrieveAcquiredChannelEnvironmentIssueBooleanById(null));
  }

  @Test
  void testRetrieveAcquiredChannelSohBooleanByIdEmpty() {
    Optional<AcquiredChannelEnvironmentIssueBoolean> result = new StationSohRepositoryJpa(
        entityManagerFactory)
        .retrieveAcquiredChannelEnvironmentIssueBooleanById(UUID.randomUUID());

    assertFalse(result.isPresent());
  }

  @Test
  void testRetrieveAcquiredChannelSohBooleanByIdPresent() {
    StationSohRepositoryJpa stationSohRepositoryJpa =
        new StationSohRepositoryJpa(entityManagerFactory);
    stationSohRepositoryJpa.storeAcquiredChannelEnvironmentIssueBoolean(List.of(ACQUIRED_CHANNEL_SOH_BOOLEAN));

    Optional<AcquiredChannelEnvironmentIssueBoolean> result = stationSohRepositoryJpa
        .retrieveAcquiredChannelEnvironmentIssueBooleanById(ACQUIRED_CHANNEL_SOH_BOOLEAN.getId());

    assertTrue(result.isPresent());
    assertEquals(ACQUIRED_CHANNEL_SOH_BOOLEAN, result.get());
  }

  @Test
  void testRetrieveAcquiredChannelSohAnalogByChannelTimeRangeAndTypeValidation() {
    assertThrows(NullPointerException.class,
        () -> new StationSohRepositoryJpa(entityManagerFactory)
            .retrieveAcquiredChannelEnvironmentIssueAnalogByChannelTimeRangeAndType(null));
  }

  @Test
  void testRetrieveAcquiredChannelSohAnalogByChannelTimeRangeAndType() {
    StationSohRepositoryJpa stationSohRepositoryJpa =
        new StationSohRepositoryJpa(entityManagerFactory);
    stationSohRepositoryJpa.storeAcquiredChannelSohAnalog(List.of(ACQUIRED_CHANNEL_SOH_ANALOG));

    ChannelTimeRangeSohTypeRequest request = ChannelTimeRangeSohTypeRequest.create(
        UtilsTestFixtures.CHANNEL.getName(),
        NOW.minusSeconds(700),
        NOW,
        AcquiredChannelEnvironmentIssueType.CLIPPED);

    List<AcquiredChannelEnvironmentIssueAnalog> result =
        stationSohRepositoryJpa.retrieveAcquiredChannelEnvironmentIssueAnalogByChannelTimeRangeAndType(request);

    assertEquals(1, result.size());
    assertTrue(result.contains(ACQUIRED_CHANNEL_SOH_ANALOG));
  }

  @Test
  void testRetrieveAcquiredChannelSohBooleanByChannelTimeRangeAndTypeValidation() {
    assertThrows(NullPointerException.class,
        () -> new StationSohRepositoryJpa(entityManagerFactory)
            .retrieveAcquiredChannelSohBooleanByChannelTimeRangeAndType(null));
  }

  @Test
  void testRetrieveAcquiredChannelSohBooleanByChannelTimeRangeAndType() {
    StationSohRepositoryJpa stationSohRepositoryJpa =
        new StationSohRepositoryJpa(entityManagerFactory);
    stationSohRepositoryJpa.storeAcquiredChannelEnvironmentIssueBoolean(List.of(ACQUIRED_CHANNEL_SOH_BOOLEAN));

    ChannelTimeRangeSohTypeRequest request = ChannelTimeRangeSohTypeRequest.create(
        UtilsTestFixtures.CHANNEL.getName(),
        NOW.minusSeconds(700),
        NOW,
        AcquiredChannelEnvironmentIssueType.CLOCK_LOCKED);

    List<AcquiredChannelEnvironmentIssueBoolean> result =
        stationSohRepositoryJpa.retrieveAcquiredChannelSohBooleanByChannelTimeRangeAndType(request);

    assertEquals(1, result.size());
    assertTrue(result.contains(ACQUIRED_CHANNEL_SOH_BOOLEAN));
  }

  @Test
  void testRetrieveAcquiredChannelEnvironmentalIssueAnalogByTimeValidation() {
    assertThrows(NullPointerException.class,
        () -> new StationSohRepositoryJpa(entityManagerFactory)
            .retrieveAcquiredChannelEnvironmentIssueAnalogByTime(null));
  }

  @Test
  void testRetrieveAcquiredChannelEnvironmentalIssueAnalogByTime() {
    StationSohRepositoryJpa stationSohRepositoryJpa =
        new StationSohRepositoryJpa(entityManagerFactory);
    stationSohRepositoryJpa.storeAcquiredChannelSohAnalog(List.of(ACQUIRED_CHANNEL_SOH_ANALOG));

    TimeRangeRequest request = TimeRangeRequest.create(
        NOW.minusSeconds(700),
        NOW
    );

    List<AcquiredChannelEnvironmentIssueAnalog> result =
        stationSohRepositoryJpa.retrieveAcquiredChannelEnvironmentIssueAnalogByTime(request);

    assertEquals(1, result.size());
    assertTrue(result.contains(ACQUIRED_CHANNEL_SOH_ANALOG));
  }

  @Test
  void testRetrieveAcquiredChannelEnvironmentalIssueBooleanByTimeValidation() {
    assertThrows(NullPointerException.class,
        () -> new StationSohRepositoryJpa(entityManagerFactory)
            .retrieveAcquiredChannelEnvironmentIssueBooleanByTime(null));
  }

  @Test
  void testRetrieveAcquiredChannelEnvironmentalIssueBooleanByTime() {
    StationSohRepositoryJpa stationSohRepositoryJpa =
        new StationSohRepositoryJpa(entityManagerFactory);
    stationSohRepositoryJpa.storeAcquiredChannelEnvironmentIssueBoolean(List.of(ACQUIRED_CHANNEL_SOH_BOOLEAN));

    TimeRangeRequest request = TimeRangeRequest.create(
        NOW.minusSeconds(700),
        NOW
    );

    List<AcquiredChannelEnvironmentIssueBoolean> result =
        stationSohRepositoryJpa.retrieveAcquiredChannelEnvironmentIssueBooleanByTime(request);

    assertEquals(1, result.size());
    assertTrue(result.contains(ACQUIRED_CHANNEL_SOH_BOOLEAN));
  }
}