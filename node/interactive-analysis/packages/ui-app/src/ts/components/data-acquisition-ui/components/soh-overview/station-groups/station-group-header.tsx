import { SohTypes } from '@gms/common-graphql';
import React from 'react';
import { StatusCounts } from '../types';

/**
 * Props for StationGroupHeader
 */
export interface StationGroupHeaderProps {
  statusCounts: StatusCounts;
  displayName: string;
  totalStationCount: number;
  capabilityStatus?: SohTypes.SohStatusSummary;
}

/**
 * * StationGroupHeaderElement
 * Group header component for an soh group. Shows the count of each station status,
 * and the display name of the group. Note, this is a private variable that is memoized
 * for export below to prevent unnecessary renders.
 */
const StationGroupHeaderElement: React.FunctionComponent<StationGroupHeaderProps> = props => (
  <div className="soh-overview-group-header__container" data-cy="soh-overview-header">
    <div
      className="soh-overview-group-header__network"
      data-capability-status={props.capabilityStatus.toLowerCase()}
      title={`Total station count for this group: ${props.totalStationCount}`}
    >
      {props.displayName.replace(/_/g, ' ')}
    </div>
    <div
      className={`soh-overview-group-header__count--${
        props.statusCounts.hasCapabilityRollup
          ? SohTypes.SohStatusSummary.BAD.toLocaleLowerCase()
          : 'none'
      } soh-overview-group-header__count`}
      data-cy={`soh-overview-group-header__count--${SohTypes.SohStatusSummary.BAD.toLocaleLowerCase()}`}
    >
      {props.statusCounts.badCount}
    </div>
    <div
      className={`soh-overview-group-header__count--${
        props.statusCounts.hasCapabilityRollup
          ? SohTypes.SohStatusSummary.MARGINAL.toLocaleLowerCase()
          : 'none'
      } soh-overview-group-header__count`}
      data-cy={`soh-overview-group-header__count--${SohTypes.SohStatusSummary.MARGINAL.toLocaleLowerCase()}`}
    >
      {props.statusCounts.marginalCount}
    </div>
    <div
      className={`soh-overview-group-header__count--${
        props.statusCounts.hasCapabilityRollup
          ? SohTypes.SohStatusSummary.GOOD.toLocaleLowerCase()
          : 'none'
      } soh-overview-group-header__count`}
      data-cy={`soh-overview-group-header__count--${SohTypes.SohStatusSummary.GOOD.toLocaleLowerCase()}`}
    >
      {props.statusCounts.okCount}
    </div>
  </div>
);

export const StationGroupHeader = React.memo(StationGroupHeaderElement);
