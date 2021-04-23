import gql from 'graphql-tag';
import { dataPayloadFragment } from '../../cache/gqls';

/**
 * Input to the reject detection hypotheses mutation
 */
export interface RejectDetectionsInput {
  detectionIds: string[];
}

export const rejectDetectionsMutation = gql`
  mutation rejectDetections($detectionIds: [String]!) {
    rejectDetections(detectionIds: $detectionIds) {
      ...DataPayloadFragment
    }
  }
  ${dataPayloadFragment}
`;
