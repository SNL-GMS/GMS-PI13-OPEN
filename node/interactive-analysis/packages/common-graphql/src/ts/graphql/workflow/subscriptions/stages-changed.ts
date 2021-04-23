import gql from 'graphql-tag';
import { processingStageFragment } from '../gqls';

export const stagesChangedSubscription = gql`
  subscription stagesChanged {
    stagesChanged {
      ...ProcessingStageFragment
    }
  }
  ${processingStageFragment}
`;
