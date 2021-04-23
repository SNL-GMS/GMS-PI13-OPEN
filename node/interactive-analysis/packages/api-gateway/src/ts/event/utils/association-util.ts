import produce from 'immer';
import cloneDeep from 'lodash/cloneDeep';
import {
  FeatureMeasurementTypeName,
  SignalDetectionHypothesis
} from '../../signal-detection/model';
import { ProcessingStationProcessor } from '../../station/processing-station/processing-station-processor';
import { findPhaseFeatureMeasurementValue } from '../../util/feature-measurement-utils';
import {
  Event,
  EventHypothesis,
  LocationBehavior,
  SignalDetectionEventAssociation
} from '../model-and-schema/model';

/**
 * Filters the given associations to include only the associations to be removed
 * @param sdHypIdsToRemove the sd hypothesis to be unassociated
 * @param associations the list of associations before the unassociation occurs
 */
export function getAssociationsToRemove(
  sdHypIdsToRemove: string[],
  associations: SignalDetectionEventAssociation[]
): SignalDetectionEventAssociation[] {
  return associations
    .filter(assoc =>
      sdHypIdsToRemove.find(sdHypId => sdHypId === assoc.signalDetectionHypothesisId)
    )
    .map(assoc => ({ ...assoc, modified: true, rejected: true }));
}

/**
 * Return an updated set of Location Behaviors based on SD association/disassociation *
 * @returns The updated location behaviors list to replace new on
 */
export function updateLocationBehaviors(
  event: Event,
  sdHypo: SignalDetectionHypothesis,
  locBehaviors: LocationBehavior[],
  associate: boolean
): LocationBehavior[] {
  const updatedLocationBehaviors: LocationBehavior[] = cloneDeep(locBehaviors);
  // For each Arrival, Azimuth or Slowness find the FM. If we need to associate and not already
  // in the list create a new entry. If remove then don't add to new list (might deep copy and remove)
  const phaseFMValue = findPhaseFeatureMeasurementValue(sdHypo.featureMeasurements);
  const station = ProcessingStationProcessor.Instance().getStationByName(sdHypo.stationName);
  sdHypo.featureMeasurements.forEach(fm => {
    if (
      fm.featureMeasurementType === FeatureMeasurementTypeName.ARRIVAL_TIME ||
      fm.featureMeasurementType === FeatureMeasurementTypeName.RECEIVER_TO_SOURCE_AZIMUTH ||
      fm.featureMeasurementType === FeatureMeasurementTypeName.SOURCE_TO_RECEIVER_AZIMUTH ||
      fm.featureMeasurementType === FeatureMeasurementTypeName.SLOWNESS
    ) {
      // Find the location behavior index. If associate and not in list add it,
      // else if disassociate and in the list remove it.
      const behaviorIndex = updatedLocationBehaviors.findIndex(
        locBeh =>
          locBeh.signalDetectionId === sdHypo.parentSignalDetectionId &&
          locBeh.featureMeasurementType === fm.featureMeasurementType
      );
      if (behaviorIndex === -1 && associate) {
        const newLoc: LocationBehavior = {
          defining: false,
          featureMeasurementType: fm.featureMeasurementType,
          signalDetectionId: sdHypo.parentSignalDetectionId,
          featurePrediction: {
            phase: phaseFMValue ? phaseFMValue.phase : 'P',
            featurePredictionComponents: [] /* Java declares as a set not sure in JSON */,
            extrapolated: false,
            predictionType: fm.featureMeasurementType,
            sourceLocation:
              event.currentEventHypothesis.eventHypothesis.preferredLocationSolution
                .locationSolution.location,
            receiverLocation: station.location,
            channelName: fm.channel.name,
            stationName: sdHypo.stationName,
            predictedValue: fm.measurementValue
          },
          residual: 0,
          weight: 0
        };
        updatedLocationBehaviors.push(newLoc);
      } else if (behaviorIndex >= 0 && !associate) {
        updatedLocationBehaviors.splice(behaviorIndex, 1);
      }
    }
  });
  return updatedLocationBehaviors;
}

/**
 * Returns a modified event hypothesis that does not contain associations to undefined sds
 * @param eventHyp the event hypothesis
 */
function removeAssociationsToUndefinedSdsFromEventHyp(
  eventHyp: EventHypothesis,
  validSdHypIds: string[]
): EventHypothesis {
  const associationList = [];
  eventHyp.associations.forEach(assoc => {
    const sdhFound = validSdHypIds.indexOf(assoc.signalDetectionHypothesisId) > -1;
    if (sdhFound) {
      associationList.push(assoc);
    }
  });
  const validatedHyp = {
    ...eventHyp,
    associations: associationList
  };
  return validatedHyp;
}

/**
 * Returns a modified event that does not contain associations to undefined sds
 * @param event the event
 * @param validSdHypIds list of sd hypothesis which are loaded in the system
 */
function removeAssociationsToUndefinedSdsFromEvent(event: Event, validSdHypIds: string[]): Event {
  const validatedEvent = {
    ...event,
    hypotheses: event.hypotheses.map(hyp =>
      removeAssociationsToUndefinedSdsFromEventHyp(hyp, validSdHypIds)
    )
  };
  return validatedEvent;
}

/**
 * Removes associations to undefined sds from the events and adds them to the cache
 * @param events the events
 * @param validSdHypIds list of sd hypothesis which are loaded in the system
 */
export function removeAssociationsToUndefinedSds(
  events: Event[],
  validSdHypIds: string[]
): Event[] {
  const validatedEvents = events.map(event =>
    produce<Event>(event, (draftState: Event) =>
      removeAssociationsToUndefinedSdsFromEvent(event, validSdHypIds)
    )
  );
  return validatedEvents.map(event =>
    produce<Event>(event, draftState => {
      draftState.currentEventHypothesis.eventHypothesis = removeAssociationsToUndefinedSdsFromEventHyp(
        event.currentEventHypothesis.eventHypothesis,
        validSdHypIds
      );
    })
  );
}
