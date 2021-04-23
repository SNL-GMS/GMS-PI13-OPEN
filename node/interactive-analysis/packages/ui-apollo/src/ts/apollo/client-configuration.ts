import {
  GRAPHQL_PROXY_URI,
  IS_NODE_ENV_DEVELOPMENT,
  SUBSCRIPTIONS_PROXY_URI
} from '@gms/common-util';
import { ApolloClient } from 'apollo-client';
import { ApolloLink } from 'apollo-link';
import { inMemoryCacheConfiguration } from './cache-configuration';
import { Client } from './types';

// The graphql URL
export const graphqlProxyUri = GRAPHQL_PROXY_URI;

// The subscriptions URL
export const subscriptionsProxyUri = SUBSCRIPTIONS_PROXY_URI;

// configure the cache setup
const cache = inMemoryCacheConfiguration;

// configure the client
export const configureClient = (link: ApolloLink): Client =>
  new ApolloClient<any>({
    link,
    defaultOptions: {
      query: {
        fetchPolicy: 'cache-first',
        errorPolicy: 'all'
      },
      mutate: {
        fetchPolicy: 'cache-first',
        errorPolicy: 'all'
      },
      watchQuery: {
        fetchPolicy: 'cache-first',
        errorPolicy: 'all'
      }
    },
    queryDeduplication: true,
    assumeImmutableResults: true,
    cache,
    // enable apollo dev tools when running in development mode
    connectToDevTools: IS_NODE_ENV_DEVELOPMENT
  });
