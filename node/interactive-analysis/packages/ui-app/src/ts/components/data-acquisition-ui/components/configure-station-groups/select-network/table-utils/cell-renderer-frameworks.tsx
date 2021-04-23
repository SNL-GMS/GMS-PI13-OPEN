import { Tooltip } from '@blueprintjs/core';
import React from 'react';
import { gmsColors } from '~scss-config/color-preferences';

/**
 * Renders the modified dot.
 */
// tslint:disable-next-line:max-classes-per-file
export class NetworkModifiedDot extends React.Component<any, {}> {
  public constructor(props) {
    super(props);
  }

  /**
   * react component lifecycle
   */
  public render() {
    return (
      <Tooltip className="dirty-dot-wrapper">
        <div
          style={{
            backgroundColor: this.props.data.modified ? gmsColors.gmsMain : 'transparent'
          }}
          className="list-entry-dirty-dot"
        />
      </Tooltip>
    );
  }
}
