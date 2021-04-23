/**
 * Example of using the form that actually accepts input
 */

import React from 'react';
import { DropDown } from '../components';

enum ExampleEnum {
  ExampleValue1 = 'Example Value One',
  ExampleValue2 = 'Example Value Two',
  ExampleValue3 = 'Example Value Three'
}

interface DropDownExampleState {
  selected: string;
}
/**
 * Example displaying how to use the Table component.
 */
export class DropDownExample extends React.Component<{}, DropDownExampleState> {
  public constructor(props: {}) {
    super(props);
    this.state = { selected: ExampleEnum.ExampleValue1 };
  }

  /**
   * React render method
   */
  public render() {
    return (
      <div
        className="ag-dark"
        style={{
          flex: '1 1 auto',
          position: 'relative',
          width: '700px'
        }}
      >
        <DropDown
          onMaybeValue={this.onSubmit}
          value={this.state.selected}
          dropDownItems={ExampleEnum}
        />
        <div>{`Selected: ${this.state.selected}`}</div>
      </div>
    );
  }

  private readonly onSubmit = (dropDownValue: any) => {
    this.setState({ selected: dropDownValue });
  }
}
