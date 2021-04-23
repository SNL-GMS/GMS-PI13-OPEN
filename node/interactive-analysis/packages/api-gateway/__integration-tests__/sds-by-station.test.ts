// tslint:disable: no-console
// tslint:disable: no-magic-numbers
// tslint:disable: max-line-length
import config from 'config';
import { HttpClientWrapper } from '../src/ts/util/http-wrapper';
import { STATION_IDS, TIME_RANGE } from './util/integration-utils';

// Initialize an http client
// Retrieve the request configuration for the service call
const requestConfig = config.get('signalDetection.backend.services.sdsByStation.requestConfig');
const httpWrapper = new HttpClientWrapper();

beforeAll(() => {
  jest.setTimeout(30000);
});

describe('sds by station tests', () => {
  test('get signal detections for ASAR', async () => {
    const query = {
      stationIds: STATION_IDS,
      startTime: TIME_RANGE.startTime,
      endTime: TIME_RANGE.endTime
    };
    const response = await httpWrapper.request(requestConfig, query);
    expect(response.status).toEqual(200);
    const idKeys = Object.keys(response.data);
    expect(idKeys).toMatchSnapshot();
    const sdsByStationId: any[] = response.data[idKeys[0]];
    expect(sdsByStationId).toHaveLength(4);
    sdsByStationId.forEach(sd => {
      expect(Object.keys(sd)).toMatchSnapshot();
    });
  });
});
