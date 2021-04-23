package gms.shared.frameworks.osd.coi.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import gms.shared.frameworks.osd.coi.DoubleValue;
import java.io.Serializable;

/**
 * A single component of a feature prediction.  The actual {@link FeaturePrediction} is equal to the
 * sum of its {@link FeaturePredictionComponent}s which may represent baseline values or corrections
 * to the baseline values.
 */
@AutoValue
public abstract class FeaturePredictionComponent implements Serializable {

  public abstract DoubleValue getValue();

  public abstract boolean isExtrapolated();

  public abstract FeaturePredictionCorrectionType getPredictionComponentType();

  @JsonCreator
  public static FeaturePredictionComponent from(
      @JsonProperty("value") DoubleValue value,
      @JsonProperty("extrapolated") boolean extrapolated,
      @JsonProperty("predictionComponentType") FeaturePredictionCorrectionType predictionComponentType) {
    return new AutoValue_FeaturePredictionComponent(value, extrapolated, predictionComponentType);
  }
}
