import { uniqueId } from 'lodash';
import React from 'react';
import { PercentBarProps } from './types/percent-bar';

/**
 * A simple component for rendering a percent bar.
 */
export class PercentBar extends React.PureComponent<PercentBarProps> {
  private readonly id: string;

  public constructor(props) {
    super(props);
    this.id = uniqueId();
  }

  public render() {
    return (
      <React.Fragment>
        <div
          className="percent-bar"
          key={this.id}
          style={{
            width: `100%`,
            // Parent has overflow hidden, which cuts off the overflow,
            // so translation will only show the appropriate amount of the bar.
            // tslint:disable-next-line: no-magic-numbers
            transform: `translateX(-${100 - this.props.percentage}%)`
          }}
        />
      </React.Fragment>
    );
  }
}
