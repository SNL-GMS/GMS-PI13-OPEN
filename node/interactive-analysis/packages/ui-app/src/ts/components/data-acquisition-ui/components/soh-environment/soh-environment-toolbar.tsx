import { ToolbarTypes } from '@gms/ui-core-components';
import * as React from 'react';
import { useBaseDisplaySize } from '~components/common-ui/components/base-display/base-display-hooks';
import { messageConfig } from '~components/data-acquisition-ui/config/message-config';
import { isSohStationStaleTimeMS } from '~components/data-acquisition-ui/shared/table/utils';
import { SohToolbar } from '~components/data-acquisition-ui/shared/toolbars/soh-toolbar';
import { gmsLayout } from '~scss-config/layout-preferences';

const PADDING_PX = Number(gmsLayout.displayPaddingPx) * 2;

export interface UpdateInfo {
  reprocessingPeriod: number;
  updateTime: number;
  sohStationStaleTimeMS: number;
}

export interface EnvironmentToolbarProps {
  filterDropdown: ToolbarTypes.CheckboxDropdownItem[];
  monitorStatusesToDisplay: Map<any, boolean>;
  updateInfo: UpdateInfo;
  setMonitorStatusesToDisplay(statuses: any): void;
}

export const EnvironmentToolbar: React.FunctionComponent<EnvironmentToolbarProps> = props => {
  const [widthPx] = useBaseDisplaySize();
  return (
    <React.Fragment>
      <SohToolbar
        setStatusesToDisplay={statuses => props.setMonitorStatusesToDisplay(statuses)}
        leftItems={props.filterDropdown}
        rightItems={[]}
        statusFilterText={messageConfig.labels.sohToolbar.filterMonitorsByStatus}
        statusesToDisplay={props.monitorStatusesToDisplay}
        widthPx={widthPx - PADDING_PX * 2}
        toggleHighlight={() => {
          return;
        }}
        updatedAt={props.updateInfo.updateTime}
        updateIntervalSecs={props.updateInfo.reprocessingPeriod}
        sohStationStaleTimeMS={props.updateInfo.sohStationStaleTimeMS}
        displayTimeWarning={isSohStationStaleTimeMS(
          props.updateInfo.updateTime,
          props.updateInfo.sohStationStaleTimeMS
        )}
      />
      {props.children}
    </React.Fragment>
  );
};
