package gms.core.performancemonitoring.ssam.control.cache;

import gms.shared.frameworks.osd.coi.soh.CapabilitySohRollup;
import gms.shared.frameworks.osd.coi.soh.StationSoh;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A StationSohCorrelatingCacheProcessor objects keeps track of how StationSoh objects are correlated
 * with CapabilitySohRollup objects. The idea is that as both types of objects flow into the system,
 * StationSohs get associated with whatever CapabilitySohRollup objects they helped create, by matching
 * the StationSoh UUIDs stored in the CapabilitySohRollup object to the UUIDs of the uncoming
 * StationSohs.
 */
public class StationSohCorrelatingCacheProcessor implements
    CorrelatingCache<CapabilitySohRollup, StationSoh> {

  /**
   * This class pairs a CapabilitySohRollup object with a set of correlated StationSoh objects.
   * The set can be added to later.
   */
  private static class CorrelationWrapper {

    final CapabilitySohRollup capabilitySohRollup;

    final Set<StationSoh> correlatedStationSohSet = new HashSet<>();

    public CorrelationWrapper(CapabilitySohRollup capabilitySohRollup) {
      this.capabilitySohRollup = capabilitySohRollup;
    }

    /**
     * Added a StationSoh object, correlating it with the CapabilitySohRollup object.
     * @param stationSoh the StatioSoh object to add.
     */
    public void addStationSoh(StationSoh stationSoh) {
      if (capabilitySohRollup.getBasedOnStationSohs().contains(stationSoh.getId())) {
        correlatedStationSohSet.add(stationSoh);
      } else {
        throw new IllegalStateException("Capability rollup for "
            + capabilitySohRollup.getForStationGroup()
            + "does not have StationSoh with UUID " + stationSoh.getId());
      }
    }
  }

  private final Map<String, CorrelationWrapper> correlationWrapperByGroup = new ConcurrentHashMap<>();
  private final Map<UUID, StationSoh> stationSohById = new ConcurrentHashMap<>();

  /**
   * FluxSink to send a completed correlation to. A correlation is completed once the List has
   * all of the StationSohs referenced in the capability rollup.
   *
   * TODO: Turn the List into a Set, because order does not matter.
   */
  private final FluxSink<Pair<CapabilitySohRollup, List<StationSoh>>> target;
  
  private StationSohCorrelatingCacheProcessor(
      FluxSink<Pair<CapabilitySohRollup, List<StationSoh>>> target
  ) {
    this.target = target;
  }

  /**
   * Created a StationSohCorrelatingCacheProcessor object.
   * @param target The FluxSink to send completed correlations to.
   * @return a StationSohCorrelatingCacheProcessor object
   */
  public static StationSohCorrelatingCacheProcessor create(
      FluxSink<Pair<CapabilitySohRollup, List<StationSoh>>> target
  ) {
    Objects.requireNonNull(target);
    return new StationSohCorrelatingCacheProcessor(target);
  }

  /**
   * Add a new StationSoh object to the existing correlations
   * @param stationSoh StationSoh object to add
   */
  @Override
  public synchronized void add(StationSoh stationSoh) {
    Objects.requireNonNull(stationSoh);

    var previousStationSohKeyOptional = stationSohById.values().stream()
        .filter(soh -> soh.getStationName().equals(stationSoh.getStationName()))
        .findFirst();

    previousStationSohKeyOptional.ifPresent(
        previous -> stationSohById.remove(previous.getId())
    );

    stationSohById.put(stationSoh.getId(), stationSoh);

    doCorrelations(null, stationSoh);
  }

  /**
   * "Track" a CapabilitySohRollup object; that is, add it to the set of CapabilitySohRollup objects
   * that will be associated with StationSoh objects.
   *
   * @param capabilitySohRollup CapabilitySohRollup object to track
   */
  @Override
  public synchronized void track(CapabilitySohRollup capabilitySohRollup) {
    Objects.requireNonNull(capabilitySohRollup);

    correlationWrapperByGroup.put(
        capabilitySohRollup.getForStationGroup(),
        new CorrelationWrapper(capabilitySohRollup)
    );

    doCorrelations(capabilitySohRollup, null);
  }

  /**
   * Correlate any new StationSoh objects with any new CapabilitySohRollup objects. If a
   * CapabilitySohRollup object has been correlated with all of the StationSohs objects it references,
   * the correlation is complete, and it is paired with the set of StationSoh objects and sinked.
   * It is also removed from the current set of correlations so that it doesnt take space.
   */
  private void doCorrelations(
      CapabilitySohRollup capabilitySohRollupToCorrelate,
      StationSoh stationSohToCorrelate
  ) {

    // Loop through all of our current correlations
    Flux.fromIterable(correlationWrapperByGroup.entrySet())
        //
        // If we were given a StationSoh, only concern our selves with CapabililitySohRollups
        // that are associated with it.
        //
        .filter(
            entry -> Objects.isNull(stationSohToCorrelate)
                || entry.getValue().capabilitySohRollup.getBasedOnStationSohs()
                .contains(stationSohToCorrelate.getId())
        )

        //
        // If we were given a CapabilitySohRollup, only concern our selves with it.
        //
        .filter(
            entry -> Objects.isNull(capabilitySohRollupToCorrelate)
                // Reference comparison. These should be the same object.
                || entry.getValue().capabilitySohRollup == capabilitySohRollupToCorrelate
        )

        //
        // For each correlation, find any StationSohs in the stationSohById map that arent
        // part of the correlation, and add them if they need to be added.
        //
        .doOnNext(entry -> {

          var correlation = entry.getValue();
          var capabilitySohRollup = correlation.capabilitySohRollup;

          capabilitySohRollup.getBasedOnStationSohs().forEach(
              id -> Optional.ofNullable(stationSohById.get(id))
                  .ifPresent(correlation::addStationSoh));
        })

        //
        // Since we will now be sinking any complete correlations, filter those that are not
        // complete.
        //
        .filter(entry -> {
          var capabilitySohStationSohCount = entry.getValue().capabilitySohRollup
              .getBasedOnStationSohs().size();
          var correlatedCount = entry.getValue().correlatedStationSohSet.size();
          return capabilitySohStationSohCount == correlatedCount;
        })

        //
        // Turn the completed correlations of Pairs, where the first of the pair is the
        // capabilitySohRollup, and the second is the set of StationSohs associated with it.
        //
        .map(entry -> {
          var capabilitySohRollup = entry.getValue().capabilitySohRollup;

          List<StationSoh> correlatedStationSohs = new ArrayList<>(
              entry.getValue().correlatedStationSohSet);

          return Pair.of(
              capabilitySohRollup,
              correlatedStationSohs
          );
        })

        //
        // Subscribe a consumer that sinks the pair and removes the correlation from the map.
        //
        .subscribe(pair -> {
          Optional.ofNullable(correlationWrapperByGroup.remove(pair.getKey().getForStationGroup()))
              .ifPresent(wrapper -> target.next(pair));
        });
  }

}
