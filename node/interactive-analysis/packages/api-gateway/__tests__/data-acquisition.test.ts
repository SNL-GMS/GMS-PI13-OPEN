import { delayExecution } from '@gms/common-util';
import { graphql } from 'graphql';
import sortBy from 'lodash/sortBy';
import { schema } from '../src/ts/server/api-gateway-schema';

const delayMs = 500;
const timeRange = {
  startTime: 1274385600,
  endTime: 1274400000
};

// ---- Query test cases ----

// Test case - basic content query for data acquisition
// Query for transferred files by time range
it('Transferred Files Content query should match snapshot', async () => {
  // language=GraphQL
  const query = `
  query transferredFilesByTimeRange {
    transferredFilesByTimeRange(timeRange: {startTime: ${timeRange.startTime}, endTime: ${timeRange.endTime}}) {
      stationName
      channelNames
      startTime
      endTime
      duration
      location
      priority
    }
  }
  `;

  const rootValue = {};
  // Execution the GraphQL query with a small delay to allow the API gateway to settle async
  // HTTP requests fetching data from the mock backend
  const result = await delayExecution(async () => graphql(schema, query, rootValue), delayMs);
  const { data } = result;

  // Compare response to snapshot
  expect(sortBy(data.transferredFilesByTimeRange, 'stationName')).toMatchSnapshot();
});
