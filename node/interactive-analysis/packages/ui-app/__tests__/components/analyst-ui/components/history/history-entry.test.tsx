import Enzyme from 'enzyme';
import React from 'react';
import {
  GenericHistoryEntry,
  makeKeyHandler,
  makeMouseHandler
} from '../../../../../src/ts/components/analyst-ui/components/history/history-stack/history-entry';
import {
  GenericHistoryEntryProps,
  HistoryEntryAction
} from '../../../../../src/ts/components/analyst-ui/components/history/history-stack/types';

describe('history entry', () => {
  const history: GenericHistoryEntryProps = {
    message: 'test',
    entryType: HistoryEntryAction.undo,
    isChild: false,
    isAssociated: true,
    isOrphaned: false,
    isInConflict: false,
    isEventReset: false,
    isAffected: false,
    handleMouseOut: undefined,
    handleAction: undefined,
    handleKeyDown: undefined,
    handleKeyUp: undefined,
    handleMouseEnter: undefined
  };
  test('renders correctly', () => {
    const wrapper = Enzyme.shallow(<GenericHistoryEntry {...history} />);
    expect(wrapper).toMatchSnapshot();
  });
  test('renders correctly', () => {
    const wrapper = Enzyme.shallow(
      <GenericHistoryEntry
        {...history}
        isAssociated={false}
        isInConflict={true}
        entryType={HistoryEntryAction.redo}
        isAffected={true}
        isEventReset={true}
        isOrphaned={true}
      />
    );
    expect(wrapper).toMatchSnapshot();
  });
  test('mouse in and out handlers work', () => {
    const handler = jest.fn();
    const handledHandler = makeMouseHandler(handler, true);
    const fauxEvent = {
      currentTarget: {
        focus: jest.fn()
      }
    };
    handledHandler(fauxEvent);
    expect(fauxEvent.currentTarget.focus).toHaveBeenCalled();
  });
  test('key handlers work', () => {
    const handler = jest.fn();
    const handledHandler = makeKeyHandler(handler);
    const fauxEvent = {
      currentTarget: {
        focus: jest.fn()
      }
    };
    handledHandler(fauxEvent);
    expect(handler).toHaveBeenCalled();
  });
});
