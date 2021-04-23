import Immutable from 'immutable';
import cloneDeep from 'lodash/cloneDeep';
import { ChannelSegment, TimeSeries } from '../channel-segment/model';
import { WorkspaceState } from '../common/model';
import { Event } from '../event/model-and-schema/model';
import { gatewayLogger } from '../log/gateway-logger';
import { QcMask } from '../qc-mask/model';
import { SignalDetection } from '../signal-detection/model';
import { ProcessingStationData } from '../station/processing-station/model';
import { ReferenceStationData } from '../station/reference-station/model';
import { getInitialTimeRange } from '../util/workflow-util';
import { WaveformFilterDefinition } from '../waveform-filter/model';
import { ProcessingActivityInterval, WorkflowData } from '../workflow/model';
import { GlobalCache } from './global-cache';
import { UserCache } from './user-cache';
import { UserCacheItem } from './user-cache-item';

/**
 * API gateway Cache Processor for a single global data cache that represents the
 * OSD state for all users.
 */
export class CacheProcessor {
  /** The singleton instance */
  private static instance: CacheProcessor;

  /**
   * Shared workspace state
   */
  private workspaceState: WorkspaceState;

  /** The singleton instance of Global Cache */
  private globalCache: GlobalCache;

  /**
   * Stored User Cache
   */
  private userCaches: Immutable.Map<string, UserCache>;

  /**
   * Constructor
   */
  private constructor() {
    /* no-op */
  }

  /**
   * Returns the singleton instance of the cache processor.
   * @returns the instance of the cache processor
   */
  public static Instance(): CacheProcessor {
    if (CacheProcessor.instance === undefined) {
      CacheProcessor.instance = new CacheProcessor();
      CacheProcessor.instance.initialize();
    }
    return CacheProcessor.instance;
  }

  // ----- User Cache ------

  /**
   * Gets or creates a user cache for the given id
   * @param id the user session id
   * @returns a user cache object
   */
  public getCacheForUser(id: string): UserCache {
    if (id === undefined) {
      gatewayLogger.error(
        'Undefined session id, likely caused by an async resolver that is not reading the session context'
      );
    }
    if (this.userCaches.has(id)) {
      return this.userCaches.get(id);
    }
    const eventCache = new UserCacheItem<Event>(
      // clone the events to ensure that the global cache is not accidentally modified, consider freezing
      cloneDeep(CacheProcessor.Instance().globalCache.getEvents()),
      (events: Event[]) => CacheProcessor.Instance().commitEvents(events)
    );

    const sdCache = new UserCacheItem<SignalDetection>(
      // clone the events to ensure that the global cache is not accidentally modified, consider freezing
      cloneDeep(CacheProcessor.Instance().globalCache.getSignalDetections()),
      (sds: SignalDetection[]) => CacheProcessor.Instance().commitSignalDetections(sds)
    );

    const newUserCache = new UserCache(getInitialTimeRange(), undefined, eventCache, sdCache);
    this.userCaches = this.userCaches.set(id, newUserCache);
    gatewayLogger.info(`Creating new User Cache with ID: ${id}`);
    return newUserCache;
  }

  /**
   * Deletes the user caches for a session ID
   * @param sessionId the unique session id
   */
  public deleteUserCache(sessionId: string): void {
    if (this.userCaches.has(sessionId)) {
      this.userCaches = this.userCaches.delete(sessionId);
    }
  }

  // ----- Workspace State Functions ------

  /**
   * Returns the workspace state
   * @returns the workspace state
   */
  public getWorkspaceState(): WorkspaceState {
    return cloneDeep(this.workspaceState);
  }

  /**
   * Adds or updates event to user map in workspace state
   * @param eventId event id to remove user from
   * @param userName the username to remove
   */
  public addOrUpdateEventToUser(eventId: string, userName: string): void {
    const eventToUsers = this.workspaceState.eventToUsers.find(
      evToUsers => evToUsers.eventId === eventId
    );
    if (eventToUsers) {
      // Event is already in the map so if the username is not in the list push it
      if (eventToUsers.userNames.indexOf(userName) < 0) {
        eventToUsers.userNames.push(userName);
      }
    } else {
      // Event is not in map, create new username array
      this.workspaceState.eventToUsers.push({ eventId, userNames: [userName] });
    }
  }

  /**
   * Removes a user from the event in the workspace state
   * @param eventId event id to remove user from
   * @param userName the username to remove
   */
  public removeUserFromEvent(eventId: string, userName: string): void {
    const eventToUsers = this.workspaceState.eventToUsers.find(
      evToUsers => evToUsers.eventId === eventId
    );
    const index = eventToUsers ? eventToUsers.userNames.indexOf(userName) : undefined;
    if (index >= 0) {
      eventToUsers.userNames.splice(index, 1);
    }
  }

