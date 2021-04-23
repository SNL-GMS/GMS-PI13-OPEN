import { KeyValue, uuid4 } from '@gms/common-util';
import produce from 'immer';
import Immutable from 'immutable';
import flatMap from 'lodash/flatMap';
import includes from 'lodash/includes';
import isEqual from 'lodash/isEqual';
import uniq from 'lodash/uniq';
import { Event, EventHypothesis } from '../event/model-and-schema/model';
import { gatewayLogger as logger } from '../log/gateway-logger';
import { SignalDetection, SignalDetectionHypothesis } from '../signal-detection/model';
import { HistoryItem } from './history-item';
import {
  History,
  HistoryChange,
  HypothesisType,
  UserActionDescription,
  UserCacheData
} from './model';
import {
  userActionCreatorForEventHypothesisChange,
  userActionCreatorForSignalDetectionHypothesisChange
} from './user-action';

/**
 * Maintains the history of the User Cache
 */
export class UserCacheHistory {
  /** the event hypothesis history */
  private eventHypothesisHistory: Immutable.Map<string, HistoryItem<EventHypothesis>>;

  /** the signal detection hypothesis history */
  private signalDetectionHypothesisHistory: Immutable.Map<
    string,
    HistoryItem<SignalDetectionHypothesis>
  >;

  /** the history */
  private history: Immutable.List<History>;

  /** the redo priority order stack - used to redo history in the same order is was undone */
  private redoPriorityOrder: Immutable.List<string>;

  /** Constructor */
  public constructor() {
    this.clearHistory();
  }

