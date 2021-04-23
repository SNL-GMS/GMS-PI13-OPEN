// import React from 'react';
import { GMSTheme } from '../../../src/ts/components/charts/victory-themes';

// tslint:disable-next-line:no-implicit-dependencies no-var-requires no-require-imports
// const Enzyme = require('enzyme');

// set up window alert and open so we don't see errors
(window as any).alert = jest.fn();
(window as any).open = jest.fn();

describe('GMSTheme', () => {
  // const mockResizerContainer = Enzyme.shallow(
  //   <ResizeContainer>
  //     <div />
  //   </ResizeContainer>
  // );
  it('is exported', () => {
    expect(GMSTheme).toBeDefined();
  });
  it('has values', () => {
    expect(GMSTheme).toMatchSnapshot();
  });
});