  // ----- Configuration Functions ------

  /**
   * Gets the configuration from the global cache
   * @returns the configuration from the global cache
   */
  public getConfiguration(): Immutable.Map<string, string> {
    return this.globalCache.getConfiguration();
  }

  /**
   * Set the configuration in the global cache
   * @param configuration the configuration
   */
  public setConfiguration(configuration: Immutable.Map<string, string>): void {
    this.globalCache.setConfiguration(configuration);
  }

  // ----- Reference Station Data Functions ------

  /**
   * Returns true if the station data exists; false otherwise
   * @param id the unique id of the station
   * @returns true if the station data exists; false otherwise
   */
  public hasReferenceStationData(id: string): boolean {
    return this.globalCache.hasReferenceStationData(id);
  }

  /**
   * Gets the station data from the global cache for the given id
   * @param name the unique id (network name)
   * @returns the station data
   */
  public getReferenceStationData(name: string): ReferenceStationData {
    return this.globalCache.getReferenceStationData(name);
  }

  /**
   * Gets the station data from the global cache
   * @returns the station data
   */
  public getReferenceAllStationData(): Immutable.Map<string, ReferenceStationData> {
    return this.globalCache.getAllReferenceStationData();
  }

  /**
   * Set the station data in the global cache
   * @param stationData the station data to save/update
   */
  public setReferenceStationData(stationData: ReferenceStationData): void {
    this.globalCache.setReferenceStationData(stationData);
  }

  // ----- Processing Station Data Functions ------

  /**
   * Gets the station data from the global cache for the given id
   * @param name the unique id (network name)
   * @returns the station data
   */
  public getProcessingStationData(): ProcessingStationData {
    return this.globalCache.getProcessingStationData();
  }

  // ----- Workflow Data Functions ------

  /**
   * Returns the workflow data from the global cache.
   *
   * @returns the workflow data
   */
  public getWorkflowData(): WorkflowData {
    return this.globalCache.getWorkflowData();
  }

  /**
   * Sets the workflow data in the global cache.
   *
   * @param workflow the workflow data to set
   */
  public setWorkflowData(workflow: WorkflowData): void {
    this.globalCache.setWorkflowData(workflow);
  }

  /**
   * Returns the current open activity from the global cache.
   *
   * @returns the current open activity.
   */
  public getCurrentOpenActivity(): ProcessingActivityInterval {
    return this.globalCache.getCurrentOpenActivity();
  }

  /**
   * Sets the current open activity in the global cache.
   *
   * @param activity the activity to set
   */
  public setCurrentOpenActivity(activity: ProcessingActivityInterval): void {
    this.globalCache.setCurrentOpenActivity(activity);
  }

  // ----- Waveform Filter Definition Functions ------

  /**
   * Gets the waveform filter definitions from the global cache
   */
  public getWaveformFilterDefinitions(): Immutable.List<WaveformFilterDefinition> {
    return this.globalCache.getWaveformFilterDefinitions();
  }

  /**
   * Set the waveform filter definitions in the global cache
   * @param definitions the waveform filter definitions to save/update
   */
  public setWaveformFilterDefinitions(
    definitions: Immutable.List<WaveformFilterDefinition> | WaveformFilterDefinition[]
  ): void {
    this.globalCache.setWaveformFilterDefinitions(definitions);
  }

  // ----- Channel Segment Functions ------

  /**
   * Returns true if the channel segment exists; false otherwise
   * @param id the unique channel segment id
   * @returns true if the channel segment exists; false otherwise
   */
  public hasChannelSegment(id: string): boolean {
    return this.globalCache.hasChannelSegment(id);
  }

  /**
   * Gets the channel segment from the global cache for the given id
   * @param id the unique id
   * @returns the channel segment
   */
  public getChannelSegment(id: string): ChannelSegment<TimeSeries> {
    return this.globalCache.getChannelSegment(id);
  }

  /**
   * Gets the channel segments from the global cache
   * @returns the channel segments
   */
  public getChannelSegments(): Immutable.Map<string, ChannelSegment<TimeSeries>> {
    return this.globalCache.getChannelSegments();
  }

  /**
   * Set the channel segments in the global cache
   * @param channelSegment the channel segments to save/update
   */
  public setChannelSegments(channelSegments: ChannelSegment<TimeSeries>[]): void {
    this.globalCache.setChannelSegments(channelSegments);
  }

  /**
   * Set the channel segment in the global cache
   * @param channelSegment the channel segment to save/update
   */
  public setChannelSegment(channelSegment: ChannelSegment<TimeSeries>): void {
    this.globalCache.setChannelSegment(channelSegment);
  }

