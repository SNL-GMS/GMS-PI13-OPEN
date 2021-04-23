import * as React from 'react';

import { SoundConfigurationToolbar } from '../../../../../src/ts/components/common-ui/components/system-message/sound-configuration/sound-configuration-toolbar';
import {
  ALL_CATEGORIES,
  ALL_SEVERITIES,
  ALL_SUBCATEGORIES
} from '../../../../../src/ts/components/common-ui/components/system-message/sound-configuration/types';
import { systemMessageDefinitions } from '../../../../__data__/common-ui/system-message-definition-data';
// tslint:disable-next-line:no-implicit-dependencies no-var-requires no-require-imports
const Enzyme = require('enzyme');

// set up window alert and open so we don't see errors
(window as any).alert = jest.fn();
(window as any).open = jest.fn();

describe('Sound Configuration Toolbar', () => {
  const soundConfiguration = Enzyme.mount(
    <SoundConfigurationToolbar
      systemMessageDefinitions={systemMessageDefinitions}
      selectedOptions={{
        selectedSeverity: ALL_SEVERITIES,
        selectedCategory: ALL_CATEGORIES,
        selectedSubcategory: ALL_SUBCATEGORIES
      }}
      onChanged={jest.fn()}
    />
  );

  it('should be defined', () => {
    expect(SoundConfigurationToolbar).toBeDefined();
    expect(soundConfiguration).toBeDefined();
  });

  it('matches snapshot', () => {
    soundConfiguration.update();
    expect(soundConfiguration).toMatchSnapshot();
  });
});
