import { H4 } from '@blueprintjs/core';
import React from 'react';
import { TitleBarProps } from './types';

export const TitleBar: React.FunctionComponent<TitleBarProps> = props => (
  <div className={`top-bar ${props.className ? props.className : ''}`}>
    <H4 className="top-bar__title">{props.title}</H4>
    {props.children}
  </div>
);
