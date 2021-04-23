import { IconNames } from '@blueprintjs/icons';
import { CacheTypes } from '@gms/common-graphql';
import { Toolbar, ToolbarTypes } from '@gms/ui-core-components';
import React from 'react';
import { messageConfig } from '~analyst-ui/config/message-config';
import { HistoryStack } from './history-stack/history-stack';
import { HistoryEntryAction } from './history-stack/types';
import { HistoryContext, HistoryContextData, HistoryPanelProps } from './types';
import {
  getLastIncludedOfType,
  getNextOrderedRedo,
  getNumberOfRedos,
  getNumberOfUndos
} from './utils/history-utils';

const MARGIN_FOR_TOOLBAR_PX = 14;

/**
 * Higher order function that generates a handler for mouse enter over the provided history entry
 * @param context the history context which exposes the setHistoryActionIntent function and the history list
 * @param isIncluded a function to determine if the events/changes are included in the intended action
 * @returns a function which sets the target and conditions for the historyActionIntent (what should happen on
 * hover)
 */
export function getHandleUndoMouseEnter(
  context: HistoryContextData,
  isIncluded: (entry: CacheTypes.History | CacheTypes.HistoryChange) => boolean
) {
  return () => {
    const actionIntentTarget = getLastIncludedOfType(
      context.historyList,
      isIncluded,
      HistoryEntryAction.undo
    );
    if (actionIntentTarget) {
      context.setHistoryActionIntent({
        entryId: actionIntentTarget.id,
        entryType: HistoryEntryAction.undo,
        isChangeIncluded: isIncluded,
        isEventMode: false
      });
    }
  };
}

/**
 * handleRedoMouseEnter is a higher order function which generates the function that
 * sets the action intent for hovering over the redo button
 * @param context the context which exposes the settHistoryActionIntent function and the
 * history list
 * @returns a function which sets the action intent to target the next redoable action
 */
export function getHandleRedoMouseEnter(context: HistoryContextData) {
  return () => {
    const actionIntentTarget = getNextOrderedRedo(context.historyList);
    if (actionIntentTarget) {
      context.setHistoryActionIntent({
        entryId: actionIntentTarget.id,
        entryType: HistoryEntryAction.redo,
        isEventMode: false,
        isChangeIncluded: change =>
          actionIntentTarget.changes.reduce(
            (accum, parentChange) => accum || parentChange.id === change.id,
            false
          )
      });
    }
  };
}

/**
 * History Panel holds the Event history stack and the global history stack side-by-side
 * @param props the history panel props including a non-ideal state which is passed
 * to the panel's children
 */
export const HistoryPanel: React.FunctionComponent<HistoryPanelProps> = props => {
  const context = React.useContext(HistoryContext);
  const { historyList } = context;
  const isIncluded = (summary: CacheTypes.History | CacheTypes.HistoryChange): boolean => true;

  // When historyList changes, this will run before re-render
  React.useEffect(() => {
    // scroll to the bottom
    const containerHeight =
      context && context.glContainer ? context.glContainer.height : window.innerHeight;
    document.querySelectorAll('.history-panel').forEach((panel: HTMLElement) => {
      panel.scroll({ top: panel.scrollHeight - containerHeight, left: 0, behavior: 'smooth' });
    });
    // tslint:disable-next-line: align
  }, [historyList]);

  const undo = (quantity: number = 1) => {
    context.undoHistory(quantity);
  };
  const redo = (quantity: number = 1) => {
    context.redoHistory(quantity);
  };

  // A mapping of history actions types (undo/redo) to functions that call mutations
  const globalHistoryActions: Map<string, (id: string) => void> = new Map([
    [
      HistoryEntryAction.undo,
      (id: string) => {
        context.undoHistoryById(id);
      }
    ],
    [
      HistoryEntryAction.redo,
      (id: string) => {
        context.redoHistoryById(id);
      }
    ]
  ]);

  const undoButton: ToolbarTypes.ButtonItem = {
    rank: 1,
    type: ToolbarTypes.ToolbarItemType.Button,
    tooltip: messageConfig.tooltipMessages.history.undoButtonAction,
    label: 'Undo',
    icon: IconNames.UNDO,
    widthPx: 80,
    onClick: () => {
      undo();
    },
    onMouseEnter: getHandleUndoMouseEnter(context, isIncluded),
    onMouseOut: () => context.setHistoryActionIntent(undefined),
    disabled:
      context.historyList === undefined ||
      getNumberOfUndos(context.historyList.filter(isIncluded)) === 0
  };

  const redoButton: ToolbarTypes.ButtonItem = {
    rank: 2,
    type: ToolbarTypes.ToolbarItemType.Button,
    tooltip: messageConfig.tooltipMessages.history.redoButtonAction,
    label: 'Redo',
    icon: IconNames.REDO,
    widthPx: 80,
    onClick: () => {
      redo();
    },
    onMouseEnter: getHandleRedoMouseEnter(context),
    onMouseOut: () => context.setHistoryActionIntent(undefined),
    disabled:
      context.historyList === undefined ||
      getNumberOfRedos(context.historyList.filter(isIncluded)) === 0
  };

  const buttonGroup: ToolbarTypes.ButtonGroupItem = {
    rank: 1,
    type: ToolbarTypes.ToolbarItemType.ButtonGroup,
    tooltip: 'Undo or Redo',
    label: 'Undo or Redo',
    buttons: [undoButton, redoButton]
  };

  const maybeSpinner: ToolbarTypes.LoadingSpinnerItem = {
    rank: 1,
    type: ToolbarTypes.ToolbarItemType.LoadingSpinner,
    tooltip: 'undo or redo in progress',
    label: '',
    itemsToLoad: context.historyActionInProgress,
    hideTheWordLoading: true,
    hideOutstandingCount: true,
    onlyShowIcon: true
  };

  return (
    <div className="history-panel" data-cy="history">
      <div className="list-toolbar-wrapper">
        <Toolbar
          toolbarWidthPx={
            context && context.glContainer
              ? context.glContainer.width - MARGIN_FOR_TOOLBAR_PX
              : window.innerWidth - MARGIN_FOR_TOOLBAR_PX
          }
          items={[buttonGroup]}
          itemsLeft={[maybeSpinner]}
        />
      </div>
      <HistoryStack historyActions={globalHistoryActions} isIncluded={isIncluded} />
    </div>
  );
};
