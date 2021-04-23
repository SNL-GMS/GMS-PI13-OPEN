import * as JQuery from 'jquery';
import React from 'react';
import ReactDom from 'react-dom';

// combine scss of all components
// tslint:disable:ordered-imports
import '@blueprintjs/datetime/src/blueprint-datetime.scss';
import '@blueprintjs/icons/lib/css/blueprint-icons.css';
import '@gms/weavess/src/scss/weavess.scss';
import 'cesium/Widgets/widgets.css';
import '@gms/ui-core-components/src/scss/ui-core-components.scss';
import '../../../css/ui-app.scss';
// tslint:enable:ordered-imports

// required for golden-layout
(window as any).React = React;
(window as any).ReactDOM = ReactDom;
(window as any).$ = JQuery;
(window as any).CESIUM_BASE_URL = './cesium';

import { AppState, createStore } from '@gms/ui-state';
import { isDarkMode, replaceFavIcon } from '@gms/ui-util';
import * as Redux from 'redux';
import { checkEnvConfiguration } from '../check-env-configuration';
import { checkUserAgent } from '../check-user-agent';
import { configureElectron } from '../configure-electron';
import { configureReactPerformanceDevTool } from '../configure-react-performance-dev-tool';
import { App } from './app';

window.onload = () => {
  checkEnvConfiguration();
  checkUserAgent();
  configureReactPerformanceDevTool();

  // if the user is in dark mode, we replace the favicon with a lighter icon so it is visible
  if (isDarkMode()) {
    // tslint:disable-next-line:no-implicit-dependencies no-var-requires no-require-imports
    const logo = require('~resources/favicon--dark-192.png');
    replaceFavIcon(logo);
  }

  const store: Redux.Store<AppState> = createStore();
  ReactDom.render(App(store), document.getElementById('app'));
};

configureElectron();
