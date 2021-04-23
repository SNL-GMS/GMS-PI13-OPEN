import {
  makeLagSortingDropdown,
  makeMissingSortingDropdown,
  makeStationLabel,
  SOHLagOptions,
  SOHMissingOptions
} from '../../../../../src/ts/components/data-acquisition-ui/shared/toolbars/soh-toolbar-items';
// tslint:disable-next-line:no-implicit-dependencies no-var-requires no-require-imports
// const Enzyme = require('enzyme');

// set up window alert and open so we don't see errors
(window as any).alert = jest.fn();
(window as any).open = jest.fn();

describe('Soh toolbar items', () => {
  it('should be defined', () => {
    expect(SOHLagOptions).toBeDefined();
    expect(SOHMissingOptions).toBeDefined();
    expect(makeStationLabel).toBeDefined();
    expect(makeLagSortingDropdown).toBeDefined();
    expect(makeMissingSortingDropdown).toBeDefined();
  });

  it('function makeStationLabel should create a label', () => {
    expect(makeStationLabel('my station name')).toMatchSnapshot();
  });

  it('function makeStationLabel should create a label', () => {
    expect(makeLagSortingDropdown(SOHLagOptions.CHANNEL_FIRST, jest.fn())).toMatchSnapshot();
  });

  it('function makeStationLabel should create a label', () => {
    expect(
      makeMissingSortingDropdown(SOHMissingOptions.CHANNEL_FIRST, jest.fn())
    ).toMatchSnapshot();
  });
});
