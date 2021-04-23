import { Classes, ContextMenu, Intent, NonIdealState, Position, Spinner } from '@blueprintjs/core';
import { IconNames } from '@blueprintjs/icons';
import {
  CacheTypes,
  CommonSubscriptions,
  WorkflowSubscriptions,
  WorkflowTypes
} from '@gms/common-graphql';
import { MILLISECONDS_IN_SECOND } from '@gms/common-util';
import { UILogger } from '@gms/ui-apollo';
import { AnalystWorkspaceTypes } from '@gms/ui-state';
import { addGlForceUpdateOnResize, addGlForceUpdateOnShow, Toaster } from '@gms/ui-util';
import classNames from 'classnames';
import isEqual from 'lodash/isEqual';
import React from 'react';
import { updateApolloCacheFromDataPayload } from '~analyst-ui/react-apollo-components/mutations';
import {
  ActivityIntervalBlueprintContextMenu,
  StageIntervalBlueprintContextMenu,
  WorkflowMenuBar,
  WorkflowTable,
  WorkflowTimeAxis
} from './components';
import { WorkflowProps, WorkflowState } from './types';

/**
 * Workflow classes used to access two hour blocks of data and to let users know who in working each block
 * Also displays the state of each block (not started, in progress, complete)
 */
export class Workflow extends React.Component<WorkflowProps, WorkflowState> {
  /** The toaster reference for user notification pop-ups */
  private static readonly toaster: Toaster = new Toaster(Position.BOTTOM_RIGHT);

  /** The height of a workflow block in pixels */
  public static BLOCK_HEIGHT_PX: number = 24;

  /** Number of pixels horizontally per hour */
  public static PIXELS_PER_HOUR: number = 53;

  /** The element containing the main workflow time-blocks. Necessary due to scroll synchronization */
  private readonly timeBlockContainer: HTMLDivElement;

  /** A reference to the WorkflowTimeAxis. Necessary due to scroll synchronization */
  private timeAxis: WorkflowTimeAxis;

  /** Handlers to unsubscribe from apollo subscriptions */
  private readonly unsubscribeHandlers: { (): void }[] = [];

  /** Unsubscribe data play load handler */
  private unsubscribeDataPayloadHandler: ZenObservable.Subscription;

  // ***************************************
  // BEGIN REACT COMPONENT LIFECYCLE METHODS
  // ***************************************

  /**
   * Constructor.
   *
   * @param props The initial props
   */
  public constructor(props: WorkflowProps) {
    super(props);
    this.state = {
      expansionStates: [],
      startTimeSecs: 0,
      endTimeSecs: 1,
      intervalDurationSecs: 7200
    };
  }

  /**
   * Updates the derived state from the next props.
   *
   * @param nextProps The next (new) props
   * @param prevState The previous state
   */
  public static getDerivedStateFromProps(nextProps: WorkflowProps, prevState: WorkflowState) {
    if (nextProps && nextProps.stagesQuery.stages) {
      let startTimeSecs = Infinity;
      let endTimeSecs = -Infinity;
      nextProps.stagesQuery.stages.forEach(stage => {
        stage.intervals.forEach(interval => {
          if (interval.startTime < startTimeSecs) {
            startTimeSecs = interval.startTime;
          }
          if (interval.endTime > endTimeSecs) {
            endTimeSecs = interval.endTime;
          }
        });
      });
      let expansionStates;
      if (prevState.expansionStates.length <= 0) {
        expansionStates = nextProps.stagesQuery.stages.map(stage => ({
          stageName: stage.name,
          expanded: !!stage.activities.length
        }));
      } else {
        expansionStates = prevState.expansionStates;
      }
      return {
        expansionStates,
        startTimeSecs,
        endTimeSecs
      };
    }
    // return null to indicate no change to state.
    return null;
  }

  /**
   * Invoked when the component mounted.
   */
  public componentDidMount() {
    addGlForceUpdateOnShow(this.props.glContainer, this);
    addGlForceUpdateOnResize(this.props.glContainer, this);
    this.setupSubscriptions();
  }

  /**
   * Invoked when the component will unmount.
   */
  public componentWillUnmount() {
    // unsubscribe from all current subscriptions
    this.unsubscribeHandlers.forEach(unsubscribe => unsubscribe());
    this.unsubscribeHandlers.length = 0;
  }

