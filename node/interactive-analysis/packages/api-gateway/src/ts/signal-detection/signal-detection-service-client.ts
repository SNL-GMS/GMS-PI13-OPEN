import { jsonPretty, MILLISECONDS_IN_SECOND } from '@gms/common-util';
import { AxiosRequestConfig } from 'axios';
import { TimeRange } from '../common/model';
import { gatewayLogger as logger } from '../log/gateway-logger';
import { HttpClientWrapper, HttpResponse } from '../util/http-wrapper';
import * as model from './model';
import { SignalDetectionOSD } from './model-osd';
import { convertSDtoOSD } from './signal-detection-converter';

/**
 * Loads signal detections from a service
 * @param stationNames the station names
 * @param timeRange the time range
 * @param endTimePadding the end time padding
 * @param httpWrapper the http wrapper
 * @param requestConfig the request config
 */
export async function loadSignalDetectionsFromService(
  stationNames: string[],
  timeRange: TimeRange,
  endTimePadding: number,
  httpWrapper: HttpClientWrapper,
  requestConfig
): Promise<SignalDetectionOSD[]> {
  const query = {
    stationNames,
    startTime: new Date(timeRange.startTime * MILLISECONDS_IN_SECOND).toISOString(),
    endTime: new Date((timeRange.endTime + endTimePadding) * MILLISECONDS_IN_SECOND).toISOString()
  };
  logger.debug(
    `Calling query signal detections service request: ` +
      ` ${jsonPretty(requestConfig)} query: ${jsonPretty(query)}`
  );
  try {
    const response: HttpResponse<SignalDetectionOSD[]> = await httpWrapper.request<
      SignalDetectionOSD[]
    >(requestConfig, query);
    if (response && response.data) {
      const signalDetections: SignalDetectionOSD[] = response.data;
      return Object.seal(signalDetections);
    }
  } catch (e) {
    logger.error(`Failed signal detection service request ${e}`);
  }
  return Object.seal([]);
}

/**
 * Calls the save signal detections service.
 *
 * @param signalDetections the signal detections to save
 * @param httpWrapper the instance of the http wrapper
 * @param requestConfig the axios request configuration
 */
export async function saveSignalDetectionsToService(
  signalDetections: model.SignalDetection[],
  httpWrapper: HttpClientWrapper,
  requestConfig: AxiosRequestConfig
): Promise<HttpResponse> {
  if (!httpWrapper) {
    throw new Error('Error: save signal detections: invalid http wrapper');
  }

  if (!signalDetections || signalDetections.length === 0) {
    throw new Error('Error: save signal detections: requires at least one valid signal detection');
  }

  if (!requestConfig) {
    throw new Error('Error: save signal detections: invalid request configuration');
  }

  // convert to the OSD signal detection format
  const osdSignalDetections = signalDetections.map(convertSDtoOSD);

  logger.debug(
    `Calling save signal detections service: ` +
      `request: ${jsonPretty(requestConfig)} query: ${jsonPretty(osdSignalDetections)}`
  );
  const response = await httpWrapper.request<string[]>(
    requestConfig,
    JSON.stringify(osdSignalDetections)
  );
  const httpOkay = 200;
  logger.info(
    `Saved signal detections result: (${response.status}) : ${jsonPretty(response.data)}`
  );
  if (response.status !== httpOkay) {
    logger.error(
      `Failed to save the following signal detections: ${String(
        osdSignalDetections.map(sd => sd.id)
      )}`
    );
  }
  return response;
}

/**
 * Calls the save signal detections hypotheses service.
 *
 * @param signalDetections the signal detections to save
 * @param httpWrapper the http wrapper instance
 * @param requestConfig the axios request configuration
 */
export async function saveSignalDetectionHypothesesToService(
  saveSignalDetectionHypthesesServiceInput: model.SaveSignalDetectionHypthesesServiceInput[],
  httpWrapper: HttpClientWrapper,
  requestConfig: any
): Promise<void> {
  // If we have detections hypotheses to save, save them
  if (
    saveSignalDetectionHypthesesServiceInput &&
    saveSignalDetectionHypthesesServiceInput.length > 0
  ) {
    logger.debug('Saving sds hypotheses');
    // tslint:disable-next-line:max-line-length
    logger.debug(
      `Sending service request: ${JSON.stringify(
        requestConfig,
        undefined,
        2
      )} query: ${JSON.stringify(saveSignalDetectionHypthesesServiceInput, undefined, 2)}`
    );
    // TODO: check if saved to OSD then update and return sdsToSave etc.
    await httpWrapper.request(
      requestConfig,
      JSON.stringify(saveSignalDetectionHypthesesServiceInput)
    );
  }
}
