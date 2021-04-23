import { toDateTimeString } from '@gms/common-util';
import { ColumnDefinition } from '@gms/ui-core-components';
import React from 'react';
import { QcMaskHistoryRow } from '../types';

/**
 * Column definitions for the overlapping mask table.
 */
// TODO define the generic types for the column definition
export const MASK_HISTORY_COLUMN_DEFINITIONS: ColumnDefinition<
  QcMaskHistoryRow,
  {},
  {},
  {},
  {}
>[] = [
  {
    headerName: '',
    field: 'color',
    cellStyle: { 'text-align': 'left', 'vertical-align': 'middle' },
    width: 30,
    cellRendererFramework: e => (
      <div
        style={{
          height: '10px',
          width: '20px',
          backgroundColor: e.data.color.toString(),
          marginTop: '4px'
        }}
      />
    )
  },
  {
    headerName: 'Category',
    field: 'category',
    cellStyle: { 'text-align': 'left' },
    width: 130
  },
  {
    headerName: 'Type',
    field: 'type',
    cellStyle: { 'text-align': 'left' },
    width: 130
  },
  {
    headerName: 'Start time',
    field: 'startTime',
    cellStyle: { 'text-align': 'left' },
    width: 170,
    valueFormatter: e => toDateTimeString(e.data.startTime)
  },
  {
    headerName: 'End time',
    field: 'endTime',
    cellStyle: { 'text-align': 'left' },
    width: 170,
    valueFormatter: e => toDateTimeString(e.data.endTime)
  },
  {
    headerName: 'Rationale',
    field: 'rationale',
    cellStyle: { 'text-align': 'left' },
    width: 300
  }
];
