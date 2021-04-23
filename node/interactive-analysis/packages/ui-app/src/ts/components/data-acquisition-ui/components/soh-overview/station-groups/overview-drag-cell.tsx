import * as React from 'react';
import { DragCell } from '~components/data-acquisition-ui/shared/table/drag-cell';
import { SohOverviewContext } from '../soh-overview-context';

const cellClass = 'soh-overview-cell';

export interface OverviewDragCellProps {
  stationId: string;
}

export const OverviewDragCell: React.FunctionComponent<React.PropsWithChildren<
  OverviewDragCellProps
>> = ({ stationId, children }) => {
  const context = React.useContext(SohOverviewContext);
  return (
    <DragCell
      getSelectedStationIds={() => context.selectedStationIds}
      setSelectedStationIds={ids => context.setSelectedStationIds(ids)}
      stationId={stationId}
      getSingleDragImage={(e: React.DragEvent) => {
        if (e.target instanceof Element) {
          return e.target.querySelector(`.${cellClass}`);
        }
      }}
    >
      {children}
    </DragCell>
  );
};
