import Immutable from 'immutable';
import { Association, TimeRange } from '../common/model';
import { Event } from '../event/model-and-schema/model';
import { QcMask } from '../qc-mask/model';
import { SignalDetection } from '../signal-detection/model';
import { UserProfile } from '../user-profile/model';

/**
 * Specifies the type (method) for committing to the Global Cache.
 */
export type CommitFunc<T> = (items: T[]) => void;

/**
 * User Cache Processor interface.
 * Defines all of the common method definitions required
 * for a user cache item.
 */
// tslint:disable-next-line: interface-name
export interface IUserCacheItem<T> {
  has(id: string): boolean;
  get(id: string): T;
  getAll(): T[];
  set(item: T): void;
  setAll(items: T[]): void;
  remove(item: T): void;
  removeAll(item: T[]): void;
  commitAll(): void;
  commitWithIds(ids: string[]): void;
  updateFromGlobalCache(cachedEntries: T[], overwrite: boolean): void;
}

/**
 * User Cache interface.
 * Defines all of the common method definitions required for a user cache.
 */
// tslint:disable-next-line: interface-name
export interface IUserCache {
  getTimeRange(): TimeRange;
  setTimeRange(timeRange: TimeRange): void;
  getUserProfile(): UserProfile;
  setUserProfile(userProfile: UserProfile): void;
  getOpenEventId(): string;
  setOpenEventId(id: string): void;
  getEventById(id: string): Event | undefined;
  getEvents(): Event[];
  getInvalidEventIds(): string[];
  setEvent(userAction: UserActionDescription, event: Event): void;
  setEvents(userAction: UserActionDescription, events: Event[]): void;
  updateEventsFromGlobalCache(events: Event[], overwrite: boolean): void;
  commitAllEvents(): void;
  commitEventsWithIds(ids: string[]): void;
  getSignalDetectionById(id: string): SignalDetection | undefined;
  getSignalDetections(): SignalDetection[];
  getInvalidSignalDetectionIds(): string[];
  setSignalDetection(userAction: UserActionDescription, sd: SignalDetection): void;
  setSignalDetections(userAction: UserActionDescription, sds: SignalDetection[]): void;
  updateSignalDetectionsFromGlobalCache(sds: SignalDetection[], overwrite: boolean): void;
  commitAllSignalDetections(): void;
  commitSignalDetectionsWithIds(ids: string[]): void;
  getAssociations(): Association[];
  getSignalDetectionAndEventBySdId(id): { signalDetection: SignalDetection; event: Event };
  setEventsAndSignalDetections(
    userAction: UserActionDescription,
    events: Event[],
    sds: SignalDetection[]
  ): void;
  getRedoPriorityOrder(id: string): number | undefined;
  getHistory(): History[];
  undoHistory(numberOfItems: number): UserCacheData;
  redoHistory(numberOfItems: number): UserCacheData;
  undoHistoryById(id: string): UserCacheData;
  redoHistoryById(id: string): UserCacheData;
  getEventHistory(id: string): History[];
  undoEventHistory(numberOfItems: number): UserCacheData;
  redoEventHistory(numberOfItems: number): UserCacheData;
  undoEventHistoryById(id: string): UserCacheData;
  redoEventHistoryById(id: string): UserCacheData;
  clearHistory(): void;
}

/**
 * User context for apollo and user sessions
 */
export interface UserContext {
  readonly sessionId: string;
  readonly userName: string;
  readonly userCache: IUserCache;
  readonly userRole: string;
}

/**
 * Represents the user cache data from the user cache.
 */
export interface UserCacheData {
  readonly events: Event[];
  readonly signalDetections: SignalDetection[];
}

/**
 * Description of a user action preformed
 */
export enum UserActionDescription {
  UNKNOWN = 'Unknown',
  CHANGE_SIGNAL_DETECTION_ASSOCIATIONS_ASSOCIATE = 'Associate',
  CHANGE_SIGNAL_DETECTION_ASSOCIATIONS_ASSOCIATE_MULTIPLE = 'Associate multiple detections',
  CHANGE_SIGNAL_DETECTION_ASSOCIATIONS_UNASSOCIATE = 'Unassociate',
  CHANGE_SIGNAL_DETECTION_ASSOCIATIONS_UNASSOCIATE_MULTIPLE = 'Unassociate multiple detections',

