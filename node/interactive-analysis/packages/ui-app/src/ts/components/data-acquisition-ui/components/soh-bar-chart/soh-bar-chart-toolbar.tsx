import { ConfigurationTypes } from '@gms/common-graphql';
import { SohMonitorType, UiStationSoh } from '@gms/common-graphql/lib/graphql/soh/types';
import { DropdownItem } from '@gms/ui-core-components/lib/components/ui-widgets/toolbar/types';
import * as React from 'react';
import { useBaseDisplaySize } from '~components/common-ui/components/base-display/base-display-hooks';
import { FilterableSOHTypes } from '~components/data-acquisition-ui/components/soh-overview/types';
import { messageConfig } from '~components/data-acquisition-ui/config/message-config';
import { SohToolbar } from '~components/data-acquisition-ui/shared/toolbars/soh-toolbar';

export interface ToolbarProps {
  statusesToDisplay: Map<FilterableSOHTypes, boolean>;
  sortDropdown: DropdownItem;
  forwardRef: React.MutableRefObject<HTMLElement>;
  isStale: boolean;
  station: UiStationSoh;
  monitorType: SohMonitorType;
  uiAnalystConfiguration: ConfigurationTypes.AnalystConfiguration;
  setStatusesToDisplay: React.Dispatch<React.SetStateAction<Map<FilterableSOHTypes, boolean>>>;
}

export const Toolbar: React.FunctionComponent<ToolbarProps> = props => {
  const [widthPx] = useBaseDisplaySize();

  return (
    <div ref={ref => (props.forwardRef.current = ref)} className="soh-drill-down__header">
      <SohToolbar
        setStatusesToDisplay={statuses => {
          props.setStatusesToDisplay(statuses);
        }}
        leftItems={[]}
        rightItems={[props.sortDropdown]}
        statusFilterText={messageConfig.labels.sohToolbar.filterStatuses}
        statusesToDisplay={props.statusesToDisplay}
        widthPx={widthPx}
        toggleHighlight={() => {
          return;
        }}
        updatedAt={props.station.time}
        updateIntervalSecs={props.uiAnalystConfiguration.reprocessingPeriod}
        sohStationStaleTimeMS={props.uiAnalystConfiguration.sohStationStaleTimeMS}
        displayTimeWarning={props.isStale}
      />
      <div className="soh-drill-down-station-label display-title">
        {props.station.stationName}
        <div className="display-title__subtitle">
          {props.monitorType === SohMonitorType.MISSING
            ? messageConfig.labels.missingSubtitle
            : props.monitorType === SohMonitorType.LAG
            ? messageConfig.labels.lagSubtitle
            : messageConfig.labels.timelinessSubtitle}
        </div>
      </div>
    </div>
  );
};
