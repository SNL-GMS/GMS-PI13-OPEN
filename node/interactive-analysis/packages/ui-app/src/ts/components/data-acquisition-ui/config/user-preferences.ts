import { HorizontalDividerSizeRange } from '@gms/ui-core-components/lib/components/divider/horizontal-divider/types';
import { semanticColors } from '~scss-config/color-preferences';
import { gmsLayout } from '~scss-config/layout-preferences';

export interface DataAcquisitionUserPreferences {
  colors: {
    ok: string;
    warning: string;
    strongWarning: string;
  };
  overviewMinContainerRange: HorizontalDividerSizeRange;
  stationStatisticsMinContainerRange: HorizontalDividerSizeRange;
  sohStatusUpdateTime: number;
  transferredFilesGapsUpdateTime: number;
  tableRowHeightPx: number;
  defaultOverviewGroupHeight: number;
  minChartHeightPx: number;
  minChartWidthPx: number;
}

export const dataAcquisitionUserPreferences: DataAcquisitionUserPreferences = {
  colors: {
    ok: semanticColors.dataAcqOk,
    warning: semanticColors.dataAcqWarning,
    strongWarning: semanticColors.dataAcqStrongWarning
  },
  overviewMinContainerRange: {
    minimumTopHeightPx: 145,
    minimumBottomHeightPx: 110
  },
  stationStatisticsMinContainerRange: {
    minimumTopHeightPx: 145,
    minimumBottomHeightPx: 110
  },
  sohStatusUpdateTime: 60000,
  transferredFilesGapsUpdateTime: 600000,
  tableRowHeightPx: 36,
  defaultOverviewGroupHeight: 360,
  minChartWidthPx: gmsLayout.minChartWidthPx,
  minChartHeightPx: gmsLayout.minChartHeightPx
};
