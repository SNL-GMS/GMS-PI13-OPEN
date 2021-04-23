import {
  CacheMutations,
  CacheQueries,
  CacheTypes,
  CommonQueries,
  CommonTypes,
  EventMutations,
  EventQueries,
  EventTypes,
  FkMutations,
  FkTypes,
  QcMaskMutations,
  QcMaskQueries,
  QcMaskTypes,
  SignalDetectionMutations,
  SignalDetectionQueries,
  SignalDetectionTypes,
  WorkflowMutations,
  WorkflowTypes
} from '@gms/common-graphql';
import { AnalystWorkspaceTypes } from '@gms/ui-state';
import { DataProxy } from 'apollo-cache';
import includes from 'lodash/includes';
import { graphql } from 'react-apollo';
import { systemConfig } from '~analyst-ui/config';

/**
 * Defines the base props for the mutations.
 */
interface BaseProps {
  currentTimeInterval: CommonTypes.TimeRange;
  analystActivity: AnalystWorkspaceTypes.AnalystActivity;
}

// ----- Cache Mutations ------

/**
 * Returns a wrapped component providing the `undoHistory` mutation.
 *
 * @export
 * @template T defines the component base props required
 * @param [withRef=false] true will allow one to get the instance
 * of your wrapped component from the higher-order GraphQL component
 * @returns the wrapped component
 */
export function graphqlUndoHistoryMutation<T extends BaseProps>(withRef: boolean = false) {
  return graphql(CacheMutations.undoHistoryMutation, {
    options: (props: T) => ({
      update: (store, result: CacheTypes.UndoHistoryMutationResult) => {
        const payload = result.data.undoHistory;
        updateApolloCacheFromDataPayload(
          store,
          props.currentTimeInterval,
          props.analystActivity,
          payload
        );
      }
    }),
    name: 'undoHistory',
    withRef
  });
}

/**
 * Returns a wrapped component providing the `redoHistory` mutation.
 *
 * @export
 * @template T defines the component base props required
 * @param [withRef=false] true will allow one to get the instance
 * of your wrapped component from the higher-order GraphQL component
 * @returns the wrapped component
 */
export function graphqlRedoHistoryMutation<T extends BaseProps>(withRef: boolean = false) {
  return graphql(CacheMutations.redoHistoryMutation, {
    options: (props: T) => ({
      update: (store, result: CacheTypes.RedoHistoryMutationResult) => {
        const payload = result.data.redoHistory;
        updateApolloCacheFromDataPayload(
          store,
          props.currentTimeInterval,
          props.analystActivity,
          payload
        );
      }
    }),
    name: 'redoHistory',
    withRef
  });
}

/**
 * Returns a wrapped component providing the `undoHistoryById` mutation.
 *
 * @export
 * @template T defines the component base props required
 * @param [withRef=false] true will allow one to get the instance
 * of your wrapped component from the higher-order GraphQL component
 * @returns the wrapped component
 */
export function graphqlUndoHistoryByIdMutation<T extends BaseProps>(withRef: boolean = false) {
  return graphql(CacheMutations.undoHistoryByIdMutation, {
    options: (props: T) => ({
      update: (store, result: CacheTypes.UndoHistoryByIdMutationResult) => {
        const payload = result.data.undoHistoryById;
        updateApolloCacheFromDataPayload(
          store,
          props.currentTimeInterval,
          props.analystActivity,
          payload
        );
      }
    }),
    name: 'undoHistoryById',
    withRef
  });
}

/**
 * Returns a wrapped component providing the `redoHistoryById` mutation.
 *
 * @export
 * @template T defines the component base props required
 * @param [withRef=false] true will allow one to get the instance
 * of your wrapped component from the higher-order GraphQL component
 * @returns the wrapped component
 */
