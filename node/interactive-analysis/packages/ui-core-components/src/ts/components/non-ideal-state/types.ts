/**
 * A NonIdealStateDefinition for a given type T pairs the non-ideal state component
 * with a condition that determines whether to render it.
 */
export interface NonIdealStateDefinition<T> {
  // The JSX to render for the non ideal state
  element: JSX.Element;
  // take in the component's props and return true if the non ideal state should be rendered.
  condition(props: T): boolean;
}
