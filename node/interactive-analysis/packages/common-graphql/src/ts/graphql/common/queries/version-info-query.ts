import gql from 'graphql-tag';

export const versionInfoQuery = gql`
  query versionInfo {
    versionInfo {
      versionNumber
      commitSHA
    }
  }
`;
