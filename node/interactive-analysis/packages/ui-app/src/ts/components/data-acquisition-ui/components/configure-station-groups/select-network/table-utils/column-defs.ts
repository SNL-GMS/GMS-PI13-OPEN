import { ColumnDefinition } from '@gms/ui-core-components';
import { NetworkRow } from '../../types';
import { NetworkModifiedDot } from './cell-renderer-frameworks';

// TODO define the generic types for the column definition
export const selectNetworkDefs: ColumnDefinition<NetworkRow, {}, {}, {}, {}>[] = [
  {
    headerName: '',
    field: 'modified',
    cellStyle: { display: 'flex', 'justify-content': 'center', 'align-items': 'center' },
    width: 20,
    cellRendererFramework: NetworkModifiedDot,
    resizable: true,
    sortable: true,
    filter: true
  },
  {
    headerName: 'Network',
    field: 'network',
    cellStyle: { 'text-align': 'left' },
    resizable: true,
    sortable: true,
    filter: true,
    editable: true
  },
  {
    headerName: 'Status',
    field: 'status',
    cellStyle: { 'text-align': 'left' },
    resizable: true,
    sortable: true,
    filter: true
  },
  {
    headerName: 'Last Modified',
    field: 'modifiedTime',
    cellStyle: { 'text-align': 'left' },
    resizable: true,
    sortable: true,
    filter: true
  }
];
