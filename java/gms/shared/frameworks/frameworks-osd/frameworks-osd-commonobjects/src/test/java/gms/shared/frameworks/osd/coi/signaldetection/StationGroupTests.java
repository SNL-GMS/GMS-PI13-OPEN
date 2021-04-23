package gms.shared.frameworks.osd.coi.signaldetection;

import static org.junit.jupiter.api.Assertions.assertThrows;

import gms.shared.frameworks.osd.coi.util.TestUtilities;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class StationGroupTests {

  @Test
  void testFactoryMethodWithNullStationNameThrowsException() {
    Station station = Mockito.mock(Station.class);
    Exception e = assertThrows(NullPointerException.class,
        () -> StationGroup.from(null, "test", List.of(station)));
  }

  @Test
  void testFactoryMethodWithEmptyNameThrowsException() {
    Station station = Mockito.mock(Station.class);
    Exception e = assertThrows(IllegalArgumentException.class,
        () -> StationGroup.from("", "test", List.of(station)));
  }

  @Test
  void testFactoryMethodWithNullDescriptionThrowsException() {
    Station station = Mockito.mock(Station.class);
    Exception e = assertThrows(NullPointerException.class,
        () -> StationGroup.from("Test Station Group", null, List.of(station)));
  }

  @Test
  void  testFactoryMethodWithEmptyDescriptionThrowsException() {
    Station station = Mockito.mock(Station.class);
    Exception e = assertThrows(IllegalArgumentException.class,
        () -> StationGroup.from("test", "", List.of(station)));
  }

  @Test
  void testFactoryMethodWithNullStationListThrowsException() {
    Exception e = assertThrows(NullPointerException.class,
        () -> StationGroup.from("test", "test description", null));
  }

  @Test
  void testFactoryMethodWithEmptyStationListThrowsException() {
    Exception e = assertThrows(IllegalArgumentException.class,
        () -> StationGroup.from("test", "test description", List.of()));

  }
}
