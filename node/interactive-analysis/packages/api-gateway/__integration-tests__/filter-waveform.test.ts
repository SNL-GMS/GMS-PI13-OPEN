// tslint:disable: no-console
// tslint:disable: no-magic-numbers
import config from 'config';
import { HttpClientWrapper } from '../src/ts/util/http-wrapper';
import { getIntegrationInput } from './util/integration-utils';

// Initialize an http client
// Retrieve the request configuration for the service call
const requestConfig = config.get(
  'filterWaveform.backend.services.calculateWaveformSegments.requestConfig'
);
const httpWrapper = new HttpClientWrapper();

beforeAll(() => {
  jest.setTimeout(30000);
});

describe('filter channel segment', () => {
  test('save qc mask to KDAK', async () => {
    const filterInput = getIntegrationInput('filterWaveform');
    const response = await httpWrapper.request(requestConfig, filterInput);
    expect(response.status).toEqual(200);
    const filterChannelSegment = response.data[0];
    delete filterChannelSegment.id;
    delete filterChannelSegment.creationInfo;
    expect(filterChannelSegment).toMatchSnapshot();
  });
});
