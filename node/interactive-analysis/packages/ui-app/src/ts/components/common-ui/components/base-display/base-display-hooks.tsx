import * as React from 'react';
import { BaseDisplayContext } from '.';

/**
 * Gets the width and height of the base display that is an ancestor of the current display.
 * uses the BaseDisplayContext to get those values.
 * @returns the width and the height in pixels of the BaseDisplay that is an ancestor of the current
 * component. Uses the form [widthPx, heightPx];
 */
export const useBaseDisplaySize = (): [number, number] => {
  const baseDisplayContext = React.useContext(BaseDisplayContext);
  const widthPx = baseDisplayContext.widthPx;
  const heightPx = baseDisplayContext.heightPx;
  return [widthPx, heightPx];
};
