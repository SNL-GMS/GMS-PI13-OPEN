import config from 'config';
import { PubSub } from 'graphql-subscriptions';
import { WorkspaceState } from '../common/model';
import { gatewayLogger as logger } from '../log/gateway-logger';
import { createDataPayload, createEmptyDataPayload } from '../util/common-utils';
import { CacheProcessor } from './cache-processor';
import {
  DataPayload,
  History,
  HypothesisChangeInformation,
  InvalidData,
  UserContext
} from './model';

/**
 * Resolvers for the cache
 */

// GraphQL Resolvers
logger.info('Creating cache API Gateway GraphQL resolvers...');

const commonConfig = config.get('common.subscriptions');
export const pubsub = new PubSub();

export const resolvers = {
  // Query resolvers
  Query: {
    // returns the global history for a given user
    history: async (_, __, userContext: UserContext): Promise<History[]> => {
      logger.info(`Global history requested. User: ${userContext.userName}`);
      return userContext.userCache.getHistory();
    },
    // returns the event history for a given user and a provided event id
    eventHistory: async (_, { id }, userContext: UserContext): Promise<History[]> => {
      logger.info(`Event history requested for event id: ${id}. User: ${userContext.userName}`);
      return userContext.userCache.getEventHistory(id);
    }
  },
  // Mutation Resolvers
  Mutation: {
    undoHistory: async (_, { numberOfItems }, userContext: UserContext): Promise<DataPayload> => {
      logger.info(
        `Undo history for user: ${userContext.userName}; attempting to undo ${numberOfItems} actions`
      );
      const data = userContext.userCache.undoHistory(numberOfItems);
      return createDataPayload(data.events, data.signalDetections, []);
    },
    redoHistory: async (_, { numberOfItems }, userContext: UserContext): Promise<DataPayload> => {
      logger.info(
        `Redo history for user: ${userContext.userName}; attempting to redo ${numberOfItems} actions`
      );
      const data = userContext.userCache.redoHistory(numberOfItems);
      return createDataPayload(data.events, data.signalDetections, []);
    },
    undoHistoryById: async (_, { id }, userContext: UserContext): Promise<DataPayload> => {
      logger.info(`Undo history for user: ${userContext.userName}; attempting to undo by id ${id}`);
      const data = userContext.userCache.undoHistoryById(id);
      return createDataPayload(data.events, data.signalDetections, []);
    },
    redoHistoryById: async (_, { id }, userContext: UserContext): Promise<DataPayload> => {
      logger.info(`Redo history for user: ${userContext.userName}; attempting to redo by id ${id}`);
      const data = userContext.userCache.redoHistoryById(id);
      return createDataPayload(data.events, data.signalDetections, []);
    },
    undoEventHistory: async (
      _,
      { numberOfItems },
      userContext: UserContext
    ): Promise<DataPayload> => {
      if (userContext.userCache.getOpenEventId()) {
        logger.info(
          `Undo event history for user: ${userContext.userName} and ` +
            `event: ${userContext.userCache.getOpenEventId()}; attempting to undo ${numberOfItems} actions`
        );
        const data = userContext.userCache.undoEventHistory(numberOfItems);
        return createDataPayload(data.events, data.signalDetections, []);
      }
      logger.info(
        `Undo event history for ${userContext.userName}; no event opened nothing to undo`
      );
      return createEmptyDataPayload();
    },
    redoEventHistory: async (
      _,
      { numberOfItems },
      userContext: UserContext
    ): Promise<DataPayload> => {
      if (userContext.userCache.getOpenEventId()) {
        logger.info(
          `Redo event history for user: ${userContext.userName} and ` +
            `event: ${userContext.userCache.getOpenEventId()}; attempting to redo ${numberOfItems} actions`
        );
        const data = userContext.userCache.redoEventHistory(numberOfItems);
        return createDataPayload(data.events, data.signalDetections, []);
      }
      logger.info(
        `Undo event history for ${userContext.userName}; no event opened nothing to undo`
      );
      return createEmptyDataPayload();
    },
    undoEventHistoryById: async (_, { id }, userContext: UserContext): Promise<DataPayload> => {
      if (userContext.userCache.getOpenEventId()) {
        logger.info(
          `Undo event history for user: ${userContext.userName} and ` +
            `event: ${userContext.userCache.getOpenEventId()}; attempting to undo by id ${id}`
        );
        const data = userContext.userCache.undoEventHistoryById(id);
        return createDataPayload(data.events, data.signalDetections, []);
      }
      return createEmptyDataPayload();
    },
    redoEventHistoryById: async (_, { id }, userContext: UserContext): Promise<DataPayload> => {
      if (userContext.userCache.getOpenEventId()) {
        logger.info(
          `Redo event history for user: ${userContext.userName} and ` +
            `event: ${userContext.userCache.getOpenEventId()}; attempting to redo by id ${id}`
        );
        const data = userContext.userCache.redoEventHistoryById(id);
        return createDataPayload(data.events, data.signalDetections, []);
      }
      return createEmptyDataPayload();
    }
  },

  // Field resolvers for DataPayload
  History: {
    redoPriorityOrder: (history: History, _, userContext: UserContext): number | undefined =>
      userContext.userCache.getRedoPriorityOrder(history.id)
  },
  HypothesisChangeInformation: {
    userAction: (
      hypothesisChangeInformation: HypothesisChangeInformation,
      _,
      userContext: UserContext
    ): string => hypothesisChangeInformation.userAction.toString()
  },
  DataPayload: {
    invalid: (dataPayload: DataPayload, _, userContext: UserContext): InvalidData => ({
      eventIds: userContext.userCache.getInvalidEventIds(),
      signalDetectionIds: userContext.userCache.getInvalidSignalDetectionIds()
    }),
    workspaceState: (dataPayload: DataPayload, _, userContext: UserContext): WorkspaceState =>
      CacheProcessor.Instance().getWorkspaceState(),
    history: (dataPayload: DataPayload, _, userContext: UserContext): History[] =>
      userContext.userCache.getHistory()
  },

  // Subscription Resolvers
  Subscription: {
    // Subscription calls all clients when client saves SDs and Events to the OSD
    dataPayload: {
      subscribe: async () => pubsub.asyncIterator(commonConfig.channels.dataPayload)
    }
  }
};
