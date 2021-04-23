import { ColumnDefinition } from '@gms/ui-core-components';
import { DataSourcesRow } from '../types';

// TODO define the generic types for the column definition
export const dataSourcesDefs: ColumnDefinition<DataSourcesRow, {}, {}, {}, {}>[] = [
  {
    headerName: 'Data source',
    field: 'dataSource',
    cellStyle: { 'text-align': 'left' },
    resizable: true,
    sortable: true,
    filter: true,
    width: 200
  },
  {
    headerName: 'Available formats',
    field: 'availableFormats',
    cellStyle: { 'text-align': 'left' },
    resizable: true,
    sortable: true,
    filter: true,
    width: 300
  }
];
