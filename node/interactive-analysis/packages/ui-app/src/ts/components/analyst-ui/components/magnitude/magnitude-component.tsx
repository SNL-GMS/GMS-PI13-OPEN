import { IconNames } from '@blueprintjs/icons';
import { AnalystWorkspaceTypes } from '@gms/ui-state';
import { addGlForceUpdateOnResize, addGlForceUpdateOnShow } from '@gms/ui-util';
import React from 'react';
import { getAssocSds } from '~analyst-ui/common/utils/event-util';
import {
  DataType,
  TableDataState,
  TableInvalidState
} from '~analyst-ui/common/utils/table-invalid-state';
import { systemConfig, userPreferences } from '~analyst-ui/config';
import { MagnitudePanel } from './magnitude-panel';
import { MagnitudeComponentState, MagnitudeProps } from './types';

/**
 * Magnitude display, displays various data for location solutions. It is composed of two tables
 * Network Magnitude and Station Magnitude.
 */
export class Magnitude extends React.Component<MagnitudeProps, MagnitudeComponentState> {
  /**
   * constructor
   */
  public constructor(props: MagnitudeProps) {
    super(props);

    this.state = {
      displayedMagnitudeTypes: userPreferences.initialMagType
    };
  }

  // ***************************************
  // BEGIN REACT COMPONENT LIFECYCLE METHODS
  // ***************************************

  /**
   * Invoked when the component mounted.
   */
  public componentDidMount() {
    addGlForceUpdateOnShow(this.props.glContainer, this);
    addGlForceUpdateOnResize(this.props.glContainer, this);
  }

  /**
   * Renders the component.
   */
  public render() {
    const dataState: TableDataState =
      !this.props.eventsInTimeRangeQuery || !this.props.signalDetectionsByStationQuery
        ? TableDataState.NO_INTERVAL
        : this.props.eventsInTimeRangeQuery && this.props.eventsInTimeRangeQuery.loading
        ? TableDataState.NO_EVENTS
        : this.props.signalDetectionsByStationQuery.loading ||
          !this.props.signalDetectionsByStationQuery.signalDetectionsByStation
        ? TableDataState.NO_SDS
        : !this.props.openEventId
        ? TableDataState.NO_EVENT_OPEN
        : TableDataState.READY;

    if (dataState !== TableDataState.READY) {
      return (
        <TableInvalidState
          visual={IconNames.CHANGES}
          message={dataState}
          dataType={dataState === TableDataState.NO_SDS ? DataType.SD : DataType.EVENT}
          noEventMessage={'Select an event to adjust magnitude'}
        />
      );
    }
    const currentlyOpenEvent = this.props.eventsInTimeRangeQuery.eventsInTimeRange.find(
      event => event.id === this.props.openEventId
    );

    const associatedSds = getAssocSds(
      currentlyOpenEvent,
      this.props.signalDetectionsByStationQuery.signalDetectionsByStation
    );

    return (
      <MagnitudePanel
        stations={this.props.defaultStationsQuery.defaultProcessingStations}
        eventsInTimeRange={this.props.eventsInTimeRangeQuery.eventsInTimeRange}
        associatedSignalDetections={associatedSds}
        widthPx={this.props.glContainer ? this.props.glContainer.width : 0}
        displayedMagnitudeTypes={this.state.displayedMagnitudeTypes}
        location={this.props.location}
        currentlyOpenEvent={currentlyOpenEvent}
        magnitudeTypesForPhase={systemConfig.magnitudeTypesForPhase}
        setDisplayedMagnitudeTypes={this.setDisplayedMagnitudeTypes}
        selectedSdIds={this.props.selectedSdIds}
        setSelectedSdIds={this.props.setSelectedSdIds}
        setSelectedLocationSolution={this.props.setSelectedLocationSolution}
        computeNetworkMagnitudeSolution={this.props.computeNetworkMagnitudeSolution}
        openEventId={this.props.openEventId}
      />
    );
  }

  // ***************************************
  // END REACT COMPONENT LIFECYCLE METHODS
  // ***************************************

  /**
   * Setting the state for magnitude types
   * @param displayedMagnitudeTypes the Magnitude Types to be displayed
   */
  private readonly setDisplayedMagnitudeTypes = (
    displayedMagnitudeTypes: AnalystWorkspaceTypes.DisplayedMagnitudeTypes
  ) => {
    this.setState({ displayedMagnitudeTypes });
  }
}
