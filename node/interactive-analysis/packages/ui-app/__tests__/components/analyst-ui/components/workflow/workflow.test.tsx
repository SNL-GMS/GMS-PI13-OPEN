import { Classes, NonIdealState, Spinner } from '@blueprintjs/core';
import { AnalystWorkspaceTypes } from '@gms/ui-state';
import { NetworkStatus } from 'apollo-client';
import { GraphQLError } from 'graphql';
import React from 'react';
import { WorkflowProps } from '../../../../../src/ts/components/analyst-ui/components/workflow/types';
import { Workflow } from '../../../../../src/ts/components/analyst-ui/components/workflow/workflow-component';

// set up window alert and open so we don't see errors
window.alert = jest.fn();
window.open = jest.fn();

// tslint:disable-next-line:no-implicit-dependencies no-var-requires no-require-imports
const Enzyme = require('enzyme');
// tslint:disable-next-line:no-implicit-dependencies no-var-requires no-require-imports
const Adapter = require('enzyme-adapter-react-16');
// set up window alert so we don't see errors
window.alert = jest.fn();

const mockWorkflowProps: WorkflowProps = {
  stagesQuery: {
    loading: false,
    networkStatus: NetworkStatus.ready,
    stages: [],
    variables: {},

    refetch: async () =>
      Promise.resolve({
        data: {},
        loading: false,
        stale: false,
        networkStatus: NetworkStatus.ready
      }),
    fetchMore: async () =>
      Promise.resolve({
        data: {},
        loading: false,
        stale: false,
        networkStatus: NetworkStatus.ready
      }),
    startPolling: () => {
      /* empty */
    },
    stopPolling: () => {
      /* empty */
    },
    subscribeToMore: () => () => {
      /* empty */
    },
    updateQuery: () => () => {
      /* empty */
    }
  },

  currentStageInterval: {
    id: 'mockdata',
    name: 'Mock Stage Interval 1',
    interval: {
      id: 'mockintervalid',
      timeInterval: {
        startTime: 200,
        endTime: 250
      },
      activityInterval: {
        id: 'mockactivityintervalid',
        analystActivity: AnalystWorkspaceTypes.AnalystActivity.eventRefinement,
        name: 'Mock Activity Interval 1'
      }
    }
  },
  client: undefined,
  currentTimeInterval: {
    startTime: 200,
    endTime: 200
  },
  analystActivity: AnalystWorkspaceTypes.AnalystActivity.eventRefinement,

  markActivityInterval: async () =>
    Promise.resolve({
      data: {},
      loading: false,
      stale: false,
      networkStatus: NetworkStatus.ready
    }),
  markStageInterval: async () =>
    Promise.resolve({
      data: {},
      loading: false,
      stale: false,
      networkStatus: NetworkStatus.ready
    }),
  setTimeInterval: async () =>
    Promise.resolve({
      data: {},
      loading: false,
      stale: false,
      networkStatus: NetworkStatus.ready
    }),
  setCurrentStageInterval: async (stageInterval: AnalystWorkspaceTypes.StageInterval) =>
    Promise.resolve({
      data: {
        loading: false,
        stale: false,
        networkStatus: NetworkStatus.ready
      }
    })
};

describe('workflow tests', () => {
  beforeEach(() => {
    Enzyme.configure({ adapter: new Adapter() });
  });

  // it('renders a snapshot', (done: jest.DoneCallback) => {
  //   const wrapper: ReactWrapper = Enzyme.mount(
  //     <ApolloProvider client={makeMockApolloClient()}>
  //       <Provider store={createStore()}>
  //         <ReduxApolloWorkflowContainer />
  //       </Provider>
  //     </ApolloProvider>
  //   );

  //   setImmediate(() => {
  //     wrapper.update();

  //     expect(wrapper.find(Workflow))
  //       .toMatchSnapshot();

  //     done();
  //   });
  // });

  it('displays spinner when loading', () => {
    const wrapper = Enzyme.shallow(<Workflow {...mockWorkflowProps} />);

    // Check that Workflow is not loading
    expect(wrapper.instance().props).toHaveProperty('stagesQuery.loading', false);
    expect(wrapper.find(Spinner)).toHaveLength(0);

    // Change the loading prop
    wrapper.setProps({
      stagesQuery: { ...mockWorkflowProps.stagesQuery, loading: true }
    });

    // Check that Workflow is loading
    expect(wrapper.instance().props).toHaveProperty('stagesQuery.loading', true);
    expect(wrapper.find(Spinner)).toHaveLength(1);
  });

  it('displays non ideal state on error', () => {
    const wrapper = Enzyme.shallow(<Workflow {...mockWorkflowProps} />);

    // Check that Workflow has no error
    expect(wrapper.instance().props).toHaveProperty('stagesQuery.error', undefined);
    expect(wrapper.find(NonIdealState)).toHaveLength(0);

    // Change the error prop
    const message = 'I am now non ideal';
    wrapper.setProps({
      stagesQuery: {
        ...mockWorkflowProps.stagesQuery,
        error: new GraphQLError(message)
      }
    });

    // Check that Workflow has error
    expect(wrapper.instance().props.stagesQuery.error).toBeDefined();
    expect(wrapper.find(NonIdealState)).toHaveLength(1);
    expect(wrapper.find(NonIdealState).hasClass(Classes.INTENT_DANGER));
    expect(wrapper.find(NonIdealState).prop('description')).toBe(message);
  });
});
