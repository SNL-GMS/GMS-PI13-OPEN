import { UserProfileMutations } from '@gms/common-graphql';
import { graphql } from 'react-apollo';

// ----- User Profile Mutations ------

/**
 * Returns a wrapped component providing the `setLayout` mutation.
 *
 * @export
 * @param [withRef=false] true will allow one to get the instance
 * of your wrapped component from the higher-order GraphQL component
 * @returns the wrapped component
 */
export function graphqlSetLayoutMutation(withRef: boolean = false) {
  return graphql(UserProfileMutations.setLayoutMutation, { name: 'setLayout' });
}

/**
 * Returns a wrapped component providing the `setAudibleNotifications` mutation.
 *
 * @export
 * @param [withRef=false] true will allow one to get the instance
 * of your wrapped component from the higher-order GraphQL component
 * @returns the wrapped component
 */
export function graphqlSetAudibleNotificationsMutation(withRef: boolean = false) {
  return graphql(UserProfileMutations.setAudibleNotificationsMutation, {
    name: 'setAudibleNotifications'
  });
}
