import gql from 'graphql-tag';
import { referenceStationFragment } from '../gqls';

export const defaultReferenceStationsQuery = gql`
  query defaultReferenceStations {
    defaultReferenceStations {
      ...ReferenceStationFragment
    }
  }
  ${referenceStationFragment}
`;
