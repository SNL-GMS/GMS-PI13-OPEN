import gql from 'graphql-tag';
import { channelSohFragment } from '../gqls';

export const channelSohForStationQuery = gql`
  query channelSohForStation($stationName: String) {
    channelSohForStation(stationName: $stationName) {
      stationName
      uuid
      channelSohs {
        ...ChannelSohFragment
      }
    }
  }
  ${channelSohFragment}
`;
