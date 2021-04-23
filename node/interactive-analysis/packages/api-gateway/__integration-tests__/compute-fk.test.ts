// tslint:disable: no-console
// tslint:disable: no-magic-numbers
import config from 'config';
import { HttpClientWrapper } from '../src/ts/util/http-wrapper';
import { FK_CHANNEL_IDS, getIntegrationInput } from './util/integration-utils';

// Initialize an http client
// Retrieve the request configuration for the service call
const requestConfig = config.get('fk.backend.services.computeFk.requestConfig');
const httpWrapper = new HttpClientWrapper();

beforeAll(() => {
  jest.setTimeout(30000);
});

describe('compute fk tests', () => {
  test('compute fk', async () => {
    const fpinput = getIntegrationInput('computeFk');
    (fpinput as any).channelIds = FK_CHANNEL_IDS;
    const response = await httpWrapper.request(requestConfig, fpinput);
    expect(response.status).toEqual(200);
    expect(response.data).toBeDefined();
    const fkSpectrum = response.data[0].timeseries[0].values[0];
    // Check Fstat exists and snapshot the first array in the grid
    expect(fkSpectrum.fstat).toBeDefined();
    expect(fkSpectrum.fstat).toHaveLength(81);
    expect(fkSpectrum.fstat[0].map(value => value.toFixed(5))).toMatchSnapshot();
    // Check Power exists and snapshot the first array in the grid
    expect(fkSpectrum.power).toBeDefined();
    expect(fkSpectrum.power).toHaveLength(81);
    expect(fkSpectrum.power[0].map(value => value.toFixed(5))).toMatchSnapshot();
  });
});
