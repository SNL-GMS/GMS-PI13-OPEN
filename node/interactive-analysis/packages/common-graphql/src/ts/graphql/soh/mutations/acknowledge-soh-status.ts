import gql from 'graphql-tag';

export const acknowledgeSohStatusMutation = gql`
  mutation acknowledgeSohStatus($stationNames: [String]!, $comment: String) {
    acknowledgeSohStatus(stationNames: $stationNames, comment: $comment)
  }
`;
