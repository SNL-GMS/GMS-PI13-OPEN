import gql from 'graphql-tag';
import { processingStationFragment } from '../gqls';

export const defaultProcessingStationsQuery = gql`
  query defaultProcessingStations {
    defaultProcessingStations {
      ...ProcessingStationFragment
    }
  }
  ${processingStationFragment}
`;
