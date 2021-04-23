import { ColumnDefinition } from '@gms/ui-core-components';
import { StationsRow } from '../../types';

// TODO define the generic types for the column definition
export const availableStationsDefs: ColumnDefinition<StationsRow, {}, {}, {}, {}>[] = [
  {
    headerName: 'Available Stations',
    field: 'stations',
    cellStyle: { 'text-align': 'left' },
    resizable: true,
    sortable: true,
    filter: 'agTextColumnFilter',
    filterParams: {
      filterOptions: ['contains']
    },
    width: 200
  }
];