export function graphqlRedoHistoryByIdMutation<T extends BaseProps>(withRef: boolean = false) {
  return graphql(CacheMutations.redoHistoryByIdMutation, {
    options: (props: T) => ({
      update: (store, result: CacheTypes.RedoHistoryByIdMutationResult) => {
        const payload = result.data.redoHistoryById;
        updateApolloCacheFromDataPayload(
          store,
          props.currentTimeInterval,
          props.analystActivity,
          payload
        );
      }
    }),
    name: 'redoHistoryById',
    withRef
  });
}

/**
 * Returns a wrapped component providing the `undoEventHistory` mutation.
 *
 * @export
 * @template T defines the component base props required
 * @param [withRef=false] true will allow one to get the instance
 * of your wrapped component from the higher-order GraphQL component
 * @returns the wrapped component
 */
export function graphqlUndoEventHistoryMutation<T extends BaseProps>(withRef: boolean = false) {
  return graphql(CacheMutations.undoEventHistoryMutation, {
    options: (props: T) => ({
      update: (store, result: CacheTypes.UndoEventHistoryMutationResult) => {
        const payload = result.data.undoEventHistory;
        updateApolloCacheFromDataPayload(
          store,
          props.currentTimeInterval,
          props.analystActivity,
          payload
        );
      }
    }),
    name: 'undoEventHistory',
    withRef
  });
}

/**
 * Returns a wrapped component providing the `redoEventHistory` mutation.
 *
 * @export
 * @template T defines the component base props required
 * @param [withRef=false] true will allow one to get the instance
 * of your wrapped component from the higher-order GraphQL component
 * @returns the wrapped component
 */
export function graphqlRedoEventHistoryMutation<T extends BaseProps>(withRef: boolean = false) {
  return graphql(CacheMutations.redoEventHistoryMutation, {
    options: (props: T) => ({
      update: (store, result: CacheTypes.RedoEventHistoryMutationResult) => {
        const payload = result.data.redoEventHistory;
        updateApolloCacheFromDataPayload(
          store,
          props.currentTimeInterval,
          props.analystActivity,
          payload
        );
      }
    }),
    name: 'redoEventHistory',
    withRef
  });
}

/**
 * Returns a wrapped component providing the `undoEventHistoryById` mutation.
 *
 * @export
 * @template T defines the component base props required
 * @param [withRef=false] true will allow one to get the instance
 * of your wrapped component from the higher-order GraphQL component
 * @returns the wrapped component
 */
export function graphqlUndoEventHistoryByIdMutation<T extends BaseProps>(withRef: boolean = false) {
  return graphql(CacheMutations.undoEventHistoryByIdMutation, {
    options: (props: T) => ({
      update: (store, result: CacheTypes.UndoEventHistoryByIdMutationResult) => {
        const payload = result.data.undoEventHistoryById;
        updateApolloCacheFromDataPayload(
          store,
          props.currentTimeInterval,
          props.analystActivity,
          payload
        );
      }
    }),
    name: 'undoEventHistoryById',
    withRef
  });
}

/**
 * Returns a wrapped component providing the `redoEventHistoryById` mutation.
 *
 * @export
 * @template T defines the component base props required
 * @param [withRef=false] true will allow one to get the instance
 * of your wrapped component from the higher-order GraphQL component
 * @returns the wrapped component
 */
export function graphqlRedoEventHistoryByIdMutation<T extends BaseProps>(withRef: boolean = false) {
  return graphql(CacheMutations.redoEventHistoryByIdMutation, {
    options: (props: T) => ({
      update: (store, result: CacheTypes.RedoEventHistoryByIdMutationResult) => {
        const payload = result.data.redoEventHistoryById;
        updateApolloCacheFromDataPayload(
          store,
          props.currentTimeInterval,
          props.analystActivity,
          payload
        );
      }
    }),
    name: 'redoEventHistoryById',
    withRef
  });
}

// ----- Event Mutations ------

/**
 * Returns a wrapped component providing the `createEvent` mutation.
 *
 * @export
 * @template T defines the component base props required
 * @param [withRef=false] true will allow one to get the instance
 * of your wrapped component from the higher-order GraphQL component
 * @returns the wrapped component
 */
