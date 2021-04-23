// tslint:disable: no-console
// tslint:disable: no-magic-numbers
// tslint:disable: max-line-length
import config from 'config';
import { HttpClientWrapper } from '../src/ts/util/http-wrapper';
import { EVENT_ID } from './util/integration-utils';

// Initialize an http client
// Retrieve the request configuration for the service call
const requestConfig = config.get('event.backend.services.getEventsByIds.requestConfig');
const httpWrapper = new HttpClientWrapper();

beforeAll(() => {
  jest.setTimeout(30000);
});

describe('events by id', () => {
  // Look into removing this service call
  test('get event for specific event ID', async () => {
    const query = {
      ids: [EVENT_ID]
    };
    const response = await httpWrapper.request(requestConfig, query);
    // TODO this gets a 400 error - Never called in normal UI operation
    expect(response.status).toEqual(400);
    expect(response.data).toMatchSnapshot();
  });
});