  /**
   * Sets the history.
   * @param description the user action that caused the change
   * @param events the events that changed
   * @param signalDetections the signal detections that changed
   * @param previousUserCacheData the user cache data (previous state)
   * @param userCacheData the user cache data (current state)
   * @param conflictCreated true if a conflict was created; false otherwise
   */
  public setHistory(
    description: UserActionDescription,
    events: Event[],
    signalDetections: SignalDetection[],
    previousUserCacheData: UserCacheData,
    userCacheData: UserCacheData,
    conflictCreated: boolean
  ): void {
    let changes: Immutable.List<HistoryChange> = Immutable.List();

    // add an entry for each event that has changed
    events.forEach(event => {
      const hypothesis = event.currentEventHypothesis.eventHypothesis;

      const originalHypothesis = previousUserCacheData.events.find(e => e.id === event.id)
        ? previousUserCacheData.events.find(e => e.id === event.id).currentEventHypothesis
            .eventHypothesis
        : undefined;

      // If this hypothesis has not been added to the history it needs to be added
      if (!this.eventHypothesisHistory.has(hypothesis.id)) {
        // Create a temporary version of the original hypothesis and copy in the updated
        // hypothesis id - this represents the 'starting point' for the new hypothesis
        // If there wasn't an original hypothesis the starting point is undefined - aka creating an event
        const temp = originalHypothesis
          ? produce<EventHypothesis>(originalHypothesis, draftState => {
              draftState.id = hypothesis.id;
            })
          : undefined;
        // Set the updated hypothesis in the history
        this.eventHypothesisHistory = this.eventHypothesisHistory.set(
          hypothesis.id,
          new HistoryItem(temp)
        );
      }

      // update the event hypothesis history
      this.eventHypothesisHistory.get(hypothesis.id).add(hypothesis);

      const userAction = userActionCreatorForEventHypothesisChange(
        description,
        originalHypothesis,
        hypothesis
      );

      changes = changes.push({
        id: uuid4(),
        active: true,
        eventId: event.id,
        conflictCreated,
        hypothesisChangeInformation: {
          id: uuid4(),
          hypothesisId: hypothesis.id,
          type: HypothesisType.EventHypothesis,
          parentId: event.id,
          userAction
        }
      });
    });

    // map the signal detections to the appropriate event or mark it as unassociated
    signalDetections.forEach(signalDetection => {
      const sdHypId = this.getSDHypIdToUse(
        description,
        signalDetection,
        userCacheData.signalDetections,
        events
      );
      const hypothesis = signalDetection.signalDetectionHypotheses.find(hyp => hyp.id === sdHypId);

      const originalHypothesis = previousUserCacheData.signalDetections.find(
        sd => sd.id === signalDetection.id
      )
        ? previousUserCacheData.signalDetections.find(sd => sd.id === signalDetection.id)
            .currentHypothesis
        : undefined;

      // If this hypothesis has not been added to the history it needs to be addeduuid4()
      if (!this.signalDetectionHypothesisHistory.has(hypothesis.id)) {
        // Create a temporary version of the original hypothesis and copy in the updated
        // hypothesis id - this represents the 'starting point' for the new hypothesis
        // If there wasn't an original hypothesis (new detection) or the original hypothesis
        // is modified (already a new hyp) the starting point is undefined
        const temp =
          originalHypothesis && !originalHypothesis.modified
            ? produce<SignalDetectionHypothesis>(originalHypothesis, draftState => {
                draftState.id = hypothesis.id;
              })
            : undefined;
        // Set the updated hypothesis in the history
        this.signalDetectionHypothesisHistory = this.signalDetectionHypothesisHistory.set(
          hypothesis.id,
          new HistoryItem(temp)
        );
      }

      // update the signal detection hypothesis history
      this.signalDetectionHypothesisHistory.get(hypothesis.id).add(hypothesis);

      const theId = this.getEventIdForChange(description, signalDetection, sdHypId);

      const userAction = userActionCreatorForSignalDetectionHypothesisChange(
        description,
        signalDetection,
        originalHypothesis,
        hypothesis
      );

      changes = changes.push({
        id: uuid4(),
        active: true,
        eventId: theId,
        conflictCreated,
        hypothesisChangeInformation: {
          id: uuid4(),
          hypothesisId: hypothesis.id,
          type: HypothesisType.SignalDetectionHypothesis,
          parentId: signalDetection.id,
          userAction
        }
      });
    });

    // remove any history that is no longer active
    const eventIds = uniq(changes.toArray().map(change => change.eventId));
    const isGlobal = eventIds.length > 1 || eventIds.some(id => id === undefined);

    this.history.forEach((value, key) => {
      this.history = this.history.set(key, {
        ...value,
        // remove the changes that are inactive and match the event id (unless a global change)
        changes:
          isGlobal && value.changes.some(change => !change.active)
            ? Immutable.List()
            : value.changes.filter(change => change.active || !includes(eventIds, change.eventId))
      });
    });
    // filter out any of the history that no longer has an active change
    this.history = Immutable.List(this.history.filter(value => value.changes.size > 0));

    this.history = this.history.push({
      id: uuid4(),
      description,
      changes
    });
  }

  /**
   * Returns the redo priority order number for a given history id.
   * @param id the unique history id
   */
  public getRedoPriorityOrder(id: string): number | undefined {
    const index: number = this.redoPriorityOrder.findIndex(item => item === id);
    // with an array we want to return the opposite of the index - last item in list should priority 1
    return index >= 0 ? this.redoPriorityOrder.size - index : undefined;
  }

  // ----- Global History Functions ------

  /**
   * Returns the global history.
   */
  public getHistory(): History[] {
    // tslint:disable-next-line: no-empty
    return produce<History[]>(this.history.toArray(), draftState => {});
  }

  /**
   * Undo History, sets the history back in time for the number of actions.
   * @param numberOfItems the number of actions (history) to undo
   * @param userCacheData the user cache data (current state)
   * @returns the user cache data that was updated
   */
  public undoHistory(numberOfItems: number, userCacheData: UserCacheData): UserCacheData {
    return this.undoRedoHistory(numberOfItems, undefined, 'undo', userCacheData);
  }

  /**
   * Redo History, sets the history forward in time for the number of actions.
   * @param numberOfItems the number of actions (history) to redo
   * @param userCacheData the user cache data (current state)
   * @returns the user cache data that was updated
   */
  public redoHistory(numberOfItems: number, userCacheData: UserCacheData): UserCacheData {
    return this.undoRedoHistory(numberOfItems, undefined, 'redo', userCacheData);
  }

