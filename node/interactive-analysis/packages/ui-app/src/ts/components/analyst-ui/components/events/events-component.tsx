import { IconNames } from '@blueprintjs/icons';
import {
  CommonTypes,
  EventTypes,
  SignalDetectionTypes,
  SignalDetectionUtils
} from '@gms/common-graphql';
import { UILogger } from '@gms/ui-apollo';
import { Table, TableApi } from '@gms/ui-core-components';
import { AnalystWorkspaceTypes } from '@gms/ui-state';
import { addGlForceUpdateOnResize, addGlForceUpdateOnShow } from '@gms/ui-util';
import classNames from 'classnames';
import defer from 'lodash/defer';
import isEqual from 'lodash/isEqual';
import memoizeOne from 'memoize-one';
import React from 'react';
import { openEvent } from '~analyst-ui/common/actions/event-actions';
import {
  DataType,
  TableDataState,
  TableInvalidState
} from '~analyst-ui/common/utils/table-invalid-state';
import { userPreferences } from '~analyst-ui/config';
import { InteractionContext } from '~analyst-ui/interactions/interaction-provider/types';
import { gmsColors, semanticColors } from '~scss-config/color-preferences';
import { EventsToolbar } from './events-toolbar';
import { columnDefs } from './table-utils/column-defs';
import {
  EventFilters,
  EventsProps,
  EventsRow,
  EventsState,
  SignalDetectionConflict
} from './types';

/**
 * Displays event information in tabular form
 */
export class Events extends React.Component<EventsProps, EventsState> {
  /**
   * To interact directly with the table
   */
  private mainTable: TableApi;

  /**
   * A memoized function for generating the table rows.
   * The memoization function caches the results using
   * the most recent argument and returns the results.
   *
   * @param currentTimeInterval the current time interval
   * @param openEventId the current open event id
   * @param eventsInTimeRange the events for the current time range
   * @param signalDetectionsByStation the signal detections by stations
   * @param showEventOfType map indicating event types to display
   *
   * @returns an array of event row objects
   */
  private readonly memoizedGenerateTableRows: (
    workspaceState: CommonTypes.WorkspaceState,
    currentTimeInterval: CommonTypes.TimeRange,
    openEventId: string,
    eventsInTimeRange: EventTypes.Event[],
    signalDetectionsByStation: SignalDetectionTypes.SignalDetection[],
    showEventOfType: Map<EventFilters, boolean>
  ) => EventsRow[];

  /**
   * Handlers to unsubscribe from apollo subscriptions
   */
  private readonly unsubscribeHandlers: { (): void }[] = [];

