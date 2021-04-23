import { EventTypes } from '@gms/common-graphql';
import { UILogger } from '@gms/ui-apollo';
import React from 'react';
import { InteractionContext, InteractionProviderProps } from './types';

/**
 * Provides one implementation of graphQL and redux capabilities and provides them to child components via a context
 */
export const InteractionProvider: React.FunctionComponent<InteractionProviderProps> = props => {
  /**
   * Save current open event.
   */
  const saveCurrentlyOpenEvent = () => {
    if (props.openEventId === undefined || props.openEventId === null || props.openEventId === '') {
      return;
    }
    const variables: EventTypes.SaveEventMutationArgs = {
      eventId: props.openEventId
    };
    props
      .saveEvent({
        variables
      })
      .catch(e => UILogger.Instance().error(`Failed to save current open event: ${e.message}`));
  };

  /**
   * Save all events.
   */
  const saveAllEvents = () => {
    props
      .saveAllModifiedEvents({ variables: {} })
      .catch(e => UILogger.Instance().error(`Failed to save all events: ${e.message}`));
  };
  const isIntervalOpened =
    props.currentTimeInterval !== undefined && props.currentTimeInterval !== null;
  const undo = (count: number) => {
    if (isIntervalOpened) {
      props.incrementHistoryActionInProgress();
      props
        .undoHistory({ variables: { numberOfItems: 1 } })
        .then(() => {
          props.decrementHistoryActionInProgress();
        })
        .catch(() => {
          return;
        });
    }
  };

  /**
   * Redo a history change.
   * @param count the number of changes to redo
   */
  const redo = (count: number) => {
    if (isIntervalOpened) {
      props.incrementHistoryActionInProgress();
      props
        .redoHistory({ variables: { numberOfItems: 1 } })
        .then(() => {
          props.decrementHistoryActionInProgress();
        })
        .catch(() => {
          return;
        });
    }
  };

  return (
    <React.Fragment>
      <InteractionContext.Provider
        value={{
          saveCurrentlyOpenEvent,
          saveAllEvents,
          undo,
          redo
        }}
      >
        {props.children}
      </InteractionContext.Provider>
    </React.Fragment>
  );
};
