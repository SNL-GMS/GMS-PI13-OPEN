import { ContextMenu, Menu, MenuItem } from '@blueprintjs/core';
import { Form, FormTypes, WidgetTypes } from '@gms/ui-core-components';
import { uniq } from 'lodash';
import React from 'react';

/**
 * Interfaces defines the props for the acknowledge dialog form.
 */
interface AcknowledgeWithCommentDialogProps {
  stationNames: string[];
  onAcknowledge(stationNames: string[], comment: string): void;
  onCancel(): void;
}

// max number of charaters for the text area
const MAX_ACK_COMMENT_CHAR = 1024;

/**
 * Creates the acknowledge dialog form for acknowledging with a comment.
 * @param props the props
 */
const AcknowledgeWithCommentDialog: React.FunctionComponent<AcknowledgeWithCommentDialogProps> = props => {
  const { stationNames, onAcknowledge, onCancel } = props;

  const formItems: FormTypes.FormItem[] = [];
  formItems.push({
    itemKey: 'stationLabel',
    labelText: stationNames.length === 0 ? `Station` : `Stations`,
    itemType: FormTypes.ItemType.Display,
    displayText:
      stationNames.length > 4
        ? `${stationNames.slice(0, 4).join(', ')}...`
        : stationNames.join(', '),
    tooltip: stationNames.join(', ')
  });

  formItems.push({
    itemKey: 'comment',
    labelText: 'Comment',
    itemType: FormTypes.ItemType.Input,
    topAlign: true,
    'data-cy': 'acknowledge-comment',
    value: {
      params: {
        tooltip: 'Enter the comment',
        maxChar: MAX_ACK_COMMENT_CHAR
      },
      defaultValue: ``,
      type: WidgetTypes.WidgetInputType.TextArea
    }
  });

  const acknowledgePanel: FormTypes.FormPanel = {
    formItems,
    key: 'AcknowledgeWithComment'
  };

  return (
    <div>
      <Form
        header={'Acknowledge'}
        defaultPanel={acknowledgePanel}
        onSubmit={(data: any) => {
          onAcknowledge(stationNames, data.comment);
          ContextMenu.hide();
        }}
        onCancel={onCancel}
        submitButtonText={`Acknowledge`}
        requiresModificationForSubmit={true}
      />
    </div>
  );
};

/**
 * Station SOH context menu props
 */
export interface StationSohContextMenuProps {
  stationNames: string[];
  acknowledgeCallback(stationNames: string[], comment?: string);
}

/**
 * Station SOH context item menu props
 */
export interface StationSohContextMenuItemProps extends StationSohContextMenuProps {
  disabled: boolean;
}

/**
 * Creates menu item text for station acknowledgement
 */
export const getStationAcknowledgementMenuText = (
  stationNames: string[],
  withComment: boolean = false
): string => {
  const text =
    stationNames.length > 1 ? `Acknowledge ${stationNames.length} stations` : 'Acknowledge station';
  return withComment ? `${text} with comment...` : text;
};

/**
 * Creates menu item for acknowledging stations without a comment
 */
const AcknowledgeMenuItem: React.FunctionComponent<StationSohContextMenuItemProps> = props => {
  const stationList = uniq(props.stationNames);
  return React.createElement(MenuItem, {
    key: `acknowledge`,
    disabled: props.disabled,
    'data-cy': 'acknowledge-without-comment',
    onClick: () => {
      props.acknowledgeCallback(stationList);
    },
    text: getStationAcknowledgementMenuText(stationList),
    className: 'acknowledge-soh-status'
  } as any);
};

/**
 * Creates menu item for acknowledging stations with a comment
 */
const AcknowledgeWithCommentMenuItem: React.FunctionComponent<StationSohContextMenuItemProps> = props => {
  const stationList = uniq(props.stationNames);
  const clientOffset = 25;

  return React.createElement(MenuItem, {
    key: `acknowledge-with-comment`,
    disabled: props.disabled,
    'data-cy': 'acknowledge-with-comment',
    onClick: (e: React.MouseEvent) => {
      e.preventDefault();
      ContextMenu.hide();
      ContextMenu.show(
        React.createElement(AcknowledgeWithCommentDialog, {
          stationNames: stationList,
          onAcknowledge: (stationNames: string[], comment?: string) => {
            props.acknowledgeCallback(stationNames, comment);
          },
          onCancel: ContextMenu.hide
        }),
        { left: e.clientX - clientOffset, top: e.clientY - clientOffset },
        undefined,
        true
      );
    },
    text: getStationAcknowledgementMenuText(stationList, true),
    className: 'acknowledge-soh-status'
  } as any);
};

/**
 * Context menu for acknowledging station SOH
 */
export const StationSohContextMenu: React.FunctionComponent<StationSohContextMenuProps> = props =>
  React.createElement(
    Menu,
    {}, // empty props
    React.createElement(AcknowledgeMenuItem, {
      ...props,
      disabled: false
    }),
    React.createElement(AcknowledgeWithCommentMenuItem, {
      ...props,
      disabled: false
    })
  );

/**
 * Context menu for acknowledging station SOH
 */
export const DisabledStationSohContextMenu: React.FunctionComponent<StationSohContextMenuProps> = props =>
  React.createElement(
    Menu,
    {}, // empty props
    React.createElement(AcknowledgeMenuItem, {
      ...props,
      disabled: true
    }),
    React.createElement(AcknowledgeWithCommentMenuItem, {
      ...props,
      disabled: true
    })
  );
