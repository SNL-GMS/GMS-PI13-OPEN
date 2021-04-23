import { UserProfileQueries, UserProfileTypes } from '@gms/common-graphql';
import { SystemMessageQueries } from '@gms/common-graphql/lib/graphql/system-message';
import { IS_INTERACTIVE_ANALYSIS_MODE_SOH } from '@gms/common-util';
import { graphql } from 'react-apollo';

// ----- System Message Queries ------

/**
 * Returns a wrapped component providing the `systemMessageDefinitionsQuery` query.
 *
 * @export
 * @returns the wrapped component
 */
export function graphqlSystemMessageDefinitionsQuery() {
  return graphql(SystemMessageQueries.systemMessageDefinitionsQuery, {
    name: 'systemMessageDefinitionsQuery'
  });
}

// ----- User Profile Queries ------

/**
 * Returns a wrapped component providing the `userProfileQuery` query.
 *
 * @export
 * @returns the wrapped component
 */
export function graphqlUserProfileQuery<T extends UserProfileTypes.UserProfileQueryArguments>() {
  return graphql(UserProfileQueries.userProfileQuery, {
    name: 'userProfileQuery',
    options: (props: T) => {
      const variables: UserProfileTypes.UserProfileQueryArguments = {
        defaultLayoutName: IS_INTERACTIVE_ANALYSIS_MODE_SOH
          ? UserProfileTypes.DefaultLayoutNames.SOH_LAYOUT
          : UserProfileTypes.DefaultLayoutNames.ANALYST_LAYOUT
      };
      return {
        variables
      };
    }
  });
}
