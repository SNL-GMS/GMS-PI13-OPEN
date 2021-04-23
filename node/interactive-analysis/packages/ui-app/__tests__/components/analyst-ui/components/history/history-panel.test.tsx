import Enzyme from 'enzyme';
import React from 'react';
import {
  getHandleRedoMouseEnter,
  getHandleUndoMouseEnter,
  HistoryPanel
} from '../../../../../src/ts/components/analyst-ui/components/history/history-panel';
import * as HistoryTypes from '../../../../../src/ts/components/analyst-ui/components/history/types';
import { historyList, providerState } from '../../../../__data__/history-data';

describe('history panel', () => {
  const history: HistoryTypes.HistoryPanelProps = {
    nonIdealState: undefined
  };

  test('has exposed function', () => {
    expect(HistoryPanel).toBeDefined();
    expect(getHandleUndoMouseEnter).toBeDefined();
  });

  test('renders non-ideal-state', () => {
    const nonIdealDiv = <div>I'm a non-ideal div, duh</div>;
    const wrap = Enzyme.shallow(
      <HistoryTypes.HistoryContext.Provider value={providerState}>
        <HistoryPanel
          {...history}
          nonIdealState={{
            loadingEvent: nonIdealDiv,
            loadingHistory: undefined
          }}
        />
      </HistoryTypes.HistoryContext.Provider>
    );
    expect(wrap).toMatchSnapshot();
  });

  providerState.historyList = historyList;
  const wrapper = Enzyme.mount(
    <HistoryTypes.HistoryContext.Provider value={providerState}>
      <HistoryPanel {...history} />
    </HistoryTypes.HistoryContext.Provider>
  );

  test('renders ideal-state', () => {
    expect(wrapper).toMatchSnapshot();
  });

  test('has getHandleUndoMouseEnter function', () => {
    providerState.historyList = historyList;
    const mockFunc = jest.fn();
    mockFunc.mockReturnValue(true);
    const returnedFunc = getHandleUndoMouseEnter(providerState, mockFunc);
    expect(returnedFunc).toBeInstanceOf(Function);
    returnedFunc();
    // tslint:disable-next-line: no-unbound-method
    expect(providerState.setHistoryActionIntent).toHaveBeenCalled();
    expect(mockFunc).toHaveBeenCalled();
  });

  test('has getHandleRedoMouseEnter function', () => {
    providerState.historyList = historyList;
    const mockFunc = jest.fn();
    mockFunc.mockReturnValue(true);
    const returnedFunc = getHandleRedoMouseEnter(providerState);
    expect(returnedFunc).toBeInstanceOf(Function);
    returnedFunc();
    // tslint:disable-next-line: no-unbound-method
    expect(providerState.setHistoryActionIntent).toHaveBeenCalled();
  });

  test('undo button click triggers undo call', () => {
    const undoButton = wrapper.find('button[title="Undo last action"]');
    undoButton.simulate('click');
    // tslint:disable-next-line: no-unbound-method
    expect(providerState.undoHistory).toHaveBeenCalledWith(1);
  });

  test('redo button click triggers redo call', () => {
    const redoButton = wrapper.find('button[title="Redo last undone action"]');
    redoButton.simulate('click');
    // tslint:disable-next-line: no-unbound-method
    expect(providerState.redoHistory).toHaveBeenCalledWith(1);
  });
});
