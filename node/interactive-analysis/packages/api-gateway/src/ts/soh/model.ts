import { SohTypes } from '@gms/common-graphql';
import { SohMonitorType } from '@gms/common-graphql/lib/graphql/soh/types';

export interface AcknowledgedSohStatusChange {
  id: string;
  acknowledgedBy: string;
  acknowledgedAt: string;
  comment?: string;
  acknowledgedChanges: SohStatusChange[];
  acknowledgedStation: string;
}

export interface SohStatusChange {
  firstChangeTime: number;
  sohMonitorType: SohTypes.SohMonitorType;
  changedChannel: string;
}

/**
 * A Quieted Soh status change, that is used to keep track of when a channel
 * monitor was quieted, and how much longer it should be quite.
 */
export interface QuietedSohStatusChange {
  readonly stationName: string;
  readonly sohMonitorType: SohMonitorType;
  readonly channelName: string;
  readonly quietUntil: string;
  readonly quietedBy: string;
  readonly quietDuration: string;
  readonly comment?: string;
}
