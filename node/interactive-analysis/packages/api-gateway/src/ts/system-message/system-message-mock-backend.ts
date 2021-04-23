import { SystemMessageDefinition } from '@gms/common-graphql/lib/graphql/system-message/types';
import { readJsonData } from '@gms/common-util';
import config from 'config';
import path from 'path';
import { gatewayLogger as logger } from '../log/gateway-logger';
import { HttpMockWrapper } from '../util/http-wrapper';
import { resolveTestDataPaths } from '../util/test-data-util';

/**
 * Encapsulates backend data supporting retrieval by the API gateway.
 */
interface SystemMessageDataStore {
  systemMessageDefinitions: SystemMessageDefinition[];
}

// Declare a data store for system message mock backend
let dataStore: SystemMessageDataStore;

/**
 * Reads in test data and stores it
 */
export const initialize = (httpMockWrapper: HttpMockWrapper): void => {
  logger.info('Initializing mock backend for System Message Data.');

  if (!httpMockWrapper) {
    throw new Error('Cannot initialize System Messages services with undefined HTTP mock wrapper.');
  }

  dataStore = loadTestData();

  const backendConfig = config.get('systemMessage.backend');
  httpMockWrapper.onMock(
    backendConfig.services.getSystemMessageDefinitions.requestConfig.url,
    getSystemMessageDefinitionsData
  );
};

/**
 * Reads in test data and stores it.
 */
function loadTestData(): SystemMessageDataStore {
  // Get test data configuration settings
  const testDataConfig = config.get('testData.additionalTestData');
  const dataPath = resolveTestDataPaths().additionalDataHome;

  // Load station soh from file
  const systemMessageDefinitionsPath = path.join(
    dataPath,
    testDataConfig.systemMessageDefinitionsFileName
  );
  logger.info(`Loading system message test data from path: ${systemMessageDefinitionsPath}`);
  const systemMessageDefinitionsResponse: any[] = readJsonData(systemMessageDefinitionsPath);

  return {
    systemMessageDefinitions: systemMessageDefinitionsResponse
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
      throw new Error('Mock backend system message data store has not been initialized.');
    }
  }
}

/**
 * Returns test data system message definitions
 * @returns a SystemMessageDefinition[]
 */
export function getSystemMessageDefinitionsData(): SystemMessageDefinition[] {
  handleUninitializedDataStore();
  return dataStore.systemMessageDefinitions;
}
