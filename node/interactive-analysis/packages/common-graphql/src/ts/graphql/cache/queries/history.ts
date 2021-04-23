import gql from 'graphql-tag';
import { historyFragment } from '../gqls';

/**
 * Get global history query.
 */
export const historyQuery = gql`
  query History {
    history {
      ...HistoryFragment
    }
  }
  ${historyFragment}
`;
