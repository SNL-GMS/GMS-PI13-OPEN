import gql from 'graphql-tag';
import { dataPayloadFragment } from '../../cache/gqls';

export const computeFksMutation = gql`
  mutation computeFks($fkInput: [FkInput]!) {
    computeFks(fkInput: $fkInput) {
      ...DataPayloadFragment
    }
  }
  ${dataPayloadFragment}
`;
