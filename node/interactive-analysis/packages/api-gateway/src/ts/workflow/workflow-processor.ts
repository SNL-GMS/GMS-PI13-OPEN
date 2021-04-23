import { epochSecondsNow, toOSDTime, uuid4 } from '@gms/common-util';
import config from 'config';
import { PubSub } from 'graphql-subscriptions';
import filter from 'lodash/filter';
import find from 'lodash/find';
import { CacheProcessor } from '../cache/cache-processor';
import { DataPayload, UserContext } from '../cache/model';
import { ConfigProcessor } from '../config/config-processor';
import { EventProcessor } from '../event/event-processor';
import { gatewayLogger as logger } from '../log/gateway-logger';
import { SignalDetectionProcessor } from '../signal-detection/signal-detection-processor';
import { createDataPayload } from '../util/common-utils';
import { HttpClientWrapper, HttpResponse } from '../util/http-wrapper';
import { normalizeStartEndTimes } from '../util/workflow-util';
import * as model from './model';
import * as workflowBackend from './workflow-mock-backend';

/**
 * API gateway processor for workflow data APIs. This class supports:
 * - data fetching & caching from the backend service interfaces
 * - mocking of backend service interfaces based on test configuration
 * - session management
 * - GraphQL query resolution from the user interface client
 */
export class WorkflowProcessor {
  /** The singleton instance */
  private static instance: WorkflowProcessor;

  /** Local configuration settings */
  private readonly settings: any;

  /** HTTP client wrapper for communicating with backend services */
  private readonly httpWrapper: HttpClientWrapper;

  /** Interval time values for creation of new intervals */
  private intervalCreationStartTimeSec: number;

  /** How many seconds is the interval duration (default 2 hrs) */
  private intervalDurationSec: number = 7200;

  /** Interval Elapsed secs */
  private intervalElapsedSecs: number = 0;

  /** How often to create new workflow interval (default 2 hrs) */
  private createIntervalDurationSec: number = 7200;

  /** Create the publish/subscribe API for GraphQL subscriptions */
  private readonly pubsub: PubSub = new PubSub();

  /**
   * Returns the singleton instance of the cache processor.
   * @returns the instance of the cache processor
   */
  public static Instance(): WorkflowProcessor {
    if (WorkflowProcessor.instance === undefined) {
      WorkflowProcessor.instance = new WorkflowProcessor();
      WorkflowProcessor.instance.initialize();
    }
    return WorkflowProcessor.instance;
  }

  /**
   * Constructor - initialize the processor, loading settings and initializing
   * the HTTP client wrapper.
   */
  private constructor() {
    // Load configuration settings
    this.settings = config.get('workflow');

    // Initialize an http client
    this.httpWrapper = new HttpClientWrapper();
  }

  /**
   * Sets the new time range
   * @param startTimeSec the start time in epoch seconds
   * @param endTimeSec the end time in epoch seconds
   */
  public setNewTimeRange = (userContext: UserContext, startTimeSec: number, endTimeSec: number) => {
    logger.info(`Setting new Start Time: ${startTimeSec}
        and End Time: ${endTimeSec} for user: ${userContext.userName}`);
    const newTimeRange = normalizeStartEndTimes(startTimeSec, endTimeSec, this.intervalDurationSec);
    userContext.userCache.setTimeRange(newTimeRange);

    let intervalStartSec = newTimeRange.startTime;
    let intervalEndSec = newTimeRange.endTime;
    const totalDuration = newTimeRange.endTime - newTimeRange.startTime;
    intervalEndSec = newTimeRange.startTime + this.intervalDurationSec;
    for (let i = 0; i < Math.trunc(totalDuration / this.intervalDurationSec) + 1; i++) {
      // check to see if interval exists in  given time range
      if (
        !CacheProcessor.Instance()
          .getWorkflowData()
          .intervals.find(
            interval => interval.startTime >= intervalStartSec && interval.endTime <= intervalEndSec
          )
      ) {
        this.populateInterval(userContext, intervalStartSec, intervalEndSec);
      }
      intervalStartSec += this.intervalDurationSec;
      intervalEndSec += this.intervalDurationSec;
    }
  }

