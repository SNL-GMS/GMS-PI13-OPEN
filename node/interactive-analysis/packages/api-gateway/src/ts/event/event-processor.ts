import { uuid4 } from '@gms/common-util';
import config from 'config';
import { produce } from 'immer';
import cloneDeep from 'lodash/cloneDeep';
import flatMap from 'lodash/flatMap';
import includes from 'lodash/includes';
import uniqBy from 'lodash/uniqBy';
import { CacheProcessor } from '../cache/cache-processor';
import { DataPayload, UserActionDescription, UserContext } from '../cache/model';
import { PhaseType, TimeRange } from '../common/model';
import { ConfigProcessor } from '../config/config-processor';
import { gatewayLogger as logger } from '../log/gateway-logger';
import {
  FeatureMeasurementTypeName,
  NewDetectionInput,
  SignalDetection,
  SignalDetectionHypothesis,
  UpdateDetectionInput
} from '../signal-detection/model';
import { convertSignalDetectionHypothesisToOSD } from '../signal-detection/signal-detection-converter';
import { SignalDetectionProcessor } from '../signal-detection/signal-detection-processor';
import { ProcessingChannel } from '../station/processing-station/model';
import { ProcessingStationProcessor } from '../station/processing-station/processing-station-processor';
import { systemConfig } from '../system-config';
import { createDataPayload, replaceByIdOrAddToList } from '../util/common-utils';
import { HttpClientWrapper, HttpResponse } from '../util/http-wrapper';
import {
  createSignalDetection,
  getSignalDetectionHypForEvent,
  getSignalDetectionsAssociatedToEvent
} from '../util/signal-detection-utils';
import { convertOSDProcessingChannel } from '../util/station-utils';
import { getStage } from '../util/workflow-util';
import * as eventMockBackend from './event-mock-backend';
import {
  callLocateEvent,
  computeNetworkMagnitudeSolutionsWithService,
  getEventsInTimeRangeFromService,
  getFeaturePredictionsFromService,
  SaveEventsServiceResponse,
  saveEventsToService
} from './event-services-client';
import * as model from './model-and-schema/model';
import * as osdModel from './model-and-schema/model-osd';
import {
  getAssociationsToRemove,
  removeAssociationsToUndefinedSds,
  updateLocationBehaviors
} from './utils/association-util';
import {
  convertEventHypothesisToOSD,
  convertLocationSolutionFromOSD,
  convertLocationSolutionToOSD
} from './utils/event-format-converter';
import { updateEventCurrentHypothesis, updateEventHypothesis } from './utils/event-produce';
import {
  createEmptyEvent,
  getEventHyposAssocToSd,
  getEventHyposAssocToSdMap,
  isEventInTimeRange
} from './utils/event-utils';
import {
  createLocationSolutionSet,
  getLatestLSSForEventHyp,
  getRandomLocationForNewHypothesis,
  makeSignalDetectionSnapshots,
  produceEventHypothesisForLocationCall
} from './utils/location-utils';
import {
  getDefiningBehaviorsForEvent,
  getNetworkMagClientArguments
} from './utils/network-magnitude-utils';

/**
 * Event processor obtains events by IDs or time range. Handles location event, and creating a
 * new event location. Modifies and creates existing events and feature predictions.
 */
export class EventProcessor {
  /** The singleton instance */
  private static instance: EventProcessor;

  /** Settings for the event processor */
  private readonly settings: any;

  /** Axios http wrapper  */
  private readonly httpWrapper: HttpClientWrapper;

  /** Default phases */
  private defaultPhases: string[] = [];

  /** Preferred Location solution restraint order */
  private preferredLocationSolutionRestraintOrder: string[] = [];

  private constructor() {
    this.settings = config.get('event');
    this.httpWrapper = new HttpClientWrapper();
  }

  /**
   * Returns the singleton instance of the cache processor.
   * @returns the instance of the cache processor
   */
  public static Instance(): EventProcessor {
    if (EventProcessor.instance === undefined) {
      EventProcessor.instance = new EventProcessor();
      EventProcessor.instance.initialize();
    }
    return EventProcessor.instance;
  }

  /**
   * Returns true if the mock backend is enabled
   */
  public isMocked(): boolean {
    return this.settings.backend.mock.enable;
  }

  /**
   * Retrieve the event hypotheses with the provided ID.
   *
   * @param hypothesisId The ID of the event hypotheses to retrieve
   */
  public getEventHypothesisById(
    userContext: UserContext,
    hypothesisId: string
  ): model.EventHypothesis {
    // get the events by time range
    const events = userContext.userCache.getEvents();
    const hypotheses = [];
    events.forEach(event => {
      event.hypotheses.forEach(hypo => hypotheses.push(hypo));
    });
    const hyp = hypotheses.find(hypothesis => hypothesis.id === hypothesisId);
    if (!hyp) {
      logger.warn('Event Hypothesis Not found: ' + hypothesisId);
    }
    return hyp;
  }

  /**
   * Retrieve the current (modified?) event hypothesis for the event with the provided ID.
   * If not set then set the current event hypothesis
   * @param eventId The ID of the event to locate the current hypothesis
   *
   * @returns current PreferredEventHypothesis
   */
  public getCurrentEventHypothesisByEventId(
    userContext: UserContext,
    eventId: string
  ): model.PreferredEventHypothesis {
    // Check cache for event with given ID
    const event = userContext.userCache.getEventById(eventId);
    if (!event) {
      return undefined;
    }

    // Find the event and return current event hypothesis if set
    if (event && event.currentEventHypothesis) {
      return event.currentEventHypothesis;
    }

    throw new Error(`Current event hypothesis is undefined for event id ${eventId}`);
  }

  /**
   * Retrieve the event with the provided hypothesis ID.
   * @param eventHypId The ID of the event hyp to retrieve
   */
  public getEventByHypId(userContext: UserContext, eventHypId: string): model.Event {
    let eventToReturn: model.Event;
    userContext.userCache.getEvents().forEach((event: model.Event) => {
      if (event.hypotheses.findIndex(eventHyp => eventHyp.id === eventHypId) >= 0) {
        eventToReturn = event;
      }
    });
    return eventToReturn;
  }

