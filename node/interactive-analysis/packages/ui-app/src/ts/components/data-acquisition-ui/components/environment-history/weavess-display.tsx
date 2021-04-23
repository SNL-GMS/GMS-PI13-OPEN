import { MILLISECONDS_IN_SECOND } from '@gms/common-util';
import { DistinctColorPalette, hslToHex } from '@gms/ui-util';
import { Weavess, WeavessConstants, WeavessTypes, WeavessUtils } from '@gms/weavess';
import * as React from 'react';
import { dataAcquisitionUserPreferences } from '~components/data-acquisition-ui/config';
import { HistoricalAceiQueryContext } from '~components/data-acquisition-ui/react-apollo-components/historical-acei-query';
import { EnvironmentHistoryPanelProps } from './types';

export const WeavessDisplay: React.FunctionComponent<EnvironmentHistoryPanelProps & {
  startTime: Date;
  endTime: Date;
}> = props => {
  const context = React.useContext(HistoricalAceiQueryContext);

  const channelSohs = props.channelSohs;
  const channelNames = channelSohs.map(channel => channel.channelName);

  /** unique color palette created for the channels and station */
  const [colorPalette] = React.useState<DistinctColorPalette>(
    new DistinctColorPalette(channelNames, props.station.stationName)
  );

  const stations: WeavessTypes.Station[] = [];
  channelSohs.forEach((channelSoh, i) => {
    const data = context.data.find(c => c.channelName === channelSoh.channelName);

    stations.push({
      id: String(i),
      name: `station ${channelSoh.channelName}`,
      defaultChannel: {
        height: 60,
        defaultRange: {
          min: -0.2,
          max: 1.2
        },
        yAxisTicks: [0, 1],
        id: channelSoh.channelName,
        name: `${channelSoh.channelName.replace(`${props.station.stationName}.`, '')}`,
        waveform: {
          channelSegmentId: 'data',
          channelSegments: new Map<string, WeavessTypes.ChannelSegment>([
            [
              'data',
              {
                dataSegments: data?.issues.map<WeavessTypes.DataSegment>(issue => ({
                  color: hslToHex(colorPalette.getColor(channelSoh.channelName)),
                  displayType: [WeavessTypes.DisplayType.LINE],
                  pointSize: 4,
                  data: WeavessUtils.Data.createStepPoints(issue)
                }))
              }
            ]
          ])
        }
      },
      nonDefaultChannels: []
    });
  });

  return (
    <div
      style={{
        height: `${dataAcquisitionUserPreferences.minChartHeightPx}px`
      }}
      className={'weavess-container'}
      tabIndex={0}
    >
      <div className={'weavess-container__wrapper'}>
        <Weavess
          startTimeSecs={props.startTime.valueOf() / MILLISECONDS_IN_SECOND}
          endTimeSecs={props.endTime.valueOf() / MILLISECONDS_IN_SECOND}
          stations={stations}
          selections={{
            channels: undefined
          }}
          configuration={{
            labelWidthPx: 180,
            defaultChannel: {
              disableMeasureWindow: true,
              disableMaskModification: true,
              disablePreditedPhaseModification: true,
              disableSignalDetectionModification: true
            }
          }}
          events={WeavessConstants.DEFAULT_UNDEFINED_EVENTS}
        />
      </div>
    </div>
  );
};