export function graphqlCreateEventMutation<T extends BaseProps>(withRef: boolean = false) {
  return graphql(EventMutations.createEventMutation, {
    options: (props: T) => ({
      update: (store, result: EventTypes.CreateEventMutationResult) => {
        const payload = result.data.createEvent;
        updateApolloCacheFromDataPayload(
          store,
          props.currentTimeInterval,
          props.analystActivity,
          payload
        );
      }
    }),
    name: 'createEvent',
    withRef
  });
}

/**
 * Returns a wrapped component providing the `updateEvents` mutation.
 *
 * @export
 * @template T defines the component base props required
 * @param [withRef=false] true will allow one to get the instance
 * of your wrapped component from the higher-order GraphQL component
 * @returns the wrapped component
 */
export function graphqlUpdateEventsMutation<T extends BaseProps>(withRef: boolean = false) {
  return graphql(EventMutations.updateEventsMutation, {
    options: (props: T) => ({
      update: (store, result: EventTypes.UpdateEventsMutationResult) => {
        const payload = result.data.updateEvents;
        updateApolloCacheFromDataPayload(
          store,
          props.currentTimeInterval,
          props.analystActivity,
          payload
        );
      }
    }),
    name: 'updateEvents',
    withRef
  });
}

/**
 * Returns a wrapped component providing the `saveEvent` mutation.
 *
 * @export
 * @template T defines the component base props required
 * @param [withRef=false] true will allow one to get the instance
 * of your wrapped component from the higher-order GraphQL component
 * @returns the wrapped component
 */
export function graphqlSaveEventMutation<T extends BaseProps>(withRef: boolean = false) {
  return graphql(EventMutations.saveEventMutation, {
    options: (props: T) => ({
      update: (store, result: EventTypes.SaveEventMutationResult) => {
        const payload = result.data.saveEvent;
        updateApolloCacheFromDataPayload(
          store,
          props.currentTimeInterval,
          props.analystActivity,
          payload
        );
      }
    }),
    name: 'saveEvent',
    withRef
  });
}

/**
 * Returns a wrapped component providing the `updateFeaturePredictions` mutation.
 *
 * @export
 * @param [withRef=false] true will allow one to get the instance
 * of your wrapped component from the higher-order GraphQL component
 * @returns the wrapped component
 */
export function graphqlUpdateFeaturePredictionsMutation<T extends BaseProps>(
  withRef: boolean = false
) {
  return graphql(EventMutations.updateFeaturePredictionsMutation, {
    options: (props: T) => ({
      update: (store, result: EventTypes.UpdateFeaturePredictionsMutationResult) => {
        const payload = result.data.updateFeaturePredictions;
        updateApolloCacheFromDataPayload(
          store,
          props.currentTimeInterval,
          props.analystActivity,
          payload
        );
      }
    }),
    name: 'updateFeaturePredictions',
    withRef
  });
}

/**
 * Returns a wrapped component providing the `locateEvent` mutation.
 *
 * @export
 * @template T defines the component base props required
 * @param [withRef=false] true will allow one to get the instance
 * of your wrapped component from the higher-order GraphQL component
 * @returns the wrapped component
 */
export function graphqlLocateEventMutation<T extends BaseProps>(withRef: boolean = false) {
  return graphql(EventMutations.locateEventMutation, {
    options: (props: T) => ({
      update: (store, result: EventTypes.LocateEventMutationResult) => {
        const payload = result.data.locateEvent;
        updateApolloCacheFromDataPayload(
          store,
          props.currentTimeInterval,
          props.analystActivity,
          payload
        );
      }
    }),
    name: 'locateEvent',
    withRef
  });
}

/**
 * Returns a wrapped component providing the `computeNetworkMagnitudeSolution` mutation.
 *
 * @export
 * @template T defines the component base props required
 * @param [withRef=false] true will allow one to get the instance
 * of your wrapped component from the higher-order GraphQL component
 * @returns the wrapped component
 */
