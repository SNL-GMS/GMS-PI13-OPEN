import gql from 'graphql-tag';
import { dataPayloadFragment } from '../../cache/gqls';

export const createEventMutation = gql`
  mutation createEvent($signalDetectionIds: [String]!) {
    createEvent(signalDetectionIds: $signalDetectionIds) {
      ...DataPayloadFragment
    }
  }
  ${dataPayloadFragment}
`;
