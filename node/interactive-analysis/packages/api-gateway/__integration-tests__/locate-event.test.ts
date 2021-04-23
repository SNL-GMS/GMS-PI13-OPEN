// tslint:disable: no-console
// tslint:disable: no-magic-numbers
// tslint:disable: max-line-length
import config from 'config';
import { HttpClientWrapper } from '../src/ts/util/http-wrapper';
import { getIntegrationInput } from './util/integration-utils';

// Initialize an http client
// Retrieve the request configuration for the service call
const requestConfig = config.get('event.backend.services.locateEvent.requestConfig');
const httpWrapper = new HttpClientWrapper();

beforeAll(() => {
  jest.setTimeout(30000);
});
describe('locate event tests', () => {
  test('locate event', async () => {
    const fpinput = getIntegrationInput('locateEvent');
    const response = await httpWrapper.request(requestConfig, fpinput);
    expect(response.status).toEqual(200);
    expect(response.data).toBeDefined();
    // Gets solution id keys
    const solutionId = Object.keys(response.data);
    expect(solutionId).toMatchSnapshot();
    // Gets the first location solution from the array of 1
    const value = response.data[solutionId[0]][0];
    expect(Object.keys(value).sort()).toMatchSnapshot();
  });
});
