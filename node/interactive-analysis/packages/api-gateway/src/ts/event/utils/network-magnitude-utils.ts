import { produce } from 'immer';
import cloneDeep from 'lodash/cloneDeep';
import { Location, PhaseType } from '../../common/model';
import { gatewayLogger } from '../../log/gateway-logger';
import {
  FeatureMeasurementTypeName,
  SignalDetectionHypothesis
} from '../../signal-detection/model';
import { ProcessingStation } from '../../station/processing-station/model';
import { SystemConfig } from '../../system-config';
import {
  findAmplitudeFeatureMeasurementValue,
  findArrivalTimeFeatureMeasurementValue,
  findPhaseFeatureMeasurementValue
} from '../../util/feature-measurement-utils';
import {
  DefiningBehavior,
  Event,
  EventHypothesis,
  LocationSolution,
  LocationSolutionSet,
  MagnitudeType
} from '../model-and-schema/model';
import { getLatestLSSForEventHyp } from './location-utils';

/**
 * Per magnitude arguments used for the compute network magnitude client
 */
export interface ComputeNetworkMagnitudeArguments {
  stationIdsForDetectionHypothesisIds: { [s: string]: string };
  hypothesisIdToLocationMap: { [s: string]: Location };
  definingDefs: DefiningBehavior[];
  eventHyp: EventHypothesis;
  sdHyps: SignalDetectionHypothesis[];
}

/**
 * Sorts a list of signal detections by arrival time
 * @param sdHyps signal detection hypothesis to sort
 */
export function sortSignalDetectionHypsByArrivalTime(sdHyps: SignalDetectionHypothesis[]) {
  return sdHyps.sort(
    (a, b) =>
      findArrivalTimeFeatureMeasurementValue(a.featureMeasurements).value -
      findArrivalTimeFeatureMeasurementValue(b.featureMeasurements).value
  );
}

/**
 * Modifies the given event hypothesis by replacing a previous lss with a new lss that shares its id
 * @param eventHyp Event hypothesis to modify
 * @param newLss New location solution set,
 */
export function replaceLocationSolutionSet(eventHyp: EventHypothesis, newLss: LocationSolutionSet) {
  const index = eventHyp.locationSolutionSets.findIndex(lss => lss.id === newLss.id);
  if (index >= 0) {
    eventHyp.locationSolutionSets[index] = newLss;
  }
}

/**
 * Creates a new location solution set where network magnitudes of the given type are removed
 * @param lss Location Solution Set to be removed
 * @param magType magnitude type to remove
 */
export function stripMagnitudesOfTypeFromLocationSolutionSet(
  lss: LocationSolutionSet,
  magType: MagnitudeType
): LocationSolutionSet {
  return {
    count: lss.count,
    id: lss.id,
    locationSolutions: lss.locationSolutions.map(ls =>
      stripMagnitudesOfTypeFromLocation(ls, magType)
    )
  };
}

/**
 * Creates a new location with magnitude solutions of the given type removed
 * @param locationSolution location solution to remove network mag solutions from
 * @param magType magnitude type to remove
 */
export function stripMagnitudesOfTypeFromLocation(
  locationSolution: LocationSolution,
  magType: MagnitudeType
): LocationSolution {
  return produce<LocationSolution>(locationSolution, draftState => {
    draftState.networkMagnitudeSolutions = draftState.networkMagnitudeSolutions.filter(
      nms => nms.magnitudeType !== magType
    );
  });
}

/**
 * Gets the first sds by arrival time on the given stations
 * @param stations stations to look for first arrived
 * @param phase phase of sds to look for
 * @param signalDetections signal detections to look through
 */
