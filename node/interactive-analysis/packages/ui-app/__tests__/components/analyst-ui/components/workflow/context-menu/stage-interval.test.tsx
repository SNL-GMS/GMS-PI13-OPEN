import { WorkflowTypes } from '@gms/common-graphql';
import { StageIntervalBlueprintContextMenu } from '../../../../../../src/ts/components/analyst-ui/components/workflow/components';

// tslint:disable-next-line:no-implicit-dependencies no-var-requires no-require-imports
const Enzyme = require('enzyme');
// tslint:disable-next-line:no-implicit-dependencies no-var-requires no-require-imports
const Adapter = require('enzyme-adapter-react-16');
// set up window alert and open so we don't see errors
window.alert = jest.fn();
window.open = jest.fn();
describe('workflow context menu stage interval tests', () => {
  beforeEach(() => {
    Enzyme.configure({ adapter: new Adapter() });
  });

  it('can mark stage interval on click', () => {
    const fn = jest.fn();

    const wrapper = Enzyme.shallow(StageIntervalBlueprintContextMenu(fn));

    wrapper.find('.menu-item-mark-stage-interval').simulate('click');

    expect(fn).toHaveBeenCalledWith(WorkflowTypes.IntervalStatus.Complete);
  });
});
