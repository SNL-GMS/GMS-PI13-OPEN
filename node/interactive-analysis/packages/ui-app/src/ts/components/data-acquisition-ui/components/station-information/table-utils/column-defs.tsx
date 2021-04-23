import { ColumnDefinition } from '@gms/ui-core-components';
import { PopoverStationNamesRow, StationInformationRow } from '../types';
import {
  AutomaticProcessingDropdown,
  DataAcquisitionDropdown,
  InteractiveProcessingDropdown,
  StationInformationModifiedDot
} from './cell-renderer-frameworks';

/**
 * Column Definitions for Station Information List
 */
// TODO define the generic types for the column definition
export const columnDefs: ColumnDefinition<StationInformationRow, {}, {}, {}, {}>[] = [
  {
    headerName: '',
    field: 'modified',
    cellStyle: { display: 'flex', 'justify-content': 'center', 'align-items': 'center' },
    width: 20,
    cellRendererFramework: StationInformationModifiedDot,
    resizable: true,
    sortable: true,
    filter: true
  },
  {
    headerName: 'Station',
    field: 'station',
    cellStyle: { 'text-align': 'left' },
    cellRenderer: 'agGroupCellRenderer',
    editable: false,
    width: 100,
    resizable: true,
    sortable: true,
    filter: true
  },
  {
    headerName: 'Data Acquisition',
    cellStyle: { 'text-align': 'left' },
    width: 200,
    field: 'dataAcquisition',
    cellRendererFramework: DataAcquisitionDropdown,
    resizable: true,
    sortable: true,
    filter: true
  },
  {
    headerName: 'Interactive Processing',
    field: 'interactiveProcessing',
    cellStyle: { 'text-align': 'left' },
    width: 300,
    cellRendererFramework: InteractiveProcessingDropdown,
    resizable: true,
    sortable: true,
    filter: true
  },
  {
    headerName: 'Automatic Processing',
    field: 'automaticProcessing',
    width: 300,
    cellStyle: { 'text-align': 'left' },
    cellRendererFramework: AutomaticProcessingDropdown,
    resizable: true,
    sortable: true,
    filter: true
  }
];

// TODO define the generic types for the column definition
export const popoverTableColumnDefs: ColumnDefinition<PopoverStationNamesRow, {}, {}, {}, {}>[] = [
  {
    headerName: 'Stations',
    field: 'stationName',
    cellStyle: { 'text-align': 'left' },
    width: 100,
    resizable: true,
    sortable: true,
    filter: true
  }
];
