import { NumberCellRendererParams, PercentBar } from '@gms/ui-core-components';
import * as React from 'react';
import { SohRollupCell } from '~components/data-acquisition-ui/shared/table/soh-cell-renderers';
import { CellStatus, formatSohValue } from '~components/data-acquisition-ui/shared/table/utils';
import { getCellStatus, getDataReceivedStatus, setTooltip } from '../../../shared/table/utils';
import { StationStatisticsTableDataContext } from '../types';
import { StationStatisticsDragCell } from './station-statistics-drag-cell';

/**
 * Creates an environment cell, including percent bar child
 */
const EnvironmentCellRenderer: React.FunctionComponent<NumberCellRendererParams> = props => (
  <StationStatisticsTableDataContext.Consumer>
    {context => {
      const data = context.data.find(d => d.id === props.data.id);
      if (!data) return null;

      const stationName = data.stationData?.stationName;
      const dataReceivedStatus = getDataReceivedStatus(data.stationEnvironment);

      // If it is station environment, set to that value; otherwise, use channel environment
      const titleToUse = props.isStationCell
        ? data.stationEnvironment?.toString()
        : setTooltip(data.channelEnvironment);

      // If it is station environment, it is non-contributing; otherwise, figure out channel rollup
      const cellStatusToUse = props.isStationCell
        ? CellStatus.NON_CONTRIBUTING
        : getCellStatus(data.channelEnvironment?.status, data.channelEnvironment?.isContributing);

      // If it is station environment, set to data received status above; otherwise, figure out channel status
      const dataReceivedStatusToUse = props.isStationCell
        ? dataReceivedStatus
        : getDataReceivedStatus(data.channelEnvironment);

      // If it is station environment, format stationEnvironment; otherwise, format channel value
      const valueToUse = props.isStationCell
        ? formatSohValue(data.stationEnvironment)
        : formatSohValue(data.channelEnvironment?.value);

      // If it is station environment, set stationEnvironment percentage; otherwise, use channel percentage
      const percentageToUse = props.isStationCell
        ? data.stationEnvironment
        : data.channelEnvironment?.value;

      return (
        <StationStatisticsDragCell stationId={stationName}>
          <div title={`${titleToUse}`} data-cy="environment-cell">
            <SohRollupCell
              {...props}
              className={`table-cell--numeric`}
              cellStatus={cellStatusToUse}
              dataReceivedStatus={dataReceivedStatusToUse}
              stationId={`${stationName}`}
              value={valueToUse}
            >
              <PercentBar percentage={percentageToUse} />
            </SohRollupCell>
          </div>
        </StationStatisticsDragCell>
      );
    }}
  </StationStatisticsTableDataContext.Consumer>
);

export const ChannelEnvironmentCellRenderer: React.FunctionComponent<NumberCellRendererParams> = props => (
  <EnvironmentCellRenderer {...props} isStationCell={false} />
);

export const StationEnvironmentCellRenderer: React.FunctionComponent<NumberCellRendererParams> = props => (
  <EnvironmentCellRenderer {...props} isStationCell={true} />
);
