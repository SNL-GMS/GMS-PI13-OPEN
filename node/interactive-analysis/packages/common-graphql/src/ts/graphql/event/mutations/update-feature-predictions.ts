import gql from 'graphql-tag';
import { dataPayloadFragment } from '../../cache/gqls';

export const updateFeaturePredictionsMutation = gql`
  mutation updateFeaturePredictionsMutation($eventId: String!) {
    updateFeaturePredictions(eventId: $eventId) {
      ...DataPayloadFragment
    }
  }
  ${dataPayloadFragment}
`;
