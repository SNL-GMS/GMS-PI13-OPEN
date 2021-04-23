// tslint:disable: no-console
// tslint:disable: no-magic-numbers
// tslint:disable: max-line-length
import config from 'config';
import { HttpClientWrapper } from '../src/ts/util/http-wrapper';
import { KDAK_CHANNEL_IDS, TIME_RANGE } from './util/integration-utils';

// Initialize an http client
// Retrieve the request configuration for the service call
const requestConfig = config.get('qcMask.backend.services.masksByChannelIds.requestConfig');
const httpWrapper = new HttpClientWrapper();

beforeAll(() => {
  jest.setTimeout(30000);
});

describe('masks by channel', () => {
  test('get masks for KDAK channels', async () => {
    const query = {
      'channel-ids': KDAK_CHANNEL_IDS,
      'start-time': TIME_RANGE.startTime,
      'end-time': TIME_RANGE.endTime
    };
    const response = await httpWrapper.request(requestConfig, query);
    expect(response.status).toEqual(200);
    expect(response.data).toBeDefined();

    const keys = Object.keys(response.data);
    expect(keys.length).toBeGreaterThan(0);
    const channelMask = response.data[keys[0]][0];
    expect(channelMask).toBeDefined();
    expect(Object.keys(channelMask)).toMatchSnapshot();
  });
});
