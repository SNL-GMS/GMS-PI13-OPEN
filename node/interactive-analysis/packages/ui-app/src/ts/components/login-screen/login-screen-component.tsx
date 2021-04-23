import { Button, Classes, H2, Intent, NonIdealState, Spinner } from '@blueprintjs/core';
import { IconNames } from '@blueprintjs/icons';
import { AuthStatus } from '@gms/ui-state/lib/state/user-session/types';
import delay from 'lodash/delay';
import React from 'react';
import { Redirect } from 'react-router';
import { legalNotice } from '~config/legal-notice';
import { authenticateWith, checkIsAuthenticated } from '../../authentication';
import { LoginScreenProps, LoginScreenState } from './types';

// tslint:disable-next-line:no-implicit-dependencies no-var-requires no-require-imports
const logo = require('../../resources/gms_logo_with_text.png');
const LOGO_WIDTH_PX = 370;

// the number of milliseconds between reconnects
const RECONNECT_TIMEOUT_MS = 1000;

/**
 * Checks login status. If the user is logged in, it routes to the page above it. Otherwise it displays a login page
 */
export class LoginScreenComponent extends React.Component<LoginScreenProps, LoginScreenState> {
  /** unique timer id used for attempting reconnects to the gateway */
  private reconnectTimerId: number = undefined;
  /** reference to input - used for autofocus */
  private userNameInput: HTMLInputElement = undefined;
  public constructor(props) {
    super(props);
    this.state = {
      username: '',
      password: ''
    };
  }

  /**
   * Called immediately after a component is mounted.
   * Setting state here will trigger re-rendering.
   */
  public componentDidMount() {
    if (!this.props.authenticationCheckComplete) {
      // check if the user is authenticated
      checkIsAuthenticated()
        .then(this.setAuthStatus)
        .catch();
    }
    if (this.userNameInput) {
      this.userNameInput.focus();
    }
  }

  /**
   * Called immediately after updating occurs. Not called for the initial render.
   *
   * @param prevProps the previous props
   * @param prevState the previous state
   */
  public componentDidUpdate(prevProps: LoginScreenProps, prevState: LoginScreenState) {
    if (this.userNameInput) {
      this.userNameInput.focus();
    }
    if (this.props.failedToConnect) {
      // attempt to reconnect if the app failed to connect to the gateway
      this.reconnect();
    }
  }

  /**
   * Called immediately before a component is destroyed. Perform any necessary
   * cleanup in this method, such as cancelled network requests,
   * or cleaning up any DOM elements created in componentDidMount.
   */
  public componentWillUnmount() {
    if (this.reconnectTimerId) {
      // destroy reconnect timer
      clearTimeout(this.reconnectTimerId);
      this.reconnectTimerId = undefined;
    }
  }

  /**
   * Display a loading spinner
   */
  public render() {
    const { from } = this.props.location.state
      ? this.props.location.state
      : { from: { pathname: '/' } };

    // error state: failed to connect to the gateway
    if (this.props.failedToConnect) {
      return (
        <NonIdealState
          icon={IconNames.ERROR}
          action={<Spinner intent={Intent.DANGER} />}
          title="No connection to server"
          description="Attempting to connect..."
        />
      );
    }

    // attempting to login
    if (!this.props.authenticationCheckComplete) {
      return (
        <NonIdealState
          action={<Spinner intent={Intent.PRIMARY} />}
          title="Checking login"
          description="Attempting to login..."
        />
      );
    }

    // user is authenticated; redirect to the requested page
    if (this.props.authenticated) {
      return <Redirect to={from} />;
    }

    // display the login page
    return (
      <div className={`${Classes.DARK} login-screen-body`}>
        <div className="login-container">
          <img src={logo} width={LOGO_WIDTH_PX} />
          {legalNotice !== '' ? <H2>Legal Notice</H2> : null}
          <div className="login-legal">{legalNotice}</div>
          <div className="login-row user">
            <div className="login-label">Username:</div>
            <div className="login-input">
              <input
                ref={ref => {
                  if (ref) {
                    this.userNameInput = ref;
                  }
                }}
                type="text"
                className="login-input"
                data-cy="username-input"
                value={this.state.username}
                onChange={this.updateState}
                onKeyDown={this.stopPropagationAndLogin}
              />
            </div>
          </div>
          <div className="login-row password">
            <div className="login-label">Password:</div>
            <div className="login-input">
              <input
                type="password"
                className="login-input"
                value={this.state.password}
                disabled={true}
                onKeyDown={this.stopPropagationAndLogin}
                // TODO onChange={/** handle on change event for input field */}
              />
            </div>
          </div>
          <div className="login-row login-button">
            <Button
              onClick={this.login}
              text="Login"
              data-cy="login-btn"
              disabled={this.state.username === ''}
            />
          </div>
        </div>
      </div>
    );
  }

  /**
   * Updates the state with the username at login
   */
  private readonly updateState = (e: React.ChangeEvent<HTMLInputElement>): void => {
    this.setState({ username: e.target.value });
  }

  /**
   * Performs the login
   */
  private readonly login = () => {
    authenticateWith(this.state.username)
      .then(this.setAuthStatus)
      .catch();
  }

  /**
   * Attempts to reconnect to the gateway.
   */
  private readonly reconnect = () => {
    // attempt to reconnect to the gateway
    if (!this.reconnectTimerId) {
      this.delayedReconnect();
    }
  }

  /**
   * Checks if able to connect if not tries to reconnect otherwise updates the auth status
   * @param result result of the auth status
   */
  private readonly ifFailedToConnectTryAgain = (result: AuthStatus): void => {
    if (result.failedToConnect) {
      this.reconnectTimerId = undefined;
      this.reconnect();
    } else {
      this.setAuthStatus(result);
    }
  }

  /**
   * Checks if user is authenticated and attempts to connect
   */
  private readonly reconnectIfNotAuthenticated = () => {
    if (this.props.failedToConnect) {
      checkIsAuthenticated()
        .then(this.ifFailedToConnectTryAgain)
        .catch();
    }
  }

  /**
   * Timer delay to reattempt to login
   */
  private readonly delayedReconnect = () => {
    delay(this.reconnectIfNotAuthenticated, RECONNECT_TIMEOUT_MS);
  }

  /**
   * Set the login status.
   *
   * @param user the user's authentication status
   */
  private readonly setAuthStatus = (user: AuthStatus): void => {
    this.props.setAuthStatus(user);
  }

  /**
   * Stops the propagation of the onEnter keypress and calls login
   */
  private readonly stopPropagationAndLogin = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && this.state.username !== '') {
      e.stopPropagation();
      this.login();
    }
  }
}
