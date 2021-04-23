import { Classes, Colors, Toaster } from '@blueprintjs/core';
import React from 'react';
import { DEFAULT_UNDEFINED_EVENTS } from '../constants';
import { DataByTime } from '../entities';
import { Weavess, WeavessTypes, WeavessUtils } from '../weavess';

export interface WeavessFlatLineExampleProps {
  showExampleControls: boolean;
}

export interface WeavessFlatLineExampleState {
  toggleShowContent: string;
  stations: WeavessTypes.Station[];
}

export class WeavessFlatLineExample extends React.Component<
  WeavessFlatLineExampleProps,
  WeavessFlatLineExampleState
> {
  public static defaultProps: WeavessFlatLineExampleProps = {
    showExampleControls: true
  };

  public static SAMPLE_RATE: number = 0.1;

  // tslint:disable-next-line:no-magic-numbers
  public static NUM_SAMPLES: number = WeavessFlatLineExample.SAMPLE_RATE * 1800; // 10 minutes of data

  // tslint:disable-next-line:no-magic-numbers
  public static startTimeSecs: number = 1507593600; // Tue, 10 Oct 2017 00:00:00 GMT

  // tslint:disable-next-line:no-magic-numbers
  public static endTimeSecs: number = WeavessFlatLineExample.startTimeSecs + 1800; // + 30 minutes

  public toaster: Toaster;

  public weavess: Weavess;

  public constructor(props: WeavessFlatLineExampleProps) {
    super(props);
    this.state = {
      toggleShowContent: '',
      stations: []
    };
  }

  public componentDidMount() {
    this.setState({
      stations: this.generateDummyData()
    });
  }

  /* tslint:disable:no-magic-numbers */
  // tslint:disable-next-line: cyclomatic-complexity
  public render() {
    return (
      <div
        className={Classes.DARK}
        style={{
          height: '90%',
          width: '100%',
          padding: '0.5rem',
          color: Colors.GRAY4,
          display: 'flex',
          justifyContent: 'center',
          alignItems: 'center'
        }}
      >
        <div
          className={Classes.DARK}
          style={{
            height: '100%',
            width: '100%'
          }}
        >
          <div
            style={{
              height: '100%',
              width: '100%',
              display: 'flex',
              flexDirection: 'column'
            }}
          >
            <div
              style={{
                flex: '1 1 auto',
                position: 'relative'
              }}
            >
              <div
                style={{
                  position: 'absolute',
                  top: '0px',
                  bottom: '0px',
                  left: '0px',
                  right: '0px'
                }}
              >
                <Weavess
                  ref={ref => {
                    if (ref) {
                      this.weavess = ref;
                    }
                  }}
                  startTimeSecs={WeavessFlatLineExample.startTimeSecs}
                  endTimeSecs={WeavessFlatLineExample.endTimeSecs}
                  stations={this.state.stations}
                  selections={{
                    channels: undefined
                  }}
                  configuration={{
                    suppressLabelYAxis: true
                  }}
                  events={DEFAULT_UNDEFINED_EVENTS}
                  markers={{
                    verticalMarkers: [
                      {
                        id: 'marker',
                        color: 'pink',
                        lineStyle: WeavessTypes.LineStyle.DASHED,
                        timeSecs: WeavessFlatLineExample.startTimeSecs + 1200
                      }
                    ]
                  }}
                />
              </div>
            </div>
          </div>
        </div>
      </div>
    );
  }

  private readonly generateDummyData = () => {
    const stations: WeavessTypes.Station[] = [];

    // create channels w/ random noise as data
    for (let i = 0; i < 10; i += 1) {
      const data: DataByTime = { values: [] };
      // tslint:disable: max-line-length
      let value = Math.round(WeavessUtils.RandomNumber.getSecureRandomNumber());
      data.values.push({ timeSecs: WeavessFlatLineExample.startTimeSecs, value });
      data.values.push({ timeSecs: WeavessFlatLineExample.startTimeSecs + 100, value });
      data.values.push({ timeSecs: WeavessFlatLineExample.startTimeSecs + 300, value });
      value = Math.round(WeavessUtils.RandomNumber.getSecureRandomNumber());
      data.values.push({ timeSecs: WeavessFlatLineExample.startTimeSecs + 300, value });
      data.values.push({ timeSecs: WeavessFlatLineExample.startTimeSecs + 1000, value });
      value = Math.round(WeavessUtils.RandomNumber.getSecureRandomNumber());
      data.values.push({ timeSecs: WeavessFlatLineExample.startTimeSecs + 1000, value });
      data.values.push({ timeSecs: WeavessFlatLineExample.startTimeSecs + 1100, value });
      data.values.push({ timeSecs: WeavessFlatLineExample.startTimeSecs + 1200, value });
      value = Math.round(WeavessUtils.RandomNumber.getSecureRandomNumber());
      data.values.push({ timeSecs: WeavessFlatLineExample.startTimeSecs + 1200, value });
      value = Math.round(WeavessUtils.RandomNumber.getSecureRandomNumber());
      data.values.push({ timeSecs: WeavessFlatLineExample.startTimeSecs + 1200, value });
      data.values.push({ timeSecs: WeavessFlatLineExample.startTimeSecs + 1300, value });
      value = Math.round(WeavessUtils.RandomNumber.getSecureRandomNumber());
      data.values.push({ timeSecs: WeavessFlatLineExample.startTimeSecs + 1300, value });
      data.values.push({ timeSecs: WeavessFlatLineExample.startTimeSecs + 1500, value });
      data.values.push({ timeSecs: WeavessFlatLineExample.endTimeSecs, value });
      // tslint:enable: max-line-length

      stations.push({
        id: String(i),
        name: `station ${i}`,
        defaultChannel: {
          height: 50,
          defaultRange: {
            min: -1,
            max: 2
          },
          id: String(i),
          name: `channel ${i}`,
          waveform: {
            channelSegmentId: 'data',
            channelSegments: new Map<string, WeavessTypes.ChannelSegment>([
              [
                'data',
                {
                  dataSegments: [
                    {
                      color: 'dodgerblue',
                      displayType: [WeavessTypes.DisplayType.LINE],
                      pointSize: 4,
                      data
                    }
                  ]
                }
              ]
            ]),
            markers: {
              verticalMarkers: [
                {
                  id: 'marker',
                  color: 'lime',
                  lineStyle: WeavessTypes.LineStyle.DASHED,
                  timeSecs: WeavessFlatLineExample.startTimeSecs + 5
                }
              ]
            }
          }
        },
        nonDefaultChannels: []
      });
    }
    return stations;
  }
}
