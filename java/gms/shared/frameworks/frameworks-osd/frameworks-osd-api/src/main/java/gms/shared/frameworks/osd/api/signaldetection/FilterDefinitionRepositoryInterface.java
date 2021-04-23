package gms.shared.frameworks.osd.api.signaldetection;

import gms.shared.frameworks.osd.coi.signaldetection.FilterDefinition;
import java.util.Collection;

/**
 * Repository for {@link FilterDefinition}
 */
public interface FilterDefinitionRepositoryInterface {

  /**
   * Stores the {@link FilterDefinition}
   *
   * @param filterDefinition FilterDefinition to store, not null
   * @throws NullPointerException if filterDefinition is null
   */
  void store(FilterDefinition filterDefinition);

  /**
   * Retrieves all of the {@link FilterDefinition}s stored in this {@link
   * FilterDefinitionRepositoryInterface}
   *
   * @param filterDefinitionNames an empty list of {@link FilterDefinition} names)
   * @return collection of FilterDefinitions, not null
   */
  Collection<FilterDefinition> retrieveAllFilterDefinitions(Collection<String> filterDefinitionNames);
}
