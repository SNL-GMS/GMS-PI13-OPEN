import gql from 'graphql-tag';
import { dataPayloadFragment } from '../../cache/gqls';

/**
 * Locate Event Mutation Definition
 */
export const saveEventMutation = gql`
  mutation saveEvent($eventId: String!) {
    saveEvent(eventId: $eventId) {
      ...DataPayloadFragment
    }
  }
  ${dataPayloadFragment}
`;