  /**
   * Locate Event streaming call to calculate the location solutions for the given EventHypothesis
   * @param userContext user context for the current user
   * @param eventHypothesisId Id to find event hypothesis to send
   * @param preferredLocationSolutionId which preferred location solution to send the service
   * @param locationBehaviors to send in the event hypothesis this tells streaming call
   *        which feature measurements to use
   *
   * @returns Event with updated hypothesis with updated Location Solutions
   */
  public async locateEvent(
    userContext: UserContext,
    eventHypothesisId: string,
    preferredLocationSolutionId: string,
    locationBehaviors: model.LocationBehavior[]
  ): Promise<model.Event> {
    const updatedEvent = this.prepareEventForModification(
      this.getEventByHypId(userContext, eventHypothesisId)
    );

    if (!updatedEvent) {
      logger.warn(
        `Could not find Event for event hypothesis id: ` +
          `${eventHypothesisId} for locate, returning empty array.`
      );
      return undefined;
    }
    let updatedEventHypothesis = produceEventHypothesisForLocationCall(
      updatedEvent.currentEventHypothesis.eventHypothesis,
      preferredLocationSolutionId,
      locationBehaviors
    );

    // We get the magnitude defining settings from the event and use them to calculate mag after locate
    const definingSettingsForEvent = flatMap(
      systemConfig.getCalculableMagnitudes(this.settings.backend.mock.enable),
      magType => getDefiningBehaviorsForEvent(updatedEvent, magType)
    );

    // Convert OSD compatible event hypothesis to populate
    const osdEventHypo: osdModel.EventHypothesisOSD = convertEventHypothesisToOSD(
      userContext,
      updatedEventHypothesis
    );

    // Get signal detections associated and convert them to OSD
    const signalDetections: SignalDetection[] = await SignalDetectionProcessor.Instance().getSignalDetectionsByEventId(
      userContext,
      osdEventHypo.eventId
    );

    const locationSolutions = await this.computeLocationSolutions(
      userContext,
      osdEventHypo,
      locationBehaviors,
      signalDetections
    );

    // If the location solutions list is empty then all three calls failed. So raise an exception
    if (locationSolutions.length === 0) {
      throw new Error(`Locate event failed for event ${osdEventHypo.eventId}`);
    }

    // Get SD hypothesis for network magnitude calc
    const sdHypothesis = updatedEventHypothesis.associations
      .map(assoc =>
        SignalDetectionProcessor.Instance().getSignalDetectionHypothesisById(
          userContext.userCache.getSignalDetections(),
          assoc.signalDetectionHypothesisId
        )
      )
      .filter(sdh => sdh);

    const sds = sdHypothesis.map(sdh =>
      userContext.userCache.getSignalDetectionById(sdh.parentSignalDetectionId)
    );

    const eventAfterLocate = this.updateEventWithLocationSolutions(updatedEvent, locationSolutions);

    const locSolutionsWithMagnitudes = await this.getUpdatedLocationSolutionsWithNewMagnitudes(
      userContext,
      eventAfterLocate,
      sds,
      definingSettingsForEvent,
      true
    );

    const updatedLocationSolutionSet = produce<model.LocationSolutionSet>(
      getLatestLSSForEventHyp(eventAfterLocate.currentEventHypothesis.eventHypothesis),
      draftState => {
        draftState.locationSolutions = locSolutionsWithMagnitudes;
      }
    );

    updatedEventHypothesis = produce<model.EventHypothesis>(
      eventAfterLocate.currentEventHypothesis.eventHypothesis,
      draftState => {
        draftState.preferredLocationSolution.locationSolution = this.getPreferredLocation(
          updatedLocationSolutionSet
        );
        draftState.locationSolutionSets = replaceByIdOrAddToList<model.LocationSolutionSet>(
          draftState.locationSolutionSets,
          updatedLocationSolutionSet
        );
      }
    );

    const updatedEventWithMagnitudes = produce<model.Event>(eventAfterLocate, draftState => {
      draftState.hypotheses = replaceByIdOrAddToList<model.EventHypothesis>(
        draftState.hypotheses,
        updatedEventHypothesis
      );
      if (draftState.currentEventHypothesis.eventHypothesis.id === updatedEventHypothesis.id) {
        draftState.currentEventHypothesis.eventHypothesis = updatedEventHypothesis;
      }
    });

    const finalUpdatedEvent = await this.computeFeaturePredictions(
      userContext,
      updatedEventWithMagnitudes
    );

    return finalUpdatedEvent;
  }

  /**
   * Requests events from the cache
   *
   * @param timeRange range of time to get events from
   */
  public async getEventsInTimeRange(
    userContext: UserContext,
    timeRange: TimeRange
  ): Promise<model.Event[]> {
    const events = userContext.userCache
      .getEvents()
      .filter(event => isEventInTimeRange(event, timeRange));
    return events;
  }

  /**
   * Requests events from the backend and returns a list
   * Events are added to the cache
   *
   * @param timeRange range of time to get events from
   */
  public async loadEventsInTimeRange(
    userContext: UserContext,
    timeRange: TimeRange
  ): Promise<model.Event[]> {
    const requestConfig = this.settings.backend.services.getEventsByTimeAndLatLong.requestConfig;
    const events = await getEventsInTimeRangeFromService(
      timeRange,
      CacheProcessor.Instance().getCurrentOpenActivity()
        ? getStage(
            CacheProcessor.Instance().getWorkflowData().stages,
            CacheProcessor.Instance().getCurrentOpenActivity().stageIntervalId
          ).id
        : '',
      userContext.userCache.getSignalDetections(),
      ProcessingStationProcessor.Instance().getDefaultProcessingStations(),
      id => ProcessingStationProcessor.Instance().getStationByChannelName(id),
      this.httpWrapper,
      requestConfig
    );
    const validSdHypIds = SignalDetectionProcessor.Instance().getAllValidSdHypIds(userContext);
    const cleanedEvents = removeAssociationsToUndefinedSds(events, validSdHypIds);
    CacheProcessor.Instance().addLoadedEventsToGlobalCache(cleanedEvents);
    return this.getEventsInTimeRangeFromCache(userContext, timeRange);
  }

  /**
   * Creates an empty event and then associates the sd hyps based on input list
   * @param userContext user context for the current user
   * @param sdHypIds list of ids linked to the hypotheses to associate
   */
  public async createEvent(
    userContext: UserContext,
    sdIds: string[]
  ): Promise<{ event: model.Event; sds: SignalDetection[] }> {
    // Create the event object
    const emptyEvent: model.Event = createEmptyEvent(
      CacheProcessor.Instance().getCurrentOpenActivity()
        ? getStage(
            CacheProcessor.Instance().getWorkflowData().stages,
            CacheProcessor.Instance().getCurrentOpenActivity().stageIntervalId
          ).id
        : ''
    );
    const signalDetections = SignalDetectionProcessor.Instance().getSignalDetectionsById(
      userContext,
      sdIds
    );
    const sdHypIds = signalDetections.map(sd => sd.currentHypothesis.id);

    let updatedEvent = await this.setNewEventHypothesisLocation(userContext, emptyEvent, sdHypIds);
    let updatedSds;
    if (sdIds && sdIds.length > 0) {
      const { event, sds } = this.associateSignalDetections(updatedEvent, signalDetections);
      updatedEvent = event;
      updatedSds = sds;
    }
    return { event: updatedEvent, sds: updatedSds };
  }

  /**
   * Updates the events with the provided IDs using the provided input parameters. If no updates parameters are
   * included, this method will throw an error
   */
  public async updateEvents(
    userContext: UserContext,
    eventIds: string[],
    input: model.UpdateEventInput
  ): Promise<DataPayload[]> {
    return Promise.all(
      eventIds.map(async eventId => this.updateEvent(userContext, eventId, input))
    );
  }

  /**
   * Updates the event with the provided ID using the provided input parameters. If no updates parameters are
   * included, this method will throw an error
   * @param eventId: The ID of the vent to update
   * @param input: The input parameters to update in the event
   */
  public async updateEvent(
    userContext: UserContext,
    eventId: string,
    input: model.UpdateEventInput
  ): Promise<DataPayload> {
    // Try to retrieve the event with the provided ID; throw an error if it is missing
    let description: UserActionDescription = UserActionDescription.UNKNOWN;

    let updatedEvent = userContext.userCache.getEventById(eventId);

    if (!updatedEvent) {
      throw new Error(`Attempt to update a missing event with ID ${eventId}`);
    }

    // before overwriting get the users last open event id
    const previouslyOpenEventId = userContext.userCache.getOpenEventId();
    // For now call computeFeaturePredictions this is the first time
    // TODO: Rework compute FP as part of AzSlow Compute work
    // This is first opportunity to populate based on an individual event loading on UI
    if (input.status === model.EventStatus.OpenForRefinement) {
      // Set open event for user
      userContext.userCache.setOpenEventId(eventId);
      // Only recompute fps if it is not already open for refinement
      if (updatedEvent.status !== model.EventStatus.OpenForRefinement) {
        updatedEvent = await this.computeFeaturePredictions(userContext, updatedEvent);
        description = UserActionDescription.UPDATE_EVENT_STATUS_OPEN_FOR_REFINEMENT;
        CacheProcessor.Instance().addOrUpdateEventToUser(updatedEvent.id, userContext.userName);
      }
    }
    if (input.status === model.EventStatus.Complete) {
      description = UserActionDescription.UPDATE_EVENT_MARK_COMPLETE;
      CacheProcessor.Instance().removeUserFromEvent(updatedEvent.id, userContext.userName);
    }

    if (input.status || input.preferredHypothesisId) {
      updatedEvent = produce<model.Event>(updatedEvent, draftState => {
        // Update the events status
        if (input.status) {
          draftState.status = input.status;
        }

        // Update the preferred hypothesis if provided in the input
        if (input.preferredHypothesisId) {
          description = UserActionDescription.UPDATE_EVENT_PREFERRED_HYP;
          draftState.preferredEventHypothesisHistory = [
            ...updatedEvent.preferredEventHypothesisHistory,
            {
              processingStageId: input.processingStageId,
              eventHypothesis: {
                id: input.preferredHypothesisId,
                rejected: false,
                modified: true,
                eventId: draftState.id,
                parentEventHypotheses: Object.seal([]),
                locationSolutionSets: Object.seal([]),
                preferredLocationSolution: undefined,
                associations: Object.seal([])
              }
            }
          ];
        }
      });
      logger.info(`Updating event ${eventId}. User: ${userContext.userName}`);

      // Call to set event in cache after modifying the event
      if (description !== UserActionDescription.UNKNOWN) {
        userContext.userCache.setEvent(description, updatedEvent);
      }

      const previouslyOpenEvent = previouslyOpenEventId
        ? userContext.userCache.getEventById(previouslyOpenEventId)
        : undefined;
      const signalDetectionsInConflictForPreviouslyOpenEvent = previouslyOpenEvent
        ? [...previouslyOpenEvent.associations]
            .map(association =>
              userContext.userCache.getSignalDetectionById(association.signalDetectionId)
            )
            .filter(sd => sd.hasConflict)
        : [];

      const signalDetectionsInConflictForOpenEvent = [
        ...userContext.userCache.getEventById(updatedEvent.id).associations
      ]
        .map(association =>
          userContext.userCache.getSignalDetectionById(association.signalDetectionId)
        )
        .filter(sd => sd.hasConflict);

      const signalDetections = uniqBy(
        [
          ...signalDetectionsInConflictForPreviouslyOpenEvent,
          ...signalDetectionsInConflictForOpenEvent
        ],
        'id'
      );

      return createDataPayload([updatedEvent], signalDetections, []);
    }

    // Throw an error if no updates were made (invalid input), since we don't want to publish a subscription callback
    // for no-op updates
    throw new Error(`No valid input provided to update event with ID: ${eventId}`);
  }

