import { MILLISECONDS_IN_SECOND, readJsonData, toEpochSeconds } from '@gms/common-util';
import config from 'config';
import path from 'path';
import { TimeRange } from '../common/model';
import { gatewayLogger as logger } from '../log/gateway-logger';
import { HttpMockWrapper } from '../util/http-wrapper';
import { resolveTestDataPaths } from '../util/test-data-util';
import * as model from './model';
import * as osdModel from './model-osd';

/**
 * Encapsulates backend data supporting retrieval by the API gateway.
 */
interface SdDataStore {
  readonly signalDetections: osdModel.SignalDetectionOSD[];
}

/**
 * Signal detection for time range input
 */
interface SignalDetectionForTimeRangeInput {
  readonly stationNames: string[];
  readonly startTime: string;
  readonly endTime: string;
}

// Declare a data store for the mock signal detection backend
let dataStore: SdDataStore;

/**
 * Initialize the mock backend for signal detections.
 *
 * @param httpMockWrapper the http mock wrapper
 */
export function initialize(httpMockWrapper: HttpMockWrapper): void {
  logger.info('Initializing the Signal Detection Mock-backend service');

  // Load test data from the configured data set
  dataStore = loadTestData();

  const backendConfig = config.get('signalDetection.backend');
  httpMockWrapper.onMock(
    backendConfig.services.sdsByStation.requestConfig.url,
    getSignalDetectionsForTimerange
  );
  httpMockWrapper.onMock(backendConfig.services.saveSds.requestConfig.url, saveSignalDetections);
}

/**
 * Help function to convert date string into epoch seconds
 */
export function getEpochSeconds(dateString: any): number {
  if (dateString === undefined) {
    return 0;
  }

  // Maybe if this is a number then it is epoch just return
  if (!isNaN(dateString)) {
    return dateString;
  }
  return new Date(dateString).getTime() / MILLISECONDS_IN_SECOND;
}

/**
 * Get signal detection hypothesis by ID
 * @param hypoId signal detection hypothesis ID
 * @returns a Signal Detection Hypothesis
 */
export function getSignalDetectionHypothesisById(
  hypoId: string
): osdModel.SignalDetectionHypothesisOSD {
  let currentSDH: osdModel.SignalDetectionHypothesisOSD;
  dataStore.signalDetections.forEach(sd => {
    if (sd && sd.signalDetectionHypotheses && sd.signalDetectionHypotheses.length > 0) {
      if (sd.signalDetectionHypotheses[sd.signalDetectionHypotheses.length - 1].id === hypoId) {
        currentSDH = sd.signalDetectionHypotheses[sd.signalDetectionHypotheses.length - 1];
      }
    }
  });
  return currentSDH;
}

/**
 * Get signal detections for time range mapped to a station ID
 * @param requestConfig as SignalDetectionForTimeRangeInput
 * @returns a map as Map<string, model.SignalDetection[]>
 */
export function getSignalDetectionsForTimerange(
  requestConfig: SignalDetectionForTimeRangeInput
): osdModel.SignalDetectionOSD[] {
  const timeRange: TimeRange = {
    startTime: getEpochSeconds(requestConfig.startTime),
    endTime: getEpochSeconds(requestConfig.endTime)
  };

  const stationNames = requestConfig.stationNames;
  let signalDetections: osdModel.SignalDetectionOSD[] = [];
  stationNames.forEach(stationId => {
    const filteredSds = dataStore.signalDetections.filter(sd => {
      if (sd.signalDetectionHypotheses && sd.signalDetectionHypotheses.length > 0) {
        const currentHypo = sd.signalDetectionHypotheses[sd.signalDetectionHypotheses.length - 1];

        // Look up the Arrival Time FM the measurement value is a string and not a number
        const arrivalTimeMeasurementValue = currentHypo.featureMeasurements.find(
          fm => fm.featureMeasurementType === model.FeatureMeasurementTypeName.ARRIVAL_TIME
        ).measurementValue as osdModel.InstantMeasurementValueOSD;
        const value: any = arrivalTimeMeasurementValue.value;
        const arrivalTimeEpoch = toEpochSeconds(value);
        if (
          arrivalTimeEpoch &&
          stationId === sd.stationName &&
          arrivalTimeEpoch >= timeRange.startTime &&
          arrivalTimeEpoch <= timeRange.endTime
        ) {
          return true;
        }
      }
    });
    signalDetections = signalDetections.concat(filteredSds);
  });
  return Object.seal(signalDetections);
}

/**
 * Load test data into the mock backend data store from the configured test data set.
 */
export function loadTestData(): SdDataStore {
  const testDataConfig = config.get('testData.standardTestDataSet');

  // Read the Signal Detections from the JSON file
  const dataPath = resolveTestDataPaths().jsonHome;
  const signalDetectionFilePath = dataPath
    .concat(path.sep)
    .concat(testDataConfig.signalDetection.signalDetectionFileName);

  logger.info(`Loading Signal Detections from: ${signalDetectionFilePath}`);

  let signalDetections: osdModel.SignalDetectionOSD[] = [];
  try {
    signalDetections = readJsonData(signalDetectionFilePath);
  } catch (e) {
    logger.error(`Failed to read signal detections from file:
            ${testDataConfig.signalDetection.signalDetectionFileName}`);
  }
  logger.info(`Loaded ${signalDetections.length} Signal Detections`);
  return { signalDetections };
}

/**
 * Saves signal detections to the data store
 */
export function saveSignalDetections(query: string) {
  const sds: osdModel.SignalDetectionOSD[] = JSON.parse(query);
  if (sds) {
    sds.forEach(sd => {
      const index = dataStore.signalDetections.findIndex(dsSd => dsSd.id === sd.id);
      dataStore.signalDetections[index] = sd;
    });
  }
}
