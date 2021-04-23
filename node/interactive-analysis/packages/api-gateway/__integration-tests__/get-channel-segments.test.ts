// tslint:disable: no-console
// tslint:disable: no-magic-numbers
// tslint:disable: max-line-length
import config from 'config';
import { HttpClientWrapper } from '../src/ts/util/http-wrapper';
import {
  ASAR_FKB_CHANNEL_SEGMENT_ID,
  KDAK_CHANNEL_IDS,
  TIME_RANGE
} from './util/integration-utils';

// Initialize an http client
// Retrieve the request configuration for the service call
const httpWrapper = new HttpClientWrapper();

beforeAll(() => {
  jest.setTimeout(30000);
});

describe('channel segment tests', () => {
  test('segment by id', async () => {
    const query = {
      ids: [ASAR_FKB_CHANNEL_SEGMENT_ID],
      withTimeseries: true
    };
    const requestConfig = config.get(
      'channelSegment.backend.services.channelSegmentsById.requestConfig'
    );
    const response = await httpWrapper.request(requestConfig, query);
    // Removes changing fields from the snapshot
    Object.keys(response.data).forEach(key => {
      delete response.data[key].creationInfo;
      delete response.data[key].id;
    });
    expect(response.status).toEqual(200);
    expect(response.data).toMatchSnapshot();
  });

  test('segment by channel id and time range', async () => {
    const query = {
      'channel-ids': KDAK_CHANNEL_IDS,
      'start-time': TIME_RANGE.startTime,
      'end-time': TIME_RANGE.shortEndTime,
      'with-waveforms': true
    };
    const requestConfig = config.get(
      'channelSegment.backend.services.channelSegmentsByTimeRange.requestConfig'
    );
    const response = await httpWrapper.request(requestConfig, query);
    // Removes changing fields from the snapshot
    Object.keys(response.data).forEach(key => {
      delete response.data[key].creationInfo;
      delete response.data[key].id;
    });
    expect(response.status).toEqual(200);
    expect(response.data).toMatchSnapshot();
  });
});
