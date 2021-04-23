import { ConfigurationTypes, SohTypes } from '@gms/common-graphql';
import { ValueType } from '@gms/common-util/lib/types/value-type';
import GoldenLayout from '@gms/golden-layout';
import { DataAcquisitionWorkspaceTypes } from '@gms/ui-state';

/** Represents a type for MISSING and LAG */
export type MISSING_LAG = SohTypes.SohMonitorType.LAG | SohTypes.SohMonitorType.MISSING;

/** The missing/lag history panel props */
export interface MissingLagHistoryPanelProps {
  monitorType: MISSING_LAG;
  station: SohTypes.UiStationSoh;
  sohStatus: DataAcquisitionWorkspaceTypes.SohStatus;
  sohHistoricalDurations: number[];
  valueType: ValueType;
}

interface SohMisLatHistoryComponentProps {
  glContainer?: GoldenLayout.Container;
  selectedStationIds: string[];
  sohStatus: DataAcquisitionWorkspaceTypes.SohStatus;
  type: MISSING_LAG;
  setSelectedStationIds(ids: string[]): void;
}

export type SohMissingLagHistoryComponentProps = SohMisLatHistoryComponentProps &
  ConfigurationTypes.UIConfigurationQueryProps;
