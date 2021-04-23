import { Intent, Spinner } from '@blueprintjs/core';
import React from 'react';
import { LoadingSpinnerProps } from './types';
// A loading spinner widget to be used in toolbars the world over
export const LoadingSpinner: React.FunctionComponent<LoadingSpinnerProps> = props => (
  <div
    className="loading-spinner__container"
    style={{
      minWidth: `${props.widthPx}px`
    }}
  >
    {props.itemsToLoad > 0 ? (
      <span>
        <Spinner
          intent={Intent.PRIMARY}
          size={Spinner.SIZE_SMALL}
          value={props.itemsLoaded ? props.itemsLoaded / props.itemsToLoad : undefined}
        />
        {props.onlyShowSpinner ? null : (
          <span>
            {props.hideTheWordLoading ? '' : 'Loading'}
            {props.hideOutstandingCount ? props.itemsToLoad : ''}...
          </span>
        )}
      </span>
    ) : null}
  </div>
);