  /**
   * Convert the event data into table rows
   *
   * @param currentTimeInterval the current time interval
   * @param openEventId the current open event id
   * @param eventsInTimeRange the events for the current time range
   * @param signalDetectionsByStation the signal detections by stations
   * @param showEventOfType map indicating event types to display
   *
   * @returns an array of event row objects
   */
  private static readonly generateTableRows = (
    workspaceState: CommonTypes.WorkspaceState,
    currentTimeInterval: CommonTypes.TimeRange,
    openEventId: string,
    eventsInTimeRange: EventTypes.Event[],
    signalDetectionsByStation: SignalDetectionTypes.SignalDetection[],
    showEventOfType: Map<EventFilters, boolean>
  ): EventsRow[] => {
    const events = eventsInTimeRange.filter(event =>
      Events.filterEvent(currentTimeInterval, event, showEventOfType)
    );
    return events.map(event => {
      const eventHyp = event.currentEventHypothesis.eventHypothesis;
      const signalDetectionConflicts: SignalDetectionConflict[] = signalDetectionsByStation
        ? event.conflictingSdIds.map(sdId => {
            const signalD = signalDetectionsByStation.find(sd => sd.id === sdId);
            if (signalD) {
              return {
                phase: SignalDetectionUtils.findPhaseFeatureMeasurementValue(
                  signalD.currentHypothesis.featureMeasurements
                ).phase,
                arrivalTime: SignalDetectionUtils.findArrivalTimeFeatureMeasurementValue(
                  signalD.currentHypothesis.featureMeasurements
                ).value,
                id: signalD.currentHypothesis.id,
                stationName: signalD.stationName
              };
            }
          })
        : [];
      const eventWithUsers =
        workspaceState && workspaceState.eventToUsers
          ? workspaceState.eventToUsers.find(eventToUsers => eventToUsers.eventId === event.id)
          : undefined;
      const activeAnalysts = eventWithUsers ? eventWithUsers.userNames : [];

      return {
        id: event.id,
        eventHypId: eventHyp.id,
        associationIds: event.currentEventHypothesis.eventHypothesis.signalDetectionAssociations.map(
          sd => sd.signalDetectionHypothesis.id
        ),
        isOpen: event.id === openEventId,
        stageId: event.currentEventHypothesis.processingStage
          ? event.currentEventHypothesis.processingStage.id
          : undefined,
        lat: eventHyp.preferredLocationSolution.locationSolution.location.latitudeDegrees,
        lon: eventHyp.preferredLocationSolution.locationSolution.location.longitudeDegrees,
        depth: eventHyp.preferredLocationSolution.locationSolution.location.depthKm,
        time: eventHyp.preferredLocationSolution.locationSolution.location.time,
        modified: event.modified,
        signalDetectionConflicts,
        activeAnalysts,
        numDetections: eventHyp.signalDetectionAssociations.filter(assoc => !assoc.rejected).length,
        status: event.status,
        edgeEvent: Events.isEdgeEvent(currentTimeInterval, event)
      };
    });
  }

  /**
   * Determines if the event should be included based on current filter settings.
   *
   * @param currentTimeInterval the current time interval
   * @param event the event to check if it should be filtered
   * @param showEventOfType map indicating event types to display
   *
   * @returns true if the event should be displayed; false otherwise
   */
  private static readonly filterEvent = (
    currentTimeInterval: CommonTypes.TimeRange,
    event: EventTypes.Event,
    showEventOfType: Map<EventFilters, boolean>
  ): boolean => {
    const isComplete = event && event.status === 'Complete';
    const isEdge = event && Events.isEdgeEvent(currentTimeInterval, event);

    let showInList = true;
    if (!showEventOfType.get(EventFilters.EDGE)) {
      showInList = !isEdge;
    }
    if (showInList && !showEventOfType.get(EventFilters.COMPLETED)) {
      showInList = !isComplete;
    }
    return showInList;
  }

  /**
   * Determines if the given event is an edge event.
   *
   * @param currentTimeInterval the current time interval
   * @param event the event to check if it should be filtered
   *
   * @returns true if the event is an edge event; false otherwise
   */
  private static isEdgeEvent(
    currentTimeInterval: CommonTypes.TimeRange,
    event: EventTypes.Event
  ): boolean {
    const time =
      event.currentEventHypothesis.eventHypothesis.preferredLocationSolution.locationSolution
        .location.time;
    return time < currentTimeInterval.startTime || time > currentTimeInterval.endTime;
  }

  // ***************************************
  // BEGIN REACT COMPONENT LIFECYCLE METHODS
  // ***************************************

  /**
   * Constructor.
   *
   * @param props The initial props
   */
  public constructor(props: EventsProps) {
    super(props);
    this.memoizedGenerateTableRows = memoizeOne(
      Events.generateTableRows,
      /* tell memoize to use a deep comparison for complex objects */
      isEqual
    );
    const showEventOfType = new Map<EventFilters, boolean>();
    showEventOfType.set(EventFilters.COMPLETED, true);
    showEventOfType.set(EventFilters.EDGE, true);
    this.state = {
      currentTimeInterval: props.currentTimeInterval,
      suppressScrollOnNewData: false,
      showEventOfType
    };
  }

