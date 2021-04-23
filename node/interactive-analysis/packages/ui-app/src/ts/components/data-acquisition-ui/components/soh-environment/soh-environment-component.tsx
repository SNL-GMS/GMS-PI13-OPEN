import { SohTypes } from '@gms/common-graphql';
import { UILogger } from '@gms/ui-apollo';
import { ToolbarTypes } from '@gms/ui-core-components';
import cloneDeep from 'lodash/cloneDeep';
import React from 'react';
import { BaseDisplay } from '~components/common-ui/components/base-display';
import { messageConfig } from '~components/data-acquisition-ui/config/message-config';
import { dataAcquisitionUserPreferences } from '~components/data-acquisition-ui/config/user-preferences';
import { DrillDownTitle } from '~components/data-acquisition-ui/shared/drill-down-components';
import { SohContext, SohContextData } from '~components/data-acquisition-ui/shared/soh-context';
import { isSohStationStaleTimeMS } from '~components/data-acquisition-ui/shared/table/utils';
import { initialFiltersToDisplay } from '~components/data-acquisition-ui/shared/toolbars/soh-toolbar';
import { FilterableSOHTypes, FilterableSohTypesDisplayStrings } from '../soh-overview/types';
import { EnvironmentPanel } from './soh-environment-panel';
import { EnvironmentToolbar, UpdateInfo } from './soh-environment-toolbar';
import { EnvironmentProps, EnvironmentState } from './types';

/** the filter item width in pixels */
const filterItemWidthPx = 240;

/**
 * Parent SOH Environment using query to get soh status
 */
export class EnvironmentComponent extends React.PureComponent<EnvironmentProps, EnvironmentState> {
  /**
   * constructor
   */
  public constructor(props) {
    super(props);
    this.state = {
      monitorStatusesToDisplay: cloneDeep(initialFiltersToDisplay),
      channelStatusesToDisplay: cloneDeep(initialFiltersToDisplay)
    };
  }

  // ***************************************
  // BEGIN REACT COMPONENT LIFECYCLE METHODS
  // ***************************************

  /**
   * Renders the component.
   */
  public render() {
    const station = this.getStation();
    return (
      <SohContext.Provider value={this.getContextDefaults()}>
        <BaseDisplay
          glContainer={this.props.glContainer}
          className="environment-display top-level-container  scroll-box scroll-box--y full-width-height soh-env-component"
          onContextMenu={e => {
            e.preventDefault();
          }}
        >
          <this.EnvironmentChartHeader />
          <EnvironmentPanel
            channelSohs={station.channelSohs}
            channelStatusesToDisplay={this.state.channelStatusesToDisplay}
            monitorStatusesToDisplay={this.state.monitorStatusesToDisplay}
            isStale={isSohStationStaleTimeMS(
              station.time,
              this.props.uiConfigurationQuery.uiAnalystConfiguration.sohStationStaleTimeMS
            )}
            defaultQuietDurationMs={
              this.props.uiConfigurationQuery.uiAnalystConfiguration.acknowledgementQuietDuration
            }
            quietingDurationSelections={
              this.props.uiConfigurationQuery.uiAnalystConfiguration.availableQuietDurations
            }
            stationName={station.stationName}
          />
        </BaseDisplay>
      </SohContext.Provider>
    );
  }

  // ***************************************
  // END REACT COMPONENT LIFECYCLE METHODS
  // ***************************************

  /** Renders the title */
  private readonly Title: React.FunctionComponent<{ stationName: string }> = ({ stationName }) => (
    <DrillDownTitle title={stationName} subtitle={messageConfig.labels.environmentalSubtitle} />
  )

  /** Renders the environment toolbar */
  private readonly EnvironmentToolbar: React.FunctionComponent<{}> = () => (
    <EnvironmentToolbar
      setMonitorStatusesToDisplay={statuses => {
        this.setState({ monitorStatusesToDisplay: statuses });
      }}
      filterDropdown={[this.makeFilterDropDown()]}
      monitorStatusesToDisplay={this.state.monitorStatusesToDisplay}
      updateInfo={this.generateUpdateInfo()}
    />
  )

  // tslint:disable-next-line: arrow-return-shorthand
  private readonly EnvironmentChartHeader: React.FunctionComponent<{}> = () => {
    return (
      <div>
        <this.EnvironmentToolbar />
        <this.Title stationName={this.getStation().stationName} />
      </div>
    );
  }

  /**
   * Returns the default values for the Context
   */
  private readonly getContextDefaults = (): SohContextData => ({
    glContainer: this.props.glContainer,
    selectedAceiType: this.props.selectedAceiType,
    quietChannelMonitorStatuses: this.quietChannelMonitorStatuses,
    setSelectedAceiType: this.props.setSelectedAceiType
  })

  /** Returns the selected station */
  private readonly getStation = (): SohTypes.UiStationSoh =>
    this.props.sohStatus?.stationAndStationGroupSoh?.stationSoh?.find(
      s => s.stationName === this.props.selectedStationIds[0]
    )

  /**
   * Call the GraphQL mutation function to quiet a channel monitor status.
   * @param stationName name of the station
   * @param channelPairs channel monitor pairs to quiet
   * @param quietDurationMs the duration to quiet the channel/monitor
   * @param comment (optional) the comment for the quiet action
   */
  private readonly quietChannelMonitorStatuses = (
    stationName: string,
    channelPairs: SohTypes.ChannelMonitorPair[],
    quietDurationMs: number,
    comment?: string
  ) => {
    const input: SohTypes.ChannelMonitorInput = {
      channelMonitorPairs: channelPairs,
      stationName,
      quietDurationMs,
      comment
    };
    this.props
      .quietChannelMonitorStatuses({
        variables: {
          channelMonitorsToQuiet: input
        }
      })
      .catch(err => {
        UILogger.Instance().error(err);
      });
  }

  /**
   * Creates the filter drop down
   */
  private readonly makeFilterDropDown = (): ToolbarTypes.CheckboxDropdownItem => ({
    enumOfKeys: FilterableSOHTypes,
    label: 'Filter Channels by Status',
    menuLabel: 'Filter Channels by Status',
    rank: 2,
    widthPx: filterItemWidthPx,
    type: ToolbarTypes.ToolbarItemType.CheckboxList,
    tooltip: 'Filter Channels by Status',
    values: this.state.channelStatusesToDisplay,
    enumKeysToDisplayStrings: FilterableSohTypesDisplayStrings,
    onChange: statuses => this.setState({ channelStatusesToDisplay: statuses }),
    cyData: 'filter-soh-channels',
    colors: new Map([
      [FilterableSOHTypes.GOOD, dataAcquisitionUserPreferences.colors.ok],
      [FilterableSOHTypes.MARGINAL, dataAcquisitionUserPreferences.colors.warning],
      [FilterableSOHTypes.BAD, dataAcquisitionUserPreferences.colors.strongWarning],
      [FilterableSOHTypes.NONE, 'NULL_CHECKBOX_COLOR_SWATCH']
    ])
  })

  /**
   * Creates the update information for the toolbar.
   */
  private readonly generateUpdateInfo = (): UpdateInfo => ({
    reprocessingPeriod: this.props.uiConfigurationQuery.uiAnalystConfiguration.reprocessingPeriod,
    sohStationStaleTimeMS: this.props.uiConfigurationQuery.uiAnalystConfiguration
      .sohStationStaleTimeMS,
    updateTime: this.getStation().time
  })
}
