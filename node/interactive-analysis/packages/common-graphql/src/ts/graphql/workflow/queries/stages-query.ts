import gql from 'graphql-tag';
import { processingStageFragment } from '../gqls';

export const stagesQuery = gql`
  query stages {
    stages {
      ...ProcessingStageFragment
    }
  }
  ${processingStageFragment}
`;
