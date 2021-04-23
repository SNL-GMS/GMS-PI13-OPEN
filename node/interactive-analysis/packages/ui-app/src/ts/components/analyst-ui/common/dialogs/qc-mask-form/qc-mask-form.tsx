import { ContextMenu } from '@blueprintjs/core';
import { QcMaskTypes } from '@gms/common-graphql';
import { dateToISOString, MILLISECONDS_IN_SECOND } from '@gms/common-util';
import { Form, FormTypes, Table, WidgetTypes } from '@gms/ui-core-components';
import { Toaster } from '@gms/ui-util';
import classNames from 'classnames';
import flatten from 'lodash/flatten';
import React from 'react';
import { getKeyForEnumValue } from '~analyst-ui/common/utils/enum-util';
import { QcMaskDisplayFilters, userPreferences } from '~analyst-ui/config/';
import { QcMaskCategory, QcMaskType } from '~analyst-ui/config/system-config';
import { QcMaskDialogBoxType, QcMaskHistoryRow } from '../types';
import { MASK_HISTORY_COLUMN_DEFINITIONS } from './constants';
import {
  QcMaskDialogBoxProps as QcMaskFormProps,
  QcMaskDialogBoxState as QcMaskFormState
} from './types';

/**
 * QcMaskDialogBox component.
 */
export class QcMaskForm extends React.Component<QcMaskFormProps, QcMaskFormState> {
  /** The toaster reference for user notification pop-ups */
  private static readonly toaster: Toaster = new Toaster();

  private constructor(props) {
    super(props);
    // Creating a mask
    if (this.props.qcMaskDialogBoxType === QcMaskDialogBoxType.Create) {
      this.state = {
        startDate: new Date(this.props.startTimeSecs * MILLISECONDS_IN_SECOND),
        endDate: new Date(this.props.endTimeSecs * MILLISECONDS_IN_SECOND),
        type: 'SPIKE',
        rationale: '',
        showHistory: false,
        mask: undefined,
        startTimeOnHold: false,
        endTimeOnHold: false,
        category: undefined
      };
    } else {
      if (this.props.mask) {
        this.state = {
          startDate: new Date(this.props.mask.currentVersion.startTime * MILLISECONDS_IN_SECOND),
          endDate: new Date(this.props.mask.currentVersion.endTime * MILLISECONDS_IN_SECOND),
          rationale: this.props.mask.currentVersion.rationale,
          type: this.props.mask.currentVersion.type,
          mask: this.props.mask,
          showHistory: false,
          startTimeOnHold: false,
          endTimeOnHold: false,
          category: this.props.mask.currentVersion.category
        };
      }
    }
  }

  /**
   * React component lifecycle.
   */
  // tslint:disable-next-line:cyclomatic-complexity
  public render() {
    const formItems = this.generateFormItems();
    const currentVersionPanel: FormTypes.FormPanel = {
      formItems,
      key: 'Current Version'
    };
    const extraPanels: FormTypes.FormPanel[] = [];

    if (this.props.qcMaskDialogBoxType !== QcMaskDialogBoxType.Create) {
      const allVersionsPanel: FormTypes.FormPanel = {
        content: this.renderMaskHistoryTable({
          rowData: this.generateMaskHistoryTableRows(this.state.mask),
          overlayNoRowsTemplate: 'No versions',
          rowClassRules: {
            'versions-table__row--first-in-table': params => {
              if (params.data['first-in-table']) {
                return true;
              }
              return false;
            }
          }
        }),
        key: 'All Versions'
      };
      extraPanels.push(allVersionsPanel);
    }
    const qcMaskSwatch = this.renderMaskSwatch(
      this.props.qcMaskDialogBoxType === QcMaskDialogBoxType.Create
        ? QcMaskCategory.ANALYST_DEFINED
        : QcMaskCategory[this.state.category]
    );
    return (
      <div className="qc-dialog">
        <Form
          header={'QC Mask'}
          headerDecoration={qcMaskSwatch}
          extraPanels={extraPanels}
          defaultPanel={currentVersionPanel}
          onSubmit={this.onAcceptReject}
          onCancel={ContextMenu.hide}
          submitButtonText={
            this.props.qcMaskDialogBoxType === QcMaskDialogBoxType.Reject ? 'Reject' : 'Save'
          }
          disableSubmit={this.props.qcMaskDialogBoxType === QcMaskDialogBoxType.View}
          requiresModificationForSubmit={
            this.props.qcMaskDialogBoxType === QcMaskDialogBoxType.Modify
          }
        />
      </div>
    );
  }