export function graphqlComputeNetworkMagnitudeSolutionMutation<T extends BaseProps>(
  withRef: boolean = false
) {
  return graphql(EventMutations.computeNetworkMagnitudeSolutionMutation, {
    options: (props: T) => ({
      update: (store, result: EventTypes.ComputeNetworkMagnitudeSolutionMutationResult) => {
        const payload = result.data.computeNetworkMagnitudeSolution.dataPayload;
        updateApolloCacheFromDataPayload(
          store,
          props.currentTimeInterval,
          props.analystActivity,
          payload
        );
      }
    }),
    name: 'computeNetworkMagnitudeSolution',
    withRef
  });
}

/**
 * Returns a wrapped component providing the `changeSignalDetectionAssociations` mutation.
 *
 * @export
 * @template T defines the component base props required
 * @param [withRef=false] true will allow one to get the instance
 * of your wrapped component from the higher-order GraphQL component
 * @returns the wrapped component
 */
export function graphqlChangeSignalDetectionsAssociationsMutation<T extends BaseProps>(
  withRef: boolean = false
) {
  return graphql(EventMutations.changeSignalDetectionAssociationsMutation, {
    options: (props: T) => ({
      update: (store, result: EventTypes.ChangeSignalDetectionAssociationsMutationResult) => {
        const payload = result.data.changeSignalDetectionAssociations;
        updateApolloCacheFromDataPayload(
          store,
          props.currentTimeInterval,
          props.analystActivity,
          payload
        );
      }
    }),
    name: 'changeSignalDetectionAssociations',
    withRef
  });
}

// ----- FK Mutations ------

/**
 * Returns a wrapped component providing the `computeFks` mutation.
 *
 * @export
 * @template T defines the component base props required
 * @param [withRef=false] true will allow one to get the instance
 * of your wrapped component from the higher-order GraphQL component
 * @returns the wrapped component
 */
export function graphqlComputeFksMutation<T extends BaseProps>(withRef: boolean = false) {
  return graphql(FkMutations.computeFksMutation, {
    options: (props: T) => ({
      update: (store, result: FkTypes.ComputeFksMutationResult) => {
        const payload = result.data.computeFks;
        updateApolloCacheFromDataPayload(
          store,
          props.currentTimeInterval,
          props.analystActivity,
          payload
        );
      }
    }),
    name: 'computeFks',
    withRef
  });
}

/**
 * Returns a wrapped component providing the `markFksReviewed` mutation.
 *
 * @export
 * @template T defines the component base props required
 * @param [withRef=false] true will allow one to get the instance
 * of your wrapped component from the higher-order GraphQL component
 * @returns the wrapped component
 */
export function graphqlMarkFksReviewedMutation<T extends BaseProps>(withRef: boolean = false) {
  return graphql(FkMutations.markFksReviewedMutation, {
    options: (props: T) => ({
      update: (store, result: FkTypes.MarkFksReviewedMutationResult) => {
        const payload = result.data.markFksReviewed;
        updateApolloCacheFromDataPayload(
          store,
          props.currentTimeInterval,
          props.analystActivity,
          payload
        );
      }
    }),
    name: 'markFksReviewed',
    withRef
  });
}

// ----- QC Mask Mutations ------

/**
 * Returns a wrapped component providing the `createQcMask` mutation.
 *
 * @export
 * @template T defines the component base props required
 * @param [withRef=false] true will allow one to get the instance
 * of your wrapped component from the higher-order GraphQL component
 * @returns the wrapped component
 */
export function graphqlCreateQcMaskMutation<T extends BaseProps>(withRef: boolean = false) {
  return graphql(QcMaskMutations.createQcMaskMutation, {
    options: (props: T) => ({
      update: (store, result: QcMaskTypes.CreateQcMaskMutationResult) => {
        const payload = result.data.createQcMask;
        updateApolloCacheFromDataPayload(
          store,
          props.currentTimeInterval,
          props.analystActivity,
          payload
        );
      }
    }),
    name: 'createQcMask',
    withRef
  });
}

/**
 * Returns a wrapped component providing the `updateQcMask` mutation.
 *
 * @export
 * @param [withRef=false] true will allow one to get the instance
 * of your wrapped component from the higher-order GraphQL component
 * @returns the wrapped component
 */
