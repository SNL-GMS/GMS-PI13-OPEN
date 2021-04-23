import { AnalystWorkspaceTypes } from '@gms/ui-state';
import Immutable from 'immutable';

export interface InteractionConsumerReduxProps {
  keyPressActionQueue: Immutable.Map<AnalystWorkspaceTypes.KeyAction, number>;
  setKeyPressActionQueue(actions: Immutable.Map<AnalystWorkspaceTypes.KeyAction, number>): void;
}

export type InteractionConsumerProps = InteractionConsumerReduxProps;
