import { ColumnDefinition } from '@gms/ui-core-components';
import { DataDestinationsRow } from '../types';

// TODO define the generic types for the column definition
export const dataDestinationsDefs: ColumnDefinition<
  DataDestinationsRow,
  DataDestinationsRow,
  {},
  {},
  {}
>[] = [
  {
    headerName: 'Data destination',
    field: 'dataDestinations',
    resizable: true,
    sortable: true,
    filter: true,
    width: 300
  },
  {
    headerName: 'Enable forwarding',
    cellRenderer: params => `<input type='checkbox' ${params.value ? 'checked' : ''} />`,
    field: 'enableForwarding',
    cellStyle: { 'text-align': 'center' },
    resizable: true,
    width: 200
  }
];
