import produce from 'immer';
import Immutable from 'immutable';
import flatMap from 'lodash/flatMap';
import includes from 'lodash/includes';
import { Association, TimeRange } from '../common/model';
import { Event, SignalDetectionEventAssociation } from '../event/model-and-schema/model';
import { gatewayLogger as logger } from '../log/gateway-logger';
import { SignalDetection, SignalDetectionHypothesis } from '../signal-detection/model';
import { UserProfile } from '../user-profile/model';
import {
  History,
  IUserCache,
  IUserCacheItem,
  NON_UNDO_ACTIONS,
  UserActionDescription,
  UserCacheData
} from './model';
import { UserCacheHistory } from './user-cache-history';

/**
 * The User Cache for a use
 */
export class UserCache implements IUserCache {
  /** the current time range */
  private currentTimeRange: TimeRange;

  /** the current opened event id */
  private openEventId: string;

  /** the events */
  private readonly events: IUserCacheItem<Event>;

  /** the signal detections */
  private readonly signalDetections: IUserCacheItem<SignalDetection>;

  /** the associations */
  private associations: Immutable.List<Association>;

  /** The user's profile */
  private userProfile: UserProfile;

  /** the user cache history */
  private readonly userCacheHistory: UserCacheHistory;

  /** Constructor */
  public constructor(
    currentTimeRange: TimeRange,
    currentOpenEventId: string,
    events: IUserCacheItem<Event>,
    signalDetections: IUserCacheItem<SignalDetection>
  ) {
    this.associations = Immutable.List();
    this.currentTimeRange = currentTimeRange;
    this.openEventId = currentOpenEventId;
    this.events = events;
    this.signalDetections = signalDetections;
    this.userCacheHistory = new UserCacheHistory();
    this.determineAssociations();
  }

  // ----- TimeRange Functions ------

  /**
   * Returns the time range for the user
   * @returns the time range
   */
  public getTimeRange(): TimeRange {
    return this.currentTimeRange;
  }

  /**
   * Sets time range for user
   * @param timeRange the new time range for the user
   */
  public setTimeRange(timeRange: TimeRange): void {
    this.currentTimeRange = timeRange;
  }

  // ----- User Profile Functions ------

  /**
   * Gets the user profile
   */
  public getUserProfile(): UserProfile {
    return this.userProfile;
  }

  /**
   * Sets the user's profile to the given value
   * @param userProfile the user profile
   */
  public setUserProfile(userProfile: UserProfile): void {
    this.userProfile = userProfile;
  }

  // ----- Event Functions ------

  /**
   * Returns the open event id for the user
   * @returns the open event id
   */
  public getOpenEventId(): string {
    return this.openEventId;
  }

  /**
   * Sets open event id for the user
   * @param id the new time range for the user
   */
  public setOpenEventId(id: string): void {
    this.openEventId = id;
  }

  /**
   * Get an event by id from the user cache.
   * @param id the unique id of the event
   * @returns the event
   */
  public getEventById(id: string): Event | undefined {
    const event = this.getInternalEventById(id);
    return event.currentEventHypothesis && event.currentEventHypothesis.eventHypothesis
      ? event
      : undefined;
  }

  /**
   * Get events from the event user cache.
   * @returns the events
   */
  public getEvents(): Event[] {
    const events = this.getAllEvents().filter(
      event => event && event.currentEventHypothesis && event.currentEventHypothesis.eventHypothesis
    );
    return events;
  }

  /**
   * Gets the events that have been invalidated from the user cache.
   * @returns the unique ids of the invalid events
   */
  public getInvalidEventIds(): string[] {
    return this.getAllEvents()
      .filter(
        event => !event.currentEventHypothesis || !event.currentEventHypothesis.eventHypothesis
      )
      .map(event => event.id);
  }

  /**
   * Set modified event to the user cache
   * @param description user action that caused the change
   * @param event the modified event
   */
  public setEvent(description: UserActionDescription, event: Event): void {
    this.setEvents(description, [event]);
  }

  /**
   * Set modified events to the user cache
   * @param description the user action that caused the change
   * @param events Modified events to add to cache
   */
  public setEvents(description: UserActionDescription, events: Event[]): void {
    logger.info(`saving events to user cache with open event id ${this.getOpenEventId()}`);
    this.setEventsAndSignalDetections(description, events, []);
  }

