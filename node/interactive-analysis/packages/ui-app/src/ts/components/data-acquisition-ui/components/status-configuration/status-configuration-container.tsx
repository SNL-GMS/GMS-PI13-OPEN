import { compose } from '@gms/common-util';
import { ReactApolloQueries } from '~data-acquisition-ui/react-apollo-components';
import { StatusConfigurations } from './status-configuration-component';

/**
 * A new apollo component, that's wrapping the StationInformation component and injecting
 * apollo graphQL queries and mutations.
 */
export const ApolloStatusConfigurationContainer = compose(
  ReactApolloQueries.graphqlDefaultReferenceStationsQuery()
)(StatusConfigurations);
