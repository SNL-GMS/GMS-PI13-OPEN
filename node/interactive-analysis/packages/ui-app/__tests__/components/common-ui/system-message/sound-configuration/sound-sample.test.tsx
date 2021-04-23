import * as React from 'react';

import { SoundSample } from '../../../../../src/ts/components/common-ui/components/system-message/sound-configuration/sound-sample';

// tslint:disable-next-line:no-implicit-dependencies no-var-requires no-require-imports
const Enzyme = require('enzyme');

// set up window alert and open so we don't see errors
(window as any).alert = jest.fn();
(window as any).open = jest.fn();

describe('Sound Sample Component', () => {
  const soundSample = Enzyme.mount(<SoundSample soundToPlay={'test.mp3'} />);

  it('should be defined', () => {
    expect(SoundSample).toBeDefined();
    expect(soundSample).toBeDefined();
  });

  it('matches snapshot', () => {
    soundSample.update();
    expect(soundSample).toMatchSnapshot();
  });

  const noneSound = Enzyme.mount(<SoundSample soundToPlay={'None'} />);

  it("validates 'None' sound", () => {
    noneSound.update();
    expect(noneSound).toMatchSnapshot();
  });
});
