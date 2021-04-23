package gms.core.interactiveanalysis.config.service;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import gms.shared.frameworks.configuration.Selector;
import java.util.Collections;
import java.util.List;

/**
 * Data transfer object containing a configuration name and a list of selectors.
 */
@AutoValue
public abstract class ConfigQuery {

  /**
   * Gets the configuration name
   * @return the configuration name
   */
  public abstract String getConfigurationName();

  /**
   * Gets the selectors
   * @return the selectors
   */
  public abstract List<Selector> getSelectors();

  /**
   * Create a {@link ConfigQuery} from all parameters
   * @param configName the name of the configuration
   * @param selectors the selectors
   * @return a {@link ConfigQuery}
   */
  @JsonCreator
  public static ConfigQuery from(
      @JsonProperty("configName") String configName,
      @JsonProperty("selectors") List<Selector> selectors) {
    return new AutoValue_ConfigQuery(
        configName, Collections.unmodifiableList(selectors));
  }
}
