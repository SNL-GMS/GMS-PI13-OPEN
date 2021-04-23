import { jsonPretty, toOSDTime } from '@gms/common-util';
import { ApolloError } from 'apollo-server-core';
import { AxiosRequestConfig } from 'axios';
import { UserContext } from '../cache/model';
import { Location, TimeRange } from '../common/model';
import { gatewayLogger as logger } from '../log/gateway-logger';
import { performanceLogger } from '../log/performance-logger';
import { SignalDetection } from '../signal-detection/model';
import { SignalDetectionHypothesisOSD } from '../signal-detection/model-osd';
import { convertSDtoOSD } from '../signal-detection/signal-detection-converter';
import { ProcessingStation } from '../station/processing-station/model';
import { systemConfig } from '../system-config';
import { isObjectEmpty } from '../util/common-utils';
import { HttpClientWrapper, HttpResponse, isHttpResponseError } from '../util/http-wrapper';
import * as model from './model-and-schema/model';
import * as osdModel from './model-and-schema/model-osd';
import {
  alignPreferredEventHypothesisHistoryWithHypothesis,
  convertEventFromOSD,
  convertEventToOSD
} from './utils/event-format-converter';
import { generateSignalDetectionBehaviorsMap } from './utils/event-utils';

/** Signal detection response from the service */
interface LocationResponse {
  [key: string]: osdModel.LocationSolutionOSD[];
}

/**
 * Wrapper around the convert function that makes the processor supply other arguments
 */
export type ConvertLocationSolutionWrapper = (
  ls: osdModel.LocationSolutionOSD,
  assocs: model.SignalDetectionEventAssociation[]
) => Promise<model.LocationSolution>;

/**
 * The save events service response data
 */
export interface SaveEventsServiceResponse {
  /** the events that were stored successfully */
  storedEvents: string[];
  /** the events that were updated successfully */
  updatedEvents: string[];
  /** the events that failed to store successfully */
  errorEvents: string[];
}

/**
 * Helper method makes each individual FP Service call
 * to Compute FeaturePrediction called by computeFeaturePredictions public method
 * @param input FP Service Request Body
 * @param eventHyp  Hypothesis to get fp's for
 * @param httpWrapper calls appropriate service based on yaml
 * @param requestConfig HttpWrapper Configuration
 * @returns The location solution with feature prediction list populated
 */
export async function getFeaturePredictionsFromService(
  input: model.FeaturePredictionStreamingInput,
  eventHyp: model.EventHypothesis,
  convertLocationSolutionWrapper: ConvertLocationSolutionWrapper,
  httpWrapper: HttpClientWrapper,
  requestConfig: any
): Promise<model.LocationSolution> {
  // const requestConfig = this.settings.backend.services.computeFeaturePredictions.requestConfig;
  // tslint:disable-next-line:max-line-length
  logger.debug(
    `ComputeFP sending service request: ${JSON.stringify(
      requestConfig,
      undefined,
      2
    )} query: ${JSON.stringify(input, undefined, 2)}`
  );
  performanceLogger.performance(
    'computeFeaturePredictions',
    'requestedFromService',
    input.sourceLocation.id
  );
  let locationSolution: model.LocationSolution;
  try {
    const response: HttpResponse<osdModel.LocationSolutionOSD> = await httpWrapper.request<
      osdModel.LocationSolutionOSD
    >(requestConfig, input);
    performanceLogger.performance(
      'computeFeaturePredictions',
      'returnedFromService',
      input.sourceLocation.id
    );

    const emptyLocationSolution = !response || !response.data || isObjectEmpty(response.data);
    if (!emptyLocationSolution) {
      locationSolution = await convertLocationSolutionWrapper(response.data, eventHyp.associations);
    } else {
      logger.warn(`LocationSolution returned from FP Service is not defined!`);
    }
  } catch (e) {
    logger.warn(`Failed to compute Feature Prediction for event ${eventHyp.eventId} ${e}`);
  }
  return locationSolution;
}

/**
 * Calls a remote service for events in time range
 * @param timeRange time range to request events withing
 * @param currentStageId the current stage id, used to select the correct current hypothesis
 * @param allSignalDetections list of all sds in the system
 * @param stations default stations
 * @param getStationByChannelIdWrapper callback to get stations by channel id
 * @param validateFm warns if the feature measurement isn't in the system
 * @param httpWrapper the http wrapper used to make requests
 * @param requestConfig the configuration with the service urls
 */
