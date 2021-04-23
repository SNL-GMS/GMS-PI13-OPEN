import { CommonTypes, EventTypes } from '@gms/common-graphql';
import { AnalystWorkspaceTypes } from '@gms/ui-state';
import sortBy from 'lodash/sortBy';
import { MutationFunction } from 'react-apollo';
import {
  getLatestLocationSolutionSet,
  getOpenEvent,
  getPreferredLocationSolutionIdFromEventHypothesis
} from '../utils/event-util';

/**
 * Updates and marks and event as opened.
 *
 * @param events the available events
 * @param openEventId the event id to open
 * @param analystActivity the current analyst activity
 * @param setOpenEventId the function to set the open event id
 * @param updateEvents the function to update the event
 */
export const openEvent = (
  events: EventTypes.Event[],
  openEventId: string,
  analystActivity: AnalystWorkspaceTypes.AnalystActivity,
  updateEvents: MutationFunction<{}>,
  setOpenEventId: (
    event: EventTypes.Event | undefined,
    latestLocationSolutionSet: EventTypes.LocationSolutionSet | undefined,
    preferredLocationSolutionId: string | undefined
  ) => void
) => {
  const event: EventTypes.Event = getOpenEvent(openEventId, events);
  if (event !== undefined) {
    const processingStageId = event.currentEventHypothesis.processingStage
      ? event.currentEventHypothesis.processingStage.id
      : undefined;
    if (processingStageId) {
      const variables: EventTypes.UpdateEventsMutationArgs = {
        eventIds: [openEventId],
        input: {
          processingStageId,
          status: EventTypes.EventStatus.OpenForRefinement
        }
      };
      if (updateEvents !== undefined) {
        updateEvents({
          variables
        }).catch();
      }
      if (setOpenEventId) {
        setOpenEventId(
          event,
          getLatestLocationSolutionSet(event),
          event.currentEventHypothesis && event.currentEventHypothesis.eventHypothesis
            ? getPreferredLocationSolutionIdFromEventHypothesis(
                event.currentEventHypothesis.eventHypothesis
              )
            : undefined
        );
      }
    } else {
      if (setOpenEventId) {
        setOpenEventId(
          event,
          getLatestLocationSolutionSet(event),
          event.currentEventHypothesis && event.currentEventHypothesis.eventHypothesis
            ? getPreferredLocationSolutionIdFromEventHypothesis(
                event.currentEventHypothesis.eventHypothesis
              )
            : undefined
        );
      }
    }
  }
};

/**
 * Action that auto opens the first non completed event within
 * the provided time interval.
 *
 * @param data the available events
 * @param currentTimeInterval the current time interval
 * @param openEventId the current open event id
 * @param analystActivity the current analyst activity
 * @param setOpenEventId the function to set the open event id
 * @param updateEvents the function to update the event
 */
export const autoOpenEvent = (
  events: EventTypes.Event[],
  currentTimeInterval: CommonTypes.TimeRange,
  openEventId: string,
  analystActivity: AnalystWorkspaceTypes.AnalystActivity,
  setOpenEventId: (
    event: EventTypes.Event | undefined,
    latestLocationSolutionSet: EventTypes.LocationSolutionSet | undefined,
    preferredLocationSolutionId: string | undefined
  ) => void,
  updateEvents: MutationFunction<{}>
): void => {
  if (
    events &&
    !openEventId &&
    analystActivity === AnalystWorkspaceTypes.AnalystActivity.eventRefinement
  ) {
    const sortedEvents = sortBy(
      events,
      e =>
        e.currentEventHypothesis.eventHypothesis.preferredLocationSolution.locationSolution.location
          .time
    );
    const event = sortedEvents.find(e => {
      const time =
        e.currentEventHypothesis.eventHypothesis.preferredLocationSolution.locationSolution.location
          .time;
      return (
        time >= currentTimeInterval.startTime &&
        time <= currentTimeInterval.endTime &&
        e.status !== EventTypes.EventStatus.Complete
      );
    });
    if (event && event.id !== openEventId) {
      openEvent(events, event.id, analystActivity, updateEvents, setOpenEventId);
    }
  }
};
