import gql from 'graphql-tag';

export const saveStationGroupSohStatusMutation = gql`
  mutation saveStationGroupSohStatus($input: [StationGroupSohStatusInput]!) {
    saveStationGroupSohStatus(input: $input) {
      result
    }
  }
`;