  /**
   * Undo History by id, sets the history back in time to the given id.
   * @param id the unique history id
   * @param userCacheData the user cache data (current state)
   * @returns the user cache data that was updated
   */
  public undoHistoryById(id: string, userCacheData: UserCacheData): UserCacheData {
    return this.undoRedoHistory(undefined, id, 'undo', userCacheData);
  }

  /**
   * Redo History by id, sets the history back in time to the given id.
   * @param id the unique history id
   * @param userCacheData the user cache data (current state)
   * @returns the user cache data that was updated
   */
  public redoHistoryById(id: string, userCacheData: UserCacheData): UserCacheData {
    return this.undoRedoHistory(undefined, id, 'redo', userCacheData);
  }

  // ----- Event History Functions ------

  /**
   * Returns the event history for the given id.
   * @param id the unique open event id
   */
  public getEventHistory(id: string): History[] {
    let eventHistory = this.history.filter(h => h.changes.some(value => value.eventId === id));
    eventHistory.forEach((value, key) => {
      eventHistory = eventHistory.set(key, {
        ...value,
        // remove any of the changes that aren't part of the event
        changes: value.changes.filter(change => change.eventId === id)
      });
    });
    // tslint:disable-next-line: no-empty
    return produce<History[]>(eventHistory.toArray(), draftState => {});
  }

  /**
   * Undo Event History, sets the history forward or backward in time for the open event
   * and for the number of actions.
   * @param id the unique event id
   * @param numberOfItems the number of actions (history) to undo
   * @param userCacheData the user cache data
   * @returns the user cache data that was updated
   */
  public undoEventHistory(
    id: string,
    numberOfItems: number,
    userCacheData: UserCacheData
  ): UserCacheData {
    return this.undoRedoEventHistory(id, numberOfItems, undefined, 'undo', userCacheData);
  }

  /**
   * Redo Event History, sets the history forward or backward in time for the open event
   * and for the number of actions.
   * @param id the unique event id
   * @param numberOfItems the number of actions (history) to redo
   * @param userCacheData the user cache data
   * @returns the user cache data that was updated
   */
  public redoEventHistory(
    id: string,
    numberOfItems: number,
    userCacheData: UserCacheData
  ): UserCacheData {
    return this.undoRedoEventHistory(id, numberOfItems, undefined, 'redo', userCacheData);
  }

  /**
   * Undo Event History, sets the history forward or backward in time for the open event
   * and for the number of actions.
   * @param id the unique event id
   * @param numberOfItems the number of actions (history) to undo
   * @param userCacheData the user cache data
   * @returns the user cache data that was updated
   */
  public undoEventHistoryById(
    id: string,
    historyId: string,
    userCacheData: UserCacheData
  ): UserCacheData {
    return this.undoRedoEventHistory(id, undefined, historyId, 'undo', userCacheData);
  }

  /**
   * Redo Event History, sets the history forward or backward in time for the open event
   * and for the number of actions.
   * @param id the unique event id
   * @param numberOfItems the number of actions (history) to redo
   * @param userCacheData the user cache data
   * @returns the user cache data that was updated
   */
  public redoEventHistoryById(
    id: string,
    historyId: string,
    userCacheData: UserCacheData
  ): UserCacheData {
    return this.undoRedoEventHistory(id, undefined, historyId, 'redo', userCacheData);
  }

  /**
   * Clears all of the user's history (wipes everything).
   */
  public clearHistory() {
    this.signalDetectionHypothesisHistory = Immutable.Map();
    this.eventHypothesisHistory = Immutable.Map();
    this.redoPriorityOrder = Immutable.List();
    this.history = Immutable.List();
  }

  // ----- Private Functions ------

