import { classList, getDataAttributesFromProps } from '@gms/ui-util';
import * as React from 'react';
import { ScrollBoxProps } from './types';

export const ScrollBox: React.FunctionComponent<React.PropsWithChildren<
  ScrollBoxProps
>> = props => {
  const dataAttributes = getDataAttributesFromProps(props);
  return (
    <div
      className={classList(
        {
          'scroll-box': true,
          'scroll-box--x': props.orientation === 'x',
          'scroll-box--y': props.orientation === 'y'
        },
        props.className
      )}
      {...dataAttributes}
    >
      {props.children}
    </div>
  );
};
