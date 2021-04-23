import gql from 'graphql-tag';
import { dataPayloadFragment } from '../../cache/gqls';

/**
 * The undo history mutation
 */
export const undoHistoryMutation = gql`
  mutation undoHistory($numberOfItems: Float!) {
    undoHistory(numberOfItems: $numberOfItems) {
      ...DataPayloadFragment
    }
  }
  ${dataPayloadFragment}
`;
