import debounce from 'lodash/debounce';
import * as React from 'react';
import { isDomElement } from './dom-util';

/**
 * React hook to call an action on a repeating schedule.
 * Cleans up after itself in useEffect's cleanup callback.
 * @param action the function to call
 * @param periodMs the period of time to run.
 */
export const useActionEveryInterval = (action: () => void, periodMs) => {
  const [lastTimeRun, setLastTimeRun] = React.useState(Date.now());
  const runAction = () => {
    const debouncedAction = debounce(() => {
      setLastTimeRun(Date.now());
      action();
    });
    const timeoutHandle = setTimeout(debouncedAction, periodMs);
    return () => {
      debouncedAction.cancel();
      clearTimeout(timeoutHandle);
    };
  };
  React.useEffect(runAction, [lastTimeRun]);
};

/**
 * @returns the ref which should target the element of interest,
 * and the height and width of that element:
 * [ref, height, width]
 */
export const useElementSize = (): [React.MutableRefObject<HTMLElement>, number, number] => {
  const ref = React.useRef<HTMLDivElement>(null);
  const height = ref.current?.clientHeight;
  const width = ref.current?.clientWidth;
  return [ref, height, width];
};

/**
 * A forceUpdate function for use within a function component.
 * Tracks an ever changing value to ensure that comparisons
 * of the component's state are always going to be unique.
 * Usage:
 * const demo: React.FunctionComponent<{}> = () => {
 *   const forceUpdate = useForceUpdate();
 *   return <button onClick={forceUpdate()} />;
 * }
 */
export const useForceUpdate = () => {
  const [, setValue] = React.useState({});
  return () => {
    setValue({}); // new empty object is different every time
  };
};

/**
 * The visual states a highlighted element can be in
 */
export enum HighlightVisualState {
  HIDDEN = 'HIDDEN',
  REVEALED = 'REVEALED',
  HIGHLIGHTED = 'HIGHLIGHTED'
}

export interface HighlightManager {
  getVisualState(): HighlightVisualState;
  onMouseOver(): void;
  onMouseUp(): void;
  onMouseOut(): void;
  onMouseDown(): void;
}

export const useHighlightManager = (): HighlightManager => {
  const [visualState, setVisualState] = React.useState(HighlightVisualState.HIDDEN);

  const onMouseOver = () =>
    visualState !== HighlightVisualState.HIGHLIGHTED &&
    setVisualState(HighlightVisualState.REVEALED);

  const onMouseUp = () => setVisualState(HighlightVisualState.HIDDEN);

  const onMouseOut = () =>
    visualState !== HighlightVisualState.HIGHLIGHTED && setVisualState(HighlightVisualState.HIDDEN);

  const onMouseDown = () => {
    setVisualState(HighlightVisualState.HIGHLIGHTED);
  };

  return {
    getVisualState: () => visualState,
    onMouseOver,
    onMouseUp,
    onMouseOut,
    onMouseDown
  };
};

/**
 * Creates, manages and exposes an interval, consisting of a start and end time,
 * and a setter function.
 * @param initialStartTime The starting time (farther in the past)
 * @param initialEndTime The ending time (farther in the future)
 * @returns an array of three objects:
 * * startTime (earlier)
 * * endTime (later)
 * * setInterval
 * Example of use:
 * const [start, end, setInterval] = useInterval(Date.now(), new Date(<some timestamp in the future>))
 */
export const useInterval = (
  initialStartTime: Date,
  initialEndTime: Date
): [Date, Date, (start: Date, end: Date) => void] => {
  const [interval, setInternalInterval] = React.useState({
    startTime: initialStartTime,
    endTime: initialEndTime
  });

  const setInterval = (s: Date, e: Date) => {
    setInternalInterval({ startTime: s, endTime: e });
  };
  return [interval.startTime, interval.endTime, setInterval];
};

/**
 * Adds mouseup listeners to any and all elements that match the css query selector string passed in.
 * Removes the events on removal of the component.
 * @param querySelector the CSS query string to select
 * @param callback the callback function to call for mouseup events on any and all matching elements
 */
export const useMouseUpListenerBySelector = (querySelector: string, callback: EventListener) => {
  let elements: NodeListOf<Element>;
  React.useEffect(() => {
    elements = document.querySelectorAll(querySelector);
    elements.forEach(element => {
      if (isDomElement(element)) {
        element.addEventListener('mouseup', callback);
      }
    });
  }, []);
  return () => {
    elements.forEach(element => element.removeEventListener('mouseup', callback));
  };
};
