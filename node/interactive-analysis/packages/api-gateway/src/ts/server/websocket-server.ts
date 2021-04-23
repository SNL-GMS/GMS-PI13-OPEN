import config from 'config';
import cookie from 'cookie';
import { execute, GraphQLSchema, subscribe } from 'graphql';
import http from 'http';
import { ConnectionContext, SubscriptionServer } from 'subscriptions-transport-ws';
import { CacheProcessor } from '../cache/cache-processor';
import { UserContext } from '../cache/model';
import { gatewayLogger as logger } from '../log/gateway-logger';
import { ExpressUserMap, UNDEFINED_USER } from './express-user';

/**
 * Creates the HTTP server
 * @param schema the schema
 * @param userMap the user map
 * @param ssl true if running in ssl; false otherwise
 */
export const createWebSocketServer = (
  schema: GraphQLSchema,
  userMap: ExpressUserMap
): http.Server => {
  logger.info(`Creating the web socket server...`);

  // Load configuration settings
  const gqlConfig = config.get('server.graphql');

  // GraphQL Path
  const graphqlPath = gqlConfig.http.graphqlPath;
  logger.info(`graphqlPath ${graphqlPath}`);

  // GraphQL HTTP server port
  const httpPort = gqlConfig.http.port;
  logger.info(`httpPort ${httpPort}`);

  // GraphQL Websocket port
  const wsPort = gqlConfig.ws.port;
  logger.info(`wsPort ${wsPort}`);

  const handleWebSocket = (request, response) => {
    const responseCode = 404;
    response.writeHead(responseCode);
    response.end();
  };

  const websocketServer: http.Server =
    // Create the Websocket server supporting GraphQL subscriptions over WS
    http.createServer(handleWebSocket);

  // Listen for GraphQL subscription connections
  websocketServer.listen(wsPort, () => {
    // Create the subscription server
    new SubscriptionServer(
      {
        schema,
        execute,
        subscribe,
        onConnect: (
          connectionParams: Object,
          webSocket: WebSocket,
          context: ConnectionContext
        ): UserContext => {
          // TODO: We should put 'int analysis' string into a config
          // tslint:disable-next-line: no-string-literal
          const sessionIdRaw: string = cookie.parse(context.request.headers.cookie)
            .InteractiveAnalysis;
          const sessionId = sessionIdRaw.split(':')[1].split('.')[0];
          // TODO return string 'undefined' to avoid analyst.userName error in workflow
          // TODO - this case is hit when using firefox, more debugging is needed to get this fixed properly
          return {
            sessionId,
            // tslint:disable-next-line: max-line-length
            userName:
              userMap.has(sessionId) && userMap.get(sessionId).userName
                ? userMap.get(sessionId).userName
                : UNDEFINED_USER,
            userCache: userMap.has(sessionId)
              ? CacheProcessor.Instance().getCacheForUser(sessionId)
              : undefined,
            userRole: 'DEFAULT'
          };
        }
      },
      {
        server: websocketServer,
        path: gqlConfig.ws.path
      }
    );
    logger.info(`Websocket Server is listening on port ${wsPort}`);
  });

  return websocketServer;
};
