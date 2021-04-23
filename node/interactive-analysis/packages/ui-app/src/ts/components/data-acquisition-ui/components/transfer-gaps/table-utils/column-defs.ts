import { ColumnDefinition } from '@gms/ui-core-components';
import { TransferGapsRow } from '../types';

/**
 * Column Definitions for Transfer Gaps list
 */
// TODO define the generic types for the column definition
export const columnDefs: ColumnDefinition<TransferGapsRow, {}, {}, {}, {}>[] = [
  {
    headerName: 'Station',
    field: 'name',
    width: 100,
    cellRenderer: 'agGroupCellRenderer',
    resizable: true,
    sortable: true,
    filter: true
  },
  {
    headerName: 'Priority',
    field: 'priority',
    cellStyle: { 'text-align': 'left' },
    editable: true,
    width: 100,
    resizable: true,
    sortable: true,
    filter: true
  },
  {
    headerName: 'Gap Start Time',
    cellStyle: { 'text-align': 'right' },
    width: 220,
    field: 'gapStartTime',
    resizable: true,
    sortable: true,
    filter: true
  },
  {
    headerName: 'Gap End Time',
    field: 'gapEndTime',
    cellStyle: { 'text-align': 'right' },
    width: 220,
    resizable: true,
    sortable: true,
    filter: true
  },
  {
    headerName: 'Duration',
    field: 'duration',
    width: 100,
    cellStyle: { 'text-align': 'right' },
    resizable: true,
    sortable: true,
    filter: true
  },
  {
    headerName: 'Location',
    field: 'location',
    width: 120,
    resizable: true,
    sortable: true,
    filter: true
  }
];
