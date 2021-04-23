import { readJsonData } from '@gms/common-util';
import config from 'config';
import path from 'path';
import { gatewayLogger as logger } from '../log/gateway-logger';
import { HttpMockWrapper } from '../util/http-wrapper';
import { resolveTestDataPaths } from '../util/test-data-util';
import * as model from './model';

/**
 * Encapsulates backend data supporting retrieval by the API gateway.
 */
interface DataAcquisitionDataStore {
  fileGaps: model.TransferredFile[];
}

// Declare a data store for the data acquisition status mask backend
let dataStore: DataAcquisitionDataStore;

/**
 * Configure mock HTTP interfaces for a simulated set of data acquisition status mock backend services.
 *
 * @param httpMockWrapper The HTTP mock wrapper used to configure mock backend service interfaces
 */
export function initialize(httpMockWrapper: HttpMockWrapper) {
  logger.info('Initializing mock backend for Data Acquisition Status data');

  if (!httpMockWrapper) {
    throw new Error(
      'Cannot initialize mock Data Acquisition Status services with undefined HTTP mock wrapper'
    );
  }

  // Load test data from the configured data set
  dataStore = loadTestData();

  // Load the data acquisition status  backend service config settings
  const backendConfig = config.get('dataAcquisition.backend');

  // Configure mock service interfaces
  httpMockWrapper.onMock(
    backendConfig.services.transferredFilesByTimeRange.requestConfig.url,
    getTransferFilesByTimeRange
  );
}

/**
 * Reads in test data and stores it
 */
function loadTestData(): DataAcquisitionDataStore {
  // Get test data configuration settings
  const testDataConfig = config.get('testData.additionalTestData');
  const dataPath = resolveTestDataPaths().additionalDataHome;

  logger.info(`Loading data acquisition status test data from path: ${dataPath}`);

  // Read the test data set from file; parse into object structure representing rows in the file
  const transferredFilesResponse: model.TransferredFile[] = readJsonData(
    dataPath.concat(path.sep).concat(testDataConfig.transferredFileName)
  );

  const store: DataAcquisitionDataStore = { fileGaps: transferredFilesResponse };
  return store;
}

/**
 * Gets Transferred File Gaps
 *
 * @returns TransferredFile[]
 */
function getTransferFilesByTimeRange(): model.TransferredFile[] {
  handleUninitializedDataStore();
  return dataStore.fileGaps;
}

/**
 * Handle cases where the data store has not been initialized.
 */
function handleUninitializedDataStore() {
  // If the data store is uninitialized, throw an error
  if (!dataStore) {
    throw new Error('Mock backend data acquisition data store has not been initialized');
  }
}