export function getFirstSdHypsWithGivenPhaseForGivenStations(
  stations: ProcessingStation[],
  stationIdsForAllSdHypIds: Map<string, string>,
  phase: PhaseType,
  signalDetectionHyps: SignalDetectionHypothesis[]
): SignalDetectionHypothesis[] {
  const firstSds = [];
  stations.forEach(station => {
    const maybeSD = signalDetectionHyps.find(
      sdHyp =>
        stationIdsForAllSdHypIds.get(sdHyp.id) === station.name &&
        findPhaseFeatureMeasurementValue(sdHyp.featureMeasurements).phase === phase
    );
    if (maybeSD) {
      firstSds.push(maybeSD);
    }
  });
  return firstSds;
}

/**
 * Checks whether a feature measurement for an sd hyp is defined
 * @param sdHyp sd hyp to test
 * @param amplitudeFeatureMeasurementTypeName feature measurement type to look for
 */
export const isAmplitudeFeatureMeasurementDefined = (
  sdHyp: SignalDetectionHypothesis,
  amplitudeFeatureMeasurementTypeName: FeatureMeasurementTypeName
): boolean =>
  findAmplitudeFeatureMeasurementValue(
    sdHyp.featureMeasurements,
    amplitudeFeatureMeasurementTypeName
  ) !== undefined;

/**
 * Gets the defining behaviors from an event
 * @param event the event to mine behaviors from. If none are found, return []
 */
export function getDefiningBehaviorsForEvent(
  event: Event,
  magType: MagnitudeType
): DefiningBehavior[] {
  const currentHyp = event.currentEventHypothesis.eventHypothesis;
  // We can always get the first location solution from the set because all location solutions in a set
  // will have the same defining behaviors
  const locationSolution = getLatestLSSForEventHyp(currentHyp).locationSolutions[0];
  if (!locationSolution) {
    gatewayLogger.error(
      'Attempting to get defining behaviors from event with empty location solution set'
    );
    return [];
  }
  const maybeNetworkMagnitude = locationSolution.networkMagnitudeSolutions.find(
    nms => nms.magnitudeType === magType
  );
  if (maybeNetworkMagnitude) {
    return maybeNetworkMagnitude.networkMagnitudeBehaviors.map(nmb => ({
      defining: nmb.defining,
      stationName: nmb.stationMagnitudeSolution.stationName,
      magnitudeType: magType
    }));
  }
  return [];
}

/**
 * Creates a defining behavior for a given station and mag type
 * @param stationName the station name
 * @param magnitudeType the magnitude type
 * @param defining optionally set what the defining status should be, defaults to true
 */
export function createDefaultDefiningBehaviorForStation(
  stationName: string,
  magnitudeType: MagnitudeType,
  defining
): DefiningBehavior {
  return {
    stationName,
    magnitudeType,
    defining
  };
}

/**
 * Merges user changes to behavior with the default behaviors, preferring the users changes
 * @param preferredBehaviors the preferred behaviors
 * @param previousBehaviors the previous behaviors
 */
export function mergePreferredAndPreviousBehaviors(
  preferredBehaviors: DefiningBehavior[],
  previousBehaviors: DefiningBehavior[]
): DefiningBehavior[] {
  return previousBehaviors.map(pDef => {
    const maybeChange = preferredBehaviors.find(
      userChange =>
        pDef.stationName === userChange.stationName &&
        pDef.magnitudeType === userChange.magnitudeType
    );
    return maybeChange ? maybeChange : pDef;
  });
}

/**
 * Gets the defining settings used for a locate call by merging event, default, and user-defined settings
 * @param event Event the compute magnitude for
 * @param userDefiningChanges the user's defining changes
 * @param firstOfPhase the first sd of each phase
 * @param stationIdsForAllSdHypIds a mapping from sdHypIds to stationIds
 * @param magType magnitude type ot get defining settings for
 * @param defaultDefiningSetting: boolean
 */