  /**
   * Updates the derived state from the next props.
   *
   * @param nextProps The next (new) props
   * @param prevState The previous state
   */
  public static getDerivedStateFromProps(nextProps: EventsProps, prevState: EventsState) {
    return {
      currentTimeInterval: nextProps.currentTimeInterval,
      // Always scroll to the top when the current interval changes, otherwise do not auto scroll
      suppressScrollOnNewData: isEqual(nextProps.currentTimeInterval, prevState.currentTimeInterval)
    };
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
   */
  // tslint:disable-next-line:cyclomatic-complexity
  public componentDidUpdate(prevProps: EventsProps) {
    if (
      this.props.currentTimeInterval &&
      !isEqual(this.props.currentTimeInterval, prevProps.currentTimeInterval)
    ) {
      this.setupSubscriptions(this.props);
    }

    if (this.mainTable && this.mainTable.getSortModel().length === 0) {
      this.mainTable.setSortModel([{ colId: 'time', sort: 'asc' }]);
    }

    const prevEventsInTimeRange = prevProps.eventsInTimeRangeQuery
      ? prevProps.eventsInTimeRangeQuery.eventsInTimeRange
      : [];

    const eventsInTimeRange = this.props.eventsInTimeRangeQuery
      ? this.props.eventsInTimeRangeQuery.eventsInTimeRange
      : [];

    // If the selected event has changed, select it in the table
    if (
      (this.props.openEventId && prevProps.openEventId !== this.props.openEventId) ||
      !isEqual(prevEventsInTimeRange, eventsInTimeRange)
    ) {
      defer(() => {
        this.selectRowsFromProps(this.props);
        // Auto scroll to ensure the selected event is displayed when the data changes
        if (this.mainTable && this.mainTable.getSelectedRows().length !== 0) {
          this.mainTable.ensureNodeVisible(this.mainTable.getSelectedRows()[0], 'middle');
        }
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
      : this.props.eventsInTimeRangeQuery && this.props.eventsInTimeRangeQuery.loading
      ? TableDataState.NO_EVENTS
      : TableDataState.READY;

    if (dataState !== TableDataState.READY) {
      return (
        <TableInvalidState visual={IconNames.PULSE} message={dataState} dataType={DataType.EVENT} />
      );
    }

    const mainTableRowData = this.memoizedGenerateTableRows(
      this.props.workspaceStateQuery.workspaceState,
      this.props.currentTimeInterval,
      this.props.openEventId,
      this.props.eventsInTimeRangeQuery ? this.props.eventsInTimeRangeQuery.eventsInTimeRange : [],
      this.props.signalDetectionsByStationQuery
        ? this.props.signalDetectionsByStationQuery.signalDetectionsByStation
        : [],
      this.state.showEventOfType
    );
    const numEventsInTable = this.getNumEventsInInterval();
    const numCompleteEvents = this.getNumCompleteEventsInInterval();
    const canSaveEvent =
      this.props.openEventId === undefined ||
      this.props.openEventId === null ||
      this.props.openEventId === '';
    const widthPx = this.props.glContainer
      ? this.props.glContainer.width - userPreferences.list.widthOfTableMarginsPx - 1
      : 0;
    return (
      <InteractionContext.Consumer>
        {responderCallbacks => (
          <div
            className={classNames('ag-theme-dark', 'table-container')}
            tabIndex={0}
            data-cy="events"
            onMouseEnter={e => {
              e.currentTarget.focus();
            }}
          >
            <div className={'events-status-bar'}>
              <div className={'list-toolbar-wrapper'}>
                <EventsToolbar
                  canSaveEvent={canSaveEvent}
                  completeEventsCount={numCompleteEvents}
                  disableMarkSelectedComplete={this.shouldDisableMarkSelectedComplete()}
                  eventsInTable={numEventsInTable}
                  handleMarkSelectedComplete={() => this.handleMarkSelectedComplete()}
                  onFilterChecked={val => this.onFilterChecked(val)}
                  saveAll={() => responderCallbacks.saveAllEvents()}
                  saveCurrentlyOpenEvent={() => responderCallbacks.saveCurrentlyOpenEvent()}
                  setSelectionOnAll={set => this.setSelectionOnAll(set)}
                  showEventOfType={this.state.showEventOfType}
                  widthPx={widthPx}
                />
              </div>
            </div>
            <div className={'list-wrapper'}>
              <div className={'max'}>
                <Table
                  id="table-events"
                  key="table-events"
                  context={{
                    markEventComplete: this.markEventsComplete
                  }}
                  gridOptions={{
                    getRowClass: params => `event-row ${params.data.isOpen ? 'open-event-row' : ''}`
                  }}
                  onGridReady={this.onMainTableReady}
                  columnDefs={columnDefs}
                  rowData={mainTableRowData}
                  onRowDoubleClicked={params =>
                    openEvent(
                      this.props.eventsInTimeRangeQuery.eventsInTimeRange,
                      params.data.id,
                      this.props.analystActivity,
                      this.props.updateEvents,
                      this.props.setOpenEventId
                    )
                  }
                  onRowClicked={this.onRowClicked}
                  getRowNodeId={node => node.id}
                  getRowStyle={params => ({
                    'background-color': params.data.isOpen
                      ? semanticColors.analystOpenEvent
                      : params.data.edgeEvent
                      ? gmsColors.gmsBackground
                      : '',
                    filter: params.data.edgeEvent ? 'brightness(0.5)' : ''
                  })}
                  rowSelection="multiple"
                  overlayNoRowsTemplate="No events to display"
                  rowDeselection={true}
                  suppressScrollOnNewData={this.state.suppressScrollOnNewData}
                />
              </div>
            </div>
          </div>
        )}
      </InteractionContext.Consumer>
    );
  }

  // *************************************
  // END REACT COMPONENT LIFECYCLE METHODS
  // *************************************

  /**
   * Selects the next open event in the list
   */
  private readonly openNextEventInList = () => {
    let firstEventNode = null;
    if (this.mainTable.getDisplayedRowCount() <= 1) {
      return;
    }
    this.mainTable.forEachNodeAfterFilterAndSort(node => {
      if (
        !firstEventNode &&
        node.data.edgeEvent === false &&
        ((this.props.openEventId === node.data.id && node.data.status !== 'Complete') ||
          (this.props.openEventId !== node.data.id && node.data.status !== 'Complete'))
      ) {
        openEvent(
          this.props.eventsInTimeRangeQuery.eventsInTimeRange,
          node.data.id,
          this.props.analystActivity,
          this.props.updateEvents,
          this.props.setOpenEventId
        );
        firstEventNode = node;
      }
    });
  }

  /**
   * Initialize graphql subscriptions on the apollo client
   *
   * @param EventsProps props of the event
   */
  private readonly setupSubscriptions = (props: EventsProps): void => {
    if (!props.eventsInTimeRangeQuery) return;

    // First, unsubscribe from all current subscriptions
    this.unsubscribeHandlers.forEach(unsubscribe => unsubscribe());
    this.unsubscribeHandlers.length = 0;

    // Don't register subscriptions if the current time interval is undefined/null
    if (!props.currentTimeInterval) return;
  }

  /**
   * Set class members when main table is ready
   *
   * @param event event of the table action
   */
  private readonly onMainTableReady = (event: any) => {
    this.mainTable = event.api;
  }
  /**
   * Selects rows based on props
   *
   * @param EventsProps props of the event
   */
  private readonly selectRowsFromProps = (props: EventsProps) => {
    if (this.mainTable) {
      this.mainTable.deselectAll();
      this.mainTable.forEachNode(node => {
        props.selectedEventIds.forEach(eid => {
          if (node.data.id === eid) {
            node.setSelected(true);
            this.mainTable.ensureNodeVisible(node, 'middle');
          }
        });
      });
    }
  }

  /**
   * Get the total number of events in the time interval
   *
   * @returns The number of events in the interval as number
   */
  private getNumEventsInInterval() {
    return this.props.eventsInTimeRangeQuery && this.props.eventsInTimeRangeQuery.eventsInTimeRange
      ? this.props.eventsInTimeRangeQuery.eventsInTimeRange.filter(
          event =>
            event.currentEventHypothesis.eventHypothesis.preferredLocationSolution.locationSolution
              .location.time >= this.props.currentTimeInterval.startTime &&
            event.currentEventHypothesis.eventHypothesis.preferredLocationSolution.locationSolution
              .location.time <= this.props.currentTimeInterval.endTime
        ).length
      : 0;
  }

  /**
   * Get the number of complete events in the time interval
   *
   * @returns Number of completed events in interval as number
   */
  private getNumCompleteEventsInInterval() {
    return this.props.eventsInTimeRangeQuery && this.props.eventsInTimeRangeQuery.eventsInTimeRange
      ? this.props.eventsInTimeRangeQuery.eventsInTimeRange.filter(
          event =>
            event.currentEventHypothesis.eventHypothesis.preferredLocationSolution.locationSolution
              .location.time >= this.props.currentTimeInterval.startTime &&
            event.currentEventHypothesis.eventHypothesis.preferredLocationSolution.locationSolution
              .location.time <= this.props.currentTimeInterval.endTime &&
            event.status === 'Complete'
        ).length
      : 0;
  }
  /**
   * Called when a filter is selected in the event filters
   */
  private readonly onFilterChecked = (val: Map<any, boolean>) => {
    this.setState({ showEventOfType: val });
  }

  /**
   * Called when button 'Mark selected complete' is clicked
   */
  private handleMarkSelectedComplete() {
    const selectedNodes = this.mainTable.getSelectedNodes();
    if (selectedNodes.length === 0) return;
    const eventIds = selectedNodes.map(node => node.data.id);
    const stageId = selectedNodes[0].data.stageId;
    this.markEventsComplete(eventIds, stageId);
  }

  /**
   * Determines the disabled status for 'Mark selected complete' button
   */
  private shouldDisableMarkSelectedComplete() {
    let nodesSelected = false;
    let conflictSelected = false;
    if (this.mainTable && this.mainTable.getSelectedNodes()) {
      nodesSelected = this.mainTable.getSelectedNodes().length > 0;
      conflictSelected =
        this.mainTable
          .getSelectedNodes()
          .filter(rowNode => rowNode.data.signalDetectionConflicts.length > 0).length > 0;
    }
    return !nodesSelected || conflictSelected;
  }

  /**
   * Handle table row click
   *
   * @param event Row click event
   */
  private readonly onRowClicked = (event: any) => {
    if (this.mainTable) {
      defer(() => {
        const selectedEventIds = this.mainTable.getSelectedNodes().map(node => node.data.id);
        this.props.setSelectedEventIds(selectedEventIds);
      });
    }
  }

  /**
   * Selects or deselects all in table
   *
   * @param select If true selects, if false deselects
   */
  private readonly setSelectionOnAll = (select: boolean) => {
    if (this.mainTable) {
      if (select) {
        const selectedIds = [];
        this.mainTable.forEachNodeAfterFilter((node: any) => {
          selectedIds.push(node.data.id);
        });
        this.mainTable.selectAllFiltered();
        this.props.setSelectedEventIds(selectedIds);
      } else {
        this.mainTable.deselectAll();
        this.props.setSelectedEventIds([]);
      }
    }
  }

  /**
   * Execute mutation - mark event complete
   *
   * @param eventIds event ids
   * @param processingStageId processing stage id
   */
  private readonly markEventsComplete = (eventIds: string[], processingStageId: string) => {
    const variables: EventTypes.UpdateEventsMutationArgs = {
      eventIds,
      input: {
        processingStageId,
        status: 'Complete'
      }
    };
    this.props
      .updateEvents({
        variables
      })
      .then(() => {
        this.openNextEvent();
      })
      .catch(e => UILogger.Instance().error(`Failed to open next event: ${e.message}`));
  }

  /**
   * Opens the next eligible even in the list if in event refinement mode
   */
  private readonly openNextEvent = () => {
    if (this.props.analystActivity === AnalystWorkspaceTypes.AnalystActivity.eventRefinement) {
      defer(() => {
        this.openNextEventInList();
      });
    } else {
      this.props.setOpenEventId(undefined, undefined, undefined);
    }
  }
}
