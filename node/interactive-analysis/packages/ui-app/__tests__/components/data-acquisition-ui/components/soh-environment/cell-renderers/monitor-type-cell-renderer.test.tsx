import { SohTypes } from '@gms/common-graphql';
import React from 'react';
import {
  MonitorTypeCellRenderer,
  MonitorTypeCellRendererBase
} from '../../../../../../src/ts/components/data-acquisition-ui/components/soh-environment/cell-renderers/monitor-type-cell-renderer';
// tslint:disable-next-line: max-line-length
// import { EnvironmentalSoh } from '../../../../../src/ts/components/data-acquisition-ui/components/soh-environment/types';
import {
  EnvironmentalSoh,
  EnvironmentTableDataContext,
  EnvironmentTableRow
} from '../../../../../../src/ts/components/data-acquisition-ui/components/soh-environment/types';
// tslint:disable-next-line:no-implicit-dependencies no-var-requires no-require-imports
const Enzyme = require('enzyme');

// set up window alert and open so we don't see errors
(window as any).alert = jest.fn();
(window as any).open = jest.fn();

describe('Monitor Type Cell Renderer', () => {
  const valueAndStatusByChannelNameValue: EnvironmentalSoh = {
    value: 10,
    status: SohTypes.SohStatusSummary.GOOD,
    monitorTypes: SohTypes.SohMonitorType.MISSING,
    channelName: 'channelName',
    hasUnacknowledgedChanges: true,
    quietTimingInfo: {
      quietUntilMs: 100000000,
      quietDurationMs: 100000
    },
    isSelected: true,
    isContributing: true
  };

  const valueAndStatusByChannelName: Map<string, EnvironmentalSoh> = new Map();
  valueAndStatusByChannelName.set('monitorType', valueAndStatusByChannelNameValue);

  const envRow: EnvironmentTableRow = {
    id: '1234',
    monitorType: SohTypes.SohMonitorType.MISSING,
    monitorStatus: SohTypes.SohStatusSummary.GOOD,
    valueAndStatusByChannelName: new Map<string, EnvironmentalSoh>(),
    monitorIsSelected: true
  };

  const myProps: any = {
    data: envRow,
    colDef: {
      headerName: 'monitorType'
    }
  };

  const monitorTypeCellRenderer = Enzyme.mount(
    <EnvironmentTableDataContext.Provider value={{ data: [myProps.data] }}>
      {MonitorTypeCellRenderer(myProps)}
    </EnvironmentTableDataContext.Provider>
  );

  const monitorTypeCellRendererBase = Enzyme.mount(
    <EnvironmentTableDataContext.Provider value={{ data: [myProps.data] }}>
      <MonitorTypeCellRendererBase {...envRow} />
    </EnvironmentTableDataContext.Provider>
  );

  it('should be defined', () => {
    expect(MonitorTypeCellRendererBase).toBeDefined();
    expect(MonitorTypeCellRenderer).toBeDefined();
  });

  it('should match snapshot when calling monitorTypeCellRenderer', () => {
    expect(monitorTypeCellRenderer).toMatchSnapshot();
  });

  it('should match snapshot for MonitorTypeCellRendererBase', () => {
    expect(monitorTypeCellRendererBase).toMatchSnapshot();
  });
});
