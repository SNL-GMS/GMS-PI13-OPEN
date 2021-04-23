import { EventTypes, SignalDetectionTypes } from '@gms/common-graphql';
import flatMap from 'lodash/flatMap';
import { userPreferences } from '~analyst-ui/config/user-preferences';

/**
 * Get the current open event
 * @param openEventId Id of open event
 * @param eventsInTimeRangeQuery eventsInTimeRangeQuery
 */
export function getOpenEvent(
  openEventId: string | undefined,
  events: EventTypes.Event[]
): EventTypes.Event | undefined {
  const event =
    openEventId && events && events.length > 0 ? events.find(e => e.id === openEventId) : undefined;
  return event && event.currentEventHypothesis && event.currentEventHypothesis.eventHypothesis
    ? event
    : undefined;
}

/**
 * Get the time of the currently open event as a number.
 * @param openEventId Id of the open event
 * @param eventsInTimeRangeQuery The apollo query
 */
export function getEventTime(
  openEventId: string | undefined,
  events: EventTypes.Event[]
): number | undefined {
  const event = getOpenEvent(openEventId, events);
  return event && event.currentEventHypothesis && event.currentEventHypothesis.eventHypothesis
    ? event.currentEventHypothesis.eventHypothesis.preferredLocationSolution.locationSolution
        .location.time
    : undefined;
}

/**
 * Gets the signal detections associated to the given event
 *
 * @param event the open even to get sd's for
 * @param signalDetections signalDetections to look through
 */
export function getAssocSds(
  event: EventTypes.Event,
  signalDetections: SignalDetectionTypes.SignalDetection[]
) {
  return event && event.currentEventHypothesis && event.currentEventHypothesis.eventHypothesis
    ? flatMap(event.currentEventHypothesis.eventHypothesis.signalDetectionAssociations, assocSD => {
        const maybeSD = signalDetections.find(
          sd =>
            assocSD.signalDetectionHypothesis.id === sd.currentHypothesis.id && !assocSD.rejected
        );
        if (maybeSD) {
          return maybeSD;
        }
      }).filter(assocSD => assocSD !== undefined)
    : [];
}

/**
 * Returns the latest location solution set for the provided event.
 *
 * @param event an event
 */
export function getLatestLocationSolutionSet(
  event: EventTypes.Event
): EventTypes.LocationSolutionSet | undefined {
  return event && event.currentEventHypothesis && event.currentEventHypothesis.eventHypothesis
    ? getLatestLocationSolutionSetForEventHypothesis(event.currentEventHypothesis.eventHypothesis)
    : undefined;
}

/**
 * Returns the latest location solution set for the provided event hypothesis.
 *
 * @param eventHypothesis an event hypothesis
 */
export function getLatestLocationSolutionSetForEventHypothesis(
  eventHypothesis: EventTypes.EventHypothesis
): EventTypes.LocationSolutionSet {
  return eventHypothesis &&
    eventHypothesis.locationSolutionSets &&
    eventHypothesis.locationSolutionSets.length > 0
    ? eventHypothesis.locationSolutionSets.reduce((prev, curr) => {
        if (curr.count > prev.count) {
          return curr;
        }
        return prev;
      }, eventHypothesis.locationSolutionSets[0])
    : undefined;
}

/**
 * Gets the default preferred location id for an event based off the config
 * Or if not found returns the id for the preferred location solution
 *
 * @param locationSolutionSet Location Solution Set to  get default preferred location solution from
 */
export function getPreferredDefaultLocationId(
  locationSolutionSet: EventTypes.LocationSolutionSet
): string | undefined {
  if (!locationSolutionSet.locationSolutions || locationSolutionSet.locationSolutions.length < 1) {
    return undefined;
  }

  let toReturn: string;
  // A for loop is used so we can break
  // tslint:disable-next-line: prefer-for-of
  for (
    let i = 0;
    i < userPreferences.location.preferredLocationSolutionRestraintOrder.length;
    i++
  ) {
    const dr = userPreferences.location.preferredLocationSolutionRestraintOrder[i];
    const maybeLS = locationSolutionSet.locationSolutions.find(
      ls => ls.locationRestraint.depthRestraintType === dr
    );
    if (maybeLS) {
      toReturn = maybeLS.id;
      break;
    }
  }
  if (toReturn) {
    return toReturn;
  }
  return locationSolutionSet[0].id;
}

/**
 * Gets the default preferred location id for an event based off the config
 * Or if not found returns the id for the preferred location solution
 *
 * @param eventHypothesis Event hypothesis to get default preferred location solution from
 */
export function getPreferredLocationSolutionIdFromEventHypothesis(
  eventHypothesis: EventTypes.EventHypothesis
): string {
  if (!eventHypothesis.locationSolutionSets || eventHypothesis.locationSolutionSets.length < 1) {
    return undefined;
  }
  const set = getLatestLocationSolutionSetForEventHypothesis(eventHypothesis);
  return getPreferredDefaultLocationId(set);
}

/**
 * Gets the distance to station for the given event, location solution id, and station id
 * @param event event with location solution
 * @param locationSolutionId location solution id
 * @param stationId station id
 */
export function getDistanceToStationForEventAndLocationSolutionId(
  event: EventTypes.Event,
  locationSolutionId: string,
  stationId: string
): EventTypes.LocationToStationDistance | undefined {
  const locationSolutions = flatMap(
    event.currentEventHypothesis.eventHypothesis.locationSolutionSets,
    lss => lss.locationSolutions
  );
  const maybeLs = locationSolutions.find(ls => ls.id === locationSolutionId);
  if (maybeLs) {
    const maybeDistance = maybeLs.locationToStationDistances.find(
      lsd => lsd.stationId === stationId
    );
    if (maybeDistance) {
      return maybeDistance;
    }
  }
  return undefined;
}

/**
 * Gets the distance to all stations for the given location solution id
 * @param event event with location solution
 * @param locationSolutionId location solution id
 */
export function getDistanceToStationsForLocationSolutionId(
  event: EventTypes.Event,
  locationSolutionId: string
): EventTypes.LocationToStationDistance[] {
  if (!event) {
    return [];
  }
  const locationSolutions = flatMap(
    event.currentEventHypothesis.eventHypothesis.locationSolutionSets,
    lss => lss.locationSolutions
  );
  const maybeLs = locationSolutions.find(ls => ls.id === locationSolutionId);
  if (maybeLs) {
    return maybeLs.locationToStationDistances;
  }
  return event.currentEventHypothesis.eventHypothesis.preferredLocationSolution.locationSolution
    .locationToStationDistances;
}

/**
 * Checks if event hypothesis has changed or if the number of location solution sets has changed
 * @param event current props event
 * @param prevEvent previous props event
 *
 * @returns boolean if changes have occurred
 */
export function shouldUpdateSelectedLocationSolution(
  prevEvent: EventTypes.Event,
  event: EventTypes.Event
) {
  return (
    prevEvent.currentEventHypothesis.eventHypothesis.id !==
      event.currentEventHypothesis.eventHypothesis.id ||
    prevEvent.currentEventHypothesis.eventHypothesis.locationSolutionSets.length !==
      event.currentEventHypothesis.eventHypothesis.locationSolutionSets.length
  );
}
