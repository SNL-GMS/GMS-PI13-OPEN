import { SohTypes } from '@gms/common-graphql';
import GoldenLayout from '@gms/golden-layout';
import React from 'react';

/**
 * The type for the data used by the context
 */
export interface SohContextData {
  glContainer: GoldenLayout.Container;
  selectedAceiType?: SohTypes.AceiType;
  quietChannelMonitorStatuses(
    stationName: string,
    channelPairs: SohTypes.ChannelMonitorPair[],
    quietDuration: number,
    comment?: string
  ): void;
  setSelectedAceiType?(aceiType: SohTypes.AceiType): void;
}

/**
 * Instantiate the Context and set up the defaults.
 */
export const SohContext: React.Context<SohContextData> = React.createContext<SohContextData>(
  undefined
);
