import { IS_NODE_ENV_DEVELOPMENT, isWindowDefined } from '@gms/common-util';
import ApolloClient from 'apollo-client';
import { ApolloLink } from 'apollo-link';
import { BatchHttpLink } from 'apollo-link-batch-http';
import { inMemoryCacheConfiguration } from './cache-configuration';
import { graphqlProxyUri } from './client-configuration';
import { Client } from './types';

// we can't initialize the websocket client if we aren't running in the browser or a renderer process.
// this shouldn't run in the main electron process.
export const windowIsDefined = isWindowDefined();

// batch interval time in ms
const batchInterval = 5000;

// max number of requests to batch
const batchMax = 1000;

// configure the cache setup
const cache = inMemoryCacheConfiguration;

// configure the client
export const configureClient = (link: ApolloLink): Client =>
  new ApolloClient<any>({
    link,
    defaultOptions: {
      query: {
        fetchPolicy: 'no-cache',
        errorPolicy: 'none'
      },
      mutate: {
        fetchPolicy: 'no-cache',
        errorPolicy: 'none'
      },
      watchQuery: {
        fetchPolicy: 'no-cache',
        errorPolicy: 'none'
      }
    },
    queryDeduplication: true,
    assumeImmutableResults: true,
    cache,
    // enable apollo dev tools when running in development mode
    connectToDevTools: IS_NODE_ENV_DEVELOPMENT
  });

// Batch apollo link for the client logger
// NOTE: this does not use the normal custom
// links because those use the UILogger
const BatchLink = url => {
  try {
    return new BatchHttpLink({
      uri: url,
      batchInterval,
      batchMax
    });
  } catch (error) {
    // tslint:disable-next-line: no-console
    console.error(`Failed to create logger Batch HTTP Link: ${error}`);
    return undefined;
  }
};

/**
 * Create an apollo client for logging.
 */
export const createApolloClientLogger = (
  url: string = graphqlProxyUri ? `${graphqlProxyUri}/graphql` : undefined
): Client => {
  try {
    const link = windowIsDefined ? BatchLink(url) : undefined;
    if (link !== undefined) {
      return configureClient(link);
    }
    // tslint:disable-next-line: no-console
    console.error(`Failed to create logger apollo client`);
    return undefined;
  } catch (error) {
    // tslint:disable-next-line: no-console
    console.error(`Failed to create logger apollo client: ${error}`);
    return undefined;
  }
};
