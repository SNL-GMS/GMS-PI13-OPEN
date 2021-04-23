import { uuid } from '@gms/common-util';
import * as React from 'react';
import { NewMessageIndicator } from '../../../../src/ts/components/common-ui/components/system-message/new-message-indicator';
import { NewMessageIndicatorProps } from '../../../../src/ts/components/common-ui/components/system-message/types';

// tslint:disable-next-line:no-implicit-dependencies no-var-requires no-require-imports
const Enzyme = require('enzyme');

// set up window alert and open so we don't see errors
(window as any).alert = jest.fn();
(window as any).open = jest.fn();

// tslint:disable-next-line: deprecation
const lodash = require.requireActual('lodash');
lodash.uniqueId = () => '1';

let idCount = 0;
uuid.asString = jest.fn().mockImplementation(() => ++idCount);

describe('New Messages Indicator', () => {
  it('is defined', () => {
    expect(NewMessageIndicator).toBeDefined();
  });

  const props: NewMessageIndicatorProps = {
    isVisible: true,
    handleNewMessageIndicatorClick: jest.fn()
  };

  const wrapper = Enzyme.mount(<NewMessageIndicator {...props} />);

  it('matches its snapshot', () => {
    expect(wrapper).toMatchSnapshot();
  });

  it('scrolls to latest on click', () => {
    wrapper
      .find('[data-cy="new-messages-button"]')
      .first()
      .simulate('click');
    // tslint:disable-next-line: no-unbound-method
    expect(props.handleNewMessageIndicatorClick).toHaveBeenCalledTimes(1);
  });
});
