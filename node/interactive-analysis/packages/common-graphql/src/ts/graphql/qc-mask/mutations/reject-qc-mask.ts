import gql from 'graphql-tag';
import { dataPayloadFragment } from '../../cache/gqls';

export const rejectQcMaskMutation = gql`
  mutation rejectQcMask($maskId: String!, $inputRationale: String!) {
    rejectQcMask(qcMaskId: $maskId, rationale: $inputRationale) {
      ...DataPayloadFragment
    }
  }
  ${dataPayloadFragment}
`;
