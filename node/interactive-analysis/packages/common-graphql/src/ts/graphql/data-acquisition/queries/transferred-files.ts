import gql from 'graphql-tag';
import { fileGapsFragment } from '../gqls';

export const transferredFilesByTimeRangeQuery = gql`
  query transferredFilesByTimeRange($timeRange: TimeRange!) {
    transferredFilesByTimeRange(timeRange: $timeRange) {
      ...FileGapsFragment
    }
  }
  ${fileGapsFragment}
`;
