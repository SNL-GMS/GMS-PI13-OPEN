import {
  getSecureRandomNumber,
  MILLISECONDS_IN_SECOND,
  readJsonData,
  toEpochSeconds,
  toOSDTime,
  uuid4
} from '@gms/common-util';
import config from 'config';
import { produce } from 'immer';
import path from 'path';
import { TimeRange } from '../common/model';
import { gatewayLogger as logger } from '../log/gateway-logger';
import { performanceLogger } from '../log/performance-logger';
import { FeatureMeasurementTypeName } from '../signal-detection/model';
import {
  PhaseTypeMeasurementValueOSD,
  SignalDetectionHypothesisOSD
} from '../signal-detection/model-osd';
import { ProcessingStationProcessor } from '../station/processing-station/processing-station-processor';
import { mockBackendConfig } from '../system-config';
import { getRandomLatitude, getRandomLongitude } from '../util/common-utils';
import { HttpMockWrapper } from '../util/http-wrapper';
import { resolveTestDataPaths } from '../util/test-data-util';
import {
  DefiningBehavior,
  FeaturePrediction,
  FeaturePredictionStreamingInput,
  MagnitudeModel,
  MagnitudeType,
  NetworkMagnitudeServiceQuery
} from './model-and-schema/model';
import * as osdModel from './model-and-schema/model-osd';
import { randomizeResiduals } from './utils/location-utils';

/**
 * Mock backend HTTP services providing access to processing station data. If mock services are enabled in the
 * configuration file, this module loads a test data set specified in the configuration file and configures
 * mock HTTP interfaces for the API gateway backend service calls.
 */

/** We multiply a random number by this to get our mocked magnitude */
const scalarFactorForMockedMagnitudes = 9;

/** Currently used mag types */
const SUPPORTED_MAG_TYPES = [
  MagnitudeType.MB,
  MagnitudeType.MBMLE,
  MagnitudeType.MS,
  MagnitudeType.MSMLE
];

/**
 * Event Data store for holding the data read in by loadTestData
 */
interface EventDataStore {
  eventList: osdModel.EventOSD[];
  networkMagnitudeSolutionsMap: Map<MagnitudeType, osdModel.NetworkMagnitudeSolutionOSD[]>;
}

let dataStore: EventDataStore = {
  eventList: [],
  networkMagnitudeSolutionsMap: new Map()
};

/**
 * Events by time input
 */
interface EventsByTimeInput {
  startTime: string;
  endTime: string;
  minLat?: number;
  maxLat?: number;
  minLong?: number;
  maxLong?: number;
}

/**
 * Get events by IDs input
 */
interface GetEventsByIdsInput {
  ids: string[];
}

/**
 * TODO: once the endpoint for network magnitude solution is up and running remove the mockEnabled variable
 * Configure mock HTTP interfaces for a simulated set of QC mask backend services.
 * @param httpMockWrapper The HTTP mock wrapper used to configure mock backend service interfaces
 */
export function initialize(httpMockWrapper: HttpMockWrapper, mockEnabled: boolean) {
  logger.info('Initializing mock backend for Event data');

  if (!httpMockWrapper) {
    throw new Error('Cannot initialize mock Event services with undefined HTTP mock wrapper');
  }

  // Initialize Map to have all supported mag types
  SUPPORTED_MAG_TYPES.forEach(magType => dataStore.networkMagnitudeSolutionsMap.set(magType, []));

  // Load test data from the configured data set
  dataStore = loadTestData();

  // Load the Event backend service config settings
  const backendConfig = config.get('event.backend');

  // Override the OSD methods if in mock mode
  if (mockEnabled) {
    httpMockWrapper.onMock(
      backendConfig.services.getEventsByTimeAndLatLong.requestConfig.url,
      getEventsByTimeAndLatLong
    );
    httpMockWrapper.onMock(backendConfig.services.getEventsByIds.requestConfig.url, getEventsByIds);
    httpMockWrapper.onMock(
      backendConfig.services.computeFeaturePredictions.requestConfig.url,
      computeFeaturePredictions
    );
    httpMockWrapper.onMock(backendConfig.services.locateEvent.requestConfig.url, locateEvent);
    httpMockWrapper.onMock(backendConfig.services.saveEvents.requestConfig.url, saveEvents);
    httpMockWrapper.onMock(
      backendConfig.services.computeNetworkMagnitudeSolution.requestConfig.url,
      computeNetworkMagnitudeSolution
    );
  }
}

/**
 * Load test data into the mock backend data store from the configured test data set.
 */
