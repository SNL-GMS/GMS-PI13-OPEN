import { SohTypes } from '@gms/common-graphql';

/**
 * The data needed to render the cell, passed into the cell render framework
 */
export interface CellData {
  value: number;
  status: SohTypes.SohStatusSummary;
  isContributing: boolean;
}
