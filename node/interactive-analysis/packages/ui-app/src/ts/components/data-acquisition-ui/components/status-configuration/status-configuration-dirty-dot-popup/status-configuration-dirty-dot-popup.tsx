import { DataAcquisitionTypes } from '@gms/common-graphql';
import React from 'react';

export interface StatusConfigurationDirtyDotPopupProps {
  statusConfiguration: DataAcquisitionTypes.StatusConfiguration;
}

export class StatusConfigurationDirtyDotPopup extends React.Component<
  StatusConfigurationDirtyDotPopupProps,
  {}
> {
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
        {this.props.statusConfiguration.modified ? <div>Station has unsaved changes</div> : null}
      </div>
    );
  }
}
