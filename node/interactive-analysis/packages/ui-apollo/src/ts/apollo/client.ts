import { isWindowDefined } from '@gms/common-util';
import { ApolloLink } from 'apollo-link';
import { SubscriptionClient } from 'subscriptions-transport-ws';
import { UILogger } from '../ui-logger';
import { configureClient, graphqlProxyUri, subscriptionsProxyUri } from './client-configuration';
import { Link, SplitLink, WsClient, WsLink } from './links';
import { ApolloClientConfiguration } from './types';

// we can't initialize the websocket client if we aren't running in the browser or a renderer process.
// this shouldn't run in the main electron process.
const windowIsDefined = isWindowDefined();

// the connection
let connection = null;

// the apollo client
let client = null;

// create the connection
const createConnection = (
  url: string,
  ws: string
):
  | {
      /**
       * The subscription client
       */
      wsClient: SubscriptionClient;
      /**
       * The apollo link
       */
      link: ApolloLink;
    }
  | undefined => {
  const wsClient = windowIsDefined ? WsClient(ws) : undefined;
  const wsLink = windowIsDefined ? WsLink(wsClient) : undefined;
  const link = Link(url, false, false);

  if (link !== undefined) {
    return {
      wsClient,
      link: SplitLink(wsLink, link)
    };
  }
  return undefined;
};

/**
 * Create an apollo client with support for subscriptions
 */
export const createApolloClientConfiguration = (
  url: string = graphqlProxyUri ? `${graphqlProxyUri}/graphql` : undefined,
  ws: string = subscriptionsProxyUri ? `${subscriptionsProxyUri}/subscriptions` : undefined
): ApolloClientConfiguration => {
  try {
    if (!connection && !client) {
      if (!connection) {
        UILogger.Instance().info(`Establishing connection`);
        connection = createConnection(url, ws);
      }

      if (connection) {
        if (!client) {
          UILogger.Instance().info(`Configuring client`);
          client = configureClient(connection.link);
        }

        UILogger.Instance().info(`Connected and configured with graphql uri: ${graphqlProxyUri}`);
        UILogger.Instance().info(
          `Connected and configured with subscriptions uri: ${subscriptionsProxyUri}`
        );

        return {
          client,
          wsClient: connection.wsClient
        };
      }
      UILogger.Instance().error(`Failed to create connection:
        url: ${url} ws:${ws}`);

      return {
        client: undefined,
        wsClient: undefined
      };
    }
    return {
      client,
      wsClient: connection.wsClient
    };
  } catch (error) {
    UILogger.Instance().error(`Failed to create Apollo Client:
      url: ${url} ws:${ws} :
      ${error}`);
    return {
      client: undefined,
      wsClient: undefined
    };
  }
};
