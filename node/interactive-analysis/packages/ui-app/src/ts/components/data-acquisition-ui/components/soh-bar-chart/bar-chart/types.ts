import { ConfigurationTypes, SohTypes } from '@gms/common-graphql';
import { SohMonitorType } from '@gms/common-graphql/lib/graphql/soh/types';
import { ValueType } from '@gms/common-util/lib/types/value-type';
import { SohStatus } from '@gms/ui-state/lib/state/data-acquisition-workspace/types';
import { ChannelSohForMonitorType, QuietChannelMonitorStatuses } from '../types';

export interface BarChartPanelProps {
  minHeightPx: number;
  chartHeaderHeight: number;
  type: SohMonitorType;
  station: SohTypes.UiStationSoh;
  sohStatus: SohStatus;
  channelSoh: ChannelSohForMonitorType[];
  uiAnalystConfiguration: ConfigurationTypes.AnalystConfiguration;
  valueType: ValueType;
  quietChannelMonitorStatuses: QuietChannelMonitorStatuses;
}

export interface ChartData {
  barData: {
    value: {
      y: number;
      x: string;
      quietUntilMs?: number;
      quietDurationMs?: number;
      channelStatus?: SohTypes.SohStatusSummary;
      onContextMenus?: {
        onContextMenuBar(e: React.MouseEvent<any, MouseEvent>, data: any): void;
        onContextMenuBarLabel(e: React.MouseEvent<any, MouseEvent>, index: any): void;
      };
    };
    id: string;
    color: string;
  }[];
  barCategories: {
    x: string[];
    y: any[];
  };
  thresholdsBad: number[];
  thresholdsMarginal: number[];
}
