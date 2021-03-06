import { WorkflowTypes } from '@gms/common-graphql';
import { ActivityIntervalBlueprintContextMenu } from '../../../../../../src/ts/components/analyst-ui/components/workflow/components';

// tslint:disable-next-line:no-implicit-dependencies no-var-requires no-require-imports
const Enzyme = require('enzyme');
// tslint:disable-next-line:no-implicit-dependencies no-var-requires no-require-imports
const Adapter = require('enzyme-adapter-react-16');

// set up window alert and open so we don't see errors
window.alert = jest.fn();
window.open = jest.fn();

describe('workflow context menu activity interval tests', () => {
  beforeEach(() => {
    Enzyme.configure({ adapter: new Adapter() });
  });

  it('can open activity interval on click', () => {
    const fn = jest.fn();

    const wrapper = Enzyme.shallow(ActivityIntervalBlueprintContextMenu(fn));

    wrapper.find('.menu-item-open-activity-interval').simulate('click');

    expect(fn).toHaveBeenCalledWith(WorkflowTypes.IntervalStatus.InProgress);
  });

  it('can mark complete activity interval on click', () => {
    const fn = jest.fn();

    const wrapper = Enzyme.shallow(ActivityIntervalBlueprintContextMenu(fn));

    wrapper.find('.menu-item-mark-activity-interval').simulate('click');

    expect(fn).toHaveBeenCalledWith(WorkflowTypes.IntervalStatus.Complete);
  });

  it('can mark incomplete activity interval on click', () => {
    const fn = jest.fn();

    const wrapper = Enzyme.shallow(ActivityIntervalBlueprintContextMenu(fn));

    wrapper.find('.menu-item-unmark-activity-interval').simulate('click');

    expect(fn).toHaveBeenCalledWith(WorkflowTypes.IntervalStatus.NotComplete);
  });
});
