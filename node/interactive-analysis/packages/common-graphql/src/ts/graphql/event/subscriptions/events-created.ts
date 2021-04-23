import gql from 'graphql-tag';
import { eventFragment } from '../gqls';

export const eventsCreatedSubscription = gql`
  subscription eventsCreated {
    eventsCreated {
      ...EventFragment
    }
  }
  ${eventFragment}
`;