export const getDefiningSettings = (
  event: Event,
  userDefiningChanges: DefiningBehavior[],
  firstOfPhase: SignalDetectionHypothesis[],
  stationIdsForAllSdHypIds: Map<string, string>,
  magType: MagnitudeType,
  defaultDefiningSetting: boolean
) => {
  const defaultDefining = firstOfPhase.map(sdHyp =>
    createDefaultDefiningBehaviorForStation(
      stationIdsForAllSdHypIds.get(sdHyp.id),
      magType,
      defaultDefiningSetting
    )
  );

  const definingFromEvent = getDefiningBehaviorsForEvent(event, magType);
  const userDefiningChangesForMag = userDefiningChanges.filter(
    def => def.magnitudeType === magType
  );

  const definingSettings = mergePreferredAndPreviousBehaviors(
    userDefiningChangesForMag,
    mergePreferredAndPreviousBehaviors(definingFromEvent, defaultDefining)
  );

  return definingSettings;
};

/**
 * Creates a mapping between an sd hypothesis id and an osd location format
 * @param sdHyps Signal Detection hypothesis for the mapping
 * @param stationIdsForAllSdHypIds a mapping of sdhypId to stationId
 * @param defaultStations default stations for the interval
 * @param defaultDepth the default depth of a location
 */
export const getHypothesisIdToLocationMap = (
  sdHyps: SignalDetectionHypothesis[],
  stationIdsForAllSdHypIds: Map<string, string>,
  defaultStations: ProcessingStation[],
  defaultDepth: number
): { [s: string]: Location } => {
  const hypothesisIdToLocationMap: { [s: string]: Location } = {};
  sdHyps.forEach(sdHyp => {
    const stationIdForHyp = stationIdsForAllSdHypIds.get(sdHyp.id);
    const station = defaultStations.find(st => stationIdForHyp === st.name);
    if (station) {
      hypothesisIdToLocationMap[sdHyp.id] = {
        depthKm: station.location.depthKm ? station.location.depthKm : defaultDepth,
        elevationKm: station.location.elevationKm,
        latitudeDegrees: station.location.latitudeDegrees,
        longitudeDegrees: station.location.longitudeDegrees
      };
    } else {
      gatewayLogger.warn(`station not found when mapping hypothesis ids to location maps`);
    }
  });

  return hypothesisIdToLocationMap;
};

/**
 * Converts and filters the given map to an osd stationId format
 * @param sdHyps list of signal detection hypothesis
 * @param stationIdsForAllSdHypIds a superset of station ids - must be filtered and converted to an osd-friendly mapping
 */
export const getStationIdsForHypothesisIds = (
  sdHyps: SignalDetectionHypothesis[],
  stationIdsForAllSdHypIds: Map<string, string>
): { [s: string]: string } => {
  const stationIdsForDetectionHypothesisIds: { [s: string]: string } = {};
  sdHyps.forEach(sdHyp => {
    const stationIdForHyp = stationIdsForAllSdHypIds.get(sdHyp.id);
    stationIdsForDetectionHypothesisIds[sdHyp.id] = stationIdForHyp;
  });
  return stationIdsForDetectionHypothesisIds;
};

/**
 * Creates a cloned and modified event hypothesis for use in the compute magnitude query
 * @param event event to compute mags for
 * @param magType the magnitude type
 */
export const getEventHypForComputeMagnitude = (event: Event, magType: MagnitudeType) => {
  const clonedHypothesis = cloneDeep(event.currentEventHypothesis.eventHypothesis);
  const lss = getLatestLSSForEventHyp(clonedHypothesis);
  const strippedLss = stripMagnitudesOfTypeFromLocationSolutionSet(lss, magType);
  replaceLocationSolutionSet(clonedHypothesis, strippedLss);
  return clonedHypothesis;
};

/**
 * Gets the first sd by arrival time per station for the given magnitude
 * @param sdHypsUnfiltered Unfiltered list of sdHyps
 * @param stationIdsForAllSdHypIds the station ids for each sd hyp id
 * @param systemConfig the system config
 * @param defaultStations the default stations for the interval
 * @param magType mag type to get sdHyps for
 */