  /**
   * Creates and returns a new event hypothesis for a given event that is not
   * already marked as modified. If the event is already modified it returns the current
   * event hypothesis.
   * @param event the event to update and prepare
   *
   * @returns updated event
   */
  public prepareEventForModification(event: model.Event): model.Event {
    if (event.currentEventHypothesis.eventHypothesis.modified) {
      return event;
    }

    const newHypId = uuid4();
    const updatedPreferredEventHypothesis = produce<model.PreferredEventHypothesis>(
      event.currentEventHypothesis,
      draftState => {
        draftState.eventHypothesis.id = newHypId;
        draftState.eventHypothesis.modified = true;

        // Update location solution set
        draftState.eventHypothesis.locationSolutionSets.forEach((lss, index) => {
          draftState.eventHypothesis.locationSolutionSets[index].locationSolutions.forEach(
            (ls, idx) => {
              const oldUuid =
                draftState.eventHypothesis.locationSolutionSets[index].locationSolutions[idx].id;
              const newUuid = uuid4();
              draftState.eventHypothesis.locationSolutionSets[index].locationSolutions[
                idx
              ].id = newUuid;
              if (
                draftState.eventHypothesis.preferredLocationSolution.locationSolution.id === oldUuid
              ) {
                draftState.eventHypothesis.preferredLocationSolution.locationSolution =
                  draftState.eventHypothesis.locationSolutionSets[index].locationSolutions[idx];
              }
            }
          );
        });

        // remove previously rejected associations from the cloned event hypothesis to avoid
        // having a duplicate entry inserted into the rejectedSignalDetectionAssociations of the Event
        draftState.eventHypothesis.associations = draftState.eventHypothesis.associations.filter(
          assoc => !assoc.rejected
        );

        // create new association ids and update the event hypothesis id
        draftState.eventHypothesis.associations.forEach((assoc, index) => {
          draftState.eventHypothesis.associations[index].id = uuid4();
          draftState.eventHypothesis.associations[index].eventHypothesisId = newHypId;
        });
      }
    );

    const updatedEvent = produce<model.Event>(event, draftState => {
      draftState.hypotheses = Object.seal([
        ...event.hypotheses,
        updatedPreferredEventHypothesis.eventHypothesis
      ]);
      draftState.preferredEventHypothesisHistory = Object.seal([
        ...event.preferredEventHypothesisHistory,
        updatedPreferredEventHypothesis
      ]);
      draftState.currentEventHypothesis = updatedPreferredEventHypothesis;
    });
    return updatedEvent;
  }
  /**
   * Updates and event hypothesis with associations
   * @param event the event to update
   * @param associations the new associations
   *
   * @returns updated event
   */
  public updateEventHypothesisWithAssociations(
    event: model.Event,
    associations: model.SignalDetectionEventAssociation[]
  ): model.Event {
    const updatedEvent = produce<model.Event>(event, draftState => {
      draftState.currentEventHypothesis.eventHypothesis.associations = associations.map(assoc => ({
        ...assoc,
        eventHypothesisId: draftState.currentEventHypothesis.eventHypothesis.id
      }));
    });
    return updateEventHypothesis(updatedEvent);
  }

  /**
   * Updates the location behaviors from an association change
   * @param associationsToChange a list of associations to change
   * @param event the event that holds the location behaviors
   * @param signalDetections signal detections that are associated
   * @param associate whether the sds are being associated (true) or unassociated (false)
   *
   * @returns updated event
   */
  public updateLocationSolutionsFromAssociationChange(
    associationsToChange: model.SignalDetectionEventAssociation[],
    event: model.Event,
    signalDetections: SignalDetection[],
    associate: boolean
  ): model.Event {
    let updatedEvent = produce<model.Event>(event, draftState => {
      const updatedLocBehaviors = [];
      associationsToChange.forEach(assoc => {
        const sdHypo = SignalDetectionProcessor.Instance().getSignalDetectionHypothesisById(
          signalDetections,
          assoc.signalDetectionHypothesisId
        );
        // Update the location behaviors
        if (sdHypo) {
          // Add location behaviors to the event from preferred solution if it exists
          // Will be skipped when event is first created
          if (draftState.currentEventHypothesis.eventHypothesis.preferredLocationSolution) {
            const locBehaviors =
              event.currentEventHypothesis.eventHypothesis.preferredLocationSolution
                .locationSolution.locationBehaviors;
            updatedLocBehaviors.push(
              ...updateLocationBehaviors(event, sdHypo, locBehaviors, associate)
            );
          }
        }
      });
      // TODO can we avoid the uniqBy call here?
      // tslint:disable-next-line: max-line-length
      draftState.currentEventHypothesis.eventHypothesis.preferredLocationSolution.locationSolution.locationBehaviors = uniqBy(
        updatedLocBehaviors,
        'featureMeasurementId'
      );
    });

    updatedEvent = updateEventHypothesis(updatedEvent);

    return updatedEvent;
  }

  /**
   * Unassociated the given sd hypothesis from the given event hypothesis
   * @param event event to unassociate sds from
   * @param eventHypothesis the id of the event hypothesis to unassociated from
   * @param signalDetections the sds to be unassociated
   *
   * @returns collection of the updated event and signal detections that have been updated
   */
  public unassociateSignalDetections(
    event: model.Event,
    signalDetections: SignalDetection[]
  ): { event: model.Event; sds: SignalDetection[] } {
    if (!event) {
      return undefined;
    }

    // Prepare event for modification before updating the event
    const eventToUpdate = this.prepareEventForModification(event);

    const eventHypothesis = event.currentEventHypothesis.eventHypothesis;

    const sdHyps: SignalDetectionHypothesis[] = [];
    // Loop through the sds that are being unassociated
    signalDetections.forEach(sd => {
      // Get the signal detection hypothesis for the event
      const hyp = getSignalDetectionHypForEvent(sd, eventToUpdate.id);
      // Keep collection of all hyps
      sdHyps.push(hyp);
    });

    // Get a collection of associations that are to be removed
    const associationsToRemove: model.SignalDetectionEventAssociation[] = getAssociationsToRemove(
      sdHyps.map(hyp => hyp.id),
      eventHypothesis.associations
    );
    const associationsToKeep: model.SignalDetectionEventAssociation[] = eventHypothesis.associations.filter(
      assoc => !associationsToRemove.find(assocB => assocB.id === assoc.id)
    );

    // Get a collection of sds to publish checking first from the updated sds then from the originals
    const sdsToPublish = sdHyps.map(sdHyp =>
      signalDetections.find(sd => sd.id === sdHyp.parentSignalDetectionId)
    );

    // Update the event with rejected associations
    let updatedEvent = this.updateEventHypothesisWithAssociations(eventToUpdate, [
      ...associationsToRemove,
      ...associationsToKeep
    ]);

    // Update the location solutions as needed with removed associations
    updatedEvent = this.updateLocationSolutionsFromAssociationChange(
      associationsToRemove,
      updatedEvent,
      sdsToPublish,
      false
    );

    // Return updated event and sds
    return { event: updatedEvent, sds: sdsToPublish };
  }

