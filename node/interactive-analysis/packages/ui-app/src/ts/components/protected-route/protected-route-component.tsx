import React from 'react';
import { Redirect } from 'react-router';
import { ProtectedRouteProps } from './types';

/**
 * Routes to the login screen if the user session is not authenticated
 */
export class ProtectedRouteComponent extends React.Component<ProtectedRouteProps, never> {
  public constructor(props) {
    super(props);
  }

  public render() {
    if (this.props.authenticated) {
      return this.props.render(this.props.componentProps);
    }
    return (
      <Redirect
        to={{
          pathname: '/login',
          state: { from: this.props.path }
        }}
      />
    );
  }
}