export async function getEventsInTimeRangeFromService(
  timeRange: TimeRange,
  currentStageId: string,
  allSignalDetections: SignalDetection[],
  stations: ProcessingStation[],
  getStationByChannelIdWrapper: (id: string) => ProcessingStation,
  httpWrapper: HttpClientWrapper,
  requestConfig: any
): Promise<model.Event[]> {
  const query = {
    startTime: toOSDTime(timeRange.startTime),
    endTime: toOSDTime(timeRange.endTime)
  };
  const response: HttpResponse<osdModel.EventOSD[]> = await httpWrapper.request<
    osdModel.EventOSD[]
  >(requestConfig, query);
  if (response !== undefined && response.data !== undefined && response.data.length > 0) {
    const gatewayEvents = response.data.map(eventOsd =>
      convertEventFromOSD(
        eventOsd,
        currentStageId,
        stations,
        allSignalDetections,
        getStationByChannelIdWrapper
      )
    );
    const alignedEvents = gatewayEvents.map(alignPreferredEventHypothesisHistoryWithHypothesis);
    return alignedEvents;
  }
  return [];
}

/**
 * Calls the event locate service
 * @param userContext user context
 * @param osdEventHypo the osd event hypothesis to locate
 * @param signalDetections signal detections associated to that event
 * @param locationBehaviors location behaviors set by the user
 * @param httpWrapper the http client wrapper
 * @param requestConfig the request config
 */
export async function callLocateEvent(
  osdEventHypo: osdModel.EventHypothesisOSD,
  signalDetections: SignalDetection[],
  locationBehaviors: model.LocationBehavior[],
  convertLocationSolutionWrapper: ConvertLocationSolutionWrapper,
  httpWrapper: HttpClientWrapper,
  requestConfig: any
): Promise<model.LocationSolution[]> {
  const sdHyps = signalDetections.map(sd => sd.currentHypothesis);
  const signalDetectionBehaviorsMap = generateSignalDetectionBehaviorsMap(
    locationBehaviors,
    sdHyps
  );
  const eventHypothesisToEventLocatorPluginConfigurationOptionMap: {
    [id: string]: osdModel.EventLocationDefinitionAndFieldMapOSD;
  } = {};
  eventHypothesisToEventLocatorPluginConfigurationOptionMap[osdEventHypo.id] = {
    eventLocationDefinition: {
      ...systemConfig.defaultEventLocationDefinition,
      signalDetectionBehaviorsMap
    },
    fieldMap: {}
  };
  const query: osdModel.LocateEventQueryOSD = {
    eventHypotheses: [osdEventHypo],
    signalDetections: signalDetections.map(convertSDtoOSD),
    parameters: {
      // TODO Where do we put this variable?
      pluginName: 'eventLocationApacheLmPlugin',
      eventHypothesisToEventLocatorPluginConfigurationOptionMap
    }
  };
  try {
    const response: HttpResponse<LocationResponse> = await httpWrapper.request<LocationResponse>(
      requestConfig,
      query
    );
    // If the call is empty then it failed (caught by HttpWrapper)
    if (response && response.data && isObjectEmpty(response.data)) {
      logger.warn(`LocateEvent endpoint call failed no results returned.`);
      // tslint:disable-next-line:max-line-length
      logger.debug(
        `Location Solution sending service request: ${JSON.stringify(
          requestConfig,
          undefined,
          2
        )} query: ${JSON.stringify(query, undefined, 2)}`
      );
      return [];
    }
    if (!response.data) return [];
    // Only process first map entry (should be only one map entry with one LocationSolution)
    // tslint:disable-next-line: no-for-in
    for (const key in response.data) {
      if (!response.data.hasOwnProperty(key)) {
        continue;
      }
      // Convert OSD Location Solutions
      const locationSolutionsOSD: osdModel.LocationSolutionOSD[] = response.data[key];
      if (locationSolutionsOSD) {
        const promises = locationSolutionsOSD.map(async losOSD =>
          convertLocationSolutionWrapper(losOSD, osdEventHypo.associations)
        );
        const locSols = await Promise.all(promises);
        return locSols;
      }
      return [];
    }
  } catch (e) {
    logger.warn(`Problem calling Locate Event error ${e}`);
  }
}

