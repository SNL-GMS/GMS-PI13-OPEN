import {
  DataAcquisitionQueries,
  DataAcquisitionTypes,
  ReferenceStationQueries
} from '@gms/common-graphql';
import { MILLISECONDS_IN_SECOND } from '@gms/common-util';
import { graphql } from 'react-apollo';
import { dataAcquisitionUserPreferences } from '../config';

// ----- Data Acquisition Queries ------

/**
 * Returns a wrapped component providing the `transferredFilesByTimeRangeQuery` query.
 *
 * @export
 * @template T defines the component base props required
 * @returns the wrapped component
 */
export function graphqlTransferredFilesByTimeRangeQueryQuery<T>() {
  return graphql(DataAcquisitionQueries.transferredFilesByTimeRangeQuery, {
    options: (props: T) => {
      const now = new Date(Date.now());
      const twoDaysAgo = new Date();
      const daysAsHours = 12;
      twoDaysAgo.setHours(now.getHours() - daysAsHours);
      const variables: DataAcquisitionTypes.TransferredFilesByTimeRangeQueryArgs = {
        timeRange: {
          startTime: twoDaysAgo.getTime() / MILLISECONDS_IN_SECOND,
          endTime: now.getTime() / MILLISECONDS_IN_SECOND
        }
      };
      const pollInterval = dataAcquisitionUserPreferences.transferredFilesGapsUpdateTime;
      return {
        variables,
        pollInterval,
        fetchPolicy: 'network-only'
      };
    },
    name: 'transferredFilesByTimeRangeQuery'
  });
}

// ----- SOH Status Queries ------

// ----- Reference Processing Queries ------

/**
 * Returns a wrapped component providing the `defaultStationsQuery` query.
 *
 * @export
 * @returns the wrapped component
 */
export function graphqlDefaultReferenceStationsQuery() {
  return graphql(ReferenceStationQueries.defaultReferenceStationsQuery, {
    name: 'defaultStationsQuery'
  });
}
