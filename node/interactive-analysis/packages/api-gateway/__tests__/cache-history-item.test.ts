import { HistoryItem } from '../src/ts/cache/history-item';

describe('Cache history item - undo redo', () => {
  /**
   * Sample cache data interface.
   */
  interface CacheData {
    aBoolean: boolean;
    aArray: number[];
    aString: string;
    aNumber: number;
    aObject: {
      value: string;
    };
  }

  // the original cache data
  const originalCacheData: CacheData = {
    aBoolean: false,
    // tslint:disable-next-line: no-magic-numbers
    aArray: [1, 2, 3, 4, 5],
    aString: 'my string',
    aNumber: 1,
    aObject: {
      value: 'my object'
    }
  };

  // updated cache data version 1
  const updatedCacheData1: CacheData = {
    aBoolean: true,
    // tslint:disable-next-line: no-magic-numbers
    aArray: [1, 4, 3, 2, 5],
    aString: 'updated my string',
    aNumber: 5,
    aObject: {
      value: 'updated my object'
    }
  };

  // updated cache data version 2
  const updatedCacheData2: CacheData = {
    aBoolean: true,
    // tslint:disable-next-line: no-magic-numbers
    aArray: [1],
    aString: undefined,
    aNumber: 10,
    aObject: {
      value: 'the correct value'
    }
  };

  it('Can create cache history item (with original value undefined)', () => {
    const history: HistoryItem<CacheData> = new HistoryItem(undefined);
    expect(history.index()).toEqual(0);
    expect(history.size()).toEqual(1);
    expect(history.value()).toEqual(undefined);
  });
  it('Can add cache history item (with original value undefined)', () => {
    const history: HistoryItem<CacheData> = new HistoryItem(undefined);
    history.add(updatedCacheData1);
    expect(history.index()).toEqual(1);
    expect(history.size()).toEqual(2);
    expect(history.value()).toEqual(updatedCacheData1);
  });
  it('Can undo cache history item (with original value undefined)', () => {
    const history: HistoryItem<CacheData> = new HistoryItem(undefined);
    history.add(updatedCacheData1);
    expect(history.index()).toEqual(1);
    history.undo();
    expect(history.index()).toEqual(0);
    expect(history.size()).toEqual(2);
    expect(history.value()).toEqual(undefined);
  });
  it('Can redo cache history item (with original value undefined)', () => {
    const history: HistoryItem<CacheData> = new HistoryItem(undefined);
    history.add(updatedCacheData1);
    expect(history.index()).toEqual(1);
    history.undo();
    history.redo();
    expect(history.index()).toEqual(1);
    expect(history.size()).toEqual(2);
    expect(history.value()).toEqual(updatedCacheData1);
  });

  it('Can create cache history item', () => {
    const history: HistoryItem<CacheData> = new HistoryItem(originalCacheData);
    expect(history.index()).toEqual(0);
    expect(history.size()).toEqual(1);
    expect(history.value()).toEqual(originalCacheData);
  });
  it('Can add cache history item', () => {
    const history: HistoryItem<CacheData> = new HistoryItem(originalCacheData);
    history.add(updatedCacheData1);
    expect(history.index()).toEqual(1);
    expect(history.size()).toEqual(2);
    expect(history.value()).toEqual(updatedCacheData1);
    history.add(updatedCacheData2);
    expect(history.index()).toEqual(2);
    expect(history.size()).toEqual(3);
    expect(history.value()).toEqual(updatedCacheData2);
  });
  it('Can undo cache history item', () => {
    const history: HistoryItem<CacheData> = new HistoryItem(originalCacheData);
    history.add(updatedCacheData1);
    history.add(updatedCacheData2);
    history.undo();
    expect(history.index()).toEqual(1);
    expect(history.size()).toEqual(3);
    expect(history.value()).toEqual(updatedCacheData1);
    history.undo();
    expect(history.index()).toEqual(0);
    expect(history.size()).toEqual(3);
    expect(history.value()).toEqual(originalCacheData);
  });
  it('Can redo cache history item', () => {
    const history: HistoryItem<CacheData> = new HistoryItem(originalCacheData);
    history.add(updatedCacheData1);
    history.add(updatedCacheData2);
    history.undo();
    history.undo();
    history.redo();
    expect(history.index()).toEqual(1);
    expect(history.size()).toEqual(3);
    expect(history.value()).toEqual(updatedCacheData1);
    history.redo();
    expect(history.index()).toEqual(2);
    expect(history.size()).toEqual(3);
    expect(history.value()).toEqual(updatedCacheData2);
  });
  it('Can reset cache history on add', () => {
    const history: HistoryItem<CacheData> = new HistoryItem(originalCacheData);
    history.add(updatedCacheData1);
    history.add(updatedCacheData2);
    history.undo();
    history.undo();
    expect(history.index()).toEqual(0);
    expect(history.size()).toEqual(3);
    expect(history.value()).toEqual(originalCacheData);
    history.add(updatedCacheData1);
    expect(history.index()).toEqual(1);
    expect(history.size()).toEqual(2);
    expect(history.value()).toEqual(updatedCacheData1);
  });
});
