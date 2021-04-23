import { SohTypes } from '@gms/common-graphql';
import GoldenLayout from '@gms/golden-layout';
import { DataAcquisitionWorkspaceTypes } from '@gms/ui-state';
import React from 'react';

export interface SohReduxProps {
  glContainer?: GoldenLayout.Container;
  selectedStationIds: string[];
  sohStatus: DataAcquisitionWorkspaceTypes.SohStatus;
  selectedAceiType: SohTypes.AceiType;
  setSelectedStationIds(ids: string[]): void;
  setSelectedAceiType(aceiType: SohTypes.AceiType): void;
}

export interface Offset {
  left: number;
  top: number;
}

export interface DragCellProps {
  stationId: string;
  getSelectedStationIds(): string[];
  setSelectedStationIds(ids: string[]): void;
  getSingleDragImage(e: React.DragEvent): HTMLElement;
}
