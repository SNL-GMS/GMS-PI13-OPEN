import { uuid } from '@gms/common-util';
import { createStore } from '@gms/ui-state';
import * as React from 'react';
import { withApolloReduxProvider } from '../../../../src/ts/app/apollo-redux-provider';
import { wrapSystemMessageSubscription } from '../../../../src/ts/components/common-ui/components/system-message/system-message-subscription';

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

describe('System Message Component', () => {
  it('should be defined', () => {
    expect(wrapSystemMessageSubscription).toBeDefined();
  });

  const store: any = createStore();

  const Wrapper = withApolloReduxProvider(wrapSystemMessageSubscription(React.Fragment, {}), store);

  const systemMessageDisplay: any = Enzyme.mount(<Wrapper />);

  it('matches snapshot', () => {
    expect(systemMessageDisplay).toMatchSnapshot();
  });
});
