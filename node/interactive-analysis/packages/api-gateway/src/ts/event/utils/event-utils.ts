import { uuid4 } from '@gms/common-util';
import includes from 'lodash/includes';
import { TimeRange } from '../../common/model';
import {
  FeatureMeasurementTypeName,
  SignalDetectionHypothesis
} from '../../signal-detection/model';
import { Event, EventHypothesis, EventStatus, LocationBehavior } from '../model-and-schema/model';
import { DefinitionMapContainer, DefinitionMapOSD } from '../model-and-schema/model-osd';

/**
 * Creates a new, empty event hyp
 * @param eventId the event id
 */
export function createEmptyEventHypothesis(eventId: string): EventHypothesis {
  return {
    id: uuid4(),
    associations: [],
    eventId,
    parentEventHypotheses: [],
    rejected: false,
    modified: true,
    locationSolutionSets: [],
    preferredLocationSolution: undefined
  };
}

/**
 * Creates empty event
 * @param stageId Stage id for the preferred hypothesis
 */
export function createEmptyEvent(stageId: string): Event {
  const eventId = uuid4();
  const preferredHyp = {
    processingStageId: stageId,
    eventHypothesis: createEmptyEventHypothesis(eventId)
  };
  return {
    id: eventId,
    rejectedSignalDetectionAssociations: [],
    currentEventHypothesis: preferredHyp,
    hypotheses: [preferredHyp.eventHypothesis],
    monitoringOrganization: 'TEST',
    preferredEventHypothesisHistory: [preferredHyp],
    finalEventHypothesisHistory: [],
    status: EventStatus.ReadyForRefinement,
    associations: [],
    signalDetectionIds: [],
    hasConflict: false
  };
}

/**
 * Get all event hypothesis associated to an sd of all hyps
 * @param allEvents list of all events to search
 * @param signalDetectionHypoId hypothesis id to find
 */
export function getEventHyposAssocToSd(allEvents: Event[], signalDetectionHypIds: string[]) {
  return getEventHyposAssocToSdMapAndArray(allEvents, signalDetectionHypIds).array;
}

/**
 * Get all event hypothesis associated to an sd returning a map of event id to hyp
 *
 * @param allEvents the events
 * @param signalDetectionHypoId the signal detection hypothesis ids
 */
export function getEventHyposAssocToSdMap(allEvents: Event[], signalDetectionHypIds: string[]) {
  return getEventHyposAssocToSdMapAndArray(allEvents, signalDetectionHypIds).map;
}

/**
 * Get all event hypothesis associated to an sd returning both a map and array
 *
 * @param allEvents the events
 * @param signalDetectionHypoId the signal detection hypothesis ids
 */
function getEventHyposAssocToSdMapAndArray(allEvents: Event[], signalDetectionHypIds: string[]) {
  const eventHypoList: EventHypothesis[] = [];
  const eventIdToHypoMap = new Map<string, EventHypothesis>();

  allEvents.forEach(evt => {
    evt.currentEventHypothesis.eventHypothesis.associations.forEach(assoc => {
      // Lookup EventHypothesis and check if not in the map entry for the SD Hypo
      if (!assoc.rejected && includes(signalDetectionHypIds, assoc.signalDetectionHypothesisId)) {
        eventHypoList.push(evt.currentEventHypothesis.eventHypothesis);
        eventIdToHypoMap.set(evt.id, evt.currentEventHypothesis.eventHypothesis);
      }
    });
  });

  return { map: eventIdToHypoMap, array: eventHypoList };
}

/**
 * Returns true if event is in the given time range
 * @param event the event
 * @param timeRange the time range
 */
export function isEventInTimeRange(event: Event, timeRange: TimeRange): boolean {
  if (!event || !event.currentEventHypothesis) {
    return false;
  }

  const eventTime =
    event.currentEventHypothesis.eventHypothesis.preferredLocationSolution.locationSolution.location
      .time;

  const startTimeMatch = eventTime >= timeRange.startTime;
  const endTimeMatch = eventTime <= timeRange.endTime;

  // Return true if the event is in time range
  return startTimeMatch && endTimeMatch;
}

/**
 * Gets the location behaviors from the given list which share
 * a feature measurement with the given signal detection hypothesis
 * @param sdHyp Signal detection hypothesis
 * @param locationBehaviors list of location behaviors to search through
 */
export function getLocationBehaviorsForSd(
  sdHyp: SignalDetectionHypothesis,
  locationBehaviors: LocationBehavior[]
): LocationBehavior[] {
  return locationBehaviors
    .filter(lB =>
      sdHyp.featureMeasurements.find(
        sdFM =>
          sdHyp.parentSignalDetectionId === lB.signalDetectionId &&
          sdFM.featureMeasurementType === lB.featureMeasurementType
      )
    )
    .filter(lb => lb !== undefined);
}

/**
 * Generates the mapping of sd hyps to defining settings for a location call
 * @param locationBehaviors an array of user-set location behaviors
 * @param signalDetectionHyps the signal detection hypothesis associated to the event being located
 */
export function generateSignalDetectionBehaviorsMap(
  locationBehaviors: LocationBehavior[],
  signalDetectionHyps: SignalDetectionHypothesis[]
): { [id: string]: DefinitionMapContainer } {
  const sdMap: { [id: string]: DefinitionMapContainer } = {};
  signalDetectionHyps.forEach(sdHyp => {
    // To link locBehaviors to sd hyps, we have to fo through feature measurement
    const behaviorsForSd = getLocationBehaviorsForSd(sdHyp, locationBehaviors);
    const definitionMap: DefinitionMapOSD = {};
    if (!behaviorsForSd) {
      // If there is no existent set of defining settings for an SD,
      // sets a default set to false
      definitionMap[FeatureMeasurementTypeName.ARRIVAL_TIME] = {
        defining: false,
        systemOverridable: false,
        overrideThreshold: 1000
      };
      definitionMap[FeatureMeasurementTypeName.SLOWNESS] = {
        defining: false,
        systemOverridable: false,
        overrideThreshold: 1000
      };
      definitionMap[FeatureMeasurementTypeName.SOURCE_TO_RECEIVER_AZIMUTH] = {
        defining: false,
        systemOverridable: false,
        overrideThreshold: 1000
      };
    } else {
      behaviorsForSd.forEach(locationBehavior => {
        const fmType = locationBehavior.featureMeasurementType;
        definitionMap[fmType] = {
          defining: locationBehavior.defining,
          systemOverridable: false,
          overrideThreshold: 1000
        };
      });
    }
    sdMap[sdHyp.id] = { definitionMap };
  });
  return sdMap;
}