  /**
   * React component lifecycle
   * On mount, scroll all the way to the right
   */
  public componentDidUpdate(prevProps: WorkflowProps) {
    if (
      this.props.currentTimeInterval &&
      !isEqual(this.props.currentTimeInterval, prevProps.currentTimeInterval)
    ) {
      this.setupSubscriptions();
    }

    // the first time we get data, set the scroll position to the right
    if (!prevProps.stagesQuery.stages && this.timeBlockContainer) {
      this.timeBlockContainer.scrollLeft =
        this.timeBlockContainer.scrollWidth - this.timeBlockContainer.clientWidth;
    } else {
      // otherwise, only scroll to the right if the user is already scrolled all the way right.
      if (
        this.timeBlockContainer &&
        this.timeBlockContainer.scrollLeft ===
          this.timeBlockContainer.scrollWidth -
            this.timeBlockContainer.clientWidth -
            Workflow.PIXELS_PER_HOUR * 2
      ) {
        this.timeBlockContainer.scrollLeft =
          this.timeBlockContainer.scrollWidth - this.timeBlockContainer.clientWidth;
      }
    }
  }

  /**
   * Renders the component.
   */
  public render() {
    // if the golden-layout container is not visible, do not attempt to render
    // the component, this is to prevent JS errors that may occur when trying to
    // render the component while the golden-layout container is hidden
    if (this.props.glContainer) {
      if (this.props.glContainer.isHidden) {
        return <NonIdealState />;
      }
    }

    if (this.props.stagesQuery.loading) {
      return (
        <div className={'gms-workflow-loading-container'}>
          <Spinner intent={Intent.PRIMARY} />
        </div>
      );
    }

    if (this.props.stagesQuery.error) {
      return (
        <div className={classNames('gms-workflow-loading-container', Classes.INTENT_DANGER)}>
          <NonIdealState
            icon={IconNames.ERROR}
            action={<Spinner intent={Intent.DANGER} />}
            className={Classes.INTENT_DANGER}
            title="Something went wrong!"
            description={this.props.stagesQuery.error.message}
          />
        </div>
      );
    }

    let stagesWithSortedIntervals: WorkflowTypes.ProcessingStage[] = [];
    if (this.props.stagesQuery.stages) {
      stagesWithSortedIntervals = this.props.stagesQuery.stages.slice(0);
      stagesWithSortedIntervals = stagesWithSortedIntervals.map(stage => {
        const intervals = [...stage.intervals].sort((a, b) => a.startTime - b.startTime);
        const newStage: WorkflowTypes.ProcessingStage = { ...stage, intervals };
        return newStage;
      });
    }
    const currentStageId = this.props.currentStageInterval
      ? this.props.currentStageInterval.interval.activityInterval.id
      : undefined;
    return (
      <div
        style={{
          padding: '0.25rem',
          height: '100%',
          width: '100%',
          userSelect: 'none'
        }}
      >
        <div className="workflow-wrapper">
          <WorkflowMenuBar
            setExpanded={this.setExpanded}
            expansionStates={this.state.expansionStates}
            startTimeSecs={this.state.startTimeSecs}
            endTimeSecs={this.state.endTimeSecs}
            onNewInterval={this.onNewTimeInterval}
            onToast={this.onWorkflowToast}
            glContainer={this.props.glContainer}
          />
          <WorkflowTable
            stages={stagesWithSortedIntervals}
            reportStageExpansion={this.reportStageExpansion}
            currentlySelectedIntervalId={currentStageId}
            startTimeSecs={this.state.startTimeSecs}
            endTimeSecs={this.state.endTimeSecs}
            pixelsPerHour={Workflow.PIXELS_PER_HOUR}
            showStageIntervalContextMenu={this.showStageIntervalContextMenu}
            showActivityIntervalContextMenu={this.showActivityIntervalContextMenu}
            markActivityInterval={this.markActivityInterval}
            expansionStates={this.state.expansionStates}
            onScroll={this.onWorkflowScroll}
          />
          <WorkflowTimeAxis
            ref={ref => {
              this.timeAxis = ref;
            }}
            pixelsPerHour={Workflow.PIXELS_PER_HOUR}
            startTimeSecs={this.state.startTimeSecs}
            endTimeSecs={this.state.endTimeSecs}
            intervalDurationSecs={this.state.intervalDurationSecs}
          />
        </div>
      </div>
    );
  }

