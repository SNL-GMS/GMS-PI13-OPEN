import { UserProfileMutations, UserProfileQueries, UserProfileTypes } from '@gms/common-graphql';
import { IS_INTERACTIVE_ANALYSIS_MODE_SOH } from '@gms/common-util';
import { graphql } from 'react-apollo';

// ----- User Profile Mutations ------

/**
 * Returns a wrapped component providing the `setAudibleNotifications` mutation.
 *
 * @export
 * @param [withRef=false] true will allow one to get the instance
 * of your wrapped component from the higher-order GraphQL component
 * @returns the wrapped component
 */
export function graphqlSetAudibleNotificationsMutation<T extends UserProfileTypes.UserProfileProps>(
  withRef: boolean = false
) {
  return graphql(UserProfileMutations.setAudibleNotificationsMutation, {
    options: (props: T) => ({
      update: (
        store,
        result: {
          data: {
            setAudibleNotifications: {
              audibleNotifications: UserProfileTypes.AudibleNotification[];
            };
          };
        }
      ) => {
        const payload = result.data.setAudibleNotifications.audibleNotifications;
        const variables: UserProfileTypes.UserProfileQueryArguments = {
          defaultLayoutName: IS_INTERACTIVE_ANALYSIS_MODE_SOH
            ? UserProfileTypes.DefaultLayoutNames.SOH_LAYOUT
            : UserProfileTypes.DefaultLayoutNames.ANALYST_LAYOUT
        };
        store.writeQuery<{
          userProfile: UserProfileTypes.UserProfile;
        }>({
          query: UserProfileQueries.userProfileQuery,
          variables,
          data: {
            userProfile: {
              ...props.userProfileQuery.userProfile,
              audibleNotifications: payload
            }
          }
        });
      }
    }),
    name: 'setAudibleNotifications'
  });
}