function loadTestData(): EventDataStore {
  // Get test data configuration settings
  const testDataConfig = config.get('testData.standardTestDataSet');

  // mock events and hypotheses
  let serializedEvents: osdModel.EventOSD[] = [];

  // Read the Signal Detections from the JSON file
  const dataPath = resolveTestDataPaths().jsonHome;
  const eventFilePath = dataPath.concat(path.sep).concat(testDataConfig.events.eventsFileName);

  // Read the necessary files into arrays of objects
  logger.info(`Loading Event test data from path: ${eventFilePath}`);
  try {
    serializedEvents = readJsonData(eventFilePath);
  } catch (e) {
    logger.error(`Failed to read event from file: ${eventFilePath}`);
  }
  logger.info(`Event mock backend loaded ${serializedEvents.length} events`);

  serializedEvents.forEach(event => {
    dataStore.eventList.push(event);
  });

  return dataStore;
}

/**
 * Retrieve events that match provided IDs.
 * @param ids The IDs of the events to retrieve
 */
export async function getEventsByIds(input: GetEventsByIdsInput): Promise<osdModel.EventOSD[]> {
  const ids = input.ids;
  const events = dataStore.eventList.filter(event => ids.indexOf(event.id) >= 0);
  return events;
}

/**
 * Gets events by a time and latitude and longitude
 * @param input events by time input
 * @returns Event OSD representation as a promise
 */
export async function getEventsByTimeAndLatLong(
  input: EventsByTimeInput
): Promise<osdModel.EventOSD[]> {
  const timeRange: TimeRange = {
    startTime: toEpochSeconds(input.startTime),
    endTime: toEpochSeconds(input.endTime)
  };
  if (!timeRange) {
    logger.error('No time range given for event mock backend');
  }
  let eventsInRange: osdModel.EventOSD[] = [];
  eventsInRange = dataStore.eventList.filter(event => {
    const eventTime =
      event.hypotheses[event.hypotheses.length - 1].preferredLocationSolution.locationSolution
        .location.time;
    const eventTimeSec = toEpochSeconds(eventTime);
    return eventTimeSec >= timeRange.startTime && eventTimeSec < timeRange.endTime;
  });
  return eventsInRange;
  // return eventsInRange;
}

/**
 * Locate event is a streaming call to COI to compute the location solution
 * @param input Event Hypothesis to use in the compute call (contains the EventHypothesis)
 * @returns OSD Map (not really a map) of LocationSolution[]
 * (this should be a copy of the preferred ls with a new id)
 */
export async function locateEvent(input: osdModel.LocateEventQueryOSD) {
  if (!input || !input.eventHypotheses || input.eventHypotheses.length === 0) {
    return { key: [] };
  }

  // Only support one event hypothesis for now
  const eventHypothesis: osdModel.EventHypothesisOSD = input.eventHypotheses[0];
  const preferredLocationSolution: osdModel.LocationSolutionOSD =
    eventHypothesis.preferredLocationSolution.locationSolution;

  // Create a location solution based on preferred LS passed. Give it a new uuid and return
  const locRestraints =
    input.parameters.eventHypothesisToEventLocatorPluginConfigurationOptionMap[
      input.eventHypotheses[0].id
    ].eventLocationDefinition.locationRestraints;

  const solutions: osdModel.LocationSolutionOSD[] = locRestraints.map(lr =>
    produce<osdModel.LocationSolutionOSD>(preferredLocationSolution, draftState => {
      draftState.locationRestraint.depthRestraintType = lr.depthRestraintType;
      draftState.id = uuid4();
      const location = getRandomLocationForLocate(toEpochSeconds(draftState.location.time));
      draftState.location = {
        ...location
      };
      draftState.locationBehaviors = randomizeResiduals(
        preferredLocationSolution.locationBehaviors
      );
    })
  );

  const toReturn = {};
  toReturn[eventHypothesis.id] = solutions;
  return toReturn;
}

/**
 * Return the Feature Predictions in the LocationSolution found by how?
 * @param input streaming input to compute feature predictions
 * @return promise of location solution with updated feature predictions
 */
