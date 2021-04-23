import { SohMonitorType } from '@gms/common-graphql/lib/graphql/soh/types';
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

const buildQuery = (types: SohMonitorType[]): string =>
  `query historicalSohByStationQuery {
    historicalSohByStation(queryInput:
      {
        stationName: "AAK",
        startTime: 1274392801,
        endTime: 1274400000,
        sohMonitorTypes: [${String(types)}]
      } ) {
      stationName
      monitorValues {
        channelName
        valuesByType {
          LAG {
            type
          }
          MISSING {
            type
          }
        }
      }
    }
  }
  `;

describe('Fetch SOH Historical data', () => {
  it('loads station soh historical soh LAG and MISSING data', async () => {
    // Execute the GraphQL query
    const query = buildQuery([SohMonitorType.LAG, SohMonitorType.MISSING]);
    const rootValue = {};
    const result = await graphql(schema, query, rootValue, userContext);
    const { data } = result;
    expect(data).toMatchSnapshot();
  });

  it('loads station soh historical soh only LAG data', async () => {
    // Execute the GraphQL query
    const query = buildQuery([SohMonitorType.LAG]);
    const rootValue = {};
    const result = await graphql(schema, query, rootValue, userContext);
    const { data } = result;
    expect(data).toMatchSnapshot();
  });

  it('loads station soh historical soh only MISSING data', async () => {
    // Execute the GraphQL query
    const query = buildQuery([SohMonitorType.MISSING]);
    const rootValue = {};
    const result = await graphql(schema, query, rootValue, userContext);
    const { data } = result;
    expect(data).toMatchSnapshot();
  });

  it('loads station soh historical NO soh data', async () => {
    // Execute the GraphQL query using an env entry
    const query = buildQuery([SohMonitorType.ENV_CLIPPED]);
    const rootValue = {};
    const result = await graphql(schema, query, rootValue, userContext);
    const { data } = result;
    expect(data).toMatchSnapshot();
  });
});
