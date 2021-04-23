import { ConfigurationTypes } from '@gms/common-graphql';
import { SohMonitorType, UiStationSoh } from '@gms/common-graphql/lib/graphql/soh/types';
import { DropdownItem } from '@gms/ui-core-components/lib/components/ui-widgets/toolbar/types';
import * as React from 'react';
import { useBaseDisplaySize } from '~components/common-ui/components/base-display/base-display-hooks';
import { messageConfig } from '~components/data-acquisition-ui/config/message-config';
import { SohToolbar } from '~components/data-acquisition-ui/shared/toolbars/soh-toolbar';
import { FilterableSOHTypes } from '../soh-overview/types';

export interface LagMissingChartHeader extends ConfigurationTypes.UIConfigurationQueryProps {
  statusesToDisplay: Map<FilterableSOHTypes, boolean>;
  sortDropdown: DropdownItem;
  forwardRef: React.MutableRefObject<HTMLElement>;
  isStale: boolean;
  station: UiStationSoh;
  monitorType: SohMonitorType;
  setStatusesToDisplay: React.Dispatch<React.SetStateAction<Map<FilterableSOHTypes, boolean>>>;
}

export const LagMissingChartHeader: React.FunctionComponent<LagMissingChartHeader> = props => {
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
        updateIntervalSecs={props.uiConfigurationQuery.uiAnalystConfiguration.reprocessingPeriod}
        sohStationStaleTimeMS={
          props.uiConfigurationQuery.uiAnalystConfiguration.sohStationStaleTimeMS
        }
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