  /**
   * Updates the local cache with changes that have been made to the global cache.
   * @param events the global cache items that have changed
   * @param overwrite if true, the user cache will be overwritten with new data
   */
  public updateEventsFromGlobalCache(events: Event[], overwrite: boolean): void {
    this.events.updateFromGlobalCache(events, overwrite);
    this.determineAssociations();
  }

  /**
   * Commit a user's event cache to the global cache.
   */
  public commitAllEvents(): void {
    this.events.commitAll();
    this.determineAssociations();
  }

  /**
   * Commit a user's event cache to the global cache for the given event ids.
   * @param ids unique event ids
   */
  public commitEventsWithIds(ids: string[]): void {
    this.events.commitWithIds(ids);
    this.determineAssociations();
  }

  // ----- Signal Detection Functions ------

  /**
   * Get an signal detection by id from the user cache.
   * @param id the unique id of the event
   * @returns the signal detection
   */
  public getSignalDetectionById(id: string): SignalDetection | undefined {
    const signalDetection = this.getInternalSignalDetectionById(id);
    return signalDetection.signalDetectionHypotheses.length > 0 ? signalDetection : undefined;
  }

  /**
   * Get signal detections from the user cache.
   * @returns the signal detections
   */
  public getSignalDetections(): SignalDetection[] {
    const signalDetections = this.getAllSignalDetections().filter(
      signalDetection =>
        signalDetection &&
        signalDetection.signalDetectionHypotheses &&
        signalDetection.signalDetectionHypotheses.length > 0
    );
    return signalDetections;
  }

  /**
   * Gets the signal detections that have been invalidated from the user cache.
   * @returns the unique ids of the invalid signal detections
   */
  public getInvalidSignalDetectionIds(): string[] {
    return this.getAllSignalDetections()
      .filter(signalDetection => signalDetection.signalDetectionHypotheses.length < 1)
      .map(signalDetection => signalDetection.id);
  }

  /**
   * Set modified signal detection to the user cache
   * @param description the user action that caused the change
   * @param sd the modified signal detection
   */
  public setSignalDetection(description: UserActionDescription, sd: SignalDetection): void {
    this.setSignalDetections(description, [sd]);
  }

  /**
   * Set modified signal detections to the user cache
   * @param description the user action that caused the change
   * @param sds the modified signal detections
   */
  public setSignalDetections(description: UserActionDescription, sds: SignalDetection[]): void {
    logger.info(
      `saving signal detections to user cache with open event id ${this.getOpenEventId()}`
    );
    this.setEventsAndSignalDetections(description, [], sds);
  }

  /**
   * Updates the local cache with changes that have been made to the global cache.
   * @param sds the global cache items that have changed
   * @param overwrite if true, the user cache will be overwritten with new data
   */
  public updateSignalDetectionsFromGlobalCache(sds: SignalDetection[], overwrite: boolean): void {
    this.signalDetections.updateFromGlobalCache(sds, overwrite);
    this.determineAssociations();
  }

  /**
   * Commit a user's signal detection cache to the global cache.
   */
  public commitAllSignalDetections(): void {
    this.signalDetections.commitAll();
    this.determineAssociations();
  }

  /**
   * Commit a user's signal detection cache to the global cache for the given event ids.
   * @param ids unique signal detection ids
   */
  public commitSignalDetectionsWithIds(ids: string[]): void {
    this.signalDetections.commitWithIds(ids);
    this.determineAssociations();
  }

  // ----- Event and Signal Detection Functions ------

  /**
   * Returns an association mapping of the current events and signal detections.
   * @return the associations
   */
  public getAssociations(): Association[] {
    return this.associations.toArray();
  }

  /**
   * Returns the signal detection and event for the given signal detection id
   * @param id the unique signal detection id
   *
   * @return signal detection corresponding to the input id and event associated if applicable
   */
  public getSignalDetectionAndEventBySdId(id): { signalDetection: SignalDetection; event: Event } {
    const signalDetection = this.getSignalDetectionById(id);

    // check opened event with an associated signal detection
    const association = this.openEventId
      ? this.associations.find(a => a.eventId === this.openEventId && a.signalDetectionId === id)
      : undefined;
    if (association) {
      return { signalDetection, event: this.getEventById(association.eventId) };
    }

    // check no opened event and the signal detection only has one association
    const associationsForSd = this.associations.filter(a => a.signalDetectionId === id);
    if (associationsForSd.toArray().length === 1) {
      return { signalDetection, event: this.getEventById(associationsForSd.get(0).eventId) };
    }

    // no opened event and the signal detection has no association or more than one
    return { signalDetection, event: undefined };
  }

