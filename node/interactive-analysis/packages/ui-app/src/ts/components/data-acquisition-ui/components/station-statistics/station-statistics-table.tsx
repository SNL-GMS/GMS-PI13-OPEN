import { ContextMenu } from '@blueprintjs/core';
import {
  NumberCellRendererParams,
  NumberValueGetter,
  StringValueGetter,
  Table
} from '@gms/ui-core-components';
import delay from 'lodash/delay';
import includes from 'lodash/includes';
import * as React from 'react';
import { messageConfig } from '~components/data-acquisition-ui/config/message-config';
import {
  getHeaderHeight,
  getRowHeightWithBorder
} from '~components/data-acquisition-ui/shared/table/utils';
import { Offset } from '~components/data-acquisition-ui/shared/types';
import { buildColumnDefs, Columns, defaultColumnDefinition } from './column-definitions';
import {
  StationStatisticsContext,
  StationStatisticsContextData
} from './station-statistics-context';
import {
  StationStatisticsCellClickedEvent,
  StationStatisticsColumnDefinition,
  StationStatisticsRow,
  StationStatisticsTableDataContext,
  StationStatisticsTableProps,
  StationStatisticsTableState
} from './types';

/**
 * Station statistics table, provides a Station level SOH focusing on:
 * channelMissing, channelLag, channelEnvironment, and channelTimeliness,
 * stationMissing, stationLag, stationEnvironment, and stationTimeliness
 */
export class StationStatisticsTable extends React.Component<
  StationStatisticsTableProps,
  StationStatisticsTableState