  /**
   * Undo or Redo User Cache History, sets the history forward or backward in time
   * and for the number of actions.
   * @param id the unique event id
   * @param numberOfItems the number of actions (history) to undo/redo
   * @param action the action to be preformed (undo or redo)
   * @param userCacheData the user cache data
   * @returns the user cache data that was updated
   */
  private undoRedoHistory(
    numberOfItems: number | undefined,
    historyId: string | undefined,
    action: 'undo' | 'redo',
    userCacheData: UserCacheData
  ): UserCacheData {
    const isUndo = action === 'undo';

    // determine number of items to undo / redo
    let items = numberOfItems ? [...Array(numberOfItems)] : [];
    if (historyId) {
      const orderedHistory = isUndo ? this.history.reverse() : this.history;
      const index = orderedHistory.findIndex(history => history.id === historyId);
      items = orderedHistory
        .toArray()
        .filter((history, idx) => idx <= index)
        .map(history => history.id);
    }

    const { eventHypotheses, signalDetectionHypotheses } = this.flatKeyValuePairs(
      items.map(item => {
        const orderedHistory = isUndo ? this.history.reverse() : this.history;
        const undoRedoHistory = orderedHistory.find(
          h =>
            (!item || item === h.id) &&
            ((isUndo && h.changes.some(change => change.active)) ||
              (!isUndo &&
                ((!item && isEqual(h.id, this.redoPriorityOrder.last())) ||
                  (item && h.changes.some(change => !change.active)))))
        );
        if (undoRedoHistory) {
          const toReturn = {
            eventHypotheses: flatMap<string>(
              [
                ...undoRedoHistory.changes
                  .filter(change => (isUndo && change.active) || (!isUndo && !change.active))
                  .values()
              ]
                .filter(
                  change =>
                    change.hypothesisChangeInformation.type === HypothesisType.EventHypothesis
                )
                .map(change => [change.hypothesisChangeInformation.hypothesisId])
            )
              .filter(hypId => this.eventHypothesisHistory.get(hypId) !== undefined)
              .map<KeyValue<string, EventHypothesis>>(hypId => ({
                id: hypId,
                value: this.eventHypothesisHistory.get(hypId)[action]()
              })),
            signalDetectionHypotheses: flatMap<string>(
              [
                ...undoRedoHistory.changes
                  .filter(change => (isUndo && change.active) || (!isUndo && !change.active))
                  .values()
              ]
                .filter(
                  change =>
                    change.hypothesisChangeInformation.type ===
                    HypothesisType.SignalDetectionHypothesis
                )
                .map(change => [change.hypothesisChangeInformation.hypothesisId])
            )
              .filter(hypId => this.signalDetectionHypothesisHistory.get(hypId) !== undefined)
              .map<KeyValue<string, SignalDetectionHypothesis>>(hypId => ({
                id: hypId,
                value: this.signalDetectionHypothesisHistory.get(hypId)[action]()
              }))
          };

          // update the change active state for the history being undone or redone
          this.updateHistoryActiveState(undoRedoHistory, !isUndo);

          return toReturn;
        }
        return { signalDetectionHypotheses: [], eventHypotheses: [] };
      })
    );
    return this.updateUserCacheData(eventHypotheses, signalDetectionHypotheses, userCacheData);
  }

