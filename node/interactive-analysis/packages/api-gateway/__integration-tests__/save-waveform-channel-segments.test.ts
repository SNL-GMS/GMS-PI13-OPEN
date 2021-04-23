// tslint:disable: no-console
// tslint:disable: no-magic-numbers
// tslint:disable: max-line-length
import { epochSecondsNow, jsonPretty, toOSDTime, uuid4 } from '@gms/common-util';
import config from 'config';
import { SaveWaveformChannelSegmentsResponse } from '../src/ts/channel-segment/channel-segment-services-client';
import { HttpClientWrapper } from '../src/ts/util/http-wrapper';
import { getIntegrationInput } from './util/integration-utils';

// Initialize an http client
// Retrieve the request configuration for the service call
const requestConfig = config.get('channelSegment.backend.services.saveWaveforms.requestConfig');
const httpWrapper = new HttpClientWrapper();

beforeAll(() => {
  jest.setTimeout(30000);
});

describe('save waveform channel segments', () => {
  const channelSegmentToSave = getIntegrationInput('saveWaveformChannelSegment')[0];

  test('save a single waveform channel segment', async () => {
    const newId = uuid4();
    const start = epochSecondsNow();
    const end = start + 10;

    const startStr = toOSDTime(start);
    const endStr = toOSDTime(end);

    channelSegmentToSave.id = newId;
    channelSegmentToSave.creationInfo.creationTime = startStr;
    channelSegmentToSave.startTime = startStr;
    channelSegmentToSave.endTime = endStr;
    const timeseriesToSave = channelSegmentToSave.timeseries[0];
    timeseriesToSave.startTime = startStr;
    timeseriesToSave.values = new Array(timeseriesToSave.sampleCount).fill(0);
    const response = await httpWrapper.request<SaveWaveformChannelSegmentsResponse>(requestConfig, [
      channelSegmentToSave
    ]);
    console.log(jsonPretty(response.data));
    expect(response.status).toEqual(200);
    expect(response.data.stored.length).toEqual(1);
    expect(response.data.failed.length).toEqual(0);
  });
});
