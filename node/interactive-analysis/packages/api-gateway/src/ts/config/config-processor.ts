import config from 'config';
import get from 'lodash/get';
import { CacheProcessor } from '../cache/cache-processor';
import { gatewayLogger as logger } from '../log/gateway-logger';
import { HttpClientWrapper } from '../util/http-wrapper';
import * as configMockBackend from './config-mock-backend';

/**
 * API gateway processor for configuration-related data APIs. This class supports:
 * - data fetching & caching from the backend service interfaces
 * - mocking of backend service interfaces based on test configuration
 * - session management
 * - GraphQL query resolution from the user interface client
 */
export class ConfigProcessor {
  /** The singleton instance */
  private static instance: ConfigProcessor;

  /** Local configuration settings */
  private readonly settings: any;

  /** HTTP client wrapper for communicating with backend services */
  private readonly httpWrapper: HttpClientWrapper;

  /** Boolean that checks if processor has been initialized */
  private readonly isInitialized: boolean = false;

  /**
   * Returns the singleton instance of the cache processor.
   * @returns the instance of the cache processor
   */
  public static Instance(): ConfigProcessor {
    if (ConfigProcessor.instance === undefined) {
      ConfigProcessor.instance = new ConfigProcessor();
      ConfigProcessor.instance.initialize();
    }
    return ConfigProcessor.instance;
  }

  /**
   * Constructor - initialize the ConfigProcessor
   */
  private constructor() {
    // Load configuration settings
    this.settings = config.get('config');

    // Initialize an http client wrapper
    this.httpWrapper = new HttpClientWrapper();
  }

  /**
   * Retrieve a configuration by key
   * @param key The key to retrieve the configuration value for
   */
  public getConfigByKey(key: string): any {
    logger.debug(`Local fetching configuration for key: ${key}`);
    return get(CacheProcessor.Instance().getConfiguration(), key);
  }

  /**
   * Fetch configuration data from the backend for the provided key string.
   * If the provided key is undefined, this function throws an error.
   * This function propagates errors from the underlying HTTP call.
   * @param key The key to retrieve the configuration value for
   */
  public fetchConfigByKey(key: string): any {
    logger.debug(`Fetching configuration for key: ${key}`);

    // Handle invalid input
    if (!key) {
      throw new Error('Cannot fetch configuration for an undefined key');
    }

    // Retrieve the settings for the service
    // const serviceConfig = this.settings.backend.services.configByKey.requestConfig;
    const input: configMockBackend.ConfigKeyInput = { key };
    // Call the service and extract the response data
    return configMockBackend.getConfigByKey(input);
  }

  /**
   * Initialize the configuration processor, fetching configuration data from the backend
   * This function sets up a mock backend if configured to do so.
   */
  private initialize() {
    if (this.isInitialized) {
      return;
    }
    logger.info('Initializing the configuration processor');

    // If service mocking is enabled, initialize the mock backend
    if (this.settings.backend.mock.enable) {
      configMockBackend.initialize(this.httpWrapper.createHttpMockWrapper());
    }

    // TODO: Figure out how to merge the config data into the map initialized in the Global cache
    // Cache configuration data needed to support the interactive analysis UI
    CacheProcessor.Instance().setConfiguration(this.fetchConfigByKey(this.settings.rootKey));
  }
}
