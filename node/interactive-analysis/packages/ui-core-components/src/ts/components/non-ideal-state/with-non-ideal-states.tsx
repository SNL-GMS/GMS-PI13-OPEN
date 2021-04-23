import GoldenLayout from '@gms/golden-layout';
import { useForceGlUpdateOnResizeAndShow } from '@gms/ui-util';
import * as React from 'react';
import { NonIdealStateDefinition } from './types';

/**
 * Check the list of non ideal states against the props
 * @param props the component's props of type <T> that we should check for non-ideal state conditions
 * @param nonIdealStates the ordered list of non ideal states which could be applied
 */
function maybeGetNonIdealState<T>(props: T, nonIdealStates: NonIdealStateDefinition<T>[]) {
  let nonIdealState: NonIdealStateDefinition<T>;
  nonIdealStates.forEach(nis => {
    if (!nonIdealState && nis.condition(props)) {
      nonIdealState = nis;
    }
  });
  return nonIdealState;
}

/**
 * Either renders a non-ideal state (if its condition is true), or render the provided component.
 * @param nonIdealStates A list of NonIdealStateDefinitions.
 * In the order provided, their conditions are checked.
 * If the condition returns true, then return that non-ideal state.
 * If a golden layout container is part of the props, then this component
 * attaches listeners to force the component to update on show and resize.
 * @param WrappedComponent The component that should be rendered if no non-ideal state conditions are true.
 */
export function WithNonIdealStates<T extends { glContainer?: GoldenLayout.Container }>(
  nonIdealStates: NonIdealStateDefinition<T>[],
  WrappedComponent: React.ComponentClass<T> | React.FunctionComponent<T>
) {
  return (props: T) => {
    useForceGlUpdateOnResizeAndShow(props.glContainer);
    const nonIdealState = maybeGetNonIdealState(props, nonIdealStates);
    return nonIdealState?.element ?? <WrappedComponent {...props} />;
  };
}