  /**
   * Associate the given sd hypothesis to the given event hypothesis
   *
   * @param event the event to updated with associations
   * @param signalDetections signal detections that are being associated
   *
   * @returns collection of the updated event and signal detections that have been updated
   *
   */
  public associateSignalDetections(
    event: model.Event,
    signalDetections: SignalDetection[]
  ): { event: model.Event; sds: SignalDetection[] } {
    if (!event) {
      return undefined;
    }

    const { associations, updatedSds } = this.createAssociationsToAdd(event, signalDetections);
    // Prepare event for modification before updating the event
    const eventToUpdate = this.prepareEventForModification(event);

    // Find the event associated to the event hypo; this is being returned
    const updatedEvent = this.updateLocationSolutionsFromAssociationChange(
      associations,
      eventToUpdate,
      updatedSds,
      true
    );

    const allAssociations = [
      ...event.currentEventHypothesis.eventHypothesis.associations,
      ...associations
    ];

    const eventToReturn = this.updateEventHypothesisWithAssociations(updatedEvent, allAssociations);

    return { event: eventToReturn, sds: updatedSds };
  }

  /**
   * Retrieve the preferred event hypothesis for the provided processing stage and
   * event with the provided ID.
   * @param eventId The ID of the event to locate the preferred hypothesis for
   * @param stageId The ID of the processing stage to locate the preferred hypothesis for
   */
  public getPreferredHypothesisForStage(
    userContext: UserContext,
    eventId: string,
    stageId: string
  ): model.PreferredEventHypothesis {
    // Find the preferred hypothesis object with the provided stage ID for the event with the provided ID
    const event = userContext.userCache.getEventById(eventId);
    if (event) {
      const preferredForStageList = event.preferredEventHypothesisHistory.filter(
        peh => peh.processingStageId === stageId
      );
      const preferredForStage = preferredForStageList[preferredForStageList.length - 1];
      return preferredForStage;
    }
    return undefined;
  }

  /**
   * Add a new SignalDetectionHypothesis association to the event hypothesis.
   * @param userContext user context for current user
   * @param event event to update
   * @param newSignalDetectionHypoId updated sd hypothesis id
   * @param prevSignalDetectionHypoId previous sd hypothesis id
   *
   * @returns updated event
   */
  public updateSignalDetectionAssociation(
    userContext: UserContext,
    event: model.Event,
    newSignalDetectionHypoId: string,
    prevSignalDetectionHypoId: string
  ): model.Event[] {
    // Update the new SDHypo Id in each association
    const eventHypoList: model.EventHypothesis[] = getEventHyposAssocToSd(
      userContext.userCache.getEvents(),
      [prevSignalDetectionHypoId]
    );
    const updatedEvents: model.Event[] = [];
    eventHypoList.forEach(eventHyp => {
      if (eventHyp.eventId === event.id) {
        let updatedEvent = this.prepareEventForModification(event);
        const hypothesis = updatedEvent.currentEventHypothesis.eventHypothesis;

        const updatedHypothesis = produce<model.EventHypothesis>(hypothesis, draftState => {
          draftState.associations = draftState.associations.map(assoc => ({
            ...assoc,
            // update the associations for the modified signal detection hypothesis
            signalDetectionHypothesisId:
              assoc.signalDetectionHypothesisId === prevSignalDetectionHypoId
                ? newSignalDetectionHypoId
                : assoc.signalDetectionHypothesisId
          }));
        });

        updatedEvent = produce<model.Event>(updatedEvent, draftState => {
          draftState.currentEventHypothesis.eventHypothesis = updatedHypothesis;
          draftState.hypotheses = replaceByIdOrAddToList<model.EventHypothesis>(
            draftState.hypotheses,
            updatedHypothesis
          );
          draftState.preferredEventHypothesisHistory.forEach((preferred, index) => {
            if (preferred.eventHypothesis.id === updatedHypothesis.id) {
              draftState.preferredEventHypothesisHistory[index].eventHypothesis = updatedHypothesis;
            }
          });
        });
        updatedEvents.push(updatedEvent);
      }
    });
    return updatedEvents;
  }

  /**
   * Rejects the associations for the sd hyp that has been rejected
   * @param userEvents all of the users events
   * @param origSdHypId original signal detection hypothesis id
   * @param newSdHypId new signal detection hypothesis id
   *
   * @returns updated event
   */
  public rejectAssociationForSDHypothesis(
    userEvents: model.Event[],
    origSdHypId: string,
    newSdHypId: string
  ): model.Event[] {
    const eventHypsMap = getEventHyposAssocToSdMap(userEvents, [origSdHypId]);
    if (!eventHypsMap || eventHypsMap.size <= 0) {
      return [];
    }

    const eventsChanged: model.Event[] = [];
    eventHypsMap.forEach((eventHyp, eventId) => {
      const event = userEvents.find(ev => ev.id === eventId);
      let updatedEvent = this.prepareEventForModification(event);
      let updatedHypothesis = updatedEvent.currentEventHypothesis.eventHypothesis;
      const foundAssoc = updatedHypothesis.associations.find(
        assoc => assoc.signalDetectionHypothesisId === origSdHypId
      );
      if (foundAssoc) {
        const updatedAssociation = produce<model.SignalDetectionEventAssociation>(
          foundAssoc,
          draftState => {
            draftState.rejected = true;
            draftState.modified = true;
            draftState.signalDetectionHypothesisId = newSdHypId;
          }
        );
        updatedHypothesis = produce<model.EventHypothesis>(updatedHypothesis, draftState => {
          const assocIndex = draftState.associations.findIndex(
            assoc => assoc.signalDetectionHypothesisId === origSdHypId
          );
          if (assocIndex >= 0) {
            draftState.associations[assocIndex] = updatedAssociation;
          } else {
            draftState.associations.push(updatedAssociation);
          }
        });

        updatedEvent = produce<model.Event>(updatedEvent, draftState => {
          draftState.hypotheses = replaceByIdOrAddToList<model.EventHypothesis>(
            draftState.hypotheses,
            updatedHypothesis
          );
          draftState.currentEventHypothesis.eventHypothesis = updatedHypothesis;
        });

        eventsChanged.push(updatedEvent);
      }
    });
    return eventsChanged;
  }

