// tslint:disable: no-console
// tslint:disable: no-magic-numbers
// tslint:disable: max-line-length
import config from 'config';
import { HttpClientWrapper } from '../src/ts/util/http-wrapper';
import { TIME_RANGE } from './util/integration-utils';

// Initialize an http client
// Retrieve the request configuration for the service call
const requestConfig = config.get('event.backend.services.getEventsByTimeAndLatLong.requestConfig');
const httpWrapper = new HttpClientWrapper();

beforeAll(() => {
  jest.setTimeout(30000);
});

describe('events by time range', () => {
  test('get events for time in STDS', async () => {
    const query = {
      startTime: TIME_RANGE.startTime,
      endTime: TIME_RANGE.endTime
    };
    const response = await httpWrapper.request<any[]>(requestConfig, query);
    expect(response.status).toEqual(200);
    expect(response.data.length).toBeGreaterThan(1);

    const event = response.data[0];
    expect(Object.keys(event)).toMatchSnapshot();
  });
});
