import { SohTypes } from '@gms/common-graphql';
import {
  ChannelMonitorPair,
  ChannelSoh,
  isEnvironmentalIssue
} from '@gms/common-graphql/lib/graphql/soh/types';
import { setDecimalPrecisionAsNumber } from '@gms/common-util';
import { CellClickedEvent, CellContextMenuEvent, CellEvent, Table } from '@gms/ui-core-components';
import { isEqual } from 'lodash';
import uniqWith from 'lodash/uniqWith';
import memoizeOne from 'memoize-one';
import React from 'react';
import { useBaseDisplaySize } from '~components/common-ui/components/base-display/base-display-hooks';
import { dataAcquisitionUIConfig } from '~components/data-acquisition-ui/config';
import { messageConfig } from '~components/data-acquisition-ui/config/message-config';
import { showQuietingContextMenu } from '~components/data-acquisition-ui/shared/context-menus/quieting-menu';
import { SohContext, SohContextData } from '~components/data-acquisition-ui/shared/soh-context';
import { CellData } from '~components/data-acquisition-ui/shared/table/types';
import {
  getDataReceivedStatusRollup,
  getHeaderHeight,
  getRowHeightWithBorder,
  sharedSohTableClasses
} from '~components/data-acquisition-ui/shared/table/utils';
import { Offset } from '~components/data-acquisition-ui/shared/types';
import { convertSohMonitorTypeToAceiMonitorType } from '~components/data-acquisition-ui/shared/utils';
import { gmsLayout } from '~scss-config/layout-preferences';
import { FilterableSOHTypes } from '../soh-overview/types';
import {
  defaultColumnDefinition,
  getEnvironmentColumnDefinitions,
  headerNameMonitorType
} from './soh-column-definitions';
import {
  getChannelSohToDisplay,
  getEnvironmentTableRows,
  getPerChannelEnvRollup
} from './soh-environment-utils';
import {
  ChannelValueGetterParams,
  EnvironmentColumnDefinition,
  EnvironmentPanelProps,
  EnvironmentPanelState,
  EnvironmentTableContext,
  EnvironmentTableDataContext,
  EnvironmentTableRow,
  MonitorTypeValueGetterParams,
  QuietAction
} from './types';

/**
 * Soh environment panel used to process data and props coming and pass it to core table
 */
export class EnvironmentPanel extends React.PureComponent<
  EnvironmentPanelProps,
  EnvironmentPanelState
