import { IconNames } from '@blueprintjs/icons';
import { SohTypes } from '@gms/common-graphql';
import { nonIdealStateWithNoSpinner, nonIdealStateWithSpinner } from '@gms/ui-core-components';
import { HistoricalAceiQueryData } from '~components/data-acquisition-ui/react-apollo-components/historical-acei-query';
// !!TODO This dependency should not exist; i.e. soh-env should not depend on env-history
import { EnvironmentHistoryPanelProps } from './types';

/** Returns the loading non ideal state - used when the historical data query is loading */
const loading = (props: EnvironmentHistoryPanelProps) =>
  nonIdealStateWithSpinner('Loading', `Historical data`);

/** Returns the error non ideal state for when the start and end times are in error */
const badStartEndTime = () =>
  nonIdealStateWithNoSpinner('Error', 'Invalid start and end times', IconNames.ERROR);

/** Returns the non ideal state that indicates that there is no historical data */
const noData = (props: EnvironmentHistoryPanelProps) =>
  nonIdealStateWithNoSpinner('No Data', `No historical environmental data`);

/** Returns the non ideal state that indicates that no monitor type is selected */
const noMonitorSelected = () =>
  nonIdealStateWithNoSpinner('No Monitor Selected', 'Select a environmental monitor type');

/**
 * Validates the non ideal state for the acei query component.
 * Returns the correct non ideal state if the condition is met.
 *
 * @param props the props
 * @param context the query context (the historical query data)
 * @param startTime the start time
 * @param endTime the end time
 */
export const validateNonIdealState = (
  props: EnvironmentHistoryPanelProps,
  selectedMonitorType: SohTypes.AceiType,
  context: HistoricalAceiQueryData,
  startTime: Date,
  endTime: Date
) => {
  if (selectedMonitorType === undefined) {
    return noMonitorSelected();
  }

  if (context.loading) {
    return loading(props);
  }

  if (startTime.valueOf() > endTime.valueOf()) {
    return badStartEndTime();
  }

  if (!context.data || context.data.length < 1) {
    return noData(props);
  }

  return undefined;
};
