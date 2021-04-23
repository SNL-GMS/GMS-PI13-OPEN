import { ChartTypes, LineChart } from '@gms/ui-core-components';
import { DistinctColorPalette, Point } from '@gms/ui-util';
import * as React from 'react';
import { HistoricalAceiQueryContext } from '~components/data-acquisition-ui/react-apollo-components/historical-acei-query';
import { useDomain } from '~components/data-acquisition-ui/shared/chart/custom-hooks';
import { chartTickFormatForThousands } from '../missing-lag-history/utils';
import { ExternalAxis } from './external-axis';
import { EnvironmentHistoryPanelProps } from './types';

const PADDING_PX = 20;

export const VictoryDisplay: React.FunctionComponent<EnvironmentHistoryPanelProps & {
  startTime: Date;
  endTime: Date;
  widthPx: number;
}> = props => {
  const context = React.useContext(HistoricalAceiQueryContext);

  const [domain, zoomDomain, setZoomDomain] = useDomain(props.startTime, props.endTime);
  const channelSohs = props.channelSohs;
  const channelNames = channelSohs.map(channel => channel.channelName);
  /** unique color palette created for the channels and station */
  const [colorPalette] = React.useState<DistinctColorPalette>(
    new DistinctColorPalette(channelNames, props.station.stationName)
  );

  return (
    <React.Fragment>
      <div className={'environment-history-display__chart-container'}>
        {channelSohs.map(channelSoh => {
          const data = context.data.find(c => c.channelName === channelSoh.channelName);

          const lineDefs = data?.issues.map<ChartTypes.LineDefinition>(issue => ({
            id: channelSoh.channelName,
            color: colorPalette.getColorString(channelSoh.channelName),
            value: issue.map<Point>(p => ({
              x: p[0],
              y: p[1]
            }))
          }));

          return (
            <React.Fragment key={channelSoh.channelName}>
              <LineChart
                key={channelSoh.channelName}
                id={props.station.stationName}
                classNames={'table-display'}
                domain={domain}
                zoomDomain={zoomDomain}
                onZoomDomainChange={setZoomDomain}
                lineDefs={lineDefs}
                padding={{ top: 10, right: 40, bottom: 5, left: 80 }}
                // tslint:disable-next-line: no-magic-numbers
                heightPx={120}
                widthPx={props.widthPx - PADDING_PX * 2}
                interpolation={'stepAfter'}
                stepChart={true}
                suppressXAxis={true}
                yTickCount={2}
                // TODO handle non-boolean values (analog values)
                yTickValues={[0, 1]}
                yAxisLabel={channelSoh.channelName.replace(`${props.station.stationName}.`, '')}
                yTickFormat={chartTickFormatForThousands}
              />
            </React.Fragment>
          );
        })}
      </div>
      <ExternalAxis
        domain={domain}
        zoomDomain={zoomDomain}
        onZoomDomainChange={setZoomDomain}
        widthPx={props.widthPx}
      />
    </React.Fragment>
  );
};
