import { ApolloServer } from 'apollo-server-express';
import config from 'config';
import { Express } from 'express';
import { GraphQLError, GraphQLSchema } from 'graphql';
import msgpack from 'msgpack-lite';
import { CacheProcessor } from '../cache/cache-processor';
import { UserContext } from '../cache/model';
import { gatewayLogger as logger } from '../log/gateway-logger';
import { performanceLogger } from '../log/performance-logger';
import { ExpressUserMap, UNDEFINED_USER } from './express-user';

/**
 * Creates the Apollo Express Server
 * @param app the express server
 * @param schema the schema
 * @param userMap the user map
 */
export const createApolloExpressServer = (
  app: Express,
  schema: GraphQLSchema,
  userMap: ExpressUserMap
): ApolloServer => {
  logger.info(`Creating apollo server...`);

  // Load configuration settings
  const gqlConfig = config.get('server.graphql');

  // GraphQL Path
  const graphqlPath = gqlConfig.http.graphqlPath;
  logger.info(`graphqlPath ${graphqlPath}`);

  // GraphQL Websocket port
  const wsPort = gqlConfig.ws.port;
  logger.info(`wsPort ${wsPort}`);

  const apolloServer = new ApolloServer({
    schema,
    tracing: process.env.NODE_ENV !== 'production',
    cacheControl: true,
    // TODO: disable in production
    introspection: true, // process.env.NODE_ENV !== 'production',
    debug: process.env.NODE_ENV !== 'production',
    persistedQueries: false,
    context: ({ req }: { req: any }): UserContext => ({
      sessionId: req.sessionID,
      // TODO return string 'undefined' to avoid analyst.userName error in workflow
      // TODO - this case is hit when using firefox, more debugging is needed to get this fixed properly
      userName:
        userMap.has(req.sessionID) && userMap.get(req.sessionID).userName
          ? userMap.get(req.sessionID).userName
          : UNDEFINED_USER,
      userCache: userMap.has(req.sessionID)
        ? CacheProcessor.Instance().getCacheForUser(req.sessionID)
        : undefined,
      userRole: 'DEFAULT'
    }),
    playground: {
      endpoint: graphqlPath,
      subscriptionEndpoint: `ws://${gqlConfig.ws.host}:${wsPort}${gqlConfig.ws.path}`,
      settings: {
        'request.credentials': 'include'
      }
    },
    formatResponse: (response, options) => {
      // !IMPORTANT TODO - sometimes options.request is undefined
      // !there is no adequately explained reason that options.request isn't defined - look into this issue
      if (options && options.request && options.request.http && options.request.http.headers) {
        const headers = options.request.http.headers.get('Accept');
        if (headers !== null && headers.includes('application/msgpack')) {
          response.data = msgpack.encode(response.data);
          return response;
        }
      }
      // Example of data to extract data request out of
      // "data": {
      //     "clientLog": {
      //       "logLevel": "DATA",
      //       "message": "query defaultStations (in 3340 ms)",
      //       "time": "2019-03-28T14:36:55.285Z",
      //       "__typename": "ClientLog"
      //     }
      //   },
      // tslint:disable-next-line: no-for-in
      for (const property in response) {
        if (response.hasOwnProperty(property) && property === 'data') {
          const dataRequestName = Object.keys(response[property])[0];
          if (!dataRequestName.includes('clientLog')) {
            performanceLogger.performance(dataRequestName, 'returningFromServer');
          }
        }
      }
      return response;
    },
    formatError: (error: GraphQLError) => {
      logger.error(error.message);
      return error;
    }
  });

  apolloServer.applyMiddleware({ app });

  return apolloServer;
};
