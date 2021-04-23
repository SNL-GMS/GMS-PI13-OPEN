import gql from 'graphql-tag';

export const userProfileFragment = gql`
  fragment UserProfileFragment on UserProfile {
    defaultLayoutName(defaultLayoutName: $defaultLayoutName)
    userId
    workspaceLayouts {
      name
      supportedUserInterfaceModes
      layoutConfiguration
    }
    audibleNotifications {
      notificationType
      fileName
    }
    id
  }
`;
