import {
  dateToHoursMinutesSeconds,
  getSecureRandomNumber,
  MILLISECONDS_IN_SECOND
} from '@gms/common-util';
import React from 'react';
import { SignalDetectionConflict } from '../../types';

export interface EventConflictPopupProps {
  signalDetectionConflicts: SignalDetectionConflict[];
}
/**
 * Displays signal detection information in tabular form
 */
export class EventConflictPopup extends React.Component<EventConflictPopupProps, {}> {
  // ***************************************
  // BEGIN REACT COMPONENT LIFECYCLE METHODS
  // ***************************************

  /**
   * Constructor.
   *
   * @param props The initial props
   */
  public constructor(props) {
    super(props);
  }

  /**
   * Renders the component.
   */
  public render() {
    return (
      <div>
        <div>Signal Detection(s) in conflict:</div>
        <ul>
          {this.props.signalDetectionConflicts.map(sdc => (
            <li
              // tslint:disable-next-line: newline-per-chained-call
              key={sdc.id + String(getSecureRandomNumber())}
            >
              {sdc.phase} on&nbsp;
              {sdc.stationName}&nbsp; at{' '}
              {dateToHoursMinutesSeconds(new Date(sdc.arrivalTime * MILLISECONDS_IN_SECOND))}
            </li>
          ))}
        </ul>
      </div>
    );
  }
}
