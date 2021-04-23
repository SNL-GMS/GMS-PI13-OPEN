import { SohTypes } from '@gms/common-graphql';
import { isEnvironmentalIssue } from '@gms/common-graphql/lib/graphql/soh/types';
import { prettifyAllCapsEnumType } from '@gms/common-util';
import { ToolbarTypes } from '@gms/ui-core-components';
import * as React from 'react';
import { useBaseDisplaySize } from '~components/common-ui/components/base-display/base-display-hooks';
import {
  HistoricalAceiQuery,
  HistoricalAceiQueryContext
} from '~components/data-acquisition-ui/react-apollo-components/historical-acei-query';
import { DrillDownTitle } from '~components/data-acquisition-ui/shared/drill-down-components';
import { BaseToolbar } from '~components/data-acquisition-ui/shared/toolbars/base-toolbar';
import { useTrendTimeIntervalSelector } from '~components/data-acquisition-ui/shared/toolbars/trend-time-interval-hook';
import {
  convertSohMonitorTypeToAceiMonitorType,
  isAnalogAceiMonitorType
} from '~components/data-acquisition-ui/shared/utils';
import { validateNonIdealState } from './non-ideal-states';
import { AceiContext, AceiMonitorTypeOption, EnvironmentHistoryPanelProps } from './types';
import { VictoryDisplay } from './victory-display';
import { WeavessDisplay } from './weavess-display';

/**
 * True: use Weavess.
 * False: use Victory.
 */
const useWeavessDisplay = true;

/**
 * @returns a list of all environmental issues from the SohMonitorType enum
 */
const getEnvSohMonitorTypeNames = () =>
  Object.keys(SohTypes.SohMonitorType).filter(isEnvironmentalIssue);

/**
 * TODO: add support for a  default in the toolbar dropdown
 * @returns undefined if the option matches the default. Otherwise, return the enum value.
 */
const convertAceiMonitorTypeOptionToEnum = (option: AceiMonitorTypeOption): SohTypes.AceiType =>
  option && option !== 'CHOOSE_A_MONITOR_TYPE' ? SohTypes.AceiType[option] : undefined;

/**
 * Creates and manages state for the selector dropdown list for Env Monitor Types.
 * The dropdown  will contain all monitor types from the SohMonitorType enum that are ACEI monitors.
 * Modifies the AceiContext to update which monitor type has been selected.
 *
 * @returns an array containing exactly two items. The first is a dropdown,
 * the second is the currently selected option from the dropdown, or undefined
 * if no ACEI monitor type is selected.
 */
const useMonitorTypeDropdown = (): [ToolbarTypes.DropdownItem, SohTypes.AceiType] => {
  const envMonitorTypes: AceiMonitorTypeOption[] = [
    'CHOOSE_A_MONITOR_TYPE', // default
    ...getEnvSohMonitorTypeNames()
      .map(convertSohMonitorTypeToAceiMonitorType)
      // TODO: remove filtering once analog types are supported
      .filter(monitorType => !isAnalogAceiMonitorType(monitorType))
  ];

  const context = React.useContext(AceiContext);

  const dropdown: ToolbarTypes.DropdownItem = {
    label: 'Select Monitor Type',
    rank: undefined,
    tooltip: 'Select Monitor Type',
    type: ToolbarTypes.ToolbarItemType.Dropdown,
    dropdownOptions: envMonitorTypes,
    dropdownText: envMonitorTypes.map(monitorType => prettifyAllCapsEnumType(monitorType, false)),
    value: context.selectedAceiType ? context.selectedAceiType : undefined,
    onChange: value => context.setSelectedAceiType(value),
    widthPx: 220
  };

  const aceiMonitorType = convertAceiMonitorTypeOptionToEnum(context.selectedAceiType);

  return [dropdown, aceiMonitorType];
};

/**
 * The Environment History Panel
 * Composes together the toolbar and the env history charts.
 * Performs a query when a monitor type is selected (or if it is passed down). Then renders the
 * line charts based on the result.
 * Renders a non-ideal state if the query doesn't get the required data in the result.
 */
export const EnvironmentHistoryPanel: React.FunctionComponent<EnvironmentHistoryPanelProps> = props => {
  const [startTime, endTime, timeIntervalSelector] = useTrendTimeIntervalSelector(
    'ACEI',
    props.sohHistoricalDurations
  );

  const [monitorTypeDropdown, selectedMonitorType] = useMonitorTypeDropdown();

  const [widthPx] = useBaseDisplaySize();

  return (
    <React.Fragment>
      <BaseToolbar
        widthPx={widthPx}
        itemsLeft={[monitorTypeDropdown]}
        items={[timeIntervalSelector]}
      />
      {selectedMonitorType ? (
        <DrillDownTitle
          title={props.station.stationName}
          subtitle={prettifyAllCapsEnumType(selectedMonitorType)}
        />
      ) : (
        undefined
      )}
      <HistoricalAceiQuery
        stationName={props.station.stationName}
        startTime={startTime}
        endTime={endTime}
        type={selectedMonitorType}
      >
        <HistoricalAceiQueryContext.Consumer>
          {context => {
            // update loading state for the button that shows this panel
            const nonIdealState = validateNonIdealState(
              props,
              selectedMonitorType,
              context,
              startTime,
              endTime
            );
            if (nonIdealState) {
              return nonIdealState;
            }

            return useWeavessDisplay ? (
              <WeavessDisplay {...props} startTime={startTime} endTime={endTime} />
            ) : (
              <VictoryDisplay
                {...props}
                widthPx={widthPx}
                startTime={startTime}
                endTime={endTime}
              />
            );
          }}
        </HistoricalAceiQueryContext.Consumer>
      </HistoricalAceiQuery>
    </React.Fragment>
  );
};
