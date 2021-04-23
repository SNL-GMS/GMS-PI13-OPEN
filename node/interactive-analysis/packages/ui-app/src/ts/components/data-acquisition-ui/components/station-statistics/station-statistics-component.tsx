import { SohTypes } from '@gms/common-graphql';
import { MILLISECONDS_IN_SECOND, millisToStringWithMaxPrecision } from '@gms/common-util';
import { UILogger } from '@gms/ui-apollo';
import { Toaster } from '@gms/ui-util';
import isEqual from 'lodash/isEqual';
import React from 'react';
import { BaseDisplay } from '~components/common-ui/components/base-display';
import { isAcknowledgeEnabled } from '~components/data-acquisition-ui/shared/table/utils';
import { StationStatisticsContext } from './station-statistics-context';
import { StationStatisticsPanel } from './station-statistics-panel';
import { StationStatisticsProps } from './types';

/**
 * Connected component that provides context and props to SOH Panel
 */
export class StationStatisticsComponent extends React.Component<StationStatisticsProps, {}> {
  /*
   * Series of member variables to report lag statistics every 30 mins
   */

  /** Represents a half hour (30 minutes) in milliseconds */
  private readonly MS_HALF_HOUR: number = 1800000;

  /** the soh soh lag report period; how frequently the to report */
  private readonly sohLagReportPeriod: number = this.MS_HALF_HOUR;

  /** the last time the soh report was sent (reported) */
  private lastSohReport: number = Date.now();

  /** the min soh lag value */
  private minSohLag: number;

  /** the max soh lag value */
  private maxSohLag: number;

  /** the average soh lag value */
  private avgSohLag: number = 0;

  /** the soh station count - used to calculate the average */
  private sohStationsCount: number = 0;

  // Map to not check StationSOH already sent from API Gateway
  private readonly sohCheckDisplayCriteriaMap: Map<string, number>;

  /**
   * constructor
   */
  public constructor(props) {
    super(props);
    this.sohCheckDisplayCriteriaMap = new Map<string, number>();
  }

  // ***************************************
  // BEGIN REACT COMPONENT LIFECYCLE METHODS
  // ***************************************

  /**
   * React lifecycle `componentDidUpdate`.
   * Called immediately after updating occurs. Not called for the initial render.
   *
   * @param prevProps the previous props
   * @param prevState the previous state
   */
  public componentDidUpdate(prevProps: StationStatisticsProps, prevState: StationStatisticsProps) {
    if (!isEqual(prevProps.sohStatus, this.props)) {
      // Check SOH station entries to see if the lag exceeds the time limit
      this.checkSOHDisplayCriteria(this.props.sohStatus.stationAndStationGroupSoh.stationSoh);
    }
  }

  /**
   * React lifecycle `render`.
   * Renders the component.
   */
  public render() {
    return (
      <StationStatisticsContext.Provider
        value={{
          acknowledgeSohStatus: this.acknowledgeSohStatus,
          quietTimerMs: this.props.uiConfigurationQuery.uiAnalystConfiguration
            .acknowledgementQuietDuration,
          sohStationStaleTimeMS: this.props.uiConfigurationQuery.uiAnalystConfiguration
            .sohStationStaleTimeMS,
          updateIntervalSecs: this.props.uiConfigurationQuery.uiAnalystConfiguration
            .reprocessingPeriod,
          selectedStationIds: this.props.selectedStationIds,
          setSelectedStationIds: this.props.setSelectedStationIds
        }}
      >
        <BaseDisplay
          glContainer={this.props.glContainer}
          className="soh-divider-container station-statistics-display drop-zone__wrapper"
        >
          <StationStatisticsPanel
            stationGroups={this.props.sohStatus.stationAndStationGroupSoh.stationGroups}
            stationSohs={this.props.sohStatus.stationAndStationGroupSoh.stationSoh}
            updateIntervalSecs={
              this.props.uiConfigurationQuery.uiAnalystConfiguration.reprocessingPeriod
            }
            selectedStationIds={this.props.selectedStationIds}
            setSelectedStationIds={ids => this.props.setSelectedStationIds(ids)}
          />
        </BaseDisplay>
      </StationStatisticsContext.Provider>
    );
  }

  // ******************************************
  // END REACT COMPONENT LIFECYCLE METHODS
  // ******************************************

  /**
   * Call the GraphQL mutation function and save the new state to the backend.
   * @param stationIds modified station ids
   * @param comment (optional) an optional comment for the acknowledgement
   */
  private readonly acknowledgeSohStatus = (stationNames: string[], comment?: string) => {
    // If station entries are not stale or already acknowledged then
    // call acknowledge mutation with station names
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

  /**
   * Check if SOH exceeds 1 minute display to UI criteria
   */
  private readonly checkSOHDisplayCriteria = (stationStatus: SohTypes.UiStationSoh[]): void => {
    // If UI Configuration has not loaded yet bail
    if (
      !this.props.uiConfigurationQuery ||
      !this.props.uiConfigurationQuery.uiAnalystConfiguration
    ) {
      return;
    }
    const messages: string[] = [];

    stationStatus.forEach(soh => {
      if (!this.sohCheckDisplayCriteriaMap.has(soh.stationName)) {
        // Set the entry, the first time we don't check
        this.sohCheckDisplayCriteriaMap.set(soh.stationName, soh.time);
      } else if (this.sohCheckDisplayCriteriaMap.get(soh.stationName) !== soh.time) {
        // Set the entry in the map so we don't check the entry again
        this.sohCheckDisplayCriteriaMap.set(soh.stationName, soh.time);

        // Get time and scale to milliseconds
        const deltaSecs = Date.now() / MILLISECONDS_IN_SECOND - soh.time;

        // add to avg
        this.avgSohLag += deltaSecs;
        this.sohStationsCount++;

        // Check min and max
        if (!this.minSohLag || deltaSecs < this.minSohLag) {
          this.minSohLag = deltaSecs;
        }

        if (!this.maxSohLag || deltaSecs > this.maxSohLag) {
          this.maxSohLag = deltaSecs;
        }
      }
    });

    // only log if there are messages to log
    if (messages && messages.length > 0) {
      UILogger.Instance().warn(...messages);
    }

    // Check if need to send statistic report
    if (Date.now() > this.lastSohReport + this.sohLagReportPeriod) {
      UILogger.Instance().info(
        `SOH lag statistics for last ${millisToStringWithMaxPrecision(this.sohLagReportPeriod)}. ` +
          `Max lag: ${millisToStringWithMaxPrecision(this.maxSohLag * MILLISECONDS_IN_SECOND)}, ` +
          `Min lag ${millisToStringWithMaxPrecision(this.minSohLag * MILLISECONDS_IN_SECOND)}, ` +
          `Avg lag ${millisToStringWithMaxPrecision(
            (this.avgSohLag / this.sohStationsCount) * MILLISECONDS_IN_SECOND
          )}`
      );

      // Reset statistics
      this.lastSohReport = Date.now();
      this.sohStationsCount = 0;
      this.avgSohLag = 0;
      this.minSohLag = undefined;
      this.maxSohLag = undefined;
    }
  }
}
