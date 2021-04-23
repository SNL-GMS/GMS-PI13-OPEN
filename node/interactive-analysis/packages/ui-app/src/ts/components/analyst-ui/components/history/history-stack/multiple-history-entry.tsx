import { Icon } from '@blueprintjs/core';
import { IconNames } from '@blueprintjs/icons';
import React from 'react';
import { GenericHistoryEntry, HistoryEntry } from './history-entry';
import {
  HistoryEntryAction,
  HistoryEntryDisplayFlags,
  HistoryEntryProps,
  MultipleHistoryEntryProps
} from './types';

/**
 * MultipleHistoryEntry creates a GenericHistoryEntry wrapping child changes.
 */
export const MultipleHistoryEntry: React.FunctionComponent<MultipleHistoryEntryProps> = props => {
  const [expanded, toggleExpanded] = React.useState(false);
  const parentFlags = props.historyEntryChildren.reduce(
    (accumFlags: HistoryEntryDisplayFlags, child: HistoryEntryProps) => ({
      isAffected: accumFlags.isAffected || child.isAffected,
      isAssociated: accumFlags.isAssociated || child.isAssociated,
      isInConflict: accumFlags.isInConflict || child.isInConflict,
      isOrphaned: accumFlags.isOrphaned || child.isOrphaned,
      isEventReset: accumFlags.isEventReset || child.isEventReset,
      entryType:
        accumFlags.entryType === HistoryEntryAction.undo ||
        child.entryType === HistoryEntryAction.undo
          ? HistoryEntryAction.undo
          : HistoryEntryAction.redo
      // tslint:disable-next-line: align
    }),
    {
      isAffected: false,
      isAssociated: false,
      isInConflict: false,
      isOrphaned: false,
      isEventReset: false,
      entryType: HistoryEntryAction.redo
    }
  );
  return (
    <div className={`history-row--multi`}>
      <HistoryEntry
        message={props.description}
        key={`Multi:${props.id}`}
        {...parentFlags}
        {...props}
      />
      <div className={`history-row__child-container ${expanded ? 'is-expanded' : ''}`}>
        {expanded
          ? props.historyEntryChildren.map((child, index) => (
              <div key={index}>
                <div />
                <GenericHistoryEntry
                  message={child.message}
                  key={`Child={true}:  ${props.id}-${index}`}
                  isChild={true}
                  {...child}
                />
              </div>
            ))
          : ''}
      </div>
      <div
        className={`toggle-button
          toggle-button--${parentFlags.entryType ? parentFlags.entryType.toLowerCase() : 'hidden'}`}
        onClick={clickEvent => {
          toggleExpanded(!expanded);
        }}
      >
        <Icon className={`${expanded ? 'is-inverted' : ''}`} icon={IconNames.CHEVRON_DOWN} />
      </div>
    </div>
  );
};
