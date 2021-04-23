import { SohTypes } from '@gms/common-graphql';
import { ValueType } from '@gms/common-util/lib/types/value-type';
import React from 'react';
import { BaseDisplay } from '~components/common-ui/components/base-display';
import { MissingLagHistoryPanel } from '.';
import { MISSING_LAG, SohMissingLagHistoryComponentProps } from './types';
/**
 * State of health missing and lag history component display.
 * Composes together the various charts into the lag display.
 */
export const buildSohMissingLagHistoryComponent = (
  type: MISSING_LAG,
  valueType: ValueType
): React.FunctionComponent<SohMissingLagHistoryComponentProps> => props => {
  const componentBaseNameLowerCase: string = type.toString().toLowerCase();

  /**
   * Returns the selected station
   */
  const getStation = (): SohTypes.UiStationSoh =>
    props.sohStatus?.stationAndStationGroupSoh?.stationSoh?.find(
      s => s.stationName === props.selectedStationIds[0]
    );

  return (
    <BaseDisplay
      glContainer={props.glContainer}
      className={`${componentBaseNameLowerCase}-history-display top-level-container scroll-box scroll-box--y`}
    >
      <MissingLagHistoryPanel
        monitorType={SohTypes.SohMonitorType[type]}
        station={getStation()}
        sohStatus={props.sohStatus}
        sohHistoricalDurations={
          props.uiConfigurationQuery.uiAnalystConfiguration.sohHistoricalDurations
        }
        valueType={valueType}
      />
    </BaseDisplay>
  );
};
