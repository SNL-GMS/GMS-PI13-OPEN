import React from 'react';
import { LabelValueProps } from './types';

export const LabelValue: React.FunctionComponent<LabelValueProps> = props => (
  <div className="label-value-container">
    <div className="label-value__label">{props.label.length !== 0 ? `${props.label}: ` : ''}</div>
    <div
      title={props.tooltip}
      className="label-value__value"
      style={{
        color: props.valueColor ? props.valueColor : '',
        ...props.styleForValue
      }}
    >
      {props.value}
    </div>
  </div>
);
