import gql from 'graphql-tag';

export const waveformFragment = gql`
  fragment WaveformFragment on Waveform {
    startTime
    sampleRate
    sampleCount
    values
  }
`;

export const filteredChannelSegmentFragment = gql`
  fragment FilteredChannelSegmentFragment on FilteredChannelSegment {
    id
    type
    wfFilterId
    sourceChannelId
    channelId
    startTime
    endTime
    timeseries {
      ...WaveformFragment
    }
  }
  ${waveformFragment}
`;
