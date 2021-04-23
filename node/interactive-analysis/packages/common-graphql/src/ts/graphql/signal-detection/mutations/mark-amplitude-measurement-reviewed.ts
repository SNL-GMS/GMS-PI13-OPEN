import gql from 'graphql-tag';
import { dataPayloadFragment } from '../../cache/gqls';

export const markAmplitudeMeasurementReviewedMutation = gql`
  mutation markAmplitudeMeasurementReviewed($signalDetectionIds: [String]!) {
    markAmplitudeMeasurementReviewed(signalDetectionIds: $signalDetectionIds) {
      ...DataPayloadFragment
    }
  }
  ${dataPayloadFragment}
`;