  /**
   * Sets the modified events and signal detections to the user cache
   * @param description the user action that caused the change
   * @param events the modified events
   * @param sds the modified signal detections
   */
  public setEventsAndSignalDetections(
    description: UserActionDescription,
    events: Event[],
    sds: SignalDetection[]
  ): void {
    const previousUserCacheData = {
      events: this.getAllEvents(),
      signalDetections: this.getAllSignalDetections()
    };

    // determine the events and signal detections that do not have a conflict prior to save
    const eventIdsWithNoConflicts = this.getAllEvents()
      .filter(e => !e.hasConflict)
      .map(e => e.id);
    const signalDetectionIdsWithNoConflicts = this.getAllSignalDetections()
      .filter(psd => !psd.hasConflict)
      .map(psd => psd.id);

    // update the user cache
    this.internalSetEvents(events);
    this.internalSetSignalDetections(sds);

    this.determineAssociations();

    const userCacheData = {
      events: this.getAllEvents(),
      signalDetections: this.getAllSignalDetections()
    };

    // update the references for the methods; i.e. associations, conflict, etc
    events.forEach((event, index) => (events[index] = this.getEventById(event.id)));
    sds.forEach((sd, index) => (sds[index] = this.getSignalDetectionById(sd.id)));

    // determine if a conflict was created as a result from the recent save
    const conflictCreated =
      events.find(ne => includes(eventIdsWithNoConflicts, ne.id) && ne.hasConflict) !== undefined ||
      sds.find(nsd => includes(signalDetectionIdsWithNoConflicts, nsd.id) && nsd.hasConflict) !==
        undefined;

    // Sets the action in user history
    // Only sets if the user action is undo-able
    if (!includes(NON_UNDO_ACTIONS, description)) {
      this.userCacheHistory.setHistory(
        description,
        events,
        sds,
        previousUserCacheData,
        userCacheData,
        conflictCreated
      );
    }
  }

  // ----- History (Undo/Redo) Functions ------

  /**
   * Returns the redo priority order number for a given history id.
   * @param id the unique history id
   */
  public getRedoPriorityOrder(id: string): number | undefined {
    return this.userCacheHistory.getRedoPriorityOrder(id);
  }

  /**
   * Returns the global history.
   */
  public getHistory(): History[] {
    return this.userCacheHistory.getHistory();
  }

  /**
   * Undo User Cache History, sets the history back in time for the the number of actions.
   * @param numberOfItems the number of actions (history) to undo
   * @returns the user cache data that was updated
   */
  public undoHistory(numberOfItems: number): UserCacheData {
    return this.undoRedoHistory(numberOfItems, 'undoHistory');
  }

  /**
   * Redo User Cache History, sets the history forward in time for the the number of actions.
   * @param numberOfItems the number of actions (history) to redo
   * @returns the user cache data that was updated
   */
  public redoHistory(numberOfItems: number): UserCacheData {
    return this.undoRedoHistory(numberOfItems, 'redoHistory');
  }

  /**
   * Undo User Cache History, sets the history back in time to the give history id.
   * @param id the unique history id
   * @returns the user cache data that was updated
   */
  public undoHistoryById(id: string): UserCacheData {
    return this.undoRedoHistoryById(id, 'undoHistoryById');
  }

  /**
   * Redo User Cache History, sets the history forward in time to the give history id.
   * @param id the unique history id
   * @returns the user cache data that was updated
   */
  public redoHistoryById(id: string): UserCacheData {
    return this.undoRedoHistoryById(id, 'redoHistoryById');
  }

  /**
   * Returns the event history for the given id.
   * @param id the unique open event id
   */
  public getEventHistory(id: string): History[] {
    return this.userCacheHistory.getEventHistory(id);
  }

  /**
   * Undo User Cache Event History, sets the history back in time for the open event
   * and for the number of actions.
   * @param numberOfItems the number of actions (history) to undo
   * @returns the user cache data that was updated
   */
  public undoEventHistory(numberOfItems: number): UserCacheData {
    return this.undoRedoEventHistory(numberOfItems, 'undoEventHistory');
  }

  /**
   * Redo User Cache Event History, sets the history forward in time for the open event
   * and for the number of actions.
   * @param numberOfItems the number of actions (history) to redo
   * @returns the user cache data that was updated
   */
  public redoEventHistory(numberOfItems: number): UserCacheData {
    return this.undoRedoEventHistory(numberOfItems, 'redoEventHistory');
  }

