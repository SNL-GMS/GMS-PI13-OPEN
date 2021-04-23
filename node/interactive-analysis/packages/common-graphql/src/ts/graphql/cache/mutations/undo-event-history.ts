import gql from 'graphql-tag';
import { dataPayloadFragment } from '../../cache/gqls';

/**
 * The undo event history mutation
 */
export const undoEventHistoryMutation = gql`
  mutation undoEventHistory($numberOfItems: Float!) {
    undoEventHistory(numberOfItems: $numberOfItems) {
      ...DataPayloadFragment
    }
  }
  ${dataPayloadFragment}
`;
