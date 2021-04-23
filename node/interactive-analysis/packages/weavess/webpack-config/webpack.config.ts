import { appConfig } from './webpack.config.app';
import { nodeConfig } from './webpack.config.node';
import { libCjsConfig, libUmdConfig } from './webpack.config.lib';

/**
 * The webpack configuration for libraries (commonJS)
 *
 * @param webpackConfig the webpack configuration
 */
export const webpackLibUmdConfig = libUmdConfig;

/**
 * The webpack configuration for libraries (umd)
 *
 * @param webpackConfig the webpack configuration
 */
export const webpackLibCjsConfig = libCjsConfig;

/**
 * The webpack configuration for applications
 *
 * @param webpackConfig the webpack configuration
 */
export const webpackAppConfig = appConfig;

/**
 * The webpack configuration for node applications
 *
 * @param webpackConfig the webpack configuration
 */
export const webpackNodeConfig = nodeConfig;
