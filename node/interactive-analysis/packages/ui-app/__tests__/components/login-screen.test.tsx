import React from 'react';
import { LoginScreenComponent } from '../../src/ts/components/login-screen/login-screen-component';
import { LoginScreenReduxProps } from '../../src/ts/components/login-screen/types';

// tslint:disable-next-line:no-implicit-dependencies no-var-requires no-require-imports
const Enzyme = require('enzyme');

const mockLocation: any = { state: undefined };
const reduxProps: LoginScreenReduxProps = {
  location: mockLocation,
  authenticated: false,
  authenticationCheckComplete: false,
  failedToConnect: true,
  setAuthStatus: jest.fn()
};

// set up window alert and open so we don't see errors
(window as any).alert = jest.fn();
(window as any).open = jest.fn();
describe('Login screen', () => {
  it('should be defined', () => {
    expect(LoginScreenComponent).toBeDefined();
  });

  const loginScreenNonIdealState: any = Enzyme.mount(<LoginScreenComponent {...reduxProps} />);

  it('failed to connect should return non ideal state', () => {
    expect(loginScreenNonIdealState).toMatchSnapshot();
  });

  reduxProps.failedToConnect = false;

  const loginScreenNonIdealState2: any = Enzyme.mount(<LoginScreenComponent {...reduxProps} />);

  it('failed authentication check complete should return non ideal state', () => {
    expect(loginScreenNonIdealState2).toMatchSnapshot();
  });

  reduxProps.authenticationCheckComplete = true;
  reduxProps.authenticated = false;

  const loginScreen: any = Enzyme.mount(<LoginScreenComponent {...reduxProps} />);

  it('Connected, authentication check complete, and not authenticated should return login page', () => {
    expect(loginScreen).toMatchSnapshot();
  });

  const loginScreen2: any = Enzyme.shallow(<LoginScreenComponent {...reduxProps} />);

  loginScreen2.instance().setAuthStatus({
    userName: 'someUser',
    authenticated: false,
    authenticationCheckComplete: true,
    failedToConnect: jest.fn()
  });
  // tslint:disable-next-line: no-unbound-method
  expect(reduxProps.setAuthStatus).toHaveBeenCalled();

  reduxProps.failedToConnect = true;
  const loginScreen3: any = Enzyme.shallow(<LoginScreenComponent {...reduxProps} />);

  reduxProps.authenticated = true;
  reduxProps.authenticationCheckComplete = true;
  reduxProps.failedToConnect = false;

  const loginRedirect: any = Enzyme.shallow(<LoginScreenComponent {...reduxProps} />);

  it('Authenticated and accessing login page should return redirect', () => {
    expect(loginRedirect).toMatchSnapshot();
  });

  it('Private methods work as expected', async () => {
    loginScreen3.instance().state.username = 'someUser';
    const input = { key: 'Enter', stopPropagation: jest.fn() };
    loginScreen3.instance().stopPropagationAndLogin(input);
    expect(input.stopPropagation).toHaveBeenCalled();

    loginScreen3.instance().reconnectTimerId = undefined;
    loginScreen3.instance().forceUpdate();
    loginScreen3.instance().reconnect();
    // tslint:disable-next-line: no-unbound-method
    expect(reduxProps.setAuthStatus).toHaveBeenCalled();

    await loginScreen3.instance().reconnectIfNotAuthenticated();
    // tslint:disable-next-line: no-unbound-method
    expect(reduxProps.setAuthStatus).toHaveBeenCalled();

    const authStatus: any = { failedToConnect: true, authenticated: true };
    loginScreen3.instance().ifFailedToConnectTryAgain(authStatus);
    // tslint:disable-next-line
    expect(loginScreen3.instance().reconnectTimerId).toBeUndefined;

    authStatus.failedToConnect = false;
    loginScreen3.instance().ifFailedToConnectTryAgain(authStatus);
    // tslint:disable-next-line: no-unbound-method
    expect(reduxProps.setAuthStatus).toHaveBeenCalled();

    loginScreen3.instance().setAuthStatus(authStatus);
    loginScreen3.instance().setState = jest.fn();
    loginScreen3.instance().updateState({ target: { value: 'someUsername' } });
    expect(loginScreen3.instance().setState).toHaveBeenCalled();
  });

  it('React methods work as expected', () => {
    const usernameInput = { username: 'someUser', focus: jest.fn() };
    loginScreen3.instance().userNameInput = usernameInput;
    loginScreen3.instance().componentDidUpdate({}, {});
    expect(usernameInput.focus).toHaveBeenCalled();

    loginScreen3.instance().reconnectTimerId = 'someId';
    loginScreen3.instance().componentWillUnmount();
    expect(loginScreen3.instance().reconnectTimerId).toBeUndefined();
  });
});
