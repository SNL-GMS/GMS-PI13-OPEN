import gql from 'graphql-tag';

export const locationFragment = gql`
  fragment LocationFragment on Location {
    latitudeDegrees
    longitudeDegrees
    elevationKm
  }
`;

export const workspaceStateFragment = gql`
  fragment WorkspaceStateFragment on WorkspaceState {
    eventToUsers {
      eventId
      userNames
    }
  }
`;

export const versionInfoFragment = gql`
  fragment VersionInfoFragment on VersionInfo {
    versionNumber
    commitSHA
  }
`;