> {
  /** the context type */
  public static readonly contextType: React.Context<
    StationStatisticsContextData
  > = StationStatisticsContext;

  /** the station statistics context contains station statistics scoped globals */
  public context: React.ContextType<typeof StationStatisticsContext>;

  /** the table column definitions */
  private readonly columnDefinitions: StationStatisticsColumnDefinition[];

  /**
   * A reference to the table.
   * Usage:
   * this.tableRef.getTableApi() return the table API.
   * this.tableRef.getColumnApi() returns the column API
   */
  private tableRef: Table<{ id: string }, {}>;

  /** delay updating selection by this many ms */
  private readonly SELECTION_UPDATE_DELAY_MS: number = 50;

  /**
   * The value getter for the channel lag cells.
   * Used to sort and filter cells, so AG Grid can get the value in the cell
   * from any data object
   * @param params the value getter params
   */
  private readonly channelLagValueGetter: NumberValueGetter = this.buildValueGetter<number>(
    data => data?.channelLag.value
  );

  /**
   * The value getter for the channel missing cells.
   * Used to sort and filter cells, so AG Grid can get the value in the cell
   * from any data object
   * @param params the value getter params
   */
  private readonly channelMissingValueGetter: NumberValueGetter = this.buildValueGetter<number>(
    data => data?.channelMissing.value
  );

  /**
   * The value getter for the channel environment cells.
   * Used to sort and filter cells, so AG Grid can get the value in the cell
   * from any data object
   * @param params the value getter params
   */
  private readonly channelEnvironmentValueGetter: NumberValueGetter = this.buildValueGetter<number>(
    data => data?.channelEnvironment.value
  );

  /**
   * The value getter for the channel timeliness cells.
   * Used to sort and filter cells, so AG Grid can get the value in the cell
   * from any data object
   * @param params the value getter params
   */
  private readonly channelTimelinessValueGetter: NumberValueGetter = this.buildValueGetter<number>(
    data => data?.channelTimeliness.value
  );

  /**
   * The value getter for the station lag cells.
   * Used to sort and filter cells, so AG Grid can get the value in the cell
   * from any data object
   * @param params the value getter params
   */
  private readonly stationLagValueGetter: NumberValueGetter = this.buildValueGetter<number>(
    data => data?.stationLag
  );

  /**
   * The value getter for the station missing cells.
   * Used to sort and filter cells, so AG Grid can get the value in the cell
   * from any data object
   * @param params the value getter params
   */
  private readonly stationMissingValueGetter: NumberValueGetter = this.buildValueGetter<number>(
    data => data?.stationMissing
  );

  /**
   * The value getter for the channel timeliness cells.
   * Used to sort and filter cells, so AG Grid can get the value in the cell
   * from any data object
   * @param params the value getter params
   */
  private readonly stationEnvironmentValueGetter: NumberValueGetter = this.buildValueGetter<number>(
    data => data?.stationEnvironment
  );

  /**
   * The value getter for the station missing cells.
   * Used to sort and filter cells, so AG Grid can get the value in the cell
   * from any data object
   * @param params the value getter params
   */
  private readonly stationTimelinessValueGetter: NumberValueGetter = this.buildValueGetter<number>(
    data => data?.stationTimeliness
  );

  /**
   * The value getter for the station name cells.
   * @param params the value getter params
   */
  private readonly stationNameValueGetter: StringValueGetter = this.buildValueGetter(
    data => data.stationData.stationName
  );

  /**
   * Component class constructor —
   * Define the column definitions for the table
   */
  public constructor(props) {
    super(props);
    this.columnDefinitions = buildColumnDefs(
      this.stationNameValueGetter,
      this.stationMissingValueGetter,
      this.stationTimelinessValueGetter,
      this.stationLagValueGetter,
      this.stationEnvironmentValueGetter,
      this.channelMissingValueGetter,
      this.channelTimelinessValueGetter,
      this.channelLagValueGetter,
      this.channelEnvironmentValueGetter
    );
  }

  // ******************************************
  // BEGIN REACT COMPONENT LIFECYCLE METHODS
  // ******************************************

  /**
   * React lifecycle `componentDidUpdate`.
   * Called immediately after updating occurs. Not called for the initial render.
   *
   * @param prevProps the previous props
   * @param prevState the previous state
   */
  public componentDidUpdate(prevProps: StationStatisticsTableProps) {
    if (!this.props.suppressSelection) {
      delay(
        () => this.updateRowSelection(this.context.selectedStationIds),
        this.SELECTION_UPDATE_DELAY_MS
      );
    }
  }

  /**
   * React lifecycle `render`.
   * Renders the component.
   */
  public render() {
    return (
      <div
        className="station-statistics-table__wrapper"
        data-cy="station-statistics-table__wrapper"
      >
        <StationStatisticsTableDataContext.Provider value={{ data: this.props.tableData }}>
          <Table<{ id: string }, {}>
            id={`station-statistics-table-${this.props.id}`}
            key={`station-statistics-table-${this.props.id}`}
            ref={ref => (this.tableRef = ref)}
            context={this.context}
            // provide just the row ids to the table;
            // use the react context to update the cells for performance (and memory) benefits
            rowData={this.props.tableData.map(r => ({ id: r.id }))}
            defaultColDef={defaultColumnDefinition}
            columnDefs={this.columnDefinitions}
            headerHeight={getHeaderHeight()}
            overlayNoRowsTemplate={messageConfig.table.noDataMessage}
            rowHeight={getRowHeightWithBorder()}
            rowSelection={'multiple'}
            rowDeselection={true}
            suppressCellSelection={true}
            onRowClicked={this.props.onRowClicked}
            onCellContextMenu={this.onCellContextMenu}
            suppressContextMenu={this.props.suppressContextMenu ?? false}
          />
        </StationStatisticsTableDataContext.Provider>
      </div>
    );
  }

  // ******************************************
  // END REACT COMPONENT LIFECYCLE METHODS
  // ******************************************

  /**
   * Updates the row selection with the provided ids - synced selection
   * @param ids the ids to select
   */
  public readonly updateRowSelection = (ids: string[]): void => {
    if (this.tableRef && this.tableRef.getTableApi()) {
      const selectedNodes = this.tableRef.getTableApi().getSelectedNodes();
      selectedNodes.forEach(rowNode => {
        if (!includes(ids, rowNode.id)) {
          rowNode.setSelected(false);
        }
      });

      this.tableRef.getTableApi().forEachNode(rowNode => {
        if (includes(ids, rowNode.id)) {
          rowNode.setSelected(true);
        }
      });
    }
  }

  /**
   * Updates the column visibility based on the state provided.
   * @param state the mapping of unique column id and the visible state
   */
  public readonly updateColumnVisibility = (state: Map<Columns, boolean>) => {
    state.forEach((v, k) => {
      this.tableRef?.getColumnApi()?.setColumnVisible(k, v);
    });
  }

  // ******************************************
  // START PRIVATE METHODS
  // ******************************************

  /**
   * Creates a value getter function that finds the corresponding row and then
   * uses the provided getter function to return the value.
   * Value getters are used by AG Grid to sort/filter rows.
   * They are passed in to the column definition for each column.
   * @param getter a function that accesses the value from a row data object
   */
  private buildValueGetter<T extends string | number>(getter: (d: StationStatisticsRow) => T) {
    return (params: NumberCellRendererParams): T => {
      const data = this.props.tableData.find(d => d.id === params.data.id);
      return data && getter(data);
    };
  }

  /**
   * Shows a contest menu used for acknowledging
   * @param event StationStatisticsCellClickedEvent
   */
  private readonly onCellContextMenu = (event: StationStatisticsCellClickedEvent): void => {
    const stationNames = this.getSelectedIdsOnRightClick(event);
    const offset: Offset = { left: event.event.x, top: event.event.y };
    ContextMenu.show(this.props.acknowledgeContextMenu(stationNames), offset);
    this.context.setSelectedStationIds(stationNames);
  }

  /**
   * Gets the station Ids based on the tables selection
   * @param event StationStatisticsCellClickedEvent
   * @returns selectedIds as string[]
   */
  private readonly getSelectedIdsOnRightClick = (
    event: StationStatisticsCellClickedEvent
  ): string[] => {
    let selectedIds: string[] = this.tableRef
      .getTableApi()
      .getSelectedRows()
      .map(row => row.id);
    const clickedRowId = event.node.id;
    if (!selectedIds.includes(clickedRowId)) {
      selectedIds = [event.node.id];
    }
    return selectedIds;
  }
  // ******************************************
  // END PRIVATE METHODS
  // ******************************************
}