export function graphqlUpdateQcMaskMutation<T extends BaseProps>(withRef: boolean = false) {
  return graphql(QcMaskMutations.updateQcMaskMutation, {
    options: (props: T) => ({
      update: (store, result: QcMaskTypes.UpdateQcMaskMutationResult) => {
        const payload = result.data.updateQcMask;
        updateApolloCacheFromDataPayload(
          store,
          props.currentTimeInterval,
          props.analystActivity,
          payload
        );
      }
    }),
    name: 'updateQcMask',
    withRef
  });
}

/**
 * Returns a wrapped component providing the `rejectQcMask` mutation.
 *
 * @export
 * @param [withRef=false] true will allow one to get the instance
 * of your wrapped component from the higher-order GraphQL component
 * @returns the wrapped component
 */
export function graphqlRejectQcMaskMutation<T extends BaseProps>(withRef: boolean = false) {
  return graphql(QcMaskMutations.rejectQcMaskMutation, {
    options: (props: T) => ({
      update: (store, result: QcMaskTypes.RejectQcMaskMutationResult) => {
        const payload = result.data.rejectQcMask;
        updateApolloCacheFromDataPayload(
          store,
          props.currentTimeInterval,
          props.analystActivity,
          payload
        );
      }
    }),
    name: 'rejectQcMask',
    withRef
  });
}

// ----- Signal Detection Mutations ------

/**
 * Returns a wrapped component providing the `createDetection` mutation.
 *
 * @export
 * @template T defines the component base props required
 * @param [withRef=false] true will allow one to get the instance
 * of your wrapped component from the higher-order GraphQL component
 * @returns the wrapped component
 */
export function graphqlCreateDetectionMutation<T extends BaseProps>(withRef: boolean = false) {
  return graphql(SignalDetectionMutations.createDetectionMutation, {
    options: (props: T) => ({
      update: (store, result: SignalDetectionTypes.CreateDetectionMutationResult) => {
        const payload = result.data.createDetection;
        updateApolloCacheFromDataPayload(
          store,
          props.currentTimeInterval,
          props.analystActivity,
          payload
        );
      }
    }),
    name: 'createDetection',
    withRef
  });
}

/**
 * Returns a wrapped component providing the `updateDetections` mutation.
 *
 * @export
 * @template T defines the component base props required
 * @param [withRef=false] true will allow one to get the instance
 * of your wrapped component from the higher-order GraphQL component
 * @returns the wrapped component
 */
export function graphqlUpdateDetectionsMutation<T extends BaseProps>(withRef: boolean = false) {
  return graphql(SignalDetectionMutations.updateDetectionsMutation, {
    options: (props: T) => ({
      update: (store, result: SignalDetectionTypes.UpdateDetectionsMutationResult) => {
        const payload = result.data.updateDetections;
        updateApolloCacheFromDataPayload(
          store,
          props.currentTimeInterval,
          props.analystActivity,
          payload
        );
      }
    }),
    name: 'updateDetections',
    withRef
  });
}

/**
 * Returns a wrapped component providing the `rejectDetections` mutation.
 *
 * @export
 * @template T defines the component base props required
 * @param [withRef=false] true will allow one to get the instance
 * of your wrapped component from the higher-order GraphQL component
 * @returns the wrapped component
 */
export function graphqlRejectDetectionsMutation<T extends BaseProps>(withRef: boolean = false) {
  return graphql(SignalDetectionMutations.rejectDetectionsMutation, {
    options: (props: T) => ({
      update: (store, result: SignalDetectionTypes.RejectDetectionsMutationResult) => {
        const payload = result.data.rejectDetections;
        updateApolloCacheFromDataPayload(
          store,
          props.currentTimeInterval,
          props.analystActivity,
          payload
        );
      }
    }),
    name: 'rejectDetections',
    withRef
  });
}

