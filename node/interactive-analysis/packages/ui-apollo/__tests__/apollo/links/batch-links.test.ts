import { BatchHttpLink } from 'apollo-link-batch-http';
import { BatchLink, BatchMsgPackLink } from '../../../src/ts/apollo/links/batch-link';

// tslint:disable: no-console
// tslint:disable: no-unbound-method
window.alert = jest.fn();
window.open = jest.fn();

describe('batch links', () => {
  // make sure the functions are defined
  test('should exist', () => {
    expect(BatchLink).toBeDefined();
    expect(BatchMsgPackLink).toBeDefined();
  });

  // make sure the functions return batch links
  test('should create batch links', () => {
    expect(BatchLink('test1')).toBeInstanceOf(BatchHttpLink);
    expect(BatchMsgPackLink('test2')).toBeInstanceOf(BatchHttpLink);
  });
});
