import { UserProfileTypes } from '@gms/common-graphql';
import { useForceGlUpdateOnResizeAndShow } from '@gms/ui-util';
import * as React from 'react';
import { SystemMessageAlertPlayer } from '~components/common-ui/components/system-message/audio';
import { sharedSohTableClasses } from '~components/data-acquisition-ui/shared/table/utils';
import { BaseDisplay } from '../base-display';
import { AudibleNotificationContext } from './audio/audible-notification-context';
import {
  validateAvailableSounds,
  validateConfiguredAudibleNotifications
} from './sound-configuration/sound-configuration-util';
import { SystemMessageTable } from './system-message-table';
import { useSeverityFilters } from './toolbar/severity-filters';
import { SystemMessageToolbar } from './toolbar/system-message-toolbar';
import { SystemMessageProps } from './types';

/**
 * The system message component
 * @param props the system message component props
 */
export const SystemMessage: React.FunctionComponent<SystemMessageProps> = React.memo(props => {
  const [severityFilterMap, setSeverityFilterMap] = useSeverityFilters();

  /** state for enabling and disabling auto scrolling */
  const [isAutoScrollingEnabled, setIsAutoScrollingEnabled] = React.useState(true);

  /** state for enabling and disabling sounds */
  const [isSoundEnabled, setIsSoundEnabled] = React.useState(true);

  /** validate available sound files; toast error message for each sound file that missing */
  React.useEffect(validateAvailableSounds, []);

  /** validate the configured audible notifications; toast error message for each sound file that is missing */
  React.useEffect(
    () =>
      validateConfiguredAudibleNotifications(
        props?.userProfileQuery?.userProfile?.audibleNotifications
      ),
    [props.userProfileQuery?.userProfile?.audibleNotifications]
  );

  /** force update on golden layout resize and show -> ensures that the toolbar is properly sized */
  useForceGlUpdateOnResizeAndShow(props.glContainer);

  const setAudibleNotifications = (notifications: UserProfileTypes.AudibleNotification[]) =>
    props.setAudibleNotifications({
      variables: {
        audibleNotificationsInput: notifications
      }
    });

  return (
    <AudibleNotificationContext.Provider
      value={{
        audibleNotifications: props.userProfileQuery?.userProfile?.audibleNotifications,
        setAudibleNotifications
      }}
    >
      <BaseDisplay
        glContainer={props.glContainer}
        className={`system-message-display ${sharedSohTableClasses}`}
      >
        <SystemMessageToolbar
          // tslint:disable: no-unbound-method
          addSystemMessages={props.addSystemMessages}
          clearAllSystemMessages={props.clearAllSystemMessages}
          clearExpiredSystemMessages={props.clearExpiredSystemMessages}
          clearSystemMessages={props.clearSystemMessages}
          // tslint:enable: no-unbound-method
          systemMessagesState={props.systemMessagesState}
          isAutoScrollingEnabled={isAutoScrollingEnabled}
          setIsAutoScrollingEnabled={setIsAutoScrollingEnabled}
          isSoundEnabled={isSoundEnabled}
          setIsSoundEnabled={setIsSoundEnabled}
          systemMessageDefinitions={props.systemMessageDefinitionsQuery.systemMessageDefinitions}
          severityFilterMap={severityFilterMap}
          setSeverityFilterMap={m => {
            setSeverityFilterMap(m);
          }}
        />
        <SystemMessageTable
          // tslint:disable: no-unbound-method
          addSystemMessages={props.addSystemMessages}
          clearAllSystemMessages={props.clearAllSystemMessages}
          clearExpiredSystemMessages={props.clearExpiredSystemMessages}
          clearSystemMessages={props.clearSystemMessages}
          // tslint:enable: no-unbound-method
          systemMessages={props.systemMessagesState.systemMessages?.filter(
            msg => severityFilterMap?.get(msg.severity) ?? true
          )}
          isAutoScrollingEnabled={isAutoScrollingEnabled}
          setIsAutoScrollingEnabled={setIsAutoScrollingEnabled}
          severityFilterMap={severityFilterMap}
        />
        <SystemMessageAlertPlayer
          isSoundEnabled={isSoundEnabled}
          latestSystemMessages={props.systemMessagesState.latestSystemMessages}
        />
      </BaseDisplay>
      )}
    </AudibleNotificationContext.Provider>
  );
});