  /**
   * Undo or Redo User Cache Event History, sets the event history forward or backward in time
   * for the given number of actions or history id.
   * @param id the unique event id
   * @param numberOfItems the number of actions (history) to undo/redo
   * @param historyId the unique history id (possibly undefined)
   * @param action the action to be preformed (undo or redo)
   * @param userCacheData the user cache data
   * @returns the user cache data that was updated
   */
  private undoRedoEventHistory(
    id: string,
    numberOfItems: number | undefined,
    historyId: string | undefined,
    action: 'undo' | 'redo',
    userCacheData: UserCacheData
  ): UserCacheData {
    const isUndo = action === 'undo';

    // determine number of items to undo / redo
    let items = numberOfItems ? [...Array(numberOfItems)] : [];
    if (historyId) {
      const orderedHistory = isUndo ? this.history.reverse() : this.history;
      const index = orderedHistory.findIndex(history => history.id === historyId);
      items = orderedHistory
        .toArray()
        .filter(
          (history, idx) => idx <= index && history.changes.some(change => change.eventId === id)
        )
        .map(history => history.id);
    }

    const { eventHypotheses, signalDetectionHypotheses } = this.flatKeyValuePairs(
      items.map(item => {
        const orderedHistory = isUndo ? this.history.reverse() : this.history;
        const undoRedoHistory = orderedHistory.find(
          h =>
            (!item || item === h.id) &&
            h.changes.some(change => change.eventId === id) &&
            ((isUndo && h.changes.find(change => change.eventId === id).active) ||
              (!isUndo && !h.changes.find(change => change.eventId === id).active))
        );
        if (undoRedoHistory) {
          const toReturn = {
            eventHypotheses: [
              ...undoRedoHistory.changes.filter(
                change =>
                  change.eventId === id &&
                  change.hypothesisChangeInformation.type === HypothesisType.EventHypothesis
              )
            ]
              .map(change => change.hypothesisChangeInformation.hypothesisId)
              .filter(hypId => this.eventHypothesisHistory.get(hypId) !== undefined)
              .map<KeyValue<string, EventHypothesis>>(hypId => ({
                id: hypId,
                value: this.eventHypothesisHistory.get(hypId)[action]()
              })),
            signalDetectionHypotheses: [
              ...undoRedoHistory.changes.filter(
                change =>
                  change.eventId === id &&
                  change.hypothesisChangeInformation.type ===
                    HypothesisType.SignalDetectionHypothesis
              )
            ]
              .map(change => change.hypothesisChangeInformation.hypothesisId)
              .filter(hypId => this.signalDetectionHypothesisHistory.get(hypId) !== undefined)
              .map<KeyValue<string, SignalDetectionHypothesis>>(hypId => ({
                id: hypId,
                value: this.signalDetectionHypothesisHistory.get(hypId)[action]()
              }))
          };

          // update the change active state for the history being undone or redone
          this.updateHistoryActiveState(undoRedoHistory, !isUndo, id);

          return toReturn;
        }
        return { signalDetectionHypotheses: [], eventHypotheses: [] };
      })
    );
    return this.updateUserCacheData(eventHypotheses, signalDetectionHypotheses, userCacheData);
  }

  /**
   * Updates the redo stack to track the order in which changes have bee undone so that they can be
   * redone (re-applied) in the same order.
   * @param history the history entry that is being undone or redone
   * @param active true if the change is being redone; false if the change is being undone
   */
  private updateRedoStack(history: History, active: boolean): void {
    // determine if the item already exists in the stack
    const index: number = this.redoPriorityOrder.findIndex(entry => entry === history.id);

    // if undoing an action and the item exists; remove the entry from the redo stack
    // if redoing an action and all of the changes are now active; remove the entry from the redo stack
    if (index >= 0 && (!active || (active && history.changes.every(change => change.active)))) {
      this.redoPriorityOrder = this.redoPriorityOrder.splice(index, 1);
    }

    // if undoing an action add to the redo stack
    if (!active) {
      this.redoPriorityOrder = this.redoPriorityOrder.push(history.id);
    }
  }

  /**
   * Updates the active state for a history entry.
   *
   * @param history the history entry to update
   * @param active true if the changes should be marked as active; false otherwise
   * @param id (optional) the unique event id (only used for event history)
   */
  private updateHistoryActiveState(history: History, active: boolean, id?: string): void {
    // update the change active state for the history being undone or redone
    this.history = Immutable.List(
      produce<{ history: History[] }>({ history: [...this.history.values()] }, draftState => {
        draftState.history = draftState.history.map(item =>
          item.id === history.id
            ? {
                ...item,
                changes: item.changes.map(change => ({
                  ...change,
                  active: id ? (id === change.eventId ? active : change.active) : active
                }))
              }
            : { ...item }
        );
      }).history
    );

    // update the redo stack (using the history with the updated active state)
    this.updateRedoStack(
      this.history.find(h => h.id === history.id),
      active
    );
  }

