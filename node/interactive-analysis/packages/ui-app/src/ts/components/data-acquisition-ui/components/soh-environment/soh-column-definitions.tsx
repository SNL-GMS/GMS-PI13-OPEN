import { stripOutFirstOccurrence } from '@gms/common-util';
import { orderBy } from 'lodash';
import { compareCellValues } from '~components/data-acquisition-ui/shared/table/utils';
import { ChannelCellRenderer, MonitorTypeCellRenderer } from './cell-renderers';
import { getChannelColumnHeaderClass } from './soh-environment-utils';
import {
  ChannelColumnDefinition,
  EnvironmentColumnDefinition,
  MonitorTypeColumnDefinition
} from './types';

const monitorTypeWidthPx = 200;

/** The header name for the monitor type column */
export const headerNameMonitorType = 'Monitor Type';

/**
 * The default column definition settings.
 */
export const defaultColumnDefinition: EnvironmentColumnDefinition = {
  disableStaticMarkupForHeaderComponentFramework: true,
  disableStaticMarkupForCellRendererFramework: true,
  sortable: true,
  filter: true
};

/**
 * Returns the environment table column definitions based on the provided channel data.
 * Also, filters out the channel columns that have a status that is marked as not visible.
 *
 * @param channelNames the channel names
 * @param monitorTypeValueGetter the monitor type value getter
 * @param channelValueGetter the channel value getter
 */
export const getEnvironmentColumnDefinitions = (
  channelsNames: string[],
  monitorTypeValueGetter: (params) => string,
  channelValueGetter: (params) => number
): EnvironmentColumnDefinition[] => {
  const columnDefinitions: EnvironmentColumnDefinition[] = [];

  const headerCellBlockClass = 'soh-header-cell';

  // defines the monitor type column definition
  const monitorTypeColumnDefinition: MonitorTypeColumnDefinition = {
    colId: headerNameMonitorType,
    headerName: headerNameMonitorType,
    cellRendererFramework: MonitorTypeCellRenderer,
    headerClass: `${headerCellBlockClass} ${headerCellBlockClass}--neutral`,
    width: monitorTypeWidthPx,
    pinned: 'left',
    suppressMovable: true,
    sort: 'asc',
    comparator: (a, b) => a?.localeCompare(b),
    valueGetter: monitorTypeValueGetter
  };
  columnDefinitions.push(monitorTypeColumnDefinition);

  const channelColumnDefinitions: ChannelColumnDefinition[] = [];
  orderBy(channelsNames, 'channelName').forEach(name => {
    channelColumnDefinitions.push({
      colId: name,
      headerName: stripOutFirstOccurrence(name),
      field: name,
      width: 160,
      headerClass: params => {
        // use the unique column id (which is the channel name)
        if (params.context.rollupStatusByChannelName) {
          const rollup = params.context.rollupStatusByChannelName.get(params.colDef.colId);
          const colCellData = params.context.dataReceivedByChannelName.get(params.colDef.colId);
          return getChannelColumnHeaderClass(headerCellBlockClass, rollup, colCellData);
        }
        return headerCellBlockClass;
      },
      cellRendererFramework: ChannelCellRenderer,
      comparator: compareCellValues,
      valueGetter: channelValueGetter
    });
  });
  columnDefinitions.push(...channelColumnDefinitions);
  return columnDefinitions;
};
