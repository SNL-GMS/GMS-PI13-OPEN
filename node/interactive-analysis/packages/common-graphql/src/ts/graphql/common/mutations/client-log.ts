import gql from 'graphql-tag';

/**
 * Locate Event Mutation Definition
 */
export const clientLogMutation = gql`
  mutation clientLog($logs: [ClientLogInput]) {
    clientLog(logs: $logs) @connection(key: "clientLog") {
      logLevel
    }
  }
`;
