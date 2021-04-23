import { IconNames } from '@blueprintjs/icons';
import { SohTypes } from '@gms/common-graphql';
import { nonIdealStateWithNoSpinner, nonIdealStateWithSpinner } from '@gms/ui-core-components';
import { MISSING_LAG } from './types';
import { getLabel } from './utils';

/** Returns the loading non ideal state - used when the historical data query is loading */
const loading = (monitorType: MISSING_LAG) =>
  nonIdealStateWithSpinner('Loading', `Historical ${getLabel(monitorType)}`);

/** Returns the error non ideal state for when the start and end times are in error */
const badStartEndTime = () =>
  nonIdealStateWithNoSpinner('Error', 'Invalid start and end times', IconNames.ERROR);

/** Returns the non ideal state that indicates that there is no historical data */
const noData = (monitorType: MISSING_LAG) =>
  nonIdealStateWithNoSpinner('No Data', `No historical ${getLabel(monitorType)}`);

/**
 * Validates the non ideal state for the missing/lag component.
 * Returns the correct non ideal state if the condition is met.
 *
 * @param monitorType lag or missing
 * @param isLoading loading status of the query
 * @param historicalSohByStation UI historical soh data from a query
 * @param startTime the start time
 * @param endTime the end time
 */
export const validateNonIdealState = (
  monitorType: MISSING_LAG,
  isLoading: boolean,
  historicalSohByStation: SohTypes.UiHistoricalSoh,
  startTime: Date,
  endTime: Date
) => {
  if (isLoading) {
    return loading(monitorType);
  }

  if (startTime.valueOf() > endTime.valueOf()) {
    return badStartEndTime();
  }

  if (!historicalSohByStation || historicalSohByStation?.calculationTimes?.length <= 0) {
    return noData(monitorType);
  }

  return undefined;
};
