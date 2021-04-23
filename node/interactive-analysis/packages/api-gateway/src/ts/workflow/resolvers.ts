import config from 'config';
import { PubSub } from 'graphql-subscriptions';
import { CacheProcessor } from '../cache/cache-processor';
import { UserContext } from '../cache/model';
import { pubsub as dataPayloadPubSub } from '../common/resolvers';
import { EventProcessor } from '../event/event-processor';
import { gatewayLogger as logger } from '../log/gateway-logger';
import { performanceLogger } from '../log/performance-logger';
import { createEmptyDataPayload } from '../util/common-utils';
import { getStage } from '../util/workflow-util';
import * as model from './model';
import { WorkflowProcessor } from './workflow-processor';

// Create the publish/subscribe API for GraphQL subscriptions
export const pubsub = new PubSub();

/**
 * Resolvers for the Workflow User Interface API
 */

// Load subscription configuration settings
const subConfig = config.get('workflow.subscriptions');
const commonConfig = config.get('common.subscriptions');

/**
 * Publish newly-created ProcessingStageIntervals to the GraphQL subscription channel
 * and store them in the canned data list.
 * @param stageInterval the stage interval
 */
export function stageIntervalCreated(stageInterval: model.ProcessingStageInterval) {
  logger.info(
    `Publishing newly-created ProcessingStageInterval with ID: ${stageInterval.id} to GraphQL subscribers`
  );

  // Publish the new stage interval to the subscription channel
  pubsub
    .publish(subConfig.channels.stageIntervalCreated, { stageIntervalCreated: stageInterval })
    .catch(e => logger.warn(e));
}

