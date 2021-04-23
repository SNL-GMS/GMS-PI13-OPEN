package gms.shared.frameworks.osd.api.signaldetection;

import gms.shared.frameworks.osd.coi.signaldetection.FkSpectraDefinition;
import java.util.Collection;

public interface FkSpectraDefinitionRepositoryInterface {
  /**
   * Stores the {@link FkSpectraDefinition}
   *
   * @param fkSpectraDefinition FkSpectraDefinition to store, not null
   * @throws NullPointerException if fkSpectraDefinition is null
   */
  void store(FkSpectraDefinition fkSpectraDefinition);

  /**
   * Retrieves all of the {@link FkSpectraDefinition}s stored in this {@link
   * FkSpectraDefinitionRepositoryInterface}
   *
   * @return collection of FkSpectraDefinition, not null
   */
  Collection<FkSpectraDefinition> retrieveAll(Collection<String> fkSpectraDefinitionNames);
}
