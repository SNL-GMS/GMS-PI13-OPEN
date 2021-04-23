import { AudibleNotification } from '@gms/common-graphql/lib/graphql/user-profile/types';
import * as React from 'react';
import { MutationFetchResult } from 'react-apollo';

export interface AudibleNotificationContextData {
  audibleNotifications: AudibleNotification[];
  setAudibleNotifications(notifications: AudibleNotification[]): Promise<MutationFetchResult<{}>>;
}

/**
 * The audible notification context
 */
export const AudibleNotificationContext: React.Context<AudibleNotificationContextData> = React.createContext<
  AudibleNotificationContextData
>(undefined);
