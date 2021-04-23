import React from 'react';
import { QcMaskDisplayFilters } from '~analyst-ui/config';
import { MaskDisplayFilter } from '~analyst-ui/config/user-preferences';
import { QcMaskFilter } from '../../../../../../src/ts/components/analyst-ui/common/dialogs/qc-mask-filter';

// tslint:disable-next-line:no-implicit-dependencies no-var-requires no-require-imports
const Enzyme = require('enzyme');
// set up window alert and open so we don't see errors
window.alert = jest.fn();
window.open = jest.fn();

describe('qc-mask-filter', () => {
  const maskDisplayFilter: MaskDisplayFilter = {
    color: 'string',
    visible: true,
    name: 'string'
  };
  const qcMaskDisplayFilters: QcMaskDisplayFilters = {
    ANALYST_DEFINED: maskDisplayFilter,
    CHANNEL_PROCESSING: maskDisplayFilter,
    DATA_AUTHENTICATION: maskDisplayFilter,
    REJECTED: maskDisplayFilter,
    STATION_SOH: maskDisplayFilter,
    WAVEFORM_QUALITY: maskDisplayFilter
  };
  const wrapper = Enzyme.mount(
    <QcMaskFilter maskDisplayFilters={qcMaskDisplayFilters} setMaskDisplayFilters={jest.fn()} />
  );
  it('is defined', () => {
    expect(QcMaskFilter).toBeDefined();
  });

  it('renders when given correct props', () => {
    expect(wrapper).toMatchSnapshot();
  });

  it('has no rendered state', () => {
    const state = wrapper.state();
    expect(state).toEqual(null);
  });

  it('has the props we passed in', () => {
    const props = wrapper.props();
    expect(props).toMatchSnapshot();
  });

  it('has an onChange function that calls props function that was passed in', () => {
    wrapper.instance().onChange('key', maskDisplayFilter);
    expect(wrapper.props().setMaskDisplayFilters).toHaveBeenCalledWith('key', maskDisplayFilter);
  });
});