export const getFirstSdHypPerStationForMagnitude = (
  sdHypsUnfiltered: SignalDetectionHypothesis[],
  stationIdsForAllSdHypIds: Map<string, string>,
  systemConfig: SystemConfig,
  defaultStations: ProcessingStation[],
  magType: MagnitudeType
): SignalDetectionHypothesis[] => {
  const amplitudeType = systemConfig.amplitudeTypeForMagnitude.get(magType);
  const sds = sdHypsUnfiltered.filter(sdHyp =>
    isAmplitudeFeatureMeasurementDefined(sdHyp, amplitudeType)
  );

  return getFirstSdHypsWithGivenPhaseForGivenStations(
    defaultStations,
    stationIdsForAllSdHypIds,
    systemConfig.phaseForAmplitudeType.get(amplitudeType),
    sds
  );
};

/**
 * Creates the network magnitude arguments for one mag type
 * @param event Event we are computing mags for
 * @param userDefiningChanges user defined changed if any
 * @param sdHypsUnfiltered sdsHyps associated to the event
 * @param stationIdsForAllSdHypIds a mapping of sdHypId to station id for all associated ids
 * @param defaultStations the default stations in the system
 * @param systemConfig the system config
 * @param magType the magnitude type to get arguments for
 */
export const getNetworkArgumentsForMagType = (
  event: Event,
  userDefiningChanges: DefiningBehavior[],
  sdHypsUnfiltered: SignalDetectionHypothesis[],
  stationIdsForAllSdHypIds: Map<string, string>,
  defaultStations: ProcessingStation[],
  systemConfig: SystemConfig,
  magType: MagnitudeType,
  defaultDefiningSetting: boolean
) => {
  const firstOfPhase = getFirstSdHypPerStationForMagnitude(
    sdHypsUnfiltered,
    stationIdsForAllSdHypIds,
    systemConfig,
    defaultStations,
    magType
  );

  return {
    definingDefs: getDefiningSettings(
      event,
      userDefiningChanges,
      firstOfPhase,
      stationIdsForAllSdHypIds,
      magType,
      defaultDefiningSetting
    ),
    eventHyp: getEventHypForComputeMagnitude(event, magType),
    hypothesisIdToLocationMap: getHypothesisIdToLocationMap(
      firstOfPhase,
      stationIdsForAllSdHypIds,
      defaultStations,
      systemConfig.defaultDepthForMagnitude
    ),
    sdHyps: firstOfPhase,
    stationIdsForDetectionHypothesisIds: getStationIdsForHypothesisIds(
      firstOfPhase,
      stationIdsForAllSdHypIds
    )
  };
};

/**
 * Gets the arguments for a compute network mag call
 * @param event event to compute mag for
 * @param userDefiningChanges the defining changes set by the user (if any)
 * @param magnitudeTypesToCompute magnitudes for which compute mag will be called
 * @param sdsHypsUnsorted an unsorted list of signal detection hypothesis to use
 * @param stationIdsForAllSdHypIds a mapping from sdHypId to stationId
 * @param defaultStations a list of default processing stations
 * @param systemConfig the system configuration
 */
export const getNetworkMagClientArguments = (
  event: Event,
  userDefiningChanges: DefiningBehavior[],
  magnitudeTypesToCompute: MagnitudeType[],
  sdsHypsUnsorted: SignalDetectionHypothesis[],
  stationIdsForAllSdHypIds: Map<string, string>,
  defaultStations: ProcessingStation[],
  systemConfig: SystemConfig,
  defaultDefiningSetting: boolean
): Map<MagnitudeType, ComputeNetworkMagnitudeArguments> => {
  const argsPerMagnitude = new Map<MagnitudeType, ComputeNetworkMagnitudeArguments>();
  const sdsUnfiltered = sortSignalDetectionHypsByArrivalTime(sdsHypsUnsorted);

  magnitudeTypesToCompute.forEach(magType => {
    argsPerMagnitude.set(
      magType,
      getNetworkArgumentsForMagType(
        event,
        userDefiningChanges,
        sdsUnfiltered,
        stationIdsForAllSdHypIds,
        defaultStations,
        systemConfig,
        magType,
        defaultDefiningSetting
      )
    );
  });

  return argsPerMagnitude;
};