/**
 * Returns a wrapped component providing the `markAmplitudeMeasurementReviewed` mutation.
 *
 * @export
 * @param [withRef=false] true will allow one to get the instance
 * of your wrapped component from the higher-order GraphQL component
 * @returns the wrapped component
 */
export function graphqlMarkAmplitudeMeasurementReviewed(withRef: boolean = false) {
  return graphql(SignalDetectionMutations.markAmplitudeMeasurementReviewedMutation, {
    name: 'markAmplitudeMeasurementReviewed',
    withRef
  });
}

// ----- Workflow Mutations ------

/**
 * Returns a wrapped component providing the `markActivityInterval` mutation.
 *
 * @export
 * @template T defines the component base props required
 * @param [withRef=false] true will allow one to get the instance
 * of your wrapped component from the higher-order GraphQL component
 * @returns the wrapped component
 */
export function graphqlMarkActivityIntervalMutation<T extends BaseProps>(withRef: boolean = false) {
  return graphql(WorkflowMutations.markActivityIntervalMutation, {
    options: (props: T) => ({
      update: (store, result: WorkflowTypes.MarkActivityIntervalMutationResult) => {
        const payload = result.data.markActivityInterval.dataPayload;
        updateApolloCacheFromDataPayload(
          store,
          props.currentTimeInterval,
          props.analystActivity,
          payload
        );
      }
    }),
    name: 'markActivityInterval',
    withRef
  });
}

/**
 * Returns a wrapped component providing the `markStageInterval` mutation.
 *
 * @export
 * @param [withRef=false] true will allow one to get the instance
 * of your wrapped component from the higher-order GraphQL component
 * @returns the wrapped component
 */
export function graphqlMarkStageIntervalMutation(withRef: boolean = false) {
  return graphql(WorkflowMutations.markStageIntervalMutation, { name: 'markStageInterval' });
}

/**
 * Returns a wrapped component providing the `setTimeInterval` mutation.
 *
 * @export
 * @param [withRef=false] true will allow one to get the instance
 * of your wrapped component from the higher-order GraphQL component
 * @returns the wrapped component
 */
export function graphqlSetTimeIntervalMutation(withRef: boolean = false) {
  return graphql(WorkflowMutations.setTimeIntervalMutation, { name: 'setTimeInterval' });
}

/**
 * Returns a wrapped component providing the `saveAllModifiedEvents` mutation.
 *
 * @export
 * @template T defines the component base props required
 * @param [withRef=false] true will allow one to get the instance
 * of your wrapped component from the higher-order GraphQL component
 * @returns the wrapped component
 */
export function graphqlSaveAllModifiedEvents<T extends BaseProps>(withRef: boolean = false) {
  return graphql(WorkflowMutations.saveAllModifiedEventsMutation, {
    options: (props: T) => ({
      update: (store, result: WorkflowTypes.SaveAllModifiedEventsMutationResult) => {
        const payload = result.data.saveAllModifiedEvents;
        updateApolloCacheFromDataPayload(
          store,
          props.currentTimeInterval,
          props.analystActivity,
          payload
        );
      }
    }),
    name: 'saveAllModifiedEvents',
    withRef
  });
}

// ----- Helper Functions ------

/**
 * Update the Apollo cache from the provided DataPayload.
 *
 * @export
 * @param store the apollo store
 * @param currentTimeInterval the current time interval
 * @param analystActivity the analyst activity
 * @param payload the data payload to update the cache
 */
