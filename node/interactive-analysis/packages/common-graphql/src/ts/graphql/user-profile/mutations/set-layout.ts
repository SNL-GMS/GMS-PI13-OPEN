import gql from 'graphql-tag';
import { userProfileFragment } from '../gqls';

export const setLayoutMutation = gql`
  mutation setLayout(
    $workspaceLayoutInput: UserLayoutInput!
    $defaultLayoutName: String!
    $saveAsDefaultLayout: String
  ) {
    setLayout(
      workspaceLayoutInput: $workspaceLayoutInput
      saveAsDefaultLayout: $saveAsDefaultLayout
    ) {
      ...UserProfileFragment
      defaultLayoutName(defaultLayoutName: $defaultLayoutName)
    }
  }
  ${userProfileFragment}
`;
