import gql from 'graphql-tag';
import { dataPayloadFragment } from '../../cache/gqls';

/**
 * The redo history mutation by id
 */
export const redoHistoryByIdMutation = gql`
  mutation redoHistoryById($id: String!) {
    redoHistoryById(id: $id) {
      ...DataPayloadFragment
    }
  }
  ${dataPayloadFragment}
`;
