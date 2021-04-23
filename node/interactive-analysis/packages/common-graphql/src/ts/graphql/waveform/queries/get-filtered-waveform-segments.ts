import gql from 'graphql-tag';
import { filteredChannelSegmentFragment } from '../gqls';

export const getFilteredWaveformSegmentsByChannelsQuery = gql`
  query getFilteredWaveformSegmentsByChannels(
    $timeRange: TimeRange!
    $channelIds: [String]!
    $filterIds: [String]
  ) {
    getFilteredWaveformSegmentsByChannels(
      timeRange: $timeRange
      channelIds: $channelIds
      filterIds: $filterIds
    ) {
      ...FilteredChannelSegmentFragment
    }
  }
  ${filteredChannelSegmentFragment}
`;
