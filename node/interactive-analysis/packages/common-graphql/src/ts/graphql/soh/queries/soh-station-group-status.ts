import gql from 'graphql-tag';
import { stationAndStationGroupSohFragment } from '../gqls';

export const sohStationAndGroupStatusQuery = gql`
  query stationAndStationGroupSoh {
    stationAndStationGroupSoh {
      ...StationAndStationGroupSohFragment
    }
  }
  ${stationAndStationGroupSohFragment}
`;
