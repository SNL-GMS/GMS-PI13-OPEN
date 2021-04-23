import config from 'config';
import { PubSub } from 'graphql-subscriptions';
import { produce } from 'immer';
import { DataPayload, UserActionDescription, UserContext } from '../cache/model';
import { ChannelSegmentProcessor } from '../channel-segment/channel-segment-processor';
import { ChannelSegmentType } from '../channel-segment/model';
import { EventProcessor } from '../event/event-processor';
import { gatewayLogger as logger } from '../log/gateway-logger';
import { performanceLogger } from '../log/performance-logger';
import { createDataPayload, replaceByIdOrAddToList } from '../util/common-utils';
import {
  findArrivalTimeFeatureMeasurementValue,
  findPhaseFeatureMeasurementValue
} from '../util/feature-measurement-utils';
import { doesPhaseNeedAmplitudeReview } from '../util/signal-detection-utils';
import * as model from './model';
import { SignalDetectionProcessor } from './signal-detection-processor';

/**
 * Resolvers for the signal detection API gateway
 */

// Create the publish/subscribe API for GraphQL subscriptions
export const pubsub = new PubSub();

// Load configuration settings
const settings = config.get('signalDetection');

// GraphQL Resolvers
export const resolvers = {
  // Query resolvers

  Query: {
    signalDetectionsByDefaultStations: async (_, { timeRange }, userContext: UserContext) => {
      // tslint:disable-next-line: max-line-length
      logger.info(
        `Getting signal detections for ${timeRange.startTime} to ${timeRange.endTime}. User: ${userContext.userName}`
      );
      return SignalDetectionProcessor.Instance().getSignalDetectionsForDefaultStations(
        userContext,
        timeRange
      );
    },
    signalDetectionsByStation: async (_, { stationIds, timeRange }, userContext: UserContext) => {
      // tslint:disable-next-line: max-line-length
      logger.info(
        `Getting signal detections for stations in ${timeRange.startTime} to ${timeRange.endTime}. User: ${userContext.userName}`
      );
      const sds = SignalDetectionProcessor.Instance().getSignalDetectionsByStation(
        userContext,
        stationIds,
        timeRange
      );
      logger.info(`Returning signal detection count ${sds.length}`);
      return sds;
    },
    signalDetectionsById: async (_, { detectionIds }, userContext: UserContext) => {
      logger.info(
        `Getting signal detections for ids: ${detectionIds}. User: ${userContext.userName}`
      );
      return SignalDetectionProcessor.Instance().getSignalDetectionsById(userContext, detectionIds);
    },
    signalDetectionsByEventId: async (_, { eventId }, userContext: UserContext) => {
      logger.info(`Getting signal detections for event: ${eventId}. User: ${userContext.userName}`);
      return SignalDetectionProcessor.Instance().getSignalDetectionsByEventId(userContext, eventId);
    },
    loadSignalDetectionsByStation: async (
      _,
      { stationIds, timeRange },
      userContext: UserContext
    ) => {
      // tslint:disable-next-line: max-line-length
      logger.info(
        `Loading signal detections in ${timeRange.startTime} to ${timeRange.endTime}. User: ${userContext.userName}`
      );
      return stationIds
        ? SignalDetectionProcessor.Instance().loadSignalDetections(
            userContext,
            timeRange,
            stationIds
          )
        : SignalDetectionProcessor.Instance().getSignalDetectionsForDefaultStations(
            userContext,
            timeRange
          );
    }
  },

  // Mutation resolvers
  Mutation: {
    // Create a new signal detection
    createDetection: async (_, { input }, userContext: UserContext) => {
      // tslint:disable-next-line: max-line-length
      logger.info(
        `Creating signal detection on ${input.stationId} of phase ${input.phase}. User: ${userContext.userName}`
      );
      performanceLogger.performance('createDetection', 'enteringResolver');
      const dataPayload = EventProcessor.Instance().createSignalDetection(userContext, input);
      performanceLogger.performance('createDetection', 'leavingResolver');
      return dataPayload;
    },

    // Update an existing signal detection
    updateDetection: async (_, { detectionId, input }, userContext: UserContext) => {
      logger.info(`Updating detection: ${detectionId}. User: ${userContext.userName}`);
      // Update the hypothesis
      const { signalDetection, event } = userContext.userCache.getSignalDetectionAndEventBySdId(
        detectionId
      );
      const changes = EventProcessor.Instance().updateDetection(
        userContext,
        signalDetection,
        input,
        event
      );
      userContext.userCache.setEventsAndSignalDetections(
        changes.description,
        changes.payload.events,
        changes.payload.sds
      );
      return changes.payload;
    },
    // Update a collection of existing signal detections
    updateDetections: async (_, { detectionIds, input }, userContext: UserContext) => {
      logger.info(`Updating detections: ${detectionIds}. User: ${userContext.userName}`);
      // Update the hypothesis
      performanceLogger.performance('updateDetections', 'enteringResolver');
      const changes = EventProcessor.Instance().updateDetections(userContext, detectionIds, input);
      userContext.userCache.setEventsAndSignalDetections(
        changes.description,
        changes.payload.events,
        changes.payload.sds
      );
      performanceLogger.performance('updateDetections', 'leavingResolver');
      return changes.payload;
    },
    // Reject a collection of existing signal detections
    // tslint:disable-next-line:arrow-return-shorthand
    rejectDetections: async (_, { detectionIds }, userContext: UserContext) => {
      logger.info(`Rejecting detections: ${detectionIds}. User: ${userContext.userName}`);
      // Update the detections with the reject
      const { events, sds } = EventProcessor.Instance().rejectDetections(userContext, detectionIds);

      const description =
        detectionIds.length > 1
          ? UserActionDescription.REJECT_MULTIPLE_DETECTIONS
          : UserActionDescription.REJECT_DETECTION;

      // Set data to user cache
      userContext.userCache.setEventsAndSignalDetections(description, events, sds);

      return createDataPayload(events, sds, []);
    },
    markAmplitudeMeasurementReviewed: async (
      _,
      { signalDetectionIds }: { signalDetectionIds: string[] },
      userContext: UserContext
    ): Promise<DataPayload> => {
      logger.info(
        `Marking SD with ids: ${String(signalDetectionIds)} amplitude measurement as reviewed`
      );
      const updatedSds = signalDetectionIds.map(sdId => {
        const { signalDetection } = userContext.userCache.getSignalDetectionAndEventBySdId(sdId);

        // Only update current hypothesis - no new hypothesis should be created from this operation
        const currentHypothesis = signalDetection.currentHypothesis;

        const updatedHypothesis = produce<model.SignalDetectionHypothesis>(
          currentHypothesis,
          draftState => {
            draftState.reviewed.amplitudeMeasurement = true;
          }
        );

        const updatedSignalDetection = produce<model.SignalDetection>(
          signalDetection,
          draftState => {
            draftState.signalDetectionHypotheses = replaceByIdOrAddToList<
              model.SignalDetectionHypothesis
            >(draftState.signalDetectionHypotheses, updatedHypothesis);
          }
        );
        return updatedSignalDetection;
      });
      userContext.userCache.setEventsAndSignalDetections(
        UserActionDescription.UPDATE_DETECTION_REVIEW_AMPLITUDE,
        [],
        updatedSds
      );
      return createDataPayload([], updatedSds, []);
    }
  },

  // Subscription Resolvers
  Subscription: {
    // Subscription for newly-created signal detection hypotheses
    detectionsCreated: {
      // Set up the subscription to filter results down to those detections that overlap
      // a time range provided by the subscriber upon creating the subscription
      subscribe: async () => pubsub.asyncIterator(settings.subscriptions.channels.detectionsCreated)
    }
  },

  // Field resolvers for SignalDetection
  SignalDetection: {
    modified: (signalDetection: model.SignalDetection, _, userContext: UserContext) =>
      signalDetection.signalDetectionHypotheses.filter(hyp => !hyp.rejected && hyp.modified)
        .length > 0,
    reviewed: (signalDetection: model.SignalDetection, _, userContext: UserContext) =>
      signalDetection.currentHypothesis.reviewed,
    hasConflict: (signalDetection: model.SignalDetection, _, userContext: UserContext) =>
      signalDetection.hasConflict,
    conflictingHypotheses: (signalDetection: model.SignalDetection, _, userContext: UserContext) =>
      SignalDetectionProcessor.Instance().getConflictingSdHypotheses(userContext, signalDetection),
    signalDetectionHypothesisHistory: (
      signalDetection: model.SignalDetection,
      _,
      userContext: UserContext
    ): model.SignalDetectionHypothesisHistory[] =>
      signalDetection.signalDetectionHypotheses.map(sdh => {
        const arrivalTimeFMValue = findArrivalTimeFeatureMeasurementValue(sdh.featureMeasurements);
        if (!arrivalTimeFMValue) {
          return;
        }
        return {
          id: sdh.id,
          phase: findPhaseFeatureMeasurementValue(sdh.featureMeasurements).phase,
          rejected: sdh.rejected,
          arrivalTimeSecs: findArrivalTimeFeatureMeasurementValue(sdh.featureMeasurements).value,
          arrivalTimeUncertainty: findArrivalTimeFeatureMeasurementValue(sdh.featureMeasurements)
            .standardDeviation
        };
      }),
    currentHypothesis: (
      signalDetection: model.SignalDetection,
      _,
      userContext: UserContext
    ): model.SignalDetectionHypothesis => signalDetection.currentHypothesis,
    requiresReview: (
      signalDetection: model.SignalDetection,
      _,
      userContext: UserContext
    ): model.RequiresReview => {
      const phase = findPhaseFeatureMeasurementValue(
        signalDetection.currentHypothesis.featureMeasurements
      );
      return {
        amplitudeMeasurement: phase ? doesPhaseNeedAmplitudeReview(phase.phase) : true
      };
    }
  },

  // Field resolvers for Azimuth FeatureMeasurement to populate the Fk Data
  FeatureMeasurement: {
    channelSegment: async (fm: model.FeatureMeasurement, _, userContext: UserContext) => {
      // Only populate Beam and FkPowerSpectra channel segments,
      // which come from FeatureMeasurementTypes RECEIVER_TO_SOURCE_AZIMUTH or ARRIVAL_TIME
      if (
        (fm &&
          fm.measuredChannelSegmentDescriptor &&
          (fm.featureMeasurementType ===
            model.FeatureMeasurementTypeName.RECEIVER_TO_SOURCE_AZIMUTH ||
            fm.featureMeasurementType === model.FeatureMeasurementTypeName.ARRIVAL_TIME)) ||
        fm.featureMeasurementType === model.FeatureMeasurementTypeName.FILTERED_BEAM
      ) {
        const channelSegment = ChannelSegmentProcessor.Instance().getInCacheChannelSegmentBySegmentDescriptor(
          userContext,
          fm.measuredChannelSegmentDescriptor
        );
        if (channelSegment) {
          // Make sure the channel segment is the correct type for FM
          if (
            fm.featureMeasurementType ===
              model.FeatureMeasurementTypeName.RECEIVER_TO_SOURCE_AZIMUTH &&
            channelSegment.type === ChannelSegmentType.FK_SPECTRA
          ) {
            return channelSegment;
          }
          if (
            fm.featureMeasurementType === model.FeatureMeasurementTypeName.ARRIVAL_TIME &&
            (channelSegment.type === ChannelSegmentType.FK_BEAM ||
              channelSegment.type === ChannelSegmentType.DETECTION_BEAM)
          ) {
            return channelSegment;
          }
          if (
            fm.featureMeasurementType === model.FeatureMeasurementTypeName.FILTERED_BEAM &&
            (channelSegment.type === ChannelSegmentType.FK_BEAM ||
              channelSegment.type === ChannelSegmentType.FILTER)
          ) {
            return channelSegment;
          }
        }
      }
      return undefined;
    },
    measurementValue: (fm: model.FeatureMeasurement, _, userContext) => fm.measurementValue
  },

  // Field resolvers for FeatureMeasurementValue
  FeatureMeasurementValue: {
    /**
     * Special interface resolver to determine the implementing type based on field content
     */
    __resolveType(obj) {
      if (obj) {
        if (
          obj.startTime !== undefined &&
          obj.period !== undefined &&
          obj.amplitude !== undefined
        ) {
          return 'AmplitudeMeasurementValue';
        }
        if (obj.value !== undefined && obj.standardDeviation !== undefined) {
          return 'InstantMeasurementValue';
        }
        if (obj.referenceTime !== undefined && obj.measurementValue !== undefined) {
          return 'NumericMeasurementValue';
        }
        if (obj.phase !== undefined && obj.confidence !== undefined) {
          return 'PhaseTypeMeasurementValue';
        }
        if (obj.strValue !== undefined) {
          return 'StringMeasurementValue';
        }
      }
      return undefined;
    }
  }
};
