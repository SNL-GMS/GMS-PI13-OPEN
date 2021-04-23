// tslint:disable:max-line-length
import { readJsonData } from '@gms/common-util';
import { graphql } from 'graphql';
import sortBy from 'lodash/sortBy';
import { ConfigurationProcessor } from '../src/ts/configuration/configuration-processor';
import { schema } from '../src/ts/server/api-gateway-schema';
import { ReferenceStationProcessor } from '../src/ts/station/reference-station/reference-station-processor';
import { userContext } from './__data__/user-profile-data';

// Setup
let stationResponse: any = readJsonData(
  './resources/test_data/unit-test-data/station/reference-station-response.json'
);
let referenceStation: any = readJsonData(
  './resources/test_data/unit-test-data/station/reference-station.json'
);
const dataAcquisition: any = 'dataAcquisition';

/**
 * Sets up test by loading configuration
 */
beforeEach(async () => setupTest());
async function setupTest() {
  await ConfigurationProcessor.Instance().fetchConfiguration();
}

// ---- Query test cases ----

// Test that 3 methods in station processor work as expected
// processStationData also calls processSiteData, which called processChannelData
// so this test passing, ensures all three are working as expected
describe('When Processing a OSD Station response', () => {
  it('the processing station should match expected result', () => {
    // dataAcquisition field values in processingStation are randomly generated, thus need to be set to empty
    referenceStation = referenceStation[dataAcquisition] = {};
    stationResponse = ReferenceStationProcessor.Instance().processStationData(
      stationResponse
    ).station[dataAcquisition] = {};
    expect(JSON.stringify(referenceStation)).toEqual(JSON.stringify(stationResponse));
  });
});

// Test case - query default stations with rich information
xit('Single channel query results should match snapshot', async () => {
  // language=GraphQL
  const query = `
    query defaultReferenceStations {
      defaultReferenceStations {
        name
        stationType
        location{
          latitudeDegrees
          longitudeDegrees
          elevationKm
        }
        networks{
          name
          monitoringOrganization
        }
      }
    }
  `;

  // Execute the GraphQL query
  const rootValue = {};
  // Execution the GraphQL query with a small delay to allow the API gateway to settle async
  // HTTP requests fetching data from the mock backend
  const result = await graphql(schema, query, rootValue, userContext);
  const { data } = result;
  // Compare response to snapshot
  expect(sortBy(data.defaultReferenceStations, 'name')).toMatchSnapshot();
});
