import { ReactWrapper } from 'enzyme';
import React from 'react';
import {
  WorkflowTimeAxis,
  WorkflowTimeAxisProps
} from '../../../../../src/ts/components/analyst-ui/components/workflow/components/workflow-time-axis';

// set up window alert and open so we don't see errors
window.alert = jest.fn();
window.open = jest.fn();

// tslint:disable-next-line:no-implicit-dependencies no-var-requires no-require-imports
const Enzyme = require('enzyme');
// tslint:disable-next-line:no-implicit-dependencies no-var-requires no-require-imports
const Adapter = require('enzyme-adapter-react-16');

const mockWorkflowTimeAxisProps: WorkflowTimeAxisProps = {
  startTimeSecs: 3000,
  endTimeSecs: 3600,
  intervalDurationSecs: 60,
  pixelsPerHour: 5
};

describe('workflow time axis tests', () => {
  beforeEach(() => {
    Enzyme.configure({ adapter: new Adapter() });
  });

  // Not the most useful test because the d3 does not render
  it('renders a snapshot', (done: jest.DoneCallback) => {
    const wrapper: ReactWrapper = Enzyme.mount(<WorkflowTimeAxis {...mockWorkflowTimeAxisProps} />);

    setImmediate(() => {
      wrapper.update();
      expect(wrapper.find(WorkflowTimeAxis)).toMatchSnapshot('component');

      done();
    });
  });
});
