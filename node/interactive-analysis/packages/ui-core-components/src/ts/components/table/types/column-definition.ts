// tslint:disable: no-empty-interface
import {
  ColDef as AgColDef,
  ColGroupDef as AgColGroupDef,
  Column as AgColumn
} from 'ag-grid-community';
import { CellRendererComp, CellRendererFunc, CellRendererParams } from './cell-renderer';
import { ColumnApi } from './column-api';
import { CellClickedEvent, CellContextMenuEvent, CellDoubleClickedEvent } from './events';
import { RowNode } from './row-node';
import { TableApi } from './table-api';
import { TooltipParams } from './tooltip-renderer';

export interface BaseColDefParams<
  RowDataType,
  ContextDataType,
  CellValueType,
  CellRendererParamsType,
  HeaderRendererParamsType
> {
  node: RowNode;
  data: RowDataType;
  colDef: ColumnDefinition<
    RowDataType,
    ContextDataType,
    CellValueType,
    CellRendererParamsType,
    HeaderRendererParamsType
  >;
  column: AgColumn;
  api: TableApi | null | undefined;
  columnApi: ColumnApi | null | undefined;
  context: any;
}

export interface BaseWithValueColDefParams<
  RowDataType,
  ContextDataType,
  CellValueType,
  CellRendererParamsType,
  HeaderRendererParamsType
>
  extends BaseColDefParams<
    RowDataType,
    ContextDataType,
    CellValueType,
    CellRendererParamsType,
    HeaderRendererParamsType
  > {
  value: CellValueType;
}

export interface ValueGetterParams<
  RowDataType,
  ContextDataType,
  CellValueType,
  CellRendererParamsType,
  HeaderRendererParamsType
>
  extends BaseColDefParams<
    RowDataType,
    ContextDataType,
    CellValueType,
    CellRendererParamsType,
    HeaderRendererParamsType
  > {
  getValue(field: string): any;
}

export interface CellRendererFrameworkComponent<
  RowDataType,
  ContextDataType,
  CellValueType,
  CellRendererParamsType,
  HeaderRendererParamsType
>
  extends React.ComponentClass<
    CellRendererParams<
      RowDataType,
      ContextDataType,
      CellValueType,
      CellRendererParamsType,
      HeaderRendererParamsType
    >
  > {}

export interface CellRendererFrameworkFunctionComponent<
  RowDataType,
  ContextDataType,
  CellValueType,
  CellRendererParamsType,
  HeaderRendererParamsType
>
  extends React.FunctionComponent<
    CellRendererParams<
      RowDataType,
      ContextDataType,
      CellValueType,
      CellRendererParamsType,
      HeaderRendererParamsType
    >
  > {}

export interface ValueFormatterParams<
  RowDataType,
  ContextDataType,
  CellValueType,
  CellRendererParamsType,
  HeaderRendererParamsType
>
  extends BaseWithValueColDefParams<
    RowDataType,
    ContextDataType,
    CellValueType,
    CellRendererParamsType,
    HeaderRendererParamsType
  > {}

interface AdditionalColumnDefinitionsProperties {
  /**
   * NOTE: this is enabled by default to help limit the memory leak in
   * ag-grid due to the use of header frameworks; cell components
   * do not seem to have the same memory leak issue.
   *
   * if false or undefined and a header component framework is defined, then
   * the header component will be statically rendered; otherwise the
   * header component will be rendered as expected with the React lifecycle
   */
  disableStaticMarkupForHeaderComponentFramework?: boolean;

  /**
   * NOTE: this is enabled by default to help limit the memory leak in
   * ag-grid due to the use of cell renderer frameworks; cell renderers
   * do not seem to have the same memory leak issue.
   *
   * if false or undefined and a cell renderer framework is defined, then
   * the cell renderer will be statically rendered; otherwise the
   * cell renderer will be rendered as expected with the React lifecycle
   */
  disableStaticMarkupForCellRendererFramework?: boolean;
}

