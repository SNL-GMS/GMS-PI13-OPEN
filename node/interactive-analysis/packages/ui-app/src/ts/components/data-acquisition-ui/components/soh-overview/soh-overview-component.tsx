import { UILogger } from '@gms/ui-apollo';
import { Toaster } from '@gms/ui-util';
import React from 'react';
import { BaseDisplay } from '~components/common-ui/components/base-display';
import { isAcknowledgeEnabled } from '~components/data-acquisition-ui/shared/table/utils';
import { SohOverviewContext } from './soh-overview-context';
import { SohOverviewPanel } from './soh-overview-panel';
import { SohOverviewProps } from './types';

/**
 * Parent soh component using query to get soh status and pass down to Soh Overview
 */
export class SohOverviewComponent extends React.Component<SohOverviewProps, {}> {
  /**
   * constructor
   */
  public constructor(props) {
    super(props);
  }

  // ***************************************
  // BEGIN REACT COMPONENT LIFECYCLE METHODS
  // ***************************************

  /**
   * Renders the component.
   */
  public render() {
    return (
      <SohOverviewContext.Provider
        value={{
          sohStationStaleTimeMS: this.props.uiConfigurationQuery.uiAnalystConfiguration
            .sohStationStaleTimeMS,
          acknowledgeSohStatus: this.acknowledgeSohStatus,
          glContainer: this.props.glContainer,
          selectedStationIds: this.props.selectedStationIds ? this.props.selectedStationIds : [],
          setSelectedStationIds: this.props.setSelectedStationIds,
          stationSoh: this.props.sohStatus.stationAndStationGroupSoh.stationSoh,
          stationGroupSoh: this.props.sohStatus.stationAndStationGroupSoh.stationGroups,
          quietTimerMs: this.props.uiConfigurationQuery.uiAnalystConfiguration
            .acknowledgementQuietDuration,
          updateIntervalSecs: this.props.uiConfigurationQuery.uiAnalystConfiguration
            .reprocessingPeriod
        }}
      >
        <BaseDisplay
          glContainer={this.props.glContainer}
          className="soh-overview soh-overview-display"
        >
          <SohOverviewPanel />
        </BaseDisplay>
      </SohOverviewContext.Provider>
    );
  }

  /**
   * Call the GraphQL mutation function and save the new state to the backend.
   * @param stationIds modified station ids
   * @param comment (optional) an optional comment for the acknowledgement
   */
  private readonly acknowledgeSohStatus = (stationNames: string[], comment?: string) => {
    if (
      isAcknowledgeEnabled(
        stationNames,
        this.props.sohStatus.stationAndStationGroupSoh.stationSoh,
        this.props.uiConfigurationQuery.uiAnalystConfiguration.sohStationStaleTimeMS
      )
    ) {
      this.props
        .acknowledgeSohStatus({
          variables: {
            stationNames,
            comment
          }
        })
        .catch(err => {
          UILogger.Instance().error(err);
        });
    } else {
      const toaster = new Toaster();
      toaster.toastWarn('Cannot acknowledge due to stale SOH data');
    }
  }
}
