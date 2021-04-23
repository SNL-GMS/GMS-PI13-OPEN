import gql from 'graphql-tag';
import { dataPayloadFragment } from '../../cache/gqls';

export const updateQcMaskMutation = gql`
  mutation updateQcMask($maskId: String!, $input: QcMaskInput!) {
    updateQcMask(qcMaskId: $maskId, input: $input) {
      ...DataPayloadFragment
    }
  }
  ${dataPayloadFragment}
`;