export function updateApolloCacheFromDataPayload(
  store: DataProxy,
  currentTimeInterval: CommonTypes.TimeRange,
  analystActivity: AnalystWorkspaceTypes.AnalystActivity,
  payload: CacheTypes.DataPayload
) {
  // ======== Update Events ========

  // determine the event ids affected by the data payload
  const eventIdsUpdated = [...payload.events.map(e => e.id), ...payload.invalid.eventIds];

  if (eventIdsUpdated && eventIdsUpdated.length > 0) {
    // get time range for events
    const eventsTimeRange = systemConfig.getEventsTimeRange(currentTimeInterval, analystActivity);

    // get events from client cache
    const eventsInTimeRange: EventTypes.Event[] = [
      ...store.readQuery<{ eventsInTimeRange: EventTypes.Event[] }>({
        query: EventQueries.eventsInTimeRangeQuery,
        variables: { timeRange: eventsTimeRange }
      }).eventsInTimeRange
    ];

    // update the apollo cache for events
    store.writeQuery({
      query: EventQueries.eventsInTimeRangeQuery,
      variables: { timeRange: eventsTimeRange },
      data: {
        // add, update, or remove any events
        eventsInTimeRange: [
          ...eventsInTimeRange.filter(event => !includes(eventIdsUpdated, event.id)),
          ...payload.events
        ]
      }
    });
  }

  // ======== Update Signal Detections ========

  // determine the signal detection ids affected by the data payload
  const signalDetectionIdsUpdated = [
    ...payload.sds.map(sd => sd.id),
    ...payload.invalid.signalDetectionIds
  ];

  if (signalDetectionIdsUpdated && signalDetectionIdsUpdated.length > 0) {
    // get time range for the signal detection query
    const signalDetectionsTimeRange = systemConfig.getSignalDetectionTimeRange(
      currentTimeInterval,
      analystActivity
    );

    // get signal detections from client cache
    const signalDetectionsByStation: SignalDetectionTypes.SignalDetection[] = [
      ...store.readQuery<{ signalDetectionsByStation: SignalDetectionTypes.SignalDetection[] }>({
        query: SignalDetectionQueries.signalDetectionsQuery,
        variables: { timeRange: signalDetectionsTimeRange }
      }).signalDetectionsByStation
    ];

    // update the apollo cache for signal detections
    store.writeQuery({
      query: SignalDetectionQueries.signalDetectionsQuery,
      variables: {
        timeRange: signalDetectionsTimeRange
      },
      data: {
        // add, update, or remove any signal detections
        signalDetectionsByStation: [
          ...signalDetectionsByStation.filter(
            signalDetection => !includes(signalDetectionIdsUpdated, signalDetection.id)
          ),
          ...payload.sds
        ]
      }
    });
  }

  // ======== Update QC Masks ========

  if (payload.qcMasks && payload.qcMasks.length > 0) {
    // get time range for QC Masks
    const qcMasksTimeRange = systemConfig.getDefaultTimeRange(currentTimeInterval, analystActivity);

    // Get QC Masks from client cache
    const qcMasksByChannelName = [
      ...store.readQuery<{ qcMasksByChannelName: QcMaskTypes.QcMask[] }>({
        query: QcMaskQueries.qcMasksQuery,
        variables: { timeRange: qcMasksTimeRange }
      }).qcMasksByChannelName
    ];

    // filter out masks outside current interval and update or add to query data
    payload.qcMasks
      .filter(
        qcMask =>
          qcMask.currentVersion.endTime >= qcMasksTimeRange.startTime &&
          qcMask.currentVersion.startTime <= qcMasksTimeRange.endTime
      )
      .forEach(qcMask => {
        const index = qcMasksByChannelName.findIndex(mask => mask.id === qcMask.id);
        index >= 0 ? (qcMasksByChannelName[index] = qcMask) : qcMasksByChannelName.push(qcMask);
      });

    // update the apollo cache for qc masks
    store.writeQuery({
      query: QcMaskQueries.qcMasksQuery,
      variables: { timeRange: qcMasksTimeRange },
      data: { qcMasksByChannelName }
    });
  }

  // ======== Update Workspace State ========

  if (payload.workspaceState) {
    // update the apollo cache for workspace state
    const workspaceState = payload.workspaceState;
    store.writeQuery({
      query: CommonQueries.workspaceStateQuery,
      variables: {},
      data: { workspaceState }
    });
  }

  // ======== Update History ========

  if (payload.history) {
    // update the apollo cache for history
    store.writeQuery({
      query: CacheQueries.historyQuery,
      data: {
        // add, update, or remove any history
        history: [...payload.history]
      }
    });
  }
}
