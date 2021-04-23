import gql from 'graphql-tag';
import { stationAndStationGroupSohFragment } from '../gqls';

/**
 * SOH Status Subscriptions GQL.
 * Defines the data to be queried for the sohStatus subscription.
 */
export const sohStatusSubscription = gql`
  subscription sohStatus {
    sohStatus {
      ...StationAndStationGroupSohFragment
    }
  }
  ${stationAndStationGroupSohFragment}
`;
