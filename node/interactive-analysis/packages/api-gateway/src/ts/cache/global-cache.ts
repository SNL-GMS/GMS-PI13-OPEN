import Immutable from 'immutable';
import { ChannelSegment, TimeSeries } from '../channel-segment/model';
import { Event } from '../event/model-and-schema/model';
import { QcMask } from '../qc-mask/model';
import { SignalDetection } from '../signal-detection/model';

import { ProcessingStationData } from '../station/processing-station/model';
import { ReferenceStationData } from '../station/reference-station/model';
import { WaveformFilterDefinition } from '../waveform-filter/model';
import { ProcessingActivityInterval, WorkflowData } from '../workflow/model';

/**
 * Global Cache contains various component caches that reflect the OSD entries.
 * This is a single instance that is shared with all users connected to the
 * API Gateway.
 */
export class GlobalCache {
  /** The configuration */
  private configuration: any;

  /** the reference station data */
  private referenceStationData: Immutable.Map<string, ReferenceStationData>;

  /** the processing station data */
  // !TODO MAKE PRIVATE
  public processingStationData: ProcessingStationData;

  /** the workflow data */
  private workflow: WorkflowData;

  /** the current opened processing activity */
  private currentlyOpenActivity: ProcessingActivityInterval;

  /** the waveform filter definitions */
  public waveformFilterDefinitions: Immutable.List<WaveformFilterDefinition>;

  /** the channel segments */
  private channelSegments: Immutable.Map<string, ChannelSegment<TimeSeries>>;

  /** the qc masks */
  private qcMasks: Immutable.Map<string, QcMask>;

  /** The events */
  private events: Immutable.Map<string, Event>;

  /** The signal detections */
  private signalDetections: Immutable.Map<string, SignalDetection>;

  /** Constructor */
  public constructor() {
    this.configuration = {};
    this.referenceStationData = Immutable.Map<string, ReferenceStationData>();
    // Initialize the Processing Station Cache used by the
    // ProcessingStationProcessor
    // TODO Use Immutable Maps
    this.processingStationData = {
      stationGroupMap: new Map(),
      stationMap: new Map(),
      channelGroupMap: new Map(),
      channelMap: new Map(),
      sohStationGroupNameMap: new Map()
    };

    // TODO Use Immutable Types
    this.workflow = {
      stages: [],
      activities: [],
      intervals: [],
      stageIntervals: [],
      analysts: [],
      activityIntervals: []
    };

    // TODO Use Immutable Types
    this.currentlyOpenActivity = undefined;
    this.waveformFilterDefinitions = Immutable.List<WaveformFilterDefinition>();
    this.channelSegments = Immutable.Map<string, ChannelSegment<TimeSeries>>();
    this.qcMasks = Immutable.Map<string, QcMask>();
    this.signalDetections = Immutable.Map<string, SignalDetection>();
    this.events = Immutable.Map<string, Event>();
  }

  // ----- Configuration Functions ------

  /**
   * Gets the configuration from the global cache
   * @returns the configuration
   */
  public getConfiguration(): any {
    return this.configuration;
  }

  /**
   * Set the configuration in the global cache
   * @param configuration the configuration
   */
  public setConfiguration(configuration: any): void {
    this.configuration = configuration;
  }

  // ----- Reference Station Data Functions ------

  /**
   * Returns true if the station data exists; false otherwise
   * @returns true if exists; false otherwise
   */
  public hasReferenceStationData(id: string): boolean {
    return this.referenceStationData.has(id);
  }

  /**
   * Gets the reference station data from the global cache for the given id
   * @param name the unique name (network name)
   * @returns the station data
   */
  public getReferenceStationData(name: string): ReferenceStationData {
    return this.referenceStationData.get(name);
  }

  /**
   * Gets the station data from the global cache
   * @returns the station data
   */
  public getAllReferenceStationData(): Immutable.Map<string, ReferenceStationData> {
    return this.referenceStationData;
  }

  /**
   * Set the station data in the global cache
   * @param stationData the station data to save/update
   */
  public setReferenceStationData(stationData: ReferenceStationData): void {
    this.referenceStationData = this.referenceStationData.set(
      stationData.network.name,
      stationData
    );
  }

  // ----- Processing Station Data Functions ------

  /**
   * Gets the processing station data cache from the global cache
   * @returns the processing station data cache
   */
  public getProcessingStationData(): ProcessingStationData {
    return this.processingStationData;
  }

  // ----- Workflow Data Functions ------

  /**
   * Returns the workflow data from the global cache.
   */
  public getWorkflowData(): WorkflowData {
    return this.workflow;
  }

  /**
   * Sets the workflow data in the global cache.
   *
   * @param workflow the workflow data to set
   */
  public setWorkflowData(workflow: WorkflowData): void {
    this.workflow = workflow;
  }

  /**
   * Returns the current open activity from the global cache.
   *
   * @returns the current open activity.
   */
  public getCurrentOpenActivity(): ProcessingActivityInterval {
    return this.currentlyOpenActivity;
  }