  /**
   * Retrieves current open activity Id.
   * @returns the current open activity's id or empty string if none open
   */
  public getCurrentOpenActivityId(userContext: UserContext): string {
    if (CacheProcessor.Instance().getCurrentOpenActivity()) {
      return CacheProcessor.Instance().getCurrentOpenActivity().id;
    }
    return '';
  }

  /**
   * Retrieves current open stage interval Id.
   * @returns the current open stage interval's id or empty string if none open
   */
  public getCurrentStageIntervalId(userContext: UserContext): string {
    if (CacheProcessor.Instance().getCurrentOpenActivity()) {
      return CacheProcessor.Instance().getCurrentOpenActivity().stageIntervalId;
    }
    return '';
  }

  /**
   * Retrieves the list of processing stages defined for the interactive analysis.
   * @returns a ProcessingStage[] as a promise
   */
  public async getStages(userContext: UserContext): Promise<model.ProcessingStage[]> {
    const userTimeRange = userContext.userCache.getTimeRange();
    logger.debug(`Getting stages from ${userTimeRange.startTime} to ${userTimeRange.endTime}`);
    return CacheProcessor.Instance().getWorkflowData().stages;
  }

  /**
   * Retrieves the processing stage with the provided name string.
   * @param name The name of the processing stage to retrieve
   * @returns a ProcessingStage as a promise
   */
  public async getStageByName(
    userContext: UserContext,
    name: string
  ): Promise<model.ProcessingStage> {
    return find(CacheProcessor.Instance().getWorkflowData().stages, { name });
  }

  /**
   * Retrieves the processing intervals in the provided time range.
   * @param startTime The start time of the range for which to retrieve intervals
   * @param endTime The end time of the range for which to retrieve intervals
   * @returns a ProcessingInterval[] as a promise
   */
  public async getIntervalsInRange(
    userContext: UserContext,
    startTime: number,
    endTime: number
  ): Promise<model.ProcessingInterval[]> {
    const normalizedTime = normalizeStartEndTimes(startTime, endTime, this.intervalDurationSec);
    const intervals = filter(
      CacheProcessor.Instance().getWorkflowData().intervals,
      interval =>
        interval.startTime >= normalizedTime.startTime && interval.endTime <= normalizedTime.endTime
    );
    return intervals;
  }

  /**
   * Retrieves the processing interval with the provided ID string.
   * @param id The unique ID of the processing interval to retrieve
   * @returns a ProcessingInterval as a promise
   */
  public async getInterval(
    userContext: UserContext,
    id: string
  ): Promise<model.ProcessingInterval> {
    const found = find(CacheProcessor.Instance().getWorkflowData().intervals, { id });
    return found;
  }

  /**
   * Retrieves the list of processing stage intervals which are within the workflows time range
   * @returns a ProcessingStageInterval[] as a promise
   */
  public async getStageIntervals(
    userContext: UserContext,
    id: string
  ): Promise<model.ProcessingStageInterval[]> {
    const userTimeRange = userContext.userCache.getTimeRange();
    return filter(
      CacheProcessor.Instance().getWorkflowData().stageIntervals,
      stageInterval =>
        stageInterval.startTime >= userTimeRange.startTime &&
        stageInterval.endTime <= userTimeRange.endTime
    ).sort((a, b) => a.startTime - b.endTime);
  }

  /**
   * Retrieves the processing stage interval with the provided ID string.
   * @param id The unique ID of the processing stage interval to retrieve
   * @returns a ProcessingStageInterval as a promise
   */
  public async getStageInterval(
    userContext: UserContext,
    id: string
  ): Promise<model.ProcessingStageInterval> {
    return find(CacheProcessor.Instance().getWorkflowData().stageIntervals, { id });
  }

  /**
   * Retrieves the list of processing activities defined for interactive processing.
   * @returns a ProcessingActivity[] as a promise
   */
  public async getActivities(userContext: UserContext): Promise<model.ProcessingActivity[]> {
    return CacheProcessor.Instance().getWorkflowData().activities;
  }

  /**
   * Retrieves the processing activity with the provided ID string.
   * @param id The unique ID of the processing activity to retrieve
   * @returns a ProcessingActivity as a promise
   */
  public async getActivity(
    userContext: UserContext,
    id: string
  ): Promise<model.ProcessingActivity> {
    return find(CacheProcessor.Instance().getWorkflowData().activities, { id });
  }

