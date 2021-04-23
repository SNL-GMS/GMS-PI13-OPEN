import { graphql } from 'graphql';
import { ConfigurationProcessor } from '../src/ts/configuration/configuration-processor';
import { schema } from '../src/ts/server/api-gateway-schema';
import { SohProcessor } from '../src/ts/soh/soh-processor';
import { createEmptyStationSoh } from '../src/ts/soh/soh-util';
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
  // Initialize mock backend data for new UiStationSoh
  SohProcessor.Instance();
}

describe('Fetch SOH data', () => {
  it('loads station soh and station groups', async () => {
    // param
    const query = `
    query {
      stationAndStationGroupSoh {
        isUpdateResponse
        stationSoh {
          id
          stationName
          sohStatusSummary
          needsAcknowledgement
          allStationAggregates {
            value
            valuePresent
            aggregateType
          }
          statusContributors {
            value
            contributing
            valuePresent
            statusSummary
            type
          }
          channelSohs {
            channelName
          }
          stationGroups {
            groupName
            stationName
            sohStationCapability
          }
        }
        stationGroups {
          id
          stationGroupName
          groupCapabilityStatus
          time
          priority
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

  it('loads channel soh', async () => {
    const query = `
    query channelSohForStation {
      channelSohForStation(stationName: "AAK") {
        channelSohs {
          channelName
          channelSohStatus
          allSohMonitorValueAndStatuses {
            status
            value
            monitorType
            hasUnacknowledgedChanges
            thresholdMarginal
            thresholdBad
            quietUntilMs
          }
        }
        stationName
        id
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

  it('loads channel soh for no station', async () => {
    const query = `
    query channelSohForStation {
      channelSohForStation(stationName: "No Station") {
        channelSohs {
          channelName
          channelSohStatus
          allSohMonitorValueAndStatuses {
            status
            value
            monitorType
            hasUnacknowledgedChanges
            thresholdMarginal
            thresholdBad
            quietUntilMs
          }
        }
        stationName
        id
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

describe('Soh state of health', () => {
  it('Checks create empty station soh', () => {
    // Create Empty station soh
    const stationSoh = createEmptyStationSoh('test');
    stationSoh.uuid = `1`;
    expect(stationSoh).toMatchSnapshot();
  });

  it('acknowledge soh status works as expected', async () => {
    const mutation = `
      mutation {
        acknowledgeSohStatus(stationNames: ["AAK"])
      }
      `;

    // Execute the GraphQL query
    const rootValue = {};
    const result = await graphql(schema, mutation, rootValue, userContext);
    const { data } = result;

    // Mutation should return true
    expect(data.acknowledgeSohStatus).toBeDefined();
    expect(data.acknowledgeSohStatus).toBeTruthy();
  });

  it('Can get most recent SOH for stations', () => {
    expect(SohProcessor.Instance().getSohForAllStations().length).toBeGreaterThan(0);
  });

  // TODO: Fix test to reflect number of empty stations initialized instead of just greater than 0
  it('Can get most recent SOH for station and groups', () => {
    expect(
      SohProcessor.Instance().getStationAndGroupSohWithEmptyChannels().stationGroups.length
    ).toBeGreaterThan(0);
    expect(
      SohProcessor.Instance().getStationAndGroupSohWithEmptyChannels().stationSoh.length
    ).toBeGreaterThan(0);
  });
});
