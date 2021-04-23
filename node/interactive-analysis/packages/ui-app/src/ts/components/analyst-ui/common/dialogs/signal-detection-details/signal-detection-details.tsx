import { ContextMenu, NonIdealState } from '@blueprintjs/core';
import { SignalDetectionTypes, SignalDetectionUtils } from '@gms/common-graphql';
import { timeSecondsToISOString, toDate } from '@gms/common-util';
import { Form, FormTypes, Table } from '@gms/ui-core-components';
import classNames from 'classnames';
import flatten from 'lodash/flatten';
import React from 'react';
import { SIGNAL_DETECTION_HISTORY_COLUMN_DEFINITIONS } from './constants';
import {
  SignalDetectionDetailsProps,
  SignalDetectionDetailsState,
  SignalDetectionHistoryRow
} from './types';
import { formatUncertainty } from './utils';

/**
 * SignalDetectionDetails Component
 */
export class SignalDetectionDetails extends React.Component<
  SignalDetectionDetailsProps,
  SignalDetectionDetailsState
> {
  /**
   * Constructor
   */
  public constructor(props: SignalDetectionDetailsProps) {
    super(props);
    this.state = {
      showHistory: false
    };
  }

  /**
   * React component lifecycle
   */
  public render() {
    if (!this.props.detection) {
      return <NonIdealState />;
    }

    const formItems: FormTypes.FormItem[] = [];
    const detection = this.props.detection;
    const arrivalTimeFeatureMeasurementValue = SignalDetectionUtils.findArrivalTimeFeatureMeasurementValue(
      detection.currentHypothesis.featureMeasurements
    );
    const fmPhase = SignalDetectionUtils.findPhaseFeatureMeasurementValue(
      detection.currentHypothesis.featureMeasurements
    );
    formItems.push({
      itemKey: 'Phase',
      labelText: 'Phase',
      displayText: fmPhase.phase.toString(),
      itemType: FormTypes.ItemType.Display
    });
    formItems.push({
      itemKey: 'Detection time',
      labelText: 'Detection time',
      itemType: FormTypes.ItemType.Display,
      displayText: timeSecondsToISOString(toDate(arrivalTimeFeatureMeasurementValue.value)),
      displayTextFormat: FormTypes.TextFormats.Time
    });
    formItems.push({
      itemKey: 'Time uncertainty',
      labelText: 'Time uncertainty',
      itemType: FormTypes.ItemType.Display,
      displayText: formatUncertainty(arrivalTimeFeatureMeasurementValue.standardDeviation)
    });
    formItems.push({
      itemKey: 'Rejected',
      labelText: 'Rejected',
      itemType: FormTypes.ItemType.Display,
      displayText: detection.currentHypothesis.rejected ? 'Yes' : 'No'
    });

    const defaultPanel: FormTypes.FormPanel = {
      formItems,
      key: 'Current Version'
    };
    const extraPanels: FormTypes.FormPanel[] = [
      {
        key: 'All Versions',
        content: this.renderTable({
          rowData: this.generateDetectionHistoryTableRows(detection),
          overlayNoRowsTemplate: 'No Verions',
          rowClassRules: {
            'versions-table__row--first-in-table': params => {
              if (params.data['first-in-table']) {
                return true;
              }
              return false;
            }
          }
        })
      }
    ];

    return (
      <div>
        <Form
          header={'Signal Detection'}
          headerDecoration={
            <div
              className="signal-detection-swatch"
              style={{ backgroundColor: this.props.color }}
            />
          }
          defaultPanel={defaultPanel}
          disableSubmit={true}
          onCancel={() => {
            ContextMenu.hide();
          }}
          extraPanels={extraPanels}
        />
      </div>
    );
  }

  /**
   * Render the Detection table.
   */
  private readonly renderTable = (tableProps: {}) => (
    <div className={classNames('ag-theme-dark', 'signal-detection-details-versions-table')}>
      <div className={'max'}>
        <Table
          id="table-signal-detection-details"
          key="table-signal-detection-details"
          columnDefs={SIGNAL_DETECTION_HISTORY_COLUMN_DEFINITIONS}
          getRowNodeId={node => node.id}
          rowSelection="single"
          {...tableProps}
        />
      </div>
    </div>
  )

  /**
   * Generate the table row data for the detection history.
   */
  private readonly generateDetectionHistoryTableRows = (
    detection: SignalDetectionTypes.SignalDetection
  ): SignalDetectionHistoryRow[] => {
    const rows = flatten(
      detection.signalDetectionHypothesisHistory
        .map(detectionHistory => ({
          id: detectionHistory.id,
          versionId: detectionHistory.id,
          phase: detectionHistory.phase,
          rejected: detectionHistory.rejected,
          arrivalTimeMeasurementFeatureType:
            SignalDetectionTypes.FeatureMeasurementTypeName.ARRIVAL_TIME,
          arrivalTimeMeasurementTimestamp: detectionHistory.arrivalTimeSecs,
          arrivalTimeMeasurementUncertaintySec: detectionHistory.arrivalTimeUncertainty
        }))
        .sort((a, b) => b.arrivalTimeMeasurementTimestamp - a.arrivalTimeMeasurementTimestamp)
    );
    rows[0]['first-in-table'] = true;
    return rows;
  }
}