  /**
   * Retrieves the list of processing activity intervals.
   * @returns a ProcessingActivityInterval[] as a promise
   */
  public async getActivityIntervals(
    userContext: UserContext
  ): Promise<model.ProcessingActivityInterval[]> {
    return CacheProcessor.Instance().getWorkflowData().activityIntervals;
  }

  /**
   * Retrieves the processing activity interval with the provided ID string.
   * @param id The unique ID of the processing activity interval to retrieve
   * @returns a ProcessingActivityInterval as a promise
   */
  public async getActivityInterval(
    userContext: UserContext,
    id: string
  ): Promise<model.ProcessingActivityInterval> {
    return find(CacheProcessor.Instance().getWorkflowData().activityIntervals, { id });
  }

  /**
   * Retrieves the processing stage intervals in the provided time range.
   * @param startTime The start time of the range for which to retrieve stage intervals
   * @param endTime The end time of the range for which to retrieve stage intervals
   * @returns a ProcessingStageInterval[] as a promise
   */
  public async getStageIntervalsInRange(
    userContext: UserContext,
    startTime: number,
    endTime: number
  ): Promise<model.ProcessingStageInterval[]> {
    const normalizedTime = normalizeStartEndTimes(startTime, endTime, this.intervalDurationSec);
    return filter(
      CacheProcessor.Instance().getWorkflowData().stageIntervals,
      stageInterval =>
        stageInterval.startTime >= normalizedTime.startTime &&
        stageInterval.endTime <= normalizedTime.endTime
    );
  }

  /**
   * Updates the processing stage interval object with the provided unique ID
   * to reflect the status information in the input parameter, including
   * interval status and the id of the Analyst requesting the update.
   * @param stageIntervalId The unique ID of the stage interval to mark
   * @param input The marking input to apply to the stage interval
   * @returns a ProcessingStageInterval as promise
   */
  public async markStageInterval(
    userContext: UserContext,
    stageIntervalId: string,
    input: any
  ): Promise<model.ProcessingStageInterval> {
    const stageInterval = find(CacheProcessor.Instance().getWorkflowData().stageIntervals, {
      id: stageIntervalId
    });
    if (!stageInterval) {
      throw new Error(`Couldn't find Processing Stage Interval with ID ${stageIntervalId}`);
    }
    // Check that the status update is valid for the provided ProcessingStageInterval and
    // associated ProcessingActivityIntervals (throw an error otherwise)
    this.validateStageIntervalStatus(userContext, stageInterval, input.status);

    // If the status is InProgress, update the status of each activity interval and
    // Add the provided analyst user name to the list of active analysts (if not already in the list)
    if (input.status === model.IntervalStatus.InProgress) {
      filter(CacheProcessor.Instance().getWorkflowData().activityIntervals, {
        stageIntervalId: stageInterval.id
      }).forEach((currentValue, index) => {
        this.updateActivityIntervalStatus(userContext, currentValue, input.status);
      });
    }

    // Update the stage status
    this.updateStageIntervalStatus(userContext, stageInterval, input.status);
    return stageInterval;
  }

  /**
   * Saves all events and sds
   */
  public async saveIntervalChanges(userContext: UserContext): Promise<DataPayload> {
    const modifiedEvents = EventProcessor.Instance().getModifiedEvents(userContext);
    const eventsToSave = modifiedEvents
      // filter the events that are in conflict
      .filter(event => !event.hasConflict);

    if (eventsToSave.length !== modifiedEvents.length) {
      logger.info(
        `The following events are in conflict (skipping save): ` +
          `${String(modifiedEvents.filter(event => event.hasConflict).map(event => event.id))}`
      );
    }

    const signalDetectionsToSave = SignalDetectionProcessor.Instance().getModifiedSds(userContext);

    // Save signal detections, channel segments, and events
    const { events, signalDetections } = await EventProcessor.Instance().save(
      userContext,
      signalDetectionsToSave,
      eventsToSave
    );
    return createDataPayload(events, signalDetections, []);
  }

