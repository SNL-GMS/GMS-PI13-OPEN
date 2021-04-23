import React from 'react';
import { KeyActions } from './analyst-workspace/types';

export function getKeyPressAction(e: React.KeyboardEvent<HTMLDivElement>) {
  const keyStr =
    // tslint:disable-next-line: max-line-length
    `${e.ctrlKey || e.metaKey ? 'Control+' : ''}${e.altKey ? 'Alt+' : ''}${
      e.shiftKey ? 'Shift+' : ''
    }${e.nativeEvent.code}`;
  const keyAction = KeyActions.get(keyStr);
  return keyAction;
}
