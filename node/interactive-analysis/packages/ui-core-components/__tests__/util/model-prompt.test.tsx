import * as React from 'react';
import { ModalPrompt } from '../../src/ts/components';
import { PromptProps } from '../../src/ts/components/dialog/types';
// tslint:disable-next-line:no-implicit-dependencies no-var-requires no-require-imports
const Enzyme = require('enzyme');
// tslint:disable-next-line:no-implicit-dependencies no-var-requires no-require-imports
const Adapter = require('enzyme-adapter-react-16');

const props: PromptProps = {
  title: 'Example Title',
  actionText: 'Accept',
  actionTooltipText: 'Accept the prompt',
  cancelText: 'Reject',
  cancelTooltipText: 'Reject the prompt',
  isOpen: true,
  actionCallback: jest.fn(),
  cancelButtonCallback: jest.fn(),
  onCloseCallback: jest.fn()
};

describe('modal prompt tests', () => {
  beforeEach(() => {
    Enzyme.configure({ adapter: new Adapter() });
  });

  const wrapper: any = Enzyme.mount(<ModalPrompt {...props} />);
  it('we pass in basic props', () => {
    const passedInProps = wrapper.props() as PromptProps;
    expect(passedInProps).toMatchSnapshot();
  });
  it('renders', () => {
    expect(wrapper.render()).toMatchSnapshot();
  });
  it('renders children', () => {
    const wrapperWithKids: any = Enzyme.mount(
      <ModalPrompt {...props}>
        <div>Sample Children</div>
      </ModalPrompt>
    );
    expect(wrapperWithKids.render()).toMatchSnapshot();
  });
});
