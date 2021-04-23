import { ConfigurationTypes, SohTypes } from '@gms/common-graphql';
import {
  ChannelMonitorPair,
  ChannelSoh,
  SohStatusSummary
} from '@gms/common-graphql/lib/graphql/soh/types';
import GoldenLayout from '@gms/golden-layout';
import { CellRendererParams, ColumnDefinition, ValueGetterParams } from '@gms/ui-core-components';
import { DataAcquisitionWorkspaceTypes } from '@gms/ui-state';
import React from 'react';
import { ChildMutateProps, MutationFunction } from 'react-apollo';
import { QuietTimingInfo } from '~components/data-acquisition-ui/shared/quiet-indicator';
import { DataReceivedStatus } from '~components/data-acquisition-ui/shared/table/utils';
import { Offset } from '~components/data-acquisition-ui/shared/types';
import { FilterableSOHTypes } from '../soh-overview/types';

/**
 * History Redux Props
 */
interface EnvironmentReduxProps {
  glContainer?: GoldenLayout.Container;
  selectedStationIds: string[];
  sohStatus: DataAcquisitionWorkspaceTypes.SohStatus;
  selectedAceiType: SohTypes.AceiType;
  setSelectedStationIds(ids: string[]): void;
  setSelectedAceiType(aceiType: SohTypes.AceiType): void;
}

/**
 * Mutations used by StationConfiguration
 */
interface EnvironmentMutations {
  quietChannelMonitorStatuses: MutationFunction<{}>;
}

/**
 * SohEnvironment props
 */
export type EnvironmentProps = EnvironmentReduxProps &
  ConfigurationTypes.UIConfigurationQueryProps &
  ChildMutateProps<EnvironmentMutations>;

export interface EnvironmentState {
  monitorStatusesToDisplay: Map<FilterableSOHTypes, boolean>;
  channelStatusesToDisplay: Map<FilterableSOHTypes, boolean>;
}

export interface EnvironmentalSoh {
  value: number;
  status: SohTypes.SohStatusSummary;
  monitorTypes: SohTypes.SohMonitorType;
  channelName: string;
  quietTimingInfo: QuietTimingInfo;
  hasUnacknowledgedChanges: boolean;
  isSelected: boolean;
  isContributing: boolean;
}

export interface EnvironmentTableContext {
  selectedChannelMonitorPairs: ChannelMonitorPair[];
  rollupStatusByChannelName: Map<string, SohStatusSummary>;
  dataReceivedByChannelName: Map<string, DataReceivedStatus>;
}

export interface EnvironmentTableRow {
  id: string;
  monitorType: SohTypes.SohMonitorType;
  monitorIsSelected: boolean;
  monitorStatus: SohTypes.SohStatusSummary;
  valueAndStatusByChannelName: Map<string, EnvironmentalSoh>;
}

export const EnvironmentTableDataContext: React.Context<{
  data: EnvironmentTableRow[];
}> = React.createContext<{
  data: EnvironmentTableRow[];
}>(undefined);

export type EnvironmentColumnDefinition = ColumnDefinition<
  { id: string },
  EnvironmentTableContext,
  {},
  {},
  {}
>;

export type MonitorTypeColumnDefinition = ColumnDefinition<
  { id: string },
  EnvironmentTableContext,
  string,
  {},
  {}
>;

export type MonitorTypeCellRendererParams = CellRendererParams<
  { id: string },
  EnvironmentTableContext,
  string,
  {},
  {}
>;

export type ChannelColumnDefinition = ColumnDefinition<
  { id: string },
  EnvironmentTableContext,
  number,
  {},
  {
    name: string;
    status: SohStatusSummary;
  }
>;

export type ChannelCellRendererParams = CellRendererParams<
  { id: string },
  EnvironmentTableContext,
  number,
  {},
  {
    name: string;
    status: SohStatusSummary;
  }
>;

export type MonitorTypeValueGetterParams = ValueGetterParams<
  {
    id: string;
  },
  EnvironmentTableContext,
  string,
  {},
  {}
>;

export type ChannelValueGetterParams = ValueGetterParams<
  {
    id: string;
  },
  EnvironmentTableContext,
  string,
  {},
  {}
>;

export interface EnvironmentPanelProps {
  channelSohs: ChannelSoh[];
  monitorStatusesToDisplay: Map<FilterableSOHTypes, boolean>;
  channelStatusesToDisplay: Map<FilterableSOHTypes, boolean>;
  defaultQuietDurationMs: number;
  quietingDurationSelections: number[];
  isStale: boolean;
  stationName: string;
}

export interface EnvironmentPanelState {
  selectedChannelMonitorPairs: ChannelMonitorPair[];
}

export interface QuietAction {
  stationName: string;
  channelMonitorPairs: ChannelMonitorPair[];
  position: Offset;
  quietingDurationSelections: number[];
  quietUntilMs: number;
  isStale?: boolean;
  quietChannelMonitorStatuses(
    stationName: string,
    channelPairs: ChannelMonitorPair[],
    quietDurationMs: number,
    comment?: string
  ): void;
}