  /**
   * Requests the Feature Predictions in the LocationSolutions be computed by
   * Signal Detection streaming service
   * @param Event to be processed
   *
   * @returns Event with updated feature predictions for each default phase
   */
  public async computeFeaturePredictions(
    userContext: UserContext,
    event: model.Event
  ): Promise<model.Event> {
    const currentPrefEventHypo = this.getCurrentEventHypothesisByEventId(userContext, event.id);
    // Lookup the sourceLocation
    const sourceLocationOSD: osdModel.LocationSolutionOSD = produce<osdModel.LocationSolutionOSD>(
      convertLocationSolutionToOSD(
        userContext,
        currentPrefEventHypo.eventHypothesis.preferredLocationSolution.locationSolution
      ),
      draftState => {
        draftState.networkMagnitudeSolutions = [];
      }
    );

    logger.debug(
      `Building FP Input using loc source: ${JSON.stringify(sourceLocationOSD, undefined, 2)}`
    );

    // Lookup all channels for default stations
    const channels: ProcessingChannel[] = convertOSDProcessingChannel(
      ProcessingStationProcessor.Instance().getDefaultChannels()
    );

    const featurePredictionInput: model.FeaturePredictionStreamingInput = {
      featureMeasurementTypes: [
        FeatureMeasurementTypeName.ARRIVAL_TIME,
        FeatureMeasurementTypeName.SLOWNESS,
        FeatureMeasurementTypeName.RECEIVER_TO_SOURCE_AZIMUTH
      ],
      sourceLocation: sourceLocationOSD,
      receiverLocations: channels,
      phase: PhaseType[PhaseType.P],
      model: 'ak135',
      corrections: []
    };

    // Call compute FP helper function
    const locationSolutions: model.LocationSolution[] = [];

    const computeFpPromises = this.defaultPhases.map(async sdPhase => {
      featurePredictionInput.phase = sdPhase;
      const requestConfig = this.settings.backend.services.computeFeaturePredictions.requestConfig;
      const locSolution = await getFeaturePredictionsFromService(
        featurePredictionInput,
        event.currentEventHypothesis.eventHypothesis,
        // tslint:disable-next-line: promise-function-async
        (ls, assocs) => this.convertLocationSolutionWrapper(userContext, ls, assocs),
        this.httpWrapper,
        requestConfig
      );
      if (locSolution) locationSolutions.push(locSolution);
    });

    await Promise.all(computeFpPromises);

    // Set all the FPs from each compute call into one FP list and set it on the preferred LS
    const fpList: model.FeaturePrediction[] = [];
    locationSolutions.forEach(ls => ls.featurePredictions.forEach(fp => fpList.push(fp)));
    const lssToUpdate = getLatestLSSForEventHyp(event.currentEventHypothesis.eventHypothesis);
    const updatedLocationSolutionSet = produce<model.LocationSolutionSet>(
      lssToUpdate,
      draftState => {
        draftState.locationSolutions = draftState.locationSolutions.map(ls => ({
          ...ls,
          featurePredictions: fpList
        }));
      }
    );
    const preferredLs = this.getPreferredLocation(lssToUpdate);
    const updatedPreferredLocationSolution = produce<model.LocationSolution>(preferredLs, draft => {
      draft.featurePredictions = fpList;
    });
    const updatedHypothesis = produce<model.EventHypothesis>(
      event.currentEventHypothesis.eventHypothesis,
      draftState => {
        draftState.preferredLocationSolution.locationSolution.featurePredictions = fpList;
        draftState.locationSolutionSets = replaceByIdOrAddToList<model.LocationSolutionSet>(
          draftState.locationSolutionSets,
          updatedLocationSolutionSet
        );
        draftState.preferredLocationSolution = {
          locationSolution: updatedPreferredLocationSolution
        };
      }
    );
    const updatedEvent = produce<model.Event>(event, draftState => {
      draftState.currentEventHypothesis.eventHypothesis = updatedHypothesis;
      draftState.hypotheses = replaceByIdOrAddToList<model.EventHypothesis>(
        draftState.hypotheses,
        updatedHypothesis
      );
    });

    return updatedEvent;
  }

  /**
   * Saves the provided events and signal detections
   *
   * @param userContext the user context
   * @param signalDetections the signal detections to save
   * @param events the events to save
   */
  public async save(
    userContext: UserContext,
    signalDetections: SignalDetection[],
    events: model.Event[]
  ): Promise<{ events: model.Event[]; signalDetections: SignalDetection[] }> {
    // save signal detections
    const savedSignalDetectionsResponse = await SignalDetectionProcessor.Instance().saveSignalDetections(
      userContext,
      signalDetections
    );

    // save events
    const savedEventsResponse = await this.saveEvents(userContext, events);

    // TODO undo/redo should this clear only the open event history?
    // TODO and what about global?
    // TODO only clear on successful save? - what does that look like?
    userContext.userCache.clearHistory();

    return {
      events: savedEventsResponse.events,
      signalDetections: savedSignalDetectionsResponse.signalDetections
    };
  }

  /**
   * Saves the provided events and signal detections
   *
   * @param userContext the user context
   */
  public async saveAll(
    userContext: UserContext
  ): Promise<{ events: model.Event[]; signalDetections: SignalDetection[] }> {
    const eventsToSave = this.getModifiedEvents(userContext);
    const signalDetectionsToSave = SignalDetectionProcessor.Instance().getModifiedSds(userContext);

    // save signal detections
    const savedSignalDetectionsResponse = await SignalDetectionProcessor.Instance().saveSignalDetections(
      userContext,
      signalDetectionsToSave
    );

    // save events
    const savedEventsResponse = await this.saveEvents(userContext, eventsToSave);

    // TODO undo/redo only clear on successful save? - what does that look like?
    userContext.userCache.clearHistory();

    return {
      events: savedEventsResponse.events,
      signalDetections: savedSignalDetectionsResponse.signalDetections
    };
  }

  /**
   * Saves the provided events.
   *
   * @param userContext the user context
   * @param events the events to save
   *
   * @returns the events saved and response
   */
  public async saveEvents(
    userContext: UserContext,
    events: model.Event[]
  ): Promise<{ events: model.Event[]; response: HttpResponse<SaveEventsServiceResponse> }> {
    // determined the modified events to save, no reason to save a non-modified event
    const eventsToSave = events.filter(
      event => event.currentEventHypothesis.eventHypothesis.modified
    );

    if (eventsToSave.length !== events.length) {
      logger.info(
        `The following events are not modified (skipping save): ` +
          `${String(
            events
              .filter(event => !event.currentEventHypothesis.eventHypothesis.modified)
              .map(event => event.id)
          )}`
      );
    }

    if (eventsToSave.length > 0) {
      // Call OSD endpoint to save events
      logger.info(`Saving ${eventsToSave.length} events`);
      const requestConfig = this.settings.backend.services.saveEvents.requestConfig;
      const response = await saveEventsToService(
        userContext,
        eventsToSave,
        this.httpWrapper,
        requestConfig
      );

      const httpOkay = 200;
      if (response.status === httpOkay) {
        // Save changes to Global Cache
        const resetEvents = this.resetModifiedFlags(userContext, eventsToSave);
        userContext.userCache.setEvents(UserActionDescription.SAVE_EVENT, resetEvents);
        userContext.userCache.commitEventsWithIds(resetEvents.map(e => e.id));
        return { events: resetEvents, response };
      }
    }
    logger.info(`No modified events to save`);
    return { events: [], response: undefined /* no response made */ };
  }

  /**
   * Updates an event hypothesis' location solution with an updated network magnitude solution based on the
   * given defining settings
   * @param userContext the user context
   * @param stationNames Stations for which the defining setting has changed
   * @param defining whether to set defining to true or false
   * @param magnitudeType the magnitude type to change defining settings for
   * @param eventHypothesisId the event hypothesis id to update
   */
  public async updateEventWithMagDefiningChanges(
    userContext: UserContext,
    stationNames: string[],
    defining: boolean,
    magnitudeType: model.MagnitudeType,
    eventHypothesisId: string
  ): Promise<model.Event> {
    const definingChanges: model.DefiningBehavior[] = stationNames.map(sName => ({
      stationName: sName,
      magnitudeType,
      defining
    }));

    const event = this.getEventByHypId(userContext, eventHypothesisId);
    const sds = getSignalDetectionsAssociatedToEvent(
      event,
      userContext.userCache.getSignalDetections()
    );

    const updatedLocationSolutions = await this.getUpdatedLocationSolutionsWithNewMagnitudes(
      userContext,
      event,
      sds,
      definingChanges,
      false
    );

    const eventHypothesis = this.getEventHypothesisById(userContext, eventHypothesisId);

    const updatedLatestLocationSolutionSet = produce<model.LocationSolutionSet>(
      getLatestLSSForEventHyp(eventHypothesis),
      draftState => {
        draftState.locationSolutions = updatedLocationSolutions;
      }
    );

    const updatedEventHypothesis = produce<model.EventHypothesis>(eventHypothesis, draftState => {
      draftState.preferredLocationSolution.locationSolution = this.getPreferredLocation(
        updatedLatestLocationSolutionSet
      );
      draftState.locationSolutionSets = replaceByIdOrAddToList<model.LocationSolutionSet>(
        draftState.locationSolutionSets,
        updatedLatestLocationSolutionSet
      );
    });

    const updatedEvent = updateEventCurrentHypothesis(event, updatedEventHypothesis);

    return updatedEvent;
  }

