import { SignalDetectionTypes } from '@gms/common-graphql';
import React from 'react';

export interface SDDirtyDotPopupProps {
  signalDetection: SignalDetectionTypes.SignalDetection;
}
/**
 * Displays signal detection information in tabular form
 */
export class SDDirtyDotPopup extends React.Component<SDDirtyDotPopupProps, {}> {
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
        {this.props.signalDetection.modified ? (
          <div>Signal Detection has unsaved changes</div>
        ) : null}
      </div>
    );
  }
}