export async function computeFeaturePredictions(
  input: FeaturePredictionStreamingInput
): Promise<osdModel.LocationSolutionOSD> {
  performanceLogger.performance(
    'computeFeaturePredictions',
    'enteringService',
    `${input.sourceLocation.id}`
  );
  const updatedInput = produce<FeaturePredictionStreamingInput>(input, draftState => {
    const eventArrivalTime =
      new Date(draftState.sourceLocation.location.time).valueOf() / MILLISECONDS_IN_SECOND;

    const eventLocation = {
      latitudeDegrees: draftState.sourceLocation.location.latitudeDegrees,
      longitudeDegrees: draftState.sourceLocation.location.longitudeDegrees,
      elevationKm: draftState.sourceLocation.location.depthKm,
      time: draftState.sourceLocation.location.time
    };
    // Create a unique entry for each call since there will be multiple async calls
    // made to the mock backend
    const fpList: any[] = [];
    draftState.receiverLocations.forEach(channel => {
      // Set as any in order to change number to string to send to OSD
      const distanceToSource = ProcessingStationProcessor.Instance().getDistanceToSource(
        eventLocation,
        ProcessingStationProcessor.Instance().getStationByChannelName(channel.name)
      );
      // Create mocked feature prediction for ARRIVAL, SLOWNESS, AZIMUTH
      fpList.push(
        createMockArrivalFeaturePrediction(
          channel.name,
          input.phase,
          eventArrivalTime,
          distanceToSource.degrees
        )
      );
      fpList.push(createMockSlownessFeaturePrediction(channel.name, input.phase));
      fpList.push(createMockAzimuthFeaturePrediction(channel.name, input.phase));
    });

    // Set the FPs in the source location
    draftState.sourceLocation.featurePredictions = fpList;
  });

  performanceLogger.performance(
    'computeFeaturePredictions',
    'returningFromService',
    `${input.sourceLocation.id}`
  );
  return updatedInput.sourceLocation;
}

/**
 * Saves events to the data store
 */
export function saveEvents(query: any): any {
  const events = JSON.parse(query);
  if (events) {
    events.forEach(event => {
      const index = dataStore.eventList.findIndex(dsEvent => dsEvent.id === event.id);
      dataStore.eventList[index] = event;
    });
  }
  return {
    storedEvents: events.map(e => e.id),
    updatedEvents: [],
    errorEvents: []
  };
}

/**
 * Computes a network magnitude solution event data object
 */
export function computeNetworkMagnitudeSolution(
  input: NetworkMagnitudeServiceQuery
): {
  result: osdModel.EventHypothesisOSD;
  rejectedInputs: [{ stationId: string; rational: string }];
} {
  const locationSolutions = input.event.locationSolutions;
  const magType = input.definingBehaviors[0]
    ? (input.definingBehaviors[0].magnitudeType as MagnitudeType)
    : undefined;
  locationSolutions.forEach(ls => {
    const newSolution = buildMagnitudeWithDefiningSettings(
      input.processingMetadata.detectionHypotheses,
      input.processingMetadata.stationIdsByDetectionHypothesisIds,
      input.definingBehaviors,
      magType
    );
    ls.networkMagnitudeSolutions.push(newSolution);
  });
  return {
    result: input.event,
    rejectedInputs: [{ stationId: 'PDAR', rational: 'actual-rational' }]
  };
}

/**
 * Builds a network magnitude with the correct defining settings
 * @param sdHyps Hypothesis with data for the station mags
 * @param sdHypIdToStationNameMap mapping of sd hyp id to station ids
 * @param definingBehaviors mapping of station id to defining setting
 * @param magnitudeType magnitude type to create
 */
function buildMagnitudeWithDefiningSettings(
  sdHyps: SignalDetectionHypothesisOSD[],
  sdHypIdToStationNameMap: { [s: string]: string },
  definingBehaviors: DefiningBehavior[],
  magnitudeType: MagnitudeType
): osdModel.NetworkMagnitudeSolutionOSD {
  const networkMagnitudeBehaviors: osdModel.NetworkMagnitudeBehaviorOSD[] = sdHyps
    .filter(sdHyp => sdHypIdToStationNameMap[sdHyp.id] !== undefined)
    .map(sdHyp => {
      const pdarStationId = 'PDAR';
      const cmarStationId = 'CMAR';
      const simulateEmptyResponse =
        sdHypIdToStationNameMap[sdHyp.id] === pdarStationId ||
        sdHypIdToStationNameMap[sdHyp.id] === cmarStationId;
      return {
        defining: definingBehaviors.find(
          defB => defB.stationName === sdHypIdToStationNameMap[sdHyp.id]
        ).defining,
        stationMagnitudeSolution: {
          type: magnitudeType,
          model: MagnitudeModel.VEITH_CLAWSON,
          stationName: sdHypIdToStationNameMap[sdHyp.id],
          phase: (sdHyp.featureMeasurements.find(
            fm => fm.featureMeasurementType === FeatureMeasurementTypeName.PHASE
          ).measurementValue as PhaseTypeMeasurementValueOSD).value,
          magnitude: !simulateEmptyResponse
            ? getSecureRandomNumber() * scalarFactorForMockedMagnitudes
            : undefined,
          magnitudeUncertainty: !simulateEmptyResponse ? getSecureRandomNumber() : undefined,
          modelCorrection: 0,
          stationCorrection: 0,
          measurement: sdHyp.featureMeasurements.find(
            fm => fm.featureMeasurementType === FeatureMeasurementTypeName.AMPLITUDE_A5_OVER_2
          )
        },
        residual: getSecureRandomNumber(),
        weight: 1
      };
    });
  const areAnyMagsDefining = networkMagnitudeBehaviors.reduce(
    (accum, val) => val.defining || accum,
    false
  );
  const networkMagnitude: osdModel.NetworkMagnitudeSolutionOSD = {
    magnitude: areAnyMagsDefining
      ? networkMagnitudeBehaviors
          .map(nms => nms.stationMagnitudeSolution.magnitude)
          .filter(mag => mag !== undefined)
          .reduce((accum, val) => Number(accum) + Number(val), 0) / networkMagnitudeBehaviors.length
      : undefined,
    magnitudeType,
    uncertainty: areAnyMagsDefining ? getSecureRandomNumber() : undefined,
    networkMagnitudeBehaviors
  };
  return networkMagnitude;
}

