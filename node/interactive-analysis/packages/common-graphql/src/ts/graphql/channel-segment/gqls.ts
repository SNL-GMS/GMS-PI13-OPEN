import gql from 'graphql-tag';
import { fkPowerSpectraFragment } from '../fk/gqls';
import { waveformFragment } from '../waveform/gqls';

export const timeseriesFragment = gql`
  fragment TimeseriesFragment on Timeseries {
    startTime
    sampleRate
    sampleCount
  }
`;
export const channelSegmentFragment = gql`
  fragment ChannelSegmentFragment on ChannelSegment {
    id
    name
    type
    channelId
    startTime
    endTime
    timeseriesType
    timeseries {
      ...TimeseriesFragment
      ...WaveformFragment
      ...FkPowerSpectraFragment
    }
  }
  ${fkPowerSpectraFragment}
  ${timeseriesFragment}
  ${waveformFragment}
`;
