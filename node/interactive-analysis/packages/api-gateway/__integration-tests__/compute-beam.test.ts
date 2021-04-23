// tslint:disable: no-console
// tslint:disable: no-magic-numbers
import config from 'config';
import { HttpClientWrapper } from '../src/ts/util/http-wrapper';
import { getIntegrationInput } from './util/integration-utils';

// Initialize an http client
// Retrieve the request configuration for the service call
const requestConfig = config.get('channelSegment.backend.services.computeBeam.requestConfig');
const httpWrapper = new HttpClientWrapper();

beforeAll(() => {
  jest.setTimeout(30000);
});

describe('compute beam tests', () => {
  test('compute beam', async () => {
    const fpinput: any = getIntegrationInput('computeBeam');
    const response = await httpWrapper.request(requestConfig, fpinput);
    expect(response.status).toEqual(200);
    expect(response.data).toBeDefined();
    const beam = response.data[0];
    // Expect a beam to come back
    expect(beam.type).toContain('DETECTION_BEAM');
    // Expect only one beam back
    expect(beam.timeseries).toHaveLength(1);
    const timeseries = beam.timeseries[0];
    // Expect input waveforms length to match output beam length
    expect(timeseries.values.length).toEqual(fpinput.waveforms[0].timeseries[0].values.length);
    // Snapshot just a few values(first 100) (truncated) - full snapshot fails
    const first100 = timeseries.values.slice(0, 100).map(value => value.toFixed(5));
    expect(first100).toMatchSnapshot();
  });
});
