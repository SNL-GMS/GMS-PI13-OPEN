import {
  CESIUM_OFFLINE,
  GRAPHQL_PROXY_URI,
  INTERACTIVE_ANALYSIS_MODE,
  IS_INTERACTIVE_ANALYSIS_MODE_SOH,
  IS_NODE_ENV_DEVELOPMENT,
  IS_NODE_ENV_PRODUCTION,
  NODE_ENV,
  SUBSCRIPTIONS_PROXY_URI,
  WAVEFORMS_PROXY_URI
} from '@gms/common-util';
import { UILogger } from '@gms/ui-apollo';

/**
 * Checks the ENV Configuration.
 * Logs the current environment configuration.
 */
export const checkEnvConfiguration = () => {
  UILogger.Instance().debug(
    `Environment (process.env): ` +
      `\n   process.env.NODE_ENV=${process.env.NODE_ENV}` +
      `\n   process.env.INTERACTIVE_ANALYSIS_MODE=${process.env.INTERACTIVE_ANALYSIS_MODE}` +
      `\n   process.env.GRAPHQL_PROXY_URI=${process.env.GRAPHQL_PROXY_URI}` +
      `\n   process.env.WAVEFORMS_PROXY_URI=${process.env.WAVEFORMS_PROXY_URI}` +
      `\n   process.env.SUBSCRIPTIONS_PROXY_URI=${process.env.SUBSCRIPTIONS_PROXY_URI}` +
      `\n   process.env.CESIUM_OFFLINE=${process.env.CESIUM_OFFLINE}`
  );

  UILogger.Instance().debug(
    `App Environment: ` +
      `\n   NODE_ENV=${NODE_ENV}` +
      `\n   INTERACTIVE_ANALYSIS_MODE=${INTERACTIVE_ANALYSIS_MODE}` +
      `\n   IS_INTERACTIVE_ANALYSIS_MODE_SOH=${IS_INTERACTIVE_ANALYSIS_MODE_SOH}` +
      `\n   IS_NODE_ENV_DEVELOPMENT=${IS_NODE_ENV_DEVELOPMENT}` +
      `\n   IS_NODE_ENV_PRODUCTION=${IS_NODE_ENV_PRODUCTION}` +
      `\n   GRAPHQL_PROXY_URI=${GRAPHQL_PROXY_URI}` +
      `\n   WAVEFORMS_PROXY_URI=${WAVEFORMS_PROXY_URI}` +
      `\n   SUBSCRIPTIONS_PROXY_URI=${SUBSCRIPTIONS_PROXY_URI}` +
      `\n   CESIUM_OFFLINE=${CESIUM_OFFLINE}`
  );
};
