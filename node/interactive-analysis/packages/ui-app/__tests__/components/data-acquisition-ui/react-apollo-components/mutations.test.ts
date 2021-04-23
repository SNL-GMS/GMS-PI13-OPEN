import * as graphql from 'react-apollo';
import {
  graphqlAcknowledgeSohStatusMutation,
  graphqlQuietChannelMonitorStatusesMutation,
  graphqlSaveReferenceStationMutation,
  graphqlSaveStationGroupSohStatusMutation
} from '../../../../src/ts/components/data-acquisition-ui/react-apollo-components/mutations';
// tslint:disable-next-line:no-implicit-dependencies no-var-requires no-require-imports
// const Enzyme = require('enzyme');

// set up window alert and open so we don't see errors
(window as any).alert = jest.fn();
(window as any).open = jest.fn();

describe('mutations ', () => {
  const graphqlold = graphql.graphql;
  beforeAll(() => {
    (graphql as any).graphql = jest.fn() as any;
  });
  afterAll(() => {
    (graphql as any).graphql = graphqlold;
  });
  it('should be defined', () => {
    expect(graphqlSaveReferenceStationMutation).toBeDefined();
    expect(graphqlSaveStationGroupSohStatusMutation).toBeDefined();
    expect(graphqlAcknowledgeSohStatusMutation).toBeDefined();
    expect(graphqlQuietChannelMonitorStatusesMutation).toBeDefined();
  });
  it('should have graphqlSaveReferenceStationMutation', () => {
    graphqlSaveReferenceStationMutation();
    expect(graphql.graphql).toBeCalledTimes(1);
    expect((graphql.graphql as any).mock.calls[0]).toMatchSnapshot();
  });

  it('should have graphqlSaveStationGroupSohStatusMutation', () => {
    graphqlSaveStationGroupSohStatusMutation();
    expect(graphql.graphql).toBeCalledTimes(2);
    expect((graphql.graphql as any).mock.calls[1]).toMatchSnapshot();
  });

  it('should have graphqlAcknowledgeSohStatusMutation', () => {
    graphqlAcknowledgeSohStatusMutation();
    expect(graphql.graphql).toBeCalledTimes(3);
    expect((graphql.graphql as any).mock.calls[2]).toMatchSnapshot();
  });

  it('should have graphqlQuietChannelMonitorStatusesMutation', () => {
    graphqlQuietChannelMonitorStatusesMutation();
    expect(graphql.graphql).toBeCalledTimes(4);
    expect((graphql.graphql as any).mock.calls[3]).toMatchSnapshot();
  });
});
