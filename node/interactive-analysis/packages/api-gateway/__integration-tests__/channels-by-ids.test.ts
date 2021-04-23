// tslint:disable: no-console
// tslint:disable: no-magic-numbers
import config from 'config';
import { HttpClientWrapper } from '../src/ts/util/http-wrapper';
import { KDAK_CHANNEL_IDS } from './util/integration-utils';

// Initialize an http client
// Retrieve the request configuration for the service call
const requestConfig = config.get('station.backend.services.channelsByIds.requestConfig');
const httpWrapper = new HttpClientWrapper();

beforeAll(() => {
  jest.setTimeout(60000);
});
describe('channel by id tests', () => {
  test('channel by id', async () => {
    const response = await httpWrapper.request(requestConfig, KDAK_CHANNEL_IDS);
    expect(response.status).toEqual(200);
    expect(response.data).toBeDefined();
    expect(response.data).toMatchSnapshot();
  });
});
