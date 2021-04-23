// tslint:disable: no-console
// tslint:disable: no-magic-numbers
// tslint:disable: max-line-length
import config from 'config';
import { HttpClientWrapper } from '../src/ts/util/http-wrapper';

const httpWrapper = new HttpClientWrapper();
// Initialize an http client
// Retrieve the request configuration for the service call
const requestConfig = config.get('station.backend.services.networkByName.requestConfig');

beforeAll(() => {
  jest.setTimeout(30000);
});

describe('network by name tests', () => {
  test('get demo network from service', async () => {
    const query = {
      name: 'demo'
    };
    const response = await httpWrapper.request<any>(requestConfig, query);
    // Check station count and station names
    expect(response.status).toEqual(200);
    expect(response.data.stations).toHaveLength(47);
    expect(response.data.stations.map(st => st.name).sort()).toMatchSnapshot();
    // Check single station sites and channels
    const station = response.data.stations.find(st => st.name.includes('ASAR'));
    expect(station.sites).toHaveLength(20);
    let totalChannelCount = 0;
    station.sites.map(site => {
      totalChannelCount += site.channels.length;
    });
    expect(totalChannelCount).toEqual(22);
  });
});
