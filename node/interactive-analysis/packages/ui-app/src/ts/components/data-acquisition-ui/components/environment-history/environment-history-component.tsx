import { SohTypes } from '@gms/common-graphql';
import { nonIdealStateWithNoSpinner, nonIdealStateWithSpinner } from '@gms/ui-core-components';
import React from 'react';
import { BaseDisplay } from '~components/common-ui/components/base-display';
import { isAnalogAceiMonitorType } from '~components/data-acquisition-ui/shared/utils';
import { EnvironmentHistoryPanel } from './environment-history-panel';
import { AceiContext, EnvironmentHistoryProps } from './types';

/**
 * Parent SOH Environment using query to get soh status
 */
export class EnvironmentHistoryComponent extends React.PureComponent<EnvironmentHistoryProps, {}> {
  public constructor(props) {
    super(props);
  }

  // ***************************************
  // BEGIN REACT COMPONENT LIFECYCLE METHODS
  // ***************************************

  public render() {
    // Determine if non ideal state
    const isNonIdealState = this.isNonIdealState();
    if (isNonIdealState) {
      return isNonIdealState;
    }

    const station = this.getStation();
    const channelSohs = station.channelSohs;

    return (
      <BaseDisplay
        glContainer={this.props.glContainer}
        className="environment-history-display top-level-container scroll-box scroll-box--y full-width-height soh-env-component"
        onContextMenu={e => {
          e.preventDefault();
        }}
      >
        <AceiContext.Provider
          value={{
            selectedAceiType: this.props.selectedAceiType,
            setSelectedAceiType: this.props.setSelectedAceiType
          }}
        >
          <EnvironmentHistoryPanel
            station={station}
            channelSohs={channelSohs}
            sohHistoricalDurations={
              this.props.uiConfigurationQuery.uiAnalystConfiguration.sohHistoricalDurations
            }
          />
        </AceiContext.Provider>
      </BaseDisplay>
    );
  }

  // ***************************************
  // END REACT COMPONENT LIFECYCLE METHODS
  // ***************************************

  /** Returns the selected station */
  private readonly getStation = (): SohTypes.UiStationSoh =>
    this.props.sohStatus.stationAndStationGroupSoh.stationSoh.find(
      s => s.stationName === this.props.selectedStationIds[0]
    )

  /**
   * Checks the props and determines if we should go into a non ideal state for the component
   */
  private readonly isNonIdealState = () => {
    // If the golden-layout container is not visible, do not attempt to render
    // the component, this is to prevent JS errors that may occur when trying to
    // render the component while the golden-layout container is hidden

    const station = this.getStation();

    const channelSohs = station ? station.channelSohs : [];

    return this.props.glContainer && this.props.glContainer.isHidden
      ? nonIdealStateWithNoSpinner()
      : !this.props.sohStatus
      ? nonIdealStateWithSpinner('No SOH Data', 'Station SOH')
      : this.props.sohStatus && this.props.sohStatus.loading
      ? nonIdealStateWithSpinner('Loading:', 'Station SOH')
      : !this.props.sohStatus.stationAndStationGroupSoh
      ? nonIdealStateWithSpinner('No Station Group Data:', 'For SOH')
      : !this.props.selectedStationIds || this.props.selectedStationIds.length === 0
      ? nonIdealStateWithNoSpinner(
          'No Station Selected',
          'Select a station in SOH Overview or Station Statistics to view Environment'
        )
      : !station
      ? nonIdealStateWithSpinner('Loading:', 'Station SOH')
      : this.props.selectedStationIds.length > 1
      ? nonIdealStateWithNoSpinner(
          'Multiple Stations Selected',
          'Select one station to see Environment'
        )
      : !channelSohs
      ? nonIdealStateWithSpinner('Loading', 'Channel SOH')
      : channelSohs.length === 0
      ? nonIdealStateWithNoSpinner('No Channel Data', "Check this station's configuration")
      : isAnalogAceiMonitorType(this.props.selectedAceiType)
      ? // TODO: Remove this when analog monitor types are supported
        nonIdealStateWithNoSpinner(
          'Unsupported monitor type',
          'Analog environmental monitor types not supported at this time. Select a boolean monitor type to see historical trends.'
        )
      : undefined;
  }
}
