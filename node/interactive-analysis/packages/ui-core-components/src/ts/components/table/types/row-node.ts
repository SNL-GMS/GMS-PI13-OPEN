// tslint:disable: no-empty-interface
import { RowNode as AgRowNode } from 'ag-grid-community';

/** Row Interface */
export interface Row {
  id: string;
}

/** Wrapper interface class around ag-grid interface `RowNode` */
export interface RowNode extends AgRowNode {}
