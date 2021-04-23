import gql from 'graphql-tag';

export const historicalAceiByStationQuery = gql`
  query historicalAceiByStation($queryInput: UiHistoricalAceiInput!) {
    historicalAceiByStation(queryInput: $queryInput) {
      channelName
      monitorType
      issues
    }
  }
`;
