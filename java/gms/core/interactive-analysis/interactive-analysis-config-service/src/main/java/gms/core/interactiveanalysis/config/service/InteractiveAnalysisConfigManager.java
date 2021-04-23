package gms.core.interactiveanalysis.config.service;

import gms.shared.frameworks.configuration.repository.client.ConfigurationConsumerUtility;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import java.util.Map;
import java.util.Objects;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service providing Configuration to the interactive analysis components.
 */
@Path("")
public class InteractiveAnalysisConfigManager {

  private static final Logger logger = LoggerFactory.getLogger(InteractiveAnalysisConfigManager.class);

  static final String COMPONENT_NAME = "interactive-analysis-config-service";

  private final ConfigurationConsumerUtility configConsumer;

  private InteractiveAnalysisConfigManager(ConfigurationConsumerUtility configConsumer) {
    this.configConsumer = Objects.requireNonNull(configConsumer,
        "Cannot create with null configConsumer");
  }

  /**
   * Create {@link InteractiveAnalysisConfigManager} from all it's dependencies
   *
   * @param configConsumer the configuration consumer to use, not null
   * @return a {@link InteractiveAnalysisConfigManager}
   * @throws NullPointerException if ConfigurationConsumerUtility is null
   */
  static InteractiveAnalysisConfigManager create(
      ConfigurationConsumerUtility configConsumer) {
    return new InteractiveAnalysisConfigManager(configConsumer);
  }

  @Path("/resolve")
  @POST
  @Operation(description = "Resolves a configuration")
  public Map<String, Object> resolve(@RequestBody(
      description = "A query", required = true)
      ConfigQuery query) {
    try {
      return this.configConsumer.resolve(
          query.getConfigurationName(), query.getSelectors());
    } catch(Exception ex) {
      logger.error("Error resolving configuration for query " + query, ex);
      return Map.of();
    }
  }
}
