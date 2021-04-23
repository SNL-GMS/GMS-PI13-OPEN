import { Icon, Intent, Position, Tooltip } from '@blueprintjs/core';
import { IconNames } from '@blueprintjs/icons';
import React from 'react';
import { systemConfig } from '~analyst-ui/config';
import { messageConfig } from '~analyst-ui/config/message-config';
import { canPerformAction } from '../utils/history-utils';
import { GenericHistoryEntryProps, HistoryEntryAction, HistoryEntryProps } from './types';

export const makeMouseHandler = (
  handler: (evt: React.MouseEvent<HTMLDivElement, MouseEvent>) => void,
  focus?: boolean
) => e => {
  focus && e.currentTarget.focus();
  handler && handler(e);
};

export const makeKeyHandler = (
  handler: (evt: React.KeyboardEvent<HTMLDivElement>) => void,
  focus?: boolean
) => e => {
  focus && e.currentTarget.focus();
  handler && handler(e);
};

/**
 * Private function to create the history entry jsx inside of HistoryEntry elements and
 * HiddenHistoryEntry elements.
 * @param props all props are optional
 */
export const GenericHistoryEntry: React.FunctionComponent<GenericHistoryEntryProps> = props => (
  <div
    className={`
      list__column history-entry
      history-entry--${props.entryType ? props.entryType.toLowerCase() : 'hidden'}
      ${props.isAssociated ? 'is-associated' : ''}
      ${props.isAffected ? 'is-affected' : ''}
      ${props.isCompleted ? 'is-completed' : ''}
      ${props.isOrphaned ? 'is-orphaned' : ''}
      ${props.isChild ? 'is-child' : ''}
      ${props.isEventReset ? 'is-event-reset' : ''}
    `}
    tabIndex={0}
    onClick={e => {
      props.handleAction && props.handleAction(e);
    }}
    // tslint:disable-next-line: no-unbound-method
    onMouseOver={makeMouseHandler(props.handleMouseEnter, true)}
    // tslint:disable-next-line: no-unbound-method
    onMouseOut={makeMouseHandler(props.handleMouseOut)}
    // tslint:disable-next-line: no-unbound-method
    onKeyDown={makeKeyHandler(props.handleKeyDown)}
    // tslint:disable-next-line: no-unbound-method
    onKeyUp={makeKeyHandler(props.handleKeyUp)}
  >
    <span className="history-entry__description">{props.message ? props.message : '\u00A0'}</span>
    {props.isInConflict && (// if we have an entryType, render the conflict icon
      <Tooltip
        content={'This action created a conflict'}
        hoverOpenDelay={systemConfig.interactionDelay.slow}
        position={Position.BOTTOM}
      >
        <Icon className={`history-entry__icon`} intent={Intent.DANGER} icon={IconNames.ISSUE} />
      </Tooltip>
    )}
    {props.entryType && (// if we have an entryType, render the appropriate icon
      <Tooltip
        content={
          props.entryType === HistoryEntryAction.undo
            ? props.isEventReset
              ? `${messageConfig.tooltipMessages.history.undoEventLevelActionMessage}`
              : `${messageConfig.tooltipMessages.history.undoActionMessage}`
            : props.isEventReset
            ? `${messageConfig.tooltipMessages.history.redoEventLevelActionMessage}`
            : `${messageConfig.tooltipMessages.history.redoActionMessage}`
        }
        hoverOpenDelay={systemConfig.interactionDelay.slow}
        position={Position.BOTTOM}
      >
        <Icon
          className={`history-entry__icon`}
          icon={
            props.isChild
              ? undefined
              : props.entryType === HistoryEntryAction.undo
              ? IconNames.UNDO
              : props.entryType === HistoryEntryAction.redo
              ? IconNames.REDO
              : undefined
          }
        />
      </Tooltip>
    )}
  </div>
);

/**
 * A HistoryEntry component - a clickable entry in the history stack list, which will
 * perform one or more undo or redo actions.
 * @param props message, status effects and handlers.
 */
export const HistoryEntry: React.FunctionComponent<HistoryEntryProps> = props => {
  const entryType = canPerformAction(props.changes, HistoryEntryAction.undo)
    ? HistoryEntryAction.undo
    : HistoryEntryAction.redo;
  return <GenericHistoryEntry {...props} entryType={entryType} />;
};

/**
 * Displays entry as an empty space
 * @param props any props the hidden history entry should have. All are optional
 */
export const HiddenHistoryEntry: React.FunctionComponent<GenericHistoryEntryProps> = props => (
  <GenericHistoryEntry {...props} />
);
