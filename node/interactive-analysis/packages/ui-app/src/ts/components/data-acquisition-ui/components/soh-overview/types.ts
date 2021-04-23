import { ConfigurationTypes, SohTypes } from '@gms/common-graphql';
import { ChildMutateProps, MutationFunction } from 'react-apollo';
import { SohReduxProps } from '../../shared/types';
/**
 * SohSummary props
 */
export type SohOverviewProps = SohReduxProps &
  ConfigurationTypes.UIConfigurationQueryProps &
  ChildMutateProps<SohStatusMutations>;

/**
 * Mutations used by StationConfiguration
 */
export interface SohStatusMutations {
  saveStationGroupSohStatus: MutationFunction<{}>;
  acknowledgeSohStatus: MutationFunction<{}>;
}

export interface SohOverviewGridProps {
  statusesToDisplay: string[];
}

/**
 * SohSummary state
 */
export interface SohOverviewState {
  statusCounts: StatusCounts;
}

/**
 * Keeps track of status counts
 */
export interface StatusCounts {
  hasCapabilityRollup: boolean;
  badCount: number;
  marginalCount: number;
  okCount: number;
}

export interface StationSohIssue {
  requiresAcknowledgement: boolean;
  acknowledgedAt: string;
}

export enum FilterableSOHTypes {
  GOOD = SohTypes.SohStatusSummary.GOOD,
  MARGINAL = SohTypes.SohStatusSummary.MARGINAL,
  BAD = SohTypes.SohStatusSummary.BAD,
  NONE = SohTypes.SohStatusSummary.NONE
}

export const FilterableSohTypesDisplayStrings = new Map<string, string>([
  ['GOOD', 'Good'],
  ['MARGINAL', 'Marginal'],
  ['BAD', 'Bad'],
  ['NONE', 'None']
]);

/**
 * The interface for the station group component.
 */
export interface StationGroupProps {
  stationGroupName: string;
  statusCounts: StatusCounts;
  totalStationCount: number;
  // The status and station information used to render a cell in the station group table
  sohStatuses: SohTypes.UiStationSoh[];
  // Status and station information for acknowledgeable cells.
  needsAttentionStatuses: SohTypes.UiStationSoh[];
  isHighlighted: boolean;
  selectedStationIds: string[];
  // Used to trigger a refresh of this group
  groupHeight: number;
  topContainerHeight: number;
  // Changed to trigger a refresh of the group
  setGroupHeight(h: number): void;
  setTopContainerHeight(h: number): void;
  setSelectedStationIds(ids: string[]): void;
}
