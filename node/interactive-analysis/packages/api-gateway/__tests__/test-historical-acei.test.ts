import { graphql } from 'graphql';
import { ConfigurationProcessor } from '../src/ts/configuration/configuration-processor';
import { schema } from '../src/ts/server/api-gateway-schema';
import { SohProcessor } from '../src/ts/soh/soh-processor';
import { ProcessingStationProcessor } from '../src/ts/station/processing-station/processing-station-processor';
import { userContext } from './__data__/user-profile-data';

// tslint:disable-next-line: no-magic-numbers
Date.now = jest.fn().mockReturnValue(1575410988600);

/**
 * Sets up test by loading SOH data
 */
beforeEach(async () => setupTest());
async function setupTest() {
  await ConfigurationProcessor.Instance().fetchConfiguration();
  await ProcessingStationProcessor.Instance().fetchStationData();
  SohProcessor.Instance();
}

const buildQuery = (): string =>
  `query historicalAceiByStationQuery {
    historicalAceiByStation(queryInput:
      {
        stationName: "AAK",
        startTime: 1274392801,
        endTime: 1274400000,
        type: CLIPPED
      } ) {
      channelName
      monitorType
      issues
    }
  }
  `;

describe('Fetch ACEI Historical data', () => {
  it('loads station and monitorType acei historical data', async () => {
    // Execute the GraphQL query
    const query = buildQuery();
    const rootValue = {};
    const result = await graphql(schema, query, rootValue, userContext);
    const { data } = result;

    expect(data.historicalAceiByStation[0].channelName).toEqual('AAK.AAK.BHN');
    expect(data.historicalAceiByStation[0].monitorType).toEqual('CLIPPED');
    expect(data.historicalAceiByStation[0].issues.length).toEqual(1);
    expect(data.historicalAceiByStation[0].issues[0].length).toBeGreaterThan(1);
  });
});
