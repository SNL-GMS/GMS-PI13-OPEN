import { ColumnDefinition } from '@gms/ui-core-components';
import { StationsRow } from '../../types';

// TODO define the generic types for the column definition
export const associatedStationsDefs: ColumnDefinition<StationsRow, {}, {}, {}, {}>[] = [
  {
    headerName: 'No Network Selected',
    field: 'stations',
    cellStyle: { 'text-align': 'left' },
    resizable: true,
    sortable: true,
    filter: true,
    colId: 'associated_stations'
  }
];
