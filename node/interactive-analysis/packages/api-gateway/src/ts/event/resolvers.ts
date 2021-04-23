import config from 'config';
import { PubSub } from 'graphql-subscriptions';
import { produce } from 'immer';
import flatMap from 'lodash/flatMap';
import max from 'lodash/max';
import uniqBy from 'lodash/uniqBy';
import { CacheProcessor } from '../cache/cache-processor';
import { DataPayload, UserActionDescription, UserContext } from '../cache/model';
import { DistanceSourceType, Location } from '../common/model';
import { pubsub as dataPayloadPubSub } from '../common/resolvers';
import { gatewayLogger as logger } from '../log/gateway-logger';
import { performanceLogger } from '../log/performance-logger';
import { SignalDetectionHypothesis } from '../signal-detection/model';
import { SignalDetectionProcessor } from '../signal-detection/signal-detection-processor';
import { ProcessingStationProcessor } from '../station/processing-station/processing-station-processor';
import { createDataPayload, createEmptyDataPayload } from '../util/common-utils';
import { findArrivalTimeFeatureMeasurementValue } from '../util/feature-measurement-utils';
import { getStage } from '../util/workflow-util';
import { EventProcessor } from './event-processor';
import * as model from './model-and-schema/model';
/**
 * Resolvers for the event API gateway
 */

// Create the publish/subscribe API for GraphQL subscriptions
export const pubsub = new PubSub();

// Load configuration settings
const settings = config.get('event');
const dataSubscription = config.get('common.subscriptions.channels.dataPayload');

