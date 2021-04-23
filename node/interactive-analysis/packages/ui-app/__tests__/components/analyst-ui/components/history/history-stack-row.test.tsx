import Enzyme from 'enzyme';
import React from 'react';
import {
  HistoryStackRow,
  HistoryStackRowProps
} from '../../../../../src/ts/components/analyst-ui/components/history/history-stack/history-stack-row';

describe('history stack row', () => {
  const history: HistoryStackRowProps = {
    isFirstRow: true,
    areUndoRedoAdjacent: false,
    redoTarget: true,
    undoTarget: false,
    areUndoRedoJoined: true
  };
  const child = <div />;
  test('renders correctly', () => {
    const wrapper = Enzyme.shallow(<HistoryStackRow {...history}>{child}</HistoryStackRow>);
    expect(wrapper).toMatchSnapshot();
  });
});
