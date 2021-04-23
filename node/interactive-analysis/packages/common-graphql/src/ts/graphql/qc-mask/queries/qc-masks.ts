import gql from 'graphql-tag';
import { qcMaskFragment } from '../gqls';

/**
 * Graphql query for QC masks by channel id.
 *
 * NOTE: The apollo cache is configured to use the a unique cache key and filter.
 */
export const qcMasksByChannelNameQuery = gql`
  query qcMasksByChannelName($timeRange: TimeRange!, $channelNames: [String]) {
    qcMasksByChannelName(timeRange: $timeRange, channelNames: $channelNames)
      @connection(key: "qcMasksByChannelName", filter: ["timeRange"]) {
      ...QcMaskFragment
    }
  }
  ${qcMaskFragment}
`;

/**
 * Graphql query for QC masks by channel id.
 *
 * WARNING: This query should only be used to query the internal apollo cache. The
 * channel IDs are not needed for this query, because it queries the cache using the
 * custom cache key and filter.
 *
 * NOTE: The apollo cache is configured to use the a unique cache key and filter.
 */
export const qcMasksQuery = gql`
  query qcMasksByChannelName($timeRange: TimeRange!) {
    qcMasksByChannelName(timeRange: $timeRange)
      @connection(key: "qcMasksByChannelName", filter: ["timeRange"]) {
      ...QcMaskFragment
    }
  }
  ${qcMaskFragment}
`;