// GraphQL Resolvers
export const resolvers = {
  // Query resolvers
  Query: {
    eventsInTimeRange: async (_, { timeRange }, userContext: UserContext) => {
      // tslint:disable-next-line: max-line-length
      logger.info(
        `Events in time range requested for ${timeRange.startTime} to ${timeRange.endTime}. User: ${userContext.userName}`
      );
      return EventProcessor.Instance().getEventsInTimeRange(userContext, timeRange);
    },

    eventById: async (_, { eventId }, userContext: UserContext) => {
      logger.info(`Getting event by id: ${eventId}. User: ${userContext.userName}`);
      return userContext.userCache.getEventById(eventId);
    },

    loadEventsInTimeRange: async (_, { timeRange }, userContext: UserContext) => {
      // tslint:disable-next-line: max-line-length
      logger.info(
        `Loading events in time range requested for ${timeRange.startTime} to ${timeRange.endTime}. User: ${userContext.userName}`
      );
      return EventProcessor.Instance().loadEventsInTimeRange(userContext, timeRange);
    }
  },

  // Mutation resolvers
  Mutation: {
    createEvent: async (
      _,
      { signalDetectionIds },
      userContext: UserContext
    ): Promise<DataPayload> => {
      logger.info(
        `Creating event with SD hyps: ${signalDetectionIds}. User: ${userContext.userName}`
      );
      // Update the event directly
      const { event, sds } = await EventProcessor.Instance().createEvent(
        userContext,
        signalDetectionIds
      );
      // Set data to user cache
      userContext.userCache.setEventsAndSignalDetections(
        UserActionDescription.CREATE_EVENT,
        [event],
        sds
      );

      const eventsToPublish = EventProcessor.Instance().getEventsAndSdsAffectedByAssociations(
        userContext.userCache.getEvents(),
        sds
      ).events;
      const uniqEvents = uniqBy([...eventsToPublish, event], e => e.id);
      return createDataPayload(uniqEvents, sds, []);
    },
    // Update an existing event (without creating a new event hypothesis)
    updateEvents: async (_, { eventIds, input }, userContext: UserContext) => {
      logger.info(`Updating events: ${eventIds}. User: ${userContext.userName}`);
      // Update the event directly
      performanceLogger.performance('updateEvents', 'enteringResolver');

      const dataPayloads: DataPayload[] = await EventProcessor.Instance().updateEvents(
        userContext,
        eventIds,
        input
      );

      const payloadToReturn = createDataPayload(
        flatMap(dataPayloads.map(dp => dp.events)),
        flatMap(dataPayloads.map(dp => dp.sds)),
        flatMap(dataPayloads.map(dp => dp.qcMasks))
      );

      // Create a payload to publish that removes the sds and masks
      // The sds were resolving incorrectly for different users
      const payloadToPublish = produce<DataPayload>(createEmptyDataPayload(), draftState => {
        draftState.events = payloadToReturn.events;
      });

      // tslint:disable-next-line: no-floating-promises
      dataPayloadPubSub.publish(dataSubscription, { dataPayload: payloadToPublish });

      performanceLogger.performance('updateEvents', 'leavingResolver');
      return payloadToReturn;
    },

    // TODO: What is this for (currently not used in UI)?
    // TODO: Future use?
    // Lookup (call streaming service) the feature predictions by Event Id
    updateFeaturePredictions: async (_, { eventId }, userContext: UserContext) => {
      logger.info(
        `Updating feature predictions for event ${eventId}. User: ${userContext.userName}`
      );
      const event = userContext.userCache.getEventById(eventId);
      if (event) {
        const updatedEvent = await EventProcessor.Instance().computeFeaturePredictions(
          userContext,
          event
        );
        userContext.userCache.setEvent(
          UserActionDescription.UPDATE_EVENT_FEATURE_PREDICTIONS,
          updatedEvent
        );
        return createDataPayload([updatedEvent], [], []);
      }
      return undefined;
    },

    // Mutation to (un)associate Signal Detections to event hypothesis. Returns the updated event
    // tslint:disable-next-line: max-line-length
    changeSignalDetectionAssociations: async (
      _,
      { eventHypothesisId, signalDetectionIds, associate },
      userContext: UserContext
    ): Promise<DataPayload> => {
      // tslint:disable-next-line: max-line-length
      logger.info(
        `Updating associations for event hypothesis ${eventHypothesisId}. User: ${userContext.userName}`
      );
      const ev = EventProcessor.Instance().getEventByHypId(userContext, eventHypothesisId);
      const signalDetections = SignalDetectionProcessor.Instance().getSignalDetectionsById(
        userContext,
        signalDetectionIds
      );
      if (associate) {
        const { event, sds } = EventProcessor.Instance().associateSignalDetections(
          ev,
          signalDetections
        );

        // Set data to user cache
        const description =
          sds.length > 1
            ? UserActionDescription.CHANGE_SIGNAL_DETECTION_ASSOCIATIONS_ASSOCIATE_MULTIPLE
            : UserActionDescription.CHANGE_SIGNAL_DETECTION_ASSOCIATIONS_ASSOCIATE;
        userContext.userCache.setEventsAndSignalDetections(description, [event], sds);

        // get events to publish
        const eventsToPublish = EventProcessor.Instance().getEventsAndSdsAffectedByAssociations(
          userContext.userCache.getEvents(),
          sds
        ).events;
        const uniqEvents = uniqBy([...eventsToPublish, event], e => e.id);

        return createDataPayload(uniqEvents, sds, []);
      }

      {
        // Before changing associations - get the events that will be effected for publishing/returning
        const affectedEvents = EventProcessor.Instance().getEventsAndSdsAffectedByAssociations(
          userContext.userCache.getEvents(),
          signalDetections
        ).events;

        const { event, sds } = EventProcessor.Instance().unassociateSignalDetections(
          ev,
          signalDetections
        );

        // Set data to user cache
        const description =
          sds.length > 1
            ? UserActionDescription.CHANGE_SIGNAL_DETECTION_ASSOCIATIONS_UNASSOCIATE_MULTIPLE
            : UserActionDescription.CHANGE_SIGNAL_DETECTION_ASSOCIATIONS_UNASSOCIATE;
        userContext.userCache.setEventsAndSignalDetections(description, [event], sds);

        const uniqEvents = uniqBy([...affectedEvents, event], e => e.id);
        return createDataPayload(uniqEvents, sds, []);
      }
    },

    // Mutation to Locate Event
    locateEvent: async (
      _,
      input: model.LocateEventInput,
      userContext: UserContext
    ): Promise<DataPayload> => {
      logger.info(`Locating Event Hyp: ${input.eventHypothesisId}. User: ${userContext.userName}`);
      const updatedEvent = await EventProcessor.Instance().locateEvent(
        userContext,
        input.eventHypothesisId,
        input.preferredLocationSolutionId,
        input.locationBehaviors
      );
      userContext.userCache.setEvent(UserActionDescription.UPDATE_EVENT_LOCATE, updatedEvent);
      return createDataPayload([updatedEvent], [], []);
    },

    // Mutation to compute network mag solutions
    computeNetworkMagnitudeSolution: async (
      _,
      {
        computeNetworkMagnitudeSolutionInput
      }: { computeNetworkMagnitudeSolutionInput: model.ComputeNetworkMagnitudeInput },
      userContext: UserContext
    ): Promise<model.ComputeNetworkMagnitudeDataPayload> => {
      // tslint:disable-next-line: max-line-length
      logger.info(
        `computing network Magnitude solution: ${computeNetworkMagnitudeSolutionInput.magnitudeType.toString()}. User: ${
          userContext.userName
        }`
      );
      // TODO remove undefined inputs once we are using this mutation for reals
      const event = await EventProcessor.Instance().updateEventWithMagDefiningChanges(
        userContext,
        computeNetworkMagnitudeSolutionInput.stationNames,
        computeNetworkMagnitudeSolutionInput.defining,
        computeNetworkMagnitudeSolutionInput.magnitudeType,
        computeNetworkMagnitudeSolutionInput.eventHypothesisId
      );
      userContext.userCache.setEvent(UserActionDescription.UPDATE_EVENT_MAGNITUDE, event);
      const payload = createDataPayload([event], [], []);
      return {
        status: EventProcessor.Instance().isMocked()
          ? [
              { stationId: 'PDAR', rational: 'Distance outside acceptable range' },
              { stationId: 'CMAR', rational: 'Defining value not provided for station' }
            ]
          : [],
        dataPayload: payload
      };
    },

    // Mutation to Locate Event
    saveEvent: async (_, { eventId }, userContext: UserContext): Promise<DataPayload> => {
      logger.info(`Saving event with id ${eventId} for username: ${userContext.userName}`);
      const event = userContext.userCache.getEventById(eventId);

      // Save signal detections, channel segments, and events
      // Save signal detections for the provided event
      const associatedSignalDetections = event.signalDetectionIds.map(id =>
        userContext.userCache.getSignalDetectionById(id)
      );

      const { events, signalDetections } = await EventProcessor.Instance().save(
        userContext,
        associatedSignalDetections,
        [event]
      );
      const dataPayload = createDataPayload(events, signalDetections, []);

      // !TODO do we really need to send the rejected back?
      // return all of the associated signal detections in the data payload,
      // including the rejected associated signal detections
      // eventProcessor.getAssociatedSignalDetectionsForEvent(userContext, event);

      // tslint:disable-next-line: no-floating-promises
      dataPayloadPubSub.publish(dataSubscription, { dataPayload });
      return dataPayload;
    },

    // Save all changed events and SDs
    saveAllModifiedEvents: async (_, __, userContext: UserContext) => {
      const saveResponse = await EventProcessor.Instance().saveAll(userContext);
      const dataPayload = createDataPayload(saveResponse.events, saveResponse.signalDetections, []);
      dataPayloadPubSub.publish(dataSubscription, { dataPayload }).catch(e => logger.warn(e));
      return dataPayload;
    }
  },

  // Subscription Resolvers
  Subscription: {
    // Subscription for events created
    eventsCreated: {
      subscribe: async () => pubsub.asyncIterator(settings.subscriptions.channels.eventsCreated)
    }
  },

  // Field resolvers for Event
  Event: {
    currentEventHypothesis: (event: model.Event, _, userContext: UserContext) =>
      EventProcessor.Instance().getCurrentEventHypothesisByEventId(userContext, event.id),

    modified: (event: model.Event, maybe, userContext: UserContext) =>
      event.currentEventHypothesis.eventHypothesis.modified,

    hasConflict: (event: model.Event, maybe, userContext: UserContext) => event.hasConflict,

    conflictingSdIds: async (event: model.Event, _, userContext: UserContext) =>
      event.signalDetectionIds
        .map(id => userContext.userCache.getSignalDetectionById(id))
        .filter(sd => sd.hasConflict)
        .map(sd => sd.id),

    distanceToSource: async (event: model.Event, _, userContext: UserContext) =>
      ProcessingStationProcessor.Instance().getDTSForDefaultStations(userContext, {
        sourceId: event.id,
        sourceType: DistanceSourceType.Event
      })
  },

  // Field resolvers for PreferredEventHypothesis
  PreferredEventHypothesis: {
    processingStage: (
      preferredHypothesis: model.PreferredEventHypothesis,
      _,
      userContext: UserContext
    ) =>
      getStage(
        CacheProcessor.Instance().getWorkflowData().stages,
        preferredHypothesis.processingStageId
      )
  },

  // Field resolvers for EventHypothesis
  EventHypothesis: {
    event: (hypothesis: model.EventHypothesis, _, userContext: UserContext) =>
      userContext.userCache.getEventById(hypothesis.eventId),
    signalDetectionAssociations: (hypothesis: model.EventHypothesis, _, userContext: UserContext) =>
      hypothesis.associations.filter(assoc => !assoc.rejected),
    associationsMaxArrivalTime: (
      hypothesis: model.EventHypothesis,
      _,
      userContext: UserContext
    ) => {
      // Get the associations and before returning set the max arrival time
      const associations: model.SignalDetectionEventAssociation[] = hypothesis.associations.filter(
        association => !association.rejected
      );
      // If not defined or empty return
      if (!associations || associations.length === 0) {
        return 0;
      }
      const detectionTimes: number[] = associations.map(association => {
        // Lookup the SD Hypothesis from the association's SDH id
        const sdHypo: SignalDetectionHypothesis = SignalDetectionProcessor.Instance().getSignalDetectionHypothesisById(
          userContext.userCache.getSignalDetections(),
          association.signalDetectionHypothesisId
        );
        if (!sdHypo || !sdHypo.featureMeasurements) {
          return undefined;
        }
        // Find all the arrival time feature measurement times
        const arrivalTimeMeasurementValue = findArrivalTimeFeatureMeasurementValue(
          sdHypo.featureMeasurements
        );
        if (!arrivalTimeMeasurementValue || !arrivalTimeMeasurementValue.value) {
          return undefined;
        }
        return arrivalTimeMeasurementValue.value;
      });

      // Set max arrival time value in the Event Hypo before returning
      if (detectionTimes && detectionTimes.length <= 0) {
        return 0;
      }
      const maxDetectionTime = max(detectionTimes);
      if (maxDetectionTime === undefined || maxDetectionTime === null || isNaN(maxDetectionTime)) {
        return 0;
      }
      return maxDetectionTime;
    }
  },

  // Field resolvers for SignalDetectionEventAssociation
  SignalDetectionEventAssociation: {
    signalDetectionHypothesis: (
      association: model.SignalDetectionEventAssociation,
      _,
      userContext: UserContext
    ) => {
      const sdHypo = SignalDetectionProcessor.Instance().getSignalDetectionHypothesisById(
        userContext.userCache.getSignalDetections(),
        association.signalDetectionHypothesisId
      );
      return sdHypo;
    }
  },

  // Field resolvers for LocationSolution
  LocationSolution: {
    locationType: async () => 'standard',
    locationToStationDistances: async (
      locationSolution: model.LocationSolution,
      _,
      userContext: UserContext
    ): Promise<model.LocationToStationDistance[]> => {
      const location: Location = {
        depthKm: locationSolution.location.depthKm,
        elevationKm: 0,
        latitudeDegrees: locationSolution.location.latitudeDegrees,
        longitudeDegrees: locationSolution.location.longitudeDegrees
      };

      const defaultStationsList = ProcessingStationProcessor.Instance().getDefaultProcessingStations();
      const distances: model.LocationToStationDistance[] = defaultStationsList.map(station => ({
        distance: ProcessingStationProcessor.Instance().getDistanceToSource(location, station),
        azimuth: ProcessingStationProcessor.Instance().getAzimuthToSource(location, station),
        stationId: station.name
      }));
      return distances;
    }
  }
};
