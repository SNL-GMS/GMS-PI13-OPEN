package gms.shared.frameworks.soh.repository.stationreference;

import gms.shared.frameworks.coi.exceptions.DataExistsException;
import gms.shared.frameworks.osd.coi.stationreference.ReferenceSensor;
import gms.shared.frameworks.osd.coi.stationreference.StationReferenceTestFixtures;
import gms.shared.frameworks.soh.repository.util.DbTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Testcontainers
public class ReferenceSensorRepositoryJpaTests extends DbTest {

  private static ReferenceSensorRepositoryJpa referenceSensorRepository;

  @BeforeAll
  static void testSuiteSetup() {
    referenceSensorRepository = new ReferenceSensorRepositoryJpa(entityManagerFactory);
    // store test data
    referenceSensorRepository.storeReferenceSensors(StationReferenceTestFixtures.REFERENCE_SENSORS);
  }

  @Test
  void testStoringSensorsTwiceWillThrowException() {
    RuntimeException ex = assertThrows(RuntimeException.class, () -> referenceSensorRepository
        .storeReferenceSensors(StationReferenceTestFixtures.REFERENCE_SENSORS));
    assertEquals(DataExistsException.class, ex.getCause().getClass());
  }

  @Test
  void testRetrievalByIdNonEmptyList() {
    List<UUID> searchIds = List.of(StationReferenceTestFixtures.REFERENCE_SENSOR.getId());
    List<ReferenceSensor> sensors = referenceSensorRepository
        .retrieveReferenceSensorsById(searchIds);
    assertEquals(1, sensors.size());
    assertEquals(StationReferenceTestFixtures.REFERENCE_SENSORS, sensors);
  }

  @Test
  void testRetrievalByIdEmptyList() {
    List<ReferenceSensor> sensors = referenceSensorRepository
        .retrieveReferenceSensorsById(List.of());
    assertEquals(1, sensors.size());
    assertEquals(StationReferenceTestFixtures.REFERENCE_SENSORS, sensors);
  }

  @Test
  void testRetrievalByChannelName() {
    List<String> searchIds = List
        .of(StationReferenceTestFixtures.REFERENCE_SENSOR.getChannelName());
    Map<String, List<ReferenceSensor>> sensors = referenceSensorRepository
        .retrieveSensorsByChannelName(searchIds);
    assertEquals(StationReferenceTestFixtures.REFERENCE_SENSORS,
        sensors.get(StationReferenceTestFixtures.REFERENCE_SENSOR.getChannelName()));
  }

  @Test
  void testRetrievalByIdThrowsIllegalArgumentExceptionWhenPassedAnEmptyList() {
    assertThrows(IllegalArgumentException.class,
        () -> referenceSensorRepository.retrieveSensorsByChannelName(List.of()));
  }
}
