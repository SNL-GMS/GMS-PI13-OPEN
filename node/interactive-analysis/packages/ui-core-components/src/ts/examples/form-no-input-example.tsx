/**
 * Example of using the form to display values, rather than create an input that
 * can be submitted
 */

import { dateToISOString } from '@gms/common-util';
import React from 'react';
import { Form, FormTypes } from '../components';
/**
 * Example displaying how to use the Table component.
 */
export class FormNoInputExample extends React.Component<{}, {}> {
  public constructor(props: {}) {
    super(props);
  }

  /**
   * React render method
   */
  public render() {
    const mainPanel: FormTypes.FormPanel = {
      formItems: this.makeFormItems(),
      key: 'key1'
    };
    const secondaryPanel: FormTypes.FormPanel = {
      formItems: this.makeSecondaryFormItems(),
      key: 'key2'
    };
    const tertiaryPanel: FormTypes.FormPanel = {
      formItems: this.makeTertiaryFormItems(),
      key: 'key3'
    };
    return (
      <div
        className="ag-dark"
        style={{
          flex: '1 1 auto',
          position: 'relative',
          width: '700px'
        }}
      >
        <Form
          header="Details"
          defaultPanel={mainPanel}
          extraPanels={[secondaryPanel, tertiaryPanel]}
          onCancel={() => {
            alert('Form canceled!');
          }}
          // Important! If submit isn't disabled then you have to provide a callback
          disableSubmit={true}
        />
      </div>
    );
  }
  private makeFormItems(): FormTypes.FormItem[] {
    const display: FormTypes.FormItem = {
      itemKey: 'key1',
      labelText: 'Label One',
      itemType: FormTypes.ItemType.Display,
      displayText: 'Example Value'
    };
    const timeInput: FormTypes.FormItem = {
      itemKey: 'key2',
      labelText: 'Label Two',
      itemType: FormTypes.ItemType.Display,
      displayText: dateToISOString(new Date()),
      displayTextFormat: FormTypes.TextFormats.Time
    };
    const display2: FormTypes.FormItem = {
      itemKey: 'key3',
      labelText: 'Label Three',
      itemType: FormTypes.ItemType.Display,
      hideLabelColon: true,
      displayText: 'Example Display Text'
    };
    return [display, timeInput, display2];
  }

  private makeSecondaryFormItems(): FormTypes.FormItem[] {
    const itemA: FormTypes.FormItem = {
      itemKey: 'keyA',
      labelText: 'Label A',
      itemType: FormTypes.ItemType.Display,
      displayText: 'Alternate Value'
    };
    const itemB: FormTypes.FormItem = {
      itemKey: 'keyB',
      labelText: 'Label B',
      itemType: FormTypes.ItemType.Display,
      // tslint:disable-next-line:no-magic-numbers
      displayText: dateToISOString(new Date(467532000000)),
      displayTextFormat: FormTypes.TextFormats.Time
    };
    const itemC: FormTypes.FormItem = {
      itemKey: 'keyC',
      labelText: 'Label C',
      itemType: FormTypes.ItemType.Display,
      hideLabelColon: true,
      displayText: 'Alternative Display Text'
    };
    return [itemA, itemB, itemC];
  }

  private makeTertiaryFormItems(): FormTypes.FormItem[] {
    const itemD: FormTypes.FormItem = {
      itemKey: 'keyD',
      labelText: 'Item D',
      itemType: FormTypes.ItemType.Display,
      displayText: 'Display Text D'
    };
    const itemE: FormTypes.FormItem = {
      itemKey: 'keyE',
      labelText: 'Item E',
      itemType: FormTypes.ItemType.Display,
      // tslint:disable-next-line:no-magic-numbers
      displayText: dateToISOString(new Date(1535760000000)),
      displayTextFormat: FormTypes.TextFormats.Time
    };
    const itemF: FormTypes.FormItem = {
      itemKey: 'keyF',
      labelText: 'Item F',
      itemType: FormTypes.ItemType.Display,
      hideLabelColon: true,
      displayText: 'Display TextF'
    };
    return [itemD, itemE, itemF];
  }
}
