package gms.shared.frameworks.osd.dto.soh;

import static com.google.common.base.Preconditions.checkArgument;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import java.util.List;
import org.apache.commons.lang3.Validate;

/**
 * Represents the results of a historical StationSoh query for one or more SohMonitorTypes computed
 * for a single Station, containing historical values for each Channel in the Station.
 */
@AutoValue
public abstract class HistoricalStationSoh {

  public abstract String getStationName();

  /**
   * Calculation times matching each value entry in each associated {@link
   * HistoricalSohMonitorValues}, in epoch milliseconds
   *
   * @return An array of time values in epoch milliseconds, ordered by time
   */
  public abstract long[] getCalculationTimes();

  /**
   * List of {@link HistoricalSohMonitorValues} associated with a station for the span of related
   * calculation times
   *
   * @return An unordered {@link List} of {@link HistoricalSohMonitorValues}
   */
  public abstract List<HistoricalSohMonitorValues> getMonitorValues();

  @JsonCreator
  public static HistoricalStationSoh create(
      @JsonProperty("stationName") String stationName,
      @JsonProperty("calculationTimes") long[] calculationTimes,
      @JsonProperty("monitorValues") List<HistoricalSohMonitorValues> monitorValues) {

    Validate.notEmpty(stationName);

    monitorValues.forEach(historicalValues ->
        historicalValues.getValuesByType().values().forEach(sohValues ->
            checkArgument(calculationTimes.length == sohValues.size(),
                "All monitor value arrays must have the same length as the array "
                    + "of calculation times.")));

    return new AutoValue_HistoricalStationSoh(stationName, calculationTimes, monitorValues);
  }
}
