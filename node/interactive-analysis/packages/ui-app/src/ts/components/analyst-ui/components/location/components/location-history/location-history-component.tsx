import { EventTypes } from '@gms/common-graphql';
import { dateToISOString, MILLISECONDS_IN_SECOND } from '@gms/common-util';
import { Table, TableApi } from '@gms/ui-core-components';
import { AnalystWorkspaceTypes } from '@gms/ui-state';
import classNames from 'classnames';
import cloneDeep from 'lodash/cloneDeep';
import flatMap from 'lodash/flatMap';
import isEqual from 'lodash/isEqual';
import sortBy from 'lodash/sortBy';
import uniqueId from 'lodash/uniqueId';
import memoizeOne from 'memoize-one';
import React from 'react';
import { EventUtils } from '~analyst-ui/common/utils';
import {
  getLatestLocationSolutionSet,
  getPreferredDefaultLocationId,
  getPreferredLocationSolutionIdFromEventHypothesis,
  shouldUpdateSelectedLocationSolution
} from '~analyst-ui/common/utils/event-util';
import { gmsColors } from '~scss-config/color-preferences';
import { autoGroupColumnDef, columnDefs, PREFERRED_FIELD_NAME } from './table-utils/column-defs';
import { LocationHistoryProps, LocationHistoryRow, LocationHistoryState } from './types';

/**
 * Displays a history of computed locations for the event
 */
export class LocationHistory extends React.Component<LocationHistoryProps, LocationHistoryState> {
  /**
   * A memoized function for generating the table rows.
   *
   * @param event the event
   * @param location  the selected location and preferred location solutions
   *
   * @returns an array of location history rows
   */
  private readonly memoizedGenerateTableRows: (
    event: EventTypes.Event,
    location: AnalystWorkspaceTypes.LocationSolutionState
  ) => LocationHistoryRow[];

  /** The ag-grid table reference */
  private mainTable: TableApi;

  /**
   * constructor
   */
  public constructor(props: LocationHistoryProps) {
    super(props);
    this.memoizedGenerateTableRows =
      typeof memoizeOne === 'function'
        ? memoizeOne(
            this.generateTableRows,
            /* tell memoize to use a deep comparison for complex objects */
            isEqual
          )
        : this.generateTableRows;
  }

  // ***************************************
  // BEGIN REACT COMPONENT LIFECYCLE METHODS
  // ***************************************

  /**
   * Renders the component.
   */
  public render() {
    const mainTableRowData = this.memoizedGenerateTableRows(this.props.event, this.props.location);

    return (
      <div className={classNames('ag-theme-dark', 'table-container')}>
        <div className="list-wrapper">
          <div className={'max'}>
            <Table
              id="table-location-history"
              key="table-location-history"
              context={{}}
              columnDefs={columnDefs}
              gridOptions={{
                rowClass: 'location-history-row'
              }}
              rowData={mainTableRowData}
              getRowNodeId={node => node.id}
              rowSelection="none"
              suppressContextMenu={true}
              onGridReady={this.onMainTableReady}
              onRowClicked={this.onRowClicked}
              onCellClicked={this.onCellClicked}
              getRowStyle={params => ({
                'border-bottom': params.data.isLastInLSSet
                  ? `1px dotted ${gmsColors.gmsBackground}`
                  : null,
                'background-color':
                  this.props.location.selectedLocationSolutionSetId === params.data.locationSetId
                    ? this.props.location.selectedLocationSolutionId ===
                      params.data.locationSolutionId
                      ? gmsColors.gmsTableSubsetSelected
                      : gmsColors.gmsTableSelection
                    : params.data.count % 2 === 0
                    ? gmsColors.gmsBackground
                    : gmsColors.gmsProminentBackground,
                cursor: 'pointer'
              })}
              autoGroupColumnDef={autoGroupColumnDef}
              groupMultiAutoColumn={true}
            />
          </div>
        </div>
      </div>
    );
  }

