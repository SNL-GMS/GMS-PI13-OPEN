import { AnalystWorkspaceTypes } from '@gms/ui-state';
import Immutable from 'immutable';
import React from 'react';
import {
  SignalDetectionContextMenu,
  SignalDetectionContextMenuProps
} from '../../../../../src/ts/components/analyst-ui/common/context-menus/signal-detection-context-menu';
import { signalDetectionsData } from '../../../../__data__/signal-detections-data';

// tslint:disable-next-line:no-implicit-dependencies no-var-requires no-require-imports
const Enzyme = require('enzyme');

function flushPromises(): any {
  return new Promise(setImmediate);
}

// set up window alert and open so we don't see errors
window.alert = jest.fn();
window.open = jest.fn();
/**
 * Tests the signal detection context menu component
 */
describe('signal-detection-context-menu', () => {
  // tslint:disable-next-line: one-variable-per-declaration
  const props: SignalDetectionContextMenuProps = {
    signalDetections: signalDetectionsData,
    selectedSds: [signalDetectionsData[0]],
    sdIdsToShowFk: [],
    currentOpenEvent: null,
    changeAssociation: jest.fn(),
    associateToNewEvent: jest.fn(),
    rejectDetections: jest.fn(),
    updateDetections: jest.fn(),
    measurementMode: {
      mode: AnalystWorkspaceTypes.WaveformDisplayMode.DEFAULT,
      entries: Immutable.Map<string, boolean>().set(signalDetectionsData[0].id, true)
    },
    setSelectedSdIds: jest.fn(),
    setSdIdsToShowFk: jest.fn(),
    setMeasurementModeEntries: jest.fn()
  };
  const wrapper: any = Enzyme.mount(
    <SignalDetectionContextMenu
      signalDetections={props.signalDetections}
      selectedSds={props.selectedSds}
      sdIdsToShowFk={props.sdIdsToShowFk}
      currentOpenEvent={props.currentOpenEvent}
      changeAssociation={props.changeAssociation}
      associateToNewEvent={props.associateToNewEvent}
      rejectDetections={props.rejectDetections}
      updateDetections={props.updateDetections}
      measurementMode={props.measurementMode}
      // tslint:disable-next-line: no-unbound-method
      setSelectedSdIds={props.setSelectedSdIds}
      // tslint:disable-next-line: no-unbound-method
      setSdIdsToShowFk={props.setSdIdsToShowFk}
      // tslint:disable-next-line: no-unbound-method
      setMeasurementModeEntries={props.setMeasurementModeEntries}
    />
  );

  it('should have a consistent snapshot on mount', () => {
    expect(wrapper.render()).toMatchSnapshot();
    flushPromises();
  });

  it('should have basic props built on render', () => {
    const buildProps = wrapper.props() as SignalDetectionContextMenuProps;
    expect(buildProps).toMatchSnapshot();
  });

  it('should have a function for checking if we can generate fk', () => {
    const instance = wrapper.instance();
    const spy = jest.spyOn(instance, 'canGenerateFk');
    expect(instance.canGenerateFk(signalDetectionsData[0])).toMatchSnapshot();
    flushPromises();
    expect(spy).toHaveBeenCalledWith(signalDetectionsData[0]);
  });

  it('should have a function for setSdIdsToShowFk', () => {
    const instance = wrapper.instance();
    const spy = jest.spyOn(instance, 'setSdIdsToShowFk');
    instance.setSdIdsToShowFk();
    flushPromises();
    expect(spy).toHaveBeenCalled();
    // if we call setSdIdsToShowFk it should call set selected sd ids
    // tslint:disable-next-line: no-unbound-method
    expect(props.setSelectedSdIds).toHaveBeenCalledWith([signalDetectionsData[0].id]);
  });
});
