import { ColumnDefinition, DropDown, TableCellRenderer } from '@gms/ui-core-components';
import cloneDeep from 'lodash/cloneDeep';
import * as React from 'react';
import { userPreferences } from '~components/common-ui/config/user-preferences';
import { SoundConfigurationRendererParams, SoundConfigurationRow } from './types';

export type SoundConfigurationColumnDefinition = ColumnDefinition<
  SoundConfigurationRow,
  {},
  string | number,
  {},
  {}
>;

const defaultColumnWidthPx = 200;

const getColumnPosition = (props: SoundConfigurationRendererParams): number | 'first' | 'last' => {
  const index = props.columnApi
    .getAllDisplayedColumns()
    .findIndex(c => c.getColId() === props.colDef.colId);
  return index === 0
    ? 'first'
    : index === props.columnApi.getAllDisplayedColumns().length - 1
    ? 'last'
    : index;
};

export const NotificationsStatusCellRenderer: React.FunctionComponent<SoundConfigurationRendererParams> = props =>
  props.data?.hasNotificationStatusError ? (
    <TableCellRenderer
      className={`sound-configuration--notification-status-no-file-error`}
      data-col-position={getColumnPosition(props)}
      value={'!'}
      tooltipMsg={userPreferences.configuredAudibleNotificationFileNotFound(
        props.data.sound.selectedSound
      )}
      shouldCenterText={true}
    >
      {props.children}
    </TableCellRenderer>
  ) : null;

export const SoundConfigurationCellRenderer: React.FunctionComponent<SoundConfigurationRendererParams> = props => (
  <TableCellRenderer
    data-col-position={getColumnPosition(props)}
    value={props.valueFormatted ?? props.value}
  />
);

export const SoundConfigurationDropdownRenderer: React.FunctionComponent<SoundConfigurationRendererParams> = props => {
  if (!props.data || !props.data.sound) return null;

  const availableSounds = cloneDeep(props.data.sound.availableSounds);

  // if the configured sound happens to no longer be listed in the available sounds; add it
  // this will allow the selection to be correct and for an error to be displayed
  if (
    props.data.sound.selectedSound !== 'None' &&
    !availableSounds[props.data.sound.selectedSound]
  ) {
    availableSounds[props.data.sound.selectedSound] = props.data.sound.selectedSound;
  }
  return (
    <DropDown
      className={'sound-configuration__dropdown'}
      dropDownItems={availableSounds}
      value={props.data.sound.selectedSound}
      onMaybeValue={v => props.data.sound.onSelect(v)}
      title={'Select a sound'}
    />
  );
};

const headerCellBlockClass = 'soh-header-cell';

export const defaultColumnDefinition: SoundConfigurationColumnDefinition = {
  headerClass: `${headerCellBlockClass} ${headerCellBlockClass}--neutral`,
  width: defaultColumnWidthPx,
  sortable: true,
  filter: true,
  disableStaticMarkupForHeaderComponentFramework: true,
  disableStaticMarkupForCellRendererFramework: true,
  cellRendererFramework: SoundConfigurationCellRenderer
};
