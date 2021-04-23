import {
  MonitorValue,
  SohMonitorType,
  UiHistoricalAcei,
  UiHistoricalAceiInput,
  UiHistoricalSoh,
  UiHistoricalSohInput,
  UiStationSoh
} from '@gms/common-graphql/lib/graphql/soh/types';
import { getSecureRandomNumber, readJsonData, toEpochSeconds } from '@gms/common-util';
import config from 'config';
import { cloneDeep } from 'lodash';
import path from 'path';
import { gatewayLogger as logger } from '../log/gateway-logger';
import { ProcessingStation } from '../station/processing-station/model';
import { ProcessingStationProcessor } from '../station/processing-station/processing-station-processor';
import { HttpMockWrapper } from '../util/http-wrapper';
import { resolveTestDataPaths } from '../util/test-data-util';

/**
 * Encapsulates backend data supporting retrieval by the API gateway.
 */
interface UiStationSohDataStore {
  stationSoh: UiStationSoh[];
  historicalSohData: UiHistoricalSoh;
  historicalAceiData: UiHistoricalAcei[];
}

// Declare a data store for the data acquisition status mask backend
let dataStore: UiStationSohDataStore;
/**
 * Reads in test data and stores it
 */
export const initialize = (httpMockWrapper: HttpMockWrapper): void => {
  logger.info('Initializing mock backend for Data Acquisition SOH data.');

  if (!httpMockWrapper) {
    throw new Error(
      'Cannot initialize mock Data Acquisition SOH services with undefined HTTP mock wrapper.'
    );
  }

  dataStore = loadTestData();

  // Load the data acquisition SOH backend service config settings
  const backendConfig = config.get('performanceMonitoring.backend');
  httpMockWrapper.onMock(
    backendConfig.services.stationSohLatest.requestConfig.url,
    getStationSohData
  );

  // Configure mock service interface for historical soh lag and missing
  httpMockWrapper.onMock(
    backendConfig.services.getHistoricalSohData.requestConfig.url,
    getHistoricalSohData
  );

  // Configure mock service interface for historical acei data
  httpMockWrapper.onMock(
    backendConfig.services.getHistoricalAceiData.requestConfig.url,
    getHistoricalAceiData
  );
};

/**
 * Reads in test data and stores it.
 */
function loadTestData(): UiStationSohDataStore {
  // Get test data configuration settings
  const testDataConfig = config.get('testData.additionalTestData');
  const dataPath = resolveTestDataPaths().additionalDataHome;

  // Load station soh from file
  const stationSohPath = path.join(dataPath, testDataConfig.stationSoh);
  logger.info(`Loading data acquisition SOH test data from path: ${stationSohPath}`);
  const stationSohResponse: any[] = readJsonData(stationSohPath);

  // Load historical missing/lag soh from file
  const historicalSohPath = path.join(dataPath, testDataConfig.historicalSohFilename);
  logger.info(`Loading historical soh test data from path: ${historicalSohPath}`);
  const historicalSohResponse: any = readJsonData(historicalSohPath);
  logger.info(
    `Loaded historical soh number of channels: ${historicalSohResponse.monitorValues.length} `
  );

  // Load historical acei from file
  const historicalAceiPath = path.join(dataPath, testDataConfig.historicalAceiFilename);
  logger.info(`Loading historical acei test data from path: ${historicalAceiPath}`);
  const historicalAceiResponse: any = readJsonData(historicalAceiPath);
  logger.info(`Loaded historical acei number of channels: ${historicalAceiResponse.length} `);

  return {
    stationSoh: stationSohResponse,
    historicalSohData: historicalSohResponse,
    historicalAceiData: historicalAceiResponse
  };
}

/**
 * Handle cases where the data store has not been initialized.
 */
function handleUninitializedDataStore() {
  // If the data store is uninitialized, throw an error.
  if (!dataStore) {
    dataStore = loadTestData();
    if (!dataStore) {
      throw new Error('Mock backend data acquisition data store has not been initialized.');
    }
  }
}

