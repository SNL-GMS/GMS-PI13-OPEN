import gql from 'graphql-tag';
import { channelSegmentFragment } from '../../channel-segment/gqls';

export const getRawWaveformSegmentsByChannelsQuery = gql`
  query getRawWaveformSegmentsByChannels($timeRange: TimeRange!, $channelIds: [String]!) {
    getRawWaveformSegmentsByChannels(timeRange: $timeRange, channelIds: $channelIds) {
      ...ChannelSegmentFragment
    }
  }
  ${channelSegmentFragment}
`;