  /**
   * Flattens an array of key/values for event hypotheses and signal detection hypothesis.
   * @param values the values to flatten
   */
  private flatKeyValuePairs(
    // tslint:disable-next-line: max-line-length
    values: {
      eventHypotheses: KeyValue<string, EventHypothesis>[];
      signalDetectionHypotheses: KeyValue<string, SignalDetectionHypothesis>[];
    }[]
  ): {
    eventHypotheses: KeyValue<string, EventHypothesis>[];
    signalDetectionHypotheses: KeyValue<string, SignalDetectionHypothesis>[];
  } {
    // tslint:disable-next-line: max-line-length
    return values.reduce<{
      eventHypotheses: KeyValue<string, EventHypothesis>[];
      signalDetectionHypotheses: KeyValue<string, SignalDetectionHypothesis>[];
    }>(
      (accumulator, value) => ({
        eventHypotheses: [...accumulator.eventHypotheses, ...value.eventHypotheses],
        signalDetectionHypotheses: [
          ...accumulator.signalDetectionHypotheses,
          ...value.signalDetectionHypotheses
        ]
      }),
      { eventHypotheses: [], signalDetectionHypotheses: [] }
    );
  }

  /**
   * Update the user cache data with the data returned from the undo/redo action.
   * @param eventHypotheses the event hypotheses to update
   * @param signalDetectionHypotheses the signal detections hypotheses to update
   * @param userCacheData the user cache data (current state) to update
   */
  private updateUserCacheData(
    eventHypotheses: KeyValue<string, EventHypothesis>[],
    signalDetectionHypotheses: KeyValue<string, SignalDetectionHypothesis>[],
    userCacheData: UserCacheData
  ): UserCacheData {
    // filter out only the ones that changed
    const filteredSignalDetections = produce<SignalDetection[]>(
      userCacheData.signalDetections,
      draftState =>
        userCacheData.signalDetections.filter(sd =>
          signalDetectionHypotheses.find(
            hypothesis =>
              includes(
                sd.signalDetectionHypotheses.map(hyp => hyp.id),
                hypothesis.id
              ) ||
              (hypothesis.value && sd.id === hypothesis.value.parentSignalDetectionId)
          )
        )
    );

    const filteredEvents = produce<Event[]>(userCacheData.events, draftState =>
      userCacheData.events.filter(event =>
        eventHypotheses.find(
          hypothesis =>
            includes(
              event.hypotheses.map(hyp => hyp.id),
              hypothesis.id
            ) ||
            (hypothesis.value && event.id === hypothesis.value.eventId) ||
            // include any associations to ensure all events are properly updated
            includes(
              event.associations.map(association => association.eventHypothesisId),
              hypothesis.id
            ) ||
            filteredSignalDetections.find(sd =>
              includes(
                event.associations.map(association => association.signalDetectionId),
                sd.id
              )
            ) !== undefined
        )
      )
    );
    // TODO clean up - determine what is needed here
    // signalDetectionHypotheses.filter(sdHypothesis => sdHypothesis.value).find(sdHypothesis =>
    //   includes(
    //     event.associations.map(association => association.signalDetectionHypothesisId),
    //     sdHypothesis.id)) !== undefined ||
    // signalDetectionHypotheses.filter(sdHypothesis => sdHypothesis.value).find(sdHypothesis =>
    //   includes(
    //     event.associations.map(association => association.signalDetectionId),
    //     sdHypothesis.value.parentSignalDetectionId)) !== undefined));

    const filteredUserCache = produce<UserCacheData>(userCacheData, draftState => {
      draftState.signalDetections = filteredSignalDetections;
      draftState.events = filteredEvents;
    });

    // update the user cache data
    return produce<UserCacheData>(filteredUserCache, draftState => {
      eventHypotheses.forEach(hypothesis => {
        // find the index of the event
        const eventIndex = draftState.events.findIndex(
          event =>
            includes(
              event.hypotheses.map(h => h.id),
              hypothesis.id
            ) ||
            (hypothesis.value && hypothesis.value.eventId === event.id)
        );

        if (eventIndex >= 0) {
          // update the hypotheses; find the index of the hypothesis
          const hypothesisIndex = draftState.events[eventIndex].hypotheses.findIndex(
            hyp => hyp.id === hypothesis.id
          );
          if (hypothesisIndex >= 0) {
            if (hypothesis.value) {
              // update the hypothesis
              draftState.events[eventIndex].hypotheses[hypothesisIndex] = hypothesis.value;
            } else {
              // remove existing hypothesis
              draftState.events[eventIndex].hypotheses.splice(hypothesisIndex, 1);
            }
          } else {
            if (hypothesis.value) {
              // add the hypothesis to the event
              draftState.events[eventIndex].hypotheses = [
                ...draftState.events[eventIndex].hypotheses.map(hyp => hyp),
                hypothesis.value
              ];
            } else {
              logger.error(`Hypothesis is undefined unable to insert ${hypothesis.id}`);
            }
          }

          // update the preferred hypotheses; find the index of the preferred hypothesis
          const preferredIndex = draftState.events[
            eventIndex
          ].preferredEventHypothesisHistory.findIndex(
            preferred => preferred.eventHypothesis.id === hypothesis.id
          );
          if (preferredIndex >= 0) {
            if (hypothesis.value) {
              // update the preferred hypothesis
              draftState.events[eventIndex].preferredEventHypothesisHistory[
                preferredIndex
              ].eventHypothesis = hypothesis.value;
            } else {
              // remove existing preferred hypothesis
              draftState.events[eventIndex].preferredEventHypothesisHistory.splice(
                preferredIndex,
                1
              );
            }
          } else {
            if (hypothesis.value) {
              // add the hypothesis to the preferred hypothesis
              draftState.events[eventIndex].preferredEventHypothesisHistory = [
                ...draftState.events[eventIndex].preferredEventHypothesisHistory.map(hyp => hyp),
                {
                  processingStageId: undefined, // TODO figure out what this should be set too
                  eventHypothesis: hypothesis.value
                }
              ];
            } else {
              logger.warn(`Hypothesis is undefined unable to insert ${hypothesis.id}`);
            }
          }

          // update the current hypotheses
          const isCurrent =
            draftState.events[eventIndex].currentEventHypothesis.eventHypothesis &&
            draftState.events[eventIndex].currentEventHypothesis.eventHypothesis.id ===
              hypothesis.id;
          if (isCurrent) {
            if (hypothesis.value) {
              // update the current hypothesis
              draftState.events[eventIndex].currentEventHypothesis.eventHypothesis =
                hypothesis.value;
            } else {
              // TODO figure out the desired behavior here
              // replace current hypothesis with previous preferred
              if (draftState.events[eventIndex].preferredEventHypothesisHistory.length > 0) {
                draftState.events[eventIndex].currentEventHypothesis.eventHypothesis =
                  draftState.events[eventIndex].preferredEventHypothesisHistory[
                    draftState.events[eventIndex].preferredEventHypothesisHistory.length - 1
                  ].eventHypothesis;
              } else {
                // no previous preferred hypothesis, set the current to undefined
                draftState.events[eventIndex].currentEventHypothesis.eventHypothesis = undefined;
              }
            }
          } else {
            logger.warn(`Hypothesis is not the current hypothesis ${hypothesis.id}`);
            if (hypothesis.value) {
              // TODO figure out the desired behavior here
              // replace the current hypothesis
              draftState.events[eventIndex].currentEventHypothesis.eventHypothesis =
                hypothesis.value;
            } else {
              logger.warn(
                `Hypothesis is undefined unable to set current hypothesis ${hypothesis.id}`
              );
            }
          }
        } else {
          logger.error(`Unable to find event association to hypothesis ${hypothesis.id}`);
        }
      });

      signalDetectionHypotheses.forEach(hypothesis => {
        // find the index of the signal detection
        const sdIndex = draftState.signalDetections.findIndex(
          sd =>
            includes(
              sd.signalDetectionHypotheses.map(h => h.id),
              hypothesis.id
            ) ||
            (hypothesis.value && hypothesis.value.parentSignalDetectionId === sd.id)
        );
        if (sdIndex >= 0) {
          // find the index of the hypothesis
          const hypothesisIndex = draftState.signalDetections[
            sdIndex
          ].signalDetectionHypotheses.findIndex(hyp => hyp.id === hypothesis.id);
          if (hypothesisIndex >= 0) {
            if (hypothesis.value) {
              // update the hypothesis
              draftState.signalDetections[sdIndex].signalDetectionHypotheses[hypothesisIndex] =
                hypothesis.value;
            } else {
              // remove existing hypothesis
              draftState.signalDetections[sdIndex].signalDetectionHypotheses.splice(
                hypothesisIndex,
                1
              );
            }
          } else {
            if (hypothesis.value) {
              // add the hypothesis to the signal detection
              draftState.signalDetections[sdIndex].signalDetectionHypotheses = [
                ...draftState.signalDetections[sdIndex].signalDetectionHypotheses.map(hyp => hyp),
                hypothesis.value
              ];
            } else {
              logger.error(`Hypothesis is undefined unable to insert ${hypothesis.id}`);
            }
          }
        } else {
          logger.error(
            `Unable to find signal detection association to hypothesis ${hypothesis.id}`
          );
        }
      });
    });
  }

