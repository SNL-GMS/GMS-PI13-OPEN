// GraphQL Resolvers
import { MILLISECONDS_IN_SECOND, toEpochSeconds } from '@gms/common-util';
import config from 'config';
import { UserContext } from '../cache/model';
import { gatewayLogger as logger } from '../log/gateway-logger';
import { SystemMessageProcessor } from './system-message-processor';

// Load configuration settings
const settings = config.get('systemMessage');

export const resolvers = {
  // Query Resolvers
  Query: {
    // Returns System Message Definitions
    systemMessageDefinitions: async (_, __, userContext: UserContext) => {
      logger.info(`Getting System Message Definitions. User: ${userContext.userName}`);
      return SystemMessageProcessor.Instance().getSystemMessageDefinitions();
    }
  },
  // Mutation Resolvers
  Mutation: {},
  // Subscription Resolvers
  Subscription: {
    // the system message subscription resolver
    systemMessages: {
      subscribe: () =>
        SystemMessageProcessor.Instance().pubsub.asyncIterator(
          settings.subscriptions.systemMessages
        )
    }
  },
  SystemMessage: {
    time: systemMessage => toEpochSeconds(systemMessage.time) * MILLISECONDS_IN_SECOND
  }
};
