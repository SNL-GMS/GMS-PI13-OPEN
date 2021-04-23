import { IconNames } from '@blueprintjs/icons';
import { SohTypes } from '@gms/common-graphql';
import {
  isEnvironmentalIssue,
  StationAggregateType,
  UiStationSoh
} from '@gms/common-graphql/lib/graphql/soh/types';
import { CenterIcon, HorizontalDivider, ToolbarTypes } from '@gms/ui-core-components';
import { Cancelable, cloneDeep, debounce, flatMap, isEqual, max, orderBy } from 'lodash';
import memoizeOne from 'memoize-one';
import * as React from 'react';
import { dataAcquisitionUserPreferences } from '~components/data-acquisition-ui/config';
import { messageConfig } from '~components/data-acquisition-ui/config/message-config';
import { StationDeselectHandler } from '~components/data-acquisition-ui/shared/table/station-deselect-handler';
import {
  acknowledgeContextMenu,
  getWorseStatus,
  isSohStationStaleTimeMS,
  sharedSohTableClasses
} from '~components/data-acquisition-ui/shared/table/utils';
import {
  getLatestSohTime,
  initialFiltersToDisplay,
  SohToolbar
} from '~components/data-acquisition-ui/shared/toolbars/soh-toolbar';
import { DropZone } from '../../shared/table/drop-zone';
import { Columns } from './column-definitions';
import {
  StationStatisticsContext,
  StationStatisticsContextData
} from './station-statistics-context';
import { StationStatisticsTable } from './station-statistics-table';
import {
  StationStatisticsPanelProps,
  StationStatisticsPanelState,
  StationStatisticsRow,
  StationStatisticsRowClickedEvent
} from './types';

/**
 * Renders the toolbar, divider, and station statistics display.
 */
export class StationStatisticsPanel extends React.Component<
  StationStatisticsPanelProps,
  StationStatisticsPanelState
