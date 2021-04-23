import { ContextMenu } from '@blueprintjs/core';
import { SohTypes } from '@gms/common-graphql';
import { Offset } from '~components/data-acquisition-ui/shared/types';
import { QuietAction } from '../../../../../src/ts/components/data-acquisition-ui/components/soh-environment/types';
import { showQuietingContextMenu } from '../../../../../src/ts/components/data-acquisition-ui/shared/context-menus/quieting-menu';
// set up window alert and open so we don't see errors
(window as any).alert = jest.fn();
(window as any).open = jest.fn();

describe('quieting menu', () => {
  it('should have the show quieting menu function', () => {
    expect(showQuietingContextMenu).toBeDefined();
  });

  it('should call the contextmenu.show function with menu', () => {
    const stationName = '';
    const channelPair: SohTypes.ChannelMonitorPair = {
      channelName: 'name',
      monitorType: SohTypes.SohMonitorType.MISSING
    };
    const quietChannelMonitorStatuses: (
      stationName: string,
      channelPairs: SohTypes.ChannelMonitorPair[],
      quietDurationMs: number
    ) => void = jest.fn();
    const position: Offset = {
      // tslint:disable-next-line: no-magic-numbers
      left: 10,
      // tslint:disable-next-line: no-magic-numbers
      top: 10
    };
    // tslint:disable-next-line: no-magic-numbers
    const quietingDurationSelections = [300000, 900000, 3600000, 86400000, 604800000];
    const quietUntilMs = undefined;
    const quietAction: QuietAction = {
      channelMonitorPairs: [channelPair],
      position,
      quietUntilMs,
      quietingDurationSelections,
      stationName,
      quietChannelMonitorStatuses
    };
    ContextMenu.show = jest.fn() as any;
    showQuietingContextMenu(quietAction);
    expect(ContextMenu.show).toHaveBeenCalledTimes(1);
    expect((ContextMenu.show as any).mock.calls[0]).toMatchSnapshot();
  });
});
