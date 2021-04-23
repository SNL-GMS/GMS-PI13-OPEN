import React from 'react';
import { CheckboxList } from '../../../../src/ts/components/ui-widgets/checkbox-list';

// tslint:disable-next-line:no-implicit-dependencies no-var-requires no-require-imports
const Enzyme = require('enzyme');

// set up window alert and open so we don't see errors
(window as any).alert = jest.fn();
(window as any).open = jest.fn();

describe('checkbox dropdown', () => {
  it('exists', () => {
    expect(CheckboxList).toBeDefined();
  });
  enum mockBoxEnum {
    firstBox = '1st',
    secondBox = '2nd',
    thirdBox = '3rd'
  }
  const mockEnumToCheckedMap = new Map([
    [mockBoxEnum.firstBox, false],
    [mockBoxEnum.secondBox, true],
    [mockBoxEnum.thirdBox, false]
  ]);
  const mockKeysToDisplayStrings = new Map([
    [mockBoxEnum.firstBox, 'The first checkbox'],
    [mockBoxEnum.secondBox, 'The second checkbox'],
    [mockBoxEnum.thirdBox, 'The third checkbox']
  ]);
  const mockColorMap = new Map([
    ['firstBox', '#123123'],
    ['secondBox', '#ABC123'],
    ['thirdBox', '#000000']
  ]);
  const mockOnChange = jest.fn() as jest.Mock<Map<any, boolean>>;
  const checkboxProps = {
    checkboxEnum: mockBoxEnum,
    enumToCheckedMap: mockEnumToCheckedMap,
    enumKeysToDisplayStrings: mockKeysToDisplayStrings,
    enumToColorMap: mockColorMap,
    onChange: mockOnChange
  };
  const mockBox = Enzyme.shallow(<CheckboxList {...checkboxProps} />);

  it('matches the snapshot', () => {
    expect(mockBox).toMatchSnapshot();
  });

  it('updates state onChange', () => {
    const instance = mockBox.instance();
    const onChange = instance.onChange;
    expect(instance.state.enumToCheckedMap.get(mockBoxEnum.thirdBox)).toEqual(false);
    onChange('thirdBox');
    expect(instance.state.enumToCheckedMap.get(mockBoxEnum.thirdBox)).toEqual(true);
  });
});
