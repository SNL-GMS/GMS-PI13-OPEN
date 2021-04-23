import GoldenLayout from '@gms/golden-layout';
import {
  NonIdealStateDefinition,
  nonIdealStateWithNoSpinner,
  nonIdealStateWithSpinner
} from '@gms/ui-core-components';
import { SystemMessageProps } from '../system-message/types';

export const baseNonIdealStateDefinitions: NonIdealStateDefinition<{
  glContainer?: GoldenLayout.Container;
}>[] = [
  {
    condition: props => props.glContainer && props.glContainer.isHidden,
    element: nonIdealStateWithNoSpinner()
  }
];

/**
 * Non ideal state definitions for system message
 */
export const systemMessageNonIdealStateDefinitions: NonIdealStateDefinition<
  SystemMessageProps
>[] = [
  {
    condition: props => !props.systemMessageDefinitionsQuery,
    element: nonIdealStateWithNoSpinner('No System Message Definitions')
  },
  {
    condition: props =>
      props.systemMessageDefinitionsQuery && props.systemMessageDefinitionsQuery.loading,
    element: nonIdealStateWithSpinner('Loading:', 'System Message Definitions')
  }
];