> {
  /** The context type */
  public static contextType: React.Context<StationStatisticsContextData> = StationStatisticsContext;

  /** The context wrapped around the panel, used for mutation and access to the GL Container */
  public context: React.ContextType<typeof StationStatisticsContext>;

  /**
   * A memoized function for generating the table rows.
   * @param stationSohs station soh data
   * @param groupSelected the selected group
   * @returns an array of station statistics rows
   */
  private readonly memoizedGenerateTableRows: (
    stationSohs: SohTypes.UiStationSoh[],
    groupSelected: string
  ) => StationStatisticsRow[];

  /** Default group selector text for the dropdown filter */
  private readonly defaultGroupSelectorText: string = 'All Groups';

  /** A ref to the bottom that toggles checkbox dropdown in the toolbar */
  private highlightButtonRef: HTMLDivElement = null;

  /**
   * How frequently to attempt to set the highlight
   */
  private readonly setStateTryMs: number = 200;

  /**
   * Set the highlight state on a debounced function so that many
   * event calls do not clog up the event stack.
   */
  private readonly debouncedSetStateHighlight: (() => void) & Cancelable = debounce(
    () => {
      this.setState({ isHighlighted: true });
    },
    this.setStateTryMs,
    { leading: true }
  );

  /** The reference to the acknowledged table */
  private tableAcknowledged: StationStatisticsTable;

  /** The reference to the unacknowledged table */
  private tableUnacknowledged: StationStatisticsTable;

  /**
   * constructor
   */
  public constructor(props: StationStatisticsPanelProps) {
    super(props);
    this.memoizedGenerateTableRows =
      typeof memoizeOne === 'function'
        ? memoizeOne(
            this.generateTableData,
            /* tell memoize to use a deep comparison for complex objects */
            isEqual
          )
        : this.generateTableData;
    this.state = {
      isHighlighted: false,
      statusesToDisplay: initialFiltersToDisplay,
      groupSelected: undefined,
      columnsToDisplay: new Map(
        Object.values(Columns)
          // all columns are visible by default
          .map(v => [v, true])
      )
    };
  }

  // ***************************************
  // BEGIN REACT COMPONENT LIFECYCLE METHODS
  // ***************************************

  /**
   * Renders the component.
   */
  public render() {
    /** Default top row height in pixels */
    const defaultTopRowHeightPx = 425;
    /** Icon size */
    const iconSize = 50;
    /** The selected group to filter on */
    const groupSelector = this.generateGroupSelector(
      this.props.stationGroups,
      this.state.groupSelected
    );

    const columnsToDisplayCheckBoxDropdown = this.generateColumnDisplaySelector();

    /** Will filter if a group is selected otherwise use the props passed in */
    const dataForBothTables = this.state.groupSelected
      ? this.memoizedGenerateTableRows(
          this.props.stationSohs,
          this.state.groupSelected
        ).filter(data =>
          data.stationGroups.find(group => group.groupName === this.state.groupSelected)
        )
      : this.memoizedGenerateTableRows(this.props.stationSohs, this.state.groupSelected);

    /** Filters data based on needing acknowledgement, this data is used for the top table */
    const needsAttentionSohData = dataForBothTables.filter(station => station.needsAttention);

    /** Filters the soh data that does not need attention, this data is used for the bottom table */
    const doesNotNeedAttentionSohData = dataForBothTables
      .filter(station => !station.needsAttention)
      .filter(row => this.state.statusesToDisplay.get(row.stationData.stationCapabilityStatus));

    return (
      <React.Fragment>
        <div className="soh-toolbar-container">
          <SohToolbar
            statusesToDisplay={this.state.statusesToDisplay}
            setStatusesToDisplay={this.setStatusesToDisplay}
            leftItems={[groupSelector, columnsToDisplayCheckBoxDropdown]}
            rightItems={[]}
            statusFilterText={messageConfig.labels.sohToolbar.filterStatuses}
            statusFilterTooltip={messageConfig.tooltipMessages.sohToolbar.selectStatuses}
            updatedAt={getLatestSohTime(this.props.stationSohs)}
            updateIntervalSecs={this.props.updateIntervalSecs}
            toggleHighlight={this.toggleHighlight}
            sohStationStaleTimeMS={this.context.sohStationStaleTimeMS}
            displayTimeWarning={isSohStationStaleTimeMS(
              getLatestSohTime(this.props.stationSohs),
              this.context.sohStationStaleTimeMS
            )}
          />
        </div>
        <HorizontalDivider
          topHeightPx={defaultTopRowHeightPx}
          sizeRange={dataAcquisitionUserPreferences.stationStatisticsMinContainerRange}
          top={
            <StationDeselectHandler
              setSelectedStationIds={(ids: string[]) => this.context.setSelectedStationIds(ids)}
              className={`station-statistics-table-container station-statistics-table-container--needs-attention ${sharedSohTableClasses}`}
              dataCy="soh-unacknowledged"
            >
              <div className="soh-table-label">Needs Attention</div>
              {needsAttentionSohData.length !== 0 ? (
                <StationStatisticsTable
                  ref={ref => {
                    this.tableUnacknowledged = ref;
                  }}
                  id="soh-unacknowledged"
                  tableData={needsAttentionSohData}
                  onRowClicked={this.onRowClicked}
                  acknowledgeContextMenu={this.acknowledgeContextMenu}
                />
              ) : (
                <CenterIcon
                  iconName={IconNames.TICK_CIRCLE}
                  description={'Nothing to acknowledge'}
                  iconSize={iconSize}
                  className={'table-background-icon'}
                />
              )}
            </StationDeselectHandler>
          }
          bottom={
            <StationDeselectHandler
              setSelectedStationIds={(ids: string[]) => this.context.setSelectedStationIds(ids)}
              className={`station-statistics-table-container station-statistics-table-container--acknowledged gms-drop-zone
                  ${sharedSohTableClasses}
                  ${
                    this.state.isHighlighted
                      ? 'station-statistics-table-container--highlighted'
                      : ''
                  }`}
              dataCy="soh-acknowledged"
            >
              <DropZone onDrop={(payload: string[]) => this.cellDrop(payload, this.context)}>
                <StationStatisticsTable
                  ref={ref => {
                    this.tableAcknowledged = ref;
                  }}
                  id="soh-acknowledged"
                  tableData={doesNotNeedAttentionSohData}
                  onRowClicked={this.onRowClicked}
                  acknowledgeContextMenu={(stationNames: string[]) =>
                    this.acknowledgeContextMenu(stationNames)
                  }
                  suppressContextMenu={true}
                  highlightDropZone={this.debouncedSetStateHighlight}
                />
              </DropZone>
            </StationDeselectHandler>
          }
        />
      </React.Fragment>
    );
  }

  // ***************************************
  // END REACT COMPONENT LIFECYCLE METHODS
  // ***************************************

  /**
   * Handles row selection and updated the redux state with station ID for sync selection
   * @param event StationStatisticsRowClickedEvent
   */
  private readonly onRowClicked = (event: StationStatisticsRowClickedEvent): void => {
    const stationNames: string[] = event.api.getSelectedRows().map(row => row.id);

    this.context.setSelectedStationIds(stationNames);
    if (this.tableUnacknowledged) {
      this.tableUnacknowledged.updateRowSelection(stationNames);
    }

    if (this.tableAcknowledged) {
      this.tableAcknowledged.updateRowSelection(stationNames);
    }
  }

  private readonly acknowledgeContextMenu = (stationNames: string[]): JSX.Element =>
    acknowledgeContextMenu(
      stationNames,
      this.props.stationSohs,
      this.context.sohStationStaleTimeMS,
      (names: string[], comment?: string) => this.context.acknowledgeSohStatus(names, comment)
    )

  /**
   * Cell drag logic, gets data from transfer object and calls acknowledgeSohStatus
   * @param event React.DragEvent<HTMLDivElement>
   * @param context context data for an soh panel
   */
  private readonly cellDrop = (
    stationNames: string[],
    context: StationStatisticsContextData
  ): void => {
    this.debouncedSetStateHighlight.cancel();
    this.setState({ isHighlighted: false });
    context.acknowledgeSohStatus(stationNames);
    context.setSelectedStationIds(stationNames);
  }

  /**
   * Generates the drop down for the groups
   * @param groups station groups list
   * @param groupSelected current group selected
   * @returns a toolbar item of type ToolbarTypes.ToolbarItem
   */
  private readonly generateGroupSelector = (
    groups: SohTypes.StationGroupSohStatus[],
    groupSelected: string
  ): ToolbarTypes.ToolbarItem => {
    const groupOptions = { default: this.defaultGroupSelectorText };
    orderBy(groups, 'priority', 'asc').forEach(group => {
      groupOptions[group.id] = group.stationGroupName;
    });

    return {
      rank: 2,
      label: 'Select a Group',
      tooltip: 'Show only a single group',
      type: ToolbarTypes.ToolbarItemType.Dropdown,
      onChange: e => {
        this.setGroupSelected(e !== this.defaultGroupSelectorText ? e : undefined);
      },
      dropdownOptions: groupOptions,
      value: groupSelected ? groupSelected : this.defaultGroupSelectorText,
      cyData: 'station-statistics-group-selector',
      widthPx: 150
    };
  }

  /**
   * Generates the drop down for selecting which columns are visible.
   * @returns a toolbar item of type ToolbarTypes.ToolbarItem
   */
  private readonly generateColumnDisplaySelector = (): ToolbarTypes.ToolbarItem => {
    const columnsToDisplayCheckBoxDropdown: ToolbarTypes.CheckboxDropdownItem = {
      enumOfKeys: Object.values(Columns)
        // do not allow the station column to be hidden
        .filter(v => v !== Columns.Station),
      label: 'Show columns',
      menuLabel: 'Show columns',
      rank: 3,
      widthPx: 150,
      type: ToolbarTypes.ToolbarItemType.CheckboxList,
      tooltip: 'Set which columns are visible',
      values: this.state.columnsToDisplay,
      // tslint:disable-next-line: no-console
      onChange: this.setColumnsToDisplay,
      cyData: 'filter-column'
    };
    return columnsToDisplayCheckBoxDropdown;
  }

  /**
   * @param stationSoh the soh data for the station belonging to a row
   * @param monitorType either a monitor type, or 'ENVIRONMENT' in the case of the environment cell.
   * Note that 'ENVIRONMENT' string is used instead of an enum because there is no single enum corresponding
   * to the channel issues cell. Instead, that cell is a rollup of all environmental issues.
   */
  private readonly generateChannelCellData = (
    stationSoh: SohTypes.UiStationSoh,
    monitorType: SohTypes.SohMonitorType | 'ENVIRONMENT'
  ) => {
    const contributorData =
      monitorType === 'ENVIRONMENT'
        ? this.getEnvRollup(stationSoh)
        : this.getContributorForSohMonitorType(stationSoh, monitorType);
    return {
      value: contributorData && contributorData.valuePresent ? contributorData.value : undefined,
      status: contributorData ? contributorData.statusSummary : undefined,
      isContributing: contributorData ? contributorData.contributing : false
    };
  }

  /**
   * Generates table data
   * @param props the props
   * @returns an array of table data rows
   */
  private readonly generateTableData = (
    stationSohs: SohTypes.UiStationSoh[],
    groupSelected: string
  ): StationStatisticsRow[] =>
    cloneDeep(
      flatMap(stationSohs, stationSoh => {
        const stationData = {
          stationName: stationSoh?.stationName,
          stationStatus: stationSoh?.sohStatusSummary,
          stationCapabilityStatus: groupSelected
            ? stationSoh.stationGroups.find(group => group.groupName === groupSelected)
                ?.sohStationCapability
            : this.getWorstStationCapabilityStatus(stationSoh)
        };

        // Data for channel detail

        // Channel level detail/statistics
        const channelLag = this.generateChannelCellData(stationSoh, SohTypes.SohMonitorType.LAG);
        const channelMissing = this.generateChannelCellData(
          stationSoh,
          SohTypes.SohMonitorType.MISSING
        );
        const channelEnvironment = this.generateChannelCellData(stationSoh, 'ENVIRONMENT');
        const channelTimeliness = this.generateChannelCellData(
          stationSoh,
          SohTypes.SohMonitorType.TIMELINESS
        );

        // Station level details/statistics
        // Average transmission time for acquired data samples.
        const stationLag = this.getStationStats(stationSoh, StationAggregateType.LAG);

        // Gets the total percentage of 'bad' indicators across all channels and all environmental monitors.
        const stationEnvironment = this.getStationStats(
          stationSoh,
          StationAggregateType.ENVIRONMENTAL_ISSUES
        );

        // Returns the total percentage of missing data across all channels.
        const stationMissing = this.getStationStats(stationSoh, StationAggregateType.MISSING);

        // Latest data sample time that has been acquired on any channel.
        const stationTimeliness = this.getStationStats(stationSoh, StationAggregateType.TIMELINESS);

        return {
          id: `${stationSoh.stationName}`,
          stationData,
          stationGroups: groupSelected
            ? [stationSoh.stationGroups.find(group => group.groupName === groupSelected)].filter(
                group => group !== undefined
              )
            : stationSoh.stationGroups,
          needsAcknowledgement: stationSoh.needsAcknowledgement,
          needsAttention: stationSoh.needsAttention,
          channelLag,
          channelMissing,
          channelEnvironment,
          channelTimeliness,
          stationLag,
          stationMissing,
          stationEnvironment,
          stationTimeliness
        };
      }).sort((stationA, stationB) => stationA.id.localeCompare(stationB.id))
    )

  /**
   * Returns the contributor for soh monitor type by station status
   * @param stationStatus SohTypes.UiStationSoh
   * @param monitorType: SohTypes.SohMonitorType
   * @returns SohTypes.SohContributor
   */
  private readonly getContributorForSohMonitorType = (
    stationStatus: SohTypes.UiStationSoh,
    monitorType: SohTypes.SohMonitorType
  ): SohTypes.SohContributor =>
    stationStatus.statusContributors.find(contributor => contributor.type === monitorType)

  /**
   * Toggles the highlight for the button that toggles the dropdown filter list
   * Calls a method that updates the toggleHighlight state
   * @param ref ref to the filter button
   */
  private readonly toggleHighlight = (ref: HTMLDivElement): void => {
    this.setIsHighlighted(!this.state.isHighlighted);
    this.highlightButtonRef = ref ?? this.highlightButtonRef;
    this.highlightButtonRef?.classList?.toggle('isHighlighted');
  }

  /**
   * Updates the state for isHighlighted
   * @param isHighlightedValue value to update the state with
   */
  private readonly setIsHighlighted = (isHighlightedValue: boolean): void => {
    this.setState({
      isHighlighted: isHighlightedValue
    });
  }

  /**
   * Updates the state for groupSelected
   * @param groupSelected value to update the state with
   */
  private readonly setGroupSelected = (groupSelected: string): void => {
    this.setState({
      groupSelected
    });
  }

  /**
   * Updates the state for statusesToDisplay
   * @param statusesToDisplay value to update the state with
   */
  private readonly setStatusesToDisplay = (statusesToDisplay: Map<any, boolean>): void => {
    this.setState({
      statusesToDisplay
    });
  }

  /**
   * Updates the state for which columns are displayed
   * @param columnsToDisplay value to update the state with
   */
  private readonly setColumnsToDisplay = (columnsToDisplay: Map<Columns, boolean>): void => {
    this.setState({ columnsToDisplay });
    this.tableAcknowledged?.updateColumnVisibility(columnsToDisplay);
    this.tableUnacknowledged?.updateColumnVisibility(columnsToDisplay);
  }

  /**
   * Gets the rolled up data for the environment column
   * @param stationStatus station soh to get environment rollup for
   */
  private readonly getEnvRollup = (
    stationStatus: SohTypes.UiStationSoh
  ): SohTypes.SohContributor => {
    const envContributors = stationStatus.statusContributors.filter(contributor =>
      isEnvironmentalIssue(contributor.type)
    );
    const worseStatus = envContributors
      .map(envContributor => envContributor.statusSummary)
      .reduce(getWorseStatus, SohTypes.SohStatusSummary.NONE);
    const worseValue = max(
      envContributors
        .filter(contributor => contributor.statusSummary === worseStatus)
        .map(contributor => (contributor.valuePresent ? contributor.value : undefined))
    );

    // return rollup of worse status - and the worst value of the worst status - type is not used
    return {
      contributing: envContributors.filter(c => c.contributing).length > 0,
      statusSummary: worseStatus,
      type: undefined,
      value: worseValue,
      valuePresent: worseValue !== undefined
    };
  }

  /**
   * Gets the worst capability status for the given station
   * @param station station soh
   */
  private readonly getWorstStationCapabilityStatus = (station: UiStationSoh) =>
    station.stationGroups
      .map(group => group.sohStationCapability)
      .reduce(getWorseStatus, SohTypes.SohStatusSummary.NONE)

  /**
   * Finds any one of envIssues, lag, missing, or timeliness stat for a given station
   * @param stationStatus - ui station soh we will find stats for
   * @param stationAggType - the stat we wish to find
   * @returns the percentage or seconds of the stat
   */
  private readonly getStationStats = (
    stationStatus: SohTypes.UiStationSoh,
    stationAggType: StationAggregateType
  ): number => {
    const stationAggregate = stationStatus.allStationAggregates.filter(
      stAgg => stAgg.aggregateType === stationAggType
    )[0];
    return stationAggregate && stationAggregate.valuePresent ? stationAggregate.value : undefined;
  }
}