  public componentDidUpdate(prevProps: LocationHistoryProps) {
    if (this.mainTable && this.mainTable.getSortModel().length === 0) {
      this.mainTable.setSortModel([{ colId: 'locationSetId', sort: 'desc' }]);
    }

    const latestLocationSS = getLatestLocationSolutionSet(this.props.event);
    if (prevProps.event.id !== this.props.event.id && latestLocationSS) {
      this.props.setSelectedPreferredLocationSolution(
        latestLocationSS.id,
        getPreferredLocationSolutionIdFromEventHypothesis(
          this.props.event.currentEventHypothesis.eventHypothesis
        )
      );
      // Else if location solution length or event hyp id has been updated reset selected
    } else if (
      shouldUpdateSelectedLocationSolution(prevProps.event, this.props.event) &&
      latestLocationSS
    ) {
      this.props.setSelectedPreferredLocationSolution(
        latestLocationSS.id,
        getPreferredDefaultLocationId(latestLocationSS)
      );
    }
  }

  // ***************************************
  // END REACT COMPONENT LIFECYCLE METHODS
  // ***************************************

  /**
   * Generates the table rows.
   *
   * @param event the event
   * @param location the selected location and preferred location solution state
   *
   * @returns an array of location history rows
   */
  private readonly generateTableRows = (
    event: EventTypes.Event,
    location: AnalystWorkspaceTypes.LocationSolutionState
  ): LocationHistoryRow[] => {
    // Walk through the location solution sets adding them to the history table. There
    // should be one entry in the history table per set.

    if (
      !event ||
      !event.currentEventHypothesis ||
      !event.currentEventHypothesis.eventHypothesis ||
      !event.currentEventHypothesis.eventHypothesis.locationSolutionSets
    ) {
      return [];
    }

    // Reverse is ok on received props
    const locationSolutionSets = event.currentEventHypothesis.eventHypothesis.locationSolutionSets;
    locationSolutionSets.sort((lssA, lssB) => lssB.count - lssA.count);
    const rowData: LocationHistoryRow[] = flatMap(locationSolutionSets, (lsSet, index) =>
      this.generateChildRows(
        lsSet,
        event.currentEventHypothesis.eventHypothesis.id,
        location.selectedPreferredLocationSolutionId,
        location.selectedLocationSolutionSetId
      )
    );

    return rowData;
  }

  /**
   * Generates the the child location history rows.
   *
   * @param locationSolutionSet the location solution set
   * @param eventHypothesisId the event hypothesis id
   * @param preferredLocationId the preferred location solution id
   * @param selectedLocationSolutionSetId the selected location solution set id
   *
   * @returns an location history rows
   */
  private readonly generateChildRows = (
    locationSolutionSet: EventTypes.LocationSolutionSet,
    eventHypothesisId: string,
    preferredLocationId: string,
    selectedLocationSolutionSetId: string
  ): LocationHistoryRow[] => {
    const childRows: LocationHistoryRow[] = flatMap(
      Object.keys(EventTypes.DepthRestraintType),
      key => {
        const maybeLS:
          | EventTypes.LocationSolution
          | undefined = locationSolutionSet.locationSolutions.find(
          ls => ls.locationRestraint.depthRestraintType === key
        );
        if (maybeLS) {
          return this.generateChildRow(
            locationSolutionSet,
            maybeLS,
            preferredLocationId,
            selectedLocationSolutionSetId
          );
        }
      }
    ).filter(maybeChildRow => maybeChildRow !== undefined);
    const areAnyPreferred = childRows.reduce(
      (accumulator, currentValue) => accumulator || currentValue.preferred,
      false
    );
    const sortedChildRows = sortBy(childRows, cr => cr.depthRestraintType);
    const childRowsPrime = cloneDeep(sortedChildRows).map((row, index) => ({
      ...row,
      isLastInLSSet: index === childRows.length - 1,
      isFirstInLSSet: index === 0,
      isLocationSolutionSetPreferred: areAnyPreferred
    }));
    return childRowsPrime;
  }

