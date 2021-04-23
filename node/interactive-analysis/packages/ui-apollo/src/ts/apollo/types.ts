import { Hermes } from 'apollo-cache-hermes';
import { InMemoryCache, NormalizedCacheObject } from 'apollo-cache-inmemory';
import { ApolloClient } from 'apollo-client';
import { SubscriptionClient } from 'subscriptions-transport-ws';

/** Apollo Client -> wrapper around ApolloClient */
export type Client = ApolloClient<any | InMemoryCache | Hermes | NormalizedCacheObject>;

/** WS Client -> wrapper around SubscriptionClient */
export type WsClient = SubscriptionClient;

/**
 * Apollo client configuration.
 */
export interface ApolloClientConfiguration {
  client: Client;
  wsClient: WsClient;
}
