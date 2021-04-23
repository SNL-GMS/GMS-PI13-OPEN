import { SohTypes } from '@gms/common-graphql';
import React from 'react';
import { WorstOfBadge } from '~components/data-acquisition-ui/shared/cell/worst-of-badge';
import { OverviewDragCell } from './overview-drag-cell';

const CELL_HEIGHT_PX = 30;

/**
 * The props for the station cell
 */
export interface StationCellProps {
  status: SohTypes.SohStatusSummary;
  selected: boolean;
  needsAttention: boolean;
  name: string;
  capabilityStatus?: SohTypes.SohStatusSummary;
  onClick?(event: React.MouseEvent): void;
  onRightClick(e: React.MouseEvent<HTMLDivElement>);
}

const BaseStationCell = React.forwardRef<HTMLDivElement, React.PropsWithChildren<StationCellProps>>(
  (props, ref: React.Ref<HTMLDivElement>) => (
    <div
      className={`soh-overview-cell
          ${props.needsAttention ? 'soh-overview-cell--draggable' : ''}
          ${props.selected ? 'soh-overview-cell--selected' : ''}
        `}
      data-cy={`soh-overview-cell`}
      data-cy-status={props.capabilityStatus}
      data-soh-status={props.capabilityStatus.toLowerCase()}
      data-station-id={props.name}
      key={props.name}
      ref={ref}
      tabIndex={0}
      title={props.capabilityStatus}
      onContextMenu={e => props.onRightClick(e)}
      onClick={(e: React.MouseEvent) => {
        props.onClick(e);
      }}
    >
      {props.name}
      <WorstOfBadge worstOfSohStatus={props.status} widthPx={CELL_HEIGHT_PX} />
    </div>
  )
);

/**
 * * StationCellElement
 * The station cell component, renders a station name in a colored block,
 * and attaches drag and context menu handlers.
 */
const StationCellElement = React.forwardRef<
  HTMLDivElement,
  React.PropsWithChildren<StationCellProps>
>((props, ref: React.Ref<HTMLDivElement>) => {
  {
    return props.needsAttention ? (
      <OverviewDragCell stationId={props.name}>
        <BaseStationCell ref={ref} {...props} />
      </OverviewDragCell>
    ) : (
      <BaseStationCell {...props} ref={ref} />
    );
  }
});

/**
 * * StationCell
 * Memoized version of station cell component to avoid unnecessary renders
 */
export const StationCell = React.memo(StationCellElement);
