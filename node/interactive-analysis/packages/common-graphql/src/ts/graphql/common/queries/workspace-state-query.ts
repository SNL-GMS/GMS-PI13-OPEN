import gql from 'graphql-tag';
import { workspaceStateFragment } from '../gqls';

export const workspaceStateQuery = gql`
  query workspaceState {
    workspaceState {
      ...WorkspaceStateFragment
    }
  }
  ${workspaceStateFragment}
`;