// GraphQL Resolvers
logger.info('Creating GraphQL resolvers for the workflow API...');
export const resolvers = {
  // Query resolvers
  Query: {
    stages: async (_, __, userContext: UserContext) => {
      logger.info(`Getting stages. User: ${userContext.userName}`);
      return WorkflowProcessor.Instance().getStages(userContext);
    },
    stage: async (_, { id }, userContext: UserContext) =>
      getStage(CacheProcessor.Instance().getWorkflowData().stages, id),
    intervalsInRange: async (_, { timeRange }, userContext: UserContext) =>
      WorkflowProcessor.Instance().getIntervalsInRange(
        userContext,
        timeRange.startTime,
        timeRange.endTime
      ),
    interval: async (_, { id }, userContext: UserContext) =>
      WorkflowProcessor.Instance().getInterval(userContext, id),
    stageIntervals: async (_, { id }, userContext: UserContext) =>
      WorkflowProcessor.Instance().getStageIntervals(userContext, id),
    stageInterval: async (_, { id }, userContext: UserContext) =>
      WorkflowProcessor.Instance().getStageInterval(userContext, id),
    activities: async (_, __, userContext: UserContext) =>
      WorkflowProcessor.Instance().getActivities(userContext),
    activity: async (_, { id }, userContext: UserContext) =>
      WorkflowProcessor.Instance().getActivity(userContext, id),
    activityIntervals: async (_, __, userContext: UserContext) =>
      WorkflowProcessor.Instance().getActivityIntervals(userContext),
    activityInterval: async (_, { id }, userContext: UserContext) =>
      WorkflowProcessor.Instance().getActivityInterval(userContext, id),
    stageIntervalsInRange: async (_, { timeRange }, userContext: UserContext) =>
      WorkflowProcessor.Instance().getStageIntervalsInRange(
        userContext,
        timeRange.startTime,
        timeRange.endTime
      )
  },

  // Mutation resolvers
  Mutation: {
    // Mark the processing stage interval, updating the status
    markStageInterval: async (_, { stageIntervalId, input }, userContext: UserContext) => {
      logger.info(`Mark stage interval ${stageIntervalId}. User: ${userContext.userName}`);
      // Apply the marking input to the processing stage with the provided ID
      const stageInterval = await WorkflowProcessor.Instance().markStageInterval(
        userContext,
        stageIntervalId,
        input
      );
      // Publish the updated stages
      pubsub
        .publish(subConfig.channels.stagesChanged, {
          stagesChanged: await WorkflowProcessor.Instance().getStages(userContext)
        })
        .catch(e => logger.warn(e));
      return stageInterval;
    },

    // Mark the processing activity interval, updating the status
    markActivityInterval: async (_, { activityIntervalId, input }, userContext: UserContext) => {
      performanceLogger.performance('markActivityInterval', 'enteringResolver');
      logger.info(
        `Mark Activity called with status: ${input.status}. User: ${userContext.userName}`
      );
      const currentActivityId = WorkflowProcessor.Instance().getCurrentOpenActivityId(userContext);
      // Container used to return SignalDetections that were saved
      // const dataPayload: DataPayload = createEmptyDataPayload();

      // If complete has conflicts in saving do not complete interval only save and return
      // current activity
      let activityInterval: model.ProcessingActivityInterval = await WorkflowProcessor.Instance().getActivityInterval(
        userContext,
        currentActivityId
      );
      let dataPayload = createEmptyDataPayload();
      if (input.status === model.IntervalStatus.Complete) {
        dataPayload = await WorkflowProcessor.Instance().saveIntervalChanges(userContext);
        // Publish the saved events and signal detections
        dataPayloadPubSub
          .publish(commonConfig.channels.dataPayload, { dataPayload })
          .catch(e => logger.warn(e));

        // If any events are in conflict do not complete
        if (!EventProcessor.Instance().areAnyEventsInConflict(userContext)) {
          // Apply the marking input to the processing activity with the provided ID
          activityInterval = await WorkflowProcessor.Instance().markActivityInterval(
            userContext,
            activityIntervalId,
            input
          );
        }
      } else if (input.status === model.IntervalStatus.InProgress) {
        // Apply the marking input to the processing activity with the provided ID
        activityInterval = await WorkflowProcessor.Instance().markActivityInterval(
          userContext,
          activityIntervalId,
          input
        );
        if (currentActivityId !== '') {
          // Only clear the modified signal detections if switching stages
          const newStageIntervalId = (
            await WorkflowProcessor.Instance().getActivityInterval(userContext, activityIntervalId)
          ).stageIntervalId;
          const prevStageIntervalId = (
            await WorkflowProcessor.Instance().getActivityInterval(userContext, currentActivityId)
          ).stageIntervalId;
          if (newStageIntervalId !== prevStageIntervalId) {
            userContext.userCache.clearHistory();
            await WorkflowProcessor.Instance().loadSDsAndEvents(userContext, activityInterval);
          }
        } else {
          userContext.userCache.clearHistory();
          await WorkflowProcessor.Instance().loadSDsAndEvents(userContext, activityInterval);
        }
      }
      // Publish the updated stages
      const changedStages = await WorkflowProcessor.Instance().getStages(userContext);
      pubsub
        .publish(subConfig.channels.stagesChanged, { stagesChanged: changedStages })
        .catch(e => logger.warn(e));
      // load sds and events if inProgress
      performanceLogger.performance('markActivityInterval', 'leavingResolver');
      return {
        activityInterval,
        dataPayload
      };
    },

    // set a new time interval for the workflow
    setTimeInterval: async (_, { startTimeSec, endTimeSec }, userContext: UserContext) => {
      logger.info(
        `Setting time interval of ${startTimeSec} to ${endTimeSec}. User: ${userContext.userName}`
      );
      WorkflowProcessor.Instance().setNewTimeRange(userContext, startTimeSec, endTimeSec);
      return WorkflowProcessor.Instance().getStages(userContext);
    }
  },

  // Subscription Resolvers
  Subscription: {
    stagesChanged: {
      subscribe: async () => pubsub.asyncIterator(subConfig.channels.stagesChanged)
    }
  },

  // Field resolvers for ProcessingStage
  ProcessingStage: {
    activities: async (stage: model.ProcessingStage, _, userContext: UserContext) =>
      WorkflowProcessor.Instance().getActivitiesByStage(userContext, stage.id),
    // Field intervals accepts an optional TimeRange parameter with startTime and endTime
    // date objects. If the parameter is provided, filter the stage intervals based on the
    // TimeRange bounds.
    intervals: async (stage: model.ProcessingStage, { timeRange }, userContext: UserContext) =>
      WorkflowProcessor.Instance().getIntervalsByStage(userContext, stage.id, timeRange)
  },

  // Field resolvers for ProcessingInterval
  ProcessingInterval: {
    stageIntervals: async (interval: model.ProcessingInterval, _, userContext: UserContext) =>
      WorkflowProcessor.Instance().getStageIntervalsByInterval(userContext, interval.id)
  },

  // Field resolvers for ProcessingStageInterval
  ProcessingStageInterval: {
    activityIntervals: async (
      stageInterval: model.ProcessingStageInterval,
      _,
      userContext: UserContext
    ) =>
      WorkflowProcessor.Instance().getActivityIntervalsByStageInterval(
        userContext,
        stageInterval.id
      ),
    completedBy: async (
      stageInterval: model.ProcessingStageInterval,
      _,
      userContext: UserContext
    ) => ({ userName: stageInterval.completedByUserName })
  },

  // Field resolvers for ProcessingActivityInterval
  ProcessingActivityInterval: {
    activity: async (
      activityInterval: model.ProcessingActivityInterval,
      _,
      userContext: UserContext
    ) => WorkflowProcessor.Instance().getActivity(userContext, activityInterval.activityId),
    activeAnalysts: async (
      activityInterval: model.ProcessingActivityInterval,
      _,
      userContext: UserContext
    ) =>
      activityInterval.activeAnalystUserNames
        .map(userName => ({ userName }))
        // TODO this is to keep the 'cannot return null for non-nullable field Analyst.userName' from happening
        // TODO We need to find the root cause, but this should stop it for now
        .filter(analyst => analyst !== undefined),
    completedBy: async (
      activityInterval: model.ProcessingActivityInterval,
      _,
      userContext: UserContext
    ) => ({ userName: activityInterval.completedByUserName })
  }
};