  /**
   * Undo User Cache Event History, sets the history back in time to the give history id.
   * @param id the unique history id
   * @returns the user cache data that was updated
   */
  public undoEventHistoryById(id: string): UserCacheData {
    return this.undoRedoEventHistoryById(id, 'undoEventHistoryById');
  }

  /**
   * Redo User Cache Event History, sets the history forward in time to the give history id.
   * @param id the unique history id
   * @returns the user cache data that was updated
   */
  public redoEventHistoryById(id: string): UserCacheData {
    return this.undoRedoEventHistoryById(id, 'redoEventHistoryById');
  }

  /**
   * Clears all of the user history (both global and event).
   */
  public clearHistory(): void {
    this.userCacheHistory.clearHistory();
  }

  // ----- Private Functions ------

  /**
   * Get an event by id from the user cache.
   * @param id the unique id of the event
   * @returns the event
   */
  private getInternalEventById(id: string): Event {
    return this.events.has(id)
      ? {
          ...this.events.get(id),
          associations: this.associations
            .filter(association => association.eventId === id)
            .toArray(),
          signalDetectionIds: this.associations
            .filter(association => association.eventId === id)
            .map(association => association.signalDetectionId)
            .toArray(),
          hasConflict: this.isEventInConflict(this.events.get(id), this.associations.toArray())
        }
      : undefined;
  }

  /**
   * Get all events from the user cache. (including ones with no hypothesis)
   * @returns the signal detections
   */
  private getAllEvents(): Event[] {
    return this.events
      .getAll()
      .filter(event => event !== undefined)
      .map(event => this.getInternalEventById(event.id));
  }

  /**
   * Get an signal detection by id from the user cache.
   * @param id the unique id of the event
   * @returns the signal detection
   */
  private getInternalSignalDetectionById(id: string): SignalDetection {
    const { signalDetection, event } = this.getInternalSignalDetectionAndEventBySdId(id);
    return signalDetection
      ? {
          ...this.signalDetections.get(id),
          currentHypothesis: this.getSignalDetectionHypothesisToUse(signalDetection, event),
          associations: this.associations
            .filter(association => association.signalDetectionId === id)
            .toArray(),
          hasConflict: this.isSignalDetectionInConflict(
            this.signalDetections.get(id),
            this.associations.toArray()
          )
        }
      : undefined;
  }

  /**
   * Get all signal detections from the user cache. (including ones with no hypothesis)
   * @returns the signal detections
   */
  private getAllSignalDetections(): SignalDetection[] {
    return this.signalDetections
      .getAll()
      .filter(sd => sd !== undefined)
      .map(sd => this.getInternalSignalDetectionById(sd.id));
  }

  /**
   * Returns the signal detection and event for the given signal detection id
   * @param id the unique signal detection id
   *
   * @return signal detection corresponding to the input id and event associated if applicable
   */
  private getInternalSignalDetectionAndEventBySdId(
    id
  ): { signalDetection: SignalDetection; event: Event } {
    const signalDetection = this.signalDetections.get(id);

    // check opened event with an associated signal detection
    const association = this.openEventId
      ? this.associations.find(
          a => !a.rejected && a.eventId === this.openEventId && a.signalDetectionId === id
        )
      : undefined;
    if (association) {
      return { signalDetection, event: this.events.get(association.eventId) };
    }

    // check no opened event and the signal detection only has one association
    const associationsForSd = this.associations.filter(
      a => !a.rejected && a.signalDetectionId === id
    );
    if (associationsForSd.toArray().length === 1) {
      return { signalDetection, event: this.events.get(associationsForSd.get(0).eventId) };
    }

    // no opened event and the signal detection has no association or more than one
    return { signalDetection, event: undefined };
  }

  /**
   * Sets the modified signal detections to the user cache.
   * Clears out any of the runtime functions before saving.
   * @param signalDetections the signal detections to save
   */
  private internalSetSignalDetections(signalDetections: SignalDetection[]): void {
    signalDetections.forEach(sd => {
      this.signalDetections.set(
        produce<SignalDetection>(sd, draftState => {
          // clear out all of the defined functions before save
          draftState.currentHypothesis = undefined;
          draftState.associations = undefined;
          draftState.hasConflict = undefined;
        })
      );
    });
  }

