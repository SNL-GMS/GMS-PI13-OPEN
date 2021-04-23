// tslint:disable: no-console
// tslint:disable: no-magic-numbers
// tslint:disable: max-line-length
import config from 'config';
import { HttpClientWrapper } from '../src/ts/util/http-wrapper';
import { TIME_RANGE } from './util/integration-utils';

// Initialize an http client
// Retrieve the request configuration for the service call
const requestConfig = config.get(
  'dataAcquisition.backend.services.transferredFilesByTimeRange.requestConfig'
);
const httpWrapper = new HttpClientWrapper();

beforeAll(() => {
  jest.setTimeout(30000);
});
describe('transferred file by name tests', () => {
  test('transferred file by name', async () => {
    // TODO this doesn't test much. Empty array. Test something more reasonable for this service
    const query = {
      transferStartTime: TIME_RANGE.startTime,
      transferEndTime: TIME_RANGE.endTime
    };
    const response = await httpWrapper.request(requestConfig, query);
    expect(response).toBeDefined();
    expect(response.data).toMatchSnapshot();
  });
});
