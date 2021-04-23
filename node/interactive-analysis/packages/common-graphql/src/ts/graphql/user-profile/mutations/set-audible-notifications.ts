import gql from 'graphql-tag';

export const setAudibleNotificationsMutation = gql`
  mutation setAudibleNotifications($audibleNotificationsInput: [AudibleNotificationsInput]!) {
    setAudibleNotifications(audibleNotificationsInput: $audibleNotificationsInput) {
      audibleNotifications {
        notificationType
        fileName
      }
    }
  }
`;
