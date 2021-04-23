// tslint:disable: no-console
// tslint:disable: no-magic-numbers
import config from 'config';
import { HttpClientWrapper } from '../src/ts/util/http-wrapper';
import { getIntegrationInput } from './util/integration-utils';

// Initialize an http client
// Retrieve the request configuration for the service call
const requestConfig = config.get(
  'event.backend.services.computeNetworkMagnitudeSolution.requestConfig'
);
const httpWrapper = new HttpClientWrapper();
beforeAll(() => {
  jest.setTimeout(30000);
});

// call the computer service and expect it to match our snapshot
describe('computes network magnitude solution', () => {
  test('given an event', async () => {
    const query: any = getIntegrationInput('computeMagSolution');
    const response = await httpWrapper.request(requestConfig, query);
    expect(response.status).toEqual(200);
    expect(response.data).toMatchSnapshot();
  });
});
