import { WebSocketLink } from 'apollo-link-ws';
import { SubscriptionClient } from 'subscriptions-transport-ws';
import { UILogger } from '../../ui-logger';

export const WsClient = (ws: string): SubscriptionClient | undefined => {
  try {
    return new SubscriptionClient(ws, {
      reconnect: true,
      timeout: 1000000
    });
  } catch (error) {
    UILogger.Instance().error(`Failed to create WS Client: ${error}`);
    return undefined;
  }
};

export const WsLink = (wsClient: SubscriptionClient): WebSocketLink | undefined => {
  try {
    return new WebSocketLink(wsClient);
  } catch (error) {
    UILogger.Instance().error(`Failed to create WS Link: ${error}`);
    return undefined;
  }
};
