package gms.core.performancemonitoring.ssam.control;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import gms.shared.frameworks.osd.coi.signaldetection.Location;
import gms.shared.frameworks.osd.coi.signaldetection.Station;
import gms.shared.frameworks.osd.coi.stationreference.StationType;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class NeedsSohAttentionTest {

  @Test
  void testInstantiation() {
    Instant creationTime = Instant.now();
    List<String> stationNames = Arrays.asList("stationA", "stationB", "stationC");

    NeedsSohAttention needsSohAttention = NeedsSohAttention.from(creationTime, stationNames);

    assertEquals(creationTime, needsSohAttention.getCreationTime());
    assertEquals(stationNames, needsSohAttention.getStationNames());
    // Not the same instance.
    assertFalse(stationNames == needsSohAttention.getStationNames());

    for (String stationName: stationNames) {
      Station mockStation = Mockito.mock(Station.class);
      when(mockStation.getName()).thenReturn(stationName);
      assertTrue(needsSohAttention.stationNeedsSohAttention(mockStation));
      assertTrue(needsSohAttention.stationNeedsSohAttention(stationName));
    }

    Station mockStation = Mockito.mock(Station.class);
    when(mockStation.getName()).thenReturn("stationD");
    assertFalse(needsSohAttention.stationNeedsSohAttention(mockStation));
    assertFalse(needsSohAttention.stationNeedsSohAttention("stationD"));
    assertEquals(needsSohAttention, NeedsSohAttention.from(creationTime, stationNames));
    assertEquals(needsSohAttention.hashCode(),
        NeedsSohAttention.from(creationTime, stationNames).hashCode());

    assertThrows(NullPointerException.class, () -> {
      NeedsSohAttention.from(null, Collections.emptyList());
    });

    // Now, show that it can take a null list.
    NeedsSohAttention needsSohAttention2 = NeedsSohAttention.from(creationTime, null);

    for (String stationName: stationNames) {
      assertFalse(needsSohAttention2.stationNeedsSohAttention(stationName));
    }
  }


}