  /**
   * Creates location solutions with newly calculated magnitudes from the service
   * @param userContext the user context
   * @param event the event to compute mags for
   * @param sds the associated signal detections
   * @param definingChanges the user-set defining changes, if any
   * @returns Location Solutions with updates magnitudes
   */
  public async getUpdatedLocationSolutionsWithNewMagnitudes(
    userContext: UserContext,
    event: model.Event,
    sds: SignalDetection[],
    definingChanges: model.DefiningBehavior[],
    defaultDefiningSetting: boolean
  ): Promise<model.LocationSolution[]> {
    if (!event || !sds || sds.length < 1) {
      return [];
    }

    const magnitudeTypesToCompute = systemConfig.getCalculableMagnitudes(
      this.settings.backend.mock.enable
    );

    const defaultStations = ProcessingStationProcessor.Instance().getDefaultProcessingStations();
    const stationIdsForAllSdHypIds = new Map<string, string>();
    sds.forEach(sd => {
      stationIdsForAllSdHypIds.set(sd.currentHypothesis.id, sd.stationName);
    });
    const sdHyps = sds.map(sd => sd.currentHypothesis);

    const argsPerMagnitude = getNetworkMagClientArguments(
      event,
      definingChanges,
      magnitudeTypesToCompute,
      sdHyps,
      stationIdsForAllSdHypIds,
      defaultStations,
      systemConfig,
      defaultDefiningSetting
    );

    const requestConfig = this.settings;

    const locationIdToNewNetworkMagSolutions = new Map<string, model.NetworkMagnitudeSolution[]>();
    // Calls the mag solution service for each mag type
    const promises = magnitudeTypesToCompute.map(async magType => {
      const args = argsPerMagnitude.get(magType);
      // TODO - REMOVE This flag and the code branch it creates when mag service can accept all none defining
      const areAllNonDefining = args.definingDefs.reduce(
        (accum, def) => accum && !def.defining,
        true
      );
      const updatedLocationSolutions = areAllNonDefining
        ? await this.createUpdatedLocationSolutionWithNonDefiningMag(magType, event)
        : await computeNetworkMagnitudeSolutionsWithService(
            this.httpWrapper,
            requestConfig,
            convertEventHypothesisToOSD(userContext, args.eventHyp),
            args.definingDefs,
            args.sdHyps.map(convertSignalDetectionHypothesisToOSD),
            args.stationIdsForDetectionHypothesisIds,
            args.hypothesisIdToLocationMap,
            // tslint:disable-next-line: promise-function-async
            (ls, assocs) => this.convertLocationSolutionWrapper(userContext, ls, assocs)
          );
      updatedLocationSolutions.forEach(ls => {
        // If an entry exists, adds new magnitude to the entry's list, otherwise create new list
        const maybeExistingEntry = locationIdToNewNetworkMagSolutions.get(ls.id);
        const maybeMatchingNetworkMag = ls.networkMagnitudeSolutions.find(
          nms => nms.magnitudeType === magType
        );
        if (maybeMatchingNetworkMag) {
          if (maybeExistingEntry) {
            const newEntry = [maybeMatchingNetworkMag, ...maybeExistingEntry];
            locationIdToNewNetworkMagSolutions.set(ls.id, newEntry);
          } else {
            locationIdToNewNetworkMagSolutions.set(ls.id, [
              ls.networkMagnitudeSolutions.find(nms => nms.magnitudeType === magType)
            ]);
          }
        }
      });
    });
    await Promise.all(promises);

    // We clone a location solution set out of the event hypothesis so we can replace its network mag solutions
    const newLss = cloneDeep(getLatestLSSForEventHyp(event.currentEventHypothesis.eventHypothesis));
    newLss.locationSolutions.forEach(ls => {
      const maybeSolutions = locationIdToNewNetworkMagSolutions.get(ls.id);
      if (maybeSolutions) {
        maybeSolutions.forEach(nms => {
          const indexOfExisting = ls.networkMagnitudeSolutions.findIndex(
            sols => sols.magnitudeType === nms.magnitudeType
          );
          if (indexOfExisting >= 0) {
            ls.networkMagnitudeSolutions[indexOfExisting] = nms;
          } else {
            ls.networkMagnitudeSolutions.push(nms);
          }
        });
      }
    });
    return newLss.locationSolutions;
  }

  /**
   * Gets the preferred location solution based on the preferred location restraint order
   * @param locationSolutionSet the location solution set
   */
  public getPreferredLocation(
    locationSolutionSet: model.LocationSolutionSet
  ): model.LocationSolution {
    let pls: model.LocationSolution;
    this.preferredLocationSolutionRestraintOrder.forEach(depthRestraintString => {
      if (!pls) {
        pls = locationSolutionSet.locationSolutions.find(
          ls => ls.locationRestraint.depthRestraintType === depthRestraintString
        );
      }
    });
    return pls;
  }

  /**
   * Determine if any Events are in conflict.
   * @returns boolean
   */
  public areAnyEventsInConflict(userContext: UserContext): boolean {
    const events: model.Event[] = userContext.userCache.getEvents();
    return events.some(event => event.hasConflict);
  }

  /**
   * Returns all events that have been modified
   */
  public getModifiedEvents(userContext: UserContext): model.Event[] {
    return userContext.userCache
      .getEvents()
      .filter(evt => evt.currentEventHypothesis.eventHypothesis.modified);
  }

  /**
   * Updates the given event with the given location solutions
   * @param event the event to modify
   * @param locationSolutions the location solutions to add
   *
   * @returns the updated event
   */
  public updateEventWithLocationSolutions(
    event: model.Event,
    locationSolutions: model.LocationSolution[]
  ): model.Event {
    const eventHyp = event.currentEventHypothesis.eventHypothesis;
    // Create location solution set to add to cachedEventHypo
    const locationSolutionSet = createLocationSolutionSet(eventHyp, locationSolutions);

    // Set the preferred location solution and add the set
    const pls = this.getPreferredLocation(locationSolutionSet);

    const updatedEventHypothesis = produce<model.EventHypothesis>(eventHyp, draftState => {
      if (pls) {
        draftState.preferredLocationSolution.locationSolution = pls;
      }

      draftState.locationSolutionSets = [...eventHyp.locationSolutionSets, locationSolutionSet];
    });

    const updatedEvent = updateEventCurrentHypothesis(event, updatedEventHypothesis);

    return updatedEvent;
  }

  /**
   * Calculates the events to publish
   * @param signalDetections sds affected
   * @returns an DataPayload
   */
  public getEventsAndSdsAffectedByAssociations(
    events: model.Event[],
    signalDetections: SignalDetection[]
  ): DataPayload {
    const sds = signalDetections.filter(sd => sd.associations.length > 1);
    const affectedEvents = flatMap(
      sds.map(sd =>
        [...sd.associations].map(association => events.find(e => e.id === association.eventId))
      )
    ).filter(e => e !== undefined);
    return createDataPayload(uniqBy(affectedEvents, 'id'), sds, []);
  }

  /**
   * Creates a new signal detection with an initially hypothesis and time
   * feature measurement based on the provided input.
   * @param userContext user context for current user
   * @param input The input parameters used to create the new detection
   *
   * @returns data payload with updated or new sd and event
   */
  public createSignalDetection(userContext: UserContext, input: NewDetectionInput): DataPayload {
    // TODO: Fix this!
    // Using default channel for now don't know what is the right thing to do
    const channel = ProcessingStationProcessor.Instance().getDefaultChannelForStation(
      input.stationId
    );

    // create & store the hypothesis, detection & feature measurement
    const detection = createSignalDetection(
      input.stationId,
      input.phase as PhaseType,
      input.signalDetectionTiming.arrivalTime,
      channel,
      input.signalDetectionTiming.amplitudeMeasurement
    );

    const description = UserActionDescription.CREATE_DETECTION;

    if (input.eventId) {
      const { event, sds } = this.associateSignalDetections(
        userContext.userCache.getEventById(input.eventId),
        Object.seal([detection])
      );
      // Set data to user cache
      userContext.userCache.setEventsAndSignalDetections(description, [event], sds);
      return createDataPayload([event], [...sds], []);
    }
    // Set data to user cache
    userContext.userCache.setEventsAndSignalDetections(description, [], [detection]);
    return createDataPayload([], [detection], []);
  }

