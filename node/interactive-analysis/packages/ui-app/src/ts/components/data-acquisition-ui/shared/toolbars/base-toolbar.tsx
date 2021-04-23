import { Toolbar, ToolbarTypes } from '@gms/ui-core-components';
import { ToolbarProps } from '@gms/ui-core-components/lib/components/ui-widgets/toolbar/types';
import * as React from 'react';
import { gmsLayout } from '~scss-config/layout-preferences';

/** the toolbar margin in pixels */
export const TOOLBAR_MARGIN_PIXELS = 16;

/** The base toolbar props */
export interface BaseToolbarProps extends Partial<ToolbarProps> {
  widthPx: number;
}

/**
 * Renders the base toolbar.
 * @param props the props
 */
export const BaseToolbar: React.FunctionComponent<BaseToolbarProps> = props => {
  // adjust the rank of the left items - ensure uniqueness
  let leftItemCount = 1;
  props.items?.forEach(item => {
    item.rank = leftItemCount++;
  });

  // adjust the rank of the right items - ensure uniqueness
  let rightItemCount = 1;
  props.itemsLeft?.forEach(item => {
    item.rank = rightItemCount++;
  });

  const leftToolbarItemDefs: ToolbarTypes.ToolbarItem[] = props.itemsLeft
    ? [...props.itemsLeft]
    : [];

  const rightToolbarItemDefs: ToolbarTypes.ToolbarItem[] = props.items ? [...props.items] : [];

  return (
    <Toolbar
      toolbarWidthPx={
        props.widthPx - TOOLBAR_MARGIN_PIXELS > 0
          ? props.widthPx -
            parseInt(
              gmsLayout.displayPadding.substring(0, gmsLayout.displayPadding.length - 2),
              10
            ) *
              2
          : 0
      }
      minWhiteSpacePx={1}
      {...props}
      items={rightToolbarItemDefs}
      itemsLeft={leftToolbarItemDefs}
    />
  );
};