  /**
   * Updates the processing activity interval object with the provided unique ID
   * to reflect the status information in the input parameter, including
   * interval status and the id of the Analyst requesting the update.
   * @param stageIntervalId The unique ID of the stage interval to mark
   * @param input The marking input to apply to the stage interval
   * @returns a ProcessingActivityInterval as a promise
   */
  public async markActivityInterval(
    userContext: UserContext,
    activityIntervalId: string,
    input: any
  ): Promise<model.ProcessingActivityInterval> {
    const activityInterval = find(CacheProcessor.Instance().getWorkflowData().activityIntervals, {
      id: activityIntervalId
    });
    if (!activityInterval) {
      throw new Error(`Couldn't find Processing Activity Interval with ID ${activityIntervalId}`);
    }

    // Check that the transition to the input status is valid for the provided ProcessingActivityInterval
    this.validateActivityIntervalStatus(userContext, activityInterval, input.status);

    // Update the activity interval status
    this.updateActivityIntervalStatus(userContext, activityInterval, input.status);

    if (input.status === model.IntervalStatus.InProgress) {
      // Find the parent stage and update its status
      const stage = find(CacheProcessor.Instance().getWorkflowData().stageIntervals, {
        id: activityInterval.stageIntervalId
      });
      this.updateStageIntervalStatus(userContext, stage, input.status);
      CacheProcessor.Instance().setCurrentOpenActivity(activityInterval);
    }

    return activityInterval;
  }

  /**
   * Loads SDs and Events for the interval passed in.
   * @param activityInterval Interval to load data for
   * @returns a ProcessingActivityInterval as a promise
   */
  public async loadSDsAndEvents(
    userContext: UserContext,
    activityInterval: model.ProcessingActivityInterval
  ) {
    const stage = find<model.ProcessingStageInterval>(
      CacheProcessor.Instance().getWorkflowData().stageIntervals,
      { id: activityInterval.stageIntervalId }
    );
    const startTimePadding: number = ConfigProcessor.Instance().getConfigByKey('extraLoadingTime');
    const eventPadding: number = ConfigProcessor.Instance().getConfigByKey('extraEventLoadingTime');
    const startTime: number = stage.startTime;
    const endTime: number = stage.endTime;

    const sdTimeRange = {
      startTime: startTime - startTimePadding,
      endTime: endTime + startTimePadding
    };
    const eventTimeRange = {
      startTime: startTime - eventPadding,
      endTime
    };

    logger.info(
      `Loading signal detections for activity ${activityInterval.status}` +
        ` for interval ${toOSDTime(sdTimeRange.startTime)} to ${toOSDTime(sdTimeRange.endTime)}`
    );
    await SignalDetectionProcessor.Instance().getSignalDetectionsForDefaultStations(
      userContext,
      sdTimeRange
    );
    logger.info(`Loading events for activity ${activityInterval.status}`);
    await EventProcessor.Instance().loadEventsInTimeRange(userContext, eventTimeRange);
    logger.info(
      `Finished loading signal detections and events for activity ${activityInterval.status}`
    );
  }

  /**
   * Retrieves the processing activities for the processing stage with the provided unique ID.
   * @param stageId The unique ID of the processing stage to retrieve activities for
   * @returns a ProcessingActivity as a promise
   */
  public async getActivitiesByStage(
    userContext: UserContext,
    stageId: string
  ): Promise<model.ProcessingActivity[]> {
    return filter(CacheProcessor.Instance().getWorkflowData().activities, { stageId });
  }

  /**
   * Retrieves the processing stage intervals for the processing stage with the provided unique ID.
   * If provided, the optional timeRange parameter constrains the results to those
   * intervals falling between timeRange.startTime (inclusive) and timeRange.endTime (exclusive)
   * @param stageId The unique ID of the stage to retrieve intervals for
   * @param timeRange The time range object for which to retrieve intervals
   * @returns a ProcessingStageInterval[] as a promise
   */
  public async getIntervalsByStage(
    userContext: UserContext,
    stageId: string,
    timeRange: any
  ): Promise<model.ProcessingStageInterval[]> {
    const userTimeRange = userContext.userCache.getTimeRange();
    if (timeRange) {
      return filter(
        CacheProcessor.Instance().getWorkflowData().stageIntervals,
        (stageInterval: model.ProcessingStageInterval) =>
          stageInterval.stageId === stageId &&
          stageInterval.startTime >= timeRange.startTime &&
          stageInterval.endTime < timeRange.endTime
      );
    }

    const intervals = CacheProcessor.Instance()
      .getWorkflowData()
      .stageIntervals.filter(
        interval =>
          interval.stageId === stageId &&
          interval.startTime >= userTimeRange.startTime &&
          interval.endTime <= userTimeRange.endTime
      );
    return intervals;
  }

