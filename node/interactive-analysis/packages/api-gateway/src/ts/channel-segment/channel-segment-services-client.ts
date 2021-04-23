import { jsonPretty } from '@gms/common-util';
import { AxiosRequestConfig } from 'axios';
import { UserContext } from '../cache/model';
import { gatewayLogger as logger } from '../log/gateway-logger';
import { convertChannelSegmentFromAPIToOSD } from '../util/channel-segment-utils';
import { HttpClientWrapper, HttpResponse } from '../util/http-wrapper';
import * as model from './model';

/**
 * Waveform channel segment save result.
 */
export interface WaveformChannelSegmentSaveResult {
  channelId: string;
  startTime: string;
  endTime: string;
}

/**
 * Save waveform channel segments response.
 */
export interface SaveWaveformChannelSegmentsResponse {
  stored: WaveformChannelSegmentSaveResult[];
  failed: WaveformChannelSegmentSaveResult[];
}

/**
 * Calls the Save waveform channel segments service.
 *
 * @param userContext the user context
 * @param channelSegments the channelSegments (waveform) to save
 * @param httpWrapper the instance of the http wrapper
 * @param requestConfig the axios request configuration
 *
 * @returns a saved waveform channel segments http response
 */
export async function saveWaveformChannelSegmentsToService(
  userContext: UserContext,
  channelSegments: model.ChannelSegment<model.TimeSeries>[],
  httpWrapper: HttpClientWrapper,
  requestConfig: AxiosRequestConfig
): Promise<HttpResponse> {
  if (!httpWrapper) {
    throw new Error('Error: save waveform channelSegments: invalid http wrapper');
  }

  if (!channelSegments || channelSegments.length === 0) {
    throw new Error('Error: save waveform channelSegments: requires at least one valid event');
  }

  if (!requestConfig) {
    throw new Error('Error: save waveform channelSegments: invalid request configuration');
  }

  // convert to the OSD channel segment format
  const osdChannelSegments = channelSegments.map(convertChannelSegmentFromAPIToOSD);

  logger.info(
    `Calling save waveform channel segments service: request: ` +
      `${jsonPretty(requestConfig)} query: ${JSON.stringify(osdChannelSegments, undefined, 2)}`
  );
  const response = await httpWrapper.request(requestConfig, osdChannelSegments);
  logger.info(
    `Saved waveform channel segments result: (${response.status}) : ${jsonPretty(response.data)}`
  );
  const httpOkay = 200;
  if (response.status !== httpOkay) {
    logger.error(
      `Failed to save the following waveform channel segments: ${String(
        osdChannelSegments.map(cs => cs.id)
      )}`
    );
  }
  return response;
}

/**
 * Calls the Save fk channel segments service.
 *
 * @param userContext the user context
 * @param channelSegments the channelSegments (fk) to save
 * @param httpWrapper the instance of the http wrapper
 * @param requestConfig the axios request configuration
 *
 * @returns a saved waveform channel segments http response
 */
export async function saveFkChannelSegmentsToService(
  userContext: UserContext,
  channelSegments: model.ChannelSegment<model.TimeSeries>[],
  httpWrapper: HttpClientWrapper,
  requestConfig: AxiosRequestConfig
): Promise<HttpResponse> {
  if (!httpWrapper) {
    throw new Error('Error: save fk channelSegments: invalid http wrapper');
  }

  if (!channelSegments || channelSegments.length === 0) {
    throw new Error('Error: save fk channelSegments: requires at least one valid event');
  }

  if (!requestConfig) {
    throw new Error('Error: save fk channelSegments: invalid request configuration');
  }

  // convert to the OSD channel segment format
  const osdChannelSegments = channelSegments.map(convertChannelSegmentFromAPIToOSD);

  logger.debug(
    `Calling save fk channel segments service: request: ` +
      `${jsonPretty(requestConfig)} query: ${JSON.stringify(osdChannelSegments, undefined, 2)}`
  );
  try {
    const response = await httpWrapper.request(requestConfig, osdChannelSegments);
    logger.info(
      `Saved fk channel segments result: (${response.status}) : ${jsonPretty(response.data)}`
    );
    const httpOkay = 200;
    if (response.status !== httpOkay) {
      logger.error(
        `Failed to save the following FK channel segments: ${String(
          osdChannelSegments.map(cs => cs.id)
        )}`
      );
    }
    return response;
  } catch (e) {
    logger.error(`Failed to save FK Channel Segment error ${e}`);
  }
  return undefined;
}
