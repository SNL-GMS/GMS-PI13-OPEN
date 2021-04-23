// tslint:disable: no-console
// tslint:disable: no-magic-numbers
// tslint:disable: max-line-length
import config from 'config';
import { HttpClientWrapper } from '../src/ts/util/http-wrapper';
import { STATION_IDS } from './util/integration-utils';

// Initialize an http client
// Retrieve the request configuration for the service call
const requestConfig = config.get('station.backend.services.stationsByIds.requestConfig');
const httpWrapper = new HttpClientWrapper();

beforeAll(() => {
  jest.setTimeout(60000);
});
describe('stations by ids tests', () => {
  test('stations by ids', async () => {
    const response = await httpWrapper.request(requestConfig, STATION_IDS);
    expect(response.status).toEqual(200);
    expect(response.data).toBeDefined();
    expect(response.data).toMatchSnapshot();
  });
});
