import { Icon, Intent, Tooltip } from '@blueprintjs/core';
import { IconNames } from '@blueprintjs/icons';
import React from 'react';
import { gmsColors } from '~scss-config/color-preferences';
import { SDConflictPopup } from '../components/sd-conflict-popup';
import { SDDirtyDotPopup } from '../components/sd-dirty-dot-popup';

/**
 * Renders the A/5 Measurement cell
 * @param props the ag-grid props
 */
export const AFiveMeasurementCellRenderer: React.FunctionComponent<any> = props => (
  <div
    style={{
      margin: 0,
      padding: 0,
      textAlign: 'right',
      height: '25px',
      color: props.value.requiresReview ? gmsColors.gmsRecessed : undefined,
      backgroundColor: props.value.requiresReview
        ? props.node.rowIndex % 2 === 0
          ? gmsColors.gmsTableRequiresReviewEvenRow
          : gmsColors.gmsTableRequiresReviewOddRow
        : 'auto'
    }}
  >
    {props.valueFormatted}
  </div>
);

/**
 * Renders the Detection color cell for the signal detection list
 */
export class DetectionColorCellRenderer extends React.Component<any, {}> {
  public render() {
    return (
      <div
        data-cy="signal-detection-color-swatch"
        style={{
          height: '20px',
          width: '20px',
          position: 'relative',
          top: '1px',
          backgroundColor: this.props.data.color
        }}
      />
    );
  }
}

/**
 * Renders the modified color cell for the signal detection list
 */
export class ModifiedDot extends React.Component<any, {}> {
  public render() {
    return (
      <Tooltip
        content={<SDDirtyDotPopup signalDetection={this.props.data} />}
        className="dirty-dot-wrapper"
      >
        <div
          style={{
            backgroundColor: this.props.data.modified ? gmsColors.gmsMain : 'transparent'
          }}
          className="list-entry-dirty-dot"
        />
      </Tooltip>
    );
  }
}

/**
 * Renders the modified color cell for the signal detection list
 */
export class SignalDetectionConflictMarker extends React.Component<any, {}> {
  public render() {
    return this.props.data.conflicts.length > 1 ? (
      <Tooltip content={<SDConflictPopup sdConflicts={this.props.data.conflicts} />}>
        <Icon icon={IconNames.ISSUE} intent={Intent.DANGER} />
      </Tooltip>
    ) : null;
  }
}
