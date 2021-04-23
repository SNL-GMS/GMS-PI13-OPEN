import gql from 'graphql-tag';
import { dataPayloadFragment } from '../../cache/gqls';

export const updateDetectionsMutation = gql`
  mutation updateDetections($detectionIds: [String]!, $input: UpdateDetectionInput!) {
    updateDetections(detectionIds: $detectionIds, input: $input) {
      ...DataPayloadFragment
    }
  }
  ${dataPayloadFragment}
`;
