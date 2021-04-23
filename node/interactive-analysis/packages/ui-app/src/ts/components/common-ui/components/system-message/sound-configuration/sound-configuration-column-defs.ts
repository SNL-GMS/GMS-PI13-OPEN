import { ColumnDefinition } from '@gms/ui-core-components';
import {
  NotificationsStatusCellRenderer,
  SoundConfigurationDropdownRenderer
} from './sound-configuration-cell-renderer';
import { SoundConfigurationRow } from './types';

export const columnDefs: ColumnDefinition<SoundConfigurationRow, {}, {}, {}, {}>[] = [
  {
    headerName: '',
    field: 'hasNotificationStatusError',
    width: 40,
    minWidth: 40,
    resizable: false,
    suppressMovable: true,
    cellRendererFramework: NotificationsStatusCellRenderer,
    suppressFilter: true,
    suppressSorting: true
  },
  {
    headerName: 'Sound',
    field: 'sound',
    cellRendererFramework: SoundConfigurationDropdownRenderer
  },
  {
    headerName: 'Category',
    field: 'category'
  },
  {
    headerName: 'Subcategory',
    field: 'subcategory'
  },
  {
    headerName: 'Severity',
    field: 'severity'
  },
  {
    headerName: 'Message',
    field: 'message',
    flex: 1,
    minWidth: 300
  }
];
