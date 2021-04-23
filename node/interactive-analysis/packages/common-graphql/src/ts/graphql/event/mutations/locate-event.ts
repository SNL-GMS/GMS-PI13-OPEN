import gql from 'graphql-tag';
import { dataPayloadFragment } from '../../cache/gqls';

/**
 * Locate Event Mutation Definition
 */
export const locateEventMutation = gql`
  mutation locateEvent(
    $eventHypothesisId: String!
    $preferredLocationSolutionId: String!
    $locationBehaviors: [LocationBehaviorInput]!
  ) {
    locateEvent(
      eventHypothesisId: $eventHypothesisId
      preferredLocationSolutionId: $preferredLocationSolutionId
      locationBehaviors: $locationBehaviors
    ) {
      ...DataPayloadFragment
    }
  }
  ${dataPayloadFragment}
`;
