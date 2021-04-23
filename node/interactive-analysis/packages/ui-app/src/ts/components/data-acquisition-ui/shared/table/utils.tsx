import { SohTypes } from '@gms/common-graphql';
import { SohStatusSummary } from '@gms/common-graphql/lib/graphql/soh/types';
import { MILLISECONDS_IN_SECOND, setDecimalPrecision } from '@gms/common-util';
import { uniq } from 'lodash';
import React from 'react';
import { dataAcquisitionUserPreferences } from '~components/data-acquisition-ui/config';
import { messageConfig } from '~components/data-acquisition-ui/config/message-config';
import {
  DisabledStationSohContextMenu,
  StationSohContextMenu
} from '../context-menus/stations-cell-context-menu';
import { CellData } from './types';

const nonContributingTooltipMessage =
  messageConfig.tooltipMessages.stationStatistics.nonContributingCell;
const nullTooltipMessage = messageConfig.tooltipMessages.stationStatistics.nullCell;
const notReceivedTooltipMessage = messageConfig.tooltipMessages.stationStatistics.notReceivedCell;

/**
 * Returns the height of a row, based on the user preferences, plus a border.
 * This helps get around a linter bug that doesn't see types for values in preferences
 */
export const getRowHeightWithBorder: () => number = () => {
  const defaultBorderSize = 4;
  const rowHeight: number = dataAcquisitionUserPreferences.tableRowHeightPx;
  return rowHeight + defaultBorderSize;
};

/**
 * Returns the height of a row, based on the user preferences, plus a border.
 * This helps get around a linter bug that doesn't see types for values in preferences
 */
export const getHeaderHeight: () => number = () => {
  const extraHeight = 4;
  const rowHeight: number = getRowHeightWithBorder();
  return rowHeight + extraHeight;
};

export const formatSohValue = (value: number): string => {
  if (isNaN(value) || value === null || value === undefined) {
    return 'Unknown';
  }
  return setDecimalPrecision(value);
};

export const sharedSohTableClasses = `
  with-separated-rows
  ag-theme-dark
  ag-theme-dark--soh
  `;

/**
 * Checks if data is null or undefined
 * @param data data to check
 */
export const isNullData = (data: CellData) => data?.value === null || data?.value === undefined;

/**
 * Checks if status is null or undefined
 * @param data CellData to check
 */
export const isNullStatus = (data: CellData) => data?.status === null || data?.status === undefined;

export enum CellStatus {
  GOOD = 'good',
  MARGINAL = 'marginal',
  BAD = 'bad',
  NON_CONTRIBUTING = 'non-contributing'
}

export enum DataReceivedStatus {
  RECEIVED = 'received',
  NOT_ENOUGH_DATA = 'not-enough-data',
  NOT_RECEIVED = 'not-received'
}

const sohStatusMatchesCellStatus = (statusSummary: SohTypes.SohStatusSummary) =>
  Object.values<string>(CellStatus).includes(statusSummary?.toLowerCase());

/**
 * Return a CellStatus that determines the cell status, or non-contributing
 * @param data the cell data to check
 */
export const getCellStatus = (
  status: SohTypes.SohStatusSummary,
  isContributing: boolean = true
) => {
  if (isContributing) {
    if (sohStatusMatchesCellStatus(status)) {
      return (status.toLowerCase() as unknown) as CellStatus;
    }
  }
  return CellStatus.NON_CONTRIBUTING;
};

/**
 * Interprets the cell data to determine if there were any data problems, and of what type.
 * If data is a number in the case of station stats, check if number is defined.
 * @param data the cell data/number to check
 */
export const getDataReceivedStatus = (data: CellData | number) => {
  if (typeof data === 'number') {
    return data !== undefined ? DataReceivedStatus.RECEIVED : DataReceivedStatus.NOT_ENOUGH_DATA;
  }
  return isNullStatus(data)
    ? DataReceivedStatus.NOT_RECEIVED
    : isNullData(data)
    ? DataReceivedStatus.NOT_ENOUGH_DATA
    : DataReceivedStatus.RECEIVED;
};

/**
 * RECEIVED if any data was received.
 * NOT_ENOUGH_DATA if none were received and at least one has NOT_ENOUGH_DATA.
 * NOT_RECEIVED if none were ever received.
 * @param data a cell to check
 */
export const getDataReceivedStatusRollup = (data: CellData[]) =>
  data.reduce<DataReceivedStatus>((worstStatus: DataReceivedStatus, cell) => {
    const cellStatus = getDataReceivedStatus(cell);
    if (cellStatus === DataReceivedStatus.RECEIVED || worstStatus === DataReceivedStatus.RECEIVED) {
      return DataReceivedStatus.RECEIVED;
    }
    if (
      cellStatus === DataReceivedStatus.NOT_ENOUGH_DATA ||
      worstStatus === DataReceivedStatus.NOT_ENOUGH_DATA
    ) {
      return DataReceivedStatus.NOT_ENOUGH_DATA;
    }
    return DataReceivedStatus.NOT_RECEIVED;
  }, DataReceivedStatus.NOT_RECEIVED);