/** Wrapper interface class around ag-grid interface `ColDef` */
export interface ColumnDefinition<
  RowDataType,
  ContextDataType,
  CellValueType,
  CellRendererParamsType,
  HeaderRendererParamsType
> extends AgColDef, AdditionalColumnDefinitionsProperties {
  cellRendererParams?: CellRendererParamsType;

  /** The custom header component parameters */
  headerComponentParams?: HeaderRendererParamsType;

  /** Expression or function to get the cells value. */
  valueGetter?:
    | ((
        params: ValueGetterParams<
          RowDataType,
          ContextDataType,
          CellValueType,
          CellRendererParamsType,
          HeaderRendererParamsType
        >
      ) => CellValueType)
    | string;

  /** Expression or function to get the cells value for filtering. */
  filterValueGetter?:
    | ((
        params: ValueGetterParams<
          RowDataType,
          ContextDataType,
          CellValueType,
          CellRendererParamsType,
          HeaderRendererParamsType
        >
      ) => any)
    | string;

  /**
   * A function to format a value, should return a string.
   * Not used for CSV export or copy to clipboard, only for UI cell rendering.
   */
  valueFormatter?:
    | ((
        params: ValueFormatterParams<
          RowDataType,
          ContextDataType,
          CellValueType,
          CellRendererParamsType,
          HeaderRendererParamsType
        >
      ) => string)
    | string;

  // /** A function for rendering a cell. */
  cellRenderer?:
    | {
        new (): CellRendererComp<
          RowDataType,
          ContextDataType,
          CellValueType,
          CellRendererParamsType,
          HeaderRendererParamsType
        >;
      }
    | CellRendererFunc<
        RowDataType,
        ContextDataType,
        CellValueType,
        CellRendererParamsType,
        HeaderRendererParamsType
      >
    | string;

  cellRendererFramework?:
    | CellRendererFrameworkComponent<
        RowDataType,
        ContextDataType,
        CellValueType,
        CellRendererParamsType,
        HeaderRendererParamsType
      >
    | CellRendererFrameworkFunctionComponent<
        RowDataType,
        ContextDataType,
        CellValueType,
        CellRendererParamsType,
        HeaderRendererParamsType
      >;

  /** For native drag and drop, set to true to allow custom onRowDrag processing */
  dndSourceOnRowDrag?(params: { rowNode: RowNode; dragEvent: DragEvent }): void;

  /** The function used to calculate the tooltip of the object, tooltipField takes precedence */
  tooltipValueGetter?(params: TooltipParams): string;

  /**
   * Function to return the key for a value - use this if the value is an object (not a primitive type) and you
   * want to a) group by this field or b) use set filter on this field.
   */
  keyCreator?(value: RowDataType): string;

  /** Comparator function for custom sorting. */
  comparator?(
    valueA: CellValueType,
    valueB: CellValueType,
    nodeA: RowNode,
    nodeB: RowNode,
    isInverted: boolean
  ): number;

  /**
   * Comparator for values, used by renderer to know if values have
   * changed. Cells who's values have not changed don't get refreshed.
   */
  equals?(valueA: CellValueType, valueB: CellValueType): boolean;

  /** Function callback, gets called when a cell is clicked. */
  onCellClicked?(event: CellClickedEvent<RowDataType, ContextDataType, CellValueType>): void;

  /** Function callback, gets called when a cell is double clicked. */
  onCellDoubleClicked?(
    event: CellDoubleClickedEvent<RowDataType, ContextDataType, CellValueType>
  ): void;

  /** Function callback, gets called when a cell is right clicked. */
  onCellContextMenu?(
    event: CellContextMenuEvent<RowDataType, ContextDataType, CellValueType>
  ): void;
}

/** Wrapper interface class around ag-grid interface `ColGroupDef` */
export interface ColumnGroupDefinition
  extends AgColGroupDef,
    AdditionalColumnDefinitionsProperties {}
