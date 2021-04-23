import { SignalDetectionTypes } from '@gms/common-graphql';
import { dateToHoursMinutesSeconds, MILLISECONDS_IN_SECOND } from '@gms/common-util';
import sortBy from 'lodash/sortBy';
import React from 'react';

export interface SDConflictPopupProps {
  sdConflicts: SignalDetectionTypes.ConflictingSdHypData[];
}
/**
 * Displays signal detection information in tabular form
 */
export class SDConflictPopup extends React.Component<SDConflictPopupProps, {}> {
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
    const orderedConflicts = sortBy(this.props.sdConflicts, e => e.eventTime);
    return (
      <div>
        <div>Conflicts exist (event time, sd station , sd phase, sd time):</div>
        <ul>
          {orderedConflicts.map(conflict => {
            const eventTime = conflict.eventTime
              ? dateToHoursMinutesSeconds(new Date(conflict.eventTime * MILLISECONDS_IN_SECOND))
              : '';
            const sdTime = dateToHoursMinutesSeconds(
              new Date(conflict.arrivalTime * MILLISECONDS_IN_SECOND)
            );
            return (
              <li key={conflict.eventId}>
                {`${eventTime}, ${conflict.stationName}, ${conflict.phase}, ${sdTime}`}
              </li>
            );
          })}
        </ul>
      </div>
    );
  }
}
