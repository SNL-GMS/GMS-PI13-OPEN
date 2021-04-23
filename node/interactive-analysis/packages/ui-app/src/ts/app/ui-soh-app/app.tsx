// tslint:disable: max-line-length
import { Intent, NonIdealState, Spinner } from '@blueprintjs/core';
import { IconNames } from '@blueprintjs/icons';
import { IS_INTERACTIVE_ANALYSIS_MODE_SOH } from '@gms/common-util';
import { AppState } from '@gms/ui-state';
import React from 'react';
import { Provider } from 'react-redux';
import { HashRouter, Route, Switch } from 'react-router-dom';
import * as Redux from 'redux';
import { withApolloReduxProvider } from '~app/apollo-redux-provider';
import { CommonUIComponents } from '~components/common-ui';
import { wrapSystemMessageSubscription } from '~components/common-ui/components/system-message/system-message-subscription';
import { SohMissingHistory } from '~components/data-acquisition-ui/components';
import { SohEnvironmentHistory } from '~components/data-acquisition-ui/components/environment-history';
import { SohLagHistory } from '~components/data-acquisition-ui/components/soh-lag-history';
import { StationStatistics } from '~components/data-acquisition-ui/components/station-statistics';
import { LoadingScreen } from '~components/loading-screen';
import { LoginScreen } from '~components/login-screen';
import { ProtectedRoute } from '~components/protected-route';
import { GoldenLayoutContext, Workspace } from '~components/workspace';
import { SohLag, SohMissing, SohTimeliness } from '~data-acquisition-ui/components/soh-bar-chart';
import { SohEnvironment } from '~data-acquisition-ui/components/soh-environment';
import { SohOverview } from '~data-acquisition-ui/components/soh-overview';
import { wrapSohStatusSubscriptions } from '~data-acquisition-ui/react-apollo-components/soh-status-subscription-wrapper';
import { createPopoutComponent } from '../create-popout-component';
import { glContextData } from './golden-layout-config';

/**
 * Wraps the component route (not for SOH).
 * Provides the required context providers to the component.
 * @param Component the component route
 * @param props the props passed down from the route to the component
 * @param store the redux store
 * @param suppressPopinIcon true to force suppress the golden-layout popin icon
 */
const wrap = (
  Component: any,
  props: any,
  store: Redux.Store<AppState>,
  suppressPopinIcon: boolean = false
) =>
  createPopoutComponent(
    withApolloReduxProvider(wrapSystemMessageSubscription(Component, props), store),
    props,
    suppressPopinIcon
  );

/**
 * Wraps the component route for SOH.
 * Provides the required context providers to the component.
 * @param Component the component route
 * @param props the props passed down from the route to the component
 * @param store the redux store
 * @param suppressPopinIcon true to force suppress the golden-layout popin icon
 */
const wrapSoh = (
  Component: any,
  props: any,
  store: Redux.Store<AppState>,
  suppressPopinIcon: boolean = false
) =>
  createPopoutComponent(
    withApolloReduxProvider(
      wrapSystemMessageSubscription(wrapSohStatusSubscriptions(Component, props, store), props),
      store
    ),
    props,
    suppressPopinIcon
  );

export const App = (store: Redux.Store<AppState>): React.ReactElement => {
  const app: React.ReactElement = !IS_INTERACTIVE_ANALYSIS_MODE_SOH ? (
    <NonIdealState
      icon={IconNames.ERROR}
      action={<Spinner intent={Intent.DANGER} />}
      title="Invalid settings"
      description={'Not configured for SOH mode - Please check settings'}
    />
  ) : (
    <Provider store={store}>
      <HashRouter>
        {
          // ! CAUTION: when changing the route paths
          // The route paths must match the `golden-layout` component name for popout windows
          // For example, the component name `signal-detections` must have the route path of `signal-detections`
        }
        {
          // For performance use `render` which accepts a functional component
          // that won’t get unnecessarily remounted like with component.
        }
        <Switch>
          {
            // Authentication
          }
          <Route path="/login" render={() => <LoginScreen />} />
          <ProtectedRoute path="/loading" render={props => <LoadingScreen />} />
          {
            // Common UI
          }
          <ProtectedRoute
            path="/system-messages"
            render={props => wrap(CommonUIComponents.SystemMessage, props, store)}
          />
          {
            // Data Acquisition
          }
          <ProtectedRoute
            path="/soh-overview"
            render={props => wrapSoh(SohOverview, props, store)}
          />
          <ProtectedRoute
            path="/station-statistics"
            render={props => wrapSoh(StationStatistics, props, store)}
          />
          <ProtectedRoute
            path="/soh-environment"
            render={props => wrapSoh(SohEnvironment, props, store)}
          />
          <ProtectedRoute path="/soh-missing" render={props => wrapSoh(SohMissing, props, store)} />
          <ProtectedRoute path="/soh-lag" render={props => wrapSoh(SohLag, props, store)} />
          <ProtectedRoute
            path="/soh-environment-trends"
            render={props => wrapSoh(SohEnvironmentHistory, props, store)}
          />
          <ProtectedRoute
            path="/soh-lag-trends"
            render={props => wrapSoh(SohLagHistory, props, store)}
          />
          <ProtectedRoute
            path="/soh-missing-trends"
            render={props => wrapSoh(SohMissingHistory, props, store)}
          />
          <ProtectedRoute
            path="/soh-timeliness"
            render={props => wrapSoh(SohTimeliness, props, store)}
          />
          {
            // Workspace
          }
          <GoldenLayoutContext.Provider value={glContextData(store)}>
            <ProtectedRoute path="/" render={props => wrapSoh(Workspace, props, store, true)} />
          </GoldenLayoutContext.Provider>
        </Switch>
      </HashRouter>
    </Provider>
  );
  return app;
};
