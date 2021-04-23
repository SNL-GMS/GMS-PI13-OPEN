import { CacheTypes } from '@gms/common-graphql';

export interface MessageConfig {
  historyFilters: Map<string, boolean>;
  tooltipMessages: {
    history: {
      currentHistoryActionMessage: string;
      redoActionMessage: string;
      undoActionMessage: string;
      undoEventLevelActionMessage: string;
      redoEventLevelActionMessage: string;
      undoButtonAction: string;
      redoButtonAction: string;
    };
    location: {
      associatedOrCreatedMessage: string;
      locateCallInProgressMessage: string;
      rejectedOrUnassociatedMessage: string;
    };
    magnitude: {
      azimuthSourceToReceiverMessage: string;
      noStationsSetToDefiningMessage: string;
      setAllStationsNotDefiningMessage: string;
      setAllStationsDefiningMessage: string;
      sourceToReceiverAzimuthMessage: string;
      noAmplitudeMessage: string;
    };
  };
}
export const messageConfig: MessageConfig = {
  // If true, filter out the keys from the history list
  historyFilters: new Map([
    [CacheTypes.UserActionDescription.UPDATE_EVENT_FROM_SIGNAL_DETECTION_CHANGE, true]
  ]),
  tooltipMessages: {
    history: {
      currentHistoryActionMessage: 'The current state displayed',
      redoActionMessage: 'Redo this action',
      undoActionMessage: 'Undo this action',
      undoEventLevelActionMessage: 'Revert event history',
      redoEventLevelActionMessage: 'Restore event history',
      undoButtonAction: 'Undo last action',
      redoButtonAction: 'Redo last undone action'
    },
    location: {
      associatedOrCreatedMessage: 'SD Associated or Created since last locate',
      locateCallInProgressMessage: 'Displays if a locate call is in progress',
      rejectedOrUnassociatedMessage: 'SD Rejected or Unassociated since last locate'
    },
    magnitude: {
      azimuthSourceToReceiverMessage: 'Source-to-Receiver Azimuth (\u00B0)',
      noStationsSetToDefiningMessage:
        'Select at least one defining station to calculate network magnitude',
      setAllStationsNotDefiningMessage: 'Set all stations as not defining',
      setAllStationsDefiningMessage: 'Set all stations as defining',
      sourceToReceiverAzimuthMessage: 'Source to Receiver Azimuth (\u00B0)',
      noAmplitudeMessage: 'Make amplitude measurement to set as defining'
    }
  }
};
