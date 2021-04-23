import { ConfigurationTypes, SohTypes } from '@gms/common-graphql';
import { ValueType } from '@gms/common-util/lib/types/value-type';
import GoldenLayout from '@gms/golden-layout';
import { DataAcquisitionWorkspaceTypes } from '@gms/ui-state';
import { SohStatus } from '@gms/ui-state/lib/state/data-acquisition-workspace/types';
import { ChildMutateProps, MutationFunction } from 'react-apollo';

export type Type =
  | SohTypes.SohMonitorType.TIMELINESS
  | SohTypes.SohMonitorType.MISSING
  | SohTypes.SohMonitorType.LAG;

export type QuietChannelMonitorStatuses = (
  stationName: string,
  channelPairs: SohTypes.ChannelMonitorPair[],
  quietDurationMs: number,
  comment?: string
) => void;

export interface ChannelSohForMonitorType {
  value: number;
  status: SohTypes.SohStatusSummary;
  quietExpiresAt: number;
  quietDurationMs?: number;
  name: string;
  thresholdBad: number;
  thresholdMarginal: number;
  hasUnacknowledgedChanges: boolean;
  isNullData?: boolean;
}

/**
 * Defined mutations
 */
interface Mutations {
  quietChannelMonitorStatuses: MutationFunction<{}>;
}

/**
 * SohBarChartProps props
 */
export type SohBarChartProps = {
  glContainer?: GoldenLayout.Container;
  type: Type;
  selectedStationIds: string[];
  sohStatus: DataAcquisitionWorkspaceTypes.SohStatus;
  valueType: ValueType;
  setSelectedStationIds(ids: string[]): void;
} & ConfigurationTypes.UIConfigurationQueryProps &
  ChildMutateProps<Mutations>;

/**
 * SohBarChartProps props
 */
// tslint:disable-next-line: no-empty-interface
export interface SohBarChartState {}

/**
 * SohBarChartPanelProps props
 */
export interface SohBarChartPanelProps {
  minHeightPx: number;
  type: Type;
  station: SohTypes.UiStationSoh;
  sohStatus: SohStatus;
  uiAnalystConfiguration: ConfigurationTypes.AnalystConfiguration;
  valueType: ValueType;
  quietChannelMonitorStatuses: QuietChannelMonitorStatuses;
}
