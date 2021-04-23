import { SohTypes } from '@gms/common-graphql';
import {
  determinePrecisionByType,
  MILLISECONDS_IN_SECOND,
  setDecimalPrecisionAsNumber,
  toDateString,
  toTimeString
} from '@gms/common-util';
import { ValueType } from '@gms/common-util/lib/types/value-type';
import { ChartTypes } from '@gms/ui-core-components';
import { Line } from '@gms/ui-core-components/lib/components/charts/types';
import { DistinctColorPalette } from '@gms/ui-util';
import { dataAcquisitionUIConfig } from '~components/data-acquisition-ui/config';
import { HistoricalSohQueryData } from '~components/data-acquisition-ui/react-apollo-components/historical-soh-query';
import { BarLineChartData } from '~components/data-acquisition-ui/shared/chart/types';
import { THOUSAND } from './constants';
import { MISSING_LAG } from './types';

/** Returns based on the type provided */
export const getName = (type: MISSING_LAG): string =>
  type === SohTypes.SohMonitorType.LAG ? 'Lag' : 'Missing';

/** Returns the label based on the type provided */
export const getLabel = (type: MISSING_LAG): string => `${getName(type)} data`;

/** Returns the data type of the data based on the type provided */
export const getDataType = (type: MISSING_LAG): string =>
  type === SohTypes.SohMonitorType.LAG ? 's' : '%';

/** A helper function for formatting chart tic labels; formats numbers to represent thousandths */
export const chartTickFormatForThousands = (value: number) =>
  value >= THOUSAND ? `${value / THOUSAND}k` : value;

/** Returns the bar chart Y axis label based on the type provided */
export const barChartYAxisLabel = (type: MISSING_LAG): string =>
  `Average ${getName(type)} (${getDataType(type)})`;

/** Returns the bar chart X axis label */
export const barChartXAxisLabel = (): string => 'Channel Name';

/** The bar chart Y axis tic formatter */
export const barChartYAxisTicFormat = chartTickFormatForThousands;

/** The bar chart X axis formatter */
export const barChartXAxisTicFormat = (stationName: string) => (name: string) =>
  name && name.replace ? name.replace(`${stationName}.`, '') : name;

/** Returns the line chart Y axis label based on the type provided */
export const lineChartYAxisLabel = (type: MISSING_LAG): string =>
  `${getName(type)} (${getDataType(type)})`;

/** Returns the line chart X axis label */
export const lineChartXAxisLabel = (): string => 'Time';

/** The line chart Y axis tic formatter */
export const lineChartYAxisTicFormat = chartTickFormatForThousands;

/** The line chart X axis tic formatter */
export const lineChartXAxisTicFormat = (timestamp: number) => {
  const dateString = toDateString(timestamp / MILLISECONDS_IN_SECOND);
  const timeString = toTimeString(timestamp / MILLISECONDS_IN_SECOND);
  return `${dateString}\n${timeString}`;
};

export const getChartHeight = (targetHeightPx: number) =>
  Math.max(targetHeightPx, dataAcquisitionUIConfig.dataAcquisitionUserPreferences.minChartHeightPx);

/**
 * Helper function to return properly scaled value from MonitorValue value
 */
const getMonitorValue = (value: number, monitorType: SohTypes.SohMonitorType) =>
  monitorType === SohTypes.SohMonitorType.LAG
    ? setDecimalPrecisionAsNumber(value) / MILLISECONDS_IN_SECOND
    : setDecimalPrecisionAsNumber(value);

/**
 * Iterate over a channel's values and remove duplicate values used by the LineDef
 * @param values The values to iterate over
 * @param monitorType SohMonitor type i.e. lag or Missing
 * @param calculationTimes The y value in the line plot (time)
 *
 * @return The line (x,y values), with duplicate x values dropped and
 *         the average of all the values is used by the Bar chart in the BarDefinition.
 */
const lineValuesAndAverage = (
  values: number[],
  monitorType: SohTypes.SohMonitorType,
  calculationTimes: number[]
): [Line, number] => {
  // Total value of all the MonitorValue values used in calculating the average value for bar chart
  let sumValue = 0;
  const line: Line = values
    .map((value, index: number) => {
      const currentValue = getMonitorValue(value, monitorType);
      // Add current value
      sumValue += currentValue;

      // If this is not the first value or last value look to see if it can be dropped
      if (index > 0 && index < values.length - 1) {
        // Get the value before and the value after current to compare to
        const beforeValue = getMonitorValue(values[index - 1], monitorType);
        const afterValue = getMonitorValue(values[index + 1], monitorType);
        if (currentValue === beforeValue && currentValue === afterValue) {
          return undefined;
        }
      }
      return {
        x: calculationTimes[index],
        y: currentValue
      };
    })
    .filter(value => value !== undefined);

  // Return the points of the line and the average
  // Average: Total sum of the values and divide by the number of points
  return [line, sumValue / values.length];
};

/**
 * Returns the chart data needed to populate the bar and line charts for
 * the historical data.
 * @param props the missing/lag history props
 * @param context the historical soh query data
 */
export const getChartData = (
  monitorType: MISSING_LAG,
  station: SohTypes.UiStationSoh,
  context: HistoricalSohQueryData,
  colorPalette: DistinctColorPalette,
  valueType: ValueType
): BarLineChartData => {
  const monitorValues = context.data.monitorValues;

  const categories: { x: string[]; y: string[] } = {
    x: monitorValues.map(monitor => monitor.channelName),
    y: []
  };

  const lineDefs: ChartTypes.LineDefinition[] = [];
  const barDefs: ChartTypes.BarDefinition[] = [];

  // Iterate through each channel monitor type building the line definition
  // and bar definition and add them to their respective arrays
  monitorValues.forEach(monitorValue => {
    // Get the Point (x,y) values used in the line definition
    // and average value value of x points used in the bar definition
    const [lineValues, avgValue] = lineValuesAndAverage(
      monitorValue.valuesByType[monitorType].values,
      monitorType,
      context.data.calculationTimes
    );

    // Add the line definition for this channel
    lineDefs.push({
      id: monitorValue.channelName,
      color: colorPalette.getColorString(monitorValue.channelName),
      value: lineValues
    });
    // Add the bar definition for this channel
    barDefs.push({
      id: monitorValue.channelName,
      color: colorPalette.getColorString(monitorValue.channelName),
      value: {
        x: categories.x.find(name => name === monitorValue.channelName),
        y: determinePrecisionByType(avgValue, valueType, false) as number
      }
    });
  });

  // Valid the bar definitions
  barDefs.forEach(
    (barDef, index) =>
      barDef.id !== categories.x[index] &&
      // tslint:disable-next-line: no-console
      console.error('channel names of historical data may not match actual values')
  );

  const statuses = station.channelSohs.map(c =>
    c.allSohMonitorValueAndStatuses.find(v => v.monitorType === monitorType)
  );

  const thresholdsMarginal = statuses.map(s => s.thresholdMarginal);
  const thresholdsBad = statuses.map(s => s.thresholdBad);

  return { categories, lineDefs, barDefs, thresholdsMarginal, thresholdsBad };
};
