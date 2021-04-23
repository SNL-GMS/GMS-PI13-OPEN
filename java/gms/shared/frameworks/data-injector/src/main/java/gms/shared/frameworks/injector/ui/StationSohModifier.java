package gms.shared.frameworks.injector.ui;

import gms.shared.frameworks.injector.Modifier;
import gms.shared.frameworks.osd.coi.signaldetection.StationGroup;
import java.util.List;
import org.apache.kafka.clients.producer.Producer;
import gms.shared.frameworks.osd.coi.soh.StationSoh;

/**
 * Calls the StationSohGenerator which makes a list of stations and
 */
public class StationSohModifier implements Modifier<Iterable<StationSoh>> {
  private CapabilitySohRollupModifier capabilityRollupModifier;
  private StationSohGenerator stationSohGenerator;

  /**
   * Constructor for StationSohModifier
   */
  public StationSohModifier() {
    /* Load StationGroups used by CapabilityRollupModifier and StationSohGenerator */
    String baseFilePath = "gms/shared/frameworks/injector/";
    String stationGroupMapFilePath = baseFilePath + "StationGroupMap.json";
    List<StationGroup> stationGroups =
        UiDataInjectorUtility.loadStationGroupsFromFile(stationGroupMapFilePath);

    this.capabilityRollupModifier = new CapabilitySohRollupModifier(stationGroups);
    this.stationSohGenerator = new StationSohGenerator(stationGroups);
  }

  @Override
  public List<StationSoh> apply(Iterable<StationSoh> stationSohIterable) {
    List<StationSoh> newUpdatedStations = this.stationSohGenerator.getUpdatedStations();

    // Call the apply for the CapabiltySohRollupModifier with updated StationSoh
    this.capabilityRollupModifier.applyStationSoh(newUpdatedStations);

    // Call publish (which for now just prints the CapabilitySOHRollup list)
    this.capabilityRollupModifier.publishRollups();
    return newUpdatedStations;
  }

  /**
   * Sets this capabilityRollupModifier's producer
   * @param producer Producer<String, String>
   */
  @Override
  public void setProducer(Producer<String, String> producer) {
    this.capabilityRollupModifier.setProducer(producer);
  }
}