  /**
   * Sets the modified events to the user cache.
   * Clears out any of the runtime functions before saving.
   * @param events the events to save
   */
  private internalSetEvents(events: Event[]): void {
    events.forEach(event => {
      this.events.set(
        produce<Event>(event, draftState => {
          // clear out all of the defined functions before save
          draftState.associations = undefined;
          draftState.hasConflict = undefined;
        })
      );
    });
  }

  /**
   * Returns a list of association groupings for the events and signal detections.
   */
  private determineAssociations(): void {
    const events = this.getEvents();
    const signalDetections = this.getSignalDetections();

    const findSignalDetectionId = (association: SignalDetectionEventAssociation) => {
      const signalDetection = signalDetections.find(sd =>
        includes(
          [...sd.signalDetectionHypotheses.map(hyp => hyp.id)],
          association.signalDetectionHypothesisId
        )
      );
      return signalDetection ? signalDetection.id : undefined;
    };

    this.associations = Immutable.List(
      flatMap<Association>(
        events
          .filter(
            event => event.currentEventHypothesis && event.currentEventHypothesis.eventHypothesis
          )
          .map(event =>
            event.currentEventHypothesis.eventHypothesis.associations
              .map(association => ({
                eventId: event.id,
                signalDetectionId: findSignalDetectionId(association),
                associationId: association.id,
                eventHypothesisId: association.eventHypothesisId,
                signalDetectionHypothesisId: association.signalDetectionHypothesisId,
                rejected: association.rejected
              }))
              .filter(
                association =>
                  association.associationId &&
                  association.eventId &&
                  association.eventHypothesisId &&
                  association.signalDetectionId &&
                  association.signalDetectionHypothesisId
              )
          )
      )
    );
  }

  /**
   * Undo or Redo User Cache History, sets the history forward or backward in time for the open event
   * and for the number of actions.
   * @param numberOfItems the number of actions (history) to undo/redo
   * @param action the action to be preformed (undo or redo)
   * @returns the user cache data that was updated
   */
  private undoRedoHistory(
    numberOfItems: number,
    action: 'redoHistory' | 'undoHistory'
  ): UserCacheData {
    // tslint:disable-next-line: no-string-literal
    const updatedUserCacheData = this.userCacheHistory[action](numberOfItems, {
      events: this.getAllEvents(),
      signalDetections: this.getAllSignalDetections()
    });
    return this.updateUserCacheAfterUndoRedo(updatedUserCacheData);
  }

  /**
   * Undo or Redo User Cache History, sets the history forward or backward in time to the given history id.
   * @param id the unique history id
   * @param action the action to be preformed (undo or redo)
   * @returns the user cache data that was updated
   */
  private undoRedoHistoryById(
    id: string,
    action: 'redoHistoryById' | 'undoHistoryById'
  ): UserCacheData {
    // tslint:disable-next-line: no-string-literal
    const updatedUserCacheData = this.userCacheHistory[action](id, {
      events: this.getAllEvents(),
      signalDetections: this.getAllSignalDetections()
    });
    return this.updateUserCacheAfterUndoRedo(updatedUserCacheData);
  }

  /**
   * Undo or Redo User Cache Event History, sets the history forward or backward in time for the open event
   * and for the number of actions.
   * @param numberOfItems the number of actions (history) to undo/redo
   * @param action the action to be preformed (undo or redo)
   * @returns the user cache data that was updated
   */
  private undoRedoEventHistory(
    numberOfItems: number,
    action: 'redoEventHistory' | 'undoEventHistory'
  ): UserCacheData {
    if (this.getOpenEventId()) {
      // tslint:disable-next-line: no-string-literal
      const updatedUserCacheData = this.userCacheHistory[action](
        this.getOpenEventId(),
        numberOfItems,
        { events: this.getAllEvents(), signalDetections: this.getAllSignalDetections() }
      );
      return this.updateUserCacheAfterUndoRedo(updatedUserCacheData);
    }
    return undefined;
  }

  /**
   * Undo or Redo User Cache Event History, sets the history forward or backward in time for the open event
   * and for the number of actions.
   * @param id the unique history id
   * @param action the action to be preformed (undo or redo)
   * @returns the user cache data that was updated
   */
  private undoRedoEventHistoryById(
    id: string,
    action: 'redoEventHistoryById' | 'undoEventHistoryById'
  ): UserCacheData {
    if (this.getOpenEventId()) {
      // tslint:disable-next-line: no-string-literal
      const updatedUserCacheData = this.userCacheHistory[action](this.getOpenEventId(), id, {
        events: this.getAllEvents(),
        signalDetections: this.getAllSignalDetections()
      });
      return this.updateUserCacheAfterUndoRedo(updatedUserCacheData);
    }
    return undefined;
  }

