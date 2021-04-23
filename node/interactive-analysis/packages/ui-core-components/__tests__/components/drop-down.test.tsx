import React from 'react';
import { DropDown } from '../../src/ts/components/ui-widgets/drop-down';
import { DropDownProps } from '../../src/ts/components/ui-widgets/drop-down/types';

// tslint:disable-next-line:no-implicit-dependencies no-var-requires no-require-imports
const Enzyme = require('enzyme');

// set up window alert and open so we don't see errors
(window as any).alert = jest.fn();
(window as any).open = jest.fn();

function flushPromises(): any {
  return new Promise(setImmediate);
}

describe('Core drop down', () => {
  enum TEST_ENUM {
    test = 'test',
    foo = 'foo',
    bar = 'bar'
  }
  let testValue = TEST_ENUM.bar;
  const props: DropDownProps = {
    dropDownItems: TEST_ENUM,
    value: testValue,
    widthPx: 120,
    disabled: false,
    onMaybeValue: value => {
      testValue = value;
    }
  };
  const mockDropdown = Enzyme.shallow(<DropDown {...props} />);

  it('Renders', () => {
    expect(mockDropdown).toMatchSnapshot();
  });
  it('Can have a value set', () => {
    mockDropdown.find('HTMLSelect').prop('onChange')({
      target: { value: TEST_ENUM.foo }
    });
    flushPromises();
    expect(testValue).toEqual(TEST_ENUM.foo);
  });
  it('Can render custom value', () => {
    const props2: DropDownProps = {
      dropDownItems: TEST_ENUM,
      value: testValue,
      widthPx: 120,
      disabled: false,
      onMaybeValue: value => {
        testValue = value;
      },
      custom: true
    };
    const mockDropdownCustom = Enzyme.shallow(<DropDown {...props2} />);
    expect(mockDropdownCustom).toMatchSnapshot();
  });
});
