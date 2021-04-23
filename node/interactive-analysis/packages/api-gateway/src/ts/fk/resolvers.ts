import { PubSub } from 'graphql-subscriptions';
import flatMap from 'lodash/flatMap';
import { DataPayload, UserActionDescription, UserContext } from '../cache/model';
import { gatewayLogger as logger } from '../log/gateway-logger';
import { performanceLogger } from '../log/performance-logger';
import { createDataPayload } from '../util/common-utils';
import { FkProcessor } from './fk-processor';
import { FkFrequencyThumbnailBySDId } from './model';

/**
 * Resolvers for the signal detection API gateway
 */

// Create the publish/subscribe API for GraphQL subscriptions
export const pubsub = new PubSub();

// GraphQL Resolvers
export const resolvers = {
  // Query Resolvers
  Query: {
    // Compute the Fk Thumbnails for UI Display. Should be called from UI after a new compute Fk is called
    computeFkFrequencyThumbnails: async (
      _,
      { fkInput },
      userContext: UserContext
    ): Promise<FkFrequencyThumbnailBySDId> => {
      // tslint:disable-next-line: max-line-length
      logger.info(
        `Computing FK Frequency Thumbnails for ${fkInput.signalDetectionId}. User: ${userContext.userName}`
      );
      performanceLogger.performance(
        'computeFkFrequencyThumbnails',
        'enteringResolver',
        fkInput.signalDetectionId
      );
      // Compute the Thumbnail Fks
      const thumbnailsBySdID = await FkProcessor.Instance().computeFkFrequencyThumbnails(
        userContext,
        fkInput
      );
      performanceLogger.performance(
        'computeFkFrequencyThumbnails',
        'leavingResolver',
        fkInput.signalDetectionId
      );
      return thumbnailsBySdID;
    }
  },

  // Mutation resolvers
  Mutation: {
    // Compute new fks
    computeFks: async (_, { fkInput }, userContext: UserContext) => {
      performanceLogger.performance('computeFks', 'enteringResolver', fkInput.signalDetectionId);
      // Compute a new Fk and return the modified SignalDetections
      const promises = fkInput.map(async input => {
        logger.info(
          `Computing fk for signal detection: ${input.signalDetectionId}. User: ${userContext.userName}`
        );
        return FkProcessor.Instance().computeFk(userContext, input);
      });
      // Await promises and then combine all changed events and sds into one association change
      const dataPayload = await Promise.all<DataPayload>(promises);
      const events = flatMap(dataPayload.map(dp => dp.events));
      const sds = flatMap(dataPayload.map(dp => dp.sds));

      const userAction =
        sds.length > 1
          ? UserActionDescription.COMPUTE_MULTIPLE_FK
          : UserActionDescription.COMPUTE_FK;
      // Set data to user cache
      userContext.userCache.setEventsAndSignalDetections(userAction, events, sds);

      performanceLogger.performance('computeFks', 'leavingResolver', fkInput.signalDetectionId);
      return createDataPayload(events, sds, []);
    },
    markFksReviewed: async (_, { markFksReviewedInput }, userContext: UserContext) =>
      // Call channel segment processor to update review flag in the appropriate
      // Azimuth SD Feature Measurement and return the list of ones successfully updated
      ({
        events: [],
        sds: FkProcessor.Instance().updateFkReviewedStatuses(
          userContext,
          markFksReviewedInput,
          markFksReviewedInput.reviewed
        )
      })
  }
};
