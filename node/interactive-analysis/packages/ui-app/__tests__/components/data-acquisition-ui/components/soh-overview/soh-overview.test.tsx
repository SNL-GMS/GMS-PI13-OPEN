import { CommonTypes, SohTypes } from '@gms/common-graphql';
import { WithNonIdealStates } from '@gms/ui-core-components';
import React from 'react';
import { uuid } from '../../../../../../common-util/src/ts/common-util';
import { SohOverviewComponent } from '../../../../../src/ts/components/data-acquisition-ui/components/soh-overview/soh-overview-component';
import { StationGroupsLayout } from '../../../../../src/ts/components/data-acquisition-ui/components/soh-overview/station-groups/station-groups-layout';
import { DataAcquisitionNonIdealStateDefs } from '../../../../../src/ts/components/data-acquisition-ui/shared/non-ideal-states';
import { stationAndStationGroupSohStatus } from '../../../../__data__/data-acquisition-ui/soh-overview-data';

// tslint:disable-next-line:no-implicit-dependencies no-var-requires no-require-imports
const Enzyme = require('enzyme');

// set up window alert and open so we don't see errors
(window as any).alert = jest.fn();
(window as any).open = jest.fn();

function flushPromises(): any {
  return new Promise(setImmediate);
}

// tslint:disable-next-line: deprecation
const lodash = require.requireActual('lodash');
lodash.uniqueId = () => '1';

describe('Soh Component', () => {
  beforeAll(() => {
    // Create a spy on console (console.log in this case) and provide some mocked implementation
    // In mocking global objects it's usually better than simple `jest.fn()`
    // because you can `un-mock` it in clean way doing `mockRestore`
    jest.spyOn(console, 'error').mockImplementation((msg: string) => {
      expect(msg).toEqual('got a failed promise');
    });
  });

  it('should be defined', () => {
    expect(StationGroupsLayout).toBeDefined();
  });

  uuid.asString = () => '1';

  const mockAcknowledge = jest.fn().mockReturnValue(new Promise(jest.fn()));
  const SohOverviewComponentNonIdealState = WithNonIdealStates(
    [...DataAcquisitionNonIdealStateDefs.generalSohNonIdealStateDefinitions],
    SohOverviewComponent
  );
  const sohOverviewOrNonIdealState: any = Enzyme.mount(
    <SohOverviewComponentNonIdealState
      setSelectedAceiType={jest.fn()}
      selectedAceiType={SohTypes.AceiType.BEGINNING_TIME_OUTAGE}
      sohStatus={{
        lastUpdated: 0,
        loading: false,
        error: undefined,
        stationAndStationGroupSoh: {
          stationGroups: [],
          stationSoh: [],
          isUpdateResponse: false
        }
      }}
      result={undefined}
      acknowledgeSohStatus={mockAcknowledge}
      glContainer={undefined}
      saveStationGroupSohStatus={undefined}
      mutate={undefined}
      uiConfigurationQuery={undefined}
      selectedStationIds={[]}
      setSelectedStationIds={jest.fn()}
    />
  );

  it('should render non ideal states and match snapshot', () => {
    expect(sohOverviewOrNonIdealState).toMatchSnapshot();
  });

  it('should show non-ideal state when the golden layout container is hidden', () => {
    sohOverviewOrNonIdealState.setProps({
      sohStatus: undefined,
      acknowledgeSohStatus: mockAcknowledge,
      glContainer: { isHidden: true, on: jest.fn(), off: jest.fn() },
      saveStationGroupSohStatus: undefined,
      mutate: undefined
    });
    expect(sohOverviewOrNonIdealState).toMatchSnapshot();
  });

  it('should show non-ideal state when there is no query', () => {
    sohOverviewOrNonIdealState.setProps({
      sohStatus: undefined,
      acknowledgeSohStatus: mockAcknowledge,
      glContainer: undefined,
      saveStationGroupSohStatus: undefined,
      mutate: undefined
    });
    expect(sohOverviewOrNonIdealState).toMatchSnapshot();
  });

  it('should show non-ideal state when there is no station group data', () => {
    sohOverviewOrNonIdealState.setProps({
      sohStatus: {
        loading: false,
        stationGroupSohStatus: []
      },
      uiConfigurationQuery: {
        loading: false,
        uiAnalystConfiguration: {
          logLevel: CommonTypes.LogLevel.info,
          acknowledgementQuietDuration: 10,
          redisplayPeriod: 10,
          reprocessingPeriod: 20
        }
      },
      acknowledgeSohStatus: mockAcknowledge,
      glContainer: undefined,
      saveStationGroupSohStatus: undefined,
      mutate: undefined
    });
    expect(sohOverviewOrNonIdealState).toMatchSnapshot();
  });

  it('should match snapshot with basic props', () => {
    const realDateNow = Date.now.bind(global.Date);
    // tslint:disable-next-line: no-magic-numbers
    const dateNowStub = jest.fn(() => 1530518207007);
    global.Date.now = dateNowStub;

    sohOverviewOrNonIdealState.setProps({
      sohStatus: {
        loading: false,
        stationAndStationGroupSoh: stationAndStationGroupSohStatus
      },
      uiConfigurationQuery: {
        loading: false,
        uiAnalystConfiguration: {
          acknowledgementQuietDuration: 10,
          reprocessingPeriod: 10
        }
      },
      acknowledgeSohStatus: mockAcknowledge,
      glContainer: undefined,
      saveStationGroupSohStatus: undefined,
      mutate: undefined
    });
    expect(sohOverviewOrNonIdealState).toMatchSnapshot();

    global.Date.now = realDateNow;
  });

  it('should call mutation when acknowledgeSohStatus is called', () => {
    sohOverviewOrNonIdealState
      .find('SohOverviewComponent')
      .instance()
      .acknowledgeSohStatus(['H05N', 'H06N']);
    expect(mockAcknowledge).toHaveBeenCalledTimes(1);
    expect(mockAcknowledge).toHaveBeenCalledWith({
      variables: { stationNames: ['H05N', 'H06N'] }
    });
  });

  it('should log an error when mutation fails', () => {
    const mockReject = jest.fn().mockReturnValueOnce(Promise.reject('got a failed promise'));
    sohOverviewOrNonIdealState.setProps({
      sohStatus: {
        loading: false,
        stationAndStationGroupSoh: stationAndStationGroupSohStatus
      },
      uiConfigurationQuery: {
        loading: false,
        uiAnalystConfiguration: {
          acknowledgementQuietDuration: 10,
          redisplayPeriod: 10,
          reprocessingPeriod: 20
        }
      },
      acknowledgeSohStatus: mockReject,
      glContainer: undefined,
      saveStationGroupSohStatus: undefined,
      mutate: undefined
    });
    sohOverviewOrNonIdealState
      .find('SohOverviewComponent')
      .instance()
      .acknowledgeSohStatus(['H05N', 'H06N']);
    flushPromises();
  });
});