/**
 * Gets station soh objects. Useful for unit tests
 *
 * @returns UiStationSoh[]
 */
export function getStationSohData(): UiStationSoh[] {
  handleUninitializedDataStore();
  dataStore.stationSoh.forEach(soh => (soh.time = Date.now()));
  return dataStore.stationSoh;
}

/**
 * Gets historical lag objects. Useful for unit tests
 *
 * @returns UiHistoricalLag
 */
export function getHistoricalSohData(input: UiHistoricalSohInput): UiHistoricalSoh {
  handleUninitializedDataStore();

  // Grab first in the json list and use it as a template
  const templateMonitorValue: MonitorValue = dataStore.historicalSohData.monitorValues[0];
  const station: ProcessingStation = ProcessingStationProcessor.Instance().getStationByName(
    input.stationName
  );

  // Walk thru the station channels building a monitorValues for Lag and/or Missing
  // depending on the input monitorTypes set
  const includeMissing =
    input.sohMonitorTypes.filter(type => type === SohMonitorType.MISSING).length > 0;
  const includeLag = input.sohMonitorTypes.filter(type => type === SohMonitorType.LAG).length > 0;

  const monitorValues: MonitorValue[] = station.channels.map(channel => ({
    channelName: channel.name,
    valuesByType: {
      LAG: includeLag
        ? {
            type: templateMonitorValue.valuesByType.LAG.type,
            values: templateMonitorValue.valuesByType.LAG.values.map(
              v => v * getSecureRandomNumber()
            )
          }
        : undefined,
      MISSING: includeMissing
        ? {
            type: templateMonitorValue.valuesByType.MISSING.type,
            values: templateMonitorValue.valuesByType.MISSING.values.map(
              v => v * getSecureRandomNumber()
            )
          }
        : undefined
    }
  }));

  logger.info(
    `Soh Mock backend returning historical SOH number of channels: ${monitorValues.length}`
  );

  // provide calculations times that match the query start and end time
  const size = dataStore.historicalSohData.calculationTimes.length;
  const stepSize = Math.floor((input.endTime - input.startTime) / size);
  const calculationTimes = new Array(size)
    .fill(null)
    .map((v, index) => input.startTime + stepSize * index);

  return {
    stationName: input.stationName,
    calculationTimes,
    monitorValues
  };
}

/**
 * Gets historical Acquired Channel Environment Issues (Acei). Useful for unit tests
 *
 * @returns UiHistoricalAcei data
 */
export function getHistoricalAceiData(input: UiHistoricalAceiInput): UiHistoricalAcei[] {
  handleUninitializedDataStore();
  // Grab first in the json list and use it as a template
  const templateAcei: UiHistoricalAcei = dataStore.historicalAceiData[0];
  const station: ProcessingStation = ProcessingStationProcessor.Instance().getStationByName(
    input.stationName
  );

  // Walk thru the processing station's channels and create a new entry for each.
  const aceiResult: UiHistoricalAcei[] = station.channels.map(channel => {
    // TODO this actually comes in as a string not a number (what is passed to the service)
    const startTime: number = toEpochSeconds(input.startTime as any) * 1000;
    const endTime: number = toEpochSeconds(input.endTime as any) * 1000;

    const entry: UiHistoricalAcei = cloneDeep(templateAcei);
    entry.channelName = channel.name;
    entry.monitorType = input.type;
    // override the template data because it uses the wrong times and isn't very helpful
    // TODO show example of a gap in the data
    const size = Math.floor(getSecureRandomNumber() * 1000);
    const stepSize = (endTime - startTime) / size;
    const steps = new Array(size).fill(null).map((v, index) => startTime + stepSize * index);

    entry.issues = [[...steps.map((s, index) => [s, index % 2 === 0 ? 0 : 1])]];
    return entry;
  });

  logger.info(
    `Soh Mock backend returning historical ACEI number of channels: ${aceiResult.length}`
  );
  return aceiResult;
}
