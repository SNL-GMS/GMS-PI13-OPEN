import gql from 'graphql-tag';

export const historicalSohByStationQuery = gql`
  query historicalSohByStation($queryInput: UiHistoricalSohInput!) {
    historicalSohByStation(queryInput: $queryInput) {
      stationName
      calculationTimes
      monitorValues {
        channelName
        valuesByType {
          LAG {
            type
            values
          }
          MISSING {
            type
            values
          }
        }
      }
    }
  }
`;
