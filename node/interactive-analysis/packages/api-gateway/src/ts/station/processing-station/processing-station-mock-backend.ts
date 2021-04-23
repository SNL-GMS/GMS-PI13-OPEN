import { readJsonData } from '@gms/common-util';
import config from 'config';
import path from 'path';
import { gatewayLogger as logger } from '../../log/gateway-logger';
import { HttpMockWrapper } from '../../util/http-wrapper';
import { resolveTestDataPaths } from '../../util/test-data-util';
import { ProcessingChannel, ProcessingStation, ProcessingStationGroup } from './model';

/**
 * Mock backend HTTP services providing access to processing station data. If mock services are enabled in the
 * configuration file, this module loads a test data set specified in the configuration file and configures
 * mock HTTP interfaces for the API gateway backend service calls.
 */

/**
 * Encapsulates backend data supporting retrieval by the API gateway.
 */
interface ProcessingStationDataStore {
  stationGroups: ProcessingStationGroup[];
  stationMap: Map<string, ProcessingStation>;
  channelMap: Map<string, ProcessingChannel>;
}

// Declare a backend data store for the mock station backend
let dataStore: ProcessingStationDataStore;

/**
 * Configure mock HTTP interfaces for a simulated set of station-related backend services.
 * @param httpMockWrapper The HTTP mock wrapper used to configure mock backend service interfaces
 */
export function initialize(httpMockWrapper: HttpMockWrapper) {
  logger.info('Initializing mock backend for processing station data');

  if (!httpMockWrapper) {
    throw new Error('Cannot initialize mock station services with undefined HTTP mock wrapper');
  }

  // Load test data from the configured data set
  dataStore = loadTestData();

  // Load the station backend service config settings
  const backendConfig = config.get('processingStation.backend');

  httpMockWrapper.onMock(
    backendConfig.services.stationGroupByName.requestConfig.url,
    getStationGroupByName
  );
  httpMockWrapper.onMock(
    backendConfig.services.stationsByNames.requestConfig.url,
    getStationsByNames
  );
  httpMockWrapper.onMock(
    backendConfig.services.channelsByNames.requestConfig.url,
    getChannelsByNames
  );
}

/**
 * Retrieve ProcessingStations from the list of station names
 * @param stationNames list of station names
 * @returns a processing station[]
 */
export function getStationsByNames(stationNames: string[]): ProcessingStation[] {
  const stations: ProcessingStation[] = [];

  // Walk all station groups all stations adding each channel match
  stationNames.forEach(stationName => {
    if (dataStore.stationMap.has(stationName)) {
      stations.push(dataStore.stationMap.get(stationName));
    } else {
      logger.warn(`Failed to find station name ${stationName} in the mock backend datastore!`);
    }
  });
  return stations;
}

/**
 * Retrieve ProcessingChannels from the list of channel names
 * @param channelNames list of channel names
 * @returns a processing channels[]
 */
export function getChannelsByNames(channelNames: string[]): ProcessingChannel[] {
  const channels: ProcessingChannel[] = [];

  // walk all station groups all stations adding each channel match
  channelNames.forEach(cname => {
    if (dataStore.channelMap.has(cname)) {
      channels.push(dataStore.channelMap.get(cname));
    } else {
      logger.warn(`Failed to find channel name ${cname} in the mock backend datastore!`);
    }
  });
  return channels;
}

/**
 * Retrieve a station group of processing stations
 * @param stationGroupNames StationGroup name
 * @returns ProcessingStationGroup
 */
export function getStationGroupByName(stationGroupNames: string[]): ProcessingStationGroup[] {
  // Handle undefined input
  if (!stationGroupNames || stationGroupNames.length === 0) {
    throw new Error('Unable to retrieve station group for undefined station group list');
  }

  // Handle uninitialized data store
  handleUninitializedDataStore();

  return stationGroupNames
    .map(sgName => dataStore.stationGroups.find(sg => sg.name === sgName))
    .filter(sg => sg !== undefined);
}

/**
 * Load test data into the mock backend data store from the configured test data set.
 * @returns StationDataStore
 */
function loadTestData(): ProcessingStationDataStore {
  const dataPath = resolveTestDataPaths().additionalDataHome;

  const stationProcessingConfig = config.get('testData.additionalTestData');
  const stationGroupFile = dataPath
    .concat(path.sep)
    .concat(stationProcessingConfig.stationGroupsFileName);
  logger.info(`Loading processing station test data from path: ${stationGroupFile}`);

  // Read the processing network definitions from the configured test set
  let stationGroups: ProcessingStationGroup[] = [];
  try {
    stationGroups = readJsonData(stationGroupFile);
    logger.info(`Mock backend processing station loaded ${stationGroups.length} station groups.`);
  } catch (e) {
    logger.error(
      `Failed to read station groups data from files: ` +
        `${stationProcessingConfig.stationGroupsFileName}`
    );
  }

  // Populate the data store maps
  const stationMap: Map<string, ProcessingStation> = new Map();
  const channelMap: Map<string, ProcessingChannel> = new Map();
  try {
    stationGroups.forEach(sg => {
      sg.stations.forEach(station => {
        // Add station if not already added (It can be added from a different station group)
        if (!stationMap.has(station.name)) {
          stationMap.set(station.name, station);
        }

        station.channels.forEach(channel => {
          // Add channel if not already added (It can be added from a different station group)
          if (!channelMap.has(channel.name)) {
            channelMap.set(channel.name, channel);
          }
        });
      });
    });
  } catch (e) {
    logger.error(`Failed to populate Station and Channel maps using loaded station groups ${e}`);
  }
  logger.debug([...stationMap.keys()]);
  return { stationGroups, stationMap, channelMap };
}

/**
 * Handle cases where the data store has not been initialized.
 */
function handleUninitializedDataStore() {
  // If the data store is uninitialized, throw an error
  if (!dataStore) {
    throw new Error('Mock backend station processing data store has not been initialized');
  }
}
