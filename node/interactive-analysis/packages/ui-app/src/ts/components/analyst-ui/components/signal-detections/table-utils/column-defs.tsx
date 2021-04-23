import { setDecimalPrecision } from '@gms/common-util';
import { ColumnDefinition } from '@gms/ui-core-components';
import isEqual from 'lodash/isEqual';
import moment from 'moment';
import { userPreferences } from '~analyst-ui/config';
import { SignalDetectionsRow } from '../types';
import {
  AFiveMeasurementCellRenderer,
  DetectionColorCellRenderer,
  ModifiedDot,
  SignalDetectionConflictMarker
} from './cell-renderer-frameworks';

/**
 * Column Definitions for Signal Detection List
 */
// TODO define the generic types for the column definition
export const columnDefs: ColumnDefinition<SignalDetectionsRow, {}, {}, {}, {}>[] = [
  {
    headerName: '',
    field: 'modified',
    cellStyle: { display: 'flex', 'justify-content': 'center', 'align-items': 'center' },
    width: 20,
    resizable: true,
    sortable: true,
    filter: true,
    cellRendererFramework: ModifiedDot
  },
  {
    cellStyle: { display: 'flex', 'justify-content': 'center' },
    width: 50,
    field: 'color',
    headerName: '',
    resizable: true,
    sortable: true,
    filter: true,
    cellRendererFramework: DetectionColorCellRenderer
  },
  {
    headerName: 'Time',
    field: 'time',
    cellStyle: { 'text-align': 'center' },
    width: 100,
    resizable: true,
    sortable: true,
    filter: true,
    valueFormatter: e =>
      moment
        .unix(e.data.time)
        .utc()
        .format('HH:mm:ss')
  },
  {
    headerName: 'Phase',
    field: 'phase',
    resizable: true,
    sortable: true,
    filter: true,
    cellStyle: { 'text-align': 'left' },
    width: 70
  },
  {
    headerName: 'Station',
    field: 'station',
    width: 70,
    resizable: true,
    sortable: true,
    filter: true,
    cellStyle: { 'text-align': 'left' }
  },
  {
    headerName: 'A5/2',
    field: 'aFiveMeasurement',
    editable: false,
    resizable: true,
    sortable: true,
    filter: true,
    equals: isEqual /* use a deep comparison on the complex object */,
    cellRendererFramework: AFiveMeasurementCellRenderer,
    cellStyle: { margin: '0px', padding: '0px' },
    comparator: (
      valueA: { value: number; requiresReview: boolean },
      valueB: { value: number; requiresReview: boolean },
      nodeA,
      nodeB,
      isInverted
    ) => (valueA.value > valueB.value ? 1 : valueA.value < valueB.value ? -1 : 0),
    valueFormatter: e => setDecimalPrecision(e.data.aFiveMeasurement.value),
    valueGetter: e => e.data.aFiveMeasurement,
    width: 60
  },
  {
    headerName: 'A5/2 Per.',
    field: 'aFivePeriod',
    editable: false,
    resizable: true,
    sortable: true,
    filter: true,
    cellStyle: { 'text-align': 'right' },
    valueFormatter: e => setDecimalPrecision(e.data.aFivePeriod),
    width: 80
  },
  {
    headerName: 'ALR2',
    field: 'alrMeasurement',
    editable: false,
    resizable: true,
    sortable: true,
    filter: true,
    cellStyle: { 'text-align': 'right' },
    valueFormatter: e => setDecimalPrecision(e.data.alrMeasurement),
    width: 60
  },
  {
    headerName: 'ALR2 Per.',
    field: 'alrPeriod',
    editable: false,
    resizable: true,
    sortable: true,
    filter: true,
    cellStyle: { 'text-align': 'right' },
    valueFormatter: e => setDecimalPrecision(e.data.alrPeriod),
    width: 80
  },
  {
    headerName: 'Slowness (s/\u00B0)',
    field: 'slowness',
    editable: false,
    resizable: true,
    sortable: true,
    filter: true,
    cellStyle: { 'text-align': 'right' },
    valueFormatter: e => setDecimalPrecision(e.data.slowness),
    width: 90
  },
  {
    headerName: 'Azimuth (\u00B0)',
    field: 'azimuth',
    tooltipValueGetter: params => 'Reciever to Source Azimuth (\u00B0)',
    editable: false,
    resizable: true,
    sortable: true,
    filter: true,
    cellStyle: { 'text-align': 'right' },
    valueFormatter: e => setDecimalPrecision(e.data.azimuth),
    width: 90
  },
  {
    headerName: 'Time Unc',
    field: 'timeUnc',
    editable: true,
    cellStyle: { 'text-align': 'right' },
    width: 80,
    resizable: true,
    sortable: true,
    valueFormatter: e => setDecimalPrecision(e.data.timeUnc),
    filter: true
  },
  {
    headerName: 'Assoc Evt ID',
    field: 'assocEventId',
    cellStyle: { 'text-align': 'center' },
    width: 100,
    resizable: true,
    sortable: true,
    filter: true,
    hide: !userPreferences.signalDetectionList.showIds,
    valueFormatter: e => (e.data.assocEventId ? e.data.assocEventId : 'N/A')
  },
  {
    headerName: 'Conflict',
    field: 'conflicts',
    cellRendererFramework: SignalDetectionConflictMarker,
    width: 30,
    resizable: true,
    sortable: true,
    filter: true,
    cellStyle: {
      display: 'flex',
      'justify-content': 'center',
      'align-items': 'center',
      padding: '3px 0 0 0'
    }
  },
  {
    headerName: 'ID',
    field: 'id',
    cellStyle: { 'text-align': 'center' },
    resizable: true,
    sortable: true,
    filter: true,
    hide: !userPreferences.signalDetectionList.showIds,
    width: 100
  },
  {
    headerName: 'Hyp ID',
    field: 'hypothesisId',
    cellStyle: { 'text-align': 'center' },
    resizable: true,
    sortable: true,
    filter: true,
    hide: !userPreferences.signalDetectionList.showIds,
    width: 100
  }
];
