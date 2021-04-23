import * as React from 'react';
import { DragCell } from '~components/data-acquisition-ui/shared/table/drag-cell';
import { StationStatisticsContext } from '../station-statistics-context';

const rowClass = 'ag-row';

export interface StationStatisticsDragCellProps {
  stationId: string;
}

export const StationStatisticsDragCell: React.FunctionComponent<React.PropsWithChildren<
  StationStatisticsDragCellProps
>> = props => {
  const context = React.useContext(StationStatisticsContext);
  return (
    <DragCell
      getSelectedStationIds={() => context.selectedStationIds}
      setSelectedStationIds={ids => context.setSelectedStationIds(ids)}
      stationId={props?.stationId}
      getSingleDragImage={(e: React.DragEvent) => {
        if (e.target instanceof Element) {
          return e.target.closest(`.${rowClass}`);
        }
      }}
    >
      {props.children}
    </DragCell>
  );
};
