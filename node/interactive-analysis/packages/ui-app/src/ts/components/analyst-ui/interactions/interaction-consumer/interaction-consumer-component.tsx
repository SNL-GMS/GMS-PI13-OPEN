import { AnalystWorkspaceTypes } from '@gms/ui-state';
import React from 'react';
import { InteractionContext } from '../interaction-provider/types';
import { InteractionConsumerProps } from './types';

/**
 * Consumes keypress from the redux store and calls the Interaction Provider context to perform the appropriate action
 */
export const InteractionConsumer: React.FunctionComponent<InteractionConsumerProps> = props => {
  /** the callbacks */
  const callbacks = React.useContext(InteractionContext);

  /**
   * Checks to see if an action should be performed, and if so consumes the keypress and performs it
   * @param keyAction the key action
   * @param callback the callback
   * @param shouldConsumeAllKeypress true if should consume all key presses
   */
  const consumeKeypress = (
    keyAction: AnalystWorkspaceTypes.KeyAction,
    callback: () => void,
    shouldConsumeAllKeypress: boolean = false
  ) => {
    const maybeKeyCount = props.keyPressActionQueue.get(keyAction);
    if (!isNaN(maybeKeyCount) && maybeKeyCount > 0) {
      props.setKeyPressActionQueue(
        props.keyPressActionQueue.set(keyAction, shouldConsumeAllKeypress ? 0 : maybeKeyCount - 1)
      );
      callback();
    }
  };

  consumeKeypress(
    AnalystWorkspaceTypes.KeyAction.SAVE_OPEN_EVENT,
    () => {
      callbacks.saveCurrentlyOpenEvent();
    },
    true
  );

  consumeKeypress(
    AnalystWorkspaceTypes.KeyAction.SAVE_ALL_EVENTS,
    () => {
      callbacks.saveAllEvents();
    },
    true
  );

  consumeKeypress(AnalystWorkspaceTypes.KeyAction.UNDO_GLOBAL, () => {
    callbacks.undo(1);
  });

  consumeKeypress(AnalystWorkspaceTypes.KeyAction.REDO_GLOBAL, () => {
    callbacks.redo(1);
  });

  return <React.Fragment>{props.children}</React.Fragment>;
};
