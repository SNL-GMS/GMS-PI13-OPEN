import gql from 'graphql-tag';
import { stationAndStationGroupSohFragment } from '../gqls';

export const quietChannelMonitorStatusesMutation = gql`
  mutation quietChannelMonitorStatuses($channelMonitorsToQuiet: [ChannelMonitorInput]!) {
    quietChannelMonitorStatuses(channelMonitorsToQuiet: $channelMonitorsToQuiet) {
      ...StationAndStationGroupSohFragment
    }
  }
  ${stationAndStationGroupSohFragment}
`;
