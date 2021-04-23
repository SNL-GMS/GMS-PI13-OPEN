import gql from 'graphql-tag';
import { dataPayloadFragment } from '../../cache/gqls';

export const changeSignalDetectionAssociationsMutation = gql`
  mutation changeSignalDetectionAssociations(
    $eventHypothesisId: String!
    $signalDetectionIds: [String]!
    $associate: Boolean!
  ) {
    changeSignalDetectionAssociations(
      eventHypothesisId: $eventHypothesisId
      signalDetectionIds: $signalDetectionIds
      associate: $associate
    ) {
      ...DataPayloadFragment
    }
  }
  ${dataPayloadFragment}
`;
