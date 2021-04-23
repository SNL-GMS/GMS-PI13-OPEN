import { CommonTypes, EventTypes } from '@gms/common-graphql';
import React from 'react';
import {
  StationMagnitude
  // tslint:disable-next-line: max-line-length
} from '../../../../../src/ts/components/analyst-ui/components/magnitude/components/station-magnitude/station-magnitude-component';
import {
  StationMagnitudeProps,
  StationMagnitudeSdData,
  StationMagnitudeState
} from '../../../../../src/ts/components/analyst-ui/components/magnitude/components/station-magnitude/types';
import { testMagTypes } from '../../../../__data__/test-util';

// tslint:disable-next-line:no-implicit-dependencies no-var-requires no-require-imports
const Enzyme = require('enzyme');

// set up window alert and open so we don't see errors
(window as any).alert = jest.fn();
(window as any).open = jest.fn();

const locationSolution: EventTypes.LocationSolution = {
  id: '123',
  locationType: 'type',
  locationToStationDistances: [
    {
      azimuth: 1,
      distance: {
        degrees: 1,
        km: 2
      },
      stationId: '1'
    }
  ],
  location: {
    latitudeDegrees: 1,
    longitudeDegrees: 1,
    depthKm: 1,
    time: 1
  },
  featurePredictions: undefined,
  locationRestraint: undefined,
  locationBehaviors: undefined,
  networkMagnitudeSolutions: [
    {
      uncertainty: 1,
      magnitudeType: EventTypes.MagnitudeType.MB,
      magnitude: 1,
      networkMagnitudeBehaviors: []
    },
    {
      uncertainty: 1,
      magnitudeType: EventTypes.MagnitudeType.MS,
      magnitude: 1,
      networkMagnitudeBehaviors: []
    },
    {
      uncertainty: 1,
      magnitudeType: EventTypes.MagnitudeType.MBMLE,
      magnitude: 1,
      networkMagnitudeBehaviors: []
    },
    {
      uncertainty: 1,
      magnitudeType: EventTypes.MagnitudeType.MSMLE,
      magnitude: 1,
      networkMagnitudeBehaviors: []
    }
  ],
  snapshots: undefined
};

function flushPromises(): any {
  return new Promise(setImmediate);
}

describe('when loading station Magnitude', () => {
  const validSignalDetectionForMagnitude = new Map<any, boolean>([
    [EventTypes.MagnitudeType.MB, true]
  ]);
  const magTypeToAmplitudeMap = new Map<any, StationMagnitudeSdData>([
    [
      EventTypes.MagnitudeType.MB,
      {
        channel: 'bar',
        phase: CommonTypes.PhaseType.P,
        amplitudeValue: 1,
        amplitudePeriod: 1,
        signalDetectionId: '1',
        time: 1,
        stationName: '1',
        flagForReview: false
      }
    ]
  ]);

  const lazyloadStub: StationMagnitudeProps = {
    checkBoxCallback: jest.fn(),
    setSelectedSdIds: jest.fn(),
    selectedSdIds: ['123'],
    historicalMode: false,
    locationSolution,
    computeNetworkMagnitudeSolution: undefined,
    displayedMagnitudeTypes: testMagTypes,
    amplitudesByStation: [
      {
        stationName: 'foo',
        validSignalDetectionForMagnitude,
        magTypeToAmplitudeMap
      }
    ],
    openEventId: ''
  };
  const wrapper = Enzyme.mount(<StationMagnitude {...lazyloadStub} />);

  xit('Network station magnitude table renders', () => {
    expect(wrapper).toMatchSnapshot();
  });

  xit('we populate props', () => {
    const props = wrapper.props() as StationMagnitudeProps;
    expect(props).toMatchSnapshot();
  });

  xit('we have null initial state', () => {
    const state = wrapper.state() as StationMagnitudeState;
    expect(state).toMatchSnapshot();
  });

  xit('we have a onCellClicked function click handler for selecting mb and ms rows', () => {
    const instance = wrapper.instance();
    const spy = jest.spyOn(instance, 'onCellClicked');
    instance.mainTable = {};
    instance.mainTable.getSelectedNodes = jest.fn();
    instance.mainTable.getRenderedNodes = jest.fn(() => [
      { data: { mbSignalDetectionId: '123' }, rowIndex: 1 }
    ]);
    // null call
    instance.onCellClicked(null);
    flushPromises();
    expect(spy).toHaveBeenCalledWith(null);

    // empty call
    instance.onCellClicked({});
    flushPromises();
    expect(spy).toHaveBeenCalledWith({});

    // call with good args
    const defaultClickSpy = jest.spyOn(instance, 'onDefaultClick');
    instance.onCellClicked({
      node: undefined,
      event: {},
      data: { mbSignalDetectionIdevent: '123' },
      column: { colId: 'mbChannel' }
    });
    flushPromises();
    expect(defaultClickSpy).toHaveBeenCalled();
    expect(spy).toHaveBeenCalledWith({
      node: undefined,
      event: {},
      data: { mbSignalDetectionIdevent: '123' },
      column: { colId: 'mbChannel' }
    });

    // called with shiftkey
    const shiftClickSpy = jest.spyOn(instance, 'onShiftClick');
    instance.onCellClicked({
      node: { rowIndex: 1 },
      event: { shiftKey: true },
      data: { mbSignalDetectionIdevent: '123' },
      column: { colId: 'mbChannel' }
    });
    flushPromises();
    expect(shiftClickSpy).toHaveBeenCalled();
    expect(spy).toHaveBeenCalledWith({
      node: { rowIndex: 1 },
      event: { shiftKey: true },
      data: { mbSignalDetectionIdevent: '123' },
      column: { colId: 'mbChannel' }
    });

    // called with ctrlKey/metaKey
    const onControlClickSpy = jest.spyOn(instance, 'onControlClick');
    instance.onCellClicked({
      event: { ctrlKey: true },
      data: { mbSignalDetectionIdevent: '123' },
      column: { colId: 'mbChannel' }
    });
    flushPromises();
    expect(onControlClickSpy).toHaveBeenCalled();
    expect(spy).toHaveBeenCalledWith({
      event: { ctrlKey: true },
      data: { mbSignalDetectionIdevent: '123' },
      column: { colId: 'mbChannel' }
    });
  });

  xit('we can getSelectedMbRowIndices', () => {
    const instance = wrapper.instance();
    const spy = jest.spyOn(instance, 'getSelectedMbRowIndices');
    instance.mainTable = {};
    const node = { data: { mbSignalDetectionId: '123' }, rowIndex: 1 };
    instance.mainTable.getRenderedNodes = jest.fn(() => [node]);
    const result = instance.getSelectedMbRowIndices();
    flushPromises();
    expect(result).toEqual([1]);
    expect(spy).toHaveBeenCalled();
  });

  xit('we can getSelectedMsRowIndices', () => {
    const instance = wrapper.instance();
    const spy = jest.spyOn(instance, 'getSelectedMsRowIndices');
    instance.mainTable = {};
    const node = { data: { msSignalDetectionId: '123' }, rowIndex: 1 };
    instance.mainTable.getRenderedNodes = jest.fn(() => [node]);
    const result = instance.getSelectedMsRowIndices();
    flushPromises();
    expect(result).toEqual([1]);
    expect(spy).toHaveBeenCalled();
  });
});