  /**
   * Updates the signal detection, using the provided UpdateDetectionInput object.
   * This function creates a new signal detection hypothesis and sets it to the 'current', reflecting the change.
   * @param userContext user context for current user
   * @param signalDetection signal detection to updated
   * @param input The UpdateDetectionInput object containing fields to update in the hypothesis
   * @param eventToUse event to update with (if it is associated)
   */
  public updateDetection(
    userContext: UserContext,
    signalDetection: SignalDetection,
    input: UpdateDetectionInput,
    eventToUse: model.Event
  ): { description: UserActionDescription; payload: DataPayload } {
    if (!input.phase && !input.signalDetectionTiming) {
      throw new Error(`No valid input provided to update detection with ID: ${signalDetection.id}`);
    }

    const openEventId = userContext.userCache.getOpenEventId();
    if (!eventToUse && openEventId && signalDetection.hasConflict) {
      // tslint:disable-next-line: max-line-length
      throw new Error(
        'Cannot update a detection that is in conflict and not associated to the currently open event'
      );
    }

    const originalHypothesis = signalDetection.currentHypothesis;

    const {
      sd,
      hypothesis
    } = SignalDetectionProcessor.Instance().createOrUpdateSignalDetectionHypothesis(
      signalDetection,
      eventToUse,
      false,
      input.phase,
      input.signalDetectionTiming
    );

    const updatedHypothesis = produce<SignalDetectionHypothesis>(hypothesis, draftState => {
      // mark the amplitude measurement as as `not reviewed`, since the value is being updated
      if (
        input.signalDetectionTiming &&
        input.signalDetectionTiming.amplitudeMeasurement &&
        input.signalDetectionTiming.amplitudeMeasurement.amplitude
      ) {
        draftState.reviewed.amplitudeMeasurement = false;
      }
    });

    const updatedDetection = produce<SignalDetection>(sd, draftState => {
      draftState.signalDetectionHypotheses = replaceByIdOrAddToList<SignalDetectionHypothesis>(
        draftState.signalDetectionHypotheses,
        updatedHypothesis
      );
    });

    const events = this.updateSignalDetectionAssociation(
      userContext,
      eventToUse,
      hypothesis.id,
      originalHypothesis.id
    );

    const description: UserActionDescription = input.phase
      ? UserActionDescription.UPDATE_DETECTION_RE_PHASE
      : input.signalDetectionTiming
      ? input.signalDetectionTiming.amplitudeMeasurement
        ? UserActionDescription.UPDATE_DETECTION_AMPLITUDE
        : UserActionDescription.UPDATE_DETECTION_RE_TIME
      : UserActionDescription.UPDATE_DETECTION;

    return {
      description,
      payload: createDataPayload(events, [updatedDetection], [])
    };
  }

  /**
   * Updates the collection of signal detections matching the provided list of unique IDs
   * using the provided UpdateDetectionInput object. A new hypothesis is created for each detection,
   * reflecting the updated content.
   * This function throws an error if no signal detection hypothesis exists for any of the provided IDs.
   * @param hypothesisIds The list of unique IDs identifying the signal detection hypothesis to update
   * @param input The UpdateDetectionInput object containing fields to update in the hypothesis
   */
  public updateDetections(
    userContext: UserContext,
    detectionIds: string[],
    input: UpdateDetectionInput
  ): { description: UserActionDescription; payload: DataPayload } {
    const events: Map<string, model.Event> = new Map<string, model.Event>();
    const sds: Map<string, SignalDetection> = new Map<string, SignalDetection>();
    let description: UserActionDescription;
    detectionIds.forEach(detectionId => {
      // Get signal detection and event for the detection id
      const { signalDetection, event } = userContext.userCache.getSignalDetectionAndEventBySdId(
        detectionId
      );
      const eventId = event ? event.id : undefined;
      // determine the event to use - if we have seen this event before (in the map) we want to use the already
      // edited event, rather than a fresh copy from the user cache
      const eventToUse = includes([...events.keys()], eventId) ? events.get(eventId) : event;
      // Update the detection and collect changes
      const changes = this.updateDetection(userContext, signalDetection, input, eventToUse);
      changes.payload.events.forEach(e => events.set(e.id, e));
      changes.payload.sds.forEach(sd => sds.set(sd.id, sd));
      description =
        detectionIds.length > 1 &&
        changes.description === UserActionDescription.UPDATE_DETECTION_RE_PHASE
          ? UserActionDescription.UPDATE_MULTIPLE_DETECTIONS_RE_PHASE
          : changes.description;
    });

    return {
      description,
      payload: createDataPayload([...events.values()], [...sds.values()], [])
    };
  }

  /**
   * Rejects the collection of signal detections matching the provided list of unique IDs
   * This function throws an error if no signal detection exists for any of the provided IDs.
   * @param userContext user context for current user
   * @param detectionIds The list of unique IDs identifying the signal detections to update
   *
   * @returns collection of events and signal detections that have been updated
   */
  public rejectDetections(
    userContext: UserContext,
    detectionIds: string[]
  ): { events: model.Event[]; sds: SignalDetection[] } {
    const updatedEvents: Map<string, model.Event> = new Map<string, model.Event>();
    const sds: Map<string, SignalDetection> = new Map<string, SignalDetection>();
    const allEvents: Map<string, model.Event> = new Map<string, model.Event>();
    userContext.userCache.getEvents().forEach(ev => allEvents.set(ev.id, ev));
    detectionIds.forEach(detectionId => {
      const { signalDetection, event } = userContext.userCache.getSignalDetectionAndEventBySdId(
        detectionId
      );
      const eventToUse = event
        ? includes([...updatedEvents.keys()], event.id)
          ? updatedEvents.get(event.id)
          : event
        : undefined;
      const sdCurHyp = signalDetection.currentHypothesis;
      // create a new signal detection hypothesis if not already modified;
      // otherwise just return the current hypothesis
      const {
        sd,
        hypothesis
      } = SignalDetectionProcessor.Instance().createOrUpdateSignalDetectionHypothesis(
        signalDetection,
        eventToUse,
        true
      );

      const changedEvents = this.rejectAssociationForSDHypothesis(
        [...allEvents.values()],
        sdCurHyp.id,
        hypothesis.id
      );

      changedEvents.forEach(e => {
        // Set event in the return map
        updatedEvents.set(e.id, e);
        // update event in all events list
        allEvents.set(e.id, e);
      });
      // set sd in the return map
      sds.set(sd.id, sd);
    });
    return { events: [...updatedEvents.values()], sds: [...sds.values()] };
  }

  /**
   * Initialize the event processor, setting up a mock backend if configured to do so.
   */
  private initialize(): void {
    logger.info(
      'Initializing the Event processor - Mock Enable: %s',
      this.settings.backend.mock.enable
    );

    // Override the OSD methods if in mock mode and set default phases for mock
    eventMockBackend.initialize(
      this.httpWrapper.createHttpMockWrapper(),
      this.settings.backend.mock.enable
    );
    // Retrieve default Phases from the config processor
    this.defaultPhases = ConfigProcessor.Instance().getConfigByKey('defaultSdPhases');

    if (!this.settings.backend.mock.enable) {
      this.defaultPhases = ['PKP', 'PKPbc', 'S', 'P', 'Pn', 'PcP', 'Sn']; // broken list: 'pP', LR', 'Lg',
    }

    // Retrieve the order of the DepthRestraintType for Preferred Location Solution
    this.preferredLocationSolutionRestraintOrder = ConfigProcessor.Instance().getConfigByKey(
      'preferredLocationSolutionRestraintOrder'
    );

    // TODO: Remove when Configuration service is truly up and running
    // If not found in the Config Processor
    if (!this.defaultPhases || this.defaultPhases.length < 1) {
      this.defaultPhases = ['P', 'Pn', 'PKP', 'PKPbc', 'PcP', 'pP', 'S', 'Sn', 'LR', 'Lg'];
      logger.warn(
        `Could not find any default Phases in` +
          ` config processors setting default list to ${String(this.defaultPhases)}`
      );
    }
  }

