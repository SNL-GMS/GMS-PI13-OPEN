import History from 'history';
import { RouteProps } from 'react-router';

export interface ProtectedRouteProps extends RouteProps {
  location: History.Location;
  path: string;
  authenticated: boolean;
  componentProps: any;
  render(props): any;
}
