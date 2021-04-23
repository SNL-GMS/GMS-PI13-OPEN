import * as React from 'react';
import { uuid } from '../../../../../common-util/src/ts/common-util';
import { SystemMessageTable } from '../../../../src/ts/components/common-ui/components/system-message/system-message-table';
import { buildDefaultSeverityFilterMap } from '../../../../src/ts/components/common-ui/components/system-message/toolbar/severity-filters';
import { SystemMessageTableProps } from '../../../../src/ts/components/common-ui/components/system-message/types';
import { systemMessages } from './shared-data';

// tslint:disable-next-line:no-implicit-dependencies no-var-requires no-require-imports
const Enzyme = require('enzyme');

// set up window alert and open so we don't see errors
(window as any).alert = jest.fn();
(window as any).open = jest.fn();

// tslint:disable-next-line: deprecation
const lodash = require.requireActual('lodash');
lodash.uniqueId = () => '1';

let idCount = 0;
uuid.asString = jest.fn().mockImplementation(() => ++idCount);

describe('System Message Table', () => {
  it('should be defined', () => {
    expect(SystemMessageTable).toBeDefined();
  });

  const severityFilterMap = buildDefaultSeverityFilterMap();

  const props: SystemMessageTableProps = {
    addSystemMessages: jest.fn(),
    clearAllSystemMessages: jest.fn(),
    clearExpiredSystemMessages: jest.fn(),
    clearSystemMessages: jest.fn(),
    isAutoScrollingEnabled: true,
    setIsAutoScrollingEnabled: jest.fn(),
    systemMessages,
    severityFilterMap
  };

  const table = Enzyme.mount(<SystemMessageTable {...props} />);
  it('should match a snapshot', () => {
    expect(table).toMatchSnapshot();
  });

  const instance = table.instance();

  instance.state = { hasUnseenMessages: true };

  it('should not sort if props change if auto scrolling is enabled and sorted by time', () => {
    instance.forceUpdate = jest.fn();
    instance.table = {
      getTableApi: () => ({
        getSortModel: jest.fn(() => [
          {
            colId: 'time',
            sort: 'asc'
          }
        ]),
        ensureIndexVisible: jest.fn(() => true),
        refreshInfiniteCache: jest.fn()
      })
    };
    instance.sortByTime = jest.fn();
    instance.componentDidUpdate(
      {
        isAutoScrollingEnabled: false
      },
      {
        systemMessages
      }
    );
    clearTimeout(instance.autoScrollTimer);
    expect(instance.sortByTime).not.toHaveBeenCalled();
  });

  it('should sort if props change if auto scrolling is enabled', () => {
    instance.table = {
      getTableApi: () => ({
        getSortModel: jest.fn(() => [
          {
            colId: 'message',
            sort: 'asc'
          }
        ]),
        ensureIndexVisible: jest.fn(() => true),
        refreshInfiniteCache: jest.fn()
      })
    };
    instance.sortByTime = jest.fn();
    instance.componentDidUpdate(
      {
        isAutoScrollingEnabled: false
      },
      {
        systemMessages
      }
    );
    expect(instance.sortByTime).toHaveBeenCalled();
  });

  it('can tell if it has scrolled to the end', () => {
    instance.getScrollContainer = () => ({
      scrollTop: 100, // how far from the top we have scrolled
      scrollHeight: 1000, // simulated height of whole container
      clientHeight: 900 // simulated amount visible
    });
    expect(instance.isScrolledToBottom()).toBeFalsy();
  });

  it('can tell if it has NOT scrolled to the end', () => {
    instance.getScrollContainer = () => ({
      scrollTop: 0, // how far from the top we have scrolled
      scrollHeight: 1000, // simulated height of whole container
      clientHeight: 900 // simulated amount visible
    });
    expect(instance.isScrolledToBottom()).toBeFalsy();
  });

  it('gets the right subcategory values', () => {
    const params = {
      data: {
        id: systemMessages[0].id
      }
    };
    const result = instance.subcategoryValueGetter(params);
    expect(result).toEqual(systemMessages[0].subCategory);
  });

  it('gets the right severity values', () => {
    const params = {
      data: {
        id: systemMessages[0].id
      }
    };
    const result = instance.severityValueGetter(params);
    expect(result).toEqual(systemMessages[0].severity);
  });

  it('gets the right message values', () => {
    const params = {
      data: {
        id: systemMessages[0].id
      }
    };
    const result = instance.messageValueGetter(params);
    expect(result).toEqual(systemMessages[0].message);
  });

  it('can checked if scrolled to start', () => {
    const result = instance.isScrolledToTop();
    expect(result).toBeTruthy();
  });
  it('can handle sort changed', () => {
    instance.onSortChanged();
    expect(instance.props.setIsAutoScrollingEnabled).toHaveBeenCalledTimes(1);
  });

  it('can tell scroll direction down', () => {
    // tslint:disable-next-line: no-magic-numbers
    instance.state.prevScrollPositions = [0, 10];
    expect(instance.getScrollDirection()).toEqual('down');
  });

  it('can tell scroll direction up', () => {
    // tslint:disable-next-line: no-magic-numbers
    instance.state.prevScrollPositions = [Number(instance.SCROLL_UP_THRESHOLD_PX) + 1, 0];
    expect(instance.getScrollDirection()).toEqual('up');
  });

  it('can tell scroll direction unchanged', () => {
    // tslint:disable-next-line: no-magic-numbers
    instance.state.prevScrollPositions = [10, 10];
    expect(instance.getScrollDirection()).toEqual('unchanged');
  });

  it('can use new messages indicator', () => {
    instance.table = {
      getTableApi: () => ({
        getSortModel: jest.fn(() => [
          {
            colId: 'time',
            sort: 'asc'
          }
        ]),
        ensureIndexVisible: jest.fn(() => true)
      })
    };
    instance.onNewMessageIndicatorClick();
    expect(instance.props.setIsAutoScrollingEnabled).toHaveBeenCalledWith(true);
  });

  it('can use on body scroll', () => {
    jest.useFakeTimers();
    instance.props.setIsAutoScrollingEnabled(true);
    instance.getScrollDirection = jest.fn().mockImplementation(() => 'up');
    instance.onBodyScroll();
    jest.runAllTimers();
    expect(instance.getScrollDirection).toHaveBeenCalled();
    expect(instance.props.setIsAutoScrollingEnabled).toHaveBeenLastCalledWith(false);
  });
});
