import { ConfigurationTypes, SohTypes } from '@gms/common-graphql';
import GoldenLayout from '@gms/golden-layout';
import { DataAcquisitionWorkspaceTypes } from '@gms/ui-state';
import React from 'react';

/**
 * History Redux Props
 */
interface EnvironmentReduxProps {
  glContainer?: GoldenLayout.Container;
  selectedStationIds: string[];
  selectedAceiType: SohTypes.AceiType;
  sohStatus: DataAcquisitionWorkspaceTypes.SohStatus;
  setSelectedStationIds(ids: string[]): void;
  setSelectedAceiType(aceiType: SohTypes.AceiType): void;
}

export type EnvironmentHistoryProps = EnvironmentReduxProps &
  ConfigurationTypes.UIConfigurationQueryProps;

export interface EnvironmentHistoryPanelProps {
  station: SohTypes.UiStationSoh;
  channelSohs: SohTypes.ChannelSoh[];
  sohHistoricalDurations: number[];
}

export type AceiMonitorTypeOption = 'CHOOSE_A_MONITOR_TYPE' | SohTypes.AceiType;

/** ACEI selected context data */
export interface AceiContextData {
  selectedAceiType: SohTypes.AceiType;
  setSelectedAceiType(aceiType: SohTypes.AceiType): void;
}

/** ACEI context, provides the selected ACEI type and the function to select the ACEI type */
export const AceiContext: React.Context<AceiContextData> = React.createContext<AceiContextData>(
  undefined
);