/**
 * Sets the tooltip based on contributing, null status or null value
 * @param data CellData to check
 */
export const setTooltip = (data: CellData) =>
  isNullStatus(data)
    ? notReceivedTooltipMessage
    : !data.isContributing
    ? nonContributingTooltipMessage
    : isNullData(data)
    ? nullTooltipMessage
    : '';

/**
 * Takes in a times to test and determines the delta time to compare against sohStationStaleTimeMS.
 * returns true if calculated time is greater than sohStationStaleTimeMS
 *
 * @param sohStationStaleTimeMS time limit for stale time to test against
 * @param timeToTest number in seconds gets converted to milliseconds
 */
export const isSohStationStaleTimeMS = (
  timeToTest: number,
  sohStationStaleTimeMS: number
): boolean => {
  // TODO: bring this into a util area
  // get milliseconds for current time
  const currentTime: number = new Date().valueOf();
  // get milliseconds for the time we are testing
  const timeToTestMS: number = timeToTest * MILLISECONDS_IN_SECOND;
  const deltaTime: number = Math.abs(timeToTestMS - currentTime);
  // send back true if we are past the configured time
  return deltaTime > sohStationStaleTimeMS;
};
/**
 * Helper function to determine if any of the selected Station SOH are stale or
 * are already acknowledged
 * @param stationNames selected
 * @returns boolean
 */
export const isAcknowledgeEnabled = (
  stationNames: string[],
  stationSohs: SohTypes.UiStationSoh[],
  sohStationStaleTimeMS: number
) => {
  let isDisabled = false;
  uniq(stationNames).forEach((name: string) => {
    const soh = stationSohs.find(entry => entry.stationName === name);
    if (soh && soh.time) {
      isDisabled = isSohStationStaleTimeMS(soh.time, sohStationStaleTimeMS)
        ? true
        : !soh.needsAcknowledgement || isDisabled;
    } else {
      isDisabled = true;
    }
  });
  return !isDisabled;
};

/**
 * Method returns the appropriate Station SOH Context Menu
 * @param stationNames the selected station names
 * @param stationSohs the station soh data
 * @param sohStationStaleTimeMS the station stale timeout in milliseconds
 * @param acknowledgeCallback the callback for acknowledging
 * @return StationSohContextMenu
 */
export const acknowledgeContextMenu = (
  stationNames: string[],
  stationSohs: SohTypes.UiStationSoh[],
  sohStationStaleTimeMS: number,
  acknowledgeCallback: (stationIds: string[], comment?: string) => void
) => {
  const sohContextMenuProps = {
    stationNames,
    // tslint:disable-next-line: no-unbound-method
    acknowledgeCallback
  };
  return isAcknowledgeEnabled(stationNames, stationSohs, sohStationStaleTimeMS) ? (
    <StationSohContextMenu {...sohContextMenuProps} />
  ) : (
    <DisabledStationSohContextMenu {...sohContextMenuProps} />
  );
};

/**
 * Custom comparator for comparing cell values.
 * Handles undefined
 * @param a the first value
 * @param b the second value
 * @returns number indicating sort order (standard comparator return)
 */
export const compareCellValues = (a: number, b: number) => {
  if (a === undefined && b === undefined) {
    return 0;
  }
  if (a === undefined) {
    return -1;
  }
  if (b === undefined) {
    return 1;
  }
  return a - b;
};

/**
 * Compares statuses
 * @param a first soh status
 * @param b second soh status
 * @returns the worst status
 */
export const getWorseStatus = (a: SohStatusSummary, b: SohStatusSummary): SohStatusSummary => {
  if (a === SohStatusSummary.BAD || b === SohStatusSummary.BAD) {
    return SohStatusSummary.BAD;
  }
  if (a === SohStatusSummary.MARGINAL || b === SohStatusSummary.MARGINAL) {
    return SohStatusSummary.MARGINAL;
  }
  if (a === SohStatusSummary.GOOD || b === SohStatusSummary.GOOD) {
    return SohStatusSummary.GOOD;
  }
  return SohStatusSummary.NONE;
};

export const getWorstCapabilityRollup = (groups: SohTypes.StationSohCapabilityStatus[]) =>
  groups &&
  groups.length &&
  groups.reduce(
    (
      worstFound: SohTypes.SohStatusSummary,
      capabilityStatus: SohTypes.StationSohCapabilityStatus
    ) => getWorseStatus(worstFound, capabilityStatus?.sohStationCapability),
    SohTypes.SohStatusSummary.NONE
  );
