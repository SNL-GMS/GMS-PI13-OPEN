import { PopoverPosition, Tooltip } from '@blueprintjs/core';
import * as React from 'react';
import { TooltipProps } from './types';

export const WithTooltip: <T>(
  TheComponent: React.FunctionComponent<T>,
  message: string
) => React.FunctionComponent<TooltipProps<T>> = (
  TheComponent: React.FunctionComponent,
  message: string
) => props => {
  const { position, wrapperTagName, targetTagName, ...rest } = props;
  return (
    <Tooltip
      content={message}
      position={position || PopoverPosition.BOTTOM}
      wrapperTagName={wrapperTagName}
      targetTagName={targetTagName}
    >
      <TheComponent {...rest} />
    </Tooltip>
  );
};

export const TooltipWrapper = <T, >(props: React.PropsWithChildren<TooltipProps<T>>) => (
  <Tooltip
    className={props.className || 'core-tooltip'}
    targetClassName={props.targetClassName || 'core-tooltip__target'}
    content={props.content}
    position={props.position || PopoverPosition.BOTTOM}
    wrapperTagName={props.wrapperTagName || 'div'}
    targetTagName={props.targetTagName || 'div'}
  >
    {props.children}
  </Tooltip>
);
