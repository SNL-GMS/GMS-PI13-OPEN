import gql from 'graphql-tag';
import { systemMessageDefinitionFragment } from '../gqls';

/**
 * Gets the system message definitions
 */
export const systemMessageDefinitionsQuery = gql`
  query systemMessageDefinitions {
    systemMessageDefinitions {
      ...SystemMessageDefinitionFragment
    }
  }
  ${systemMessageDefinitionFragment}
`;
