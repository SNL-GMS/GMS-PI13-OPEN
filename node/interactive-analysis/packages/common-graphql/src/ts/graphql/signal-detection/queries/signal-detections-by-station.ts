import gql from 'graphql-tag';
import { signalDetectionFragment } from '../gqls';

/**
 * Graphql query for signal setections by stations.
 *
 * NOTE: The apollo cache is configured to use the a unique cache key and filter.
 */
export const signalDetectionsByStationQuery = gql`
  query signalDetectionsByStation($timeRange: TimeRange!, $stationIds: [String]!) {
    signalDetectionsByStation(timeRange: $timeRange, stationIds: $stationIds)
      @connection(key: "signalDetectionsByStation", filter: ["timeRange"]) {
      ...SignalDetectionFragment
    }
  }
  ${signalDetectionFragment}
`;

/**
 * Graphql query for signal setections by stations.
 *
 * WARNING: This query should only be used to query the internal apollo cache. The
 * station IDs are not needed for this query, because it queries the cache using the
 * custom cache key and filter.
 *
 * NOTE: The apollo cache is configured to use the a unique cache key and filter.
 */
export const signalDetectionsQuery = gql`
  query signalDetectionsByStation($timeRange: TimeRange!) {
    signalDetectionsByStation(timeRange: $timeRange)
      @connection(key: "signalDetectionsByStation", filter: ["timeRange"]) {
      ...SignalDetectionFragment
    }
  }
  ${signalDetectionFragment}
`;
