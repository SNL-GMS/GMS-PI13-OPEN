/**
 * A helper utility that builds buttons for the Table view in the qcMaskDialogBox
 */
import { Button, Icon, Intent } from '@blueprintjs/core';
import { IconNames } from '@blueprintjs/icons';
import { toDateTimeString } from '@gms/common-util';
import { ColumnDefinition, Table } from '@gms/ui-core-components';
import classNames from 'classnames';
import React from 'react';
import { QcMaskHistoryRow } from '../types';

/**
 * Builds a modify or reject button
 *
 * @param params Table (ag-grid) parameters
 *
 * @returns a JSX.Element or null
 */
export function modifyButton(params): JSX.Element | null {
  const modifyIcon = <Icon icon={IconNames.EDIT} title={false} />;
  const viewIcon = <Icon icon={IconNames.EYE_OPEN} title={false} />;

  return !params.value.disabled ? (
    <Button
      onClick={e => {
        e.stopPropagation();
        params.value.onClick(e.clientX, e.clientY, params);
      }}
      className="qc-mask-history-table__button"
      icon={modifyIcon}
      small={true}
      minimal={true}
      title={'modify'}
    />
  ) : (
    <Button
      onClick={e => {
        e.stopPropagation();
        params.value.onClick(e.clientX, e.clientY, params);
      }}
      className="qc-mask-history-table__button"
      icon={viewIcon}
      title={'view'}
      small={true}
      minimal={true}
    />
  );
}
/**
 * Builds a button for selecting qc masks
 *
 * @param Table (ag-grid) parameters
 *
 * @returns a JSX.Element or null
 */
export function selectButton(params) {
  const selectIcon = <Icon icon={IconNames.SELECT} title={false} />;

  return !params.value.disabled ? (
    <Button
      onClick={e => {
        e.stopPropagation();
        params.value.onClick(e.clientX, e.clientY, params);
      }}
      className="qc-mask-history-table__button"
      icon={selectIcon}
      small={true}
      minimal={true}
      title={'select'}
    />
  ) : null;
}
/**
 * Builds a button for opening the qc mask reject dialog
 *
 * @param Table (ag-grid) parameters
 *
 * @returns a JSX.Element or null
 */
export function rejectButton(params) {
  const rejectIcon = <Icon icon={IconNames.CROSS} title={false} />;

  return !params.value.disabled ? (
    <Button
      onClick={e => {
        e.stopPropagation();
        params.value.onClick(e.clientX, e.clientY, params);
      }}
      className="qc-mask-history-table__button qc-mask-history-table__button--reject"
      icon={rejectIcon}
      small={true}
      minimal={true}
      title={'reject'}
      intent={Intent.DANGER}
    />
  ) : null;
}

/**
 * Column definitions for the overlapping mask table.
 */
// TODO define the generic types for the column definition
const OVERLAPPING_MASKS_COLUMN_DEFINITIONS: ColumnDefinition<QcMaskHistoryRow, {}, {}, {}, {}>[] = [
  {
    headerName: '',
    field: 'select',
    cellStyle: { 'text-align': 'left', 'vertical-align': 'middle' },
    width: 25,
    cellRendererFramework: selectButton
  },
  {
    headerName: '',
    field: 'modify',
    cellStyle: { 'text-align': 'left', 'vertical-align': 'middle' },
    width: 25,
    cellRendererFramework: modifyButton
  },
  {
    headerName: '',
    field: 'reject',
    cellStyle: { 'text-align': 'left', 'vertical-align': 'middle' },
    width: 25,
    cellRendererFramework: rejectButton
  },
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
    headerName: 'Author',
    field: 'author',
    cellStyle: { 'text-align': 'left' },
    width: 75
  },
  {
    headerName: 'Rationale',
    field: 'rationale',
    cellStyle: { 'text-align': 'left' },
    width: 300
  }
];

/**
 * Render the Mask table (used for history and overlapping masks).
 *
 * @param tableProps a set of props used by table
 *
 * @returns a JSX.Element with a rendered table
 */
export const renderOverlappingMaskTable = (tableProps: {}) => (
  <div className={classNames('ag-dark', 'qc-mask-overlapping-table')}>
    <div style={{ flex: '1 1 auto', position: 'relative', minHeight: '150px' }}>
      <div className="max">
        <Table
          id={`overlapping-mask-table`}
          key={`overlapping-mask-table`}
          columnDefs={OVERLAPPING_MASKS_COLUMN_DEFINITIONS}
          getRowNodeId={node => node.id}
          rowSelection="single"
          {...tableProps}
        />
      </div>
    </div>
  </div>
);
