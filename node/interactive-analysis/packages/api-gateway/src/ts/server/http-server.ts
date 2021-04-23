import config from 'config';
import { Express } from 'express';
import http from 'http';
import { gatewayLogger as logger } from '../log/gateway-logger';

/**
 * Creates the HTTP server
 * @param app the express server
 * @param ssl true if running in ssl; false otherwise
 */
export const createHttpServer = (app: Express): http.Server => {
  logger.info(`Creating the http server...`);

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

  const server: http.Server =
    // Listen for GraphQL requests over HTTP
    http.createServer(app);

  server.listen(httpPort, function() {
    logger.info(`listening on port ${httpPort}`);
  });

  return server;
};
