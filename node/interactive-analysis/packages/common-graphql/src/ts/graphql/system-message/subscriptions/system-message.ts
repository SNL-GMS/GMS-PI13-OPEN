import gql from 'graphql-tag';
import { systemMessageFragment } from '../gqls';

/**
 * Defines the subscription for system messages.
 */
export const systemMessageSubscription = gql`
  subscription systemMessages {
    systemMessages {
      ...SystemMessageFragment
    }
  }
  ${systemMessageFragment}
`;
