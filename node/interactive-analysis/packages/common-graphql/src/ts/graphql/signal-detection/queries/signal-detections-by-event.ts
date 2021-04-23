import gql from 'graphql-tag';
import { signalDetectionFragment } from '../gqls';

export const signalDetectionsByEventIdQuery = gql`
  query signalDetectionsByEventId($eventId: String!) {
    signalDetectionsByEventId(eventId: $eventId) {
      ...SignalDetectionFragment
    }
  }
  ${signalDetectionFragment}
`;
