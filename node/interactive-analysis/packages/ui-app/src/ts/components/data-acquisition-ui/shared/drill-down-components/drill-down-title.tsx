import * as React from 'react';
import { DrillDownTitleProps } from '.';

export const DrillDownTitle: React.FunctionComponent<DrillDownTitleProps> = props => (
  <div className="soh-drill-down-station-label display-title">
    {props.title}
    <div className="display-title__subtitle">{props.subtitle}</div>
  </div>
);
