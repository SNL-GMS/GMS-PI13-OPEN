import { Button, Dialog, Intent } from '@blueprintjs/core';
import React from 'react';
import { PromptProps } from './types';
/**
 * Prompts the user with a modal dialog. Accepts passed in children for the contents
 * of the dialog.
 */
export const ModalPrompt: React.FunctionComponent<PromptProps> = props => (
  <Dialog
    className="dialog_parent dialog_parent--wide"
    isOpen={props.isOpen}
    onClose={() => {
      props.onCloseCallback();
    }}
    title={props.title}
  >
    <div className="dialog dialog__container">
      {props.children}
      <div className="dialog__controls">
        <div className="dialog-actions">
          <Button
            text={props.actionText}
            title={props.actionTooltipText}
            intent={Intent.PRIMARY}
            onClick={() => props.actionCallback()}
          />
          {props.optionalButton ? (
            <Button
              text={props.optionalText ? props.optionalText : 'optional'}
              title={props.optionalTooltipText}
              onClick={() => (props.optionalCallback ? props.optionalCallback() : undefined)}
            />
          ) : (
            undefined
          )}
        </div>
        <Button
          text={props.cancelText ? props.cancelText : 'Cancel'}
          title={props.cancelTooltipText}
          onClick={() => props.cancelButtonCallback()}
        />
      </div>
    </div>
  </Dialog>
);