  // signal detection actions
  CREATE_DETECTION = 'Create detection',
  REJECT_DETECTION = 'Reject detection',
  REJECT_MULTIPLE_DETECTIONS = 'Reject multiple detections',
  UPDATE_DETECTION_RE_TIME = 'Time',
  UPDATE_DETECTION_RE_PHASE = 'Phase',
  UPDATE_MULTIPLE_DETECTIONS_RE_PHASE = 'Phase multiple detections',
  UPDATE_DETECTION_AMPLITUDE = 'Update detection amplitude',
  UPDATE_DETECTION_REVIEW_AMPLITUDE = 'Update detection review amplitude',
  UPDATE_DETECTION = 'Update detection',
  COMPUTE_FK = 'Fk',
  COMPUTE_MULTIPLE_FK = 'Fk multiple detections',

  // event actions
  CREATE_EVENT = 'Create event',
  UPDATE_EVENT_LOCATE = 'Locate event',
  UPDATE_EVENT_FROM_SIGNAL_DETECTION_CHANGE = 'Update event from signal detection change',
  UPDATE_EVENT_FEATURE_PREDICTIONS = 'Update event feature predictions',
  UPDATE_EVENT_STATUS_OPEN_FOR_REFINEMENT = 'Event opened for refinement',
  UPDATE_EVENT_MARK_COMPLETE = 'Event marked complete',
  UPDATE_EVENT_PREFERRED_HYP = 'Update event preferred hypothesis',
  UPDATE_EVENT_MAGNITUDE = 'Change Magnitude defining settings',
  SAVE_EVENT = 'Save event'
}

/**
 * Description of a user action performed that cannot be undone and will not be in the history
 */
export const NON_UNDO_ACTIONS: UserActionDescription[] = [
  UserActionDescription.UPDATE_EVENT_FEATURE_PREDICTIONS,
  UserActionDescription.UPDATE_EVENT_STATUS_OPEN_FOR_REFINEMENT,
  UserActionDescription.UPDATE_EVENT_MARK_COMPLETE,
  UserActionDescription.SAVE_EVENT,
  UserActionDescription.UPDATE_DETECTION_REVIEW_AMPLITUDE
];

/**
 * Defines the interface of User Action.
 */
export interface UserAction {
  /**
   * The user action description
   */
  readonly description: UserActionDescription;

  /**
   * Returns the toString value of a user action.
   *
   * @returns the string representation of the action
   */
  toString(): string;
}

/**
 * Defines the hypothesis type for a hypothesis change.
 */
export enum HypothesisType {
  EventHypothesis = 'EventHypothesis',
  SignalDetectionHypothesis = 'SignalDetectionHypothesis'
}

/**
 * Represents hypothesis information for a change.
 * Defines a mapping from a hypothesis id to the main
 * object id.
 */
export interface HypothesisChangeInformation {
  readonly id: string;
  readonly hypothesisId: string;
  readonly type: HypothesisType;
  readonly parentId: string;
  readonly userAction: UserAction;
}

/**
 * Represents a history change.
 */
export interface HistoryChange {
  readonly id: string;
  readonly active: boolean;
  readonly eventId: string;
  readonly conflictCreated: boolean;
  readonly hypothesisChangeInformation: HypothesisChangeInformation;
}

/**
 * Represents the history.
 */
export interface History {
  readonly id: string;
  readonly description: UserActionDescription;
  readonly changes: Immutable.List<HistoryChange>;
}

/**
 * Represents data that has been created or updated and needs to
 * be sent to the UI Clients
 *
 * Currently only include events and sds
 */
export interface DataPayload {
  readonly events: Event[];
  readonly sds: SignalDetection[];
  readonly qcMasks: QcMask[];
}

/**
 * Invalid data; typically data that has been deleted.
 */
export interface InvalidData {
  readonly eventIds: string[];
  readonly signalDetectionIds: string[];
}