  /**
   * Generates a single location history row.
   *
   * @param locationSolutionSet the location solution set
   * @param locationSolution the location solution
   * @param preferredLocationId the preferred location solution id
   * @param selectedLocationSolutionSetId the selected location solution set id
   *
   * @returns an location history row
   */
  private readonly generateChildRow = (
    locationSolutionSet: EventTypes.LocationSolutionSet,
    locationSolution: EventTypes.LocationSolution,
    preferredLocationId: string,
    selectedLocationSolutionSetId: string
  ): LocationHistoryRow => {
    const location = locationSolution.location;
    const ellipse =
      locationSolution.locationUncertainty &&
      locationSolution.locationUncertainty.ellipses.length > 0
        ? locationSolution.locationUncertainty.ellipses[0]
        : undefined;
    const majorAxis = ellipse ? parseFloat(ellipse.majorAxisLength) : undefined;
    const minorAxis = ellipse ? parseFloat(ellipse.minorAxisLength) : undefined;
    const capitalizedType =
      locationSolution.locationType.charAt(0).toUpperCase() +
      locationSolution.locationType.slice(1);
    const locationData: LocationHistoryRow = {
      id: uniqueId(),
      locationSolutionId: locationSolution.id,
      locationSetId: locationSolutionSet.id,
      latestLSSId: getLatestLocationSolutionSet(this.props.event).id,
      locType: capitalizedType,
      lat: location.latitudeDegrees,
      lon: location.longitudeDegrees,
      depth: location.depthKm,
      time: dateToISOString(new Date(locationSolution.location.time * MILLISECONDS_IN_SECOND)),
      restraint:
        EventTypes.PrettyDepthRestraint[locationSolution.locationRestraint.depthRestraintType],
      smajax: majorAxis,
      sminax: minorAxis,
      strike: ellipse ? ellipse.majorAxisTrend : undefined,
      stdev: locationSolution.locationUncertainty
        ? locationSolution.locationUncertainty.stDevOneObservation
        : undefined,
      preferred: preferredLocationId === locationSolution.id,
      count: locationSolutionSet.count,
      setSelectedPreferredLocationSolution: this.props.setSelectedPreferredLocationSolution,
      setToSave: this.setToSave,
      depthRestraintType: locationSolution.locationRestraint.depthRestraintType,
      selectedLocationSolutionSetId
    };
    return locationData;
  }

  /**
   * Sets the location solution to save
   *
   * @param locationSolutionSetId the location solution set id
   * @param locationSolutionId  the location solution id
   */
  private readonly setToSave = (locationSolutionSetId: string, locationSolutionId: string) => {
    const locationSolutionSet = this.props.event.currentEventHypothesis.eventHypothesis.locationSolutionSets.find(
      lss => lss.id === locationSolutionSetId
    );
    const newPreferredId = EventUtils.getPreferredDefaultLocationId(locationSolutionSet);
    this.props.setSelectedPreferredLocationSolution(locationSolutionSetId, newPreferredId);
  }

  /**
   * Ag-grid onRowClicked event handler
   *
   * @params params the event params passed by ag-grid
   */
  private readonly onRowClicked = (params: any) => {
    this.props.setSelectedLocationSolution(
      params.data.locationSetId,
      params.data.locationSolutionId
    );
  }

  /**
   * Ag-grid onCellClicked event handler
   *
   * @params params the event params passed by ag-grid
   */
  private readonly onCellClicked = (params: any) => {
    if (params && params.column) {
      // handle cell click events for the columns with custom cell renderers
      // that contain a blueprintjs switch component
      // invoke the switch onChange event handler when these cells are clicked
      if (
        params.colDef.field === PREFERRED_FIELD_NAME &&
        params.data.latestLSSId === params.data.locationSetId
      ) {
        const locationSolutionId = params.data.locationSolutionId;
        const locationSetId = params.data.locationSetId;
        this.props.setSelectedPreferredLocationSolution(locationSetId, locationSolutionId);
      }
    }
  }

  /**
   * Event handler for ag-grid that is fired when the table is ready
   *
   * @param event event of the table action
   */
  private readonly onMainTableReady = (event: any) => {
    this.mainTable = event.api;
    this.mainTable.setSortModel([{ colId: 'count', sort: 'desc' }]);
  }
}
