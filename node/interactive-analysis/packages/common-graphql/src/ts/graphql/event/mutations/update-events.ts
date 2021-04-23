import gql from 'graphql-tag';
import { dataPayloadFragment } from '../../cache/gqls';

export const updateEventsMutation = gql`
  mutation updateEvents($eventIds: [String]!, $input: UpdateEventInput!) {
    updateEvents(eventIds: $eventIds, input: $input) {
      ...DataPayloadFragment
    }
  }
  ${dataPayloadFragment}
`;
