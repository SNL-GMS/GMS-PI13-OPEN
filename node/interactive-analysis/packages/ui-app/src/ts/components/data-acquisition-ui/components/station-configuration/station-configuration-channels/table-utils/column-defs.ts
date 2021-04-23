import { ColumnDefinition } from '@gms/ui-core-components';
import { StationConfigurationRowChannel } from '../../station-configuration-main/types';

// TODO define the generic types for the column definition
export const channelDefs: ColumnDefinition<StationConfigurationRowChannel, {}, {}, {}, {}>[] = [
  {
    headerName: 'Channel',
    field: 'channelName',
    cellStyle: { 'text-align': 'left' },
    resizable: true,
    sortable: true,
    filter: true,
    width: 80
  },
  {
    headerName: 'ID',
    field: 'id',
    cellStyle: { 'text-align': 'left' },
    resizable: true,
    sortable: true,
    filter: true,
    width: 150
  },
  {
    headerName: 'Type',
    field: 'type',
    cellStyle: { 'text-align': 'left' },
    resizable: true,
    sortable: true,
    filter: true,
    width: 100
  },
  {
    headerName: 'System Change Time',
    field: 'systemChangeTime',
    cellStyle: { 'text-align': 'right' },
    resizable: true,
    sortable: true,
    filter: true,
    width: 100
  },
  {
    headerName: 'Actual Change Time',
    field: 'actualChangeTime',
    cellStyle: { 'text-align': 'right' },
    resizable: true,
    sortable: true,
    filter: true,
    width: 100
  },
  {
    headerName: 'Depth',
    field: 'depth',
    cellStyle: { 'text-align': 'right' },
    resizable: true,
    sortable: true,
    filter: true,
    width: 100
  }
];