  private readonly generateFormItems = (): FormTypes.FormItem[] => {
    const items: FormTypes.FormItem[] = [];
    const startTimeItem: FormTypes.FormItem =
      this.props.qcMaskDialogBoxType === QcMaskDialogBoxType.Reject ||
      this.props.qcMaskDialogBoxType === QcMaskDialogBoxType.View
        ? {
            itemKey: 'startTime',
            labelText: 'Start Time',
            itemType: FormTypes.ItemType.Display,
            displayText: dateToISOString(this.state.startDate),
            displayTextFormat: FormTypes.TextFormats.Time
          }
        : {
            itemKey: 'startTime',
            labelText: 'Start Time',
            itemType: FormTypes.ItemType.Input,

            value: {
              type: WidgetTypes.WidgetInputType.TimePicker,
              defaultValue: this.state.startDate
            }
          };
    items.push(startTimeItem);
    const endTimeItem: FormTypes.FormItem =
      this.props.qcMaskDialogBoxType === QcMaskDialogBoxType.Reject ||
      this.props.qcMaskDialogBoxType === QcMaskDialogBoxType.View
        ? {
            itemKey: 'endTime',
            labelText: 'End Time',
            itemType: FormTypes.ItemType.Display,
            displayText: dateToISOString(this.state.endDate),
            displayTextFormat: FormTypes.TextFormats.Time
          }
        : {
            itemKey: 'endTime',
            labelText: 'End Time',
            itemType: FormTypes.ItemType.Input,

            value: {
              type: WidgetTypes.WidgetInputType.TimePicker,
              defaultValue: this.state.endDate
            }
          };
    items.push(endTimeItem);
    const categoryItem: FormTypes.FormItem = {
      itemKey: 'category',
      labelText: 'Category',
      itemType: FormTypes.ItemType.Display,
      displayText:
        this.props.qcMaskDialogBoxType === QcMaskDialogBoxType.Create
          ? QcMaskCategory.ANALYST_DEFINED
          : QcMaskCategory[this.state.category]
    };
    items.push(categoryItem);

    if (this.state.type !== null && this.state.type !== undefined) {
      const typeItem: FormTypes.FormItem =
        this.props.qcMaskDialogBoxType === QcMaskDialogBoxType.Reject ||
        this.props.qcMaskDialogBoxType === QcMaskDialogBoxType.View
          ? {
              itemKey: 'type',
              labelText: 'Type',
              itemType: FormTypes.ItemType.Display,
              displayText: QcMaskType[this.state.type]
            }
          : {
              itemKey: 'type',
              labelText: 'Type',
              itemType: FormTypes.ItemType.Input,
              value: {
                type: WidgetTypes.WidgetInputType.DropDown,
                defaultValue: QcMaskType[this.state.type],
                params: {
                  dropDownItems: QcMaskType
                }
              }
            };
      items.push(typeItem);
    }

    const rationaleItem: FormTypes.FormItem =
      this.props.qcMaskDialogBoxType === QcMaskDialogBoxType.View
        ? {
            itemKey: 'rationale',
            labelText: 'Rationale',
            itemType: FormTypes.ItemType.Display,
            displayText: this.state.rationale
          }
        : {
            itemKey: 'rationale',
            labelText: 'Rationale',
            itemType: FormTypes.ItemType.Input,

            value: {
              defaultValue: this.state.rationale,
              type: WidgetTypes.WidgetInputType.TextArea
            }
          };
    items.push(rationaleItem);
    return items;
  }
  /**
   * Generate the table row data for the mask hisory.
   */
  private readonly generateMaskHistoryTableRows = (
    mask: QcMaskTypes.QcMask
  ): QcMaskHistoryRow[] => {
    const rows = flatten(
      mask.qcMaskVersions
        .map(m => ({
          id: mask.id,
          versionId: m.version,
          color: userPreferences.colors.maskDisplayFilters[m.category].color,
          category: userPreferences.colors.maskDisplayFilters[m.category].name,
          type: QcMaskType[m.type],
          startTime: m.startTime,
          endTime: m.endTime,
          channelSegmentIds: m.channelSegmentIds.join(', '),
          rationale: m.rationale
        }))
        .sort((a, b) => b.startTime - a.startTime)
    );
    rows[0]['first-in-table'] = true;
    return rows;
  }

  private readonly onAcceptReject = (data: any) => {
    const startTime = data.startTime
      ? data.startTime.valueOf() / MILLISECONDS_IN_SECOND
      : this.state.startDate.getTime() / MILLISECONDS_IN_SECOND;
    const endTime = data.endTime
      ? data.endTime.valueOf() / MILLISECONDS_IN_SECOND
      : this.state.endDate.getTime() / MILLISECONDS_IN_SECOND;
    const category = 'ANALYST_DEFINED';

    if (endTime < startTime) {
      QcMaskForm.toaster.toastWarn('Start time cannot be after end time');
      return;
    }

    const type = getKeyForEnumValue(data.type, QcMaskType)
      ? getKeyForEnumValue(data.type, QcMaskType)
      : data.type;
    const rationale = data.rationale ? data.rationale : this.state.rationale;
    const maskId = this.state.mask ? this.state.mask.id : undefined;

    const qcInput: QcMaskTypes.QcMaskInput = {
      timeRange: {
        startTime,
        endTime
      },
      category,
      rationale,
      type
    };
    ContextMenu.hide();
    this.props.applyChanges(this.props.qcMaskDialogBoxType, maskId, qcInput);
  }

  private readonly renderMaskSwatch = (category: QcMaskCategory): JSX.Element => {
    const maskFilters: QcMaskDisplayFilters = userPreferences.colors.maskDisplayFilters;
    const filter =
      maskFilters[Object.keys(maskFilters).find(key => maskFilters[key].name === category)];
    return <div className="qc-mask-swatch" style={{ backgroundColor: filter.color }} />;
  }

  private readonly renderMaskHistoryTable = (tableProps: {}) => (
    <div className={classNames('ag-theme-dark', 'qc-mask-history-table')}>
      <div className="max">
        <Table
          id="table-qc-mask-history"
          key="table-qc-mask-history"
          columnDefs={MASK_HISTORY_COLUMN_DEFINITIONS}
          getRowNodeId={node => node.id}
          rowSelection="single"
          {...tableProps}
        />
      </div>
    </div>
  )
}
