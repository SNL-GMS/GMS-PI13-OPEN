import { ContextMenu, Menu, MenuItem } from '@blueprintjs/core';
import { isEnvironmentalIssue } from '@gms/common-graphql/lib/graphql/soh/types';
import { prettifyAllCapsEnumType } from '@gms/common-util';
import { millisToTimeRemaining } from '@gms/common-util/lib/util/time-util';
import { Form, FormTypes, WidgetTypes } from '@gms/ui-core-components';
import React from 'react';
import { QuietAction } from '~components/data-acquisition-ui/components/soh-environment/types';
// max number of charaters for the text area
const MAX_QUIET_COMMENT_CHAR = 1024;
/**
 * Creates the quite dialog form for quieting with a comment.
 * @param props the props
 */
const QuiteWithCommentDialog: React.FunctionComponent<QuietAction> = props => {
  const { stationName, channelMonitorPairs, quietingDurationSelections } = props;
  const channelNames = channelMonitorPairs.map(p => p.channelName);
  const monitors = channelMonitorPairs.map(p => p.monitorType);
  const maxStringLength = 30;
  const formItems: FormTypes.FormItem[] = [];
  formItems.push({
    itemKey: 'stationLabel',
    labelText: `Station`,
    tooltip: stationName,
    itemType: FormTypes.ItemType.Display,
    displayText: stationName
  });
  formItems.push({
    itemKey: 'channelLabel',
    labelText: `Channel`,
    tooltip: channelNames.join(', '),
    itemType: FormTypes.ItemType.Display,
    displayText: channelNames.join(', ')
  });
  formItems.push({
    itemKey: 'monitorLabel',
    labelText: `Monitor`,
    tooltip: monitors.length > 1 ? 'Multiple' : monitors[0],
    itemType: FormTypes.ItemType.Display,
    displayText:
      monitors.length > 1
        ? 'Multiple'
        : monitors[0].length > maxStringLength
        ? `${monitors[0].substring(0, maxStringLength)}...`
        : prettifyAllCapsEnumType(monitors[0], isEnvironmentalIssue(monitors[0]))
  });
  formItems.push({
    itemKey: 'duration',
    labelText: 'Quiet for',
    itemType: FormTypes.ItemType.Input,
    'data-cy': 'quiet-duration',
    value: {
      params: {
        tooltip: 'Select the duration to quiet',
        dropDownItems: quietingDurationSelections,
        // tslint:disable-next-line: no-unnecessary-callback-wrapper
        dropdownText: quietingDurationSelections.map(q => millisToTimeRemaining(q))
      },
      defaultValue: quietingDurationSelections[0],
      type: WidgetTypes.WidgetInputType.DropDown
    }
  });
  formItems.push({
    itemKey: 'comment',
    labelText: 'Comment',
    itemType: FormTypes.ItemType.Input,
    topAlign: true,
    'data-cy': 'quiet-comment',
    value: {
      params: {
        tooltip: 'Enter the comment',
        maxChar: MAX_QUIET_COMMENT_CHAR
      },
      defaultValue: ``,
      type: WidgetTypes.WidgetInputType.TextArea
    }
  });

  const quietPanel: FormTypes.FormPanel = {
    formItems,
    key: 'QuietWithComment'
  };

  return (
    <div>
      <Form
        header={'Quiet'}
        defaultPanel={quietPanel}
        onSubmit={(data: any) => {
          props.quietChannelMonitorStatuses(
            stationName,
            channelMonitorPairs,
            Number(data.duration),
            String(data.comment)
          );
          ContextMenu.hide();
        }}
        onCancel={ContextMenu.hide}
        submitButtonText={`Quiet`}
        requiresModificationForSubmit={true}
      />
    </div>
  );
};

/**
 * Creates menu item for quieting without a comment.
 */
const QuiteMenuItem: React.FunctionComponent<QuietAction> = props => {
  const { stationName, channelMonitorPairs, quietingDurationSelections, isStale } = props;
  const disabled = isStale;
  return (
    <MenuItem text="Quiet for..." disabled={disabled} data-cy="quiet-without-comment">
      {quietingDurationSelections.map(duration => (
        <MenuItem
          key={duration}
          text={millisToTimeRemaining(duration)}
          onClick={() =>
            props.quietChannelMonitorStatuses(stationName, channelMonitorPairs, duration)
          }
        />
      ))}
    </MenuItem>
  );
};

/**
 * Creates menu item for quieting with a comment.
 */
const QuiteWithCommentMenuItem: React.FunctionComponent<QuietAction> = props => {
  const { isStale } = props;
  const disabled = isStale;
  const clientOffset = 25;
  return (
    <MenuItem
      text="Quiet with comment..."
      data-cy="quiet-with-comment"
      disabled={disabled}
      onClick={(e: React.MouseEvent) => {
        e.preventDefault();
        ContextMenu.hide();
        ContextMenu.show(
          React.createElement(QuiteWithCommentDialog, {
            ...props
          }),
          { left: e.clientX - clientOffset, top: e.clientY - clientOffset },
          undefined,
          true
        );
      }}
    />
  );
};

/**
 * Creates menu item for canceling a quite period.
 */
const CancelMenuItem: React.FunctionComponent<QuietAction> = props => {
  const { stationName, channelMonitorPairs, quietUntilMs } = props;
  const disabled = !quietUntilMs || quietUntilMs === -1;
  return (
    <MenuItem
      text="Cancel quiet period"
      data-cy="quiet-cancel"
      disabled={disabled}
      onClick={() => props.quietChannelMonitorStatuses(stationName, channelMonitorPairs, 0)}
    />
  );
};

/**
 * Show the quieting context menu
 * @param q the quiet action parameters
 */
export const showQuietingContextMenu = (q: QuietAction): void => {
  const menuJSX: JSX.Element = (
    <Menu>
      <QuiteMenuItem {...q} />
      <QuiteWithCommentMenuItem {...q} />
      <CancelMenuItem {...q} />
    </Menu>
  );
  ContextMenu.show(menuJSX, q.position);
};
