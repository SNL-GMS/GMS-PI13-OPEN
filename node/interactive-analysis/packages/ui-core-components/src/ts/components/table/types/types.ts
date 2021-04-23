import { CellRendererParams, ColumnDefinition, ValueGetterParams } from 'src/ts/ui-core-components';

/**
 * ! Here is where types are defined so that when using the table component have access to more specific types
 * ! that are generic
 */

/**
 * Column definition - value is of type string
 */
export type StringColumnDefinition = ColumnDefinition<{ id: string }, {}, string, {}, {}>;

/**
 * Column definition - value is of type number
 */
export type NumberColumnDefinition = ColumnDefinition<{ id: string }, {}, number, {}, {}>;

/**
 * Column value getter params - value is of type string
 */
export type StringValueGetterParams = ValueGetterParams<{ id: string }, {}, string, {}, {}>;

/**
 * Column value getter - value is of type string
 */
export type StringValueGetter = (params: StringValueGetterParams) => string;

/**
 * Column value getter params - value is of type number
 */
export type NumberValueGetterParams = ValueGetterParams<{ id: string }, {}, number, {}, {}>;
/**
 * Column value getter - value is of type number
 */
export type NumberValueGetter = (params: NumberValueGetterParams) => number;

/**
 * Cell renderer params - value is of type string
 */
export type StringCellRendererParams = CellRendererParams<{ id: string }, {}, string, {}, {}>;

/**
 * Cell renderer params - value is of type number
 */
export type NumberCellRendererParams = CellRendererParams<{ id: string }, {}, number, {}, {}>;
