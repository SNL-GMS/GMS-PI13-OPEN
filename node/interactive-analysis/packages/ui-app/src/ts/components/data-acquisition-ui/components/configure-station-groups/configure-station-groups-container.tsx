import { compose } from '@gms/common-util';
import { ReactApolloQueries } from '~data-acquisition-ui/react-apollo-components';
import { ConfigureStationGroups } from './configure-station-groups-component';

export const ApolloConfigureStationGroupsContainer: React.ComponentClass<Pick<{}, never>> = compose(
  ReactApolloQueries.graphqlDefaultReferenceStationsQuery()
)(ConfigureStationGroups);
