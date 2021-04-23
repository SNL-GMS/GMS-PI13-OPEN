import gql from 'graphql-tag';
import { dataPayloadFragment } from '../../cache/gqls';

export const saveAllModifiedEventsMutation = gql`
  mutation saveAllModifiedEvents {
    saveAllModifiedEvents {
      ...DataPayloadFragment
    }
  }
  ${dataPayloadFragment}
`;