> {
  /** The SOH context type */
  public static readonly contextType: React.Context<SohContextData> = SohContext;

  /** The SOH Context */
  public context: React.ContextType<typeof SohContext>;

  /** Some size constants */
  private readonly TOP_HEADER_HEIGHT_PX: number = 112;
  private readonly PADDING_PX: number = gmsLayout.displayPaddingPx * 2;

  /** A reference to the table component. */
  public table: Table<{ id: string }, EnvironmentPanelState>;

  /** A memoized function for building the environment table column definitions. */
  private readonly memoizedGetEnvironmentColumnDefinitions: (
    channelNames: string[],
    monitorTypeValueGetter: (params: MonitorTypeValueGetterParams) => string,
    channelValueGetter: (params: ChannelValueGetterParams) => number
  ) => EnvironmentColumnDefinition[];

  /** A memoized function for building the environment table rows. */
  private readonly memoizeGetEnvironmentTableRows: (
    channels: ChannelSoh[],
    selectedChannelMonitorPairs: ChannelMonitorPair[],
    aceiType: SohTypes.AceiType
  ) => EnvironmentTableRow[];

  /** Constructor */
  private constructor(props) {
    super(props);
    this.memoizedGetEnvironmentColumnDefinitions = memoizeOne(
      getEnvironmentColumnDefinitions,
      isEqual
    );
    this.memoizeGetEnvironmentTableRows = memoizeOne(getEnvironmentTableRows, isEqual);
    this.state = {
      selectedChannelMonitorPairs: []
    };
  }

  /**
   * React lifecycle `componentDidUpdate`.
   * Called immediately after updating occurs. Not called for the initial render.
   *
   * @param prevProps the previous props
   * @param prevState the previous state
   */
  public componentDidUpdate(prevProps: EnvironmentPanelProps): void {
    if (this.table && this.table.getColumnApi()) {
      // update the column visibility based on the filter
      const channelToDisplay = getChannelSohToDisplay(
        this.props.channelSohs,
        this.props.channelStatusesToDisplay
      );
      this.table
        .getColumnApi()
        .getAllColumns()
        .forEach(col => {
          const visible =
            channelToDisplay.find(
              c => col.getColId() === headerNameMonitorType || c.channelName === col.getColId()
            ) !== undefined;
          this.table.setColumnVisible(col.getColId(), visible);
        });
    }
  }

  // ******************************************
  // BEGIN REACT COMPONENT LIFECYCLE METHODS
  // ******************************************

  /**
   * React lifecycle `render`. Renders the component.
   */
  public render() {
    return (
      <this.TableDisplayWrapper>
        <EnvironmentTableDataContext.Provider value={{ data: this.getRows() }}>
          <Table<{ id: string }, EnvironmentTableContext>
            id={`soh-environment-table`}
            key={`soh-environment-table`}
            ref={ref => (this.table = ref)}
            debug={false}
            headerHeight={getHeaderHeight()}
            rowHeight={getRowHeightWithBorder()}
            suppressRowClickSelection={true}
            suppressCellSelection={true}
            context={{
              selectedChannelMonitorPairs: this.state.selectedChannelMonitorPairs,
              rollupStatusByChannelName: new Map(
                this.props.channelSohs.map(channel => [
                  channel.channelName,
                  getPerChannelEnvRollup(channel)
                ])
              ),
              dataReceivedByChannelName: this.getDataReceivedForChannelName()
            }}
            defaultColDef={defaultColumnDefinition}
            columnDefs={this.getColumnDefinitions()}
            // provide just the row ids to the table;
            // use the react context to update the cells for performance (and memory) benefits
            rowData={this.getRows().map(r => ({ id: r.id }))}
            onCellContextMenu={this.onCellContextMenu}
            onCellClicked={this.onCellClicked}
            overlayNoRowsTemplate={messageConfig.table.noDataMessage}
          />
        </EnvironmentTableDataContext.Provider>
      </this.TableDisplayWrapper>
    );
  }

  // ******************************************
  // END REACT COMPONENT LIFECYCLE METHODS
  // ******************************************

  private readonly TableDisplayWrapper: React.FunctionComponent<
    React.PropsWithChildren<{}>
  > = props => {
    const [, heightPx] = useBaseDisplaySize();
    const minHeightPx = Math.min(
      heightPx - (this.TOP_HEADER_HEIGHT_PX + this.PADDING_PX),
      dataAcquisitionUIConfig.dataAcquisitionUserPreferences.minChartHeightPx
    );
    return (
      <div
        className={`soh-environment-table ${sharedSohTableClasses} table--cell-selection-only`}
        style={{ minHeight: minHeightPx }}
      >
        {props.children}
      </div>
    );
  }

  /**
   * Returns the column definitions for the environment table.
   */
  private readonly getColumnDefinitions = (): EnvironmentColumnDefinition[] =>
    this.memoizedGetEnvironmentColumnDefinitions(
      this.props.channelSohs.map(soh => soh.channelName).sort(),
      this.monitorTypeValueGetter,
      this.channelValueGetter
    )

  /**
   * Returns the rows for the environment table
   */
  private readonly getRows = (): EnvironmentTableRow[] => {
    const rowsUnfiltered = this.memoizeGetEnvironmentTableRows(
      this.props.channelSohs,
      this.state.selectedChannelMonitorPairs,
      this.context.selectedAceiType
    );

    const isMonitorStatusVisible = (row: EnvironmentTableRow): boolean =>
      this.props.monitorStatusesToDisplay.get(FilterableSOHTypes[row.monitorStatus]);

    return rowsUnfiltered.filter(isMonitorStatusVisible);
  }

  /**
   * Selects the table cells based on the channel monitor pair and the event.
   * @param event the table event that caused the action
   * @param channelMonitorPair the channel monitor pair to select
   */
  private readonly selectCells = (
    params: CellEvent<{ id: string }, EnvironmentTableContext, any>,
    shouldRemoveIfExisting: boolean,
    callback?: () => void
  ): void => {
    const data = this.getRows().find(d => d.id === params.data.id);

    const channelMonitorPair: ChannelMonitorPair = {
      channelName: params.colDef.colId,
      monitorType: data.monitorType
    };

    // determine if the channel monitor pair is already selected
    const isAlreadySelected: boolean =
      channelMonitorPair &&
      this.state.selectedChannelMonitorPairs.find(
        cm =>
          cm.channelName === channelMonitorPair.channelName &&
          cm.monitorType === channelMonitorPair.monitorType
      ) !== undefined;

    const event = params.event;
    let selectedChannelMonitorPairs =
      event.metaKey || event.ctrlKey || (isAlreadySelected && !shouldRemoveIfExisting)
        ? (event.metaKey || event.ctrlKey) && isAlreadySelected && shouldRemoveIfExisting
          ? [...this.state.selectedChannelMonitorPairs].filter(
              cm =>
                !(
                  cm.channelName === channelMonitorPair.channelName &&
                  cm.monitorType === channelMonitorPair.monitorType
                )
            )
          : [...this.state.selectedChannelMonitorPairs]
        : [];

    if (!isAlreadySelected) {
      selectedChannelMonitorPairs.push(channelMonitorPair);
    }

    // ensure that channel monitor pairs are unique
    selectedChannelMonitorPairs = uniqWith(
      selectedChannelMonitorPairs,
      (a: ChannelMonitorPair, b: ChannelMonitorPair) =>
        a.channelName === b.channelName && a.monitorType === b.monitorType
    );

    this.setState({ selectedChannelMonitorPairs }, callback);
  }

  /**
   * ! Getter is used for ag-grid to be able to sort, it can seem like they it used but used internally
   * The value getter for the monitor type cells.
   * @param params the value getter params
   */
  private readonly monitorTypeValueGetter = (params: MonitorTypeValueGetterParams): string => {
    const data = this.getRows().find(d => d.id === params.data.id);
    return data?.monitorType;
  }

  /**
   * ! Getter is used for ag-grid to be able to sort, it can seem like it not used but used internally
   * The value getter for the chanel cells.
   * @param params the value getter params
   */
  private readonly channelValueGetter = (params: ChannelValueGetterParams): number => {
    const data = this.getRows().find(d => d.id === params.data.id);
    const environmentalSoh = data?.valueAndStatusByChannelName.get(params.colDef.colId);
    return environmentalSoh?.value;
  }

  /**
   * Returns true if the event ocurred on the Monitor Type column; false otherwise
   * @param params the table event parameters
   */
  private readonly isMonitorTypeColumn = (
    params: CellEvent<{ id: string }, EnvironmentTableContext, any>
  ): boolean => params.colDef.colId === headerNameMonitorType

  /**
   * Table event handler for handling on cell click events
   * @param params the table event parameters
   */
  private readonly onCellClicked = (
    params: CellClickedEvent<{ id: string }, EnvironmentTableContext, any>
  ): void => {
    // ignore events on the monitor type column
    if (this.isMonitorTypeColumn(params)) {
      const data = this.getRows().find(d => d.id === params.data.id);
      this.context.setSelectedAceiType(convertSohMonitorTypeToAceiMonitorType(data.monitorType));
      return;
    }

    this.selectCells(params, true);
  }

  /**
   * Call quiet channel monitor statuses context menu
   * @param params the table event parameters
   */
  private readonly onCellContextMenu = (
    params: CellContextMenuEvent<{ id: string }, EnvironmentTableContext, any>
  ): void => {
    const data = this.getRows().find(d => d.id === params.data.id);

    // ignore events on the monitor type column
    if (this.isMonitorTypeColumn(params)) {
      return;
    }

    this.selectCells(params, false);

    const envSoh = data.valueAndStatusByChannelName.get(params.colDef.colId);
    const quietAction = this.generateQuietAction(
      this.state.selectedChannelMonitorPairs,
      { left: params.event.x, top: params.event.y },
      envSoh?.quietTimingInfo?.quietUntilMs
    );
    showQuietingContextMenu(quietAction);
  }

  private readonly quietChannelMonitor = (
    stationName: string,
    channelMonitorPairs: ChannelMonitorPair[],
    durationMs: number,
    comment?: string
  ) =>
    this.context.quietChannelMonitorStatuses(stationName, channelMonitorPairs, durationMs, comment)

  private readonly generateQuietAction = (
    channelMonitorPairs: ChannelMonitorPair[],
    position: Offset,
    quietUntilMs: number
  ): QuietAction => ({
    stationName: this.props.stationName,
    channelMonitorPairs,
    position,
    quietingDurationSelections: this.props.quietingDurationSelections,
    quietUntilMs,
    isStale: this.props.isStale,
    quietChannelMonitorStatuses: this.quietChannelMonitor
  })

  private readonly getDataReceivedForChannelName = () =>
    new Map(
      this.props.channelSohs.map(channel => {
        const cellDataForChannel: CellData[] = channel.allSohMonitorValueAndStatuses
          .filter(mvs => isEnvironmentalIssue(mvs.monitorType))
          .map(mvs => ({
            status: mvs.status,
            value: mvs && mvs.valuePresent ? setDecimalPrecisionAsNumber(mvs.value, 1) : undefined,
            isContributing: true // for the sake of getting the dataReceived rollup
          }));
        const channelDataReceivedStatus = getDataReceivedStatusRollup(cellDataForChannel);
        return [channel.channelName, channelDataReceivedStatus];
      })
    )
}
