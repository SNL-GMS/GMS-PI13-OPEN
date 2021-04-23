import { CommonTypes } from '@gms/common-graphql';
import { AnalystWorkspaceTypes } from '@gms/ui-state';
import React from 'react';
import { ChildProps } from 'react-apollo';
import { EventsMutations } from '~analyst-ui/components/events/types';
import { HistoryMutations } from '~analyst-ui/components/history/types';

/**
 * The interaction provider redux props.
 */
export interface InteractionProviderReduxProps {
  analystActivity: AnalystWorkspaceTypes.AnalystActivity;
  openEventId: string;
  currentTimeInterval: CommonTypes.TimeRange;
  historyActionInProgress: number;
  incrementHistoryActionInProgress();
  decrementHistoryActionInProgress();
}

/**
 * The interaction provider props.
 */
export type InteractionProviderProps = InteractionProviderReduxProps &
  ChildProps<EventsMutations> &
  ChildProps<HistoryMutations> &
  CommonTypes.WorkspaceStateProps;

/**
 * The interaction provider callbacks.
 */
export interface InteractionCallbacks {
  saveAllEvents(): void;
  saveCurrentlyOpenEvent(): void;
  undo(count: number): void;
  redo(count: number): void;
}

/**
 * The interaction context.
 */
export const InteractionContext = React.createContext<InteractionCallbacks>(undefined);
