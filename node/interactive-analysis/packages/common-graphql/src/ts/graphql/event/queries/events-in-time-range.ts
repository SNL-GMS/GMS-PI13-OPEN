import gql from 'graphql-tag';
import { eventFragment } from '../gqls';

export const eventsInTimeRangeQuery = gql`
  query eventsInTimeRange($timeRange: TimeRange!) {
    eventsInTimeRange(timeRange: $timeRange) {
      ...EventFragment
    }
  }
  ${eventFragment}
`;
