import React from 'react';
import { ProtectedRouteComponent } from '../../src/ts/components/protected-route/protected-route-component';

// tslint:disable-next-line:no-implicit-dependencies no-var-requires no-require-imports
const Enzyme = require('enzyme');

// set up window alert and open so we don't see errors
(window as any).alert = jest.fn();
(window as any).open = jest.fn();

const props: any = {
  authenticated: true,
  componentProps: {},
  render: jest.fn(e => <div />),
  path: 'somePath'
};
describe('Protected route', () => {
  it('should be defined', () => {
    expect(ProtectedRouteComponent).toBeDefined();
  });

  const protectedRouteRenderAuthenticated: any = Enzyme.mount(
    <ProtectedRouteComponent {...props} />
  );

  it('Authenticated should match snapshot', () => {
    expect(protectedRouteRenderAuthenticated).toMatchSnapshot();
    expect(props.render).toHaveBeenCalled();
  });

  props.authenticated = false;

  const protectedRouteRedirect: any = Enzyme.shallow(<ProtectedRouteComponent {...props} />);

  it('Not authenticated should match snapshot', () => {
    expect(protectedRouteRedirect).toMatchSnapshot();
  });
});
