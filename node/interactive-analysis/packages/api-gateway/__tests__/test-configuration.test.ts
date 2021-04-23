import { graphql } from 'graphql';
import { ConfigurationProcessor } from '../src/ts/configuration/configuration-processor';
import { schema } from '../src/ts/server/api-gateway-schema';
import { ProcessingStationProcessor } from '../src/ts/station/processing-station/processing-station-processor';
import { userContext } from './__data__/user-profile-data';

beforeAll(async () => setupTest());

/**
 * Sets up test by loading SDs
 */
async function setupTest() {
  // Simulates an initial login request to fetch configuration
  await ConfigurationProcessor.Instance().fetchConfiguration();
  await ProcessingStationProcessor.Instance().fetchStationData();
}

describe('User Configuration', () => {
  // tslint:disable-next-line: max-line-length
  it('A users gets their default network and default filters loaded in the Configuration Processor after login', async () => {
    const query = `
    query {
      uiAnalystConfiguration{
        logLevel
        defaultNetwork
        redisplayPeriod
        reprocessingPeriod
        acknowledgementQuietDuration
        availableQuietDurations
        sohStationStaleTimeMS
        sohHistoricalDurations
        sohStationGroupNames {
          name
          priority
        }
        defaultFilters {
          id
          name
          description
          filterType
          filterPassBandType
          lowFrequencyHz
          highFrequencyHz
          order
          filterSource
          filterCausality
          zeroPhase
          sampleRate
          sampleRateTolerance
          groupDelaySecs
          validForSampleRate
        }
      }
    }
    `;

    // Execute the GraphQL query
    const rootValue = {};
    const result = await graphql(schema, query, rootValue, userContext);
    const { data } = result;
    // Compare response to snapshot
    expect(data).toMatchSnapshot();
  });
});
