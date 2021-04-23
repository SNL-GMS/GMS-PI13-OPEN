import gql from 'graphql-tag';
import { historyFragment } from '../gqls';

/**
 * Get event history query.
 */
export const eventHistoryQuery = gql`
  query eventHistory($id: String!) {
    eventHistory(id: $id) {
      ...HistoryFragment
    }
  }
  ${historyFragment}
`;
