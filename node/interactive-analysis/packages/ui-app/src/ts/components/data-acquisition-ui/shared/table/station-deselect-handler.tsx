import * as React from 'react';

export interface StationDeselectHandlerProps {
  className?: string;
  dataCy?: string;
  setSelectedStationIds(ids: string[]): void;
}

export const StationDeselectHandler: React.FunctionComponent<React.PropsWithChildren<
  StationDeselectHandlerProps
>> = props => (
  <div
    className={`deselect-handler ${props.className ?? ''}`}
    onKeyDown={e => {
      if (e.nativeEvent.code === 'Escape') {
        props.setSelectedStationIds([]);
      }
    }}
    data-cy={props.dataCy}
  >
    {props.children}
  </div>
);
