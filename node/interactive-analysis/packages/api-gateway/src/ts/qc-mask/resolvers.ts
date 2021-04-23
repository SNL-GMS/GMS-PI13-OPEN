import { toEpochSeconds } from '@gms/common-util';
import config from 'config';
import { PubSub } from 'graphql-subscriptions';
import { UserContext } from '../cache/model';
import { pubsub as dataPayloadPubSub } from '../common/resolvers';
import { gatewayLogger as logger } from '../log/gateway-logger';
import { createDataPayload } from '../util/common-utils';
import * as model from './model';
import { QcMaskProcessor } from './qc-mask-processor';

/**
 * Resolvers for the signal detection API gateway
 */

// Create the publish/subscribe API for GraphQL subscriptions
export const pubsub = new PubSub();

// Load configuration settings
const settings = config.get('qcMask');
const dataSubscription = config.get('common.subscriptions.channels.dataPayload');

// GraphQL Resolvers
export const resolvers = {
  // Query resolvers
  Query: {
    qcMasksByChannelName: async (_, { timeRange, channelNames }, userContext: UserContext) => {
      // tslint:disable-next-line: max-line-length
      logger.info(
        `Getting qc masks for ${timeRange.startTime} to ${timeRange.endTime}. User: ${userContext.userName}`
      );
      const masks = QcMaskProcessor.Instance().getQcMasks(userContext, timeRange, channelNames);
      return masks;
    }
  },

  // Mutation Resolvers
  Mutation: {
    createQcMask: async (_, { channelNames, input }, userContext: UserContext) => {
      logger.info(`Creating qc mask on channels: ${channelNames}. User: ${userContext.userName}`);
      // Create QC Masks
      const masksCreated: model.QcMask[] = await QcMaskProcessor.Instance().createQcMasks(
        userContext,
        channelNames,
        input
      );

      // Publish the newly created masks to the subscription channel
      const dataPayload = createDataPayload([], [], masksCreated);
      // tslint:disable-next-line: no-floating-promises
      dataPayloadPubSub.publish(dataSubscription, { dataPayload });
      return dataPayload;
    },
    updateQcMask: async (_, { qcMaskId, input }, userContext: UserContext) => {
      logger.info(`Updating mask ${qcMaskId}. User: ${userContext.userName}`);
      // Update the mask with the inputs provided
      const maskUpdated: model.QcMask = await QcMaskProcessor.Instance().updateQcMask(
        userContext,
        qcMaskId,
        input
      );

      // Publish the newly created masks to the subscription channel
      const dataPayload = createDataPayload([], [], [maskUpdated]);
      // tslint:disable-next-line: no-floating-promises
      dataPayloadPubSub.publish(dataSubscription, { dataPayload });
      return dataPayload;
    },
    rejectQcMask: async (_, { qcMaskId, rationale }, userContext: UserContext) => {
      logger.info(`Rejecting qc mask: ${qcMaskId}. User: ${userContext.userName}`);
      // Reject QC Mask
      const rejectedMask: model.QcMask = await QcMaskProcessor.Instance().rejectQcMask(
        userContext,
        qcMaskId,
        rationale
      );

      // Publish the newly created masks to the subscription channel
      const dataPayload = createDataPayload([], [], [rejectedMask]);
      // tslint:disable-next-line: no-floating-promises
      dataPayloadPubSub.publish(dataSubscription, { dataPayload });
      return dataPayload;
    }
  },

  // Subscription resolvers
  Subscription: {
    // Subscription for new QcMasks
    qcMasksCreated: {
      subscribe: async () => pubsub.asyncIterator(settings.subscriptions.channels.qcMasksCreated)
    }
  },

  // Field Resolvers
  QcMask: {
    currentVersion: async (qcMask: model.QcMask, _, userContext: UserContext) =>
      qcMask.qcMaskVersions[qcMask.qcMaskVersions.length - 1]
  },
  QcMaskVersion: {
    startTime: async (qcMaskVersion: model.QcMaskVersion, _, userContext: UserContext) =>
      toEpochSeconds(qcMaskVersion.startTime),
    endTime: async (qcMaskVersion: model.QcMaskVersion, _, userContext: UserContext) =>
      toEpochSeconds(qcMaskVersion.endTime)
  }
};
