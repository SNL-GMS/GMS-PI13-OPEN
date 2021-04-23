import gql from 'graphql-tag';

export const saveReferenceStationMutation = gql`
  mutation saveReferenceStation($input: DataAcqReferenceStation!) {
    saveReferenceStation(input: $input) {
      result
    }
  }
`;