  // ***************************************
  // END REACT COMPONENT LIFECYCLE METHODS
  // ***************************************

  /**
   * Sets state when stages are  collapsed
   *
   * @param stageName name of stage collapsed
   */
  private readonly reportStageExpansion = (stageName: string): void => {
    this.setState({
      expansionStates: this.state.expansionStates.map(expansion => ({
        stageName: expansion.stageName,
        expanded: expansion.stageName === stageName ? !expansion.expanded : expansion.expanded
      }))
    });
  }

  /**
   * Initialize graphql subscriptions on the apollo client
   */
  private readonly setupSubscriptions = (): void => {
    // first, unsubscribe from all current subscriptions
    this.unsubscribeHandlers.forEach(unsubscribe => unsubscribe());
    this.unsubscribeHandlers.length = 0;

    if (this.unsubscribeDataPayloadHandler) {
      this.unsubscribeDataPayloadHandler.unsubscribe();
    }

    // subscribe to interval-created topic
    this.unsubscribeHandlers.push(
      this.props.stagesQuery.subscribeToMore({
        document: WorkflowSubscriptions.stagesChangedSubscription
      })
    );

    if (this.props.client) {
      // subscribe to interval complete for events and SDs changed by complete
      this.unsubscribeDataPayloadHandler = this.props.client
        .subscribe({ query: CommonSubscriptions.dataPayloadSubscription })
        .subscribe((response: { data: { dataPayload: CacheTypes.DataPayload } }) => {
          if (
            (response &&
              response.data &&
              response.data.dataPayload &&
              // ensure that there are incoming changes to the data
              (response.data.dataPayload.events.length > 0 ||
                response.data.dataPayload.sds.length > 0)) ||
            response.data.dataPayload.qcMasks.length > 0
          ) {
            if (this.props.currentStageInterval) {
              updateApolloCacheFromDataPayload(
                this.props.client,
                this.props.currentTimeInterval,
                this.props.analystActivity,
                response.data.dataPayload
              );
            }
          }
        });
    }
  }

  /**
   * Sets state when stages are expanded
   *
   * @param stageName name of stage expanded
   */
  private readonly setExpanded = (expandAll: boolean) => {
    this.setState({
      expansionStates: this.state.expansionStates.map(activity => ({
        stageName: activity.stageName,
        expanded: expandAll
      }))
    });
  }

  /**
   * When the workflow is scrolled, synchs the time axis
   *
   * @param e React.UIEvent
   */
  private readonly onWorkflowScroll = (e: React.UIEvent<HTMLDivElement>) => {
    this.timeAxis.setScrollLeft(e.currentTarget.scrollLeft);
  }

  /**
   * Creates a toast in the workflow
   *
   * @param message Displayed string in the toast
   */
  private readonly onWorkflowToast = (message: string) => {
    Workflow.toaster.toastInfo(message);
  }

  /**
   * Show a context menu for a stage interval block
   */
  private readonly showStageIntervalContextMenu = (
    e: React.MouseEvent<HTMLDivElement>,
    stage: WorkflowTypes.ProcessingStage,
    interval: WorkflowTypes.ProcessingStageInterval,
    activityInterval: WorkflowTypes.ProcessingActivityInterval
  ) => {
    e.preventDefault();
    const markInterval = (status: WorkflowTypes.IntervalStatus) => {
      this.markStageInterval(stage, interval, activityInterval, status);
    };

    // otherwise, use a blueprint one.
    const stageIntervalContextMenu = StageIntervalBlueprintContextMenu(markInterval);
    ContextMenu.show(
      stageIntervalContextMenu,
      {
        left: e.clientX,
        top: e.clientY
      },
      undefined,
      true
    );
  }

  /**
   * Event handle when a new time interval is entered
   *
   * @param startDate new start of interval
   * @param endData new end of interval
   */
  private readonly onNewTimeInterval = (startDate: Date, endDate: Date) => {
    this.props
      .setTimeInterval({
        variables: {
          startTimeSec: Math.trunc(startDate.valueOf() / MILLISECONDS_IN_SECOND),
          endTimeSec: Math.trunc(endDate.valueOf() / MILLISECONDS_IN_SECOND)
        }
      })
      .catch(e => UILogger.Instance().error(`Failed to set time interval: ${e.message}`));
  }

