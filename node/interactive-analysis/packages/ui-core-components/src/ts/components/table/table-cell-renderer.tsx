import { classList, getDataAttributesFromProps } from '@gms/ui-util';
import * as React from 'react';

export interface TableCellRendererProps {
  className?: string;
  heightCSS?: string;
  isNumeric?: boolean;
  shouldCenterText?: boolean;
  tooltipMsg?: string;
  value?: string;
  leftChild?: JSX.Element | React.FunctionComponent | React.ComponentClass;
}

/**
 * Cell renderer for table cells. Accepts classes,
 * data-attributes, and a flag to indicate if it is numeric
 */
export const TableCellRenderer: React.FunctionComponent<React.PropsWithChildren<
  TableCellRendererProps
>> = props => {
  const dataAttributes = getDataAttributesFromProps(props);
  return (
    <div
      style={{
        height: props.heightCSS ? props.heightCSS : '36px',
        overflow: 'hidden'
      }}
      data-cy="table-cell"
      {...dataAttributes}
      className={`table-cell ${props.className}`}
      title={props.tooltipMsg ? props.tooltipMsg : props.value}
    >
      {props.leftChild}
      {props.value ? (
        <div
          className={classList(
            {
              'table-cell__value--numeric': props.isNumeric,
              'table-cell__value--center': props.shouldCenterText
            },
            'table-cell__value'
          )}
        >
          <span>{props.value}</span>
        </div>
      ) : null}
      {props.children}
    </div>
  );
};
