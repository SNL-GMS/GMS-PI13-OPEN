import {
  CacheQueries,
  CommonQueries,
  CommonTypes,
  EventQueries,
  EventTypes,
  ProcessingStationQueries,
  ProcessingStationTypes,
  QcMaskQueries,
  QcMaskTypes,
  SignalDetectionQueries,
  SignalDetectionTypes,
  WorkflowQueries
} from '@gms/common-graphql';
import { AnalystWorkspaceTypes } from '@gms/ui-state';
import flatten from 'lodash/flatten';
import { graphql, MutationFunction } from 'react-apollo';
import { autoOpenEvent } from '~analyst-ui/common/actions/event-actions';
import { systemConfig } from '~analyst-ui/config';
import {
  getLatestLocationSolutionSet,
  getPreferredLocationSolutionIdFromEventHypothesis
} from '../common/utils/event-util';

/**
 * Defines the base props for the queries.
 */
interface BaseProps {
  currentTimeInterval: CommonTypes.TimeRange;
  analystActivity: AnalystWorkspaceTypes.AnalystActivity;
}

// ----- Common Queries ------

/**
 * Returns a wrapped component providing the `workspaceStateQuery` query.
 *
 * @export
 * @returns the wrapped component
 */
export function graphqlWorkspaceStateQuery() {
  return graphql(CommonQueries.workspaceStateQuery, { name: 'workspaceStateQuery' });
}

// ----- Cache Queries ------

/**
 * Returns a wrapped component providing the `historyQuery` query.
 *
 * @export
 * @returns the wrapped component
 */
export function graphqlHistoryQuery() {
  return graphql(CacheQueries.historyQuery, { name: 'historyQuery' });
}

/**
 * Returns a wrapped component providing the `eventHistoryQuery` query.
 *
 * @export
 * @returns the wrapped component
 */
export function graphqlEventHistoryQuery() {
  return graphql(CacheQueries.eventHistoryQuery, { name: 'eventHistoryQuery' });
}

// ----- Event Queries ------

/**
 * Returns a wrapped component providing the `eventsInTimeRangeQuery` query.
 *
 * @export
 * @template T defines the component base props required
 * @returns the wrapped component
 */
export function graphqlEventsInTimeRangeQuery<
  T extends BaseProps & {
    openEventId: string;
    updateEvents: MutationFunction<{}>;
    setOpenEventId(
      event: EventTypes.Event | undefined,
      latestLocationSolutionSet: EventTypes.LocationSolutionSet | undefined,
      preferredLocationSolutionId: string | undefined
    ): void;
  }
>() {
  return graphql(EventQueries.eventsInTimeRangeQuery, {
    options: (props: T) => {
      const variables: EventTypes.EventsInTimeRangeQueryArgs = {
        timeRange: systemConfig.getEventsTimeRange(props.currentTimeInterval, props.analystActivity)
      };
      return {
        variables,
        onCompleted: (data: { eventsInTimeRange: EventTypes.Event[] }) => {
          autoOpenEvent(
            data.eventsInTimeRange,
            props.currentTimeInterval,
            props.openEventId,
            props.analystActivity,
            (event: EventTypes.Event) =>
              props.setOpenEventId
                ? props.setOpenEventId(
                    event,
                    getLatestLocationSolutionSet(event),
                    getPreferredLocationSolutionIdFromEventHypothesis(
                      event.currentEventHypothesis.eventHypothesis
                    )
                  )
                : undefined,
            props.updateEvents
          );
        }
      };
    },
    skip: (props: T) => !props.currentTimeInterval,
    name: 'eventsInTimeRangeQuery'
  });
}

// ----- Station Processing Queries ------

/**
 * Returns a wrapped component providing the `defaultStationsQuery` query.
 *
 * @export
 * @returns the wrapped component
 */
export function graphqlDefaultProcessingStationsQuery() {
  return graphql(ProcessingStationQueries.defaultProcessingStationsQuery, {
    name: 'defaultStationsQuery'
  });
}

// ----- QC Masks Queries ------

/**
 * Returns a wrapped component providing the `qcMasksByChannelNameQuery` query.
 *
 * @export
 * @template T defines the component base props required
 * @returns the wrapped component
 */
export function graphqlQcMasksByChannelNameQuery<
  T extends BaseProps & ProcessingStationTypes.DefaultStationsQueryProps
>() {
  return graphql(QcMaskQueries.qcMasksByChannelNameQuery, {
    options: (props: T) => {
      /**
       * Gets a list of channels from either props or direct from the apollo client
       * @param stations stations to get ids from
       *
       * @returns channelIds from stations
       */
      const getChannelListForDefaultStations = (
        processingStations: ProcessingStationTypes.ProcessingStation[]
      ): string[] => {
        const channelIds = [];
        if (processingStations && processingStations.length > 0) {
          processingStations.forEach(station => {
            // TODO: What should be the default channel
            // Get first channel in station list as default
            // channelIds.push(station.defaultChannel.name);
            // tslint:disable-next-line:max-line-length
            channelIds.push(...flatten(station.channels.map(channel => channel.name)));
          });
        }
        return channelIds;
      };

      const stations = props.defaultStationsQuery.defaultProcessingStations;
      const channelNames: string[] = getChannelListForDefaultStations(stations);
      // get signal detections in the current interval
      const variables: QcMaskTypes.QcMasksByChannelNameQueryArgs = {
        timeRange: systemConfig.getDefaultTimeRange(
          props.currentTimeInterval,
          props.analystActivity
        ),
        channelNames
      };
      return {
        variables
      };
    },
    skip: (props: T) =>
      !props.currentTimeInterval || !props.defaultStationsQuery.defaultProcessingStations,
    name: 'qcMasksByChannelNameQuery'
  });
}

// ----- Signal Detection Queries ------

/**
 * Returns a wrapped component providing the `signalDetectionsByStationQuery` query.
 *
 * @export
 * @template T defines the component base props required
 * @returns the wrapped component
 */
export function graphqlSignalDetectionsByStationQuery<
  T extends BaseProps & ProcessingStationTypes.DefaultStationsQueryProps
>() {
  return graphql(SignalDetectionQueries.signalDetectionsByStationQuery, {
    options: (props: T) => {
      // Get signal detections in the current interval
      const variables: SignalDetectionTypes.SignalDetectionsByStationQueryArgs = {
        stationIds: props.defaultStationsQuery.defaultProcessingStations.map(
          station => station.name
        ),
        timeRange: systemConfig.getSignalDetectionTimeRange(
          props.currentTimeInterval,
          props.analystActivity
        )
      };
      return {
        variables
      };
    },
    skip: (props: T) =>
      !props.currentTimeInterval || !props.defaultStationsQuery.defaultProcessingStations,
    name: 'signalDetectionsByStationQuery'
  });
}

// ----- Workflow Queries ------

/**
 * Returns a wrapped component providing the `stagesQuery` query.
 *
 * @export
 * @returns the wrapped component
 */
export function graphqlStagesQuery() {
  return graphql(WorkflowQueries.stagesQuery, { name: 'stagesQuery' });
}
