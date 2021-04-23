import { graphql } from 'graphql';
import { schema } from '../src/ts/server/api-gateway-schema';

// ---- Query test cases ----

// Test case - basic content query for network SOH status
// Query for network SOH status by time range
it('Network SOH Status query should match snapshot', async () => {
  // language=GraphQL
  const sohStatusQuery = `
    query networkSohStatus($startTime: String, $endTime: String) {
      networkSohStatus(startTime: $startTime, endTime: $endTime) {
        id
        networkId
        networkName
        startTime
        endTime
        sohStatusSummary
        stationSohStatus [{
          stationId
          stationName
          sohStatusSummary
          sohStatus {
            stationAcquisitionSohStatus {
              completeness
              completenessSummary
              lag
              lagSummary
            }
            environmentSohStatus {
              countBySohType
              summaryBySohType
            }
          }
          stationSohIssue {
            requiresAcknowledgement
            acknowledgedAt
          }
          channelSohStatus {
            channelId
            channelName
            sohStatus {
              stationAcquisitionSohStatus {
                completeness
                completenessSummary
                lag
                lagSummary
              }
              environmentSohStatus {
                countBySohType
                summaryBySohType
              }
            }
          }
        }]
      }
    }
  `;

  const rootValue = {};
  // Execution the GraphQL query with a small delay to allow the API gateway to settle async
  // HTTP requests fetching data from the mock backend
  const result = await graphql(schema, sohStatusQuery, rootValue);

  const { data } = result;

  // Compare response to snapshot
  expect(data).toMatchSnapshot();
});
