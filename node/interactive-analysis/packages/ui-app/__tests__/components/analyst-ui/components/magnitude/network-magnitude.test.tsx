import { uuid } from '@gms/common-util';
import React from 'react';
import {
  NetworkMagnitude
  // tslint:disable-next-line: max-line-length
} from '../../../../../src/ts/components/analyst-ui/components/magnitude/components/network-magnitude/network-magnitude-component';
import {
  NetworkMagnitudeProps,
  NetworkMagnitudeState
} from '../../../../../src/ts/components/analyst-ui/components/magnitude/components/network-magnitude/types';
import { eventData } from '../../../../__data__/event-data';
import { testMagTypes } from '../../../../__data__/test-util';

// tslint:disable-next-line:no-implicit-dependencies no-var-requires no-require-imports
const Enzyme = require('enzyme');

// set up window alert and open so we don't see errors
(window as any).alert = jest.fn();
(window as any).open = jest.fn();

function flushPromises(): any {
  return new Promise(setImmediate);
}

describe('when loading a bad marking the UnrecognizedMarkingWarning', () => {
  uuid.asString = jest.fn().mockReturnValue('1e872474-b19f-4325-9350-e217a6feddc0');
  const wrapper: any = Enzyme.mount(
    <NetworkMagnitude
      locationSolutionSet={eventData.currentEventHypothesis.eventHypothesis.locationSolutionSets[0]}
      computeNetworkMagnitudeSolution={undefined}
      preferredSolutionId={undefined}
      selectedSolutionId={undefined}
      displayedMagnitudeTypes={testMagTypes}
      setSelectedLocationSolution={jest.fn()}
    />
  );

  it('we pass in basic props', () => {
    const props = wrapper.props() as NetworkMagnitudeProps;
    expect(props).toMatchSnapshot();
  });

  it('we have a function for clicking on rows', () => {
    const instance = wrapper.instance();
    const spy = jest.spyOn(instance, 'onRowClicked');
    instance.mainTable = {};
    instance.mainTable.getSelectedNodes = jest.fn();
    instance.onRowClicked({});
    flushPromises();
    expect(spy).toHaveBeenCalledWith({});
    instance.mainTable = null;
    instance.onRowClicked();
    flushPromises();
    expect(spy).toHaveBeenCalledWith();
  });

  it('Select rows in the table based on the selected location solution id in the properties', () => {
    const instance = wrapper.instance();
    const spy = jest.spyOn(instance, 'selectRowsFromProps');
    instance.mainTable = {
      getSelectedNodes: jest.fn(),
      deselectAll: jest.fn(),
      ensureNodeVisible: jest.fn(),
      forEachNode: jest.fn(func => {
        func({ setSelected: jest.fn(), data: { id: '123' } });
      })
    };
    instance.selectRowsFromProps({
      locationSolutionSet: eventData.currentEventHypothesis.eventHypothesis.locationSolutionSets[0],
      computeNetworkMagnitudeSolution: undefined,
      preferredSolutionId: undefined,
      selectedSolutionId: undefined,
      setSelectedLocationSolution: jest.fn()
    });
    flushPromises();
    expect(spy).toHaveBeenCalled();
    instance.selectRowsFromProps({
      locationSolutionSet: eventData.currentEventHypothesis.eventHypothesis.locationSolutionSets[0],
      computeNetworkMagnitudeSolution: undefined,
      preferredSolutionId: undefined,
      selectedSolutionId: '123',
      setSelectedLocationSolution: jest.fn()
    });
    flushPromises();
    expect(spy).toHaveBeenCalled();
  });

  it('we have null initial state', () => {
    const state = wrapper.state() as NetworkMagnitudeState;
    expect(state).toMatchSnapshot();
  });

  it('Network magnitude table renders', () => {
    expect(wrapper.render()).toMatchSnapshot();
    expect(uuid.asString).toHaveBeenCalled();
  });

  // tslint:disable-next-line: max-line-length
  it('call component will update when changing selectedSolutionId and empty locationSolutionSet will render null', () => {
    const instance = wrapper.instance();
    const spy = jest.spyOn(instance, 'componentDidUpdate');
    wrapper.setProps({
      selectedSolutionId: 'id101',
      locationSolutionSet: undefined
    });
    expect(spy).toHaveBeenCalled();
    expect(wrapper.props().selectedSolutionId).toEqual('id101');
    expect(wrapper.props().locationSolutionSet).toEqual(undefined);
  });
});
