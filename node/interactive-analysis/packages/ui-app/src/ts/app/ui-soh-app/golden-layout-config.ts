import { UserMode } from '@gms/common-util';
import GoldenLayout from '@gms/golden-layout';
import { AppState } from '@gms/ui-state';
import { isElectron } from '@gms/ui-util';
import * as Redux from 'redux';
import { CommonUIComponents } from '~components/common-ui';
import {
  SohEnvironment,
  SohEnvironmentHistory,
  SohLag,
  SohLagHistory,
  SohMissing,
  SohMissingHistory,
  SohOverview,
  SohTimeliness,
  StationStatistics
} from '~components/data-acquisition-ui/components';
import {
  GLComponentConfig,
  GLComponentConfigList,
  GLMap,
  GoldenLayoutContextData
} from '~components/workspace';
import { GLComponentValue } from '~components/workspace/components/golden-layout/types';
import { withApolloReduxProvider } from '../apollo-redux-provider';

/**
 * Wraps the component for a golden layout panel.
 * Provides the required context providers to the component.
 * @param Component the golden layout component
 * @param store the Redux store
 */
// tslint:disable-next-line: no-unnecessary-callback-wrapper
const wrap = (Component: any, store: Redux.Store<AppState>) =>
  withApolloReduxProvider(Component, store);

// ! CAUTION: when changing the golden-layout component name
// The route paths must match the `golden-layout` component name for popout windows
// For example, the component name `signal-detections` must have the route path of `signal-detections`

const commonUIComponents: Map<string, GLComponentConfig> = new Map([
  [
    'system-messages',
    {
      type: 'react-component',
      title: 'System Messages',
      component: 'system-messages'
    }
  ]
]);

const sohComponents: Map<string, GLComponentConfig> = new Map([
  [
    'soh-overview',
    {
      type: 'react-component',
      title: 'SOH Overview',
      component: 'soh-overview'
    }
  ],
  [
    'station-statistics',
    {
      type: 'react-component',
      title: 'Station Statistics',
      component: 'station-statistics'
    }
  ],
  [
    'soh-lag',
    {
      type: 'react-component',
      title: 'SOH Lag',
      component: 'soh-lag'
    }
  ],
  [
    'soh-missing',
    {
      type: 'react-component',
      title: 'SOH Missing',
      component: 'soh-missing'
    }
  ],
  [
    'soh-environment',
    {
      type: 'react-component',
      title: 'SOH Environment',
      component: 'soh-environment'
    }
  ],
  [
    'soh-environment-trends',
    {
      type: 'react-component',
      title: 'SOH Environment Trends',
      component: 'soh-environment-trends'
    }
  ],
  [
    'soh-lag-trends',
    {
      type: 'react-component',
      title: 'SOH Lag Trends',
      component: 'soh-lag-trends'
    }
  ],
  [
    'soh-missing-trends',
    {
      type: 'react-component',
      title: 'SOH Missing Trends',
      component: 'soh-missing-trends'
    }
  ],
  [
    'soh-timeliness',
    {
      type: 'react-component',
      title: 'SOH Timeliness',
      component: 'soh-timeliness'
    }
  ]
]);

// Adding all SOH components
const components: GLComponentConfigList = {};
[...sohComponents].forEach(([componentName, glComponent]) => {
  components[componentName] = glComponent;
});

// Add desired common UI Components here
components['system-messages'] = commonUIComponents.get('system-messages');

const defaultGoldenLayoutConfig: GoldenLayout.Config = {
  settings: {
    showPopoutIcon: Boolean(isElectron()),
    showMaximiseIcon: true,
    showCloseIcon: true
  },
  content: [
    {
      type: 'row',
      content: [
        {
          type: 'column',
          content: [
            {
              ...sohComponents.get('soh-overview'),
              height: 30
            }
          ]
        },
        {
          type: 'column',
          content: [
            {
              ...sohComponents.get('station-statistics')
            }
          ]
        }
      ]
    }
  ],
  dimensions: {
    borderWidth: 2,
    minItemHeight: 30,
    minItemWidth: 30,
    headerHeight: 30
  }
};

/**
 * The Golden Layout context for the SOH UI.
 * Note: Defines the Application Menu structure.
 */
const glComponents = (store: Redux.Store<AppState>): GLMap =>
  new Map<string, GLComponentValue>([
    [
      'system-messages',
      {
        id: components['system-messages'],
        value: wrap(CommonUIComponents.SystemMessage, store)
      }
    ],
    [
      'SOH',
      new Map([
        [
          components['soh-overview'].component,
          {
            id: components['soh-overview'],
            value: wrap(SohOverview, store)
          }
        ],
        [
          components['station-statistics'].component,
          {
            id: components['station-statistics'],
            value: wrap(StationStatistics, store)
          }
        ],
        [
          components['soh-lag'].component,
          {
            id: components['soh-lag'],
            value: wrap(SohLag, store)
          }
        ],
        [
          components['soh-missing'].component,
          {
            id: components['soh-missing'],
            value: wrap(SohMissing, store)
          }
        ],
        [
          components['soh-environment'].component,
          {
            id: components['soh-environment'],
            value: wrap(SohEnvironment, store)
          }
        ],
        [
          components['soh-environment-trends'].component,
          {
            id: components['soh-environment-trends'],
            value: wrap(SohEnvironmentHistory, store)
          }
        ],
        [
          components['soh-lag-trends'].component,
          {
            id: components['soh-lag-trends'],
            value: wrap(SohLagHistory, store)
          }
        ],
        [
          components['soh-missing-trends'].component,
          {
            id: components['soh-missing-trends'],
            value: wrap(SohMissingHistory, store)
          }
        ],
        [
          components['soh-timeliness'].component,
          {
            id: components['soh-timeliness'],
            value: wrap(SohTimeliness, store)
          }
        ]
      ])
    ]
  ]);

/** The Golden Layout context for the SOH UI */
export const glContextData = (store: Redux.Store<AppState>): GoldenLayoutContextData => ({
  glComponents: glComponents(store),
  config: {
    components,
    workspace: defaultGoldenLayoutConfig
  },
  supportedUserInterfaceMode: UserMode.SOH
});
