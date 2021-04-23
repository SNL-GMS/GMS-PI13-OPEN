// tslint:disable: no-console
// tslint:disable: no-magic-numbers
import config from 'config';
import { HttpClientWrapper } from '../src/ts/util/http-wrapper';
import { getIntegrationInput } from './util/integration-utils';

// Initialize an http client
// Retrieve the request configuration for the service call
const requestConfig = config.get('event.backend.services.computeFeaturePredictions.requestConfig');
const httpWrapper = new HttpClientWrapper();

beforeAll(() => {
  jest.setTimeout(30000);
});

describe('compute feature predictions', () => {
  test('compute feature prediction', async () => {
    const fpinput = getIntegrationInput('featurePrediction');
    const response = await httpWrapper.request(requestConfig, fpinput);
    expect(response.status).toEqual(200);
    expect(response.data).toBeDefined();
    expect(Object.keys(response.data).sort()).toMatchSnapshot();
  });
});
