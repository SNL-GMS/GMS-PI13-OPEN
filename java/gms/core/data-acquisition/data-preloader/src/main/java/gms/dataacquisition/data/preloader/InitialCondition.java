package gms.dataacquisition.data.preloader;

import static java.lang.String.format;

import java.util.Arrays;
import java.util.stream.Stream;

/**
 * Enum describing all available initial conditions a {@link GenerationSpec} can contain
 */
public enum InitialCondition {

  STATION_GROUPS("stationGroups"),
  STATION("station"),
  STATIONS("stations"),
  CHANNEL("channel"),
  CHANNELS("channels"),
  RECEPTION_DELAY("receptionDelay");

  private final String name;

  InitialCondition(String name) {
    this.name = name;
  }

  public static Stream<InitialCondition> initialConditions() {
    return Arrays.stream(values());
  }

  public static InitialCondition parse(String condition) {
    return initialConditions()
        .filter(cond -> cond.name.equals(condition))
        .findFirst()
        .orElseThrow(
            () -> new IllegalArgumentException(format("Invalid InitialCondition %s", condition)));
  }

  @Override
  public String toString() {
    return name;
  }
}
