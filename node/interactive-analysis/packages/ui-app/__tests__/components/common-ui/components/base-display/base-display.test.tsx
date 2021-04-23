import * as React from 'react';
import { BaseDisplay } from '../../../../../src/ts/components/common-ui/components/base-display';

// tslint:disable-next-line:no-implicit-dependencies no-var-requires no-require-imports
const Enzyme = require('enzyme');

// set up window alert and open so we don't see errors
(window as any).alert = jest.fn();
(window as any).open = jest.fn();

// tslint:disable-next-line: deprecation
const lodash = require.requireActual('lodash');
lodash.uniqueId = () => '1';

describe('System Messages Display', () => {
  it('should be defined', () => {
    expect(BaseDisplay).toBeDefined();
  });
  const base: any = Enzyme.mount(
    <BaseDisplay
      glContainer={
        {
          widthPx: 150,
          heightPx: 150,
          on: jest.fn()
        } as any
      }
      className="mock-display"
    />
  );

  it('matches snapshot', () => {
    expect(base).toMatchSnapshot();
  });
});