  /**
   * Sets the current open activity in the global cache.
   *
   * @param activity the activity to set
   */
  public setCurrentOpenActivity(activity: ProcessingActivityInterval): void {
    this.currentlyOpenActivity = activity;
  }

  // ----- Waveform Filter Definition Functions ------

  /**
   * Gets the waveform filter definitions from the global cache
   * @returns the wave filter definitions
   */
  public getWaveformFilterDefinitions(): Immutable.List<WaveformFilterDefinition> {
    return this.waveformFilterDefinitions;
  }

  /**
   * Set the waveform filter in the global cache
   * @param definitions the waveform filter definitions to save/update
   */
  public setWaveformFilterDefinitions(
    definitions: Immutable.List<WaveformFilterDefinition> | WaveformFilterDefinition[]
  ): void {
    this.waveformFilterDefinitions = Immutable.List(definitions);
  }

  // ----- Channel Segment Functions ------

  /**
   * Returns true if the channel segment exists; false otherwise
   * @returns true if exists; false otherwise
   */
  public hasChannelSegment(id: string): boolean {
    return this.channelSegments.has(id);
  }

  /**
   * Gets the channel segment from the global cache for the given id
   * @param id the unique id
   * @returns the channel segment
   */
  public getChannelSegment(id: string): ChannelSegment<TimeSeries> {
    return this.channelSegments.get(id);
  }

  /**
   * Gets the channel segments from the global cache
   * @returns the channel segments
   */
  public getChannelSegments(): Immutable.Map<string, ChannelSegment<TimeSeries>> {
    return this.channelSegments;
  }

  /**
   * Set the channel segments in the global cache
   * @param channelSegments the channel segments to save/update
   */
  public setChannelSegments(channelSegments: ChannelSegment<TimeSeries>[]): void {
    channelSegments.forEach(seg => this.setChannelSegment(seg));
  }

  /**
   * Set the channel segment in the global cache
   * @param channelSegment the channel segment to save/update
   */
  public setChannelSegment(channelSegment: ChannelSegment<TimeSeries>): void {
    this.channelSegments = this.channelSegments.set(channelSegment.id, channelSegment);
  }

  // ----- QC Mask Functions ------

  /**
   * Returns true if the qc mask exists; false otherwise
   * @returns true if exists; false otherwise
   */
  public hasQcMask(id: string): boolean {
    return this.qcMasks.has(id);
  }

  /**
   * Gets the qc mask from the global cache for the given id
   * @param id the unique id
   * @returns the qc mask
   */
  public getQcMask(id: string): QcMask {
    return this.qcMasks.get(id);
  }

  /**
   * Gets the qc mask from the global cache
   * @returns the qc masks
   */
  public getQcMasks(): Immutable.Map<string, QcMask> {
    return this.qcMasks;
  }

  /**
   * Set the qc masks in the global cache
   * @param qcMasks the qc masks to save/update
   */
  public setQcMasks(qcMasks: QcMask[]): void {
    qcMasks.forEach(qcMask => this.setQcMask(qcMask));
  }

  /**
   * Set the qc mask in the global cache
   * @param qcMask the qc mask to save/update
   */
  public setQcMask(qcMask: QcMask): void {
    this.qcMasks = this.qcMasks.set(qcMask.id, qcMask);
  }

  // ----- Event Functions ------

  /**
   * Gets the events
   * @returns the events
   */
  public getEvents(): Immutable.Map<string, Event> {
    return this.events;
  }

  /**
   * Set the Events in the global cache
   * @param events Events
   */
  public setEvents(events: Event[]): void {
    events.forEach(e => this.setEvent(e));
  }

  /**
   * Set the event in the global cache
   * @param event Event
   */
  public setEvent(event: Event): void {
    this.events = this.events.set(event.id, event);
  }

  /**
   * Returns true if the event exists; false otherwise
   * @returns true if exists; false otherwise
   */
  public hasEvent(id: string) {
    return this.events.has(id);
  }

  // ----- Signal Detection Functions ------

  /**
   * Gets the signal detections
   * @returns the signal detections
   */
  public getSignalDetections(): Immutable.Map<string, SignalDetection> {
    return this.signalDetections;
  }

  /**
   * Set the Signal Detections in the global cache
   * @param signalDetections Signal Detections
   */
  public setSignalDetections(signalDetections: SignalDetection[]): void {
    signalDetections.forEach(sd => this.setSignalDetection(sd));
  }

  /**
   * Set the Signal Detection in the global cache
   * @param signalDetection Signal Detection
   */
  public setSignalDetection(signalDetection: SignalDetection): void {
    this.signalDetections = this.signalDetections.set(signalDetection.id, signalDetection);
  }

  /**
   * Returns true if the SD exists; false otherwise
   * @returns true if exists; false otherwise
   */
  public hasSignalDetection(id: string) {
    return this.signalDetections.has(id);
  }
}
