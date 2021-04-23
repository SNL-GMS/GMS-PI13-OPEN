import { DataAcquisitionMutations, SohMutations } from '@gms/common-graphql';
import { graphql } from 'react-apollo';

// ----- Data Acquisition Mutations ------

/**
 * Returns a wrapped component providing the `saveReferenceStation` mutation.
 *
 * @export
 * @template T defines the component base props required
 * @param [withRef=false] true will allow one to get the instance
 * of your wrapped component from the higher-order GraphQL component
 * @returns the wrapped component
 */
export function graphqlSaveReferenceStationMutation<T>(withRef: boolean = false) {
  return graphql(DataAcquisitionMutations.saveReferenceStationMutation, {
    name: 'saveReferenceStation'
  });
}

// ----- SOH Status Mutations ------

/**
 * Returns a wrapped component providing the `saveStationGroupSohStatus` mutation.
 *
 * @export
 * @template T defines the component base props required
 * @param [withRef=false] true will allow one to get the instance
 * of your wrapped component from the higher-order GraphQL component
 * @returns the wrapped component
 */
export function graphqlSaveStationGroupSohStatusMutation<T>(withRef: boolean = false) {
  return graphql(SohMutations.saveStationGroupSohStatusMutation, {
    name: 'saveStationGroupSohStatus',
    withRef
  });
}

/**
 * Returns a wrapped component providing the `acknowledgeSohStatus` mutation.
 *
 * @export
 * @template T defines the component base props required
 * @param [withRef=false] true will allow one to get the instance
 * of your wrapped component from the higher-order GraphQL component
 * @returns the wrapped component
 */
export function graphqlAcknowledgeSohStatusMutation<T>(withRef: boolean = false) {
  return graphql(SohMutations.acknowledgeSohStatusMutation, {
    name: 'acknowledgeSohStatus',
    withRef
  });
}

/**
 * Returns a wrapped component providing the `quietChannelMonitorStatuses` mutation.
 *
 * @export
 * @template T defines the component base props required
 * @param [withRef=false] true will allow one to get the instance
 * of your wrapped component from the higher-order GraphQL component
 * @returns the wrapped component
 */
export function graphqlQuietChannelMonitorStatusesMutation<T>(withRef: boolean = false) {
  return graphql(SohMutations.quietChannelMonitorStatusesMutation, {
    name: 'quietChannelMonitorStatuses',
    withRef
  });
}
