import {
  CommonQueries,
  ConfigurationQueries,
  UserProfileQueries,
  UserProfileTypes
} from '@gms/common-graphql';
import { IS_INTERACTIVE_ANALYSIS_MODE_SOH } from '@gms/common-util';
import { graphql } from 'react-apollo';

// ----- Common Queries ------

/**
 * Returns a wrapped component providing the `versionInfoQuery` query.
 *
 * @export
 * @returns the wrapped component
 */
export function graphqlVersionInfoQuery() {
  return graphql(CommonQueries.versionInfoQuery, { name: 'versionInfoQuery' });
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

// ----- UI Configuration Queries ------

/**
 * Returns a wrapped component providing the `UIConfiguration` query.
 *
 * @export
 * @template T defines the component base props required
 * @returns the wrapped component
 */
export function graphqlUIConfigurationQuery<T>() {
  return graphql(ConfigurationQueries.uiConfigurationQuery, { name: 'uiConfigurationQuery' });
}
