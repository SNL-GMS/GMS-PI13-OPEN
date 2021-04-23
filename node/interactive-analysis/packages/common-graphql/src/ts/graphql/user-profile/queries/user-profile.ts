import gql from 'graphql-tag';
import { userProfileFragment } from '../gqls';

export const userProfileQuery = gql`
  query userProfile($defaultLayoutName: String!) {
    userProfile {
      ...UserProfileFragment
    }
  }
  ${userProfileFragment}
`;
