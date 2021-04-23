import { classList } from '@gms/ui-util';
import * as React from 'react';
import { ResizeContainerProps } from '.';
import { ResizeContext } from './resize-context';

const defaultHeightPx = 360;
export const ResizeContainer: React.FunctionComponent<React.PropsWithChildren<
  ResizeContainerProps
>> = props => {
  const [height, setHeight] = React.useState(defaultHeightPx);
  const [isResizing, setIsResizing] = React.useState(false);
  const containerRef = React.useRef<HTMLDivElement>();
  return (
    <div
      className={classList(
        {
          'resize-container': true,
          'resize-container--resizing': isResizing
        },
        props.className
      )}
      data-cy={props.dataCy}
      ref={containerRef}
    >
      <ResizeContext.Provider
        value={{
          isResizing,
          height,
          containerHeight: containerRef?.current?.clientHeight,
          setIsResizing,
          setHeight
        }}
      >
        {props.children}
      </ResizeContext.Provider>
    </div>
  );
};
