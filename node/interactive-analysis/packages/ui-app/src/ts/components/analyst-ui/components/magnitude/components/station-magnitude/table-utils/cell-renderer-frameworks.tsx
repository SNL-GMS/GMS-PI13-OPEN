import { Checkbox, Position, Tooltip } from '@blueprintjs/core';
import React from 'react';
import { messageConfig } from '~analyst-ui/config/message-config';
import { MagnitudeDataForRow } from '../types';

/**
 * Universal tool tip renderer
 * @param props to render the tool tip content and ag grid props
 */
export const ToolTipRenderer: React.FunctionComponent<any> = props => {
  const div = (
    <div
      style={{
        width: `${props.eParentOfValue.clientWidth}px`,
        height: `${props.eParentOfValue.clientHeight}px`
      }}
    >
      {props.valueFormatted}
    </div>
  );

  const children = props.children ? props.children : div;

  return props.tooltip ? (
    <Tooltip content={props.tooltip} position={Position.BOTTOM}>
      {children}
    </Tooltip>
  ) : (
    children
  );
};

export const MagDefiningCheckBoxCellRenderer: React.FunctionComponent<any> = props => {
  const magType = props.magnitudeType;
  const maybeDataForMag: MagnitudeDataForRow = props.data.dataForMagnitude.get(magType);
  const isChecked: boolean = maybeDataForMag ? maybeDataForMag.defining : false;
  const hasAmplitudeForMag = maybeDataForMag && maybeDataForMag.amplitudeValue !== undefined;
  const theCheckbox = (
    <Checkbox
      label=""
      checked={isChecked}
      disabled={
        props.data.historicalMode || props.data.rejectedOrUnnassociated || !hasAmplitudeForMag
      }
      onChange={e => {
        props.data.checkBoxCallback(
          props.magnitudeType,
          props.data.station,
          !maybeDataForMag.defining
        );
      }}
      data-cy={`mag-defining-checkbox-${magType}`}
      title={props.tooltip}
    />
  );

  return (
    <ToolTipRenderer
      {...props}
      tooltip={
        !hasAmplitudeForMag
          ? messageConfig.tooltipMessages.magnitude.noAmplitudeMessage
          : props.tooltip
      }
    >
      {theCheckbox}
    </ToolTipRenderer>
  );
};
