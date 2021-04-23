import gql from 'graphql-tag';
import { analystConfigurationFragment } from '../gqls';
/**
 * Get event history query.
 */
export const uiAnalystConfigurationQuery = gql`
  query uiAnalystConfiguration {
    uiAnalystConfiguration {
      ...AnalystConfigurationFragment
    }
  }
  ${analystConfigurationFragment}
`;
