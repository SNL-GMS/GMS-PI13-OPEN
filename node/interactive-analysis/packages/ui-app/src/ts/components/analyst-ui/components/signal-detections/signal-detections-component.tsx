import { ContextMenu } from '@blueprintjs/core';
import { IconNames } from '@blueprintjs/icons';
import { EventTypes, SignalDetectionTypes, SignalDetectionUtils } from '@gms/common-graphql';
import { UILogger } from '@gms/ui-apollo';
import { Table, TableApi, Toolbar, ToolbarTypes } from '@gms/ui-core-components';
import { addGlForceUpdateOnResize, addGlForceUpdateOnShow } from '@gms/ui-util';
import classNames from 'classnames';
import defer from 'lodash/defer';
import isEqual from 'lodash/isEqual';
import memoizeOne from 'memoize-one';
import React from 'react';
import { SignalDetectionContextMenu } from '~analyst-ui/common/context-menus/signal-detection-context-menu';
import { SignalDetectionDetails } from '~analyst-ui/common/dialogs';
import { isSdFeatureMeasurementValue } from '~analyst-ui/common/utils/instance-of-util';
import {
  determineDetectionColor,
  determineIfAssociated,
  determineIfComplete,
  findEventHypothesisForDetection
} from '~analyst-ui/common/utils/signal-detection-util';
import {
  DataType,
  TableDataState,
  TableInvalidState
} from '~analyst-ui/common/utils/table-invalid-state';
import { userPreferences } from '~analyst-ui/config';
import { columnDefs } from './table-utils/column-defs';
import {
  FilterType,
  SignalDetectionsProps,
  SignalDetectionsRow,
  SignalDetectionsState
} from './types';

/**
 * Handle for user preference filtering functionality
 */
const autoFilter = userPreferences.signalDetectionList.autoFilter;

/**
 * Displays signal detection information in tabular form
 */
export class SignalDetections extends React.Component<
  SignalDetectionsProps,
  SignalDetectionsState
