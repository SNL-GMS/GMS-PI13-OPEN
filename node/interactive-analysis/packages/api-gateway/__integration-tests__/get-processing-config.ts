import config from 'config';
import { HttpClientWrapper } from '../src/ts/util/http-wrapper';

// Initialize an http client
// Retrieve the request configuration for the service call
const requestConfig = config.get(
  'configuration.backend.services.getAnalystConfiguration.requestConfig'
);
const httpWrapper = new HttpClientWrapper();

beforeAll(() => {
  // tslint:disable-next-line: no-magic-numbers
  jest.setTimeout(30000);
});

const configurationQuery = {
  configName: 'default-analyst-settings',
  selectors: []
};
describe('Processing Config tests', () => {
  test('Gets the processing config', async () => {
    const response = await httpWrapper.request(requestConfig, configurationQuery);
    // tslint:disable-next-line: no-magic-numbers
    expect(response.status).toEqual(200);
    expect(response.data).toBeDefined();
    expect(response.data).toMatchSnapshot();
  });
});
