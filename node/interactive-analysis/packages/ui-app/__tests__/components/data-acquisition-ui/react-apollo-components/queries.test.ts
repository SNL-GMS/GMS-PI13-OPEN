import * as graphql from 'react-apollo';
import {
  graphqlDefaultReferenceStationsQuery,
  graphqlTransferredFilesByTimeRangeQueryQuery
} from '../../../../src/ts/components/data-acquisition-ui/react-apollo-components/queries';
// tslint:disable-next-line:no-implicit-dependencies no-var-requires no-require-imports
// const Enzyme = require('enzyme');

// set up window alert and open so we don't see errors
(window as any).alert = jest.fn();
(window as any).open = jest.fn();

describe('queries ', () => {
  const graphqlold = graphql.graphql;
  beforeAll(() => {
    (graphql as any).graphql = jest.fn() as any;
  });
  afterAll(() => {
    (graphql as any).graphql = graphqlold;
  });
  it('should be defined', () => {
    expect(graphqlTransferredFilesByTimeRangeQueryQuery).toBeDefined();
    expect(graphqlDefaultReferenceStationsQuery).toBeDefined();
  });
  it('should have graphqlTransferredFilesByTimeRangeQueryQuery', () => {
    graphqlTransferredFilesByTimeRangeQueryQuery();
    expect(graphql.graphql).toBeCalledTimes(1);
    expect((graphql.graphql as any).mock.calls[0]).toMatchSnapshot();
  });
  it('should have graphqlTransferredFilesByTimeRangeQueryQuery', () => {
    graphqlDefaultReferenceStationsQuery();
    expect(graphql.graphql).toBeCalledTimes(2);
    expect((graphql.graphql as any).mock.calls[1]).toMatchSnapshot();
  });
});