  // ----- Event Functions ------

  /**
   * When a component adds new data (queried from the OSD),
   * update the Global Cache and sync to the individual
   * user caches (cloned global caches);
   * @param events the events to be added to the global cache
   */
  public addLoadedEventsToGlobalCache(events: Event[]): void {
    // global cache add all of them
    events.forEach(event => {
      this.globalCache.setEvent(event);
    });
    this.pushAndPublishUpdatedGlobalEvents(events.map(event => event.id));
  }

  // ----- Signal Detection Functions ------

  /**
   * When a component adds new data (queried from the OSD),
   * update the Global Cache and sync to the individual
   * user caches (cloned global caches);
   * @param sds the signal detections to be added to the global cache
   */
  public addLoadedSdsToGlobalCache(sds: SignalDetection[]): void {
    // global cache add all of them
    sds.forEach(sd => {
      this.globalCache.setSignalDetection(sd);
    });
    this.pushAndPublishUpdatedGlobalSignalDetections(sds.map(sd => sd.id));
  }

  // ----- QC Mask Functions ------

  /**
   * Returns true if the qc mask exists; false otherwise
   * @param id the qc mask id
   * @returns true if exists; false otherwise
   */
  public hasQcMask(id: string): boolean {
    return this.globalCache.hasQcMask(id);
  }

  /**
   * Gets the qc mask from the global cache for the given id
   * @param id the qc mask id
   * @returns the qc mask
   */
  public getQcMask(id: string): QcMask {
    return this.globalCache.getQcMask(id);
  }

  /**
   * Gets the qc mask from the global cache
   * @returns the qc masks
   */
  public getQcMasks(): Immutable.Map<string, QcMask> {
    return this.globalCache.getQcMasks();
  }

  /**
   * Set the qc masks in the global cache
   * @param qcMasks the qc masks to save/update
   */
  public setQcMasks(qcMasks: QcMask[]): void {
    this.globalCache.setQcMasks(qcMasks);
  }

  /**
   * Set the qc mask in the global cache
   * @param qcMask the qc mask to save/update
   */
  public setQcMask(qcMask: QcMask): void {
    this.globalCache.setQcMask(qcMask);
  }

  // ----- Private Functions ------

  /**
   * Initialize the Cache Processor (global cache and user caches)
   */
  private initialize(): void {
    this.globalCache = new GlobalCache();
    this.userCaches = Immutable.Map<string, UserCache>();

    this.workspaceState = {
      eventToUsers: []
    };
  }

  /**
   * Commit signal detections to the global cache.
   * Means that a user has saved (committed).
   * @param signalDetections the signal detections to commit
   */
  private commitSignalDetections(signalDetections: SignalDetection[]): void {
    // Walk thru all the user cache SD entries and set them on Global cache
    // then clear user cache list
    signalDetections.forEach(uSd => {
      this.globalCache.setSignalDetection(uSd);
    });
    this.pushAndPublishUpdatedGlobalSignalDetections(
      signalDetections.map(userSd => userSd.id),
      true
    );
  }

  /**
   * Push and publish updated global cache events to the user caches.
   * @param ids event ids to publish
   * @param overwrite if true, the user cache will be overwritten with new data
   */
  private pushAndPublishUpdatedGlobalSignalDetections(
    ids: string[],
    overwrite: boolean = false
  ): void {
    this.userCaches.forEach(userCache => {
      userCache.updateSignalDetectionsFromGlobalCache(
        [
          ...cloneDeep(
            this.globalCache
              .getSignalDetections()
              .filter((value, key) => ids.find(id => id === key))
              .values()
          )
        ],
        overwrite
      );
    });
  }

  /**
   * Commit events to the global cache.
   * Means that a user has saved (committed).
   * @param signalDetections the events to commit
   */
  private commitEvents(userEvents: Event[]): void {
    // Walk thru all the user cache event entries and set them on Global cache
    // then clear user cache list
    userEvents.forEach(uEvent => {
      this.globalCache.setEvent(uEvent);
    });
    this.pushAndPublishUpdatedGlobalEvents(
      userEvents.map(event => event.id),
      true
    );
  }

  /**
   * Push and publish updated global cache events to the user caches.
   * @param ids event ids to publish
   * @param overwrite if true, the user cache will be overwritten with new data
   */
  private pushAndPublishUpdatedGlobalEvents(ids: string[], overwrite: boolean = false): void {
    this.userCaches.forEach(userCache => {
      userCache.updateEventsFromGlobalCache(
        [
          ...cloneDeep(
            this.globalCache
              .getEvents()
              .filter((value, key) => ids.find(id => id === key))
              .values()
          )
        ],
        overwrite
      );
    });
  }
}
