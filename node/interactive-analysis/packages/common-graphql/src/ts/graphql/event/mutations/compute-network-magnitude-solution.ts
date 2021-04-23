import gql from 'graphql-tag';
import { dataPayloadFragment } from '../../cache/gqls';

/**
 * Mutation to get a network magnitude solution
 */
export const computeNetworkMagnitudeSolutionMutation = gql`
  mutation computeNetworkMagnitudeSolution(
    $computeNetworkMagnitudeSolutionInput: ComputeNetworkMagnitudeSolutionInput!
  ) {
    computeNetworkMagnitudeSolution(
      computeNetworkMagnitudeSolutionInput: $computeNetworkMagnitudeSolutionInput
    ) {
      status {
        stationId
        rational
      }
      dataPayload {
        ...DataPayloadFragment
      }
    }
  }
  ${dataPayloadFragment}
`;
