import { IconNames } from '@blueprintjs/icons';
import { SystemMessageTypes } from '@gms/common-graphql/lib/graphql';
import { Toolbar, ToolbarTypes } from '@gms/ui-core-components';
import React, { useState } from 'react';
import { gmsLayout } from '~scss-config/layout-preferences';
import { useBaseDisplaySize } from '../../base-display/base-display-hooks';
import { SoundConfiguration } from '../sound-configuration';
import { SystemMessageToolbarProps } from '../types';
import { SystemMessageSummary } from './system-message-summary';

const marginForToolbarPx = 16;

/**
 * System message toolbar component
 */
export const SystemMessageToolbar: React.FunctionComponent<SystemMessageToolbarProps> = React.memo(
  props => {
    /*
     * Left toolbar items
     */

    const systemMessageSummary: ToolbarTypes.CustomItem = {
      rank: 1,
      type: ToolbarTypes.ToolbarItemType.CustomItem,
      label: 'Number of system messages by severity',
      tooltip: SystemMessageTypes.SystemMessageSeverity.CRITICAL,
      widthPx: 200,
      element: (
        <SystemMessageSummary
          systemMessages={props.systemMessagesState.systemMessages}
          severityFilterMap={props.severityFilterMap}
          setSeverityFilterMap={m => props.setSeverityFilterMap(m)}
        />
      )
    };

    const leftToolbarItemDefs: ToolbarTypes.ToolbarItem[] = [systemMessageSummary];

    /*
     * Right toolbar items
     */

    let rightItemCount = 1;

    const enableDisableAutoScrolling: ToolbarTypes.SwitchItem = {
      rank: rightItemCount++,
      type: ToolbarTypes.ToolbarItemType.Switch,
      label: 'Auto scroll',
      labelRight: 'Auto scroll',
      cyData: 'system-message-auto-scroll',
      icon: props.isAutoScrollingEnabled ? IconNames.PAUSE : IconNames.PLAY,
      tooltip: props.isAutoScrollingEnabled ? 'Disable auto scrolling' : 'Enable auto scrolling',
      widthPx: 100,
      value: props.isAutoScrollingEnabled,
      onChange: () => props.setIsAutoScrollingEnabled(!props.isAutoScrollingEnabled)
    };

    const clearSystemMessages: ToolbarTypes.ButtonItem = {
      rank: rightItemCount++,
      type: ToolbarTypes.ToolbarItemType.Button,
      label: 'Clear list',
      labelRight: 'Clear list',
      cyData: 'system-message-clear',
      onlyShowIcon: true,
      icon: IconNames.TRASH,
      tooltip: 'Clear all system messages from the list',
      widthPx: 100,
      onClick: () => props.clearAllSystemMessages()
    };

    const enableDisableSounds: ToolbarTypes.ButtonItem = {
      rank: rightItemCount++,
      type: ToolbarTypes.ToolbarItemType.Button,
      label: 'Sounds',
      onlyShowIcon: true,
      icon: props.isSoundEnabled ? IconNames.VOLUME_UP : IconNames.VOLUME_OFF,
      tooltip: props.isSoundEnabled ? 'Disable sound' : 'Enable sound',
      widthPx: 30,
      onClick: () => props.setIsSoundEnabled(!props.isSoundEnabled)
    };

    // ! Disabling sound toolbar options until they are functional - DO NOT DELETE
    const [configureSoundsVisible, setConfigureSoundsVisible] = useState(false);
    const configureSounds: ToolbarTypes.ButtonItem = {
      rank: rightItemCount++,
      type: ToolbarTypes.ToolbarItemType.Button,
      label: 'Configure sounds',
      tooltip: 'Configure the sounds',
      widthPx: 125,
      onClick: () => {
        setConfigureSoundsVisible(!configureSoundsVisible);
      }
    };

    const rightToolbarItemDefs: ToolbarTypes.ToolbarItem[] = [
      enableDisableSounds,
      enableDisableAutoScrolling,
      clearSystemMessages,
      configureSounds
    ];

    const [widthPx] = useBaseDisplaySize();

    return (
      <>
        <Toolbar
          toolbarWidthPx={
            widthPx - marginForToolbarPx > 0 ? widthPx - gmsLayout.displayPaddingPx * 2 : 0
          }
          items={rightToolbarItemDefs}
          minWhiteSpacePx={1}
          itemsLeft={leftToolbarItemDefs}
        />
        <SoundConfiguration
          systemMessageDefinitions={props.systemMessageDefinitions}
          onToggle={() => setConfigureSoundsVisible(!configureSoundsVisible)}
          isVisible={configureSoundsVisible}
        />
      </>
    );
  }
);
