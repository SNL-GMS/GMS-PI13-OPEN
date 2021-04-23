package gms.dataacquisition.stationreceiver.cd11.injector;

import java.time.Instant;
import java.util.Objects;

/**
 * A frame along with its reception time.
 *
 * @param <T> a frame, typically RawStationDataFrame or Cd11Frame
 */
public class ReceivedFrame<T> implements Comparable {

  final T frame;
  final Instant receptionTime;

  ReceivedFrame(Instant receptionTime, T frame) {
    this.frame = frame;
    this.receptionTime = receptionTime;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ReceivedFrame<?> that = (ReceivedFrame<?>) o;
    return receptionTime.equals(that.receptionTime);
  }

  @Override
  public int hashCode() {
    return Objects.hash(receptionTime);
  }

  @Override
  public int compareTo(Object other) {
    return this.receptionTime.compareTo(((ReceivedFrame) other).receptionTime);
  }

}
