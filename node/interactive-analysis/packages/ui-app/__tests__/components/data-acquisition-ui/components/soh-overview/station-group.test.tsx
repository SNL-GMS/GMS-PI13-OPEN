import { ContextMenu } from '@blueprintjs/core';
import { SohTypes } from '@gms/common-graphql';
import { uuid } from '@gms/common-util';
import { ResizeContext } from '@gms/ui-core-components';
import { DragEventType } from '@gms/ui-util';
import React from 'react';
import TestRenderer from 'react-test-renderer';
import { SohOverviewContext } from '../../../../../src/ts/components/data-acquisition-ui/components/soh-overview/soh-overview-context';
import * as StationGroupFunctions from '../../../../../src/ts/components/data-acquisition-ui/components/soh-overview/station-groups/station-group';
import {
  calculateStatusCounts,
  generateSohStationGroupMap
} from '../../../../../src/ts/components/data-acquisition-ui/components/soh-overview/station-groups/station-groups-layout';
import { StationGroupProps } from '../../../../../src/ts/components/data-acquisition-ui/components/soh-overview/types';
import {
  contextValues,
  stationAndStationGroupSohStatus,
  testStationSoh
} from '../../../../__data__/data-acquisition-ui/soh-overview-data';

// tslint:disable-next-line:no-implicit-dependencies no-var-requires no-require-imports
const Enzyme = require('enzyme');
// tslint:disable-next-line: no-magic-numbers
jest.setTimeout(10000);
// set up window alert and open so we don't see errors
(window as any).alert = jest.fn();
(window as any).open = jest.fn();

// tslint:disable-next-line: deprecation
const lodash = require.requireActual('lodash');
lodash.uniqueId = () => '1';

const resizerContextValues = {
  height: 400,
  isResizing: false,
  containerHeight: 800,
  setIsResizing: jest.fn(),
  setHeight: jest.fn()
};

const statusesToDisplay: SohTypes.SohStatusSummary[] = [
  SohTypes.SohStatusSummary.GOOD,
  SohTypes.SohStatusSummary.MARGINAL,
  SohTypes.SohStatusSummary.BAD
];

let idCount = 0;
uuid.asString = jest.fn().mockImplementation(() => ++idCount);

