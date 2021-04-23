// tslint:disable:max-line-length
import { graphql } from 'graphql';
import { ConfigurationProcessor } from '../src/ts/configuration/configuration-processor';
import { schema } from '../src/ts/server/api-gateway-schema';
import { ProcessingStationProcessor } from '../src/ts/station/processing-station/processing-station-processor';
import { userContext } from './__data__/user-profile-data';

// ---- Query test cases ----
/**
 * Sets up test by loading SDs
 */
beforeAll(async () => setupTest());
async function setupTest() {
  await ConfigurationProcessor.Instance().fetchConfiguration();
  await ProcessingStationProcessor.Instance().fetchStationData();
}

// Test case - basic content query for waveforms
it('Waveform content query should match snapshot', async () => {
  // ChannelID comes from SIV/MH1, which has five segments specified
  // in `test_data/ueb/ueb_wfdisc.txt`.  The `test_data/ueb/SIV0.w`
  // file contains the first 32768 bytes of the `tonto2/GNEM/indexpool/proj
  // This is enough to read samples for all of the channel 5332 segments.
  // The file was truncated here to keep the Git repository smaller.
  // tslint:disable-next-line:no-console
  console.log('pre query');
  const query = `
  query getRawWaveformSegmentsByChannels {
    getRawWaveformSegmentsByChannels(timeRange: {startTime: 1274385600, endTime: 1274400000}, channelIds: ["KDAK.KDAK.BHZ"]) {
      startTime
      endTime
      type
      channelId
      timeseries {
        startTime
        sampleRate
        sampleCount
      }
    }
  }
  `;

  // Execute the GraphQL query
  const rootValue = {};
  const result = await graphql(schema, query, rootValue, userContext);
  // tslint:disable-next-line:no-console
  console.log('post query');

  const { data } = result;

  // Compare response to snapshot
  expect(data).toMatchSnapshot();
});
