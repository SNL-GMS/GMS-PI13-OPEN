import { UserMode } from '@gms/common-util';
import React from 'react';
import {
  GLComponentConfig,
  GLComponentConfigList
} from '../../../../../src/ts/components/workspace/components/golden-layout/types';
import {
  AboutMenu,
  AboutMenuProps
} from '../../../../../src/ts/components/workspace/components/menus/about-menu';
import {
  AppMenu,
  AppMenuProps
} from '../../../../../src/ts/components/workspace/components/menus/app-menu';
import { Toolbar } from '../../../../../src/ts/components/workspace/components/toolbar/toolbar-component';
import { ToolbarProps } from '../../../../../src/ts/components/workspace/components/toolbar/types';

// tslint:disable-next-line:no-implicit-dependencies no-var-requires no-require-imports
const Enzyme = require('enzyme');

function flushPromises(): any {
  return new Promise(setImmediate);
}

function testComponentRender(wrapper) {
  it('should have a consistent snapshot on mount', () => {
    expect(wrapper).toMatchSnapshot();
    flushPromises();
  });

  it('should have basic props built on render', () => {
    const buildProps = wrapper.props() as AppMenuProps;
    expect(buildProps).toMatchSnapshot();
  });
}

// set up window alert and open so we don't see errors
window.alert = jest.fn();
window.open = jest.fn();
/**
 * Tests the app menu component
 */

const analystUIComponents: Map<string, GLComponentConfig> = new Map([
  [
    'waveform-display',
    {
      type: 'react-component',
      title: 'Waveforms',
      component: 'waveform-display'
    }
  ]
]);

const dataAcqComponents: Map<string, GLComponentConfig> = new Map([
  [
    'soh-overview',
    {
      type: 'react-component',
      title: 'SOH Overview',
      component: 'soh-overview'
    }
  ]
]);

const components: GLComponentConfigList = {};
[...analystUIComponents, ...dataAcqComponents].forEach(([componentName, glComponent]) => {
  components[componentName] = glComponent;
});

const fauxAnalystComponents = new Map([
  [
    'Analyst',
    new Map([
      [
        components['waveform-display'].component,
        { id: components['waveform-display'], value: undefined }
      ]
    ])
  ],
  [
    'Data Acquisition',
    new Map([
      [components['soh-overview'].component, { id: components['soh-overview'], value: undefined }]
    ])
  ]
]);

describe('app-menu', () => {
  const showLogs = jest.fn();
  const appMenu = (
    <AppMenu
      components={fauxAnalystComponents}
      openLayoutName=""
      clearLayout={jest.fn()}
      logout={jest.fn()}
      openDisplay={jest.fn()}
      openWorkspace={jest.fn()}
      showAboutDialog={jest.fn()}
      saveWorkspaceAs={jest.fn()}
      getOpenDisplays={() => []}
      showLogs={showLogs}
      logo={undefined}
      userProfile={{
        id: 'id',
        userId: '1',
        defaultLayoutName: 'default',
        audibleNotifications: [],
        workspaceLayouts: [
          {
            name: 'default',
            layoutConfiguration: 'test',
            supportedUserInterfaceModes: [UserMode.ANALYST]
          }
        ]
      }}
    />
  );

  // tslint:disable-next-line: one-variable-per-declaration
  const wrapper: any = Enzyme.mount(appMenu);

  testComponentRender(wrapper);

  it('should be able to click log out', () => {
    Enzyme.shallow(appMenu)
      .find('.app-menu__logs')
      .simulate('click');
    expect(showLogs).toBeCalled();
  });

  it('should be able to click About', () => {
    Enzyme.shallow(appMenu)
      .find('.app-menu__about')
      .simulate('click');
    expect(showLogs).toBeCalled();
  });
});

describe('app-toolbar', () => {
  const props: ToolbarProps = {
    components: fauxAnalystComponents,
    clearLayout: () => jest.fn(),
    isAboutDialogOpen: true,
    logo: new Image(),
    logout: async () => jest.fn() as any,
    openDisplay: () => jest.fn(),
    openWorkspace: () => jest.fn(),
    showLogPopup: () => jest.fn(),
    showAboutDialog: () => jest.fn(),
    userName: 'Test User',
    getOpenDisplays: () => [],
    userProfile: {
      id: 'id',
      userId: '1',
      defaultLayoutName: 'default',
      workspaceLayouts: [
        {
          name: 'default',
          layoutConfiguration: 'test',
          supportedUserInterfaceModes: [UserMode.ANALYST]
        }
      ],
      audibleNotifications: []
    },
    isSaveWorkspaceAsDialogOpen: false,
    openLayoutName: '',
    setLayout: undefined,
    saveDialog: undefined,
    setOpenLayoutName: undefined,
    toggleSaveWorkspaceAsDialog: undefined,
    versionInfo: {
      versionNumber: '1',
      commitSHA: '2'
    }
  };
  const testToolbar = <Toolbar {...props} />;
  const wrapper: any = Enzyme.mount(testToolbar);
  // const toolbarInstance = wrapper.instance();
  testComponentRender(wrapper);
});

describe('about-menu', () => {
  const props: AboutMenuProps = {
    versionNumber: '9.0.1-Snapshot',
    commitSHA: 'FCDA123',
    logo: new Image()
  };
  const testAboutMenu = <AboutMenu {...props} />;
  const wrapper: any = Enzyme.mount(testAboutMenu);

  testComponentRender(wrapper);
});
