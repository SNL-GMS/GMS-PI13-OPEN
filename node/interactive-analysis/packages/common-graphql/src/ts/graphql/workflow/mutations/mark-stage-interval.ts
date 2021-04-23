import gql from 'graphql-tag';

export const markStageIntervalMutation = gql`
  mutation markStageInterval($stageIntervalId: String!, $input: IntervalStatusInput!) {
    markStageInterval(stageIntervalId: $stageIntervalId, input: $input) {
      id
    }
  }
`;
