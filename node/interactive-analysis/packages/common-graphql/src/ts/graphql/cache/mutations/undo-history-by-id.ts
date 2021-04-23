import gql from 'graphql-tag';
import { dataPayloadFragment } from '../../cache/gqls';

/**
 * The undo history mutation by id
 */
export const undoHistoryByIdMutation = gql`
  mutation undoHistoryById($id: String!) {
    undoHistoryById(id: $id) {
      ...DataPayloadFragment
    }
  }
  ${dataPayloadFragment}
`;
