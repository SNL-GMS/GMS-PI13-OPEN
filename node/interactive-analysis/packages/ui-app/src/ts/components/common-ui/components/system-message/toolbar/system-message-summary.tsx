import { SystemMessageTypes } from '@gms/common-graphql';
import { classList } from '@gms/ui-util';
import * as React from 'react';
import { SystemMessageSummaryProps } from '../types';

const NUM_DIGITS_TO_DISPLAY = 4;

/**
 * Filters the provided system messages by severity
 * @param systemMessages the system messages to filter
 * @param severity the severity to filter
 */
const filterBySeverity = (
  systemMessages: SystemMessageTypes.SystemMessage[],
  severity: SystemMessageTypes.SystemMessageSeverity
) => (systemMessages ? systemMessages.filter(msg => msg.severity === severity) : []);

interface PrefixedDisplayNumberProps {
  className?: string;
  digits: number;
  value: number;
}

const PrefixedDisplayNumber: React.FunctionComponent<PrefixedDisplayNumberProps> = ({
  digits,
  value,
  className
}) => {
  const prefixLength = digits - value.toString().length;

  return (
    <span className={`system-message-summary__count ${className ?? ''}`}>
      {prefixLength > 0 && (
        <span className="system-message-summary__count--prefix">
          {new Array(prefixLength).fill(0)}
        </span>
      )}
      <span className="system-message-summary__count--main">{value}</span>
    </span>
  );
};

export interface SummaryEntryProps {
  severity: SystemMessageTypes.SystemMessageSeverity;
  value: number;
  isShown: boolean;
  toggleFilter(severity: SystemMessageTypes.SystemMessageSeverity): void;
}

const SummaryEntry: React.FunctionComponent<SummaryEntryProps> = ({
  severity,
  value,
  isShown,
  toggleFilter
}) => (
  <span
    className={classList(
      {
        'system-message-summary__entry--disabled': !isShown
      },
      'system-message-summary__entry'
    )}
    data-severity={severity}
    onClick={e => toggleFilter(severity)}
  >
    {severity}:
    <PrefixedDisplayNumber data-severity={severity} digits={NUM_DIGITS_TO_DISPLAY} value={value} />
  </span>
);

export const SystemMessageSummary: React.FunctionComponent<SystemMessageSummaryProps> = props => (
  <div className="system-message-summary">
    {Object.keys(SystemMessageTypes.SystemMessageSeverity).map(
      (messageSeverity: SystemMessageTypes.SystemMessageSeverity) => (
        <SummaryEntry
          key={messageSeverity}
          severity={messageSeverity}
          value={filterBySeverity(props.systemMessages, messageSeverity).length}
          toggleFilter={(s: string) => {
            if (props.severityFilterMap) {
              const newMap = props.severityFilterMap?.set(
                SystemMessageTypes.SystemMessageSeverity[s],
                !props.severityFilterMap?.get(SystemMessageTypes.SystemMessageSeverity[s])
              );
              props.setSeverityFilterMap(newMap);
            }
          }}
          isShown={props.severityFilterMap?.get(messageSeverity) ?? false}
        />
      )
    )}
  </div>
);