  /**
   * Retrieves the activity intervals for the processing activity with the provided unique ID.
   * @param activityId: The unique ID of the processing activity to retrieve activity intervals for
   * @returns ProcessingActivityInterval[] as a promise
   */
  public async getIntervalsByActivity(
    userContext: UserContext,
    activityId: string
  ): Promise<model.ProcessingActivityInterval[]> {
    return filter(CacheProcessor.Instance().getWorkflowData().activityIntervals, { activityId });
  }

  /**
   * Retrieves the processing stage intervals for the provided interval ID
   * @param intervalId The unique ID of the interval for which to retrieve processing intervals
   * @returns a ProcessingStageInterval[] as a promise
   */
  public async getStageIntervalsByInterval(
    userContext: UserContext,
    intervalId: string
  ): Promise<model.ProcessingStageInterval[]> {
    return filter(CacheProcessor.Instance().getWorkflowData().stageIntervals, { intervalId });
  }

  /**
   * Retrieves the processing activity intervals for the provided processing stage interval
   * with the provided unique ID.
   * @param stageIntervalId The unique ID of the processing stage interval for which to retrieve activity intervals
   * @returns a ProcessingActivityInterval[] as a promise
   */
  public async getActivityIntervalsByStageInterval(
    userContext: UserContext,
    stageIntervalId: string
  ): Promise<model.ProcessingActivityInterval[]> {
    return filter(CacheProcessor.Instance().getWorkflowData().activityIntervals, {
      stageIntervalId
    });
  }

  /**
   * This method enforces status transition rules for ProcessingStageIntervals and associated
   * ProcessingActivityIntervals, throwing an Error for invalid transitions, which include:
   * ProcessingStageInterval:
   *  - NotStarted -> Complete
   * ProcessingActivityInterval
   *  - NotStarted -> Complete
   *  - NotStarted -> NotComplete
   *  - Complete -> NotComplete
   *  - NotComplete -> Complete
   * @param stageInterval The ProcessingStageInterval the status update would be applied to
   * @param status The ProcessingStageInterval the status update would be applied to
   */
  public validateStageIntervalStatus(
    userContext: UserContext,
    stageInterval: model.ProcessingStageInterval,
    status: model.IntervalStatus
  ) {
    // Prevent all stage transitions to NotStarted (only valid for activity intervals)
    if (status === model.IntervalStatus.NotComplete) {
      throw new Error(
        `Invalid stage status transition (* to NotComplete) ` +
          `for stage with ID: ${stageInterval.id}`
      );

      // Prevent status transitions from NotStarted directly to Complete
    }

    if (status === model.IntervalStatus.Complete) {
      if (stageInterval.status === model.IntervalStatus.NotStarted) {
        throw new Error(
          `Invalid stage status transition (NotStarted to Complete) ` +
            `for stage with ID: ${stageInterval.id}`
        );
      }

      // Prevent stage status transitions to Complete if any of the associated activities
      // has a status other than Complete or NotComplete (i.e. InProgress or NotStarted)
      filter(CacheProcessor.Instance().getWorkflowData().activityIntervals, {
        stageIntervalId: stageInterval.id
      }).forEach(currentValue => {
        if (
          currentValue.status !== model.IntervalStatus.Complete &&
          currentValue.status !== model.IntervalStatus.NotComplete
        ) {
          const activity = CacheProcessor.Instance()
            .getWorkflowData()
            .activities.find(act => act.id === currentValue.activityId);
          throw new Error(
            `Cannot transition stage to Complete because ${activity.name} associated ` +
              `with the stage is not complete (${currentValue.status})` +
              `\nActivity ID ${currentValue.id}`
          );
        }
      });

      // Validate the status transition for each associated ProcessingActivityInterval
    } else if (status === model.IntervalStatus.InProgress) {
      filter(CacheProcessor.Instance().getWorkflowData().activityIntervals, {
        stageIntervalId: stageInterval.id
      }).forEach(currentValue => {
        this.validateActivityIntervalStatus(userContext, currentValue, status);
      });
    }
  }

