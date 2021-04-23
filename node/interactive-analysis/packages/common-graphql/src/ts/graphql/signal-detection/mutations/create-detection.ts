import gql from 'graphql-tag';
import { dataPayloadFragment } from '../../cache/gqls';

export const createDetectionMutation = gql`
  mutation createDetection($input: NewDetectionInput!) {
    createDetection(input: $input) {
      ...DataPayloadFragment
    }
  }
  ${dataPayloadFragment}
`;