/**
 * Calls the Save events service.
 *
 * @param events the events to save
 * @param httpWrapper the instance of the http wrapper
 * @param returns a feature measurement by id
 * @param requestConfig the axios request configuration
 *
 * @returns a saved events http response
 */
export async function saveEventsToService(
  userContext: UserContext,
  events: model.Event[],
  httpWrapper: HttpClientWrapper,
  requestConfig: AxiosRequestConfig
): Promise<HttpResponse<SaveEventsServiceResponse>> {
  if (!httpWrapper) {
    throw new Error('Error: save events: invalid http wrapper');
  }

  if (!events || events.length === 0) {
    throw new Error('Error: save events: requires at least one valid event');
  }

  if (!requestConfig) {
    throw new Error('Error: save events: invalid request configuration');
  }

  // convert to the OSD event format
  const osdEvents = events.map(event => convertEventToOSD(userContext, event));
  logger.debug(
    `Calling save events service: request: ${jsonPretty(requestConfig)} query: ${jsonPretty(
      osdEvents
    )}`
  );
  const response = await httpWrapper.request<SaveEventsServiceResponse>(
    requestConfig,
    JSON.stringify(osdEvents)
  );
  logger.info(`Saved events result: (${response.status}) : ${jsonPretty(response.data)}`);
  const httpOkay = 200;
  if (response.status !== httpOkay) {
    logger.error(`Failed to save the following events: ${String(osdEvents.map(e => e.id))}`);
  } else if (response.data.errorEvents.length > 0) {
    logger.error(`Failed to save the following events: ${String(response.data.errorEvents)}`);
  }
  return response;
}

/**
 * Computes network magnitudes from service
 * @param httpWrapper the http wrapper
 * @param requestConfig the request config
 * @param event event to calculate mags for
 * @param definingBehaviors defining settings for the magnitude
 * @param sdHyps associated signal detection hypothesis
 * @param stationIdToHypMap mapping from sd hyp ids to station ids
 * @param hypothesisIdToLocationMap mapping from sdhyps to locations
 * @param convertLocationSolutionWrapper wrapper around the conversion of location solutions
 */
export async function computeNetworkMagnitudeSolutionsWithService(
  httpWrapper: HttpClientWrapper,
  requestConfig: any,
  event: osdModel.EventHypothesisOSD,
  definingBehaviors: model.DefiningBehavior[],
  sdHyps: SignalDetectionHypothesisOSD[],
  stationIdToHypMap: { [s: string]: string },
  hypothesisIdToLocationMap: { [s: string]: Location },
  convertLocationSolutionWrapper: ConvertLocationSolutionWrapper
): Promise<model.LocationSolution[]> {
  const query: model.NetworkMagnitudeServiceQuery = {
    definingBehaviors,
    event,
    processingMetadata: {
      detectionHypotheses: sdHyps,
      stationIdsByDetectionHypothesisIds: stationIdToHypMap,
      stationLocationsByDetectionHypothesisIds: hypothesisIdToLocationMap
    }
  };
  logger.debug(
    `Sending network service request: ` +
      `${JSON.stringify(
        requestConfig.backend.services.computeNetworkMagnitudeSolution.requestConfig,
        undefined,
        2
      )}` +
      ` query: ${JSON.stringify(query, undefined, 2)}`
  );
  const response = (await httpWrapper.request<{ result: osdModel.EventHypothesisOSD }>(
    requestConfig.backend.services.computeNetworkMagnitudeSolution.requestConfig,
    query
  )) as any;
  if (isHttpResponseError(response)) {
    throw new ApolloError('Network magnitude failed to compute', 'Service Error');
  }

  logger.debug(`compute network magnitude solution returned ${JSON.stringify(response.data)}`);
  const eventHyp: osdModel.EventHypothesisOSD = response.data.result;
  const promises = eventHyp.locationSolutions.map(async ls =>
    convertLocationSolutionWrapper(ls, eventHyp.associations)
  );
  const locs = await Promise.all(promises);
  return locs;
}
