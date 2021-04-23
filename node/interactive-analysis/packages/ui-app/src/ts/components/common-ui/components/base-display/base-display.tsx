import { getDataAttributesFromProps, useElementSize, useForceUpdate } from '@gms/ui-util';
import defer from 'lodash/defer';
import * as React from 'react';
import { BaseDisplayContext } from './base-display-context';
import { BaseDisplayProps } from './types';

const useForceUpdateOnFirstRender = () => {
  const forceUpdate = useForceUpdate();
  React.useEffect(() => {
    defer(forceUpdate);
  }, []);
};

/**
 * A base display that should be at the base of all display components.
 * Adds consistent padding to each display, and exposes the width and height
 * of the display in the BaseDisplayContext.
 * @param props requires a reference to the glContainer.
 * Also accepts data attributes in the form 'data-cy': 'example-component'
 */
export const BaseDisplay: React.FunctionComponent<React.PropsWithChildren<
  BaseDisplayProps
>> = props => {
  /**
   * Base display size behavior
   */
  const [displayRef, heightPx, widthPx] = useElementSize();

  /**
   * On the very first mount, call forceUpdate so that height and width will
   * propagate to the consumers of the context.
   */
  useForceUpdateOnFirstRender();

  /**
   * the context menu handler, if provided
   */
  const { onContextMenu } = props;

  /**
   * Get any data attributes provided to this display (like data-cy attributes)
   */
  const dataAttributes = getDataAttributesFromProps(props);

  return (
    <div
      className={`base-display ${props.className ?? ''}`}
      ref={ref => (displayRef.current = ref)}
      onContextMenu={onContextMenu}
      {...dataAttributes}
    >
      <BaseDisplayContext.Provider
        value={{
          glContainer: props.glContainer,
          widthPx: widthPx || (props.glContainer?.width ?? 0),
          heightPx: heightPx || (props.glContainer?.height ?? 0)
        }}
      >
        {props.children}
      </BaseDisplayContext.Provider>
    </div>
  );
};