  /**
   * Update the status of the provided ProcessingStageInterval to the provided status
   * @param stageInterval The ProcessingStageInterval to update
   * @param status The new status to apply to the ProcessingStageInterval
   * @param analystUserName The username of the Analyst to associate with the status update
   */
  public updateStageIntervalStatus(
    userContext: UserContext,
    stageInterval: model.ProcessingStageInterval,
    status: model.IntervalStatus
  ) {
    // Set the completed by field if the input status is Complete
    if (status === model.IntervalStatus.Complete) {
      stageInterval.completedByUserName = userContext.userName;
    }

    // Update the status
    stageInterval.status = status;
  }

  /**
   * Update the status of the provided ProcessingActivityInterval to the provided status
   * @param activityInterval The ProcessingActivityInterval to update
   * @param status The new status to apply to the ProcessingActivityInterval
   * @param analystUserName The username of the Analyst to associate with the status update
   */
  public updateActivityIntervalStatus(
    userContext: UserContext,
    activityInterval: model.ProcessingActivityInterval,
    status: model.IntervalStatus
  ) {
    if (status === model.IntervalStatus.Complete || status === model.IntervalStatus.NotComplete) {
      // Set the completed by field to the input analyst user name
      // Note: NotComplete is an alternative for activities where Complete doesn't make sense,
      // so set the completed by field for this case too
      activityInterval.completedByUserName = userContext.userName;
    } else if (status === model.IntervalStatus.InProgress) {
      // Add the provided analyst user name to the list of active analysts (if not already in the list)
      if (activityInterval.activeAnalystUserNames.indexOf(userContext.userName) === -1) {
        activityInterval.activeAnalystUserNames.push(userContext.userName);
      }
    }

    // Update the status
    activityInterval.status = status;
  }

  public async fetchWorkflowData() {
    // Call to populate workflowCache
    const requestConfig = this.settings.backend.services.workflowData.requestConfig;
    const response: HttpResponse<model.WorkflowData> = await this.httpWrapper.request<
      model.WorkflowData
    >(requestConfig);
    CacheProcessor.Instance().setWorkflowData(response.data);
  }

  /**
   * Initialize the class' http wrapper, start the create interval timer and populate the workflow cache
   */
  private initialize() {
    // If service mocking is enabled, initialize the mock backend
    if (this.settings.backend.mock.enable) {
      workflowBackend.initialize(this.httpWrapper.createHttpMockWrapper());
    }
    // Load service configuration settings
    const serviceConfig = config.get('workflow.intervalService');
    this.intervalCreationStartTimeSec = serviceConfig.intervalCreationStartTimeSec;
    this.intervalDurationSec = serviceConfig.intervalDurationSec;
    this.createIntervalDurationSec = serviceConfig.createIntervalDurationSec;

    /* Starts the interval service stub, starting a timer to create
      and publish new ProcessingStageIntervals periodically.
      Set a timer to create stage intervals periodically based on the configured interval span */
    setInterval(this.createInterval, serviceConfig.intervalCreationFrequencyMillis);
  }

  /**
   * This method enforces status transition rules, throwing an Error for invalid transitions, which include:
   * NotStarted -> Complete
   * NotStarted -> NotComplete
   * Complete -> NotComplete
   * NotComplete -> Complete
   * @param activityInterval The ProcessingActivityInterval the status update would be applied to
   * @param status The ProcessingActivityInterval the status update would be applied to
   */
  private validateActivityIntervalStatus(
    userContext: UserContext,
    activityInterval: model.ProcessingActivityInterval,
    status: model.IntervalStatus
  ) {
    // Prevent status transitions from NotStarted or NotComplete to Complete
    if (status === model.IntervalStatus.Complete) {
      if (
        activityInterval.status === model.IntervalStatus.NotStarted ||
        activityInterval.status === model.IntervalStatus.NotComplete
      ) {
        throw new Error(
          `Invalid activity status transition from ${activityInterval.status} ` +
            `to ${status} for activity with ID: ${activityInterval.id}`
        );
      }

      // Prevent status transitions from NotStarted or Complete to NotComplete
    } else if (status === model.IntervalStatus.NotComplete) {
      if (
        activityInterval.status === model.IntervalStatus.NotStarted ||
        activityInterval.status === model.IntervalStatus.Complete
      ) {
        throw new Error(
          `Invalid activity status transition from ${activityInterval.status} ` +
            `to ${status} for activity with ID: ${activityInterval.id}`
        );
      }
    }
  }