  /**
   * Show a context menu for an activity interval block.
   */
  private readonly showActivityIntervalContextMenu = (
    e: React.MouseEvent<HTMLDivElement>,
    stage: WorkflowTypes.ProcessingStage,
    interval: WorkflowTypes.ProcessingStageInterval,
    activityInterval: WorkflowTypes.ProcessingActivityInterval,
    activityCellRef: HTMLDivElement
  ) => {
    e.preventDefault();
    const markInterval = (status: WorkflowTypes.IntervalStatus) => {
      this.markActivityInterval(stage, interval, activityInterval, status, activityCellRef);
    };

    // otherwise, use a blueprint one.
    const stageIntervalContextMenu = ActivityIntervalBlueprintContextMenu(markInterval);
    ContextMenu.show(
      stageIntervalContextMenu,
      {
        left: e.clientX,
        top: e.clientY
      },
      undefined,
      true
    );
  }

  /**
   * Triggers a mutation to mark an activity interval status
   *
   * @param stage Stage to mark
   * @param interval Interval to mark
   * @param activityInterval ActivityInterval to mark
   * @param status status to change interval to
   */
  private readonly markActivityInterval = (
    stage: WorkflowTypes.ProcessingStage,
    interval: WorkflowTypes.ProcessingStageInterval,
    activityInterval: WorkflowTypes.ProcessingActivityInterval,
    status: WorkflowTypes.IntervalStatus,
    activityCellRef: HTMLDivElement
  ): void => {
    const input: WorkflowTypes.IntervalStatusInput = {
      status
    };
    if (activityInterval.status === WorkflowTypes.IntervalStatus.NotStarted) {
      activityCellRef.className = classNames({
        'interval-cell': true,
        'interval-cell--activity-cell': true,
        'interval-cell--in-progress': true
      });
    }
    this.props
      .markActivityInterval({
        variables: {
          activityIntervalId: activityInterval.id,
          input
        }
      })
      .then(() => {
        this.openActivityInterval(
          stage,
          interval,
          activityInterval,
          activityInterval.activity.name.indexOf('global') >= 0
            ? AnalystWorkspaceTypes.AnalystActivity.globalScan
            : AnalystWorkspaceTypes.AnalystActivity.eventRefinement,
          status
        );
      })
      .catch(e => UILogger.Instance().error(`Failed to open activity interval: ${e.message}`));
  }

  /**
   * Triggers a mutation to mark a stage interval status
   *
   * @param stage Stage to mark
   * @param interval Interval to mark
   * @param analystUserName user who completed interval
   * @param status status to change interval to
   */
  private markStageInterval(
    stage: WorkflowTypes.ProcessingStage,
    interval: WorkflowTypes.ProcessingStageInterval,
    activityInterval: WorkflowTypes.ProcessingActivityInterval,
    status: WorkflowTypes.IntervalStatus
  ) {
    const input: WorkflowTypes.IntervalStatusInput = {
      status
    };
    this.props
      .markStageInterval({
        variables: {
          stageIntervalId: interval.id,
          input
        }
      })
      .catch(e => UILogger.Instance().error(`Failed to mark stage interval: ${e.message}`));
  }

  /**
   * Open an activity Interval, setting the start/end time and other info in redux state.
   *
   * @param id the id
   * @param timeInterval the time interval
   * @param analystActivity the analyst activity
   * @param status Status to set
   */
  private openActivityInterval(
    stage: WorkflowTypes.ProcessingStage,
    interval: WorkflowTypes.ProcessingStageInterval,
    activityInterval: WorkflowTypes.ProcessingActivityInterval,
    analystActivity: AnalystWorkspaceTypes.AnalystActivity = AnalystWorkspaceTypes.AnalystActivity
      .eventRefinement,
    status: WorkflowTypes.IntervalStatus
  ) {
    // If not in progress don't change the current stage time interval
    if (status !== WorkflowTypes.IntervalStatus.InProgress) {
      return;
    }

    // set the stage interval
    this.props.setCurrentStageInterval({
      id: stage.id,
      name: stage.name,
      interval: {
        id: interval.id,
        timeInterval: {
          startTime: interval.startTime,
          endTime: interval.endTime
        },
        activityInterval: {
          id: activityInterval.id,
          name: activityInterval.activity.name,
          analystActivity
        }
      }
    });
  }
}