describe('SOH Station Group', () => {
  const mockStationSohStatusList = stationAndStationGroupSohStatus.stationSoh;
  const stationGroupName = stationAndStationGroupSohStatus.stationGroups[0].stationGroupName;

  it('should be defined', () => {
    expect(StationGroupFunctions.StationGroup).toBeDefined();
  });
  it('calculates the number of entries in each category of good, marginal and bad', () => {
    const statusCounts = calculateStatusCounts(stationGroupName, mockStationSohStatusList);
    expect(statusCounts).toEqual({
      hasCapabilityRollup: true,
      badCount: 0,
      marginalCount: 1,
      okCount: 3
    });
  });

  it('onCellRightClick prevents the default action and shows a context menu', () => {
    const test: any = { event: {}, value: {} };
    test.event.preventDefault = jest.fn();
    test.value.status = SohTypes.SohStatusSummary.BAD;
    test.value.id = 'mockId';
    const createSpy = jest
      .spyOn(ContextMenu, 'show')
      // tslint:disable-next-line: no-console
      .mockImplementation(() => console.log('shown'));
    StationGroupFunctions.onCellRightClick(
      test.event,
      ['TEST'],
      0,
      [testStationSoh],
      jest.fn(),
      contextValues
    );
    expect(test.event.preventDefault).toHaveBeenCalled();
    expect(createSpy).toHaveBeenCalledTimes(1);
    test.value.status = SohTypes.SohStatusSummary.GOOD;
    StationGroupFunctions.onCellRightClick(
      test.event,
      ['TEST'],
      0,
      [testStationSoh],
      jest.fn(),
      contextValues
    );
    expect(test.event.preventDefault).toHaveBeenCalled();
    expect(createSpy).toHaveBeenCalledTimes(2);
  });

  const stationGroupStatus = stationAndStationGroupSohStatus.stationGroups[0];

  const sohStatuses = generateSohStationGroupMap(
    false,
    stationAndStationGroupSohStatus.stationGroups,
    stationAndStationGroupSohStatus.stationSoh,
    statusesToDisplay
  ).get('Group 4');
  const needsAcknowledgementStatuses = generateSohStationGroupMap(
    true,
    stationAndStationGroupSohStatus.stationGroups,
    stationAndStationGroupSohStatus.stationSoh,
    statusesToDisplay
  ).get('Group 4');
  it('generates soh station group maps', () => {
    expect(sohStatuses).toBeDefined();
    expect(needsAcknowledgementStatuses).toBeDefined();
  });

  if (sohStatuses && needsAcknowledgementStatuses) {
    const stationGroupProps: StationGroupProps = {
      stationGroupName: stationGroupStatus.stationGroupName,
      statusCounts: calculateStatusCounts(
        stationGroupStatus.stationGroupName,
        stationAndStationGroupSohStatus.stationSoh
      ),
      totalStationCount: stationAndStationGroupSohStatus.stationSoh.length,
      sohStatuses,
      needsAttentionStatuses: needsAcknowledgementStatuses,
      isHighlighted: true,
      selectedStationIds: ['H05N'],
      setSelectedStationIds: jest.fn(),
      groupHeight: 400,
      topContainerHeight: 200,
      setGroupHeight: jest.fn(),
      setTopContainerHeight: jest.fn()
    };

    const stationGroup = Enzyme.mount(
      <ResizeContext.Provider value={resizerContextValues}>
        <SohOverviewContext.Provider value={contextValues}>
          <StationGroupFunctions.StationGroup {...stationGroupProps} />
        </SohOverviewContext.Provider>
      </ResizeContext.Provider>
    );
    it('matches a snapshot when given proper props', () => {
      // Using create because this is a react fragment
      expect(
        TestRenderer.create(
          <ResizeContext.Provider value={resizerContextValues}>
            <SohOverviewContext.Provider value={contextValues}>
              <StationGroupFunctions.StationGroup {...stationGroupProps} />
            </SohOverviewContext.Provider>
          </ResizeContext.Provider>
        )
      ).toMatchSnapshot();
    });

    it('triggers onRightClick when a cell is right clicked', () => {
      const firstCell = stationGroup.find('.soh-overview-cell').first();
      const rightClickSpy = jest.spyOn(StationGroupFunctions, 'onCellRightClick');
      firstCell.simulate('contextmenu'); // contextmenu calls onRightClick: https://github.com/airbnb/enzyme/issues/596
      expect(rightClickSpy).toHaveBeenCalled();
    });

    it('calls acknowledge on drop', () => {
      const selectionList = ['H05N'];
      const acknowledgeSpy = jest.fn();
      StationGroupFunctions.cellDrop(selectionList, acknowledgeSpy);
      expect(acknowledgeSpy).toHaveBeenCalledWith(selectionList);
      acknowledgeSpy.mockClear();
    });

    xit('triggers on cellDrop with expected args when dropped on dropZone', () => {
      const dropZone = stationGroup.find('.drop-zone').first();
      const dropSpy = jest
        .spyOn(StationGroupFunctions, 'cellDrop')
        .mockImplementation((payload, acknowledgeSohStatus) => {
          expect(payload).toHaveLength(1);
          expect(typeof acknowledgeSohStatus).toBe('function');
        });
      const testEvent = {
        types: 'onDrop',
        dispatchConfig: { phasedRegistrationNames: { bubbled: 'onDrop' } },
        dataTransfer: {
          types: ['application/json', DragEventType.SOH_ACKNOWLEDGEMENT],
          getData: jest.fn().mockReturnValue(JSON.stringify(['H05N']))
        }
      };
      dropZone.simulate('drop', testEvent);
      expect(dropSpy).toHaveBeenCalled();
      dropSpy.mockClear();
    });
  } else {
    throw new Error('undefined or invalid data');
  }
});