/**
 * creates a Mock Azimuth Feature Prediction
 *
 * @param channelName channel id as string
 * @param phaseTYpe phase type as string
 *
 * @returns MockAzimuthFeaturePrediction
 */
function createMockAzimuthFeaturePrediction(channelName: string, phase: string): FeaturePrediction {
  const testDataConfig = config.get('testData.additionalTestData');
  const dataPath = resolveTestDataPaths().additionalDataHome;
  const azimuthFeaturePrediction = readJsonData(
    dataPath.concat(path.sep).concat(testDataConfig.featurePredictionAzimuth)
  )[0];
  const threeSixty = 360;

  const mockAzimuthFeaturePrediction = {
    ...azimuthFeaturePrediction,
    phase,
    channelName,
    predictedValue: {
      referenceTime: Date.now(),
      measurementValue: {
        ...azimuthFeaturePrediction.predictedValue.measurementValue,
        value: getSecureRandomNumber() * threeSixty
      }
    }
  };

  return mockAzimuthFeaturePrediction;
}

/**
 * creates a Mock Slowness Feature Prediction
 *
 * @param channelName channel id as string
 * @param phaseTYpe phase type as string
 *
 * @returns mockSlownessFeaturePrediction
 */
function createMockSlownessFeaturePrediction(
  channelName: string,
  phase: string
): FeaturePrediction {
  const testDataConfig = config.get('testData.additionalTestData');
  const dataPath = resolveTestDataPaths().additionalDataHome;
  const slownessFeaturePrediction = readJsonData(
    dataPath.concat(path.sep).concat(testDataConfig.featurePredictionSlowness)
  )[0];
  // Offset used to make  slowness more variable for mock data
  const randomOffset = 20;
  const mockSlownessFeaturePrediction = {
    ...slownessFeaturePrediction,
    phase,
    channelName,
    predictedValue: {
      referenceTime: Date.now(),
      measurementValue: {
        ...slownessFeaturePrediction.predictedValue.measurementValue,
        value: getSecureRandomNumber() * randomOffset
      }
    }
  };

  return mockSlownessFeaturePrediction;
}

/**
 * creates a Mock Arrival Time Feature Prediction
 *
 * @param channelName channel id as string
 * @param phaseTYpe phase type as string
 *
 * @returns mockArrivalFeaturePrediction
 */
function createMockArrivalFeaturePrediction(
  channelName: string,
  phase: string,
  eventArrivalTime: number,
  dts: number
): FeaturePrediction {
  const testDataConfig = config.get('testData.additionalTestData');
  const dataPath = resolveTestDataPaths().additionalDataHome;
  const arrivalFeaturePrediction = readJsonData(
    dataPath.concat(path.sep).concat(testDataConfig.featurePredictionArrival)
  )[0];

  const randomSeconds = 300;
  const dtsMultiplier = 20;
  const randomTime =
    eventArrivalTime + getSecureRandomNumber() * randomSeconds + dts * dtsMultiplier;

  const mockArrivalFeaturePrediction = {
    ...arrivalFeaturePrediction,
    phase,
    channelName,
    predictedValue: {
      ...arrivalFeaturePrediction.predictedValue,
      value: toOSDTime(randomTime)
    }
  };

  return mockArrivalFeaturePrediction;
}

/**
 * Creates a new random location for a locate call
 * @param startTime the start time of the event
 */
export function getRandomLocationForLocate(startTime: number): osdModel.EventLocationOSD {
  const eventLocation = {
    latitudeDegrees: getRandomLatitude(),
    longitudeDegrees: getRandomLongitude(),
    depthKm: mockBackendConfig.defaultEventDepth,
    time: toOSDTime(startTime + 1)
  };
  return eventLocation;
}
