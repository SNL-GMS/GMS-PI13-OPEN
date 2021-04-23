import { NumberCellRendererParams } from '@gms/ui-core-components';
import * as React from 'react';
import { SohRollupCell } from '~components/data-acquisition-ui/shared/table/soh-cell-renderers';
import { CellStatus, formatSohValue } from '~components/data-acquisition-ui/shared/table/utils';
import { getCellStatus, getDataReceivedStatus, setTooltip } from '../../../shared/table/utils';
import { StationStatisticsTableDataContext } from '../types';
import { StationStatisticsDragCell } from './station-statistics-drag-cell';

/**
 * Creates a timeliness cell as a solid cell
 */
const TimelinessCellRenderer: React.FunctionComponent<NumberCellRendererParams> = props => (
  <StationStatisticsTableDataContext.Consumer>
    {context => {
      const data = context.data.find(d => d.id === props.data.id);
      if (!data) return null;

      const stationName = data.stationData?.stationName;
      const dataReceivedStatus = getDataReceivedStatus(data.stationTimeliness);

      // If it is station timeliness, set to that value; otherwise, use channel timeliness
      const titleToUse = props.isStationCell
        ? data.stationTimeliness?.toString()
        : setTooltip(data.channelTimeliness);

      // If it is station timeliness, it is non-contributing; otherwise, figure out channel rollup
      const cellStatusToUse = props.isStationCell
        ? CellStatus.NON_CONTRIBUTING
        : getCellStatus(data.channelTimeliness?.status, data.channelTimeliness?.isContributing);

      // If it is station timeliness, set to data received status above; otherwise, figure out channel status
      const dataReceivedStatusToUse = props.isStationCell
        ? dataReceivedStatus
        : getDataReceivedStatus(data.channelTimeliness);

      // If it is station timeliness, format stationTimeliness; otherwise, format channel value
      const valueToUse = props.isStationCell
        ? formatSohValue(data.stationTimeliness)
        : formatSohValue(data.channelTimeliness?.value);

      return (
        <StationStatisticsDragCell stationId={stationName}>
          <div title={`${titleToUse}`} data-cy="timeliness-cell">
            <SohRollupCell
              {...props}
              className={`
                soh-cell--solid
                table-cell--numeric`}
              cellStatus={cellStatusToUse}
              dataReceivedStatus={dataReceivedStatusToUse}
              stationId={`${stationName}`}
              value={valueToUse}
            />
          </div>
        </StationStatisticsDragCell>
      );
    }}
  </StationStatisticsTableDataContext.Consumer>
);

export const ChannelTimelinessCellRenderer: React.FunctionComponent<NumberCellRendererParams> = props => (
  <TimelinessCellRenderer {...props} isStationCell={false} />
);

export const StationTimelinessCellRenderer: React.FunctionComponent<NumberCellRendererParams> = props => (
  <TimelinessCellRenderer {...props} isStationCell={true} />
);
