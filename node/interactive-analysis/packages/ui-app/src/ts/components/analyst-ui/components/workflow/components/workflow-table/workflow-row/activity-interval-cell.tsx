import { WorkflowTypes } from '@gms/common-graphql';
import classNames from 'classnames';
import React from 'react';

/**
 * Interval Props
 */
export interface ActivityIntervalCellProps {
  interval: WorkflowTypes.ProcessingStageInterval;
  activityInterval: WorkflowTypes.ProcessingActivityInterval;
  isSelected: boolean;
  triggerActivityIntervalContextMenu(
    event: React.MouseEvent<HTMLDivElement>,
    interval: WorkflowTypes.ProcessingStageInterval,
    activityInterval: WorkflowTypes.ProcessingActivityInterval,
    activityCellRef: HTMLDivElement
  );
  triggerMarkActivityInterval(
    interval: WorkflowTypes.ProcessingStageInterval,
    activityInterval: WorkflowTypes.ProcessingActivityInterval,
    status: WorkflowTypes.IntervalStatus,
    activityCellRef: HTMLDivElement
  );
}

/**
 * Interval State
 * An activity interval's cell. Mostly identical to stage interval cell
 */
// tslint:disable-next-line: no-empty-interface
export interface ActivityIntervalCellState {}

export class ActivityIntervalCell extends React.Component<
  ActivityIntervalCellProps,
  ActivityIntervalCellState
> {
  /** Ref to activity interval cell */
  private activityIntervalCellRef: HTMLDivElement;

  /**
   * Constructor.
   *
   * @param props The initial props
   */
  public constructor(props: ActivityIntervalCellProps) {
    super(props);
  }

  public render() {
    const cellClass = classNames({
      'interval-cell': true,
      'interval-cell--selected': this.props.isSelected,
      'interval-cell--not-complete':
        this.props.activityInterval.status === WorkflowTypes.IntervalStatus.NotComplete,
      'interval-cell--in-progress':
        this.props.activityInterval.status === WorkflowTypes.IntervalStatus.InProgress,
      'interval-cell--not-started':
        this.props.activityInterval.status === WorkflowTypes.IntervalStatus.NotStarted,
      'interval-cell--complete':
        this.props.activityInterval.status === WorkflowTypes.IntervalStatus.Complete,
      'interval-cell--activity-cell': true
    });
    return (
      <div
        ref={ref => (this.activityIntervalCellRef = ref)}
        key={this.props.interval.startTime}
        data-cy={`${this.props.interval.startTime}-${this.props.activityInterval.activity.name}`}
        className={cellClass}
        onContextMenu={e => {
          this.props.triggerActivityIntervalContextMenu(
            e,
            this.props.interval,
            this.props.activityInterval,
            this.activityIntervalCellRef
          );
        }}
        onDoubleClick={async e => {
          await this.props.triggerMarkActivityInterval(
            this.props.interval,
            this.props.activityInterval,
            WorkflowTypes.IntervalStatus.InProgress,
            this.activityIntervalCellRef
          );
        }}
        title={`${
          this.props.activityInterval.status === WorkflowTypes.IntervalStatus.Complete
            ? this.props.activityInterval.completedBy.userName
            : this.props.activityInterval.activeAnalysts.map(a => a.userName).join(', ')
        }`}
      >
        <span className="workflow-ellipsis">
          {this.props.activityInterval.status === WorkflowTypes.IntervalStatus.Complete
            ? this.props.activityInterval.completedBy.userName
            : this.props.activityInterval.activeAnalysts.map(a => a.userName).join(', ')}
        </span>
      </div>
    );
  }
}