  /**
   * Resets all modified flags to false
   */
  private resetModifiedFlags(userContext: UserContext, events: model.Event[]): model.Event[] {
    return events.map(event => {
      if (event.currentEventHypothesis.eventHypothesis.modified) {
        // Reset all association modified flags
        const updatedAssociations = produce<model.SignalDetectionEventAssociation[]>(
          event.currentEventHypothesis.eventHypothesis.associations,
          draftState => {
            draftState.forEach((assoc, index) => {
              draftState[index].modified = false;
            });
          }
        );

        const updatedEvent = produce<model.Event>(event, draftState => {
          draftState.currentEventHypothesis.eventHypothesis.modified = false;
          draftState.currentEventHypothesis.eventHypothesis.associations = updatedAssociations;
        });
        return updatedEvent;
      }
    });
  }

  /**
   * Creates a list of new associations
   * @param eventHypothesisId the event hyp to associate to
   * @param signalDetectionHypoIds the sds to associate to the event
   */
  private createAssociationsToAdd(
    event: model.Event,
    signalDetections: SignalDetection[]
  ): { associations: model.SignalDetectionEventAssociation[]; updatedSds: SignalDetection[] } {
    const updatedSds = [];
    const associations = signalDetections.map(detection => {
      const maybeAssociation = detection.associations.find(
        association => association.eventId === event.id
      );
      const maybeAssociatedHypothesis = maybeAssociation
        ? detection.signalDetectionHypotheses.find(
            hyp => hyp.id === maybeAssociation.signalDetectionHypothesisId
          )
        : undefined;
      let sdHypo;
      if (maybeAssociatedHypothesis) {
        // Association already existed but was rejected, flip the rejection flag
        updatedSds.push(detection);
        return {
          id: maybeAssociation.associationId,
          eventHypothesisId: maybeAssociation.eventHypothesisId,
          rejected: false,
          signalDetectionHypothesisId: maybeAssociation.signalDetectionHypothesisId,
          modified: true
        };
      }
      const {
        sd,
        hypothesis
      } = SignalDetectionProcessor.Instance().createOrUpdateSignalDetectionHypothesis(
        detection,
        event
      );
      updatedSds.push(sd);
      sdHypo = hypothesis;

      return {
        id: uuid4(),
        eventHypothesisId: event.id, // !!!! Why are we setting the event id here; what is correct
        rejected: false,
        signalDetectionHypothesisId: sdHypo.id,
        modified: true
      };
    });

    return { associations, updatedSds };
  }

  /**
   * Gets events in the specified time range from the internal cache
   *
   * @param timeRange the time range
   */
  private readonly getEventsInTimeRangeFromCache = (
    userContext: UserContext,
    timeRange: TimeRange
  ) =>
    // Returns all events in cache that are in the provided time range
    userContext.userCache.getEvents().filter(event => {
      const eventTime =
        event.currentEventHypothesis.eventHypothesis.preferredLocationSolution.locationSolution
          .location.time;
      return eventTime >= timeRange.startTime && eventTime <= timeRange.endTime;
    })

  /**
   * Set Location for current event hyp for a newly created event
   * @param userContext user context for current user
   * @param event event to set location for
   * @param sdHypIds ids of sds that will be associated to the event
   *
   * @returns updated event
   */
  private async setNewEventHypothesisLocation(
    userContext: UserContext,
    event: model.Event,
    sdHypIds: string[]
  ): Promise<model.Event> {
    const allSds = userContext.userCache.getSignalDetections();
    const allStations = ProcessingStationProcessor.Instance().getDefaultProcessingStations();
    const eventLocation = getRandomLocationForNewHypothesis(
      sdHypIds.map(sdhId =>
        SignalDetectionProcessor.Instance().getSignalDetectionHypothesisById(allSds, sdhId)
      ),
      userContext.userCache.getTimeRange().startTime + 1,
      allSds,
      allStations
    );

    // Create the location solution and preferred location solution objects
    const locationSolution: model.LocationSolution = {
      id: uuid4(),
      featurePredictions: [],
      location: eventLocation,
      locationBehaviors: [],
      locationRestraint: {
        depthRestraintType: model.DepthRestraintType.UNRESTRAINED,
        depthRestraintKm: 0,
        latitudeRestraintType: model.RestraintType.UNRESTRAINED,
        latitudeRestraintDegrees: undefined,
        longitudeRestraintType: model.RestraintType.UNRESTRAINED,
        longitudeRestraintDegrees: undefined,
        timeRestraintType: model.RestraintType.UNRESTRAINED,
        timeRestraint: ''
      },
      snapshots: makeSignalDetectionSnapshots(
        event.currentEventHypothesis.eventHypothesis.associations,
        [],
        userContext.userCache.getSignalDetections(),
        ProcessingStationProcessor.Instance().getDefaultProcessingStations()
      ),
      networkMagnitudeSolutions: []
    };

    const preferredLocationSolution: model.PreferredLocationSolution = {
      locationSolution
    };

    // Set preferred location solution and add solution to the list
    // TODO Use Locate Event to create full solution set
    const updatedEvent = produce<model.Event>(event, draftState => {
      draftState.currentEventHypothesis.eventHypothesis.locationSolutionSets = [
        ...event.currentEventHypothesis.eventHypothesis.locationSolutionSets,
        createLocationSolutionSet(event.currentEventHypothesis.eventHypothesis, [locationSolution])
      ];
      draftState.currentEventHypothesis.eventHypothesis.preferredLocationSolution = preferredLocationSolution;
    });
    return updatedEvent;
  }

  /**
   * Wraps the location solution conversion to reduce circular dependencies
   * @param userContext the user context
   * @param ls the location solution to convert
   * @param assocs associations for the event the location solution is a part of
   */
  private async convertLocationSolutionWrapper(
    userContext: UserContext,
    ls: osdModel.LocationSolutionOSD,
    assocs: model.SignalDetectionEventAssociation[]
  ): Promise<model.LocationSolution> {
    return convertLocationSolutionFromOSD(
      ls,
      assocs,
      ProcessingStationProcessor.Instance().getDefaultProcessingStations(),
      userContext.userCache.getSignalDetections()
    );
  }

  /**
   * Computes a set of location solutions for a locate call
   * @param userContext the user context
   * @param osdEventHypo hypothesis to locate
   * @param locationBehaviors list of location behaviors for locate
   * @param signalDetections list of associated signal detections
   */
  private async computeLocationSolutions(
    userContext: UserContext,
    osdEventHypo: osdModel.EventHypothesisOSD,
    locationBehaviors: model.LocationBehavior[],
    signalDetections: SignalDetection[]
  ): Promise<model.LocationSolution[]> {
    const requestConfig = this.settings.backend.services.locateEvent.requestConfig;

    const locationSolutions = callLocateEvent(
      osdEventHypo,
      signalDetections,
      locationBehaviors,
      // tslint:disable-next-line: promise-function-async
      (ls, assocs) => this.convertLocationSolutionWrapper(userContext, ls, assocs),
      this.httpWrapper,
      requestConfig
    );
    return locationSolutions;
  }

  /**
   * Interim solution to computing magnitudes in osd mode when no stations are defining
   * @param magnitudeType the magnitude type
   * @param event arguments to the network mag call
   */
  private async createUpdatedLocationSolutionWithNonDefiningMag(
    magnitudeType: model.MagnitudeType,
    event: model.Event
  ): Promise<model.LocationSolution[]> {
    const locationSolutionsOld = getLatestLSSForEventHyp(
      event.currentEventHypothesis.eventHypothesis
    ).locationSolutions;
    const newLocSolutions = locationSolutionsOld.map(ls => {
      const maybeMagnitude = ls.networkMagnitudeSolutions.find(
        nms => nms.magnitudeType === magnitudeType
      );
      if (maybeMagnitude) {
        const newMag: model.NetworkMagnitudeSolution = {
          ...maybeMagnitude,
          magnitude: undefined,
          uncertainty: undefined,
          networkMagnitudeBehaviors: maybeMagnitude.networkMagnitudeBehaviors.map(nmb => ({
            ...nmb,
            defining: false
          }))
        };
        const newMags = [
          ...ls.networkMagnitudeSolutions.filter(nms => nms.magnitudeType !== magnitudeType),
          newMag
        ];
        return {
          ...ls,
          networkMagnitudeSolutions: newMags
        };
      }
      return ls;
    });
    return newLocSolutions;
  }
}
// tslint:disable-next-line: max-file-line-count