  /**
   * Finds the right event id based on the inputs
   * @param description action taken by user
   * @param signalDetection signal detection to get the proper event id for
   * @param userCacheSds user cached signal detections
   * @param events events being updated
   */
  private getEventIdForChange(
    description: UserActionDescription,
    signalDetection: SignalDetection,
    sdHypId: string
  ) {
    // If the action being taken is unassociate the rejected associations should not be filtered out
    // when finding the correct event id to use. For all other actions, rejected associations should
    // be filtered out
    const associations =
      description === UserActionDescription.REJECT_DETECTION ||
      description === UserActionDescription.REJECT_MULTIPLE_DETECTIONS ||
      description === UserActionDescription.CHANGE_SIGNAL_DETECTION_ASSOCIATIONS_UNASSOCIATE ||
      description ===
        UserActionDescription.CHANGE_SIGNAL_DETECTION_ASSOCIATIONS_UNASSOCIATE_MULTIPLE
        ? signalDetection.associations
        : signalDetection.associations.filter(assoc => !assoc.rejected);

    const associationFound = associations.find(
      (association, index) => association.signalDetectionHypothesisId === sdHypId
    );

    return associationFound ? associationFound.eventId : undefined;
  }

  /**
   * Gets the hyp id to use based on action and other inputs
   * @param description action taken by user
   * @param signalDetection signal detection to get the proper event id for
   * @param userCacheSds user cached signal detections
   * @param events events being updated
   */
  private getSDHypIdToUse(
    description: UserActionDescription,
    signalDetection: SignalDetection,
    userCacheSds: SignalDetection[],
    events: Event[]
  ) {
    // If the action is create event or unassociate, use the event being updated (events[0])
    // to find the right hypothesis, otherwise use the hypothesis from current event
    // this is necessary to get the correct event id for coloring the history items
    return (description === UserActionDescription.CREATE_EVENT ||
      description === UserActionDescription.CHANGE_SIGNAL_DETECTION_ASSOCIATIONS_UNASSOCIATE ||
      description ===
        UserActionDescription.CHANGE_SIGNAL_DETECTION_ASSOCIATIONS_UNASSOCIATE_MULTIPLE) &&
      events.length === 1
      ? signalDetection.signalDetectionHypotheses.find(
          hyp =>
            signalDetection.associations.find(
              association =>
                association.eventId === events[0].id &&
                association.signalDetectionHypothesisId === hyp.id
            ) !== undefined
        ).id
      : userCacheSds.find(s => s.id === signalDetection.id).currentHypothesis.id;
  }
}
