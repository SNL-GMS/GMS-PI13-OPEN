import { compose } from '@gms/common-util';
import { ReactApolloQueries } from '~data-acquisition-ui/react-apollo-components';
import { TransferGaps } from './transfer-gaps-component';

/**
 * A new apollo component, that's wrapping the TransferGaps component
 * and apollo graphQL queries.
 */
export const ApolloTransferGapsContainer = compose(
  ReactApolloQueries.graphqlTransferredFilesByTimeRangeQueryQuery()
)(TransferGaps);