  /**
   * Update the user cache after a undo or redo action.
   * @param userCacheData the user cache data to update
   */
  private updateUserCacheAfterUndoRedo(userCacheData: UserCacheData): UserCacheData {
    // update the user cache; rewrite the cache
    if (userCacheData) {
      this.internalSetEvents(userCacheData.events);
      this.internalSetSignalDetections(userCacheData.signalDetections);

      this.determineAssociations();

      // TODO figure out a better way to solve this so that we do not have to save twice
      // fixes the event association based on the user cache and the calculated associations
      const updatedUserCacheData = produce<UserCacheData>(userCacheData, draftState => {
        draftState.events.forEach((event, eventIndex) => {
          if (event.currentEventHypothesis && event.currentEventHypothesis.eventHypothesis) {
            draftState.events[
              eventIndex
              // tslint:disable-next-line: max-line-length
            ].currentEventHypothesis.eventHypothesis.associations = event.currentEventHypothesis.eventHypothesis.associations.filter(
              association =>
                includes(
                  this.associations.toArray().map(a => a.associationId),
                  association.id
                )
            );
          }
        });
      });

      this.internalSetEvents(updatedUserCacheData.events);
      this.internalSetSignalDetections(updatedUserCacheData.signalDetections);

      return {
        // update the references for the methods; i.e. associations, conflict, etc
        events: updatedUserCacheData.events
          .map(event => this.getEventById(event.id))
          .filter(event => event !== undefined),
        signalDetections: updatedUserCacheData.signalDetections
          .map(sd => this.getSignalDetectionById(sd.id))
          .filter(signalDetection => signalDetection !== undefined)
      };
    }
    return undefined;
  }

  /**
   * Returns the signal detections in conflict.
   * @param associations the associations
   */
  private signalDetectionsInConflict(associations: Association[]): SignalDetection[] {
    const sds = associations
      .filter(association => !association.rejected)
      .map(association => association.signalDetectionId);
    return sds
      .filter(sdId => sds.filter(id => sdId === id).length > 1)
      .map(id => this.signalDetections.get(id));
  }

  /**
   * Returns the events in conflict.
   * @param associations the associations
   */
  private eventsInConflict(associations: Association[]): Event[] {
    const signalDetectionIdsInConflict = this.signalDetectionsInConflict(associations).map(
      sd => sd.id
    );
    return associations
      .filter(association => !association.rejected)
      .filter(association => includes(signalDetectionIdsInConflict, association.signalDetectionId))
      .map(association => this.events.get(association.eventId));
  }

  /**
   * Returns true if the event is in conflict; false otherwise.
   * @param event the event to check
   * @param associations the associations
   */
  private isEventInConflict(event: Event, associations: Association[]): boolean {
    return event
      ? this.eventsInConflict(associations).find(e => e.id === event.id) !== undefined
      : false;
  }

  /**
   * Returns true if the signal detection is in conflict; false otherwise.
   * @param signalDetection the signal detection to check
   * @param associations the associations
   */
  private isSignalDetectionInConflict(
    signalDetection: SignalDetection,
    associations: Association[]
  ): boolean {
    return (
      this.signalDetectionsInConflict(associations).find(sd => sd.id === signalDetection.id) !==
      undefined
    );
  }

  /**
   * Returns the signal detection hypothesis to use
   * @param sd sd to get hypothesis for
   * @param event event associated to sd
   *
   * @return signal detection hypothesis for the sd corresponding to the event
   */
  private getSignalDetectionHypothesisToUse(
    sd: SignalDetection,
    event: Event
  ): SignalDetectionHypothesis {
    if (!sd || !sd.signalDetectionHypotheses || sd.signalDetectionHypotheses.length < 1) {
      return undefined;
    }

    const association = event
      ? this.associations.find(
          a => !a.rejected && a.eventId === event.id && a.signalDetectionId === sd.id
        )
      : undefined;

    if (association) {
      // return the hypothesis associated to the event
      return sd.signalDetectionHypotheses.find(
        hyp => hyp.id === association.signalDetectionHypothesisId
      );
    }

    // else return the most recent hypothesis.
    return sd.signalDetectionHypotheses[sd.signalDetectionHypotheses.length - 1];
  }
}
