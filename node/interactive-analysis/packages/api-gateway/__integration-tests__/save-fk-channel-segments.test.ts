// tslint:disable: no-console
// tslint:disable: no-magic-numbers
// tslint:disable: max-line-length
import { epochSecondsNow, toOSDTime, uuid4 } from '@gms/common-util';
import config from 'config';
import { HttpClientWrapper } from '../src/ts/util/http-wrapper';
import { getIntegrationInput } from './util/integration-utils';

// Initialize an http client
// Retrieve the request configuration for the service call
const requestConfig = config.get('channelSegment.backend.services.saveFks.requestConfig');
const httpWrapper = new HttpClientWrapper();

beforeAll(() => {
  jest.setTimeout(30000);
});

describe('save fk channel segments', () => {
  const channelSegmentsToSave = getIntegrationInput('saveFkChannelSegment')[0];

  test('save a single fk channel segment', async () => {
    const newId = uuid4();
    const start = epochSecondsNow();
    const end = start + 10;

    const startStr = toOSDTime(start);
    const endStr = toOSDTime(end);

    const channelSegmentToSave = channelSegmentsToSave.channelSegments[0];
    channelSegmentToSave.id = newId;
    channelSegmentToSave.creationInfo.creationTime = startStr;
    channelSegmentToSave.startTime = startStr;
    channelSegmentToSave.endTime = endStr;
    const timeseriesToSave = channelSegmentToSave.timeseries[0];
    timeseriesToSave.startTime = startStr;
    timeseriesToSave.values[0].fstat = new Array(81).fill(new Array(81).fill(0));
    timeseriesToSave.values[0].power = new Array(81).fill(new Array(81).fill(0));
    const response = await httpWrapper.request<string[]>(requestConfig, channelSegmentsToSave);
    expect(response.status).toEqual(200);
    expect(response.data.length).toEqual(1);
  });
});