  /**
   * Creates a new stage interval ending at the current date/time spanning the time range defined
   * in the configuration.
   * @param startTime in epoch seconds
   * @param endTime in epoch seconds
   * @returns a ProcessingInterval
   */
  private readonly populateInterval = (
    userContext: UserContext,
    startTime: number,
    endTime: number
  ): model.ProcessingInterval => {
    // Create a new ProcessingInterval with the start and end time
    const interval = {
      id: uuid4(),
      startTime,
      endTime,
      stageIntervalIds: []
    };
    CacheProcessor.Instance()
      .getWorkflowData()
      .intervals.push(interval);

    // Create a new ProcessingStageInterval for each stage defined, and add it to the canned data array
    CacheProcessor.Instance()
      .getWorkflowData()
      .stages.forEach(stage => {
        const stageInterval = {
          id: uuid4(),
          startTime,
          endTime,
          completedByUserName: '',
          stageId: stage.id,
          intervalId: interval.id,
          eventCount: 0,
          status: model.IntervalStatus.NotStarted,
          activityIntervalIds: []
        };

        CacheProcessor.Instance()
          .getWorkflowData()
          .stageIntervals.push(stageInterval);
        interval.stageIntervalIds.push(stageInterval.id);

        logger.info(
          `Created new processing stage interval with ID: ${stageInterval.id}, for stage: ${stage.name}`
        );

        // Create a new ProcessingActivityInterval for each activity associated with the stage (by ID), add it
        // to the canned data array, and update the stage interval array of activity interval IDs
        stage.activityIds.forEach(activityId => {
          const activityIntervalId = uuid4();

          CacheProcessor.Instance()
            .getWorkflowData()
            .activityIntervals.push({
              id: activityIntervalId,
              activeAnalystUserNames: [],
              completedByUserName: '',
              timeStarted: undefined,
              eventCount: 0,
              status: model.IntervalStatus.NotStarted,
              activityId,
              stageIntervalId: stageInterval.id
            });

          logger.info(`Created new processing activity interval with ID: ${activityIntervalId}`);

          stageInterval.activityIntervalIds.push(activityIntervalId);
        });
      });

    return interval;
  }

  /**
   * Creates a new stage interval ending at the current date/time spanning the time range defined
   * in the configuration.
   */
  private readonly createInterval = (userContext: UserContext) => {
    // Is it time for a new interval?
    const elapsedTimeSecs = epochSecondsNow() - this.intervalElapsedSecs;
    if (elapsedTimeSecs < this.createIntervalDurationSec) {
      return;
    }
    // Determine the new interval start and end time (based on configured interval span)
    const startTime = this.intervalCreationStartTimeSec;
    const endTime = this.intervalCreationStartTimeSec + this.intervalDurationSec;

    const newInterval = this.populateInterval(userContext, startTime, endTime);

    // Start the next interval at the current end time
    this.intervalCreationStartTimeSec = endTime;

    logger.info(
      `Creating stage and activity intervals for the time span: ${startTime} to ${endTime}`
    );

    // Publish the created interval to GraphQL subscribers
    this.intervalCreated(newInterval);

    // Reset the elapsed time for next interval creation
    this.intervalElapsedSecs = epochSecondsNow();
  }

  /**
   * Publish newly-created ProcessingIntervals to the GraphQL subscription channel
   * and store them in the canned data list.
   * @param interval the interval
   */
  private intervalCreated(interval: model.ProcessingInterval) {
    logger.info(
      `Publishing newly-created ProcessingInterval with ID: ${interval.id} to GraphQL subscribers`
    );

    // Publish the new interval to the subscription channel
    const subConfig = config.get('workflow.subscriptions');
    this.pubsub
      .publish(subConfig.channels.intervalCreated, { intervalCreated: interval })
      .catch(e => logger.warn(e));
  }
}
