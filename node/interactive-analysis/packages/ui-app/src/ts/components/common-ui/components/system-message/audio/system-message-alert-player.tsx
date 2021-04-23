import { SystemMessageTypes } from '@gms/common-graphql/lib/graphql';
import { SystemMessageType } from '@gms/common-graphql/lib/graphql/system-message/types';
import { UILogger } from '@gms/ui-apollo';
import { Toaster } from '@gms/ui-util';
import { head, last, partialRight } from 'lodash';
import sortBy from 'lodash/sortBy';
import * as React from 'react';
import { userPreferences } from '~components/common-ui/config/user-preferences';
import { AudibleNotificationContext } from './audible-notification-context';

const BASE_SOUNDS_PATH = userPreferences.baseSoundsPath;

export interface SystemMessageAlertProps {
  id?: string;
  isSoundEnabled: boolean;
  latestSystemMessages: SystemMessageTypes.SystemMessage[];
}

const severityOrder = {
  INFO: 2,
  WARNING: 1,
  CRITICAL: 0
};

const toaster = new Toaster();

/**
 * Pick a single system message based on highest severity and most recent.
 * @param latestMessages - System Messages received.
 * @returns - undefined or a single SystemMessage.
 */
const pickSystemMessage = (
  latestMessages: SystemMessageTypes.SystemMessage[]
): SystemMessageTypes.SystemMessage | undefined => {
  const sortBySeverity = partialRight(sortBy, [a => severityOrder[a.severity]]);
  const sortByDate = partialRight(sortBy, [a => a.time]);

  let mostSevereList: SystemMessageTypes.SystemMessage[] = sortBySeverity(latestMessages);
  const mostSevere = head(mostSevereList)?.severity;
  mostSevereList = mostSevereList.filter(systemMessage => systemMessage.severity === mostSevere);
  const sorted: SystemMessageTypes.SystemMessage = last(sortByDate(mostSevereList));
  return sorted;
};

export const SystemMessageAlertPlayer: React.FunctionComponent<SystemMessageAlertProps> = props => {
  const { audibleNotifications } = React.useContext(AudibleNotificationContext);
  const ref = React.createRef<HTMLAudioElement>();

  const configuredSounds: SystemMessageTypes.SystemMessage[] = props.latestSystemMessages?.filter(
    message =>
      audibleNotifications?.find(
        notification =>
          SystemMessageType[message.type] === SystemMessageType[notification.notificationType]
      )
  );

  const sorted: SystemMessageTypes.SystemMessage = pickSystemMessage(configuredSounds);

  const sound = sorted
    ? audibleNotifications?.find(
        notification =>
          SystemMessageType[notification.notificationType] === SystemMessageType[sorted.type]
      )?.fileName
    : undefined;

  React.useEffect(() => {
    if (ref && ref.current && sound && props.isSoundEnabled) {
      ref.current.play().catch(e => {
        UILogger.Instance().error(`Failed to play alert ${sound}: ${e}`);
        toaster.toastError(userPreferences.configuredAudibleNotificationFileNotFound(sound));
      });
    }
  }, [props.latestSystemMessages]);

  return sound ? <audio ref={ref} key={sound} src={`${BASE_SOUNDS_PATH}${sound}`} /> : null;
};
