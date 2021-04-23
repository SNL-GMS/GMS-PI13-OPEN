import { LogLevel } from '@gms/common-graphql/lib/graphql/common/types';
import { PubSub } from 'graphql-subscriptions';
import { CacheProcessor } from '../cache/cache-processor';
import { UserContext } from '../cache/model';
import { gatewayLogger as logger } from '../log/gateway-logger';
import * as model from './model';
/**
 * Resolvers for the common API gateway
 */
const HASH_LENGTH = 8;

// GraphQL Resolvers
logger.info('Creating common API Gateway GraphQL resolvers...');
export const pubsub = new PubSub();

export const resolvers = {
  Query: {
    versionInfo: async (_, __, userContext: UserContext) => ({
      versionNumber: process.env.CI_COMMIT_REF_NAME
        ? process.env.CI_COMMIT_REF_NAME
        : process.env.GIT_BRANCH
        ? process.env.GIT_BRANCH
        : 'development',
      commitSHA: String(process.env.GIT_COMMITHASH).substr(0, HASH_LENGTH)
    })
  },

  Mutation: {
    clientLog: async (_, { logs }: { logs: model.ClientLog[] }, userContext: UserContext) => {
      new Promise((resolve, reject) => {
        logs.forEach((log: model.ClientLog) => {
          if (log.logLevel === LogLevel.timing) {
            logger.timing(log.message, userContext.userName);
          } else {
            logger.client(log.message, log.logLevel, userContext.userName);
          }
        });
        resolve();
      });
      return undefined;
    }
  }
};

export const extendedResolvers = {
  Query: {
    workspaceState: async (_, { timeRange }, userContext: UserContext) =>
      CacheProcessor.Instance().getWorkspaceState()
  }
};
