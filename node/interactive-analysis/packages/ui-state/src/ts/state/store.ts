import { IS_NODE_ENV_DEVELOPMENT, isWindowDefined } from '@gms/common-util';
import { getElectron, getElectronEnhancer } from '@gms/ui-util';
import * as Redux from 'redux';
import { createLogger } from 'redux-logger';
import { default as thunk } from 'redux-thunk';
import { initialAppState } from './initial-state';
import { Reducer } from './root-reducer';
import { AppState } from './types';

const configureStore = (initialState?: Partial<AppState> | undefined) => {
  const electron = getElectron();
  const windowIsDefined = !electron ? isWindowDefined() : undefined;

  const windowRedux: Window & {
    __REDUX_DEVTOOLS_EXTENSION_COMPOSE__?(a: any): void;
  } = windowIsDefined ? window : undefined;

  const electronEnhancer = electron ? getElectronEnhancer() : undefined;

  const composeEnhancers = windowRedux
    ? (windowRedux as any).__REDUX_DEVTOOLS_EXTENSION_COMPOSE__ != null
      ? (windowRedux as any).__REDUX_DEVTOOLS_EXTENSION_COMPOSE__
      : Redux.compose
    : Redux.compose;

  let store: Redux.Store<AppState>;

  const middlewares = [];
  middlewares.push(thunk);
  if (IS_NODE_ENV_DEVELOPMENT) {
    const logger = createLogger({
      collapsed: true,
      duration: true,
      timestamp: false,
      level: 'info',
      logger: console,
      logErrors: true,
      diff: false
    });
    middlewares.push(logger);
  }

  if (electron && electronEnhancer) {
    // tslint:disable-next-line: no-console
    console.info('Configuring Redux store for Electron');
  } else {
    // tslint:disable-next-line: no-console
    console.info('Configuring Redux store for browser');
  }

  const enhancers =
    electron && electronEnhancer
      ? composeEnhancers(
          Redux.applyMiddleware(...middlewares),
          // Must be placed after any enhancers which dispatch
          // their own actions such as redux-thunk or redux-saga
          electronEnhancer({
            dispatchProxy: a => store.dispatch(a)
          })
        )
      : composeEnhancers(Redux.applyMiddleware(...middlewares));

  store = Redux.createStore(Reducer, initialState as any, enhancers);
  return store;
};

// tslint:disable-next-line:no-default-export
export const createStore = (): Redux.Store<AppState> => {
  const store = configureStore(initialAppState);
  return store;
};
