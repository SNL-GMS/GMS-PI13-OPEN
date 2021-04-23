import React from 'react';
import * as Apollo from 'react-apollo';
import { wrapSohStatusSubscriptions } from '../../../../src/ts/components/data-acquisition-ui/react-apollo-components/soh-status-subscription-wrapper';

// set up window alert and open so we don't see errors
(window as any).alert = jest.fn();
(window as any).open = jest.fn();

describe('soh-status-subscription-wrapper ', () => {
  const DumbComp: React.FunctionComponent<{}> = props => <div />;
  const ApolloProvider: any = Apollo.ApolloProvider;
  beforeAll(() => {
    (Apollo as any).ApolloProvider = DumbComp as any;
  });
  afterAll(() => {
    (Apollo as any).ApolloProvider = ApolloProvider;
  });
  it('should have defined members', () => {
    expect(wrapSohStatusSubscriptions).toBeDefined();
  });
});