> {
  /**
   * To interact directly with the table
   */
  private mainTable: TableApi;

  /**
   * A memoized function for generating the table rows.
   * The memoization function caches the results using
   * the most recent argument and returns the results.
   *
   * @param openEventId the current open event id
   * @param eventsInTimeRange the events in the current time range
   * @param signalDetectionsByStation the signal detections for stations
   *
   * @returns an array of signal detection table rows
   */
  private readonly memoizedGenerateTableRows: (
    openEventId: string,
    eventsInTimeRange: EventTypes.Event[],
    signalDetectionsByStation: SignalDetectionTypes.SignalDetection[]
  ) => SignalDetectionsRow[];

  /**
   * Handlers to unsubscribe from apollo subscriptions
   */
  private readonly unsubscribeHandlers: { (): void }[] = [];

  /**
   * Convert the signal detection data into table rows.
   *
   * @param openEventId the current open event id
   * @param eventsInTimeRange the events in the current time range
   * @param signalDetectionsByStation the signal detections for stations
   *
   * @returns an array of signal detection table rows
   */
  private static readonly generateTableRows = (
    openEventId: string,
    eventsInTimeRange: EventTypes.Event[],
    signalDetectionsByStation: SignalDetectionTypes.SignalDetection[]
  ): SignalDetectionsRow[] =>
    // For each station, filter down to the detections that are not rejected (we don't show rejected detections).
    // For each non-rejected detection, extract the table row content into a collection.
    // Finally, flatten the collection of row collections into a single row collection
    signalDetectionsByStation
      .filter(sd => !sd.currentHypothesis.rejected)
      .map(detection => {
        const arrivalTimeFeatureMeasurementValue = SignalDetectionUtils.findArrivalTimeFeatureMeasurementValue(
          detection.currentHypothesis.featureMeasurements
        );
        const azimuthVal = SignalDetectionUtils.findAzimuthFeatureMeasurementValue(
          detection.currentHypothesis.featureMeasurements
        )
          ? SignalDetectionUtils.findAzimuthFeatureMeasurementValue(
              detection.currentHypothesis.featureMeasurements
            ).measurementValue.value
          : null;
        const slownessVal = SignalDetectionUtils.findSlownessFeatureMeasurementValue(
          detection.currentHypothesis.featureMeasurements
        )
          ? SignalDetectionUtils.findSlownessFeatureMeasurementValue(
              detection.currentHypothesis.featureMeasurements
            ).measurementValue.value
          : null;
        const eventHypo = findEventHypothesisForDetection(detection, eventsInTimeRange);
        const assocEventId = eventHypo.map(e => e.event.id).find(id => id === openEventId);
        const fmPhase = SignalDetectionUtils.findPhaseFeatureMeasurementValue(
          detection.currentHypothesis.featureMeasurements
        );
        if (!fmPhase) {
          return;
        }
        const maybeAFiveFm = SignalDetectionUtils.findAmplitudeFeatureMeasurement(
          detection.currentHypothesis.featureMeasurements,
          SignalDetectionTypes.FeatureMeasurementTypeName.AMPLITUDE_A5_OVER_2
        );
        const aFiveValue =
          maybeAFiveFm && isSdFeatureMeasurementValue(maybeAFiveFm.measurementValue)
            ? maybeAFiveFm.measurementValue.amplitude.value
            : null;
        const aFivePeriod =
          maybeAFiveFm && isSdFeatureMeasurementValue(maybeAFiveFm.measurementValue)
            ? maybeAFiveFm.measurementValue.period
            : null;
        const maybeALRFM = SignalDetectionUtils.findAmplitudeFeatureMeasurement(
          detection.currentHypothesis.featureMeasurements,
          SignalDetectionTypes.FeatureMeasurementTypeName.AMPLITUDE_ALR_OVER_2
        );
        const alrMeasurementValue =
          maybeALRFM && isSdFeatureMeasurementValue(maybeALRFM.measurementValue)
            ? maybeALRFM.measurementValue.amplitude.value
            : null;
        const alrPeriod =
          maybeALRFM && isSdFeatureMeasurementValue(maybeALRFM.measurementValue)
            ? maybeALRFM.measurementValue.period
            : null;

        const conflictingHyps = detection.conflictingHypotheses.map(conflict => {
          const event = eventsInTimeRange
            ? eventsInTimeRange.find(ev => ev.id === conflict.eventId)
            : undefined;
          const eventTime = event
            ? event.currentEventHypothesis.eventHypothesis.preferredLocationSolution
                .locationSolution.location.time
            : undefined;
          return {
            ...conflict,
            eventTime,
            stationName: detection.stationName
          };
        });

        // TODO when we get new amplitude measurements will need to add logic to know which measurement flag to set
        return {
          id: detection.id,
          hypothesisId: detection.currentHypothesis.id,
          station: detection.stationName,
          phase: fmPhase.phase.toString(),
          time: arrivalTimeFeatureMeasurementValue.value,
          timeUnc: arrivalTimeFeatureMeasurementValue.standardDeviation,
          assocEventId,
          modified: detection.modified,
          conflicts: conflictingHyps,
          color: determineDetectionColor(detection, eventsInTimeRange, openEventId),
          isSelectedEvent: determineIfAssociated(detection, eventsInTimeRange, openEventId),
          isComplete: determineIfComplete(detection, eventsInTimeRange),
          aFiveMeasurement: {
            value: aFiveValue,
            requiresReview:
              detection.requiresReview.amplitudeMeasurement &&
              !detection.reviewed.amplitudeMeasurement
          },
          aFivePeriod,
          alrMeasurement: alrMeasurementValue,
          alrPeriod,
          slowness: slownessVal,
          azimuth: azimuthVal
        };
      })

  // ***************************************
  // BEGIN REACT COMPONENT LIFECYCLE METHODS
  // ***************************************

  /**
   * Constructor.
   *
   * @param props The initial props
   */
  public constructor(props) {
    super(props);
    this.state = {
      selectedFilter: FilterType.allRows,
      userSetFilter: false
    };
    this.memoizedGenerateTableRows = memoizeOne(
      SignalDetections.generateTableRows,
      /* tell memoize to use a deep comparison for complex objects */
      isEqual
    );
  }

  /**
   * Updates the derived state from the next props.
   *
   * @param nextProps The next (new) props
   * @param prevState The previous state
   */
  public static getDerivedStateFromProps(
    nextProps: SignalDetectionsProps,
    prevState: SignalDetectionsState
  ) {
    // We need to set new filters based on incoming props
    if (autoFilter) {
      if (nextProps.openEventId && !prevState.userSetFilter) {
        return {
          selectedFilter: FilterType.openEvent
        };
      }

      return {
        selectedFilter: prevState.selectedFilter
      };
    }
    // return null to indicate no change to state.
    return null;
  }

  /**
   * Invoked when the component mounted.
   */
  public componentDidMount() {
    addGlForceUpdateOnShow(this.props.glContainer, this);
    addGlForceUpdateOnResize(this.props.glContainer, this);
  }

  /**
   * React component lifecycle
   *
   * @param prevProps The previous properties available to this react component
   */
  public componentDidUpdate(prevProps: SignalDetectionsProps) {
    if (
      this.props.currentTimeInterval &&
      !isEqual(this.props.currentTimeInterval, prevProps.currentTimeInterval)
    ) {
      this.setupSubscriptions(this.props);
      this.forceUpdate();
    }

    if (this.mainTable && this.mainTable.getSortModel().length === 0) {
      this.mainTable.setSortModel([{ colId: 'time', sort: 'asc' }]);
    }

    // If the selected event has changed, select it in the table
    if (!isEqual(prevProps.selectedSdIds, this.props.selectedSdIds)) {
      this.selectRowsFromProps(this.props);
    }

    if (prevProps.openEventId !== this.props.openEventId) {
      this.setState({
        userSetFilter: false
      });
    }
  }

  /**
   * Invoked when the component will unmount.
   */
  public componentWillUnmount() {
    // Unsubscribe from all current subscriptions
    this.unsubscribeHandlers.forEach(unsubscribe => unsubscribe());
    this.unsubscribeHandlers.length = 0;
  }

  /**
   * Renders the component.
   */
  public render() {
    const dataState: TableDataState = !this.props.eventsInTimeRangeQuery
      ? TableDataState.NO_INTERVAL
      : this.props.signalDetectionsByStationQuery &&
        this.props.signalDetectionsByStationQuery.loading
      ? TableDataState.NO_SDS
      : TableDataState.READY;

    if (dataState !== TableDataState.READY) {
      return (
        <TableInvalidState visual={IconNames.LOCATE} message={dataState} dataType={DataType.SD} />
      );
    }

    const mainTableRowData: SignalDetectionsRow[] = this.memoizedGenerateTableRows(
      this.props.openEventId,
      this.props.eventsInTimeRangeQuery ? this.props.eventsInTimeRangeQuery.eventsInTimeRange : [],
      this.props.signalDetectionsByStationQuery
        ? this.props.signalDetectionsByStationQuery.signalDetectionsByStation
        : []
    );

    const toolbarLeftItems: ToolbarTypes.ToolbarItem[] = [
      {
        rank: 1,
        label: 'Filter by Association',
        tooltip: 'Filters signal detections by association to current event',
        type: ToolbarTypes.ToolbarItemType.Dropdown,
        onChange: enumKey => {
          this.handleFilterChange(enumKey);
        },
        dropdownOptions: FilterType,
        value: this.state.selectedFilter,
        cyData: 'signal-detection-filter',
        widthPx: 150
      }
    ];
    const toolbarItems: ToolbarTypes.ToolbarItem[] = [];
    const selectGroup: ToolbarTypes.ButtonGroupItem = {
      label: 'Select/Deselect All',
      tooltip: 'Selects or deselects all items in table',
      type: ToolbarTypes.ToolbarItemType.ButtonGroup,
      rank: 1,
      buttons: [
        {
          label: 'Deselect All',
          tooltip: 'Deselects all items in table',
          type: ToolbarTypes.ToolbarItemType.Button,
          rank: 2,
          widthPx: 98,
          onClick: () => {
            this.setSelectionOnAll(false);
          }
        },
        {
          label: 'Select All',
          tooltip: 'Selects all items in table',
          type: ToolbarTypes.ToolbarItemType.Button,
          rank: 3,
          widthPx: 81,
          onClick: () => {
            this.setSelectionOnAll(true);
          }
        }
      ]
    };
    toolbarItems.push(selectGroup);

    // ! TODO DO NOT USE `this.props.glContainer.width` TO CALCULATING WIDTH - COMPONENT MAY NOT BE INSIDE GL
    const glWidth = this.props.glContainer ? this.props.glContainer.width : 0;

    return (
      <div data-cy="signal-detections" className={classNames('ag-theme-dark', 'table-container')}>
        <div className={'list-toolbar-wrapper'}>
          <Toolbar
            items={toolbarItems}
            itemsLeft={toolbarLeftItems}
            toolbarWidthPx={glWidth - userPreferences.list.widthOfTableMarginsPx}
            minWhiteSpacePx={userPreferences.list.minWidthPx}
          />
        </div>
        <div className={'list-wrapper'}>
          <div className={'max'}>
            <Table
              id="table-signal-detections"
              key="table-signal-detections"
              context={{}}
              onGridReady={this.onMainTableReady}
              gridOptions={{
                rowClass: 'sd-row'
              }}
              columnDefs={columnDefs}
              rowData={this.filterTableData(mainTableRowData)}
              onCellValueChanged={this.onCellValueChanged}
              getRowNodeId={node => node.id}
              deltaRowDataMode={true}
              getRowClass={this.computeRowClass}
              onRowClicked={this.onRowClicked}
              onCellContextMenu={this.onCellContextMenu}
              rowSelection="multiple"
              rowDeselection={true}
              overlayNoRowsTemplate="No SDs to display"
              suppressContextMenu={true}
            />
          </div>
        </div>
      </div>
    );
  }

  // ***************************************
  // END REACT COMPONENT LIFECYCLE METHODS
  // ***************************************

  /**
   * Select rows in the table based on the selected SD IDs in the properties.
   *
   * @param props signal detection props
   */
  private readonly selectRowsFromProps = (props: SignalDetectionsProps) => {
    if (this.mainTable) {
      this.mainTable.deselectAll();
      this.mainTable.forEachNode(node => {
        props.selectedSdIds.forEach(sdId => {
          if (node.data.id === sdId) {
            node.setSelected(true);
            // Must pass in null here as ag-grid expects it
            this.mainTable.ensureNodeVisible(node, null);
          }
        });
      });
    }
  }

  /**
   * Initialize graphql subscriptions on the apollo client
   *
   * @param props signal detection props
   */
  private readonly setupSubscriptions = (props: SignalDetectionsProps): void => {
    if (!props.signalDetectionsByStationQuery) return;

    // First, unsubscribe from all current subscriptions
    this.unsubscribeHandlers.forEach(unsubscribe => unsubscribe());
    this.unsubscribeHandlers.length = 0;

    // Don't register subscriptions if the current time interval is undefined/null
    if (!props.currentTimeInterval) return;
  }

  /**
   * Set class members when main table is ready
   *
   * @param event table event
   */
  private readonly onMainTableReady = (event: any) => {
    this.mainTable = event.api;
  }

  /**
   * Style rows to show disabled state based on params
   *
   * @param params params from table.
   */
  private readonly computeRowClass = (params: any) => {
    // If the signal detection is outside the current interval, show it in a disabled state
    if (params.data.disabled) {
      return 'signal-detections-disabled-row';
    }
  }
  /**
   * Selects or deselects all in table
   *
   * @param select If tru selects, if false deselects
   */
  private readonly setSelectionOnAll = (select: boolean) => {
    if (this.mainTable) {
      if (select) {
        const selectedIds = [];
        this.mainTable.forEachNodeAfterFilter((node: any) => {
          selectedIds.push(node.data.id);
        });
        this.mainTable.selectAllFiltered();
        this.props.setSelectedSdIds(selectedIds);
      } else {
        this.mainTable.deselectAll();
        this.props.setSelectedSdIds([]);
      }
    }
  }
  /**
   * Creates a context menu and displays it
   *
   * @param params table parameters
   */
  private readonly onCellContextMenu = (params: any) => {
    params.event.preventDefault();
    params.event.stopPropagation();
    const selectedIdsInTable = this.mainTable.getSelectedNodes().map(node => node.data.id);
    if (selectedIdsInTable.indexOf(params.node.data.id) < 0) {
      selectedIdsInTable.push(params.node.data.id);
    }
    this.props.setSelectedSdIds(selectedIdsInTable);
    // tslint:disable-next-line: max-line-length
    const sds: SignalDetectionTypes.SignalDetection[] = this.props.signalDetectionsByStationQuery.signalDetectionsByStation.filter(
      sd => selectedIdsInTable.indexOf(sd.id) >= 0
    );
    const currentlyOpenEvent:
      | EventTypes.Event
      | undefined = this.props.eventsInTimeRangeQuery.eventsInTimeRange.find(
      event => event.id === this.props.openEventId
    );
    const context = (
      <SignalDetectionContextMenu
        signalDetections={this.props.signalDetectionsByStationQuery.signalDetectionsByStation}
        selectedSds={sds}
        currentOpenEvent={currentlyOpenEvent}
        changeAssociation={this.props.changeSignalDetectionAssociations}
        rejectDetections={this.props.rejectDetections}
        updateDetections={this.props.updateDetections}
        setSdIdsToShowFk={this.props.setSdIdsToShowFk}
        sdIdsToShowFk={this.props.sdIdsToShowFk}
        associateToNewEvent={this.props.createEvent}
        measurementMode={this.props.measurementMode}
        setSelectedSdIds={this.props.setSelectedSdIds}
        setMeasurementModeEntries={this.props.setMeasurementModeEntries}
      />
    );
    ContextMenu.show(
      context,
      {
        left: params.event.clientX,
        top: params.event.clientY
      },
      undefined,
      true
    );
  }
  /**
   * Uses a switch statement to determine the appropriate filtering option.
   *
   * @param data an array of signal detection table rows
   *
   * @returns filtered list based off filter type
   */
  private readonly filterTableData = (data: SignalDetectionsRow[]): SignalDetectionsRow[] => {
    let filteredList = [];
    switch (this.state.selectedFilter) {
      case 'All Detections':
        filteredList = data;
        break;
      case 'Open Event':
        filteredList = data.filter(signalDetection => signalDetection.isSelectedEvent);
        break;
      case 'Completed':
        filteredList = data.filter(signalDetection => signalDetection.isComplete);
        break;
      default:
    }
    return filteredList;
  }

  /**
   * Handles the filter dropdown change event
   *
   * @param event react form event
   */
  private readonly handleFilterChange = (val: any) => {
    // Update the filter
    const newFilter = val as FilterType;
    this.setState((prevState: SignalDetectionsState) => ({
      selectedFilter: newFilter,
      userSetFilter: true
    }));
  }

  /**
   * Update the selected SD IDs when the user clicks on a row in the table.
   */
  private readonly onRowClicked = (rowParams: any) => {
    if (this.mainTable) {
      if (rowParams.event.altKey) {
        const color = rowParams.data.color;
        // Display information of the signal detection
        const detection = this.props.signalDetectionsByStationQuery.signalDetectionsByStation.filter(
          sd => sd.id === rowParams.data.id
        );
        ContextMenu.show(
          <SignalDetectionDetails detection={detection[0]} color={color} />,
          { left: rowParams.event.clientX, top: rowParams.event.clientY },
          undefined,
          true
        );
      } else {
        defer(() => {
          const selectedSdIds = this.mainTable.getSelectedNodes().map(node => node.data.id);
          this.props.setSelectedSdIds(selectedSdIds);
        });
      }
    }
  }

  /**
   * Update the signal detection's variables based on input
   *
   * @param row table row parameters
   */
  private readonly onCellValueChanged = (row: any) => {
    // An any object has the oldValue and newValue fields by default
    // exit if the value didn't actually change, at least according to ===
    if (row.oldValue === row.newValue) return;
    const tableRow: SignalDetectionsRow = row.data;

    // Find the Signal Detection to lookup the AmplitudeMeasurementValue
    const signalDetection = this.props.signalDetectionsByStationQuery.signalDetectionsByStation.find(
      sd => sd.id === tableRow.id
    );

    const arrivalTime = SignalDetectionUtils.findArrivalTimeFeatureMeasurementValue(
      signalDetection.currentHypothesis.featureMeasurements
    ).value;

    const newArrivalTime = parseFloat(tableRow.time.toString());

    const newTimeUncertaintySec = parseFloat(tableRow.timeUnc.toString());

    const newPhase = tableRow.phase;

    let amplitudeMeasurement: SignalDetectionTypes.AmplitudeMeasurementValue;
    if (signalDetection && signalDetection.currentHypothesis) {
      amplitudeMeasurement = SignalDetectionUtils.findAmplitudeFeatureMeasurementValue(
        signalDetection.currentHypothesis.featureMeasurements,
        SignalDetectionTypes.FeatureMeasurementTypeName.AMPLITUDE_A5_OVER_2
      );
    }
    // Update phase no need to build Signal Detection timing arg (can't change time or uncertainty)
    const variables: SignalDetectionTypes.UpdateDetectionsMutationArgs = {
      detectionIds: [tableRow.id],
      input: {
        phase: newPhase,
        signalDetectionTiming: {
          arrivalTime: newArrivalTime,
          timeUncertaintySec: newTimeUncertaintySec,
          // clear out the amplitude because the signal detection has been re-timed
          amplitudeMeasurement: arrivalTime !== newArrivalTime ? undefined : amplitudeMeasurement
        }
      }
    };
    this.props
      .updateDetections({
        variables
      })
      .catch(err => UILogger.Instance().error(`Failed to update detections: ${err.message}`));
  }
}
