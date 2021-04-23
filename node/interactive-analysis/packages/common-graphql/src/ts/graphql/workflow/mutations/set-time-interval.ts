import gql from 'graphql-tag';
import { processingStageFragment } from '../gqls';

export const setTimeIntervalMutation = gql`
  mutation setTimeInterval($startTimeSec: Int!, $endTimeSec: Int!) {
    setTimeInterval(startTimeSec: $startTimeSec, endTimeSec: $endTimeSec) {
      ...ProcessingStageFragment
    }
  }
  ${processingStageFragment}
`;
